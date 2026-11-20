package com.soyvinko.app.ui.common

import android.Manifest
import android.annotation.SuppressLint
import coil3.PlatformContext
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.soyvinko.app.platform.AndroidPlatform
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.coroutines.resume
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL


actual fun hasLocationPermission(context: PlatformContext): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** Pide permiso de ubicación (si falta) y luego ejecuta `onGranted`. */
@Composable
actual fun rememberLocationPermission(onDenied: () -> Unit, onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) onGranted() else onDenied()
    }
    return {
        if (hasLocationPermission(context)) onGranted()
        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}

/** Posición actual precisa (requiere permiso). Null si el GPS no responde. */
@SuppressLint("MissingPermission")
actual suspend fun currentPosition(context: PlatformContext): GeoPos? {
    if (!hasLocationPermission(context)) return null
    val client = LocationServices.getFusedLocationProviderClient(context)
    val cancel = CancellationTokenSource()
    return suspendCancellableCoroutine { cont ->
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancel.token)
            .addOnSuccessListener { loc -> cont.resume(loc?.let { GeoPos(it.latitude, it.longitude, it.accuracy) }) }
            .addOnFailureListener { cont.resume(null) }
        cont.invokeOnCancellation { cancel.cancel() }
    }
}

/**
 * Posición en vivo mientras la pantalla está visible (p. ej. el profesional en camino).
 * Se detiene sola al salir de la pantalla.
 */
@SuppressLint("MissingPermission")
@Composable
actual fun LiveLocation(enabled: Boolean, intervalMs: Long, onUpdate: (GeoPos) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        if (!enabled || !hasLocationPermission(context)) return@DisposableEffect onDispose { }
        val client = LocationServices.getFusedLocationProviderClient(context)
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onUpdate(GeoPos(it.latitude, it.longitude, it.accuracy)) }
            }
        }
        client.requestLocationUpdates(
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs).setMinUpdateDistanceMeters(15f).build(),
            callback,
            context.mainLooper,
        )
        onDispose { client.removeLocationUpdates(callback) }
    }
}


/**
 * Mapa OpenStreetMap (sin clave). Muestra marcadores y opcionalmente una línea entre ellos;
 * `onTap` devuelve el punto tocado (para elegir una ubicación).
 */
@Composable
actual fun OsmMap(
    markers: List<MapMarker>,
    modifier: Modifier,
    center: GeoPos?,
    zoom: Double,
    drawLine: Boolean,
    routePoints: List<GeoPos>?,
    onTap: ((GeoPos) -> Unit)?,
) {
    val context = LocalContext.current
    val app = runCatching { LocalApp.current }.getOrNull()
    val avatarCache = remember { mutableMapOf<String, Bitmap>() }
    var avatarLoadedKey by remember { mutableStateOf("") }

    // Descargar en segundo plano la foto de la cuenta para los marcadores con avatarUrl
    markers.forEach { m ->
        val url = m.avatarUrl
        if (!url.isNullOrBlank() && !avatarCache.containsKey(url)) {
            val token = app?.api?.accessToken
            LaunchedEffect(url, token) {
                val fullUrl = when {
                    url.startsWith("http") -> url
                    url.startsWith("/") -> app?.api?.absoluteUrl(url) ?: url
                    else -> app?.api?.absoluteUrl("/api/v1/files/$url") ?: "https://soyvinko.com/api/v1/files/$url"
                }
                val bmp = downloadAvatarBitmap(fullUrl, token)
                if (bmp != null) {
                    avatarCache[url] = bmp
                    avatarLoadedKey = "$url-${System.currentTimeMillis()}"
                }
            }
        }
    }

    remember {
        Configuration.getInstance().apply {
            userAgentValue = "SoyVinko-Android/${AndroidPlatform.versionName}"
            osmdroidBasePath = context.cacheDir.resolve("osmdroid")
            osmdroidTileCache = context.cacheDir.resolve("osmdroid/tiles")
        }
    }
    var fitted by remember(markers.map { it.pos }, routePoints?.size) { mutableStateOf(false) }
    var lastCenter by remember { mutableStateOf<GeoPos?>(null) }
    var lastZoom by remember { mutableStateOf(zoom) }

    AndroidView(
        // El MapView dibuja fuera de sus límites si no se recorta.
        modifier = modifier.clipToBounds(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(zoom)
                controller.setCenter(GeoPoint(center?.lat ?: 10.4806, center?.lon ?: -66.9036))
                // Evita que el scroll vertical de Compose capture los gestos al arrastrar el mapa
                setOnTouchListener { v, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    false
                }
                if (onTap != null) {
                    overlays.add(object : org.osmdroid.views.overlay.Overlay() {
                        override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                            val p = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
                            onTap(GeoPos(p.latitude, p.longitude))
                            return true
                        }
                    })
                }
            }
        },
        update = { map ->
            val _dummy = avatarLoadedKey
            map.overlays.removeAll { it is Marker || it is Polyline }
            markers.forEach { m ->
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(m.pos.lat, m.pos.lon)
                    title = m.title
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    val bmp = m.avatarUrl?.let { avatarCache[it] }
                    icon = createCustomPinDrawable(map.context, m.color, m.iconType, bmp)
                })
            }
            val pointsToDraw = when {
                !routePoints.isNullOrEmpty() -> routePoints.map { GeoPoint(it.lat, it.lon) }
                drawLine && markers.size >= 2 -> markers.map { GeoPoint(it.pos.lat, it.pos.lon) }
                else -> emptyList()
            }
            if (pointsToDraw.size >= 2) {
                map.overlays.add(Polyline(map).apply {
                    setPoints(pointsToDraw)
                    outlinePaint.color = 0xFF6F3DFF.toInt()
                    outlinePaint.strokeWidth = 10f
                    outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                    outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                })
            }

            // Aplicar cambios de zoom y centro solo si cambian externamente en Compose
            if (kotlin.math.abs(zoom - lastZoom) > 0.05) {
                lastZoom = zoom
                map.controller.setZoom(zoom)
            }
            if (center != null && center != lastCenter) {
                lastCenter = center
                map.controller.animateTo(GeoPoint(center.lat, center.lon))
            }

            if (pointsToDraw.size >= 2 && !fitted) {
                fitted = true
                map.post {
                    try {
                        map.zoomToBoundingBox(BoundingBox.fromGeoPoints(pointsToDraw).increaseByScale(1.3f), true)
                    } catch (_: Exception) {}
                }
            } else if (markers.size > 1 && !fitted) {
                fitted = true
                map.post {
                    try {
                        map.zoomToBoundingBox(BoundingBox.fromGeoPoints(markers.map { GeoPoint(it.pos.lat, it.pos.lon) }).increaseByScale(1.4f), false)
                    } catch (_: Exception) {}
                }
            }
            map.invalidate()
        },
    )
}

/** Abre la navegación paso a paso en la app de mapas del teléfono (Google Maps, Waze, etc.). */
actual fun openTurnByTurn(context: PlatformContext, to: GeoPos, label: String): Boolean =
    openExternalUrl(context, "geo:${to.lat},${to.lon}?q=${to.lat},${to.lon}(${android.net.Uri.encode(label)})")

/** Descarga imagen de la cuenta con cabecera Bearer si aplica. */
private suspend fun downloadAvatarBitmap(urlString: String, token: String?): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
        if (conn.responseCode in 200..299) {
            conn.inputStream.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Genera un marcador tipo pin clásico con punta inferior y círculo interior blanco para destacar
 * el icono o la foto de la cuenta (Dave Bastidas).
 */
private fun createCustomPinDrawable(
    context: android.content.Context,
    color: Int,
    iconType: String?,
    avatarBitmap: Bitmap? = null,
): android.graphics.drawable.Drawable {
    val density = context.resources.displayMetrics.density
    val width = (50 * density).toInt().coerceAtLeast(40)
    val height = (66 * density).toInt().coerceAtLeast(52)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = width / 2f
    val headRadius = 20f * density
    val cy = headRadius + 3f * density
    val tipX = cx
    val tipY = height - 5f * density

    // 1. Sombra suave en la base del pin
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(55, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawOval(
        RectF(cx - 9 * density, height - 7 * density, cx + 9 * density, height - 1 * density),
        shadowPaint,
    )

    // 2. Construir la forma de gota/pin de mapa estilo pin clásico con punta hacia abajo
    val pinPath = Path()
    val dy = tipY - cy
    val ratio = (headRadius / dy).coerceIn(0.1f, 0.99f)
    val angleAlpha = kotlin.math.acos(ratio.toDouble())
    val sinA = kotlin.math.sin(angleAlpha).toFloat()
    val cosA = kotlin.math.cos(angleAlpha).toFloat()

    val txLeft = cx - headRadius * sinA
    val tyLeft = cy + headRadius * cosA

    val sweepAngle = 360f - 2f * Math.toDegrees(angleAlpha).toFloat()
    val startAngle = 90f + Math.toDegrees(angleAlpha).toFloat()

    pinPath.moveTo(tipX, tipY)
    pinPath.lineTo(txLeft, tyLeft)
    pinPath.arcTo(
        RectF(cx - headRadius, cy - headRadius, cx + headRadius, cy + headRadius),
        startAngle,
        sweepAngle,
        false,
    )
    pinPath.lineTo(tipX, tipY)
    pinPath.close()

    // 3. Pintar cuerpo del pin con su color
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, pinPaint)

    // Borde exterior para definición
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(50, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }
    canvas.drawPath(pinPath, strokePaint)

    // 4. Círculo interior BLANCO puro para destacar el icono o la foto
    val innerRadius = headRadius - 3.5f * density
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, innerRadius, whitePaint)

    // 5. Contenido dentro del círculo blanco
    if (avatarBitmap != null) {
        val save = canvas.save()
        val clipPath = Path().apply {
            addCircle(cx, cy, innerRadius - 0.5f * density, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)
        val src = Rect(0, 0, avatarBitmap.width, avatarBitmap.height)
        val dst = RectF(
            cx - innerRadius + 0.5f * density,
            cy - innerRadius + 0.5f * density,
            cx + innerRadius - 0.5f * density,
            cy + innerRadius - 0.5f * density,
        )
        val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(avatarBitmap, src, dst, filterPaint)
        canvas.restoreToCount(save)
    } else {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 17f * density
        }
        val symbol = when (iconType) {
            "avatar" -> "👤"
            "moto" -> "🏍️"
            "carro" -> "🚗"
            "pickup" -> "🛻"
            "camion" -> "🚚"
            else -> "📍"
        }
        val yPos = cy - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(symbol, cx, yPos, textPaint)
    }

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}
