package com.soyvinko.app.ui.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.dateTime
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject

/** Marco común de las tarjetas: icono, título, subtítulo, estado, contenido y acciones. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badge: (@Composable () -> Unit)? = null,
    iconTint: Color = Vinko.Primary,
    iconBg: Color = Vinko.Tint,
    actions: (@Composable FlowRowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconBox(icon, iconTint, iconBg)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (badge != null) {
                Spacer(Modifier.width(8.dp))
                badge()
            }
        }
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
        if (actions != null) {
            Spacer(Modifier.height(10.dp))
            LineDivider()
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}

/** Fila con etiqueta e insignia de estado. */
@Composable
fun FieldBadge(label: String, resource: String, value: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, modifier = Modifier.weight(1f))
        AdminBadge(resource, value)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(vararg tags: Pair<String, ImageVector?>) {
    FlowRow(
        Modifier.padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { tags.forEach { (t, i) -> Tag(t, icon = i) } }
}

@Composable
private fun Stars(rating: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { i ->
            Icon(
                if (i < rating) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (i < rating) Color(0xFFF5A524) else Vinko.Lilac,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text("$rating/5", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
    }
}

/** Tarjeta de un elemento según la sección, con las columnas y acciones de la web. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResourceCard(
    resource: String,
    r: JsonObject,
    user: User,
    sandbox: Boolean,
    onDlg: (AdminDlg) -> Unit,
    onOpenTicket: (String) -> Unit,
) {
    val cur = r.s("currency")?.trim()
    when (resource) {
        "contracts" -> AdminCard(
            Icons.Outlined.Handshake, r.s("request_title") ?: "Servicio acordado", r.s("category_name"),
            badge = { AdminBadge(resource, r.s("status")) },
            actions = {
                CardAction("Ver expediente", { onDlg(AdminDlg.ContractDetail(r)) }, Icons.Outlined.Visibility)
                if (user.can("plans.manage")) CardAction("Asignar plan proveedor", { onDlg(AdminDlg.ProfessionalPlan(r)) }, Icons.Outlined.WorkspacePremium)
            },
        ) {
            Tags(
                modeLabel(r.s("delivery_mode")) to if (r.s("delivery_mode") == "onsite") Icons.Outlined.Place else Icons.Outlined.Laptop,
                planLabel(r.s("plan_code")) to null,
            )
            Field("Cliente", r.s("customer_name") ?: "—")
            Field("Profesional", r.s("professional_name") ?: "—")
            Field("Precio acordado", money(r.l("agreed_price_minor"), cur ?: "USD"), strong = true)
            Field("Comisión modelo", contractFeeText(r.s("plan_code"), r.s("delivery_mode")), valueColor = Vinko.Primary)
            Field("Fecha", date(r.s("accepted_at")).ifEmpty { "—" })
        }

        "stores" -> AdminCard(
            Icons.Outlined.Storefront, r.s("name") ?: "Comercio", r.s("owner_name"),
            badge = { AdminBadge(resource, r.s("status")) },
            actions = {
                CardAction("Ver comercio", { onDlg(AdminDlg.StoreDetail(r)) }, Icons.Outlined.Visibility)
                if (user.can("plans.manage")) CardAction("Cambiar membresía", { onDlg(AdminDlg.StorePlan(r)) }, Icons.Outlined.WorkspacePremium)
                if (user.can("users.manage")) CardAction("Cambiar estado", { onDlg(AdminDlg.StoreStatus(r)) }, Icons.Outlined.ToggleOn)
            },
        ) {
            Tags(planLabel(r.s("plan_code")) to null)
            Field("Productos", "${r.l("products_count") ?: 0}")
            Field("Pedidos", "${r.l("orders_count") ?: 0}")
            Field("Fecha alta", date(r.s("created_at")))
        }

        "orders" -> AdminCard(
            Icons.Outlined.ShoppingBag, "Pedido ${shortId(r.s("id"))}", r.s("store_name"),
            badge = { AdminBadge(resource, r.s("status")) },
            actions = {
                CardAction("Ver pedido", { onDlg(AdminDlg.OrderDetail(r)) }, Icons.Outlined.Visibility)
                if (user.can("support.manage")) CardAction("Intervención orden", { onDlg(AdminDlg.AdvanceOrder(r)) }, Icons.Outlined.Gavel)
            },
        ) {
            Field("Comprador", r.s("customer_name") ?: "—")
            Field("Total", money(r.l("total_minor"), cur ?: "USD"), strong = true)
            Field("Comisión plataforma", "4% - 10% según membresía", valueColor = Vinko.Primary)
            FieldBadge("Reporte de pago", "orders_payment", r.s("payment_status"))
            Field("Fecha", date(r.s("created_at")))
        }

        "products" -> AdminCard(
            Icons.Outlined.Inventory2, r.s("name") ?: "Producto", r.s("store_name"),
            badge = { AdminBadge(resource, r.s("status")) },
        ) {
            Field("SKU", r.s("sku") ?: "—", mono = true)
            Field("Precio", money(r.l("price_minor"), cur ?: "USD"), strong = true)
            Field("Stock", "${r.l("stock") ?: 0} unidades")
            Field("Publicado", date(r.s("created_at")))
        }

        "plans" -> AdminCard(
            Icons.Outlined.WorkspacePremium, r.s("name") ?: "Plan", r.s("code"),
            badge = { AdminBadge(resource, if (r.b("active")) "active" else "inactive") },
            actions = if (user.can("plans.manage")) ({ CardAction("Editar plan", { onDlg(AdminDlg.ConfigurePlan(r)) }, Icons.Outlined.Edit) }) else null,
        ) {
            Tags(affiliateLabel(r.s("affiliate_type")) to if (r.s("affiliate_type") == "store") Icons.Outlined.Storefront else Icons.Outlined.Person)
            Field("Costo mensual", money(r.l("price_minor"), cur ?: "USD"), strong = true)
            Field("Comisión por venta / servicio", r.s("fee_summary") ?: "—", valueColor = Vinko.Primary)
            Field("Límite inventario", (r.l("max_products") ?: 0).let { if (it > 0) "$it productos" else "Sin límite / No aplica" })
            Field("Créditos incluidos", "${r.l("credits") ?: 0}")
        }

        "requests" -> AdminCard(
            Icons.AutoMirrored.Outlined.Assignment, r.s("title") ?: "Solicitud", null,
            badge = { AdminBadge(resource, r.s("status")) },
            actions = if (r.s("status") in setOf("draft", "open")) ({
                val approved = r.s("moderation_status") == "approved"
                CardAction(
                    if (approved) "Desautorizar" else "Autorizar publicación",
                    { onDlg(AdminDlg.ModerateRequest(r)) },
                    if (approved) Icons.Outlined.LockOpen else Icons.Outlined.TaskAlt,
                    primary = !approved, danger = approved,
                )
            }) else null,
        ) {
            Tags(modeLabel(r.s("delivery_mode")) to if (r.s("delivery_mode") == "onsite") Icons.Outlined.Place else Icons.Outlined.Laptop)
            FieldBadge("Revisión", "requests_moderation", r.s("moderation_status"))
            r.l("offered_price_minor")?.let { Field("Precio ofrecido", money(it, cur)) }
            Field("Creada", date(r.s("created_at")))
        }

        "files" -> AdminCard(
            if (r.s("mime_type") == "application/pdf") Icons.Outlined.Description else Icons.Outlined.Image,
            filePurposeLabel(r.s("purpose")), "${r.s("mime_type").orEmpty()} · ${bytesText(r.l("bytes"))}",
            badge = { AdminBadge(resource, r.s("moderation_status")) },
            actions = {
                CardAction("Ver archivo", { onDlg(AdminDlg.ViewFile(r)) }, Icons.Outlined.Visibility)
                CardAction("Moderar", { onDlg(AdminDlg.ModerateFile(r)) }, Icons.Outlined.Gavel, primary = r.s("moderation_status") == "pending")
            },
        ) {
            Field("Metadatos EXIF", if (r.b("exif_removed")) "Eliminados" else "Sin limpiar")
            Field("Subido", date(r.s("created_at")))
            Field("Referencia", shortId(r.s("id")), mono = true)
        }

        "reviews" -> AdminCard(
            Icons.Outlined.RateReview, "Reseña de servicio", if (r.b("recommends")) "Recomienda al profesional" else "No recomienda al profesional",
            badge = { AdminBadge(resource, r.s("moderation_status")) },
            actions = { CardAction("Moderar reseña", { onDlg(AdminDlg.ModerateReview(r)) }, Icons.Outlined.Gavel) },
        ) {
            Stars(r.l("rating") ?: 0)
            r.s("comment")?.takeIf { it.isNotBlank() }?.let {
                Text("“$it”", style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 4.dp))
            }
            Field("Fecha", date(r.s("created_at")))
        }

        "kyc" -> AdminCard(
            Icons.Outlined.VerifiedUser, kycProviderLabel(r.s("provider")), "Expediente ${shortId(r.s("id"))}",
            badge = { AdminBadge(resource, r.s("status")) },
            actions = {
                CardAction("Abrir expediente", { onDlg(AdminDlg.OpenKyc(r)) }, Icons.Outlined.Visibility, primary = true)
                if (!isMe(r.s("user_id"))) CardAction("Resolver verificación", { onDlg(AdminDlg.ReviewKyc(r)) }, Icons.Outlined.Gavel)
            },
        ) {
            Field("Presentado", dateTime(r.s("submitted_at")).ifEmpty { "—" })
            r.s("reviewed_at")?.let { Field("Revisado", date(it)) }
            r.s("expires_at")?.let { Field("Válido hasta", date(it)) }
            r.s("reason")?.let { Field("Motivo", it) }
        }

        "users" -> AdminCard(
            Icons.Outlined.Person, r.s("display_name") ?: "Usuario", r.s("email"),
            badge = { AdminBadge(resource, r.s("status")) },
            actions = if (isMe(r.s("id"))) null else ({
                if (user.can("users.manage")) CardAction("Cambiar estado", { onDlg(AdminDlg.UserStatus(r)) }, Icons.Outlined.ToggleOn)
                if (user.can("roles.manage")) CardAction("Asignar rol", { onDlg(AdminDlg.AssignRole(r)) }, Icons.Outlined.AdminPanelSettings)
            }),
        ) {
            Field("Tipo de cuenta", if (r.s("account_type") == "juridico") "Empresa (jurídica)" else "Persona natural")
            r.s("business_name")?.let { Field("Razón social", it) }
            Field("Correo verificado", r.s("email_verified_at")?.let { date(it) } ?: "Pendiente")
            Field("Alta", date(r.s("created_at")))
        }

        "tickets" -> AdminCard(
            Icons.Outlined.SupportAgent, r.s("subject") ?: "Caso de soporte", TICKET_CATEGORIES[r.s("category")] ?: "Otro tema",
            badge = { AdminBadge(resource, r.s("status")) },
            iconTint = if (r.s("priority") in setOf("high", "urgent")) Vinko.Coral else Vinko.Primary,
            iconBg = if (r.s("priority") in setOf("high", "urgent")) Vinko.CoralBg else Vinko.Tint,
            actions = { CardAction("Atender caso", { r.s("id")?.let(onOpenTicket) }, Icons.Outlined.SupportAgent, primary = true) },
        ) {
            priorityLabel(r.s("priority"))?.let { Tags(it to null) }
            r.s("description")?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Field("Abierto", dateTime(r.s("created_at")))
            if (r.s("contract_id") != null) Field("Servicio vinculado", shortId(r.s("contract_id")), mono = true)
            if (r.s("order_id") != null) Field("Pedido vinculado", shortId(r.s("order_id")), mono = true)
        }

        "fees" -> AdminCard(
            Icons.Outlined.Percent, currencyName(cur), "Comisión de retiro",
            badge = { AdminBadge("vinko_rates", if (r.b("active")) "active" else "archived") },
        ) {
            Field("Comisión", percentText(r.l("basis_points")), strong = true, valueColor = Vinko.Primary)
            r.s("reason")?.let { Field("Motivo", it) }
            Field("Fecha", date(r.s("created_at")))
        }

        "vinko_rates" -> AdminCard(
            Icons.Outlined.CurrencyExchange, "1 Vinko = ${money(r.l("rate_minor"), cur)}", currencyName(cur),
            badge = { AdminBadge(resource, r.s("status")) },
        ) {
            r.s("reason")?.let { Field("Motivo de la actualización", it) }
            Field("Administrador", r.s("creator_name") ?: "Administración")
            Field("Fecha", dateTime(r.s("created_at")))
        }

        "withdrawals" -> AdminCard(
            Icons.Outlined.AccountBalance, money(r.l("amount_minor"), cur), r.s("owner_name"),
            badge = { AdminBadge(resource, r.s("status")) },
            actions = if (sandbox && r.s("status") in setOf("pending", "processing")) ({
                CardAction("Simular retiro pagado", { onDlg(AdminDlg.Settle(r)) }, Icons.Outlined.Payments, primary = true)
            }) else null,
        ) {
            Field("Comisión", money(r.l("fee_minor"), cur))
            Field("Neto", money(r.l("net_minor"), cur), strong = true, valueColor = Vinko.Mint)
            r.s("destination_label")?.let { Field("Destino", it) }
            Field("Fecha", dateTime(r.s("created_at")))
        }

        "audit" -> AdminCard(
            Icons.Outlined.Policy, auditActionLabel(r.s("action")), "${entityLabel(r.s("entity_type"))} · ${shortId(r.s("entity_id"))}",
        ) {
            Field("Fecha", dateTime(r.s("created_at")))
            Field("Realizado por", r.s("actor_id")?.let(::shortId) ?: "Sistema", mono = r.s("actor_id") != null)
            r.s("correlation_id")?.let { Field("Correlación", it.take(18), mono = true) }
        }

        else -> AdminCard(Icons.Outlined.FolderOpen, r.s("name") ?: r.s("title") ?: shortId(r.s("id")), null) {
            r.s("status")?.let { FieldBadge("Estado", resource, it) }
        }
    }
}

