package com.soyvinko.app.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Operaciones de la API v1 por nombre, igual que `api()` en la web (registro generado en
 * OperationsRegistry.kt). Los parámetros van sin el prefijo `p_`: op("money_wallet", "currency" to "VES").
 * Un valor `null` se omite (el servidor usa su valor por defecto). Para enviar un nulo explícito
 * en un argumento obligatorio que lo admite, pasa `JsonNull` ("price" to JsonNull).
 */
class Ops(private val api: ApiClient) {
    suspend fun call(name: String, vararg params: Pair<String, Any?>): JsonElement = call(name, params.toMap())

    suspend fun call(name: String, params: Map<String, Any?>): JsonElement {
        val (path, method) = OPERATIONS[name] ?: throw ApiException("Operación no disponible: $name", "unknown_operation", 400)
        val clean = params.filterValues { it != null }
        return if (method == "GET") {
            // Sin codificar aquí: el cliente HTTP codifica una vez (fechas con "+", textos con "&", etc.).
            api.raw("GET", path, query = clean.mapValues { (_, v) -> if (v is JsonElement) v.toString().trim('"') else v.toString() })
        } else {
            api.raw("POST", path, toJson(clean))
        }
    }

    suspend fun <T> get(name: String, serializer: KSerializer<T>, vararg params: Pair<String, Any?>): T =
        ApiJson.decodeFromJsonElement(serializer, call(name, params.toMap()))

    companion object {
        fun toJson(value: Any?): JsonElement = when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to toJson(v) })
            is Iterable<*> -> JsonArray(value.map { toJson(it) })
            is Array<*> -> JsonArray(value.map { toJson(it) })
            else -> JsonPrimitive(value.toString())
        }
    }
}
