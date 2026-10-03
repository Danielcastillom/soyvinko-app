package com.soyvinko.app.ui.common

import coil3.ComponentRegistry
import coil3.request.ImageRequest
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.soyvinko.app.data.AppServices
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** ImageLoader que envía la sesión: necesario para /api/v1/files/{id} (archivos privados). */
@Composable
fun rememberApiImageLoader(): ImageLoader {
    val app = LocalApp.current
    val context = LocalPlatformContext.current
    return remember(app.api) { sharedLoader(context, app) }
}

// Un solo ImageLoader por sesión de API: las imágenes quedan en caché entre pantallas.
private var cachedLoader: Pair<Any, ImageLoader>? = null

private fun sharedLoader(context: PlatformContext, app: AppServices): ImageLoader =
    cachedLoader?.takeIf { it.first === app.api }?.second
        ?: buildApiImageLoader(context, app, crossfade = true).also { cachedLoader = app.api to it }

/** ImageLoader con la sesión de la API (descarga con Ktor y añade el token solo para el servidor propio). */
fun buildApiImageLoader(context: PlatformContext, app: AppServices, crossfade: Boolean): ImageLoader =
    ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory(httpClient = { app.api.imageHttp }))
            addPlatformDecoders()
        }
        .crossfade(crossfade)
        .build()

/** Decodificadores propios de cada plataforma, con prioridad sobre los de Coil. */
expect fun ComponentRegistry.Builder.addPlatformDecoders(): ComponentRegistry.Builder

/** Imágenes en memoria normal (no "hardware" en Android), para poder dibujarlas en capturas y pruebas. */
expect fun ImageRequest.Builder.softwareBitmaps(): ImageRequest.Builder

/** Imagen privada por id de archivo, o pública por ruta ("/images/..."). */
@Composable
fun ApiImage(fileIdOrPath: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val app = LocalApp.current
    val url = when {
        fileIdOrPath == null -> null
        fileIdOrPath.startsWith("http") -> fileIdOrPath
        fileIdOrPath.startsWith("/") -> app.api.absoluteUrl(fileIdOrPath)
        else -> app.api.absoluteUrl("/api/v1/files/$fileIdOrPath")
    }
    // Marcador del color de la marca mientras carga o si la imagen no existe/falla.
    val placeholder = remember { ColorPainter(Vinko.Tint) }
    AsyncImage(
        model = url,
        imageLoader = rememberApiImageLoader(),
        contentDescription = null,
        contentScale = contentScale,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        modifier = modifier.background(Vinko.Tint),
    )
}

/** Selector de fotos del sistema (sin permisos). Devuelve hasta `max` imágenes ya leídas. */
@Composable
expect fun rememberPhotoPicker(max: Int = 1, onPicked: (List<UploadPart>) -> Unit): () -> Unit

/**
 * Foto con la cámara (KYC, entregas, chat). Pide el permiso de cámara si falta; `onDenied` recibe
 * el motivo si no se puede abrir. `front` sugiere la cámara frontal (selfie).
 */
@Composable
expect fun rememberCameraCapture(
    onDenied: (String) -> Unit = {},
    front: Boolean = false,
    onCaptured: (UploadPart) -> Unit,
): () -> Unit

/**
 * Reduce fotos grandes (lado mayor 2560 px, JPEG 85 %) y corrige la orientación.
 * Las fotos de cámara pueden superar el límite de 10 MB del servidor. Otros tipos no se tocan.
 */
expect fun prepareImage(part: UploadPart, maxSide: Int = 2560, maxBytes: Int = 2_500_000, force: Boolean = false): UploadPart

/** Decodifica una imagen (JPEG/PNG/HEIC) reducida a `maxSide` px y derecha; null si no es válida. */
expect fun decodeImageBitmap(bytes: ByteArray, maxSide: Int = 2048): ImageBitmap?

/** Primeras páginas de un PDF como imágenes (visor de moderación). Vacío si no se puede leer. */
expect fun renderPdfPages(bytes: ByteArray, maxPages: Int = 6, width: Int = 1240): List<ImageBitmap>

/**
 * Sube un archivo a /api/v1/files/upload y devuelve su id.
 * purpose: avatar, portfolio, request, chat, product, kyc, evidence.
 */
suspend fun AppServices.uploadFile(part: UploadPart, purpose: String): String {
    val data = api.upload("/api/v1/files/upload", listOf(prepareImage(part).copy(field = "file")), mapOf("purpose" to purpose))
    return data.jsonObject["id"]!!.jsonPrimitive.content
}
