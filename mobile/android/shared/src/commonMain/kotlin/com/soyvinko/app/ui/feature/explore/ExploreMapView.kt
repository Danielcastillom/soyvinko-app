package com.soyvinko.app.ui.feature.explore

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.soyvinko.app.ui.common.GeoPos
import kotlin.math.PI
import kotlin.math.cos

enum class PinKind { Service, Search, Me, Picked }

data class Pin(val key: String, val pos: GeoPos, val kind: PinKind, val title: String, val count: Int = 1)

/** Acceso imperativo al mapa para los botones "ver todos" y "centrar en mi ubicación". */
class MapHandle {
    var points: List<GeoPos> = emptyList()
    var fitAction: ((List<GeoPos>) -> Unit)? = null
    var centerAction: ((GeoPos, Double) -> Unit)? = null

    fun fit() {
        fitAction?.invoke(points)
    }

    fun center(pos: GeoPos, zoom: Double = 15.0) {
        centerAction?.invoke(pos, zoom)
    }
}

/**
 * Mapa de Explorar: marcadores de servicios tocables (con número si hay varios en el mismo punto),
 * tu ubicación, la zona de búsqueda con su radio y, opcionalmente, toque libre para elegir un punto.
 * OpenStreetMap en Android, Apple Maps en iOS.
 */
@Composable
expect fun ExploreMapView(
    pins: List<Pin>,
    modifier: Modifier = Modifier,
    circle: Pair<GeoPos, Int>? = null,
    fitKey: Any? = null,
    initialCenter: GeoPos? = null,
    initialZoom: Double = 12.0,
    handle: MapHandle? = null,
    onPinTap: (Pin) -> Unit = {},
    onMapTap: ((GeoPos) -> Unit)? = null,
)

/** Puntos norte, sur, este y oeste del radio, para que el encuadre muestre la zona completa. */
fun circleExtents(center: GeoPos, radius: Int): List<GeoPos> {
    val dLat = radius / 111_320.0
    val dLon = radius / (111_320.0 * cos(center.lat * PI / 180).coerceAtLeast(0.01))
    return listOf(
        GeoPos(center.lat + dLat, center.lon), GeoPos(center.lat - dLat, center.lon),
        GeoPos(center.lat, center.lon + dLon), GeoPos(center.lat, center.lon - dLon),
    )
}
