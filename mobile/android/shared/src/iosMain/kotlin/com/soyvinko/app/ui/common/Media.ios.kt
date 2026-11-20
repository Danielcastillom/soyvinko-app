package com.soyvinko.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.platform.toByteArray
import com.soyvinko.app.platform.toNSData
import com.soyvinko.app.platform.topViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraDevice
import platform.UIKit.UIImagePNGRepresentation
import platform.PDFKit.PDFDocument
import platform.PDFKit.kPDFDisplayBoxMediaBox
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_group_create
import platform.darwin.dispatch_group_enter
import platform.darwin.dispatch_group_leave
import platform.darwin.dispatch_group_notify

/**
 * Redibuja la imagen derecha (aplica la orientación de la cámara) y como mucho de `maxSide` px.
 * Así las fotos salen bien en la web y en Skia, que no lee la orientación EXIF.
 */
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.normalized(maxSide: Int): UIImage {
    val (w, h) = size.useContents { width to height }
    if (w <= 0.0 || h <= 0.0) return this
    val scale = minOf(1.0, maxSide / maxOf(w, h))
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { setScale(1.0) }
    return UIGraphicsImageRenderer(size = CGSizeMake(w * scale, h * scale), format = format).imageWithActions { _ ->
        drawInRect(CGRectMake(0.0, 0.0, w * scale, h * scale))
    }
}

/** Convierte cualquier foto (HEIC, PNG…) a JPEG derecho, que es lo que acepta el servidor. */
private fun jpegPart(image: UIImage, field: String = "file", name: String = "foto.jpg"): UploadPart? =
    UIImageJPEGRepresentation(image.normalized(4096), 0.9)?.let { UploadPart(field, name, "image/jpeg", it.toByteArray()) }

private class PhotoPickerDelegate(private val onPicked: (List<UploadPart>) -> Unit) :
    NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)
        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        if (results.isEmpty()) return
        val parts = arrayOfNulls<UploadPart>(results.size)
        val group = dispatch_group_create()
        results.forEachIndexed { i, result ->
            dispatch_group_enter(group)
            // Se piden los datos de imagen (no un archivo): funciona con fotos de iCloud y HEIC.
            result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data: NSData?, _ ->
                parts[i] = data?.let { UIImage.imageWithData(it) }?.let { jpegPart(it, name = "foto-${i + 1}.jpg") }
                dispatch_group_leave(group)
            }
        }
        dispatch_group_notify(group, dispatch_get_main_queue()) {
            val picked = parts.filterNotNull()
            if (picked.isNotEmpty()) onPicked(picked)
        }
    }
}

/** Selector de fotos del sistema (PHPicker, sin permisos). Devuelve hasta `max` imágenes ya leídas. */
@Composable
actual fun rememberPhotoPicker(max: Int, onPicked: (List<UploadPart>) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onPicked)
    val delegate = remember { PhotoPickerDelegate { callback.value(it) } }
    return {
        val config = PHPickerConfiguration().apply {
            setFilter(PHPickerFilter.imagesFilter)
            setSelectionLimit(max.coerceAtLeast(1).toLong())
        }
        val picker = PHPickerViewController(configuration = config)
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

private class CameraDelegate(private val onCaptured: (UploadPart) -> Unit) :
    NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage ?: return
        jpegPart(image, name = "captura.jpg")?.let(onCaptured)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
    }
}

/** Foto con la cámara (KYC, entregas, chat). Pide el permiso de cámara si falta. */
@Composable
actual fun rememberCameraCapture(onDenied: (String) -> Unit, front: Boolean, onCaptured: (UploadPart) -> Unit): () -> Unit {
    val captured = rememberUpdatedState(onCaptured)
    val denied = rememberUpdatedState(onDenied)
    val delegate = remember { CameraDelegate { captured.value(it) } }
    val noPermission = "Permite el acceso a la cámara en Ajustes > SoyVinko para tomar la foto."
    fun present() {
        val camera = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        if (!UIImagePickerController.isSourceTypeAvailable(camera)) {
            denied.value("Este dispositivo no tiene cámara disponible.")
            return
        }
        val picker = UIImagePickerController().apply {
            sourceType = camera
            this.delegate = delegate
            val frontCamera = UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceFront
            if (front && UIImagePickerController.isCameraDeviceAvailable(frontCamera)) cameraDevice = frontCamera
        }
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
    return {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> present()
            AVAuthorizationStatusNotDetermined -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                dispatch_async(dispatch_get_main_queue()) { if (granted) present() else denied.value(noPermission) }
            }
            else -> denied.value(noPermission)
        }
    }
}

/**
 * Reduce la foto antes de subirla: lado mayor <= maxSide y JPEG <= maxBytes. Al redibujar se aplica
 * la orientación de la cámara, así la imagen sale derecha en la web.
 */
actual fun prepareImage(part: UploadPart, maxSide: Int, maxBytes: Int, force: Boolean): UploadPart {
    if (!part.mime.startsWith("image/") || part.mime == "image/gif") return part
    val source = UIImage.imageWithData(part.bytes.toNSData()) ?: return part
    val image = source.normalized(maxSide)
    var quality = 0.88
    var data = UIImageJPEGRepresentation(image, quality) ?: return part
    while (data.length.toInt() > maxBytes && quality > 0.4) {
        quality -= 0.12
        data = UIImageJPEGRepresentation(image, quality) ?: break
    }
    val name = part.fileName.substringBeforeLast('.', part.fileName) + ".jpg"
    return UploadPart(part.field, name, "image/jpeg", data.toByteArray())
}

actual fun decodeImageBitmap(bytes: ByteArray, maxSide: Int): ImageBitmap? = try {
    val image = UIImage.imageWithData(bytes.toNSData())?.normalized(maxSide)
    val png = image?.let { UIImagePNGRepresentation(it) }
    png?.let { org.jetbrains.skia.Image.makeFromEncoded(it.toByteArray()).toComposeImageBitmap() }
} catch (e: Exception) {
    null
}

/** Primeras páginas de un PDF con PDFKit. */
@OptIn(ExperimentalForeignApi::class)
actual fun renderPdfPages(bytes: ByteArray, maxPages: Int, width: Int): List<ImageBitmap> {
    val document = PDFDocument(data = bytes.toNSData())
    val count = minOf(document.pageCount.toInt(), maxPages)
    return (0 until count).mapNotNull { i ->
        val page = document.pageAtIndex(i.toULong()) ?: return@mapNotNull null
        val (pw, ph) = page.boundsForBox(kPDFDisplayBoxMediaBox).useContents { size.width to size.height }
        if (pw <= 0.0 || ph <= 0.0) return@mapNotNull null
        val target = CGSizeMake(width.toDouble(), width * ph / pw)
        val thumb = page.thumbnailOfSize(target, forBox = kPDFDisplayBoxMediaBox)
        UIImageJPEGRepresentation(thumb, 0.9)?.let {
            runCatching { org.jetbrains.skia.Image.makeFromEncoded(it.toByteArray()).toComposeImageBitmap() }.getOrNull()
        }
    }
}
