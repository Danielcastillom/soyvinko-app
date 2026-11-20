package com.soyvinko.app.ui.common

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.soyvinko.app.platform.AndroidPlatform
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.media.ExifInterface
import com.soyvinko.app.data.UploadPart
import java.io.ByteArrayOutputStream
import java.io.File

fun readUpload(context: Context, uri: Uri, field: String = "file"): UploadPart {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "image/jpeg"
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    } ?: "foto.jpg"
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    return UploadPart(field, name, mime, bytes)
}

/** Selector de fotos del sistema (sin permisos). Devuelve hasta `max` imágenes ya leídas. */
@Composable
actual fun rememberPhotoPicker(max: Int, onPicked: (List<UploadPart>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val single = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(listOf(readUpload(context, uri)))
    }
    val multiple = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxOf(2, max))) { uris ->
        if (uris.isNotEmpty()) onPicked(uris.take(max).map { readUpload(context, it) })
    }
    val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    return { if (max > 1) multiple.launch(request) else single.launch(request) }
}

/**
 * Foto con la cámara (KYC, entregas, chat). Pide el permiso CAMERA si falta: como el manifiesto lo
 * declara, Android rechaza la cámara del sistema hasta que el usuario lo concede.
 */
@Composable
actual fun rememberCameraCapture(onDenied: (String) -> Unit, front: Boolean, onCaptured: (UploadPart) -> Unit): () -> Unit {
    val context = LocalContext.current
    val target = remember(front) {
        File(context.cacheDir, "camera").apply { mkdirs() }.let { File(it, if (front) "selfie.jpg" else "captura.jpg") }
    }
    val uri = remember(target) { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target) }
    val launcher = rememberLauncherForActivityResult(TakePictureFacing(front)) { ok ->
        if (ok && target.exists() && target.length() > 0) {
            onCaptured(UploadPart("file", "foto_${System.currentTimeMillis()}.jpg", "image/jpeg", target.readBytes()))
        }
    }
    val launch = {
        try {
            target.delete()
            launcher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            onDenied("No encontramos una aplicación de cámara en este teléfono.")
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launch() else onDenied("Permite el acceso a la cámara para tomar la foto.")
    }
    return {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launch()
        else permission.launch(Manifest.permission.CAMERA)
    }
}

/** Cámara del sistema con la cámara frontal sugerida (selfie). */
private class TakePictureFacing(private val front: Boolean) : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent = super.createIntent(context, input).apply {
        if (front) {
            putExtra("android.intent.extras.CAMERA_FACING", 1)
            putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        }
    }
}

/**
 * Reduce fotos grandes (lado mayor 2560 px, JPEG 85 %) y corrige la orientación EXIF.
 * Las fotos de cámara pueden superar el límite de 10 MB del servidor. Otros tipos no se tocan.
 */
actual fun prepareImage(part: UploadPart, maxSide: Int, maxBytes: Int, force: Boolean): UploadPart {
    if (!part.mime.startsWith("image/") || part.mime == "image/gif") return part
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(part.bytes, 0, part.bytes.size, bounds)
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    if (largest <= 0 || (!force && largest <= maxSide && part.bytes.size <= maxBytes)) return part
    var sample = 1
    while (largest / (sample * 2) >= maxSide) sample *= 2
    val decoded = BitmapFactory.decodeByteArray(part.bytes, 0, part.bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return part
    val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
    val rotation = runCatching {
        when (ExifInterface(part.bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }.getOrDefault(0f)
    val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
    val bitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    return part.copy(fileName = part.fileName.substringBeforeLast('.') + ".jpg", mime = "image/jpeg", bytes = out.toByteArray())
}

actual fun decodeImageBitmap(bytes: ByteArray, maxSide: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    if (largest <= 0) return@runCatching null
    var sample = 1
    while (largest / (sample * 2) >= maxSide) sample *= 2
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}.getOrNull()

/** Primeras páginas de un PDF como imágenes, con el renderizador nativo de Android. */
actual fun renderPdfPages(bytes: ByteArray, maxPages: Int, width: Int): List<ImageBitmap> {
    val dir = File(AndroidPlatform.context.cacheDir, "admin").apply { mkdirs() }
    val file = File(dir, "documento_${System.nanoTime()}.pdf")
    return try {
        file.writeBytes(bytes)
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        try {
            (0 until minOf(renderer.pageCount, maxPages)).map { i ->
                val page = renderer.openPage(i)
                try {
                    val height = (width.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap.asImageBitmap()
                } finally {
                    page.close()
                }
            }
        } finally {
            renderer.close()
            fd.close()
        }
    } catch (e: Exception) {
        emptyList()
    } finally {
        file.delete()
    }
}
