package com.soyvinko.app.ui.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.arr
import com.soyvinko.app.ui.common.d
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.s
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

const val PARISH_COLOR = 0xFF6F3DFF.toInt()

/**
 * Mapa de una parroquia: dibuja su límite (como la web), encuadra la zona y permite
 * tocar o arrastrar el marcador para elegir el punto exacto.
 */
@Composable
fun ParishMap(area: JsonObject, point: GeoPos?, onPick: (GeoPos) -> Unit, modifier: Modifier = Modifier) {
    val code = area.s("code")
    val rings = remember(code) { parseRings(area["geometry"]) }
    val bbox = remember(code) { area["bbox"].arr().mapNotNull { (it as? JsonPrimitive)?.doubleOrNull } }
    val center = GeoPos(area.d("latitude") ?: 10.4806, area.d("longitude") ?: -66.9036)
    ParishMapView(code, rings, bbox, center, point, onPick, modifier)
}

/** `rings`: polígonos (anillo exterior + huecos). `bbox`: [oeste, sur, este, norte] como en GeoJSON. */
@Composable
expect fun ParishMapView(
    code: String?,
    rings: List<List<List<GeoPos>>>,
    bbox: List<Double>,
    center: GeoPos,
    point: GeoPos?,
    onPick: (GeoPos) -> Unit,
    modifier: Modifier,
)

/** GeoJSON Polygon / MultiPolygon → polígonos (anillo exterior + huecos). */
private fun parseRings(geometry: JsonElement?): List<List<List<GeoPos>>> {
    val g = geometry.obj() ?: return emptyList()
    val coords = g["coordinates"].arr()
    val polygons: List<List<JsonElement>> = when (g.s("type")) {
        "Polygon" -> listOf(coords)
        "MultiPolygon" -> coords.map { it.arr() }
        else -> emptyList()
    }
    return polygons.map { polygon ->
        polygon.map { ring ->
            ring.arr().mapNotNull { pt ->
                val xy = pt.arr()
                val lon = (xy.getOrNull(0) as? JsonPrimitive)?.doubleOrNull
                val lat = (xy.getOrNull(1) as? JsonPrimitive)?.doubleOrNull
                if (lon != null && lat != null) GeoPos(lat, lon) else null
            }
        }.filter { it.size >= 3 }
    }.filter { it.isNotEmpty() }
}
