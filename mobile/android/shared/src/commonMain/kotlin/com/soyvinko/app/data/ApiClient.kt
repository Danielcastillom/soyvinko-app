package com.soyvinko.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlin.concurrent.Volatile

val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    coerceInputValues = true
}

/** Motor HTTP de cada plataforma: OkHttp en Android, NSURLSession (Darwin) en iOS. */
expect fun platformHttpEngine(): HttpClientEngine

/**
 * Cliente de la API v1 (Android e iOS). Toda la lógica y los permisos viven en el servidor
 * (y en la base de datos detrás de él); la app nunca se conecta a PostgreSQL directamente.
 */
class ApiClient(
    baseUrl: String,
    private val storage: TokenStorage,
    private val userAgent: String,
    engine: HttpClientEngine = platformHttpEngine(),
) {
    private val base = Url(baseUrl.trimEnd('/'))
    private val refreshLock = Mutex()

    private val http = HttpClient(engine) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
            requestTimeoutMillis = 60_000
        }
    }

    @Volatile var accessToken: String? = null

    /** Se invoca cuando el servidor rechaza la sesión (revocada o vencida). */
    var onSessionExpired: (() -> Unit)? = null

    /**
     * Cliente para cargar imágenes privadas (/api/v1/files/{id}): añade la sesión solo a las
     * peticiones hacia el dominio de la API.
     */
    val imageHttp: HttpClient by lazy {
        HttpClient(platformHttpEngine()) {
            install(
                createClientPlugin("SoyVinkoAuth") {
                    onRequest { request, _ ->
                        val token = accessToken
                        if (token != null && request.url.host == base.host) request.headers.append(HttpHeaders.Authorization, "Bearer $token")
                    }
                },
            )
        }
    }

    suspend fun <T> get(path: String, serializer: KSerializer<T>, auth: Boolean = true): T =
        decode(call("GET", path, null, auth), serializer)

    suspend fun <B, T> post(
        path: String,
        body: B?,
        bodySerializer: KSerializer<B>?,
        serializer: KSerializer<T>,
        auth: Boolean = true,
    ): T {
        val raw = if (body != null && bodySerializer != null) ApiJson.encodeToString(bodySerializer, body) else "{}"
        return decode(call("POST", path, raw, auth), serializer)
    }

    private fun <T> decode(data: JsonElement, serializer: KSerializer<T>): T =
        try {
            ApiJson.decodeFromJsonElement(serializer, data)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Recibimos una respuesta inesperada del servidor.", "bad_response", 200)
        }

    /** Petición genérica: devuelve el campo `data` de la respuesta. */
    suspend fun raw(
        method: String,
        path: String,
        body: JsonElement? = null,
        auth: Boolean = true,
        query: Map<String, String> = emptyMap(),
    ): JsonElement = callWith(path, auth, method, query = query) {
        if (method == "GET") null else TextContent(body?.toString() ?: "{}", ContentType.Application.Json)
    }

    /** Subida multipart (archivos de solicitudes, entregas, chat, KYC). */
    suspend fun upload(
        path: String,
        files: List<UploadPart>,
        fields: Map<String, String> = emptyMap(),
    ): JsonElement = callWith(path, auth = true) {
        MultiPartFormDataContent(
            formData {
                fields.forEach { (k, v) -> append(k, v) }
                files.forEach { f ->
                    append(
                        f.field,
                        f.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, f.mime)
                            append(HttpHeaders.ContentDisposition, "filename=\"${f.fileName.replace("\"", "")}\"")
                        },
                    )
                }
            },
        )
    }

    fun absoluteUrl(path: String): String =
        if (path.startsWith("http")) path else URLBuilder(base).apply { encodedPath = path.substringBefore('?') }.buildString()

    private suspend fun call(method: String, path: String, body: String?, auth: Boolean): JsonElement =
        callWith(path, auth, method) { if (method == "GET") null else TextContent(body ?: "{}", ContentType.Application.Json) }

    private suspend fun callWith(
        path: String,
        auth: Boolean,
        method: String = "POST",
        retry: Boolean = true,
        query: Map<String, String> = emptyMap(),
        body: () -> OutgoingContent?,
    ): JsonElement {
        val sentToken = accessToken
        try {
            return execute(method, path, body(), if (auth) sentToken else null, query)
        } catch (e: ApiException) {
            if (!(auth && retry && e.isUnauthorized && storage.refreshToken != null)) throw e
            refreshSession(sentToken)
            return callWith(path, auth, method, retry = false, query = query, body = body)
        }
    }

    /** Renueva el access token una sola vez aunque varias peticiones fallen a la vez. */
    suspend fun refreshSession(staleToken: String? = accessToken): AuthPayload = refreshLock.withLock {
        val current = accessToken
        if (current != null && current != staleToken) return AuthPayload(accessToken = current)
        val refresh = storage.refreshToken
            ?: throw ApiException("Tu sesión terminó. Inicia sesión de nuevo.", "session_expired", 401)
        try {
            val data = execute(
                "POST",
                "/api/v1/auth/refresh",
                TextContent(ApiJson.encodeToString(RefreshRequest.serializer(), RefreshRequest(refresh)), ContentType.Application.Json),
                null,
            )
            val payload = decode(data, AuthPayload.serializer())
            accessToken = payload.accessToken
            payload.refreshToken?.let { storage.refreshToken = it }
            payload.user?.let { storage.cachedUserJson = ApiJson.encodeToString(User.serializer(), it) }
            payload
        } catch (e: ApiException) {
            if (e.isUnauthorized || e.status == 403) {
                accessToken = null
                storage.clear()
                onSessionExpired?.invoke()
            }
            throw e
        }
    }

    private suspend fun execute(
        method: String,
        path: String,
        body: OutgoingContent?,
        token: String?,
        query: Map<String, String> = emptyMap(),
    ): JsonElement {
        val response = try {
            http.request {
                buildUrl(path, query)
                this.method = HttpMethod.parse(method)
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, userAgent)
                if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
                if (method != "GET") setBody(body ?: TextContent("{}", ContentType.Application.Json))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("No hay conexión con SoyVinko. Revisa tu internet e inténtalo de nuevo.", "network", 0)
        }
        val text = try {
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ""
        }
        val json = runCatching { ApiJson.parseToJsonElement(text) }.getOrNull()
        if (response.status.value !in 200..299) throw errorFrom(response.status.value, json)
        return (json as? JsonObject)?.get("data") ?: JsonNull
    }

    /** Consulta escrita en la ruta (sin codificar) y, aparte, parámetros en mapa: se codifican una sola vez. */
    private fun HttpRequestBuilder.buildUrl(path: String, query: Map<String, String>) {
        url {
            protocol = base.protocol
            host = base.host
            port = base.port
            encodedPath = path.substringBefore('?')
            path.substringAfter('?', "").split('&').filter { it.contains('=') }.forEach {
                parameters.append(it.substringBefore('='), it.substringAfter('='))
            }
            query.forEach { (k, v) -> parameters.append(k, v) }
        }
    }

    private fun errorFrom(status: Int, json: JsonElement?): ApiException {
        val envelope = json?.let { runCatching { ApiJson.decodeFromJsonElement(ErrorEnvelope.serializer(), it) }.getOrNull() }
        val error = envelope?.error
        val fallback = when {
            status == 429 -> "Demasiados intentos. Espera un momento antes de volver a intentar."
            status >= 500 -> "El servicio no está disponible en este momento. Inténtalo en unos minutos."
            else -> envelope?.detail ?: "No se pudo completar la operación."
        }
        return ApiException(
            message = error?.message?.takeIf { status < 500 || error.code != "internal_error" } ?: fallback,
            code = error?.code ?: if (status >= 500) "service_unavailable" else "error",
            status = status,
            fieldErrors = error?.details.orEmpty().associate { d -> d.field.removePrefix("body.") to d.message },
        )
    }
}

class UploadPart(val field: String, val fileName: String, val mime: String, val bytes: ByteArray) {
    fun copy(field: String = this.field, fileName: String = this.fileName, mime: String = this.mime, bytes: ByteArray = this.bytes) =
        UploadPart(field, fileName, mime, bytes)
}
