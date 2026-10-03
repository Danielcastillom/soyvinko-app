package com.soyvinko.app.ui.feature.profile

import com.soyvinko.app.ui.common.sendEmail
import coil3.compose.LocalPlatformContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.dateTime
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

val TICKET_CATEGORIES = listOf(
    "service" to "Un servicio",
    "credits" to "Créditos o pagos",
    "kyc" to "Verificación",
    "marketplace" to "Una compra",
    "account" to "Mi cuenta",
    "abuse" to "Reportar abuso",
    "other" to "Otro tema",
)

private val TICKET_STATUS = mapOf(
    "open" to Triple("Abierto", Vinko.Primary, Vinko.Tint),
    "in_progress" to Triple("En atención", Vinko.Cyan, Vinko.CyanBg),
    "waiting_user" to Triple("Esperando tu respuesta", Vinko.Amber, Vinko.AmberBg),
    "resolved" to Triple("Resuelto", Vinko.Mint, Vinko.MintBg),
    "closed" to Triple("Cerrado", Vinko.Muted, Vinko.Line),
)

@Composable
fun TicketBadge(status: String?) {
    val (label, fg, bg) = TICKET_STATUS[status] ?: Triple(status.orEmpty(), Vinko.Primary, Vinko.Tint)
    Pill(label, fg, bg)
}

private const val SUPPORT_EMAIL = "contacto@soyvinko.com"

/** Centro de soporte: casos abiertos por el usuario. */
@Composable
fun SupportScreen(onBack: () -> Unit, onNew: () -> Unit, onOpen: (String) -> Unit) {
    val app = LocalApp.current
    val context = LocalPlatformContext.current
    val snackbar = LocalSnackbar.current
    val tickets = rememberResource { app.ops.call("my_tickets").objects() }
    ScreenScaffold(
        "Soporte",
        onBack = onBack,
        floatingAction = {
            if (tickets.data?.isNotEmpty() == true) {
                ExtendedFloatingActionButton(
                    onClick = onNew,
                    containerColor = Vinko.Primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Nuevo caso") },
                )
            }
        },
    ) { padding ->
        ResourceView(tickets) { list ->
            ProfilePage(padding) {
                VCard {
                    Text("ESTAMOS PARA AYUDARTE", style = MaterialTheme.typography.labelSmall, color = Vinko.Primary)
                    Spacer(Modifier.height(6.dp))
                    Text("Centro de soporte", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Cuéntanos qué pasó. Cada caso tiene seguimiento y conversación propia con nuestro equipo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Vinko.Muted,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (!sendEmail(context, SUPPORT_EMAIL)) snackbar.notify("Escríbenos a $SUPPORT_EMAIL")
                        }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Email, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("También puedes escribirnos a $SUPPORT_EMAIL", style = MaterialTheme.typography.labelMedium, color = Vinko.Primary)
                    }
                }
                if (list.isEmpty()) {
                    EmptyBox(
                        "Estamos a un mensaje",
                        "Abre un caso cuando necesites ayuda con tu cuenta, tus pagos o tus servicios.",
                        "Crear caso",
                        onNew,
                    )
                } else {
                    SectionLabel("Mis casos")
                    list.forEach { t ->
                        VCard(Modifier.clip(RoundedCornerShape(18.dp)).clickable { t.s("id")?.let(onOpen) }) {
                            Row(verticalAlignment = Alignment.Top) {
                                com.soyvinko.app.ui.common.IconTile(Icons.Outlined.Forum)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(t.s("subject").orEmpty(), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${TICKET_CATEGORIES.toMap()[t.s("category")] ?: "Otro tema"} · ${date(t.s("created_at"))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Vinko.Muted,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    TicketBadge(t.s("status"))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(64.dp))
                }
            }
        }
    }
}

/** Nuevo caso de soporte. */
@Composable
fun NewTicketScreen(onBack: () -> Unit, onCreated: (String?) -> Unit) {
    val app = LocalApp.current
    val submit = rememberSubmitter()
    var category by rememberSaveable { mutableStateOf("service") }
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val valid = subject.trim().length >= 4 && description.trim().length >= 10
    ScreenScaffold("Nuevo caso", onBack = onBack) { padding ->
        ProfilePage(padding) {
            VCard {
                CardHeader(Icons.Outlined.SupportAgent, "¿Con qué te ayudamos?", "Elige el tema para que lo atienda la persona indicada.")
                Spacer(Modifier.height(12.dp))
                ChoiceChips(TICKET_CATEGORIES, category, { category = it })
            }
            VCard {
                VinkoTextField(
                    subject, { subject = it.take(140); submit.clear() }, "Asunto",
                    capitalization = KeyboardCapitalization.Sentences,
                    supporting = "Ej. El profesional no llegó a la cita",
                    error = submit.fieldErrors["subject"],
                )
                Spacer(Modifier.height(6.dp))
                AreaField(
                    description, { description = it; submit.clear() }, "¿Cómo podemos ayudarte?",
                    placeholder = "Describe qué pasó, cuándo y con qué servicio o pedido.",
                    minLines = 5,
                    maxLength = 5000,
                    supporting = if (description.isNotEmpty() && description.trim().length < 10) "Escribe al menos 10 caracteres." else null,
                    error = submit.fieldErrors["description"],
                )
            }
            if (category == "service" || category == "marketplace") {
                Banner("Si es sobre un servicio o una compra, indica la fecha y el nombre del profesional o comercio para ubicarlo rápido.", BannerTone.Info)
            }
            submit.error?.let { Banner(it) }
            PrimaryButton(
                "Enviar caso",
                loading = submit.busy,
                enabled = valid,
                onClick = {
                    submit.run("Caso creado. Te responderemos aquí.") {
                        val id = app.ops.call(
                            "open_ticket",
                            "category" to category,
                            "subject" to subject.trim(),
                            "description" to description.trim(),
                        ).let { (it as? JsonPrimitive)?.contentOrNull }
                        onCreated(id)
                    }
                },
            )
        }
    }
}

/** Conversación de un caso de soporte. */
@Composable
fun TicketScreen(id: String, onBack: () -> Unit) {
    val app = LocalApp.current
    val user = LocalUser.current
    val thread = rememberResource(id) { app.ops.call("ticket_thread", "ticket" to id).requireObj() }
    val submit = rememberSubmitter()
    var reply by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val messages = thread.data.list("messages")
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size) }
    val closed = thread.data?.s("status") == "closed"
    ScreenScaffold(
        "Caso de soporte",
        onBack = onBack,
        actions = {
            IconButton(onClick = thread.reload) { Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar", tint = Vinko.Primary) }
        },
        bottomBar = {
            if (thread.data != null && !closed) {
                Column(Modifier.background(Color.White).imePadding().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    submit.error?.let {
                        Banner(it)
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        OutlinedTextField(
                            value = reply,
                            onValueChange = { reply = it.take(10000); submit.clear() },
                            placeholder = { Text("Escribe tu mensaje", color = Vinko.Subtle) },
                            modifier = Modifier.weight(1f),
                            maxLines = 5,
                            shape = RoundedCornerShape(22.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = vinkoFieldColors(),
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                val body = reply.trim()
                                submit.run {
                                    app.ops.call("reply_ticket", "ticket" to id, "body" to body)
                                    reply = ""
                                    thread.reload()
                                }
                            },
                            enabled = reply.isNotBlank() && !submit.busy,
                            modifier = Modifier.size(52.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Vinko.Primary, disabledContainerColor = Vinko.Lilac),
                        ) { Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Enviar", tint = Color.White) }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            ResourceView(thread) { t ->
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "header") {
                        VCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    TICKET_CATEGORIES.toMap()[t.s("category")] ?: "Otro tema",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Vinko.Muted,
                                    modifier = Modifier.weight(1f),
                                )
                                TicketBadge(t.s("status"))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(t.s("subject").orEmpty(), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(t.s("description").orEmpty(), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Caso abierto el ${dateTime(t.s("created_at"))}", style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle)
                        }
                    }
                    if (messages.isEmpty()) {
                        item(key = "empty") {
                            Banner("Recibimos tu caso. Nuestro equipo te responderá aquí lo antes posible.", BannerTone.Info)
                        }
                    }
                    items(messages, key = { it.s("id") ?: it.hashCode().toString() }) { m ->
                        Bubble(m, mine = m.s("author_id") == user.id)
                    }
                    if (closed) {
                        item(key = "closed") {
                            Banner("Este caso está cerrado. Si necesitas más ayuda, abre un caso nuevo.", BannerTone.Info)
                        }
                    }
                }
            }
        }
    }
}

private fun bubbleShape(mine: Boolean) =
    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 4.dp, bottomEnd = if (mine) 4.dp else 18.dp)

@Composable
private fun Bubble(m: JsonObject, mine: Boolean) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Text(
            if (mine) "Tú" else "Equipo SoyVinko",
            style = MaterialTheme.typography.labelMedium,
            color = if (mine) Vinko.Primary else Vinko.Mint,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .background(if (mine) Vinko.Primary else Color.White, bubbleShape(mine))
                .border(1.dp, if (mine) Vinko.Primary else Vinko.Line, bubbleShape(mine))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(m.s("body").orEmpty(), style = MaterialTheme.typography.bodyMedium, color = if (mine) Color.White else Vinko.Ink)
            Spacer(Modifier.height(4.dp))
            Text(dateTime(m.s("created_at")), style = MaterialTheme.typography.bodySmall, color = if (mine) Vinko.Lilac else Vinko.Subtle)
        }
    }
}
