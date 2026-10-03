package com.soyvinko.app.ui.feature.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.soyvinko.app.ui.common.CARACAS
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.VinkoMapDelegate
import com.soyvinko.app.ui.common.coordinate
import com.soyvinko.app.ui.common.dotImage
import com.soyvinko.app.ui.common.fit
import com.soyvinko.app.ui.common.show
import com.soyvinko.app.ui.common.uiColor
import platform.Foundation.NSNumber
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCircle
import platform.MapKit.MKCircleRenderer
import platform.MapKit.MKMapView
import platform.MapKit.addOverlay
import platform.MapKit.overlays
import platform.MapKit.removeOverlays
import platform.MapKit.MKMarkerAnnotationView
import platform.MapKit.MKOverlayRenderer
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UIColor
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private class PinAnnotation(val pin: Pin) : MKPointAnnotation()

private const val PRIMARY = 0xFF6F3DFF.toInt()

/** Versión iOS: Apple Maps con marcadores del sistema (número si hay varios servicios en el mismo punto). */
@Composable
actual fun ExploreMapView(
    pins: List<Pin>,
    modifier: Modifier,
    circle: Pair<GeoPos, Int>?,
    fitKey: Any?,
    initialCenter: GeoPos?,
    initialZoom: Double,
    handle: MapHandle?,
    onPinTap: (Pin) -> Unit,
    onMapTap: ((GeoPos) -> Unit)?,
) {
    val pinTap = rememberUpdatedState(onPinTap)
    val delegate = remember {
        VinkoMapDelegate().apply {
            viewFor = { map, annotation -> (annotation as? PinAnnotation)?.let { pinView(map, it) } }
            rendererFor = { overlay ->
                (overlay as? MKCircle)?.let {
                    MKCircleRenderer(circle = it).apply {
                        fillColor = uiColor(PRIMARY, alpha = 0.06)
                        strokeColor = uiColor(PRIMARY, alpha = 0.8)
                        lineWidth = 1.5
                        lineDashPattern = listOf(NSNumber(int = 7), NSNumber(int = 6))
                    }
                } ?: MKOverlayRenderer(overlay = overlay)
            }
            onSelect = { annotation -> (annotation as? PinAnnotation)?.let { pinTap.value(it.pin) } }
        }
    }
    delegate.onTap = onMapTap
    val state = remember { object { var fitted: Any? = Unit } }
    UIKitView(
        factory = {
            MKMapView().apply {
                this.delegate = delegate
                show(initialCenter ?: CARACAS, initialZoom, animated = false)
                delegate.attachTap(this)
                handle?.fitAction = { points -> fit(points) }
                handle?.centerAction = { pos, zoom -> show(pos, zoom, animated = true) }
            }
        },
        modifier = modifier,
        update = { map ->
            map.removeAnnotations(map.annotations.filterIsInstance<PinAnnotation>())
            map.removeOverlays(map.overlays)
            if (circle != null) {
                map.addOverlay(MKCircle.circleWithCenterCoordinate(coordinate(circle.first), radius = circle.second.toDouble()))
            }
            map.addAnnotations(pins.map { pin ->
                PinAnnotation(pin).apply {
                    setCoordinate(coordinate(pin.pos))
                    setTitle(pin.title)
                }
            })
            val points = pins.map { it.pos } + circle?.let { (c, r) -> circleExtents(c, r) }.orEmpty()
            handle?.points = points
            if (fitKey != state.fitted) {
                state.fitted = fitKey
                // Tras la primera medición del mapa; antes de eso el encuadre no tiene tamaño.
                dispatch_async(dispatch_get_main_queue()) { map.fit(points) }
            }
        },
        onRelease = {
            handle?.fitAction = null
            handle?.centerAction = null
        },
    )
}

private fun pinView(map: MKMapView, annotation: PinAnnotation): MKAnnotationView {
    val pin = annotation.pin
    return when (pin.kind) {
        PinKind.Service, PinKind.Picked -> {
            val view = (map.dequeueReusableAnnotationViewWithIdentifier("service") as? MKMarkerAnnotationView)
                ?: MKMarkerAnnotationView(annotation = annotation, reuseIdentifier = "service")
            view.annotation = annotation
            view.markerTintColor = if (pin.kind == PinKind.Picked) uiColor(0xFF150D2B.toInt()) else uiColor(PRIMARY)
            view.glyphText = if (pin.count > 1) (if (pin.count > 99) "99+" else pin.count.toString()) else null
            view.displayPriority = if (pin.kind == PinKind.Picked) 1000f else 750f
            view.canShowCallout = false
            view
        }
        PinKind.Me, PinKind.Search -> {
            val id = pin.kind.name
            val view = map.dequeueReusableAnnotationViewWithIdentifier(id)
                ?: MKAnnotationView(annotation = annotation, reuseIdentifier = id).apply {
                    image = if (pin.kind == PinKind.Me) {
                        dotImage(24.0, listOf(1.0 to uiColor(0x3342C9FF), 0.62 to UIColor.whiteColor, 0.44 to uiColor(0xFF0284C7.toInt())))
                    } else {
                        dotImage(24.0, listOf(0.8 to uiColor(PRIMARY), 0.6 to UIColor.whiteColor, 0.22 to uiColor(PRIMARY)))
                    }
                }
            view.annotation = annotation
            view.layer.zPosition = if (pin.kind == PinKind.Me) 10.0 else 0.0
            view
        }
    }
}
