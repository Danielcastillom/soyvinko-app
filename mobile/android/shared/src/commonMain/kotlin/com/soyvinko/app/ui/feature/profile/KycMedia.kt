package com.soyvinko.app.ui.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.common.decodeImageBitmap
import com.soyvinko.app.ui.common.rememberCameraCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Foto con la cámara para la verificación (`front` sugiere la cámara frontal para la selfie). */
@Composable
fun rememberKycCamera(front: Boolean, onError: (String) -> Unit, onCaptured: (UploadPart) -> Unit): () -> Unit =
    rememberCameraCapture(onDenied = onError, front = front) { part ->
        onCaptured(part.copy(fileName = if (front) "kyc_selfie.jpg" else "kyc_documento.jpg"))
    }

/**
 * Prepara una foto para subirla: corrige la orientación, limita el lado mayor (las fotos
 * de cámara pueden superar el límite de 10 MB del servidor) y la guarda como JPEG.
 */
suspend fun prepareImage(part: UploadPart, field: String, fileName: String, maxSide: Int = 2200): UploadPart =
    withContext(Dispatchers.Default) {
        if (part.mime == "application/pdf") return@withContext part.copy(field = field)
        val ready = com.soyvinko.app.ui.common.prepareImage(part, maxSide = maxSide, maxBytes = 9_000_000, force = true)
        if (ready.mime == "image/jpeg") ready.copy(field = field, fileName = fileName) else part.copy(field = field, fileName = fileName)
    }

/** Miniatura para mostrar la foto tomada. */
suspend fun previewOf(bytes: ByteArray, maxSide: Int = 900): ImageBitmap? =
    withContext(Dispatchers.Default) { decodeImageBitmap(bytes, maxSide) }
