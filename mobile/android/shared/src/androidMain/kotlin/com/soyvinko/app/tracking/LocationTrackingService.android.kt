package com.soyvinko.app.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import coil3.PlatformContext
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.hasLocationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ACTION_START = "com.soyvinko.app.tracking.START"
private const val ACTION_STOP = "com.soyvinko.app.tracking.STOP"
private const val EXTRA_CONTRACT = "contract"
private const val EXTRA_MODE = "mode"
private const val EXTRA_LABEL = "label"

internal actual fun startPlatformTracking(context: PlatformContext, contractId: String, mode: TrackingMode, label: String) {
    val intent = Intent(context, LocationTrackingService::class.java)
        .setAction(ACTION_START)
        .putExtra(EXTRA_CONTRACT, contractId)
        .putExtra(EXTRA_MODE, mode.name)
        .putExtra(EXTRA_LABEL, label)
    ContextCompat.startForegroundService(context, intent)
}

internal actual fun stopPlatformTracking(context: PlatformContext) {
    context.startService(Intent(context, LocationTrackingService::class.java).setAction(ACTION_STOP))
}

@Composable
actual fun rememberTrackingStarter(onDenied: () -> Unit): (contractId: String, mode: TrackingMode, label: String) -> Unit {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<Triple<String, TrackingMode, String>>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val p = pending[0] ?: return@rememberLauncherForActivityResult
        pending[0] = null
        if (hasLocationPermission(context)) LocationTracking.start(context, p.first, p.second, p.third) else onDenied()
    }
    return { contract, mode, label ->
        val missing = buildList {
            if (!hasLocationPermission(context)) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (missing.isEmpty()) LocationTracking.start(context, contract, mode, label)
        else {
            pending[0] = Triple(contract, mode, label)
            launcher.launch(missing.toTypedArray())
        }
    }
}

class LocationTrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var callback: LocationCallback? = null
    private var heartbeat: Job? = null
    private var lastFix: android.location.Location? = null
    private var lastSent = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                finish(null)
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val contract = intent.getStringExtra(EXTRA_CONTRACT) ?: return START_NOT_STICKY.also { stopSelf() }
                val mode = runCatching { TrackingMode.valueOf(intent.getStringExtra(EXTRA_MODE)!!) }.getOrDefault(TrackingMode.PROFESSIONAL)
                val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
                begin(contract, mode, label)
            }
        }
        // Si Android cierra el proceso, lo recrea con el mismo servicio (la sesión se renueva sola).
        return START_REDELIVER_INTENT
    }

    @SuppressLint("MissingPermission")
    private fun begin(contract: String, mode: TrackingMode, label: String) {
        if (!hasLocationPermission(this)) {
            finish("Sin permiso de ubicación.")
            return
        }
        stopUpdates()
        LocationTracking.update { TrackingSession(contract, mode, label) }
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification(mode, label),
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        val client = LocationServices.getFusedLocationProviderClient(this)
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val fix = result.lastLocation ?: return
                lastFix = fix
                if (System.currentTimeMillis() - lastSent >= 10_000) send(contract, mode, fix)
            }
        }.also {
            client.requestLocationUpdates(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15_000).setMinUpdateDistanceMeters(10f).build(),
                it, mainLooper,
            )
        }
        // Reenvía cada 60 s aunque no haya movimiento: el servidor descarta posiciones de más de 15 min.
        heartbeat = scope.launch {
            while (isActive) {
                delay(60_000)
                lastFix?.let { if (System.currentTimeMillis() - lastSent >= 55_000) send(contract, mode, it) }
            }
        }
    }

    private fun send(contract: String, mode: TrackingMode, fix: android.location.Location) {
        lastSent = System.currentTimeMillis()
        val pos = GeoPos(fix.latitude, fix.longitude, if (fix.hasAccuracy()) fix.accuracy else null)
        scope.launch {
            val stop = reportTrackingFix(contract, mode, pos) ?: return@launch
            withContext(Dispatchers.Main) { finish(stop) }
        }
    }

    private fun finish(reason: String?) {
        stopUpdates()
        LocationTracking.update { null }
        if (reason != null) android.util.Log.i("SoyVinko", "Seguimiento detenido: $reason")
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopUpdates() {
        callback?.let { LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(it) }
        callback = null
        heartbeat?.cancel()
        heartbeat = null
    }

    override fun onDestroy() {
        stopUpdates()
        LocationTracking.update { null }
        scope.cancel()
        super.onDestroy()
    }

    private fun notification(mode: TrackingMode, label: String): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Ubicación del servicio", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Aviso mientras SoyVinko comparte tu ubicación de un servicio."
                },
            )
        }
        // La pantalla principal de la app (el módulo compartido no conoce la actividad).
        val launch = (packageManager.getLaunchIntentForPackage(packageName) ?: Intent()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open = PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(
            this, 1, Intent(this, LocationTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = when (mode) {
            TrackingMode.PROFESSIONAL -> "Compartiendo tu trayecto"
            TrackingMode.CUSTOMER -> "Compartiendo tu ubicación en vivo"
        }
        val text = if (label.isBlank()) "SoyVinko envía tu posición del servicio en curso." else "Visible para $label mientras dure el servicio."
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(smallIcon())
            .setColor(0xFF6F3DFF.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setContentIntent(open)
            .addAction(0, "Detener", stop)
            .build()
    }

    @SuppressLint("DiscouragedApi")
    private fun smallIcon(): Int =
        resources.getIdentifier("ic_launcher_foreground", "drawable", packageName).takeIf { it != 0 } ?: applicationInfo.icon

    private companion object {
        const val CHANNEL = "service_location"
        const val NOTIFICATION_ID = 4107
    }
}
