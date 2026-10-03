package com.soyvinko.app.tracking

import androidx.compose.runtime.Composable
import coil3.PlatformContext
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.hasLocationPermission
import com.soyvinko.app.ui.common.rememberLocationPermission
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject

/**
 * Seguimiento con la app en segundo plano: CLLocationManager con allowsBackgroundLocationUpdates
 * (requiere UIBackgroundModes = location en Info.plist). iOS muestra el indicador azul mientras dura.
 */
private object IosTracker {
    // Kotlin/Native no admite un `object` que herede de NSObject: el delegado es una clase aparte.
    private val delegate = TrackerDelegate()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var manager: CLLocationManager? = null
    private var heartbeat: Job? = null
    private var contract = ""
    private var mode = TrackingMode.PROFESSIONAL
    private var lastFix: GeoPos? = null
    private var lastSent = 0L

    @OptIn(ExperimentalForeignApi::class)
    fun start(contractId: String, trackingMode: TrackingMode, label: String) {
        stop()
        contract = contractId
        mode = trackingMode
        LocationTracking.update { TrackingSession(contractId, trackingMode, label) }
        manager = CLLocationManager().apply {
            delegate = this@IosTracker.delegate
            desiredAccuracy = kCLLocationAccuracyBest
            distanceFilter = 10.0
            allowsBackgroundLocationUpdates = true
            pausesLocationUpdatesAutomatically = false
            showsBackgroundLocationIndicator = true
            startUpdatingLocation()
        }
        // Reenvía cada 60 s aunque no haya movimiento: el servidor descarta posiciones de más de 15 min.
        heartbeat = scope.launch {
            while (isActive) {
                delay(60_000)
                lastFix?.let { if (nowMs() - lastSent >= 55_000) send(it) }
            }
        }
    }

    fun stop() {
        manager?.stopUpdatingLocation()
        manager?.delegate = null
        manager = null
        heartbeat?.cancel()
        heartbeat = null
        lastFix = null
        LocationTracking.update { null }
    }

    private fun send(pos: GeoPos) {
        lastSent = nowMs()
        val c = contract
        val m = mode
        scope.launch {
            val reason = reportTrackingFix(c, m, pos) ?: return@launch
            println("SoyVinko: seguimiento detenido: $reason")
            stop()
        }
    }

    fun onLocation(loc: CLLocation) {
        val (lat, lon) = loc.coordinate.useContents { latitude to longitude }
        val fix = GeoPos(lat, lon, loc.horizontalAccuracy.takeIf { it >= 0 }?.toFloat())
        lastFix = fix
        if (nowMs() - lastSent >= 10_000) send(fix)
    }
}

private class TrackerDelegate : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        (didUpdateLocations.lastOrNull() as? CLLocation)?.let(IosTracker::onLocation)
    }

    @ObjCSignatureOverride
    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        LocationTracking.update { it?.copy(error = "Sin señal de GPS") }
    }
}

internal actual fun startPlatformTracking(context: PlatformContext, contractId: String, mode: TrackingMode, label: String) =
    IosTracker.start(contractId, mode, label)

internal actual fun stopPlatformTracking(context: PlatformContext) = IosTracker.stop()

@Composable
actual fun rememberTrackingStarter(onDenied: () -> Unit): (contractId: String, mode: TrackingMode, label: String) -> Unit {
    val pending = androidx.compose.runtime.remember { arrayOfNulls<Triple<String, TrackingMode, String>>(1) }
    val context = coil3.compose.LocalPlatformContext.current
    val ask = rememberLocationPermission(onDenied = onDenied) {
        val p = pending[0] ?: return@rememberLocationPermission
        pending[0] = null
        LocationTracking.start(context, p.first, p.second, p.third)
    }
    return { contract, mode, label ->
        if (hasLocationPermission(context)) LocationTracking.start(context, contract, mode, label)
        else {
            pending[0] = Triple(contract, mode, label)
            ask()
        }
    }
}
