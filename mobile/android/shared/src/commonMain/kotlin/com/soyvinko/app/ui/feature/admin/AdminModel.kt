package com.soyvinko.app.ui.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.common.statusLabel
import com.soyvinko.app.ui.theme.Vinko

/** Sección del panel: mismas claves, títulos y permisos que el arreglo `resources` de la web (pages/Admin.tsx). */
data class AdminSection(
    val key: String,
    val title: String,
    val permission: String,
    val icon: ImageVector,
    val subtitle: String,
    val description: String,
)

val ADMIN_SECTIONS = listOf(
    AdminSection(
        "contracts", "Servicios Contratados", "platform.read", Icons.Outlined.Handshake,
        "Expedientes, comisiones y planes de proveedor",
        "Servicios acordados entre clientes y profesionales, con el plan del proveedor y la comisión del modelo de negocio.",
    ),
    AdminSection(
        "stores", "Tiendas Marketplace", "platform.read", Icons.Outlined.Storefront,
        "Membresías, titulares y estado",
        "Comercios del marketplace: titular, membresía, inventario y pedidos generados.",
    ),
    AdminSection(
        "orders", "Pedidos Marketplace", "platform.read", Icons.Outlined.ShoppingBag,
        "Seguimiento e intervención de órdenes",
        "Pedidos del marketplace. Soporte puede avanzar o cancelar un pedido ante incidencias entre comprador y comercio.",
    ),
    AdminSection(
        "products", "Inventario Marketplace", "platform.read", Icons.Outlined.Inventory2,
        "Productos publicados por los comercios",
        "Inventario publicado por los comercios del marketplace.",
    ),
    AdminSection(
        "plans", "Modelo de Negocio & Planes", "plans.manage", Icons.Outlined.WorkspacePremium,
        "Membresías, comisiones y simulador",
        "Estructura de membresías mensuales, comisiones por venta o servicio y beneficios para comercios y proveedores.",
    ),
    AdminSection(
        "requests", "Solicitudes", "content.moderate", Icons.AutoMirrored.Outlined.Assignment,
        "Autorizar o retirar publicaciones",
        "Solicitudes de servicio. Autoriza su publicación o retíralas si incumplen las normas.",
    ),
    AdminSection(
        "users", "Usuarios", "users.manage", Icons.Outlined.People,
        "Estado de cuentas y roles del equipo",
        "Cuentas registradas. Suspende o reactiva cuentas y asigna roles del equipo SoyVinko.",
    ),
    AdminSection(
        "kyc", "Identidad", "kyc.review", Icons.Outlined.VerifiedUser,
        "Expedientes de verificación (KYC)",
        "Expedientes de verificación de identidad. Cada consulta de datos o documentos queda registrada en auditoría.",
    ),
    AdminSection(
        "tickets", "Soporte", "support.read", Icons.Outlined.SupportAgent,
        "Casos, respuestas y devoluciones",
        "Casos de soporte abiertos por los usuarios. Atiende, responde y gestiona su estado.",
    ),
    AdminSection(
        "fees", "Comisión de retiro", "money.configure", Icons.Outlined.Percent,
        "Tarifa por moneda y simulador",
        "Solo aplica al retiro de saldo. Recargas, uso, conservación y liberación del pago siguen sin comisión.",
    ),
    AdminSection(
        "vinko_rates", "Tasa de Cambio Vinkos", "money.configure", Icons.Outlined.CurrencyExchange,
        "Valor oficial de 1 Vinko",
        "Establece la tasa oficial de conversión para créditos Vinkos. Toda la plataforma, recargas, conversiones y wallet actualizarán sus valores en tiempo real.",
    ),
    AdminSection(
        "withdrawals", "Retiros", "money.configure", Icons.Outlined.AccountBalance,
        "Solicitudes de retiro de saldo",
        "Retiros de saldo solicitados por los usuarios, con su comisión y el neto a pagar.",
    ),
    AdminSection(
        "reviews", "Reseñas", "content.moderate", Icons.Outlined.RateReview,
        "Publicar u ocultar reseñas",
        "Reseñas de servicios finalizados. Puedes publicarlas u ocultarlas; la puntuación no se puede cambiar.",
    ),
    AdminSection(
        "files", "Archivos", "content.moderate", Icons.Outlined.FolderOpen,
        "Moderación de archivos subidos",
        "Archivos subidos por los usuarios. Revisa su contenido antes de aprobarlos.",
    ),
    AdminSection(
        "audit", "Auditoría", "platform.read", Icons.Outlined.Policy,
        "Registro de acciones sensibles",
        "Registro inmutable de acciones sensibles realizadas en la plataforma.",
    ),
)

fun sectionOf(key: String?): AdminSection? = ADMIN_SECTIONS.firstOrNull { it.key == key }

/** Secciones visibles para la persona (la web filtra por `permissions`). */
fun allowedSections(user: User): List<AdminSection> = ADMIN_SECTIONS.filter { user.can(it.permission) }

fun roleLabel(role: String): String = when (role) {
    "superadmin" -> "Superadministrador"
    "admin" -> "Administrador"
    "support" -> "Soporte"
    "professional" -> "Profesional"
    "customer" -> "Cliente"
    else -> role.replaceFirstChar(Char::uppercase)
}

// ---------------------------------------------------------------------------------------------
// Planes del modelo de negocio (etiquetas de la web)
// ---------------------------------------------------------------------------------------------

val PLAN_LABELS = mapOf(
    "STORE-PROMO" to "Promo Entrada (0-3m)",
    "STORE-STANDARD-30" to "Estándar \$30 (30 prods)",
    "STORE-PLUS-50" to "Plus \$50 (70 prods)",
    "STORE-BASE" to "Base 10% s/v",
    "PRO-PROMO" to "Promo Entrada (0-3m)",
    "PRO-MEMBERSHIP-15" to "Membresía Pro \$15 (3%/6%)",
    "PRO-BASE" to "Base 0 USD (8%/12%)",
)

fun planLabel(code: String?): String = code?.let { PLAN_LABELS[it] ?: it } ?: "Sin plan"

val PRO_PLAN_OPTIONS = listOf(
    Triple("PRO-PROMO", "Plan Promocional", "0-3 meses · 0%-3%"),
    Triple("PRO-MEMBERSHIP-15", "Membresía Profesional", "\$15 USD · 3% presencial / 6% remoto"),
    Triple("PRO-BASE", "Modalidad Base", "\$0 USD · 8% presencial / 12% remoto"),
)

val STORE_PLAN_OPTIONS = listOf(
    Triple("STORE-PROMO", "Plan Promocional Entrada", "\$0 USD · 0%-3% · 15 productos"),
    Triple("STORE-STANDARD-30", "Membresía Estándar", "\$30 USD · 4% mínimo · 30 productos"),
    Triple("STORE-PLUS-50", "Membresía Plus", "\$50 USD · 4% mínimo · 70 productos"),
    Triple("STORE-BASE", "Modalidad Base", "\$0 USD · 10% por venta · 10 productos"),
)

/** Comisión estimada de un servicio según el plan y la modalidad (misma regla que la web). */
fun contractFeeText(plan: String?, mode: String?, long: Boolean = false): String = when (plan) {
    "PRO-MEMBERSHIP-15" -> if (mode == "onsite") (if (long) "3% Presencial (Tasa mínima)" else "3% Presencial") else (if (long) "6% Remoto (Tasa mínima)" else "6% Remoto")
    "PRO-BASE" -> if (mode == "onsite") "8% Presencial" else "12% Remoto"
    else -> if (long) "0% - 3% (Promocional Entrada)" else "0% - 3% (Promo Entrada)"
}

fun modeLabel(mode: String?) = if (mode == "onsite") "Presencial" else "Remoto"

fun affiliateLabel(type: String?) = if (type == "store") "Comercio" else "Proveedor"

fun currencyName(currency: String?) = when (currency) {
    "VES" -> "Bolívares (VES)"
    "USD" -> "Dólares (USD)"
    null -> "—"
    else -> currency
}

val CURRENCY_OPTIONS = listOf("VES" to "Bs · Bolívar", "USD" to "USD · Dólar")

fun currencyPrefix(currency: String?) = if (currency == "USD") "US$ " else "Bs "

// ---------------------------------------------------------------------------------------------
// Etiquetas de estado por sección
// ---------------------------------------------------------------------------------------------

val TICKET_STATUS = listOf(
    "open" to "Abierto",
    "in_progress" to "En atención",
    "waiting_user" to "Esperando al usuario",
    "resolved" to "Resuelto",
    "closed" to "Cerrado",
)

val TICKET_CATEGORIES = mapOf(
    "service" to "Servicio contratado",
    "credits" to "Créditos",
    "kyc" to "Verificación de identidad",
    "review" to "Reseñas",
    "marketplace" to "Marketplace",
    "account" to "Cuenta",
    "abuse" to "Abuso o fraude",
    "other" to "Otro tema",
)

fun priorityLabel(p: String?) = when (p) {
    "low" -> "Prioridad baja"
    "normal" -> "Prioridad normal"
    "high" -> "Prioridad alta"
    "urgent" -> "Urgente"
    else -> null
}

val KYC_DOCS = mapOf(
    "identity_front" to "Cédula / Documento (Frente)",
    "identity_back" to "Cédula (Reverso)",
    "selfie" to "Selfie con Documento en Mano",
    "proof_of_address" to "Comprobante de Domicilio / RIF",
    "other" to "Documento Adicional",
)

fun kycProviderLabel(p: String?) = when (p) {
    "personal_ve" -> "Verificación personal (Venezuela)"
    "manual" -> "Revisión manual"
    null -> "Verificación de identidad"
    else -> p.replace('_', ' ').replaceFirstChar(Char::uppercase)
}

fun filePurposeLabel(p: String?) = when (p) {
    "avatar" -> "Foto de perfil"
    "portfolio" -> "Portafolio"
    "request" -> "Foto de solicitud"
    "chat" -> "Adjunto de chat"
    "product" -> "Foto de producto"
    "kyc" -> "Documento de identidad"
    "evidence" -> "Evidencia de entrega"
    null -> "Archivo"
    else -> p
}

private val AUDIT_ACTIONS = mapOf(
    "insert" to "Registro creado",
    "update" to "Registro actualizado",
    "delete" to "Registro eliminado",
    "user.registered" to "Usuario registrado",
    "professional.activated" to "Perfil profesional activado",
    "file.read" to "Lectura de archivo",
    "file.admin_read" to "Consulta administrativa de archivo",
    "kyc.read" to "Consulta de expediente KYC",
    "support.ticket_read" to "Consulta de caso de soporte",
    "support.ticket_updated" to "Caso de soporte actualizado",
    "role.changed" to "Cambio de rol",
    "plan.configured" to "Plan configurado",
    "category.configured" to "Categoría configurada",
    "store.status_changed" to "Estado de tienda cambiado",
    "store.plan_changed" to "Membresía de tienda cambiada",
    "professional.plan_changed" to "Plan de profesional cambiado",
    "credit.refunded" to "Crédito devuelto",
    "escrow.refund_resolved" to "Devolución de saldo resuelta",
    "vinko.rate_configured" to "Tasa Vinko configurada",
)

fun auditActionLabel(a: String?): String = when {
    a == null -> "Acción"
    a.startsWith("order.admin_") -> "Intervención de pedido (${orderActionLabel(a.removePrefix("order.admin_")).lowercase()})"
    else -> AUDIT_ACTIONS[a] ?: a.replace('_', ' ').replace(".", " · ").replaceFirstChar(Char::uppercase)
}

fun orderActionLabel(a: String) = when (a) {
    "accept" -> "Aceptación forzada"
    "dispatch" -> "Despacho"
    "receive" -> "Entrega confirmada"
    "cancel" -> "Cancelación"
    else -> a
}

private val ENTITY_LABELS = mapOf(
    "sessions" to "Sesiones", "user_roles" to "Roles", "credentials" to "Credenciales", "offerings" to "Servicios ofrecidos",
    "users" to "Usuarios", "user" to "Usuario", "branches" to "Sedes", "file" to "Archivo", "service_requests" to "Solicitudes",
    "professional" to "Profesional", "professionals" to "Profesionales", "kyc" to "Expediente KYC", "ticket" to "Caso de soporte",
    "plan" to "Plan", "category" to "Categoría", "store" to "Tienda", "order" to "Pedido", "contract" to "Servicio contratado",
    "vinko_exchange_rate" to "Tasa Vinko",
)

fun entityLabel(e: String?) = e?.let { ENTITY_LABELS[it] ?: it.replace('_', ' ').replaceFirstChar(Char::uppercase) } ?: "—"

/** Etiqueta de estado según la sección (la web usa el mismo componente Badge para todo). */
fun adminStatusLabel(resource: String, value: String?): String {
    if (value == null) return "—"
    val specific: String? = when (resource) {
        "tickets" -> TICKET_STATUS.toMap()[value]
        "users" -> mapOf("active" to "Activo", "suspended" to "Suspendido", "closed" to "Cerrado")[value]
        "stores" -> mapOf("active" to "Activa", "suspended" to "Suspendida", "inactive" to "Inactiva", "closed" to "Cerrada")[value]
        "kyc" -> mapOf(
            "pending" to "Pendiente", "in_review" to "En revisión", "approved" to "Aprobada",
            "rejected" to "Rechazada", "revoked" to "Revocada", "expired" to "Vencida",
        )[value]
        "vinko_rates" -> if (value == "active") "Vigente" else "Histórica"
        "reviews" -> mapOf("pending" to "Pendiente", "published" to "Publicada", "hidden" to "Oculta")[value]
        "files", "requests_moderation" -> mapOf("pending" to "Pendiente", "approved" to "Aprobado", "rejected" to "Rechazado")[value]
        "orders_payment" -> mapOf(
            "not_reported" to "Sin reporte", "buyer_reported" to "Reportado por el comprador",
            "seller_confirmed" to "Confirmado por el comercio", "disputed" to "En disputa",
        )[value]
        "products" -> mapOf("active" to "Publicado", "inactive" to "Inactivo", "archived" to "Archivado", "draft" to "Borrador")[value]
        "withdrawals" -> mapOf("review" to "En revisión", "paid" to "Pagado", "processing" to "Procesando")[value]
        else -> null
    }
    return specific ?: when (value) {
        "suspended" -> "Suspendido"
        "archived" -> "Archivado"
        "revoked" -> "Revocado"
        else -> statusLabel(value)
    }
}

private enum class Tone { Good, Bad, Warn, Neutral, Info }

private fun toneOf(value: String?): Tone = when (value) {
    "completed", "approved", "verified", "paid", "settled", "released", "delivered", "active", "funded", "resolved",
    "published", "seller_confirmed" -> Tone.Good
    "cancelled", "rejected", "failed", "disputed", "expired", "frozen", "disabled", "suspended", "revoked", "hidden" -> Tone.Bad
    "pending", "awaiting_confirmation", "in_review", "review", "processing", "draft", "changes_requested", "waiting_user",
    "buyer_reported", "in_progress" -> Tone.Warn
    "archived", "inactive", "closed", "not_reported" -> Tone.Neutral
    else -> Tone.Info
}

fun statusColors(value: String?): Pair<Color, Color> = when (toneOf(value)) {
    Tone.Good -> Vinko.Mint to Vinko.MintBg
    Tone.Bad -> Vinko.Coral to Vinko.CoralBg
    Tone.Warn -> Vinko.Amber to Vinko.AmberBg
    Tone.Neutral -> Vinko.Muted to Color(0xFFF1F0F5)
    Tone.Info -> Vinko.Primary to Vinko.Tint
}

/** Insignia de estado con las etiquetas de la sección. */
@Composable
fun AdminBadge(resource: String, value: String?, prefix: String? = null) {
    val (fg, bg) = statusColors(value)
    Text(
        (prefix?.let { "$it: " } ?: "") + adminStatusLabel(resource, value),
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        maxLines = 1,
        modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
