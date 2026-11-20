package com.soyvinko.app.ui.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.VinkoMapDelegate
import com.soyvinko.app.ui.common.coordinate
import com.soyvinko.app.ui.common.fit
import com.soyvinko.app.ui.common.polygonOf
import com.soyvinko.app.ui.common.show
import com.soyvinko.app.ui.common.uiColor
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.MapKit.MKMapPointForCoordinate
import platform.MapKit.MKMapRectContainsPoint
import platform.MapKit.MKMapView
import platform.MapKit.addOverlay
import platform.MapKit.overlays
import platform.MapKit.removeOverlays
import platform.MapKit.MKMarkerAnnotationView
import platform.MapKit.MKOverlayRenderer
import platform.MapKit.MKPointAnnotation
import platform.MapKit.MKPolygon
import platform.MapKit.MKPolygonRenderer
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private class PickedPoint : MKPointAnnotation()

/** Versión iOS (Apple Maps): límite de la parroquia y marcador arrastrable. */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ParishMapView(
    code: String?,
    rings: List<List<List<GeoPos>>>,
    bbox: List<Double>,
    center: GeoPos,
    point: GeoPos?,
    onPick: (GeoPos) -> Unit,
    modifier: Modifier,
) {
    val pick = rememberUpdatedState(onPick)
    val delegate = remember {
        VinkoMapDelegate().apply {
            viewFor = { map, annotation ->
                (annotation as? PickedPoint)?.let {
                    val view = (map.dequeueReusableAnnotationViewWithIdentifier("picked") as? MKMarkerAnnotationView)
                        ?: MKMarkerAnnotationView(annotation = it, reuseIdentifier = "picked")
                    view.annotation = it
                    view.markerTintColor = uiColor(PARISH_COLOR)
                    view.draggable = true
                    view
                }
            }
            rendererFor = { overlay ->
                (overlay as? MKPolygon)?.let {
                    MKPolygonRenderer(polygon = it).apply {
                        fillColor = uiColor(PARISH_COLOR, alpha = 0.08)
                        strokeColor = uiColor(PARISH_COLOR)
                        lineWidth = 2.5
                    }
                } ?: MKOverlayRenderer(overlay = overlay)
            }
            onTap = { pick.value(it) }
            onDragEnd = { pick.value(it) }
        }
    }
    val state = remember { object { var code: String? = "\u0000" } }
    UIKitView(
        factory = {
            MKMapView().apply {
                this.delegate = delegate
                show(center, 14.0, animated = false)
                delegate.attachTap(this)
            }
        },
        modifier = modifier,
        update = { map ->
            if (state.code != code) {
                state.code = code
                map.removeOverlays(map.overlays)
                rings.forEach { polygon -> map.addOverlay(polygonOf(polygon.first(), polygon.drop(1))) }
                if (bbox.size == 4) {
                    // bbox GeoJSON: [oeste, sur, este, norte].
                    val corners = listOf(GeoPos(bbox[1], bbox[0]), GeoPos(bbox[3], bbox[2]))
                    dispatch_async(dispatch_get_main_queue()) { map.fit(corners, maxZoom = 17.0) }
                }
            }
            map.removeAnnotations(map.annotations.filterIsInstance<PickedPoint>())
            if (point != null) {
                map.addAnnotation(PickedPoint().apply {
                    setCoordinate(coordinate(point))
                    setTitle("Dirección seleccionada")
                })
                val visible = map.visibleMapRect.useContents { this }
                val inside = MKMapRectContainsPoint(map.visibleMapRect, MKMapPointForCoordinate(coordinate(point)))
                if (!inside && visible.size.width > 0) map.show(point, 16.0, animated = true)
            }
        },
    )
}
