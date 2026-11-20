package com.soyvinko.app.data

import com.soyvinko.app.ui.common.GeoPos
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt

data class MapboxRoute(
    val points: List<GeoPos>,
    val distanceKm: Double,
    val durationMinutes: Int,
)

/** Servicio de cálculo de rutas reales por carretera utilizando la API de Mapbox Directions v5. */
object MapboxRouting {
    const val DEFAULT_TOKEN = "pk.eyJ1IjoiZXhhbXBsZSIsImEiOiJjbHR3ZGo5NHowcGkyeG9uNGZwZXo5NTYifQ.dummy_token_placeholder"

    private val http by lazy {
        HttpClient(platformHttpEngine()) {
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 20_000
                requestTimeoutMillis = 20_000
            }
        }
    }

    /**
     * Calcula la mejor ruta por carretera entre origen y destino.
     * Devuelve los puntos geográficos exactos siguiendo las calles y curvas (no línea recta),
     * junto con la distancia real de conducción y la duración estimada.
     */
    suspend fun calculateRoute(
        origin: GeoPos,
        destination: GeoPos,
        token: String = DEFAULT_TOKEN,
    ): MapboxRoute? {
        return try {
            val url = "https://api.mapbox.com/directions/v5/mapbox/driving/${origin.lon},${origin.lat};${destination.lon},${destination.lat}?geometries=geojson&overview=full&access_token=$token"
            val resp = http.get(url)
            val body = resp.bodyAsText()
            val root = Json.parseToJsonElement(body).jsonObject
            val code = root["code"]?.jsonPrimitive?.content
            if (code != "Ok") return null

            val routes = root["routes"]?.jsonArray
            if (routes.isNullOrEmpty()) return null

            val route = routes[0].jsonObject
            val distanceMeters = route["distance"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val durationSeconds = route["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0

            val geometry = route["geometry"]?.jsonObject
            val coordinates = geometry?.get("coordinates")?.jsonArray ?: JsonArray(emptyList())

            val points = coordinates.mapNotNull { element ->
                val arr = element.jsonArray
                if (arr.size >= 2) {
                    val lon = arr[0].jsonPrimitive.doubleOrNull ?: return@mapNotNull null
                    val lat = arr[1].jsonPrimitive.doubleOrNull ?: return@mapNotNull null
                    GeoPos(lat, lon)
                } else null
            }

            val distanceKm = ((distanceMeters / 1000.0) * 10).roundToInt() / 10.0
            val durationMin = (durationSeconds / 60.0).roundToInt()

            MapboxRoute(
                points = points,
                distanceKm = distanceKm,
                durationMinutes = durationMin,
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resuelve las coordenadas a un nombre de calle / lugar legible en español,
     * evitando mostrar números de latitud y longitud.
     */
    suspend fun reverseGeocode(
        pos: GeoPos,
        token: String = DEFAULT_TOKEN,
    ): String? {
        return try {
            val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/${pos.lon},${pos.lat}.json?language=es&access_token=$token"
            val resp = http.get(url)
            val body = resp.bodyAsText()
            val root = Json.parseToJsonElement(body).jsonObject
            val features = root["features"]?.jsonArray
            if (features.isNullOrEmpty()) return null
            val first = features[0].jsonObject
            val placeName = first["place_name"]?.jsonPrimitive?.content
                ?: first["text"]?.jsonPrimitive?.content
            val parts = placeName?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            if (parts != null && parts.size >= 2) {
                "${parts[0]}, ${parts[1]}"
            } else {
                placeName
            }
        } catch (_: Exception) {
            null
        }
    }
}
