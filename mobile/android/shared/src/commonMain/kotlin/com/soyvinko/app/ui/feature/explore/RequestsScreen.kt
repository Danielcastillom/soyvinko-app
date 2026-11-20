package com.soyvinko.app.ui.feature.explore

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.ErrorBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.StatusBadge
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.o
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberAction
import com.soyvinko.app.ui.common.rememberPhotoPicker
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.statusLabel
import com.soyvinko.app.ui.common.uploadFile
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

private val FILTERS = listOf(
    "all" to "Todas",
    "open" to "Publicadas",
    "draft" to "Borradores",
    "matched" to "Contratadas",
    "completed" to "Finalizadas",
    "closed" to "Canceladas o vencidas",
)

private fun JsonObject.inFilter(filter: String): Boolean = when (filter) {
    "all" -> true
    "closed" -> s("status") in setOf("cancelled", "expired")
    else -> s("status") == filter
}

fun JsonObject.budgetLabel(): String {
    val max = l("budget_max_minor")
    return if (max == null) "Por cotizar en ${currencyShort(s("currency"))}" else money(max, s("currency"))
}

/** Mis solicitudes (`my_requests`) con filtros por estado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(nav: NavHostController) {
    val app = LocalApp.current
    val rows = rememberResource { app.ops.call("my_requests").objects() }
    OnResume { rows.reload() }
    var filter by rememberSaveable { mutableStateOf("all") }
    ScreenScaffold(
        "Mis solicitudes",
        onBack = { if (!nav.popBackStack()) nav.navigate(Routes.HOME) },
        floatingAction = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.newRequest()) },
                containerColor = Vinko.Primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Nueva solicitud", style = MaterialTheme.typography.labelLarge) },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = rows.loading && rows.data != null,
            onRefresh = rows.reload,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            ResourceView(rows) { list ->
                if (list.isEmpty()) {
                    EmptyBox(
                        "De una necesidad a una solución",
                        "Publica tu primera solicitud y encuentra a la persona indicada.",
                        "Crear solicitud",
                    ) { nav.navigate(Routes.newRequest()) }
                    return@ResourceView
                }
                val visible = list.filter { it.inFilter(filter) }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            "Tus necesidades y las propuestas recibidas. Cada solicitud se cierra al asignar un profesional.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Vinko.Muted,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(FILTERS.filter { (k, _) -> k == "all" || k == filter || list.any { it.inFilter(k) } }) { (k, label) ->
                                Pill("$label (${list.count { it.inFilter(k) }})", filter == k, { filter = k })
                            }
                        }
                    }
                    if (visible.isEmpty()) item {
                        EmptyBox("No hay solicitudes en este estado", "Prueba con otro filtro.")
                    }
                    items(visible, key = { it.s("id").orEmpty() }) { r ->
                        Box(Modifier.padding(horizontal = 16.dp)) { RequestCard(r) { nav.navigate(Routes.request(r.s("id").orEmpty())) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestCard(r: JsonObject, onClick: () -> Unit) {
    VCard(Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(if (r.s("moderation_status") == "rejected") "rejected" else r.s("status"))
            Spacer(Modifier.weight(1f))
            val remote = r.s("delivery_mode") == "remote"
            Icon(if (remote) Icons.Outlined.Public else Icons.Outlined.Place, contentDescription = null, tint = Vinko.Lavender, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (remote) "Remoto" else "Presencial", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
        Spacer(Modifier.height(10.dp))
        Text(r.s("title").orEmpty(), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(r.s("description").orEmpty(), style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.budgetLabel(), style = MaterialTheme.typography.titleSmall, color = Vinko.Ink)
                Text(
                    "${date(r.s("created_at"))} · ${if (r.s("mode") == "direct") "Invitación directa" else "Convocatoria abierta"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Subtle,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Ver solicitud", tint = Vinko.Primary)
        }
    }
}

/** Detalle de una solicitud: fotos, propuestas, aceptar, publicar, cancelar y enlace al contrato. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(nav: NavHostController, id: String) {
    val app = LocalApp.current
    val data = rememberResource(id) { Found(app.ops.call("my_requests").objects().firstOrNull { it.s("id") == id }) }
    val proposals = rememberResource(id) { app.ops.call("request_proposals", "request" to id).objects() }
    val cats = rememberResource { app.ops.call("catalog").categories() }
    OnResume {
        data.reload()
        proposals.reload()
    }
    val action = rememberAction()
    var cancelOpen by remember { mutableStateOf(false) }
    var accepting by remember { mutableStateOf<JsonObject?>(null) }
    val addPhoto = rememberPhotoPicker(max = 1) { parts ->
        val part = parts.firstOrNull() ?: return@rememberPhotoPicker
        action.run("Foto adjunta.") {
            val ready = withContext(Dispatchers.Default) { normalizePhoto(part) }
                ?: throw ApiException("Usa fotos JPG, PNG o WEBP de hasta 10 MB cada una.")
            val file = app.uploadFile(ready, "request")
            app.ops.call("attach_request_file", "request" to id, "file" to file)
            data.reload()
        }
    }

    ScreenScaffold(
        "Solicitud",
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = {
                data.reload()
                proposals.reload()
            }) { Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar") }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = (data.loading && data.data != null) || (proposals.loading && proposals.data != null),
            onRefresh = {
                data.reload()
                proposals.reload()
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            ResourceView(data) { found ->
                val r = found.value
                if (r == null) {
                    EmptyBox("Solicitud no disponible", "Puede que ya no exista o que pertenezca a otra cuenta.", "Mis solicitudes") {
                        nav.navigate(Routes.REQUESTS) { popUpTo(Routes.REQUESTS) { inclusive = true } }
                    }
                    return@ResourceView
                }
                val status = r.s("status")
                val moderation = r.s("moderation_status")
                val direct = r.s("mode") == "direct"
                val draft = status == "draft"
                val contract = r.s("contract_id")
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        Column {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(status)
                                if (moderation != null && moderation != "approved") StatusBadge(moderation)
                                ModeChip(r.s("delivery_mode"))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(r.s("title").orEmpty(), style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "${if (r.s("delivery_mode") == "remote") "Proyecto remoto" else "Servicio presencial"} · " +
                                    if (r.l("budget_max_minor") == null) "Cotización en ${currencyShort(r.s("currency"))}" else money(r.l("budget_max_minor"), r.s("currency")),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Vinko.Muted,
                            )
                        }
                    }
                    if (moderation == "rejected") item {
                        Banner("La administración desautorizó esta solicitud. No está disponible para los profesionales.")
                    }
                    if (status == "open" && moderation == "approved") item {
                        Banner(
                            if (direct) "Solicitud enviada al profesional. Esperando su cotización."
                            else "Solicitud publicada. Esperando propuestas de los profesionales.",
                            BannerTone.Success,
                        )
                    }
                    if (contract != null) item {
                        VCard {
                            Text("Profesional asignado", style = MaterialTheme.typography.titleMedium)
                            Text("Esta solicitud ya no recibe propuestas. Continúa en el servicio contratado.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                            Spacer(Modifier.height(12.dp))
                            PrimaryButton("Ver servicio contratado", onClick = { nav.navigate(Routes.contract(contract)) })
                        }
                    }
                    item {
                        VCard {
                            Text("Lo que necesitas", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(r.s("description").orEmpty(), style = MaterialTheme.typography.bodyMedium)
                            val photos = r.list("attachments")
                            if (photos.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                PhotoGrid(photos.size) { i ->
                                    val photo = photos[i]
                                    Box(Modifier.fillMaxSize()) {
                                        NetImage(photo.s("file_id"), Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Vinko.Tint), description = "Foto de referencia ${i + 1}")
                                        photo.s("moderation_status")?.takeIf { it != "approved" }?.let {
                                            Box(Modifier.align(Alignment.BottomStart).padding(4.dp)) { StatusBadge(it) }
                                        }
                                        if (draft) {
                                            Box(
                                                Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.55f))
                                                    .clickable(enabled = !action.busy) {
                                                        action.run("Foto quitada.") {
                                                            app.ops.call("remove_request_image", "request" to id, "file" to photo.s("file_id"))
                                                            data.reload()
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) { Icon(Icons.Outlined.Close, contentDescription = "Quitar foto ${i + 1}", tint = Color.White, modifier = Modifier.size(16.dp)) }
                                        }
                                    }
                                }
                            }
                            if (draft && photos.size < 6) {
                                Spacer(Modifier.height(10.dp))
                                TextButton(onClick = addPhoto, enabled = !action.busy) {
                                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Añadir una foto de referencia", color = Vinko.Primary)
                                }
                            }
                            if ((draft && moderation != "rejected") || status in setOf("draft", "open")) Spacer(Modifier.height(12.dp))
                            if (draft && moderation != "rejected") {
                                PrimaryButton(
                                    if (direct) "Enviar al profesional" else "Publicar solicitud",
                                    loading = action.busy,
                                    onClick = {
                                        action.run(if (direct) "Solicitud enviada al profesional." else "Solicitud publicada.") {
                                            app.ops.call("publish_request", "request" to id)
                                            data.reload()
                                        }
                                    },
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            if (status in setOf("draft", "open")) {
                                SecondaryButton("Cancelar solicitud", onClick = { cancelOpen = true }, enabled = !action.busy)
                            }
                        }
                    }
                    item {
                        Text(
                            if (direct) "Cotización del profesional" else "Propuestas recibidas",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    val offers = proposals.data
                    when {
                        offers == null && proposals.loading -> item {
                            Text("Cargando propuestas…", style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
                        }
                        offers == null -> item { ErrorBox(proposals.error ?: "Inténtalo de nuevo.", proposals.reload) }
                        offers.isEmpty() -> item {
                            VCard {
                                Text(
                                    if (contract != null) "La solicitud ya está asignada" else "Aquí llegarán las propuestas",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    when {
                                        contract != null -> "Puedes continuar en el servicio contratado."
                                        direct -> "El profesional enviará el precio en la moneda que elegiste. Tú decides si lo aceptas."
                                        else -> "Podrás recibir varias propuestas y elegir una. Al asignar el trabajo, se cierran las demás ofertas."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Vinko.Muted,
                                )
                            }
                        }
                        else -> items(offers, key = { it.s("id").orEmpty() }) { p ->
                            val acceptable = p.s("status") == "pending" && status == "open" && moderation == "approved" && isFuture(p.s("expires_at"))
                            ProposalCard(
                                p,
                                acceptable = acceptable,
                                busy = action.busy,
                                onProfessional = { pid -> nav.navigate(Routes.professional(pid)) },
                                onAccept = { accepting = p },
                            )
                        }
                    }
                    item { RequestDetails(r, cats.data.orEmpty()) }
                }
            }
        }
    }

    accepting?.let { p ->
        val name = p.o("professional")?.s("name") ?: "este profesional"
        AlertDialog(
            onDismissRequest = { accepting = null },
            containerColor = Color.White,
            title = { Text("Elegir profesional") },
            text = {
                Text(
                    "¿Contratar a $name por ${money(p.l("price_minor"), p.s("currency"))}? Se abrirá el chat entre ambos y la solicitud dejará de recibir ofertas.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(enabled = !action.busy, onClick = {
                    action.run(null) {
                        val contractId = app.ops.call("accept_proposal", "proposal" to p.s("id")).jsonPrimitive.content
                        accepting = null
                        nav.navigate(Routes.contract(contractId))
                    }
                }) { Text("Contratar", color = Vinko.Primary) }
            },
            dismissButton = { TextButton(onClick = { accepting = null }) { Text("Volver", color = Vinko.Muted) } },
        )
    }
    if (cancelOpen) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { cancelOpen = false },
            containerColor = Color.White,
            title = { Text("Cancelar solicitud") },
            text = {
                Column {
                    Text(
                        "Los profesionales dejarán de verla y las propuestas pendientes se cerrarán.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Vinko.Muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    AreaField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = "Motivo",
                        supporting = "Describe el motivo con al menos 10 caracteres.",
                        maxLength = 1000,
                        minLines = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = reason.trim().length >= 10 && !action.busy,
                    onClick = {
                        action.run("Solicitud cancelada.") {
                            app.ops.call("cancel_request", "request" to id, "reason" to reason.trim())
                            cancelOpen = false
                            data.reload()
                            proposals.reload()
                        }
                    },
                ) { Text("Cancelar solicitud", color = if (reason.trim().length >= 10) Vinko.Coral else Vinko.Subtle) }
            },
            dismissButton = { TextButton(onClick = { cancelOpen = false }) { Text("Volver", color = Vinko.Muted) } },
        )
    }
}

@Composable
private fun ProposalCard(p: JsonObject, acceptable: Boolean, busy: Boolean, onProfessional: (String) -> Unit, onAccept: () -> Unit) {
    val pro = p.o("professional")?.toPro()
    VCard {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(enabled = pro != null) { pro?.let { onProfessional(it.id) } },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(pro?.initials ?: "P", 44.dp, border = false)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        pro?.name ?: "Profesional",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (pro?.verified == true) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Outlined.Verified, contentDescription = "Verificado", tint = Vinko.Mint, modifier = Modifier.size(16.dp))
                    }
                }
                if (pro != null) RatingRow(pro.rating, pro.reviewCount)
            }
            StatusBadge(p.s("status"))
        }
        p.s("message")?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Precio propuesto", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                Text(money(p.l("price_minor"), p.s("currency")), style = MaterialTheme.typography.titleLarge, color = Vinko.Primary)
            }
            p.s("expires_at")?.let {
                Text(
                    if (isFuture(it)) "Válida hasta ${date(it)}" else "Venció el ${date(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFuture(it)) Vinko.Subtle else Vinko.Coral,
                )
            }
        }
        if (acceptable) {
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Elegir profesional", onClick = onAccept, enabled = !busy)
        }
    }
}

@Composable
private fun RequestDetails(r: JsonObject, categories: List<Category>) {
    VCard(Modifier.padding(top = 6.dp)) {
        Text("Detalles", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        val direct = r.s("mode") == "direct"
        InfoRow("Modalidad", statusLabel(r.s("delivery_mode")))
        InfoRow("Tipo", if (direct) "Invitación a un profesional" else "Convocatoria abierta")
        categories.firstOrNull { it.id == r.s("category_id") }?.let { InfoRow("Categoría", it.name) }
        r.o("offering_snapshot")?.let { snap ->
            snap.s("title")?.let { InfoRow("Servicio", it) }
            snap.l("price_min_minor")?.let { InfoRow("Precio anunciado", "Desde ${money(it, snap.s("currency"))}") }
        }
        InfoRow("Moneda", if (r.s("currency") == "USD") "USD · Dólar" else "Bs · Bolívar")
        InfoRow("Presupuesto", r.budgetLabel())
        if (!direct && r.s("delivery_mode") == "onsite") r.l("search_radius_m")?.let { InfoRow("Radio de búsqueda", "${it / 1000} km") }
        InfoRow("Creada", date(r.s("created_at")))
        if (r.s("contract_id") == null && r.s("status") in setOf("draft", "open")) InfoRow("Disponible hasta", date(r.s("expires_at")))
        if (r.b("requires_platform_payment")) {
            Spacer(Modifier.height(8.dp))
            Banner("Pago protegido: el saldo se reserva después de contratar.", BannerTone.Info)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (direct) "Invitación a un profesional específico." else "Convocatoria para los profesionales de la categoría.",
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Muted,
        )
    }
}
