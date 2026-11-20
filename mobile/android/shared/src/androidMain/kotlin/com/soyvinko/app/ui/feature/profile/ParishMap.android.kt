package com.soyvinko.app.ui.feature.profile

import coil3.compose.LocalPlatformContext
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.soyvinko.app.platform.AndroidPlatform
import com.soyvinko.app.ui.common.GeoPos
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon

private const val PRIMARY = 0xFF6F3DFF.toInt()

/** Versión Android (osmdroid). */
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
    val context = LocalPlatformContext.current
    remember {
        Configuration.getInstance().apply {
            userAgentValue = "SoyVinko-Android/${AndroidPlatform.versionName}"
            osmdroidBasePath = context.cacheDir.resolve("osmdroid")
            osmdroidTileCache = context.cacheDir.resolve("osmdroid/tiles")
        }
    }
    val pick = rememberUpdatedState(onPick)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                minZoomLevel = 5.0
                controller.setZoom(14.0)
                controller.setCenter(GeoPoint(center.lat, center.lon))
                overlays.add(object : Overlay() {
                    override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                        val p = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
                        pick.value(GeoPos(p.latitude, p.longitude))
                        return true
                    }
                })
            }
        },
        onRelease = { it.onDetach() },
        update = { map ->
            if (map.tag != code) {
                map.tag = code
                map.overlays.removeAll { it is Polygon }
                rings.forEach { polygon ->
                    val shape = Polygon(map).apply {
                        setPoints(polygon.first().map { GeoPoint(it.lat, it.lon) })
                        setHoles(polygon.drop(1).map { ring -> ring.map { GeoPoint(it.lat, it.lon) } })
                        fillPaint.color = 0x146F3DFF
                        outlinePaint.color = PRIMARY
                        outlinePaint.strokeWidth = 5f
                        setOnClickListener { _, _, _ -> false }
                    }
                    map.overlays.add(0, shape)
                }
                if (bbox.size == 4) {
                    val box = BoundingBox(bbox[3], bbox[2], bbox[1], bbox[0])
                    val fit = { map.zoomToBoundingBox(box, false, 48) ; Unit }
                    if (map.width > 0 && map.height > 0) map.post { fit() } else map.addOnFirstLayoutListener { _, _, _, _, _ -> fit() }
                }
            }
            map.overlays.removeAll { it is Marker }
            if (point != null) {
                val geo = GeoPoint(point.lat, point.lon)
                map.overlays.add(Marker(map).apply {
                    position = geo
                    title = "Dirección seleccionada"
                    isDraggable = true
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(PRIMARY)
                        setStroke(8, android.graphics.Color.WHITE)
                        setSize(64, 64)
                    }
                    setOnMarkerClickListener { _, _ -> false }
                    setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                        override fun onMarkerDrag(marker: Marker) = Unit
                        override fun onMarkerDragStart(marker: Marker) = Unit
                        override fun onMarkerDragEnd(marker: Marker) {
                            pick.value(GeoPos(marker.position.latitude, marker.position.longitude))
                        }
                    })
                })
                if (map.width > 0 && !map.boundingBox.contains(geo)) {
                    map.controller.animateTo(geo, maxOf(map.zoomLevelDouble, 16.0), 400L)
                }
            }
            map.invalidate()
        },
    )
}
