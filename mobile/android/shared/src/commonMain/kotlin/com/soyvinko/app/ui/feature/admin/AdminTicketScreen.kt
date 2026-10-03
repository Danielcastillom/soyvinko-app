package com.soyvinko.app.ui.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.dateTime
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

const val TICKET_REASON = "Consulta para atender el caso desde el panel de soporte."

private sealed interface TicketDlg {
    data object Manage : TicketDlg
    data object Refund : TicketDlg
    data object Escrow : TicketDlg
}

/**
 * Atender un caso (api.support_ticket con motivo, igual que la web): hilo con notas internas, respuesta
 * al usuario (reply_ticket), gestión de estado/asignación (manage_ticket) y devoluciones si hay servicio vinculado.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminTicketScreen(id: String, onBack: () -> Unit) {
    val app = LocalApp.current
    val user = LocalUser.current
    val ticket = rememberResource(id) {
        app.ops.call("support_ticket", "ticket" to id, "reason" to TICKET_REASON).obj()
            ?: throw ApiException("No encontramos ese caso de soporte.", "ticket_not_found", 404)
    }
    val t = ticket.data
    val canManage = user.can("support.manage")
    val closed = t?.s("status") == "closed"
    var dlg by remember { mutableStateOf<TicketDlg?>(null) }
    var reply by rememberSaveable { mutableStateOf("") }
    var replyError by remember { mutableStateOf<String?>(null) }
    val send = rememberAdminOp()

    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            "Atender caso",
            onBack = onBack,
            actions = { IconButton(onClick = ticket.reload) { Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar", tint = Vinko.Primary) } },
            bottomBar = {
                if (t != null && canManage && !closed) {
                    Column(Modifier.background(Color.White).imePadding().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp)) {
                        replyError?.let {
                            Banner(it)
                            Spacer(Modifier.height(8.dp))
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            OutlinedTextField(
                                value = reply,
                                onValueChange = { reply = it.take(10_000); replyError = null },
                                placeholder = { Text("Respuesta al usuario", color = Vinko.Subtle) },
                                modifier = Modifier.weight(1f),
                                maxLines = 5,
                                shape = RoundedCornerShape(22.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                colors = adminFieldColors(),
                            )
                            Spacer(Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = {
                                    val body = reply.trim()
                                    send.run("Respuesta enviada.", { replyError = it }, after = { reply = ""; ticket.reload() }) {
                                        app.ops.call("reply_ticket", "ticket" to id, "body" to body)
                                    }
                                },
                                enabled = reply.isNotBlank() && !send.busy,
                                modifier = Modifier.size(52.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Vinko.Primary, disabledContainerColor = Vinko.Lilac),
                            ) {
                                if (send.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                else Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Enviar respuesta", tint = Color.White)
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                ResourceView(ticket) { tk ->
                    val messages = tk.list("messages")
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(key = "header") { TicketHeader(tk) }
                        val contract = tk.s("contract_id")
                        val canRefund = contract != null && user.can("credits.refund")
                        val canEscrow = contract != null && user.can("money.refund")
                        if (canManage || canRefund || canEscrow) item(key = "actions") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (canManage) CardAction("Gestionar caso", { dlg = TicketDlg.Manage }, Icons.Outlined.ManageAccounts, primary = true)
                                if (canRefund) CardAction("Devolver crédito", { dlg = TicketDlg.Refund }, Icons.Outlined.Replay)
                                if (canEscrow) CardAction("Resolver devolución de saldo", { dlg = TicketDlg.Escrow }, Icons.Outlined.AccountBalanceWallet)
                            }
                        }
                        item(key = "thread-label") {
                            Text("Conversación (${messages.size})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                        }
                        if (messages.isEmpty()) item(key = "empty") {
                            Banner("Aún no hay mensajes en este caso. Tu respuesta llegará al usuario por la app.", BannerTone.Info)
                        }
                        items(messages, key = { it.s("id") ?: it.hashCode().toString() }) { m ->
                            Message(m, fromUser = m.s("author_id") == tk.s("opened_by"), mine = m.s("author_id") == user.id)
                        }
                        if (closed) item(key = "closed") {
                            Banner("Este caso está cerrado. Puedes reabrirlo desde “Gestionar caso”.", BannerTone.Info)
                        }
                    }
                }
            }
        }
        if (t != null) when (dlg) {
            TicketDlg.Manage -> ManageTicketSheet(t, onDismiss = { dlg = null }, onDone = { dlg = null; ticket.reload() })
            TicketDlg.Refund -> ReasonSheet(
                "Devolver crédito", "Devolver crédito", 10, "Crédito devuelto al profesional.", { dlg = null }, { dlg = null; ticket.reload() },
                subtitle = "Devuelve al profesional el crédito consumido por el servicio ${shortId(t.s("contract_id"))}. Solo aplica a servicios cancelados o en disputa.",
            ) { reason -> app.ops.call("refund_contract_credit", "contract" to t.s("contract_id"), "ticket" to id, "reason" to reason) }
            TicketDlg.Escrow -> ReasonSheet(
                "Resolver devolución de saldo", "Devolver saldo", 10, "Devolución de saldo resuelta.", { dlg = null }, { dlg = null; ticket.reload() },
                subtitle = "Devuelve al cliente los fondos en custodia del servicio ${shortId(t.s("contract_id"))}. Solo aplica a servicios en disputa o cancelados.",
                destructive = true,
            ) { reason -> app.ops.call("resolve_escrow_refund", "contract" to t.s("contract_id"), "ticket" to id, "reason" to reason) }
            null -> Unit
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TicketHeader(t: JsonObject) {
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(TICKET_CATEGORIES[t.s("category")] ?: "Otro tema", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted, modifier = Modifier.weight(1f))
            AdminBadge("tickets", t.s("status"))
        }
        Spacer(Modifier.height(8.dp))
        Text(t.s("subject").orEmpty(), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(t.s("description").orEmpty(), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            priorityLabel(t.s("priority"))?.let {
                val hot = t.s("priority") in setOf("high", "urgent")
                Tag(it, fg = if (hot) Vinko.Coral else Vinko.Primary, bg = if (hot) Vinko.CoralBg else Vinko.Tint)
            }
            t.s("contract_id")?.let { Tag("Servicio ${shortId(it)}") }
            t.s("order_id")?.let { Tag("Pedido ${shortId(it)}") }
        }
        Spacer(Modifier.height(8.dp))
        Field("Abierto", dateTime(t.s("created_at")))
        Field("Asignado a", t.s("assigned_to")?.let { if (it == LocalUser.current.id) "Ti" else shortId(it) } ?: "Sin asignar")
        t.s("resolved_at")?.let { Field("Resuelto", dateTime(it)) }
    }
}

private fun bubbleShape(right: Boolean) =
    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (right) 18.dp else 4.dp, bottomEnd = if (right) 4.dp else 18.dp)

@Composable
private fun Message(m: JsonObject, fromUser: Boolean, mine: Boolean) {
    if (m.b("internal_only")) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Vinko.AmberBg, RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFFF3E2A9), RoundedCornerShape(14.dp))
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Vinko.Amber, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nota interna", style = MaterialTheme.typography.labelMedium, color = Vinko.Amber, modifier = Modifier.weight(1f))
                Text(dateTime(m.s("created_at")), style = MaterialTheme.typography.bodySmall, color = Vinko.Amber)
            }
            Spacer(Modifier.height(4.dp))
            Text(m.s("body").orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Vinko.Ink)
        }
        return
    }
    val right = !fromUser
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (right) Alignment.End else Alignment.Start) {
        Text(
            when {
                fromUser -> "Usuario"
                mine -> "Tú · Equipo SoyVinko"
                else -> "Equipo SoyVinko"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (right) Vinko.Primary else Vinko.Mint,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .background(if (right) Vinko.Primary else Color.White, bubbleShape(right))
                .border(1.dp, if (right) Vinko.Primary else Vinko.Line, bubbleShape(right))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(m.s("body").orEmpty(), style = MaterialTheme.typography.bodyMedium, color = if (right) Color.White else Vinko.Ink)
            Spacer(Modifier.height(4.dp))
            Text(dateTime(m.s("created_at")), style = MaterialTheme.typography.bodySmall, color = if (right) Vinko.Lilac else Vinko.Subtle)
        }
    }
}

/** Gestionar caso: estado, asignación y nota interna obligatoria (api.manage_ticket). */
@Composable
private fun ManageTicketSheet(t: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    val me = LocalUser.current.id
    var status by remember { mutableStateOf(t.s("status").takeUnless { it == "open" } ?: "in_progress") }
    var assignMe by remember { mutableStateOf(true) }
    val current = t.s("assigned_to")
    ReasonSheet(
        "Gestionar caso", "Guardar gestión", 5, "Caso actualizado.", onDismiss, onDone,
        subtitle = t.s("subject"),
        reasonLabel = "Nota interna de gestión",
        reasonHint = "Solo la ve el equipo",
        extra = {
            FieldLabel("Estado del caso")
            ChoiceGroup(TICKET_STATUS, status) { status = it }
            SwitchRow(
                "Asignarme este caso",
                when {
                    assignMe -> "Quedará asignado a ti."
                    current == null -> "Seguirá sin asignar."
                    current == me -> "Se quitará tu asignación."
                    else -> "Se mantiene la asignación actual."
                },
                assignMe,
            ) { assignMe = it }
        },
    ) { note ->
        val assignee: Any = when {
            assignMe -> me
            current != null && current != me -> current
            else -> JsonNull
        }
        app.ops.call("manage_ticket", "ticket" to t.s("id"), "status" to status, "assignee" to assignee, "note" to note)
    }
}
