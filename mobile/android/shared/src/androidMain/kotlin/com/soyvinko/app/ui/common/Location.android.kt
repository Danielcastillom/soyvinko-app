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
    onTap: ((GeoPos) -> Unit)?,
) {
    val context = LocalContext.current
    remember {
        Configuration.getInstance().apply {
            userAgentValue = "SoyVinko-Android/${AndroidPlatform.versionName}"
            osmdroidBasePath = context.cacheDir.resolve("osmdroid")
            osmdroidTileCache = context.cacheDir.resolve("osmdroid/tiles")
        }
    }
    var fitted by remember { mutableStateOf(false) }
    AndroidView(
        // El MapView dibuja fuera de sus límites si no se recorta.
        modifier = modifier.clipToBounds(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(zoom)
                // Venezuela por defecto.
                controller.setCenter(GeoPoint(center?.lat ?: 10.4806, center?.lon ?: -66.9036))
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
            map.overlays.removeAll { it is Marker || it is Polyline }
            markers.forEach { m ->
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(m.pos.lat, m.pos.lon)
                    title = m.title
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(m.color)
                        setStroke(6, android.graphics.Color.WHITE)
                        setSize(48, 48)
                    }
                })
            }
            if (drawLine && markers.size >= 2) {
                map.overlays.add(Polyline(map).apply {
                    setPoints(markers.map { GeoPoint(it.pos.lat, it.pos.lon) })
                    outlinePaint.color = 0xFF6F3DFF.toInt()
                    outlinePaint.strokeWidth = 8f
                })
            }
            when {
                center != null -> map.controller.setCenter(GeoPoint(center.lat, center.lon))
                markers.size == 1 -> map.controller.setCenter(GeoPoint(markers[0].pos.lat, markers[0].pos.lon))
                markers.size > 1 && !fitted -> {
                    fitted = true
                    map.post {
                        map.zoomToBoundingBox(BoundingBox.fromGeoPoints(markers.map { GeoPoint(it.pos.lat, it.pos.lon) }).increaseByScale(1.4f), false)
                    }
                }
            }
            map.invalidate()
        },
    )
}

/** Abre la navegación paso a paso en la app de mapas del teléfono (Google Maps, Waze, etc.). */
actual fun openTurnByTurn(context: PlatformContext, to: GeoPos, label: String): Boolean =
    openExternalUrl(context, "geo:${to.lat},${to.lon}?q=${to.lat},${to.lon}(${android.net.Uri.encode(label)})")
