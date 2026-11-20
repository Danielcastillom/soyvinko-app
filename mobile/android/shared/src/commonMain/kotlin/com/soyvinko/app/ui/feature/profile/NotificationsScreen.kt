package com.soyvinko.app.ui.feature.profile

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.relative
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/** Cómo se presenta cada tema de notificación (igual que el panel de la web). */
data class NotificationInfo(
    val title: String,
    val text: String,
    val icon: ImageVector,
    val fg: Color,
    val bg: Color,
)

fun notificationInfo(topic: String?): NotificationInfo = when (topic) {
    "delivery.submitted" -> NotificationInfo("Tienes una entrega por revisar", "Revisa el trabajo y confírmalo para liberar el pago.", Icons.Outlined.Inventory2, Vinko.Primary, Vinko.Tint)
    "review.requested" -> NotificationInfo("Ya puedes calificar tu servicio", "Cuéntale a la comunidad cómo te fue.", Icons.Outlined.StarOutline, Vinko.Amber, Vinko.AmberBg)
    "proposal.received" -> NotificationInfo("Recibiste una nueva propuesta", "Compárala y elige al profesional que prefieras.", Icons.Outlined.PanTool, Vinko.Mint, Vinko.MintBg)
    "contract.accepted" -> NotificationInfo("Tu servicio fue confirmado", "Ya hay un acuerdo con el profesional.", Icons.Outlined.CheckCircle, Vinko.Mint, Vinko.MintBg)
    "contract.funded" -> NotificationInfo("El pago quedó protegido en custodia", "El dinero se libera cuando confirmes el trabajo.", Icons.Outlined.Shield, Vinko.Primary, Vinko.Tint)
    "request.available" -> NotificationInfo("Hay una solicitud nueva en tu zona", "Envía tu propuesta antes que otros profesionales.", Icons.Outlined.Search, Vinko.Cyan, Vinko.CyanBg)
    "chat.message" -> NotificationInfo("Tienes un mensaje nuevo", "Abre tus servicios para leer la conversación.", Icons.Outlined.ChatBubbleOutline, Vinko.Cyan, Vinko.CyanBg)
    else -> if (topic != null && (topic.startsWith("contract.") || topic.startsWith("delivery.") || topic.startsWith("review.")))
        NotificationInfo("Hay novedades en tu servicio", "Abre el servicio para ver los detalles.", Icons.Outlined.Notifications, Vinko.Primary, Vinko.Tint)
    else NotificationInfo("Hay novedades en tu cuenta", "Revisa tu actividad reciente.", Icons.Outlined.Notifications, Vinko.Primary, Vinko.Tint)
}

/** Ruta a la que lleva una notificación; null si no tiene destino propio. */
fun notificationRoute(topic: String?, aggregateId: String?): String? {
    val t = topic.orEmpty()
    return when {
        t == "proposal.received" && aggregateId != null -> Routes.request(aggregateId)
        t == "request.available" -> Routes.OPPORTUNITIES
        t == "chat.message" -> Routes.CONTRACTS
        (t.startsWith("contract.") || t.startsWith("delivery.") || t.startsWith("review.")) && aggregateId != null -> Routes.contract(aggregateId)
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val notifications = rememberResource { app.ops.call("my_notifications").objects() }
    var readLocally by remember { mutableStateOf(emptySet<String>()) }
    var marking by remember { mutableStateOf(false) }
    val all = notifications.data.orEmpty()
    fun isUnread(n: JsonObject) = n.s("read_at") == null && n.s("id") !in readLocally
    val unread = all.filter(::isUnread)

    fun markAll() {
        if (marking) return
        scope.launch {
            marking = true
            try {
                coroutineScope {
                    unread.map { n -> async { runCatching { app.ops.call("read_notification", "notification" to n.s("id")) } } }.awaitAll()
                }
                readLocally = readLocally + unread.mapNotNull { it.s("id") }
                snackbar.notify("Todo está marcado como leído.")
                notifications.reload()
            } finally {
                marking = false
            }
        }
    }

    fun open(n: JsonObject) {
        val id = n.s("id")
        if (isUnread(n) && id != null) {
            readLocally = readLocally + id
            fireAndForget { app.ops.call("read_notification", "notification" to id) }
        }
        notificationRoute(n.s("topic"), n.s("aggregate_id"))?.let(onOpen)
    }

    ScreenScaffold(
        "Novedades",
        onBack = onBack,
        actions = {
            if (unread.isNotEmpty()) {
                TextButton(onClick = ::markAll, enabled = !marking) {
                    Icon(Icons.Outlined.DoneAll, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Marcar todo como leído", color = Vinko.Primary, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = notifications.loading && notifications.data != null,
            onRefresh = notifications.reload,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            ResourceView(notifications) { list ->
                if (list.isEmpty()) {
                    EmptyBox("Estás al día", "Las novedades de tus servicios, propuestas y mensajes aparecerán aquí.")
                } else {
                    val (fresh, old) = list.partition(::isUnread)
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (fresh.isNotEmpty()) {
                            item(key = "h-new") { Box(Modifier.padding(top = 4.dp)) { SectionLabel("Sin leer · ${fresh.size}") } }
                            items(fresh, key = { it.s("id") ?: it.hashCode().toString() }) { NotificationRow(it, unread = true) { open(it) } }
                        }
                        if (old.isNotEmpty()) {
                            item(key = "h-old") { Box(Modifier.padding(top = 8.dp)) { SectionLabel("Anteriores") } }
                            items(old, key = { it.s("id") ?: it.hashCode().toString() }) { NotificationRow(it, unread = false) { open(it) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: JsonObject, unread: Boolean, onClick: () -> Unit) {
    val info = notificationInfo(n.s("topic"))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (unread) Color.White else Color.White.copy(alpha = 0.55f))
            .border(1.dp, if (unread) Vinko.Lilac else Vinko.Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.soyvinko.app.ui.common.IconTile(info.icon, info.fg, info.bg)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                info.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (unread) Vinko.Ink else Vinko.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(info.text, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(relative(n.s("created_at")), style = MaterialTheme.typography.labelMedium, color = if (unread) Vinko.Primary else Vinko.Subtle)
        }
        if (unread) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(10.dp).background(Vinko.Primary, CircleShape))
        }
    }
}
