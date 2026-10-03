package com.soyvinko.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.StatusBadge
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberAction
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.relative
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.statusLabel
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject
import com.soyvinko.app.ui.common.Caracas
import com.soyvinko.app.ui.common.nowInstant
import kotlinx.datetime.toLocalDateTime

private fun greeting(): String {
    val hour = nowInstant().toLocalDateTime(Caracas).hour
    return when {
        hour < 12 -> "Buenos días"
        hour < 19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

private data class Tone(val fg: Color, val bg: Color)
private val Violet = Tone(Vinko.Primary, Vinko.Tint)
private val Mint = Tone(Vinko.Mint, Vinko.MintBg)
private val Cyan = Tone(Vinko.Cyan, Vinko.CyanBg)
private val Amber = Tone(Vinko.Amber, Vinko.AmberBg)
private val Coral = Tone(Vinko.Coral, Vinko.CoralBg)

/** Mismos textos, iconos y destinos que "Novedades" en el panel web (Dashboard.tsx). */
private data class NoticeInfo(val title: String, val icon: ImageVector, val tone: Tone, val route: (String) -> String)

private fun notice(topic: String?): NoticeInfo = when (topic) {
    "delivery.submitted" -> NoticeInfo("Tienes una entrega por revisar", Icons.Outlined.Inventory2, Violet) { Routes.contract(it) }
    "review.requested" -> NoticeInfo("Ya puedes calificar tu servicio", Icons.Outlined.Star, Amber) { Routes.contract(it) }
    "proposal.received" -> NoticeInfo("Recibiste una nueva propuesta", Icons.Outlined.PanTool, Mint) { Routes.request(it) }
    "contract.accepted" -> NoticeInfo("Tu servicio fue confirmado", Icons.Outlined.CheckCircle, Mint) { Routes.contract(it) }
    "contract.funded" -> NoticeInfo("El pago quedó protegido en custodia", Icons.Outlined.Shield, Violet) { Routes.contract(it) }
    "request.available" -> NoticeInfo("Hay una solicitud nueva en tu zona", Icons.Outlined.Search, Cyan) { Routes.OPPORTUNITIES }
    "chat.message" -> NoticeInfo("Tienes un mensaje nuevo", Icons.Outlined.ChatBubbleOutline, Cyan) { Routes.CONTRACTS }
    else -> NoticeInfo("Hay novedades en tu cuenta", Icons.Outlined.Notifications, Violet) { Routes.NOTIFICATIONS }
}

private data class Task(val key: String, val icon: ImageVector, val tone: Tone, val title: String, val text: String, val route: String, val cta: String)

/** Tareas derivadas del estado real de los contratos, igual que "Requiere tu atención" en la web. */
private fun tasksFrom(contracts: List<JsonObject>, notifications: List<JsonObject>): List<Task> {
    val tasks = mutableListOf<Task>()
    for (c in contracts) {
        val id = c.s("id") ?: continue
        val isCustomer = c.b("is_customer")
        val other = (if (isCustomer) c.s("professional_name") else c.s("customer_name")) ?: "la otra parte"
        val title = c.s("title") ?: "Servicio contratado"
        when {
            c.s("status") == "disputed" ->
                tasks += Task("d$id", Icons.Outlined.Gavel, Coral, "Hay una disputa abierta", "$title · con $other", Routes.contract(id), "Revisar")
            c.s("status") == "awaiting_confirmation" &&
                ((isCustomer && c.s("customer_finished_at") == null) || (c.b("is_professional") && c.s("professional_finished_at") == null)) ->
                tasks += Task("f$id", Icons.Outlined.DoneAll, Violet, "Confirma que el servicio terminó", "$title · $other ya lo marcó como finalizado", Routes.contract(id), "Confirmar")
            c.s("status") == "accepted" && c.b("is_professional") ->
                tasks += Task("s$id", Icons.Outlined.WorkOutline, Mint, "Tienes un servicio por iniciar", "$title · para $other", Routes.contract(id), "Ver")
            c.s("status") == "completed" && (c.l("reviews_count") ?: 0L) == 0L ->
                tasks += Task("r$id", Icons.Outlined.Star, Amber, "Califica tu experiencia", "$title · tu opinión ayuda a la comunidad", Routes.contract(id), "Calificar")
        }
    }
    for (n in notifications) {
        val target = n.s("aggregate_id") ?: continue
        if (n.s("read_at") == null && n.s("topic") == "delivery.submitted" && tasks.none { it.route == Routes.contract(target) }) {
            tasks += Task("n${n.s("id")}", Icons.Outlined.Inventory2, Violet, "Revisa la entrega recibida", "Aprueba el trabajo o solicita cambios", Routes.contract(target), "Revisar")
        }
    }
    return tasks.take(5)
}

private val STEPS = listOf("accepted", "in_progress", "awaiting_confirmation", "completed")
private fun stepOf(status: String?) = if (status in setOf("en_route", "arrived")) 1 else maxOf(STEPS.indexOf(status), 0)

/** "Mi resumen": paridad con el panel de la web. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(user: User, onUserUpdated: (User) -> Unit, onNavigate: (String) -> Unit) {
    val app = LocalApp.current
    val stats = rememberResource { app.ops.call("dashboard").obj() }
    val contracts = rememberResource { app.ops.call("my_contracts").objects() }
    val wallet = rememberResource { app.ops.call("my_wallet").obj() }
    val requests = rememberResource { app.ops.call("my_requests").objects() }
    val ves = rememberResource { app.ops.call("money_wallet", "currency" to "VES").obj() }
    val kyc = rememberResource { app.ops.call("my_kyc").objects() }
    val account = rememberResource { app.ops.call("my_account").obj() }
    val notifications = rememberResource { app.ops.call("my_notifications").objects() }
    val action = rememberAction()
    var filter by remember { mutableStateOf("active") }
    val reloadAll = {
        listOf(stats, contracts, wallet, requests, ves, kyc, account, notifications).forEach { it.reload() }
    }
    LaunchedEffect(Unit) {
        try { onUserUpdated(app.auth.me()) } catch (_: ApiException) { }
    }

    val allContracts = contracts.data.orEmpty()
    val unread = notifications.data.orEmpty().filter { it.s("read_at") == null }
    val tasks = tasksFrom(allContracts, notifications.data.orEmpty())
    val kycApproved = kyc.data.orEmpty().any { it.s("status") == "approved" }
    val kycPending = kyc.data.orEmpty().any { it.s("status") in setOf("pending", "in_review") }
    val setup = listOf(
        Triple(user.emailVerified, "Confirma tu correo" to "Protege tu cuenta y habilita los retiros.", Routes.SECURITY to Icons.Outlined.MarkEmailUnread),
        Triple(kycApproved, (if (kycPending) "Identidad en revisión" else "Verifica tu identidad") to (if (kycPending) "Te avisaremos al aprobarla." else "Necesaria para cobrar y retirar."), Routes.KYC to Icons.Outlined.VerifiedUser),
        Triple(account.data.list("addresses").isNotEmpty(), "Guarda una dirección" to "Encuentra profesionales cerca de ti.", Routes.ADDRESSES to Icons.Outlined.LocationOn),
        Triple(user.isProfessional, "Activa tu perfil profesional" to "Ofrece tus servicios y recibe 3 Vinkos.", Routes.PRO to Icons.Outlined.WorkOutline),
    )
    val setupDone = setup.count { it.first }
    val active = stats.data.l("active_contracts") ?: 0
    val shown = allContracts.filter {
        when (filter) {
            "completed" -> it.s("status") == "completed"
            "all" -> true
            else -> it.s("status") !in setOf("completed", "cancelled")
        }
    }

    PullToRefreshBox(
        isRefreshing = stats.loading && stats.data != null,
        onRefresh = reloadAll,
        modifier = Modifier.fillMaxSize().background(Vinko.Cream),
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Encabezado
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(HeroGradient, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(Color.White.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(user.name.trim().take(1).uppercase(), fontFamily = Manrope, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = Vinko.Lilac, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (user.isCompany) "Cuenta empresa" else if (user.isProfessional) "Cliente y profesional" else "Cuenta personal",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                    }
                    Box {
                        IconButton(onClick = { onNavigate(Routes.NOTIFICATIONS) }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Novedades", tint = Color.White)
                        }
                        if (unread.isNotEmpty()) {
                            Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp).background(Vinko.Coral, CircleShape), contentAlignment = Alignment.Center) {
                                Text("${minOf(unread.size, 9)}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("${greeting()}, ${user.firstName}.", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        stats.data == null -> "Cargando tu resumen…"
                        tasks.isNotEmpty() -> "Tienes ${tasks.size} ${if (tasks.size == 1) "tarea pendiente" else "tareas pendientes"} y $active ${if (active == 1L) "servicio" else "servicios"} en curso."
                        else -> "Todo está al día. ¿Qué quieres resolver hoy?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroButton("Nueva solicitud", Icons.Outlined.Add, filled = true, modifier = Modifier.weight(1f)) { onNavigate(Routes.newRequest()) }
                    HeroButton("Explorar", Icons.Outlined.Explore, filled = false, modifier = Modifier.weight(1f)) { onNavigate(Routes.EXPLORE) }
                }
            }

            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                stats.error?.let { Banner(it) }

                // Completa tu cuenta
                if (!kyc.loading && !account.loading && setupDone < setup.size) {
                    Card {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Completa tu cuenta", style = MaterialTheme.typography.titleMedium)
                                Text("$setupDone de ${setup.size} pasos listos", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Progress(setupDone.toFloat() / setup.size)
                        Spacer(Modifier.height(8.dp))
                        setup.forEach { (done, texts, target) ->
                            Row(
                                Modifier.fillMaxWidth().clickable(enabled = !done) { onNavigate(target.first) }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (done) Tile(Icons.Outlined.CheckCircle, Mint) else Tile(target.second, Violet)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(texts.first, style = MaterialTheme.typography.titleSmall, color = if (done) Vinko.Muted else Vinko.Ink)
                                    Text(if (done) "Listo" else texts.second, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                                }
                                if (!done) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Vinko.Lilac)
                            }
                        }
                    }
                }

                // Indicadores
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Kpi(Modifier.weight(1f), Icons.Outlined.Description, "Solicitudes abiertas", stats.data.l("requests")?.toString(),
                        requests.data?.let { "${it.size} en total" } ?: "", Violet) { onNavigate(Routes.REQUESTS) }
                    Kpi(Modifier.weight(1f), Icons.Outlined.WorkOutline, "Servicios en curso", stats.data.l("active_contracts")?.toString(),
                        "${stats.data.l("completed_contracts") ?: 0} finalizados", Mint) { onNavigate(Routes.CONTRACTS) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Kpi(Modifier.weight(1f), Icons.Outlined.AccountBalanceWallet, "Saldo disponible", ves.data?.let { money(it.l("available_minor") ?: 0, "VES") },
                        "${money(ves.data.l("reserved_for_services_minor") ?: 0, "VES")} en custodia", Cyan) { onNavigate(Routes.WALLET) }
                    Kpi(Modifier.weight(1f), Icons.Outlined.Toll, "Vinkos", (wallet.data.l("balance") ?: wallet.data.l("credits"))?.toString(),
                        if (user.isProfessional) "1 Vinko por contratación" else "Para recibir clientes", Amber) { onNavigate(Routes.WALLET) }
                }

                // Requiere tu atención
                if (tasks.isNotEmpty()) {
                    Card(border = Vinko.Lilac) {
                        SectionTitle("Requiere tu atención", tasks.size)
                        tasks.forEachIndexed { i, t ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onNavigate(t.route) }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Tile(t.icon, t.tone)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(t.title, style = MaterialTheme.typography.titleSmall)
                                    Text(t.text, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(t.cta, style = MaterialTheme.typography.labelMedium, color = Vinko.Primary,
                                    modifier = Modifier.background(Vinko.Tint, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                            if (i < tasks.lastIndex) Divider()
                        }
                    }
                }

                // Tus servicios
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tus servicios", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onNavigate(Routes.CONTRACTS) }) { Text("Ver todos", color = Vinko.Primary) }
                    }
                    Row(Modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(12.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("active" to "Activos", "completed" to "Finalizados", "all" to "Todos").forEach { (key, label) ->
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (filter == key) Vinko.Ink else Vinko.Muted,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (filter == key) Color.White else Color.Transparent, RoundedCornerShape(9.dp))
                                    .clickable { filter = key }
                                    .padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    when {
                        contracts.data == null && contracts.loading -> Text("Cargando…", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, modifier = Modifier.padding(vertical = 12.dp))
                        shown.isEmpty() -> Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (filter == "completed") "Aún no tienes servicios finalizados" else "Tu próximo proyecto empieza aquí", style = MaterialTheme.typography.titleSmall)
                            Text("Explora profesionales o publica lo que necesitas.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                            TextButton(onClick = { onNavigate(Routes.EXPLORE) }) { Text("Encontrar un profesional", color = Vinko.Primary) }
                        }
                        else -> shown.take(5).forEachIndexed { i, c ->
                            val other = (if (c.b("is_customer")) c.s("professional_name") else c.s("customer_name")) ?: "—"
                            Row(
                                Modifier.fillMaxWidth().clickable { c.s("id")?.let { onNavigate(Routes.contract(it)) } }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(42.dp).background(HeroGradient, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Text(other.trim().take(1).uppercase(), color = Color.White, fontFamily = Manrope, style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.s("title") ?: "Servicio contratado", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${if (c.b("is_customer")) "Profesional" else "Cliente"}: $other · ${statusLabel(c.s("delivery_mode"))}",
                                        style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    if (c.s("status") !in setOf("cancelled", "disputed")) {
                                        Spacer(Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.width(160.dp)) {
                                            STEPS.indices.forEach { s ->
                                                Box(Modifier.weight(1f).height(4.dp).background(if (s <= stepOf(c.s("status"))) Vinko.Primary else Vinko.Tint, RoundedCornerShape(4.dp)))
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusBadge(c.s("status"))
                                    Spacer(Modifier.height(4.dp))
                                    Text(money(c.l("agreed_price_minor"), c.s("currency")), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (i < minOf(shown.size, 5) - 1) Divider()
                        }
                    }
                }

                // Novedades
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Novedades", unread.size, Modifier.weight(1f))
                        if (unread.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    action.run(null) {
                                        unread.forEach { n -> app.ops.call("read_notification", "notification" to n.s("id")) }
                                        notifications.reload(); stats.reload()
                                    }
                                },
                                enabled = !action.busy,
                            ) { Text("Marcar todo leído", color = Vinko.Primary) }
                        }
                    }
                    val feed = notifications.data.orEmpty().take(5)
                    if (feed.isEmpty()) {
                        Text("Estás al día. Las novedades de tus servicios aparecerán aquí.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, modifier = Modifier.padding(vertical = 10.dp))
                    }
                    feed.forEachIndexed { i, n ->
                        val info = notice(n.s("topic"))
                        val isUnread = n.s("read_at") == null
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (isUnread) action.run(null) { app.ops.call("read_notification", "notification" to n.s("id")); notifications.reload() }
                                onNavigate(info.route(n.s("aggregate_id").orEmpty()))
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Tile(info.icon, info.tone, size = 36)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(info.title, style = if (isUnread) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium)
                                Text(relative(n.s("created_at")), style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                            }
                            if (isUnread) Box(Modifier.size(8.dp).background(Vinko.Primary, CircleShape))
                        }
                        if (i < feed.lastIndex) Divider()
                    }
                }

                // Tus solicitudes
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tus solicitudes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onNavigate(Routes.REQUESTS) }) { Text("Ver todas", color = Vinko.Primary) }
                    }
                    val recent = requests.data.orEmpty().take(3)
                    if (recent.isEmpty()) {
                        Text("Aún no publicas solicitudes. Publica la primera y recibe propuestas.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, modifier = Modifier.padding(vertical = 10.dp))
                    }
                    recent.forEachIndexed { i, r ->
                        Row(
                            Modifier.fillMaxWidth().clickable { r.s("id")?.let { onNavigate(Routes.request(it)) } }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(r.s("title") ?: "Solicitud", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (r.s("status") == "open" && r.s("expires_at") != null) "Vence ${date(r.s("expires_at"))}" else date(r.s("created_at")),
                                    style = MaterialTheme.typography.bodySmall, color = Vinko.Muted,
                                )
                            }
                            StatusBadge(r.s("status"))
                        }
                        if (i < recent.lastIndex) Divider()
                    }
                }

                // Accesos rápidos a todas las secciones
                Text("Más en SoyVinko", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
                Card(padding = 6) {
                    if (user.isProfessional) {
                        Shortcut(Icons.Outlined.Search, "Oportunidades", "Solicitudes de tu zona y remotas") { onNavigate(Routes.OPPORTUNITIES) }
                        Shortcut(Icons.Outlined.TaskAlt, "Mis propuestas", "Cotizaciones enviadas") { onNavigate(Routes.MY_PROPOSALS) }
                    }
                    Shortcut(Icons.Outlined.Storefront, "Marketplace", "Productos de comercios de confianza") { onNavigate(Routes.MARKETPLACE) }
                    Shortcut(Icons.Outlined.SupportAgent, "Soporte", "Ayuda y disputas", last = true) { onNavigate(Routes.SUPPORT) }
                }

                // Banner profesional
                Column(
                    Modifier.fillMaxWidth().background(HeroGradient, RoundedCornerShape(20.dp)).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        if (user.isProfessional) "Tu próximo cliente te está buscando." else "Lo que sabes hacer también vale.",
                        style = MaterialTheme.typography.titleLarge, color = Color.White,
                    )
                    Text(
                        if (user.isProfessional) "Tienes ${wallet.data.l("balance") ?: stats.data.l("credit_balance") ?: 0} Vinkos para nuevas contrataciones. Revisa las solicitudes de tu zona."
                        else "Activa tu perfil profesional, ofrece tus servicios y recibe 3 Vinkos de bienvenida.",
                        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(4.dp))
                    HeroButton(if (user.isProfessional) "Ver oportunidades" else "Activar mi perfil", Icons.Outlined.WorkOutline, filled = true) {
                        onNavigate(if (user.isProfessional) Routes.OPPORTUNITIES else Routes.PRO)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HeroButton(text: String, icon: ImageVector, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .background(if (filled) Color.White else Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = if (filled) 0f else 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) Vinko.Primary else Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (filled) Vinko.Primary else Color.White, maxLines = 1)
    }
}

@Composable
private fun Card(padding: Int = 16, border: Color = Vinko.Line, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(padding.dp),
    ) { content() }
}

@Composable
private fun SectionTitle(text: String, count: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge)
        if (count > 0) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(22.dp).background(Vinko.Primary, CircleShape), contentAlignment = Alignment.Center) {
                Text("$count", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    }
}

@Composable
private fun Progress(value: Float) {
    Box(Modifier.fillMaxWidth().height(8.dp).background(Vinko.Tint, RoundedCornerShape(8.dp))) {
        Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(8.dp).background(Vinko.Primary, RoundedCornerShape(8.dp)))
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(Vinko.Line))

@Composable
private fun Tile(icon: ImageVector, tone: Tone, size: Int = 40) {
    Box(Modifier.size(size.dp).background(tone.bg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tone.fg, modifier = Modifier.size((size / 2).dp))
    }
}

@Composable
private fun Kpi(modifier: Modifier, icon: ImageVector, label: String, value: String?, sub: String, tone: Tone, onClick: () -> Unit) {
    Column(
        modifier
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Tile(icon, tone)
        Spacer(Modifier.height(10.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Vinko.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value ?: "—", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Shortcut(icon: ImageVector, title: String, subtitle: String, last: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Tile(icon, Violet)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Vinko.Lilac, modifier = Modifier.size(20.dp))
    }
    if (!last) Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(1.dp).background(Vinko.Line))
}
