package com.soyvinko.app.ui.feature.explore

import com.soyvinko.app.ui.common.foldAccents
import com.soyvinko.app.ui.common.oneDecimal
import com.soyvinko.app.ui.common.nowInstant
import com.soyvinko.app.ui.common.parseInstant
import com.soyvinko.app.ui.common.toDecimalOrNull
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Carpenter
import androidx.compose.material.icons.outlined.CarRepair
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.Plumbing
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.common.decodeImageBitmap
import com.soyvinko.app.ui.common.prepareImage
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.d
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.s
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Servicio publicado por un profesional (`offerings` de `public_listing` / `professional_profile`). */
data class Offering(
    val id: String,
    val categoryId: String?,
    val title: String,
    val description: String?,
    val deliveryMode: String,
    val currency: String,
    val priceMin: Long?,
    val priceMax: Long?,
    val pricingType: String?,
)

/** Perfil público de un profesional, tal como lo devuelven discover, nearby_professionals y professional_profile. */
data class Pro(
    val id: String,
    val name: String,
    val bio: String?,
    val years: Long?,
    val verified: Boolean,
    val rating: Double?,
    val reviewCount: Long,
    val recommendations: Long,
    val city: String?,
    val distanceM: Long?,
    val sponsored: Boolean,
    val requiresPlatformPayment: Boolean,
    val offerings: List<Offering>,
    val branches: List<JsonObject>,
    val portfolio: List<JsonObject>,
) {
    val initials: String
        get() = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "P" }
}

fun JsonObject.toOffering(): Offering? {
    val id = s("id") ?: return null
    return Offering(
        id = id,
        categoryId = s("category_id"),
        title = s("title").orEmpty(),
        description = s("description"),
        deliveryMode = s("delivery_mode") ?: "onsite",
        currency = s("currency") ?: "VES",
        priceMin = l("price_min_minor"),
        priceMax = l("price_max_minor"),
        pricingType = s("pricing_type"),
    )
}

fun JsonObject.toPro(): Pro? {
    val id = s("id") ?: return null
    val branches = list("branches")
    return Pro(
        id = id,
        name = s("name") ?: "Profesional SoyVinko",
        bio = s("bio")?.takeIf { it.isNotBlank() },
        years = l("years_experience"),
        verified = b("verified"),
        rating = d("rating"),
        reviewCount = l("review_count") ?: 0,
        recommendations = l("recommendations") ?: 0,
        city = s("city") ?: branches.firstNotNullOfOrNull { it.s("city") },
        distanceM = l("distance_m"),
        sponsored = b("sponsored"),
        requiresPlatformPayment = b("requires_platform_payment"),
        offerings = list("offerings").mapNotNull { it.toOffering() },
        branches = branches,
        portfolio = list("portfolio"),
    )
}

fun JsonElement?.pros(): List<Pro> = objects().mapNotNull { it.toPro() }

data class Category(val id: String, val slug: String, val name: String)

fun JsonElement?.categories(): List<Category> =
    objects().mapNotNull { c -> c.s("id")?.let { Category(it, c.s("slug").orEmpty(), c.s("name").orEmpty()) } }

// Ilustraciones públicas de la web (frontend/src/serviceImages.ts).
private val SERVICE_IMAGES = setOf(
    "carpinteria", "cerrajeria", "cerrajeria-automotriz", "programacion", "electricidad", "limpieza", "pintura",
    "refrigeracion", "electrodomesticos", "traduccion", "plomeria", "informatica", "diseno-grafico",
)

/** Categorías destacadas de la web, en su orden. */
val FEATURED = listOf("plomeria", "electricidad", "informatica", "cerrajeria", "diseno-grafico", "programacion")

fun serviceImage(slug: String?): String? = slug?.takeIf { it in SERVICE_IMAGES }?.let { "/images/services/$it-v1.webp" }

/** Nombres cortos que usa la web en las tarjetas de categoría. */
fun Category.shortName(): String = when (name) {
    "Reparación y soporte de computadores" -> "Tecnología"
    "Refrigeración y aire acondicionado" -> "Refrigeración"
    "Reparación de electrodomésticos" -> "Electrodomésticos"
    else -> name
}

fun categoryIcon(slug: String?): ImageVector = when (slug) {
    "plomeria" -> Icons.Outlined.Plumbing
    "electricidad" -> Icons.Outlined.ElectricalServices
    "informatica" -> Icons.Outlined.Computer
    "cerrajeria" -> Icons.Outlined.Key
    "cerrajeria-automotriz" -> Icons.Outlined.CarRepair
    "diseno-grafico" -> Icons.Outlined.Brush
    "programacion" -> Icons.Outlined.Code
    "carpinteria" -> Icons.Outlined.Carpenter
    "limpieza" -> Icons.Outlined.CleaningServices
    "pintura" -> Icons.Outlined.FormatPaint
    "refrigeracion" -> Icons.Outlined.AcUnit
    "electrodomesticos" -> Icons.Outlined.LocalLaundryService
    "traduccion" -> Icons.Outlined.Translate
    else -> Icons.Outlined.Handyman
}

/** Destacadas primero (orden de la web) y luego el resto por nombre. */
fun List<Category>.ordered(): List<Category> =
    sortedWith(compareBy<Category> { FEATURED.indexOf(it.slug).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenBy { it.shortName() })

fun distanceLabel(meters: Long?): String? = when {
    meters == null -> null
    meters < 1000 -> "$meters m"
    else -> (oneDecimal(meters / 1000.0) + " km")
}

fun ratingLabel(rating: Double?): String = rating?.let { oneDecimal(it) } ?: "Nuevo"

fun reviewsLabel(count: Long): String = when (count) {
    0L -> "En SoyVinko"
    1L -> "(1 opinión)"
    else -> "($count opiniones)"
}

/** Precio de un servicio según su tipo de tarifa. */
fun Offering.priceLabel(): String = when {
    priceMin == null -> "A cotizar"
    pricingType == "hourly" -> "${money(priceMin, currency)} / hora"
    pricingType == "fixed" -> money(priceMin, currency)
    priceMax != null && priceMax > priceMin -> "${money(priceMin, currency)} – ${money(priceMax, currency)}"
    else -> "Desde ${money(priceMin, currency)}"
}

fun pricingCaption(type: String?): String = when (type) {
    "fixed" -> "Precio fijo"
    "hourly" -> "Por hora"
    "from" -> "Precio de referencia"
    else -> "Por cotizar"
}

fun currencyShort(currency: String?): String = if (currency == "USD") "USD" else "Bs"

/** Búsqueda sin distinguir mayúsculas ni acentos ("plomeria" encuentra "Plomería"). */
fun fold(text: String): String =
    foldAccents(text)

fun Pro.matches(query: String, categories: Map<String, Category>): Boolean {
    val q = fold(query.trim())
    if (q.isEmpty()) return true
    val haystack = buildString {
        append(name).append(' ').append(bio.orEmpty())
        offerings.forEach { o -> append(' ').append(o.title).append(' ').append(categories[o.categoryId]?.name.orEmpty()) }
    }
    return fold(haystack).contains(q)
}

/**
 * Importe escrito en unidades mayores con formato es-VE ("1.500,50", "1500,5", "35") a unidades menores.
 * Devuelve null si el texto no es un número válido.
 */
fun parseMajorToMinor(text: String): Long? {
    val t = text.trim().replace(" ", "").replace("Bs", "").replace("$", "")
    if (t.isEmpty()) return null
    val normalized = when {
        t.contains(',') -> t.replace(".", "").replace(',', '.')
        Regex("^\\d{1,3}(\\.\\d{3})+$").matches(t) -> t.replace(".", "")
        else -> t
    }
    if (!Regex("^\\d+(\\.\\d{1,2})?$").matches(normalized)) return null
    return normalized.toDecimalOrNull()?.scaledHalfUp(2)
}

fun isFuture(iso: String?): Boolean =
    parseInstant(iso)?.let { it > nowInstant() } ?: false

fun JsonObject.geo(latKey: String = "latitude", lonKey: String = "longitude"): GeoPos? {
    val lat = d(latKey) ?: return null
    val lon = d(lonKey) ?: return null
    return if (lat.isFinite() && lon.isFinite() && kotlin.math.abs(lat) <= 90 && kotlin.math.abs(lon) <= 180) GeoPos(lat, lon) else null
}

// Fotos de referencia: el servidor acepta JPG, PNG o WEBP de hasta 10 MB (web: RequestPhotos.tsx).
private val PHOTO_TYPES = setOf("image/jpeg", "image/png", "image/webp")
private const val MAX_PHOTO_BYTES = 10 * 1024 * 1024

/** Deja la foto lista para subir: si es HEIC u otro formato, o pesa demasiado, la convierte a JPG. */
fun normalizePhoto(part: UploadPart): UploadPart? {
    if (part.mime in PHOTO_TYPES && part.bytes.size in 1..MAX_PHOTO_BYTES) return part
    // Tipo desconocido: se intenta leer igual como imagen.
    val image = if (part.mime.startsWith("image/")) part else part.copy(mime = "image/unknown")
    val jpeg = prepareImage(image, maxSide = 2560, maxBytes = MAX_PHOTO_BYTES, force = true)
    if (jpeg.mime != "image/jpeg" || jpeg.bytes.isEmpty() || jpeg.bytes.size > MAX_PHOTO_BYTES) return null
    return jpeg.copy(fileName = part.fileName.substringBeforeLast('.') + ".jpg")
}

/** Miniatura para mostrar una foto aún no subida sin cargarla completa en memoria. */
fun decodeThumb(bytes: ByteArray, target: Int = 360): ImageBitmap? = decodeImageBitmap(bytes, maxSide = target * 2)
