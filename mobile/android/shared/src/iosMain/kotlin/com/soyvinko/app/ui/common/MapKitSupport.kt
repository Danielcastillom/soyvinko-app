package com.soyvinko.app.ui.common

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSSelectorFromString
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKAnnotationViewDragState
import platform.MapKit.MKAnnotationViewDragStateEnding
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKOverlayProtocol
import platform.MapKit.MKOverlayRenderer
import platform.MapKit.MKPolygon
import platform.MapKit.MKPolyline
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView
import platform.darwin.NSObject
import kotlin.math.pow

/** Color ARGB (como en Android) a UIColor. */
internal fun uiColor(argb: Int, alpha: Double? = null) = UIColor(
    red = ((argb shr 16) and 0xFF) / 255.0,
    green = ((argb shr 8) and 0xFF) / 255.0,
    blue = (argb and 0xFF) / 255.0,
    alpha = alpha ?: (((argb ushr 24) and 0xFF) / 255.0),
)

internal fun coordinate(p: GeoPos) = CLLocationCoordinate2DMake(p.lat, p.lon)

@OptIn(ExperimentalForeignApi::class)
internal fun CValue<CLLocationCoordinate2D>.toGeoPos(): GeoPos = useContents { GeoPos(latitude, longitude) }

/** Grados visibles para un zoom tipo OpenStreetMap (15 ≈ un barrio). */
internal fun spanFor(zoom: Double) = (360.0 / 2.0.pow(zoom)) * 3.0

internal const val CARACAS_LAT = 10.4806
internal const val CARACAS_LON = -66.9036

/** Centra el mapa en un punto con el zoom indicado. */
internal fun MKMapView.show(pos: GeoPos, zoom: Double, animated: Boolean) =
    setRegion(MKCoordinateRegionMake(coordinate(pos), MKCoordinateSpanMake(spanFor(zoom), spanFor(zoom))), animated)

/** Encuadra todos los puntos con margen; un único punto se muestra con zoom de barrio. */
internal fun MKMapView.fit(points: List<GeoPos>, maxZoom: Double = 15.0, animated: Boolean = false) {
    if (points.isEmpty()) return
    val minLat = points.minOf { it.lat }
    val maxLat = points.maxOf { it.lat }
    val minLon = points.minOf { it.lon }
    val maxLon = points.maxOf { it.lon }
    val center = GeoPos((minLat + maxLat) / 2, (minLon + maxLon) / 2)
    val minSpan = spanFor(maxZoom)
    val latSpan = maxOf((maxLat - minLat) * 1.3, minSpan)
    val lonSpan = maxOf((maxLon - minLon) * 1.3, minSpan)
    setRegion(MKCoordinateRegionMake(coordinate(center), MKCoordinateSpanMake(latSpan, lonSpan)), animated)
}

/** Línea entre varios puntos (trayecto). */
@OptIn(ExperimentalForeignApi::class)
internal fun polylineOf(points: List<GeoPos>): MKPolyline = memScoped {
    val coords = allocArray<CLLocationCoordinate2D>(points.size)
    points.forEachIndexed { i, p ->
        coords[i].latitude = p.lat
        coords[i].longitude = p.lon
    }
    MKPolyline.polylineWithCoordinates(coords, points.size.toULong())
}

/** Polígono con huecos (límites de parroquias). */
@OptIn(ExperimentalForeignApi::class)
internal fun polygonOf(outer: List<GeoPos>, holes: List<List<GeoPos>> = emptyList()): MKPolygon = memScoped {
    val coords = allocArray<CLLocationCoordinate2D>(outer.size)
    outer.forEachIndexed { i, p ->
        coords[i].latitude = p.lat
        coords[i].longitude = p.lon
    }
    MKPolygon.polygonWithCoordinates(coords, outer.size.toULong(), interiorPolygons = holes.map { polygonOf(it) })
}

/** Círculo de color con borde blanco opcional (tu ubicación, punto elegido). */
@OptIn(ExperimentalForeignApi::class)
internal fun dotImage(size: Double, rings: List<Pair<Double, UIColor>>): UIImage =
    UIGraphicsImageRenderer(size = CGSizeMake(size, size)).imageWithActions { _ ->
        rings.forEach { (fraction, color) ->
            val d = size * fraction
            val o = (size - d) / 2
            color.setFill()
            UIBezierPath.bezierPathWithOvalInRect(CGRectMake(o, o, d, d)).fill()
        }
    }

/**
 * Delegado común de los mapas de MapKit: vistas de marcadores, estilos de líneas/zonas, toques en
 * marcadores, toques libres en el mapa (para elegir un punto) y fin de arrastre de un marcador.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class VinkoMapDelegate : NSObject(), MKMapViewDelegateProtocol {
    var viewFor: (MKMapView, MKAnnotationProtocol) -> MKAnnotationView? = { _, _ -> null }
    var rendererFor: (MKOverlayProtocol) -> MKOverlayRenderer = { MKOverlayRenderer(overlay = it) }
    var onSelect: ((MKAnnotationProtocol) -> Unit)? = null
    var onTap: ((GeoPos) -> Unit)? = null
    var onDragEnd: ((GeoPos) -> Unit)? = null

    @ObjCSignatureOverride
    override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? =
        viewFor(mapView, viewForAnnotation)

    @ObjCSignatureOverride
    override fun mapView(mapView: MKMapView, rendererForOverlay: MKOverlayProtocol): MKOverlayRenderer =
        rendererFor(rendererForOverlay)

    @ObjCSignatureOverride
    override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
        val annotation = didSelectAnnotationView.annotation ?: return
        val select = onSelect ?: return
        mapView.deselectAnnotation(annotation, animated = false)
        select(annotation)
    }

    override fun mapView(
        mapView: MKMapView,
        annotationView: MKAnnotationView,
        didChangeDragState: MKAnnotationViewDragState,
        fromOldState: MKAnnotationViewDragState,
    ) {
        if (didChangeDragState == MKAnnotationViewDragStateEnding) {
            annotationView.annotation?.coordinate?.let { onDragEnd?.invoke(it.toGeoPos()) }
        }
    }

    /** Conecta el gesto de toque libre al mapa. */
    fun attachTap(map: MKMapView) {
        map.addGestureRecognizer(UITapGestureRecognizer(target = this, action = NSSelectorFromString("handleTap:")))
    }

    @ObjCAction
    fun handleTap(recognizer: UITapGestureRecognizer) {
        val map = recognizer.view as? MKMapView ?: return
        val tap = onTap ?: return
        if (recognizer.state != UIGestureRecognizerStateEnded) return
        val point: CValue<CGPoint> = recognizer.locationInView(map)
        // Un toque sobre un marcador lo gestiona didSelectAnnotationView, no el mapa.
        var hit: UIView? = map.hitTest(point, withEvent = null)
        while (hit != null && hit != map) {
            if (hit is MKAnnotationView) return
            hit = hit.superview
        }
        tap(map.convertPoint(point, toCoordinateFromView = map).toGeoPos())
    }
}

internal val CARACAS = GeoPos(CARACAS_LAT, CARACAS_LON)
