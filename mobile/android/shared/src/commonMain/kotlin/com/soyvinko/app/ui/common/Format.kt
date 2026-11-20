package com.soyvinko.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

/** Zona horaria de Venezuela: todas las fechas se muestran en hora de Caracas. */
val Caracas: TimeZone = TimeZone.of("America/Caracas")

private val MONTHS = listOf("ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sept.", "oct.", "nov.", "dic.")
private val MONTHS_LONG = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
private val WEEKDAYS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")

/**
 * Número con formato es-VE: separador de miles "." y decimales ",". `grouping = false` equivale a
 * String.format("%.1f") (sin miles); `trim` quita ceros finales de los decimales (NumberFormat).
 */
fun formatNumber(value: Double, decimals: Int = 2, grouping: Boolean = true, trim: Boolean = false): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = kotlin.math.round(abs(value) * factor).toLong()
    val negative = value < 0 && scaled != 0L
    val integer = scaled / factor
    var fraction = if (decimals > 0) (scaled % factor).toString().padStart(decimals, '0') else ""
    if (trim) fraction = fraction.trimEnd('0')
    val whole = if (grouping) groupThousands(integer) else integer.toString()
    val text = if (fraction.isNotEmpty()) "$whole,$fraction" else whole
    return if (negative) "-$text" else text
}

/** Entero con separador de miles es-VE (NumberFormat.getIntegerInstance). */
fun formatInt(value: Long): String = if (value < 0) "-" + groupThousands(-value) else groupThousands(value)

private fun groupThousands(n: Long): String = n.toString().reversed().chunked(3).joinToString(".").reversed()

/** "4,5" como String.format(es-VE, "%.1f"). */
fun oneDecimal(value: Double): String = formatNumber(value, 1, grouping = false)

fun Double.toRadians(): Double = this * kotlin.math.PI / 180.0

private val ACCENTS = mapOf(
    'á' to 'a', 'à' to 'a', 'ä' to 'a', 'â' to 'a', 'ã' to 'a', 'é' to 'e', 'è' to 'e', 'ë' to 'e', 'ê' to 'e',
    'í' to 'i', 'ì' to 'i', 'ï' to 'i', 'î' to 'i', 'ó' to 'o', 'ò' to 'o', 'ö' to 'o', 'ô' to 'o', 'õ' to 'o',
    'ú' to 'u', 'ù' to 'u', 'ü' to 'u', 'û' to 'u', 'ñ' to 'n', 'ç' to 'c',
)

/** Minúsculas sin acentos, para buscar ("Electricista" encuentra "electricísta"). */
fun foldAccents(text: String): String = text.lowercase().map { ACCENTS[it] ?: it }.joinToString("")

/** Importes en unidades menores, igual que la web: money(450000, "VES") = "Bs 4.500,00". */
fun money(minor: Long?, currency: String? = "VES"): String {
    if (minor == null) return "A convenir"
    val symbol = when (currency) { "VES" -> "Bs"; "USD" -> "US$"; null -> ""; else -> currency }
    return "$symbol ${formatNumber(minor / 100.0)}".trim()
}

/** Fecha ISO-8601 de la API ("2026-10-02T22:15:00+00:00") a instante; null si no es válida. */
fun parseInstant(iso: String?): Instant? = iso?.let { runCatching { Instant.parse(it) }.getOrNull() }

fun nowInstant(): Instant = Clock.System.now()

/** Hora local de Caracas de una fecha ISO. */
fun caracas(iso: String?): LocalDateTime? = parseInstant(iso)?.toLocalDateTime(Caracas)

/** Minutos transcurridos desde una fecha ISO (negativos si es futura). */
fun minutesSince(iso: String?): Long? = parseInstant(iso)?.let { (nowInstant() - it).inWholeMinutes }

fun hour12(t: LocalDateTime): String {
    val h = t.hour % 12
    return "${if (h == 0) 12 else h}:${t.minute.toString().padStart(2, '0')} ${if (t.hour < 12) "a. m." else "p. m."}"
}

fun date(iso: String?): String = caracas(iso)?.let { "${it.dayOfMonth} ${MONTHS[it.monthNumber - 1]} ${it.year}" } ?: ""
fun dateTime(iso: String?): String = caracas(iso)?.let { "${it.dayOfMonth} ${MONTHS[it.monthNumber - 1]}, ${hour12(it)}" } ?: ""
fun time(iso: String?): String = caracas(iso)?.let { hour12(it) } ?: ""

fun relative(iso: String?): String {
    val minutes = minutesSince(iso) ?: return ""
    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "Hace $minutes min"
        minutes < 60 * 24 -> "Hace ${minutes / 60} h"
        minutes < 60 * 24 * 7 -> "Hace ${minutes / 1440} ${if (minutes / 1440 == 1L) "día" else "días"}"
        else -> date(iso)
    }
}

// Etiquetas de estado de la web (frontend/src/components.tsx).
private val STATUS = mapOf(
    "active" to "Activo", "inactive" to "Inactivo", "paused" to "En pausa", "draft" to "Borrador",
    "pending" to "Pendiente", "approved" to "Aprobado", "rejected" to "Rechazado", "in_review" to "En revisión",
    "review" to "En revisión", "open" to "Publicada", "matched" to "Contratado", "accepted" to "Aceptado",
    "en_route" to "En camino", "arrived" to "En el lugar", "in_progress" to "En curso",
    "awaiting_confirmation" to "Por confirmar", "completed" to "Finalizado", "cancelled" to "Cancelado",
    "disputed" to "En disputa", "expired" to "Vencido", "withdrawn" to "Retirada", "published" to "Publicada",
    "hidden" to "Oculta", "funded" to "Fondos reservados", "unfunded" to "Sin financiar", "released" to "Liberado",
    "settled" to "Confirmada", "paid" to "Pagado", "processing" to "Procesando", "verified" to "Verificado",
    "disabled" to "Desactivado", "closed" to "Cerrado", "resolved" to "Resuelto", "delivered" to "Entregado",
    "dispatched" to "Enviado", "onsite" to "Presencial", "remote" to "Remoto", "guaranteed" to "Pago protegido",
    "external" to "Pago directo", "frozen" to "Congelado", "failed" to "Fallido", "submitted" to "Enviada",
    "changes_requested" to "Cambios pedidos",
)

fun statusLabel(value: String?): String = value?.let { STATUS[it] ?: it.replace('_', ' ').replaceFirstChar(Char::uppercase) } ?: ""

@Composable
fun StatusBadge(value: String?) {
    val (fg, bg) = when (value) {
        "completed", "approved", "verified", "paid", "settled", "released", "delivered", "active", "funded", "resolved" -> Vinko.Mint to Vinko.MintBg
        "cancelled", "rejected", "failed", "disputed", "expired", "frozen", "disabled" -> Vinko.Coral to Vinko.CoralBg
        "pending", "awaiting_confirmation", "in_review", "review", "processing", "draft", "changes_requested" -> Vinko.Amber to Vinko.AmberBg
        else -> Vinko.Primary to Vinko.Tint
    }
    Text(
        statusLabel(value),
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

// Lectura cómoda de JSON flexible de la API.
fun JsonElement?.obj(): JsonObject? = this as? JsonObject
fun JsonElement?.arr(): List<JsonElement> = (this as? JsonArray)?.toList().orEmpty()
fun JsonElement?.objects(): List<JsonObject> = arr().mapNotNull { it as? JsonObject }
fun JsonObject?.s(key: String): String? = (this?.get(key) as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull
fun JsonObject?.l(key: String): Long? = (this?.get(key) as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }
fun JsonObject?.d(key: String): Double? = (this?.get(key) as? JsonPrimitive)?.doubleOrNull
fun JsonObject?.b(key: String): Boolean = (this?.get(key) as? JsonPrimitive)?.booleanOrNull ?: false
fun JsonObject?.o(key: String): JsonObject? = this?.get(key) as? JsonObject
fun JsonObject?.list(key: String): List<JsonObject> = this?.get(key).objects()

val Transparent = Color.Transparent

/** Milisegundos desde 1970 (reemplaza System.currentTimeMillis en código común). */
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** Milisegundos de una fecha ISO del servidor; null si no se puede leer. */
fun epochMillis(iso: String?): Long? = parseInstant(iso)?.toEpochMilliseconds()

/** Fecha de hoy en Caracas. */
fun todayCaracas(): LocalDate = nowInstant().toLocalDateTime(Caracas).date

/** "3 oct. 2026" (como "d MMM yyyy" en es-VE). */
fun shortDate(date: LocalDate): String = "${date.dayOfMonth} ${MONTHS[date.monthNumber - 1]} ${date.year}"

/** "Sábado 3 de octubre" (como "EEEE d 'de' MMMM"). */
fun longDayMonth(date: LocalDate): String =
    "${WEEKDAYS[date.dayOfWeek.ordinal]} ${date.dayOfMonth} de ${MONTHS_LONG[date.monthNumber - 1]}".replaceFirstChar(Char::uppercase)

/** "3 oct., 4:05 p. m." (como "d MMM, h:mm a"). */
fun shortDateTime(t: LocalDateTime): String = "${t.dayOfMonth} ${MONTHS[t.monthNumber - 1]}, ${hour12(t)}"

/** ISO 8601 en UTC sin fracciones de segundo (como Instant.truncatedTo(SECONDS).toString()). */
fun Instant.isoSeconds(): String = Instant.fromEpochSeconds(epochSeconds).toString()
