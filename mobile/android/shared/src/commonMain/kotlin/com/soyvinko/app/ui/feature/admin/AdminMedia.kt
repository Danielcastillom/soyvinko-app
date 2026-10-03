package com.soyvinko.app.ui.feature.admin

import com.soyvinko.app.ui.common.PlatformBackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.AppServices
import com.soyvinko.app.ui.common.decodeImageBitmap
import com.soyvinko.app.ui.common.renderPdfPages
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Archivo consultado por moderación: páginas ya decodificadas (una para imágenes, varias para PDF). */
class ModeratedFile(val mime: String, val pages: List<ImageBitmap>, val bytes: Int) {
    val isPdf: Boolean get() = mime == "application/pdf"
}

const val KYC_DOC_REASON = "Consulta de documento del expediente KYC."
const val FILE_REVIEW_REASON = "Revisión de archivo en cola de moderación."

/**
 * Igual que downloadModeration() de la web: POST /api/v1/files/moderation/{id} {reason} devuelve el
 * contenido en base64 y su tipo. Se decodifica en el dispositivo (imagen o páginas de PDF), sin navegador.
 */
@OptIn(ExperimentalEncodingApi::class)
suspend fun AppServices.fetchModeratedFile(fileId: String, reason: String): ModeratedFile {
    val data = api.raw("POST", "/api/v1/files/moderation/$fileId", buildJsonObject { put("reason", reason) }).obj()
    val content = data.s("content") ?: throw ApiException("El archivo ya no está disponible.", "file_unavailable", 404)
    val mime = data.s("mime") ?: "image/jpeg"
    return withContext(Dispatchers.Default) {
        val bytes = try {
            Base64.Mime.decode(content)
        } catch (e: IllegalArgumentException) {
            throw ApiException("No pudimos leer el archivo recibido.", "bad_file", 200)
        }
        val pages = if (mime == "application/pdf") renderPdfPages(bytes) else listOfNotNull(decodeImage(bytes))
        if (pages.isEmpty()) throw ApiException("No pudimos mostrar este archivo en el dispositivo.", "bad_file", 200)
        ModeratedFile(mime, pages, bytes.size)
    }
}

/** Decodifica una imagen reduciéndola si es muy grande (lado mayor ≤ 2048 px). */
fun decodeImage(bytes: ByteArray, maxSide: Int = 2048): ImageBitmap? = decodeImageBitmap(bytes, maxSide)

/** Vista previa de un archivo moderado dentro de una tarjeta; al tocarla se abre a pantalla completa. */
@Composable
fun ModeratedPreview(file: ModeratedFile, onOpen: () -> Unit, maxHeight: Int = 260) {
    val first = file.pages.first()
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Vinko.Tint)
            .clickable(onClick = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        Image(first, contentDescription = "Documento", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
        Row(
            Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.ZoomOutMap, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                if (file.isPdf) "PDF · ${file.pages.size} pág." else "Ampliar",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
    }
}

/** Visor a pantalla completa dentro de la misma ventana, con zoom por pellizco. */
@Composable
fun ModeratedViewer(title: String, file: ModeratedFile, onClose: () -> Unit) {
    PlatformBackHandler(onBack = onClose)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
    ) {
        if (file.pages.size == 1) {
            Image(
                file.pages.first(),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
            )
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 72.dp, bottom = 24.dp, start = 12.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                file.pages.forEachIndexed { i, page ->
                    Text("Página ${i + 1}", style = MaterialTheme.typography.labelMedium, color = Vinko.Lilac)
                    Image(page, contentDescription = "Página ${i + 1}", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onClose, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}
