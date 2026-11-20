package com.soyvinko.app.ui.feature.transport

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.toRadians
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class VehicleTypeInfo(
    val id: String,
    val name: String,
    val subtitle: String,
    val emoji: String,
    val capacity: String,
    val baseRateSummary: String,
    val color: Color,
    val icon: ImageVector,
)

val VEHICLE_CATALOG = listOf(
    VehicleTypeInfo(
        id = "moto",
        name = "Moto Express",
        subtitle = "Rápida y económica para encomiendas o 1 pasajero",
        emoji = "🏍️",
        capacity = "Hasta 20 kg • 1 persona",
        baseRateSummary = "$1.00 base (hasta 10 km) + $0.20/km",
        color = Color(0xFF6F3DFF), // Violeta Principal (#6F3DFF)
        icon = Icons.Outlined.TwoWheeler,
    ),
    VehicleTypeInfo(
        id = "carro",
        name = "Carro / Taxi",
        subtitle = "Traslado de personas o paquetes medianos protegidos",
        emoji = "🚗",
        capacity = "Hasta 4 pasajeros • Maletero 80 kg",
        baseRateSummary = "$2.00 base (hasta 10 km) + $0.40/km",
        color = Color(0xFF0284C7), // Azul Cian (#0284C7)
        icon = Icons.Outlined.DirectionsCar,
    ),
    VehicleTypeInfo(
        id = "pickup",
        name = "Camioneta / Pickup",
        subtitle = "Mudanzas medianas, electrodomésticos, cajas y bultos",
        emoji = "🛻",
        capacity = "Hasta 800 kg de carga en batea",
        baseRateSummary = "$4.00 base (hasta 10 km) + $0.80/km",
        color = Color(0xFF0C8E72), // Verde Menta (#0C8E72)
        icon = Icons.Outlined.LocalShipping,
    ),
    VehicleTypeInfo(
        id = "camion",
        name = "Camión / Flete",
        subtitle = "Mudanzas completas, carga pesada y grandes volúmenes",
        emoji = "🚚",
        capacity = "Hasta 3.500 kg de carga industrial",
        baseRateSummary = "$8.00 base (hasta 10 km) + $1.50/km",
        color = Color(0xFF4B1FAE), // Morado Profundo (#4B1FAE)
        icon = Icons.Outlined.LocalShipping,
    ),
)

fun findVehicleTypeInfo(type: String): VehicleTypeInfo? =
    VEHICLE_CATALOG.firstOrNull { it.id.equals(type, ignoreCase = true) }

/**
 * Calcula la distancia ortodrómica en kilómetros entre dos coordenadas geográficas.
 */
fun haversineKm(pos1: GeoPos, pos2: GeoPos): Double {
    val earthRadiusKm = 6371.0
    val dLat = (pos2.lat - pos1.lat).toRadians()
    val dLon = (pos2.lon - pos1.lon).toRadians()
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(pos1.lat.toRadians()) * cos(pos2.lat.toRadians()) *
        sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}

/**
 * Calcula el costo medio estándar del transporte según la distancia por carretera:
 * - Tramo de 0 a 10 km: $1.00 base.
 * - Kilómetro adicional (> 10 km): +$0.20 por km adicional.
 * - Multiplicador según tipo de vehículo.
 * - Redondeo hacia arriba al entero más cercano (ej. 1.60 -> 2.00), mínimo $1.00.
 */
fun calculateStandardTransportFare(distanceKm: Double, vehicleType: String): Double {
    val d = distanceKm.coerceAtLeast(0.1)
    val rawDistanceFare = if (d <= 10.0) {
        1.0
    } else {
        1.0 + ((d - 10.0) * 0.20)
    }

    val multiplier = when (vehicleType.lowercase()) {
        "moto" -> 1.0
        "carro" -> 2.0
        "pickup" -> 4.0
        "camion" -> 8.0
        else -> 1.0
    }

    val rawFare = rawDistanceFare * multiplier
    return ceil(rawFare).coerceAtLeast(1.0)
}

fun vehicleIcon(type: String): ImageVector = when (type.lowercase()) {
    "moto" -> Icons.Outlined.TwoWheeler
    "pickup" -> Icons.Outlined.LocalShipping
    "camion" -> Icons.Outlined.LocalShipping
    else -> Icons.Outlined.DirectionsCar
}

fun vehicleLabel(type: String): String = when (type.lowercase()) {
    "moto" -> "Moto"
    "carro" -> "Carro"
    "pickup" -> "Camioneta Pickup"
    "camion" -> "Camión"
    else -> "Vehículo"
}

fun shipmentTypeLabel(type: String?): String = when (type) {
    "ride" -> "Viaje de Pasajero"
    "package" -> "Paquete Express"
    "freight" -> "Flete / Carga"
    "food" -> "Delivery Comida"
    else -> "Envío"
}

fun formatTransportDistance(distanceKm: Double): String = when {
    distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m"
    else -> {
        val rounded = ((distanceKm * 10).toInt()) / 10.0
        "$rounded km"
    }
}

fun formatTransportFare(fareUsd: Double): String {
    val integerPart = fareUsd.toInt()
    return if (fareUsd == integerPart.toDouble()) {
        "$$integerPart.00"
    } else {
        val rounded = ((fareUsd * 100).toInt()) / 100.0
        "$$rounded"
    }
}

// ---------------------------------------------------------------------------------------------
// Funciones Validadoras UX/UI
// ---------------------------------------------------------------------------------------------

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
) {
    companion object {
        fun valid() = ValidationResult(isValid = true)
        fun invalid(message: String) = ValidationResult(isValid = false, errorMessage = message)
    }
}

/**
 * Valida formatos de placa vehicular estándar o tradicional venezolana.
 * Ejemplos válidos: "AB123CD", "ABC12D", "AA123BB", "7A1B2C3".
 */
fun validateLicensePlate(plate: String): ValidationResult {
    val clean = plate.trim().uppercase()
    if (clean.isBlank()) {
        return ValidationResult.invalid("Ingresa la placa del vehículo.")
    }
    if (clean.length !in 5..8) {
        return ValidationResult.invalid("La placa debe contener entre 5 y 8 caracteres (ej: AB123CD).")
    }
    val alnum = clean.filter { it.isLetterOrDigit() }
    if (alnum.length != clean.length) {
        return ValidationResult.invalid("La placa no debe incluir guiones ni caracteres especiales.")
    }
    val hasLetters = clean.any { it.isLetter() }
    val hasDigits = clean.any { it.isDigit() }
    if (!hasLetters || !hasDigits) {
        return ValidationResult.invalid("Una placa válida combina letras y números.")
    }
    return ValidationResult.valid()
}

/**
 * Valida números telefónicos en formato venezolano (0412, 0414, 0424, 0416, 0426 o prefijo +58).
 */
fun validateVenezuelanPhone(phone: String): ValidationResult {
    val clean = phone.trim().replace(" ", "").replace("-", "")
    if (clean.isBlank()) {
        return ValidationResult.invalid("Ingresa un número telefónico.")
    }
    val digitsOnly = when {
        clean.startsWith("+58") -> clean.removePrefix("+58")
        clean.startsWith("58") -> clean.removePrefix("58")
        else -> clean
    }
    val localNumber = if (!digitsOnly.startsWith("0")) "0$digitsOnly" else digitsOnly

    val prefixes = listOf("0412", "0414", "0424", "0416", "0426", "0212")
    if (localNumber.length != 11) {
        return ValidationResult.invalid("El número debe tener 11 dígitos (ej: 04121234567).")
    }
    if (prefixes.none { localNumber.startsWith(it) }) {
        return ValidationResult.invalid("Prefijo inválido. Debe iniciar con 0412, 0414, 0424, 0416 o 0426.")
    }
    return ValidationResult.valid()
}

/**
 * Valida la dirección del punto de origen o destino.
 */
fun validateTransportAddress(address: String, label: String = "dirección"): ValidationResult {
    val clean = address.trim()
    if (clean.isBlank()) {
        return ValidationResult.invalid("Por favor indica la $label.")
    }
    if (clean.length < 5) {
        return ValidationResult.invalid("La $label debe ser más detallada (calle, edificio o punto de referencia).")
    }
    return ValidationResult.valid()
}

/**
 * Valida una contraoferta o tarifa personalizada.
 */
fun validateCustomFare(fare: Double, standardFare: Double, vehicleType: String): ValidationResult {
    if (fare <= 0.0) {
        return ValidationResult.invalid("La tarifa debe ser mayor a cero.")
    }
    val minAllowed = (standardFare * 0.6).coerceAtLeast(1.0)
    if (fare < minAllowed) {
        val minStr = formatTransportFare(minAllowed)
        return ValidationResult.invalid("La tarifa mínima permitida para $vehicleType es $minStr.")
    }
    val maxAllowed = (standardFare * 3.5).coerceAtLeast(15.0)
    if (fare > maxAllowed) {
        return ValidationResult.invalid("La tarifa excede el límite máximo razonable ($${maxAllowed.toInt()}.00).")
    }
    return ValidationResult.valid()
}

// ---------------------------------------------------------------------------------------------
// Rutas, Tiempos Estimados y Finanzas Rider
// ---------------------------------------------------------------------------------------------

data class RiderEarnings(
    val grossFareMinor: Long,
    val commissionMinor: Long,
    val netFareMinor: Long,
    val currency: String = "USD",
)

/**
 * Calcula el desglose de ingresos del rider aplicando la comisión de servicio.
 */
fun calculateRiderEarnings(grossFareMinor: Long, commissionPercent: Double = 10.0, currency: String = "USD"): RiderEarnings {
    val commission = (grossFareMinor * (commissionPercent / 100.0)).toLong()
    val net = (grossFareMinor - commission).coerceAtLeast(0L)
    return RiderEarnings(
        grossFareMinor = grossFareMinor,
        commissionMinor = commission,
        netFareMinor = net,
        currency = currency,
    )
}

/**
 * Calcula el tiempo estimado de viaje (ETA) en minutos según distancia y vehículo.
 */
fun estimateTripDurationMinutes(distanceKm: Double, vehicleType: String): Int {
    val d = distanceKm.coerceAtLeast(0.1)
    val avgSpeedKmH = when (vehicleType.lowercase()) {
        "moto" -> 35.0
        "carro" -> 30.0
        "pickup" -> 28.0
        "camion" -> 22.0
        else -> 30.0
    }
    val hours = d / avgSpeedKmH
    val minutes = (hours * 60).toInt() + 4 // +4 min margen semáforos
    return minutes.coerceAtLeast(5)
}

/**
 * Genera el enlace de navegación externa para Google Maps o Waze.
 */
fun getExternalNavigationUrl(pos: GeoPos, destinationName: String = ""): String {
    val encodedName = destinationName.trim().replace(" ", "+")
    return "https://www.google.com/maps/dir/?api=1&destination=${pos.lat},${pos.lon}&destination_place_id=$encodedName"
}

/**
 * Genera el texto formateado del comprobante digital de despacho para WhatsApp.
 */
fun buildShipmentReceiptText(
    shipmentId: String,
    title: String,
    fareMinor: Long,
    currency: String,
    vehicleType: String,
    originAddress: String,
    destAddress: String,
    status: String,
    driverName: String? = null,
    paymentMethod: String? = "wallet",
    distanceKm: Double? = null,
): String {
    val methodLabel = when (paymentMethod) {
        "wallet" -> "Billetera Digital Vinko (Garantía Escrow ✅)"
        "pago_movil" -> "Pago Móvil Directo"
        "cash" -> "Efectivo"
        else -> "Pago Acordado"
    }
    val statusLabel = when (status) {
        "delivered" -> "✅ ENTREGADO CON ÉXITO"
        "in_transit" -> "🚚 EN CAMINO AL DESTINO"
        "picking_up" -> "📍 CONDUCTOR EN ORIGEN"
        "accepted" -> "⚡ CONDUCTOR ASIGNADO"
        else -> "EN PROCESO"
    }

    val distStr = if (distanceKm != null && distanceKm > 0) "📏 *Distancia:* ${formatTransportDistance(distanceKm)}\n" else ""
    val driverStr = if (!driverName.isNullOrBlank()) "👤 *Conductor:* $driverName\n" else ""

    return """
🧾 *COMPROBANTE DIGITAL DE DESPACHO*
*SOYVINKO VENEZUELA*
──────────────────────
📌 *Folio:* #${shipmentId.take(8).uppercase()}
📋 *Estado:* $statusLabel
📦 *Servicio:* ${title.ifBlank { "Despacho / Transporte" }}
🚗 *Vehículo:* ${vehicleLabel(vehicleType)}
$driverStr
📍 *Punto de Salida:*
$originAddress

🏁 *Punto de Entrega:*
$destAddress
$distStr💰 *Monto del Servicio:* ${money(fareMinor, currency)}
💳 *Método de Pago:* $methodLabel
──────────────────────
🛡️ _Operación verificada y registrada en la plataforma SoyVinko._
🌐 _https://soyvinko.com_
""".trimIndent()
}
