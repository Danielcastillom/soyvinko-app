package com.soyvinko.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import coil3.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.MapKit.MKMapView
import platform.MapKit.addOverlay
import platform.MapKit.overlays
import platform.MapKit.removeOverlays
import platform.MapKit.MKMarkerAnnotationView
import platform.MapKit.MKPointAnnotation
import platform.MapKit.MKPolyline
import platform.MapKit.MKPolylineRenderer
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

private fun CLLocation.toGeoPos(): GeoPos {
    val (lat, lon) = coordinate.useContents { latitude to longitude }
    return GeoPos(lat, lon, horizontalAccuracy.takeIf { it >= 0 }?.toFloat())
}

internal fun locationAuthorized(): Boolean = CLLocationManager.authorizationStatus().let {
    it == kCLAuthorizationStatusAuthorizedWhenInUse || it == kCLAuthorizationStatusAuthorizedAlways
}

/**
 * Delegado único para permisos y posiciones. CLLocationManager exige vivir en el hilo principal
 * y que su delegado responda a didFailWithError cuando se piden posiciones.
 */
private class LocationDelegate(
    val onAuthorization: (CLLocationManager) -> Unit = {},
    val onLocation: (CLLocation) -> Unit = {},
    val onError: () -> Unit = {},
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) = onAuthorization(manager)

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        (didUpdateLocations.lastOrNull() as? CLLocation)?.let(onLocation)
    }

    @ObjCSignatureOverride
    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) = onError()
}

actual fun hasLocationPermission(context: PlatformContext): Boolean = locationAuthorized()

/** Pide permiso de ubicación "mientras se usa la app" (si falta) y luego ejecuta `onGranted`. */
@Composable
actual fun rememberLocationPermission(onDenied: () -> Unit, onGranted: () -> Unit): () -> Unit {
    val granted = rememberUpdatedState(onGranted)
    val denied = rememberUpdatedState(onDenied)
    val holder = remember { arrayOfNulls<Any>(2) }
    return {
        when {
            locationAuthorized() -> granted.value()
            CLLocationManager.authorizationStatus() != kCLAuthorizationStatusNotDetermined -> denied.value()
            else -> {
                var asked = false
                val delegate = LocationDelegate(onAuthorization = { manager ->
                    // El delegado se llama una vez al asignarse (estado "sin decidir") y otra con la respuesta.
                    val status = manager.authorizationStatus
                    if (asked && status != kCLAuthorizationStatusNotDetermined) {
                        holder[0] = null
                        holder[1] = null
                        if (locationAuthorized()) granted.value() else denied.value()
                    }
                })
                val manager = CLLocationManager().apply { this.delegate = delegate }
                holder[0] = manager
                holder[1] = delegate
                asked = true
                manager.requestWhenInUseAuthorization()
            }
        }
    }
}

/** Posición actual precisa (requiere permiso). Null si el GPS no responde en 25 s. */
actual suspend fun currentPosition(context: PlatformContext): GeoPos? {
    if (!locationAuthorized()) return null
    return withContext(Dispatchers.Main) {
        withTimeoutOrNull(25_000) {
            suspendCancellableCoroutine { cont ->
                val manager = CLLocationManager()
                var done = false
                fun finish(pos: GeoPos?) {
                    if (done) return
                    done = true
                    manager.stopUpdatingLocation()
                    manager.delegate = null
                    if (cont.isActive) cont.resume(pos)
                }
                val delegate = LocationDelegate(
                    onLocation = { loc ->
                        // Se descartan posiciones viejas en caché; se espera una fija reciente.
                        val age = NSDate().timeIntervalSince1970 - loc.timestamp.timeIntervalSince1970
                        if (age < 30 && loc.horizontalAccuracy in 0.0..200.0) finish(loc.toGeoPos())
                    },
                    onError = { finish(null) },
                )
                manager.delegate = delegate
                manager.desiredAccuracy = kCLLocationAccuracyBest
                manager.startUpdatingLocation()
                cont.invokeOnCancellation {
                    // Mantiene vivo el delegado hasta cancelar (la propiedad delegate es débil).
                    delegate.hashCode()
                    manager.stopUpdatingLocation()
                    manager.delegate = null
                }
            }
        }
    }
}

/** Posición en vivo mientras la pantalla está visible; envía como mucho una cada `intervalMs`. */
@Composable
actual fun LiveLocation(enabled: Boolean, intervalMs: Long, onUpdate: (GeoPos) -> Unit) {
    val update = rememberUpdatedState(onUpdate)
    DisposableEffect(enabled, intervalMs) {
        if (!enabled || !locationAuthorized()) return@DisposableEffect onDispose { }
        var last = 0.0
        val delegate = LocationDelegate(onLocation = { loc ->
            val now = NSDate().timeIntervalSince1970
            if ((now - last) * 1000 >= intervalMs) {
                last = now
                update.value(loc.toGeoPos())
            }
        })
        val manager = CLLocationManager().apply {
            this.delegate = delegate
            desiredAccuracy = kCLLocationAccuracyBest
            distanceFilter = 15.0
            startUpdatingLocation()
        }
        onDispose {
            manager.stopUpdatingLocation()
            manager.delegate = null
            delegate.hashCode()
        }
    }
}

/** Marcador con el color de la app (morado, verde…). */
private class ColoredPin(val argb: Int) : MKPointAnnotation()

/**
 * Mapa con Apple Maps (MapKit). Muestra marcadores y opcionalmente una línea entre ellos;
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
    val delegate = remember {
        VinkoMapDelegate().apply {
            viewFor = { map, annotation ->
                (annotation as? ColoredPin)?.let { pin ->
                    val view = (map.dequeueReusableAnnotationViewWithIdentifier("pin") as? MKMarkerAnnotationView)
                        ?: MKMarkerAnnotationView(annotation = pin, reuseIdentifier = "pin")
                    view.annotation = pin
                    view.markerTintColor = uiColor(pin.argb)
                    view.canShowCallout = true
                    view
                }
            }
            rendererFor = { overlay ->
                MKPolylineRenderer(polyline = overlay as MKPolyline).apply {
                    strokeColor = uiColor(0xFF6F3DFF.toInt())
                    lineWidth = 4.0
                }
            }
        }
    }
    delegate.onTap = onTap
    val state = remember { object { var fitted = false } }
    UIKitView(
        factory = {
            MKMapView().apply {
                this.delegate = delegate
                showsUserLocation = locationAuthorized()
                show(center ?: CARACAS, zoom, animated = false)
                delegate.attachTap(this)
            }
        },
        modifier = modifier,
        update = { map ->
            map.removeAnnotations(map.annotations.filterIsInstance<ColoredPin>())
            map.removeOverlays(map.overlays)
            map.addAnnotations(markers.map { m ->
                ColoredPin(m.color).apply {
                    setCoordinate(coordinate(m.pos))
                    setTitle(m.title)
                }
            })
            if (drawLine && markers.size >= 2) map.addOverlay(polylineOf(markers.map { it.pos }))
            when {
                center != null -> map.setCenterCoordinate(coordinate(center), animated = true)
                markers.size == 1 -> map.setCenterCoordinate(coordinate(markers[0].pos), animated = true)
                markers.size > 1 && !state.fitted -> {
                    state.fitted = true
                    dispatch_async(dispatch_get_main_queue()) { map.fit(markers.map { it.pos }, maxZoom = 16.0) }
                }
            }
        },
    )
}

/** Abre la navegación paso a paso: Google Maps o Waze si están instalados; si no, Apple Maps. */
actual fun openTurnByTurn(context: PlatformContext, to: GeoPos, label: String): Boolean {
    val q = encodeUrlComponent(label)
    return openExternalUrl(context, "comgooglemaps://?daddr=${to.lat},${to.lon}&directionsmode=driving") ||
        openExternalUrl(context, "waze://?ll=${to.lat},${to.lon}&navigate=yes") ||
        openExternalUrl(context, "maps://?daddr=${to.lat},${to.lon}&q=$q&dirflg=d")
}
