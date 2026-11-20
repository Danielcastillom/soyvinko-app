package com.soyvinko.app.ui.feature.explore

import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import com.soyvinko.app.platform.AndroidPlatform
import com.soyvinko.app.ui.common.GeoPos
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon

private val Caracas = GeoPos(10.4806, -66.9036)

private fun fitTo(map: MapView, points: List<GeoPos>) {
    if (points.isEmpty()) return
    if (!map.isLayoutOccurred) {
        // Encuadrar antes de la primera medición deja el mapa sin zoom: se espera al primer layout.
        map.addOnFirstLayoutListener { _, _, _, _, _ -> fitTo(map, points) }
        return
    }
    map.post {
        if (map.width == 0 || map.height == 0) return@post
        if (points.size == 1 || points.distinct().size == 1) {
            map.controller.setZoom(14.0)
            map.controller.setCenter(GeoPoint(points[0].lat, points[0].lon))
        } else {
            val box = BoundingBox.fromGeoPoints(points.map { GeoPoint(it.lat, it.lon) }).increaseByScale(1.15f)
            map.zoomToBoundingBox(box, false, 48)
            if (map.zoomLevelDouble > 15.0) map.controller.setZoom(15.0)
        }
    }
}

/** Versión Android: osmdroid (OpenStreetMap) igual que OsmMap, que no permite tocar marcadores. */
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
    val context = LocalPlatformContext.current
    remember {
        Configuration.getInstance().apply {
            userAgentValue = "SoyVinko-Android/${AndroidPlatform.versionName}"
            osmdroidBasePath = context.cacheDir.resolve("osmdroid")
            osmdroidTileCache = context.cacheDir.resolve("osmdroid/tiles")
        }
    }
    val pinTap by rememberUpdatedState(onPinTap)
    val mapTap by rememberUpdatedState(onMapTap)
    val icons = remember { mutableMapOf<String, Drawable>() }
    val state = remember { object { var fitted: Any? = Unit } }
    AndroidView(
        modifier = modifier.clipToBounds(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                minZoomLevel = 5.0
                controller.setZoom(initialZoom)
                val start = initialCenter ?: Caracas
                controller.setCenter(GeoPoint(start.lat, start.lon))
                overlays.add(object : Overlay() {
                    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                        val p = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
                        mapTap?.invoke(GeoPos(p.latitude, p.longitude))
                        return mapTap != null
                    }
                })
                handle?.fitAction = { points -> fitTo(this, points) }
                handle?.centerAction = { pos, zoom ->
                    controller.setZoom(zoom)
                    controller.animateTo(GeoPoint(pos.lat, pos.lon))
                }
            }
        },
        onRelease = { map ->
            handle?.fitAction = null
            handle?.centerAction = null
            map.onDetach()
        },
        update = { map ->
            map.overlays.removeAll { it is Marker || it is Polygon }
            if (circle != null) {
                map.overlays.add(Polygon(map).apply {
                    points = Polygon.pointsAsCircle(GeoPoint(circle.first.lat, circle.first.lon), circle.second.toDouble())
                    fillPaint.color = 0x0F6F3DFF
                    outlinePaint.color = 0xCC6F3DFF.toInt()
                    outlinePaint.strokeWidth = 3f
                    outlinePaint.pathEffect = DashPathEffect(floatArrayOf(14f, 12f), 0f)
                    setInfoWindow(null)
                    setOnClickListener { _, _, _ -> false }
                })
            }
            // Los servicios encima de la zona; tu ubicación siempre encima de todo.
            pins.sortedBy { if (it.kind == PinKind.Me) 2 else if (it.kind == PinKind.Service) 1 else 0 }.forEach { pin ->
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(pin.pos.lat, pin.pos.lon)
                    title = pin.title
                    setInfoWindow(null)
                    val iconKey = "${pin.kind}-${pin.count}"
                    icon = icons.getOrPut(iconKey) { pinDrawable(context, pin.kind, pin.count) }
                    if (pin.kind == PinKind.Service || pin.kind == PinKind.Picked) setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    else setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ ->
                        pinTap(pin)
                        true
                    }
                })
            }
            val points = pins.map { it.pos } + circle?.let { (c, r) -> circleExtents(c, r) }.orEmpty()
            handle?.points = points
            if (fitKey != state.fitted) {
                state.fitted = fitKey
                fitTo(map, points)
            }
            map.invalidate()
        },
    )
}

private fun pinDrawable(context: PlatformContext, kind: PinKind, count: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    return when (kind) {
        PinKind.Service, PinKind.Picked -> {
            val w = (36 * density).toInt()
            val h = (46 * density).toInt()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val r = w / 2f - 3 * density
            val cx = w / 2f
            val cy = r + 3 * density
            val path = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
            val tail = Path().apply {
                moveTo(cx - r * 0.62f, cy + r * 0.7f)
                lineTo(cx, h - 2 * density)
                lineTo(cx + r * 0.62f, cy + r * 0.7f)
                close()
            }
            path.op(tail, Path.Op.UNION)
            paint.style = Paint.Style.FILL
            paint.color = if (kind == PinKind.Picked) 0xFF150D2B.toInt() else 0xFF6F3DFF.toInt()
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f * density
            paint.color = android.graphics.Color.WHITE
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
            if (count > 1) {
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = 14 * density
                paint.typeface = Typeface.DEFAULT_BOLD
                val label = if (count > 99) "99+" else count.toString()
                canvas.drawText(label, cx, cy - (paint.descent() + paint.ascent()) / 2, paint)
            } else {
                canvas.drawCircle(cx, cy, r * 0.36f, paint)
            }
            BitmapDrawable(context.resources, bmp)
        }
        PinKind.Me, PinKind.Search -> {
            val size = (24 * density).toInt()
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val c = size / 2f
            if (kind == PinKind.Me) {
                paint.color = 0x3342C9FF
                canvas.drawCircle(c, c, c, paint)
                paint.color = android.graphics.Color.WHITE
                canvas.drawCircle(c, c, c * 0.62f, paint)
                paint.color = 0xFF0284C7.toInt()
                canvas.drawCircle(c, c, c * 0.44f, paint)
            } else {
                paint.color = android.graphics.Color.WHITE
                canvas.drawCircle(c, c, c * 0.8f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3 * density
                paint.color = 0xFF6F3DFF.toInt()
                canvas.drawCircle(c, c, c * 0.62f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(c, c, c * 0.22f, paint)
            }
            BitmapDrawable(context.resources, bmp)
        }
    }
}
