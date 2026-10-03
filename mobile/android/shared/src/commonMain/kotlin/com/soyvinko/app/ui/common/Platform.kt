package com.soyvinko.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import coil3.PlatformContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Abre una URL en otra app del teléfono (tel:, mailto:, WhatsApp, Maps, app autenticadora).
 * Nunca abre la web de SoyVinko: todas las secciones son nativas. Devuelve false si no hay app.
 */
expect fun openExternalUrl(context: PlatformContext, url: String): Boolean

fun dialPhone(context: PlatformContext, phone: String): Boolean =
    openExternalUrl(context, "tel:" + phone.filter { it.isDigit() || it == '+' })

fun sendEmail(context: PlatformContext, to: String, subject: String? = null): Boolean =
    openExternalUrl(context, "mailto:$to" + (subject?.let { "?subject=" + encodeUrlComponent(it) } ?: ""))

/** WhatsApp con el número en formato internacional de Venezuela (0414… -> 58414…). */
fun openWhatsApp(context: PlatformContext, phone: String, text: String? = null): Boolean {
    var digits = phone.filter { it.isDigit() }
    if (digits.startsWith("0")) digits = "58" + digits.drop(1)
    return openExternalUrl(context, "whatsapp://send?phone=$digits" + (text?.let { "&text=" + encodeUrlComponent(it) } ?: ""))
}

/** Codificación de componentes de URL (RFC 3986). */
fun encodeUrlComponent(value: String): String = buildString {
    value.encodeToByteArray().forEach { b ->
        val c = b.toInt().toChar()
        if (c.isLetterOrDigit() && c.code < 128 || c in "-_.~") append(c)
        else append("%" + (b.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
    }
}

/** Identificador único (claves de idempotencia). */
@OptIn(ExperimentalUuidApi::class)
fun newUuid(): String = Uuid.random().toString()

/** Botón Atrás del sistema (Android). En iOS no hay botón Atrás: no hace nada. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)

/** "Android" o "iOS". */
expect val platformName: String

/** Decodifica %XX (UTF-8), como Uri.decode: el "+" se mantiene. */
fun decodeUrlComponent(value: String): String {
    if ('%' !in value) return value
    val out = ArrayList<Byte>(value.length)
    var i = 0
    while (i < value.length) {
        val c = value[i]
        if (c == '%' && i + 2 < value.length && value.substring(i + 1, i + 3).toIntOrNull(16) != null) {
            out.add(value.substring(i + 1, i + 3).toInt(16).toByte())
            i += 3
        } else {
            c.toString().encodeToByteArray().forEach(out::add)
            i++
        }
    }
    return out.toByteArray().decodeToString()
}

/** Diálogo a pantalla completa (mapas, visor de fotos) que dibuja también bajo las barras del sistema. */
expect fun fullScreenDialogProperties(): DialogProperties

// Mismo patrón que android.util.Patterns.EMAIL_ADDRESS.
private val EMAIL = Regex(
    "[a-zA-Z0-9+._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+",
)

fun isValidEmail(value: String): Boolean = EMAIL.matches(value.trim())
