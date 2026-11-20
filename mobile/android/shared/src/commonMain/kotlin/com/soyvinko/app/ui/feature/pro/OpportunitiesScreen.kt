package com.soyvinko.app.ui.feature.pro

import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.days
import com.soyvinko.app.ui.common.nowInstant
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.dateTime
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberAction
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject

/** Presupuesto visible para el profesional (sin datos del cliente). */
fun budgetText(r: JsonObject): String {
    val currency = r.s("currency")?.trim()
    val min = r.l("budget_min_minor")
    val max = r.l("budget_max_minor")
    return when {
        min != null && max != null && min != max -> "${money(min, currency)} – ${money(max, currency)}"
        max != null -> money(max, currency)
        r.l("offered_price_minor") != null -> money(r.l("offered_price_minor"), currency)
        else -> "A cotizar en ${currencyShort(currency)}"
    }
}

@Composable
fun NotProfessional(
    nav: NavHostController,
    title: String,
    text: String = "Publica tus servicios, encuentra oportunidades y recibe 3 Vinkos de bienvenida.",
) {
    ScreenScaffold(title, onBack = { nav.popBackStack() }) { padding ->
        Box(Modifier.padding(padding)) {
            EmptyBox(
                "Activa tu perfil profesional",
                text,
                "Activar perfil",
            ) { nav.navigate(Routes.PRO) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Lista de oportunidades
// ---------------------------------------------------------------------------------------------

@Composable
fun OpportunitiesScreen(nav: NavHostController) {
    val app = LocalApp.current
    val user = LocalUser.current
    if (!user.isProfessional) {
        NotProfessional(nav, "Oportunidades")
        return
    }
    var mode by rememberSaveable { mutableStateOf("onsite") }
    val feed = rememberResource(mode) { app.ops.call("service_feed", "delivery_mode" to mode, "limit" to 50) }
    val mine = rememberResource { app.ops.call("my_proposals") }
    val cats = rememberCategories()
    val proposed = mine.data.objects().mapNotNull { it.s("request_id") }.toSet()
    val catName = { id: String? -> cats.data.orEmpty().firstOrNull { it.first == id }?.second ?: "Servicio" }

    ScreenScaffold(
        "Oportunidades",
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { feed.reload(); mine.reload() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar", tint = Vinko.Ink)
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Segmented(listOf("onsite" to "En tu zona", "remote" to "Proyectos remotos"), mode, { mode = it })
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Toll, contentDescription = null, tint = Vinko.Amber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Enviar propuestas es gratis. Si te contratan se descuenta 1 Vinko.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Vinko.Muted,
                    )
                }
            }
            ResourceView(feed) { data ->
                val items = data.objects()
                if (items.isEmpty()) {
                    if (mode == "onsite") {
                        EmptyBox(
                            "No hay solicitudes en tu zona por ahora",
                            "Revisa que tus servicios presenciales estén activos y que tus sedes tengan cobertura para esas categorías.",
                            "Revisar mi configuración",
                        ) { nav.navigate(Routes.PRO_SETUP) }
                    } else {
                        EmptyBox(
                            "No hay proyectos remotos por ahora",
                            "Publica servicios remotos para recibir proyectos de clientes de todo el país.",
                            "Revisar mis servicios",
                        ) { nav.navigate(Routes.PRO_SETUP) }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items, key = { it.s("request_id") ?: it.toString() }) { r ->
                            OpportunityCard(r, catName(r.s("category_id")), r.s("request_id") in proposed) {
                                r.s("request_id")?.let { nav.navigate(opportunityRoute(it, mode)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpportunityCard(r: JsonObject, category: String, proposed: Boolean, onClick: () -> Unit) {
    val direct = r.s("mode") == "direct"
    VCard(Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (direct) Pill("Invitación para ti", Vinko.Amber, Vinko.AmberBg, Icons.Outlined.Bolt)
            else Pill("Convocatoria abierta", icon = Icons.Outlined.Campaign)
            Spacer(Modifier.weight(1f))
            Text("Vence ${date(r.s("expires_at"))}", style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle)
        }
        Spacer(Modifier.height(10.dp))
        Text(category.uppercase(), style = MaterialTheme.typography.labelSmall, color = Vinko.Primary)
        Spacer(Modifier.height(2.dp))
        Text(r.s("title") ?: "", style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(r.s("description") ?: "", style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
        val photos = r.list("attachments").mapNotNull { it.s("file_id") }
        if (photos.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.take(4).forEach { ProImage(it, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))) }
                if (photos.size > 4) {
                    Box(Modifier.size(64.dp).background(Vinko.Tint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("+${photos.size - 4}", style = MaterialTheme.typography.titleSmall, color = Vinko.Primary)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        LineDivider()
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (r.s("delivery_mode") == "remote") Icons.Outlined.Public else Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Vinko.Lavender,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (r.s("delivery_mode") == "remote") "Remoto" else "En tu zona de cobertura",
                        style = MaterialTheme.typography.bodySmall,
                        color = Vinko.Muted,
                    )
                }
                if (proposed) {
                    Spacer(Modifier.height(4.dp))
                    Pill("Propuesta enviada", Vinko.Mint, Vinko.MintBg, Icons.Outlined.CheckCircle)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Presupuesto", style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle)
                Text(budgetText(r), style = MaterialTheme.typography.titleSmall, color = Vinko.Primary)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Detalle y propuesta
// ---------------------------------------------------------------------------------------------

@Composable
fun OpportunityDetailScreen(id: String, mode: String, nav: NavHostController) {
    val app = LocalApp.current
    val feed = rememberResource(id, mode) {
        app.ops.call("service_feed", "delivery_mode" to mode, "limit" to 100).objects().filter { it.s("request_id") == id }
    }
    val mine = rememberResource { app.ops.call("my_proposals") }
    val wallet = rememberResource { app.ops.call("my_wallet") }
    val cats = rememberCategories()

    ScreenScaffold("Oportunidad", onBack = { nav.popBackStack() }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            ResourceView(feed) { rows ->
                val r = rows.firstOrNull()
                if (r == null) {
                    EmptyBox(
                        "Esta solicitud ya no está disponible",
                        "Pudo ser asignada a otro profesional, cancelada por el cliente o vencida.",
                        "Ver oportunidades",
                    ) { nav.popBackStack() }
                } else {
                    val proposal = mine.data.objects().firstOrNull { it.s("request_id") == id }
                    OpportunityDetail(
                        r = r,
                        category = cats.data.orEmpty().firstOrNull { it.first == r.s("category_id") }?.second ?: "Servicio",
                        proposal = proposal,
                        balance = wallet.data.obj().l("balance"),
                        nav = nav,
                        onProposalSent = { mine.reload() },
                    )
                }
            }
        }
    }
}

@Composable
private fun OpportunityDetail(
    r: JsonObject,
    category: String,
    proposal: JsonObject?,
    balance: Long?,
    nav: NavHostController,
    onProposalSent: () -> Unit,
) {
    val app = LocalApp.current
    val action = rememberAction()
    var preview by remember { mutableStateOf<String?>(null) }
    var confirmAccept by remember { mutableStateOf(false) }
    val direct = r.s("mode") == "direct"
    val remote = r.s("delivery_mode") == "remote"
    val protectedPayment = r.b("requires_platform_payment")
    val requestId = r.s("request_id").orEmpty()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (direct) Pill("Invitación para ti", Vinko.Amber, Vinko.AmberBg, Icons.Outlined.Bolt)
            else Pill("Convocatoria abierta", icon = Icons.Outlined.Campaign)
            if (remote) Pill("Remoto", Vinko.Cyan, Vinko.CyanBg, Icons.Outlined.Public) else Pill("Presencial", Vinko.Cyan, Vinko.CyanBg, Icons.Outlined.LocationOn)
            if (protectedPayment) Pill("Pago protegido", Vinko.Mint, Vinko.MintBg, Icons.Outlined.Shield)
        }
        Column {
            Text(category.uppercase(), style = MaterialTheme.typography.labelSmall, color = Vinko.Primary)
            Spacer(Modifier.height(4.dp))
            Text(r.s("title") ?: "", style = MaterialTheme.typography.headlineSmall)
        }

        VCard {
            Text("Lo que necesita el cliente", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(r.s("description") ?: "", style = MaterialTheme.typography.bodyMedium)
            val photos = r.list("attachments").mapNotNull { it.s("file_id") }
            if (photos.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    photos.forEach { f ->
                        ProImage(f, Modifier.size(112.dp).clip(RoundedCornerShape(14.dp)).clickable { preview = f })
                    }
                }
            }
        }

        VCard {
            KeyValue("Presupuesto", budgetText(r), strong = true, valueColor = Vinko.Primary)
            KeyValue("Moneda de pago", if (r.s("currency")?.trim() == "USD") "Dólares (USD)" else "Bolívares (Bs)")
            KeyValue("Disponible hasta", dateTime(r.s("expires_at")))
            Spacer(Modifier.height(8.dp))
            LineDivider()
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconLine(
                    if (remote) Icons.Outlined.Public else Icons.Outlined.LocationOn,
                    if (remote) "Trabajo remoto: no requiere desplazamiento." else "Dentro de la zona de cobertura de tus sedes. La dirección exacta se comparte al formalizar el contrato.",
                    tint = Vinko.Lavender,
                )
                IconLine(
                    if (protectedPayment) Icons.Outlined.Shield else Icons.Outlined.Handshake,
                    if (protectedPayment) "Pago protegido: el cliente reserva el importe en la plataforma y se libera al confirmar la entrega."
                    else "Pago directo acordado con el cliente (Pago Móvil, transferencia o efectivo).",
                    tint = Vinko.Lavender,
                )
                IconLine(Icons.Outlined.Lock, "Los datos del cliente se comparten solo al formalizar el contrato.", tint = Vinko.Lavender)
            }
        }

        if (balance != null && balance <= 0) {
            Banner("No tienes Vinkos disponibles. Puedes enviar propuestas, pero necesitas al menos 1 Vinko para que te contraten.")
            SecondaryButton("Obtener Vinkos", onClick = { nav.navigate(Routes.WALLET) })
        } else {
            Banner(
                "Enviar tu propuesta es gratis. Si el cliente te contrata, se descuenta 1 Vinko de tu saldo" +
                    (balance?.let { " (tienes $it)" } ?: "") + ".",
                BannerTone.Info,
            )
        }

        when {
            proposal != null -> {
                Banner(
                    "Ya enviaste una propuesta de ${money(proposal.l("price_minor"), proposal.s("currency"))}. Estado: ${proposalLabel(proposal.s("status"))}.",
                    BannerTone.Success,
                )
                SecondaryButton("Ver mis propuestas", onClick = { nav.navigate(Routes.MY_PROPOSALS) })
            }
            direct && !r.b("requires_quote") -> {
                VCard {
                    Text("El cliente te eligió directamente", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Al aceptar se formaliza el contrato digital por ${budgetText(r)} y se habilitan el chat y los datos de contacto.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Vinko.Muted,
                    )
                }
                PrimaryButton("Aceptar invitación · 1 Vinko", loading = action.busy, onClick = { confirmAccept = true })
            }
            else -> ProposalForm(r, direct, onSent = onProposalSent)
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmAccept) {
        ConfirmDialog(
            title = "¿Aceptar esta contratación?",
            text = "Se formaliza el contrato digital con el cliente, se descuenta 1 Vinko de tu saldo y se habilitan el chat y los datos de contacto.",
            confirm = "Aceptar · 1 Vinko",
            onDismiss = { confirmAccept = false },
            onConfirm = {
                action.run(null) {
                    val contract = app.ops.call("accept_direct_request", "request" to requestId).idText()
                    if (contract != null) nav.navigate(Routes.contract(contract)) { popUpTo(OPPORTUNITY) { inclusive = true } }
                }
            },
        )
    }

    preview?.let { file ->
        Dialog(onDismissRequest = { preview = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                ProImage(file, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                IconButton(
                    onClick = { preview = null },
                    modifier = Modifier.statusBarsPadding().padding(12.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape),
                ) { Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = Color.White) }
            }
        }
    }
}

@Composable
private fun ProposalForm(r: JsonObject, direct: Boolean, onSent: () -> Unit) {
    val app = LocalApp.current
    val form = rememberFormAction()
    val currency = r.s("currency")?.trim()
    var price by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var days by rememberSaveable { mutableStateOf("7") }
    var tried by rememberSaveable { mutableStateOf(false) }
    val requestExpiry = parseInstant(r.s("expires_at"))
    // Fija la vigencia una sola vez por opción: un reintento envía exactamente los mismos datos
    // y el servidor devuelve la misma propuesta (idempotente) en lugar de duplicarla.
    val expiresAt = remember(days, r.s("expires_at")) {
        val wanted = nowInstant() + days.toLong().days
        val cap = requestExpiry?.minus(60.seconds)
        if (cap != null && wanted > cap) cap else wanted
    }
    val minor = parseMajor(price)
    val priceError = when {
        !tried -> null
        minor == null -> "Escribe tu precio, por ejemplo 45,00."
        minor <= 0 -> "El precio debe ser mayor que cero."
        else -> null
    }
    val messageError = if (tried && message.isBlank()) "Cuéntale al cliente cómo vas a resolverlo." else null

    VCard {
        Text(if (direct) "Enviar cotización" else "Enviar propuesta", style = MaterialTheme.typography.titleLarge)
        Text(
            "El cliente verá tu precio y tu mensaje, y confirmará el precio antes de asignarte el trabajo.",
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Muted,
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            form.error?.let { Banner(it) }
            MoneyField(
                price, { price = it }, currency, "Tu precio",
                supporting = "En la moneda elegida por el cliente. No se convierte entre Bs y USD.",
                error = priceError,
            )
            AreaField(
                message, { message = it.take(4000) }, "Mensaje para el cliente",
                minLines = 4,
                placeholder = "Cómo resolverás la necesidad, materiales incluidos, tiempo estimado y garantía.",
                error = messageError,
            )
            Text("Vigencia de tu propuesta", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
            ChoiceChips(listOf("3" to "3 días", "7" to "7 días", "14" to "14 días"), days, { days = it })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Vinko.Lavender, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Válida hasta el ${dateTime(expiresAt.iso())}" + if (requestExpiry != null && expiresAt < nowInstant() + days.toLong().days - 120.seconds) " (cierre de la solicitud)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Muted,
                )
            }
            PrimaryButton(if (direct) "Enviar cotización" else "Enviar propuesta", loading = form.busy, onClick = {
                tried = true
                val amount = parseMajor(price)
                if (amount == null || amount <= 0 || message.isBlank()) return@PrimaryButton
                form.run(if (direct) "Cotización enviada al cliente." else "Propuesta enviada al cliente.", onSuccess = onSent) {
                    app.ops.call(
                        "submit_proposal",
                        "request" to r.s("request_id"),
                        "message" to message.trim(),
                        "price_minor" to amount,
                        "expires_at" to expiresAt.iso(),
                    )
                }
            })
        }
    }
}

fun proposalLabel(status: String?): String = when (status) {
    "pending" -> "En espera del cliente"
    "accepted" -> "Aceptada"
    "rejected" -> "No seleccionada"
    "withdrawn" -> "Retirada"
    else -> status.orEmpty()
}
