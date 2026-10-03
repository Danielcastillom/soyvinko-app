package com.soyvinko.app.ui.feature.contracts

import com.soyvinko.app.ui.common.longDayMonth
import com.soyvinko.app.ui.common.todayCaracas
import com.soyvinko.app.ui.common.caracas
import kotlinx.datetime.minus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.common.ApiImage
import com.soyvinko.app.ui.common.ErrorBox
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.StatusBadge
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberCameraCapture
import com.soyvinko.app.ui.common.rememberPhotoPicker
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.time
import com.soyvinko.app.ui.common.uploadFile
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private const val PAGE = 50

/** Mensaje local aún no confirmado por el servidor (texto o foto). */
private class Pending(val key: String, val body: String?, val part: UploadPart?) {
    var fileId by mutableStateOf<String?>(null)
    var failed by mutableStateOf<String?>(null)
}

/**
 * Chat del contrato: api.chat_messages (se consulta cada 5 s mientras la pantalla está visible),
 * api.send_message y api.send_attachment (foto subida con purpose "chat").
 */
@Composable
fun ChatScreen(id: String, nav: NavHostController) {
    val app = LocalApp.current
    val me = LocalUser.current.id
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val detail = rememberResource(id) { app.ops.call("contract_detail", "contract" to id).obj() }
    val c = detail.data
    val status = c?.s("status")
    val counterpart = if (c?.s("customer_id") == me) c.s("professional_name") else c?.s("customer_name")

    // Más reciente primero (igual que la API).
    var messages by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasOlder by remember { mutableStateOf(false) }
    var loadingOlder by remember { mutableStateOf(false) }
    val pending = remember { mutableStateListOf<Pending>() }
    var text by remember { mutableStateOf("") }
    var viewer by remember { mutableStateOf<String?>(null) }
    val list = rememberLazyListState()

    fun merge(incoming: List<JsonObject>) {
        val byId = LinkedHashMap<String, JsonObject>()
        (messages + incoming).forEach { m -> m.s("id")?.let { byId[it] = m } }
        messages = byId.values.sortedWith(compareByDescending<JsonObject> { it.s("created_at").orEmpty() }.thenByDescending { it.s("id").orEmpty() })
    }

    suspend fun fetchLatest() {
        val page = app.ops.call("chat_messages", "contract" to id, "limit" to PAGE).objects()
        // Los mensajes recientes reemplazan a sus versiones anteriores (p. ej. mensajes retirados por moderación).
        val newest = page.map { it.s("id") }.toSet()
        messages = messages.filter { it.s("id") !in newest }
        merge(page)
        if (!loaded) hasOlder = page.size >= PAGE
        loaded = true
        error = null
    }

    suspend fun fetchOlder() {
        val oldest = messages.lastOrNull() ?: return
        val page = app.ops.call(
            "chat_messages", "contract" to id, "limit" to PAGE,
            "before" to oldest.s("created_at"), "before_id" to oldest.s("id"),
        ).objects()
        merge(page)
        hasOlder = page.size >= PAGE
    }

    LaunchedEffect(id) {
        try { fetchLatest() } catch (e: ApiException) { error = friendlyError(e) }
    }
    val newestId = messages.firstOrNull()?.s("id")
    LaunchedEffect(newestId) {
        if (newestId != null && list.firstVisibleItemIndex <= 1) list.animateScrollToItem(0)
    }
    PollWhileVisible(5_000, id) { fetchLatest() }

    fun deliver(p: Pending) = scope.launch {
        p.failed = null
        try {
            if (p.body != null) {
                app.ops.call("send_message", "contract" to id, "body" to p.body, "key" to p.key)
            } else if (p.part != null) {
                val file = p.fileId ?: app.uploadFile(p.part, "chat").also { p.fileId = it }
                app.ops.call("send_attachment", "contract" to id, "file" to file, "key" to p.key)
            }
            fetchLatest()
            pending.remove(p)
        } catch (e: ApiException) {
            p.failed = friendlyError(e)
        }
    }

    fun sendText() {
        val body = text.trim()
        if (body.isEmpty()) return
        text = ""
        val p = Pending(newKey(), body, null)
        pending += p
        scope.launch { list.animateScrollToItem(0) }
        deliver(p)
    }

    fun sendPhoto(part: UploadPart) {
        val p = Pending(newKey(), null, part)
        pending += p
        scope.launch { list.animateScrollToItem(0) }
        deliver(p)
    }

    val pick = rememberPhotoPicker { parts -> parts.firstOrNull()?.let(::sendPhoto) }
    val camera = rememberCameraCapture { sendPhoto(it) }
    val canText = c != null && status != "cancelled"
    val canAttach = c != null && status != "cancelled" && status != "completed"

    ScreenScaffold(
        counterpart ?: "Chat del servicio",
        onBack = { nav.popBackStack() },
        bottomBar = {
            if (c != null) Composer(
                text = text,
                onText = { text = it.take(10_000) },
                enabled = canText,
                canAttach = canAttach,
                onSend = ::sendText,
                onGallery = pick,
                onCamera = camera,
                closedReason = when (status) {
                    "cancelled" -> "Este servicio fue cancelado: la conversación quedó cerrada."
                    else -> null
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (c != null) ChatHeader(c, onClick = {
                if (nav.previousBackStackEntry?.destination?.route == Routes.CONTRACT) nav.popBackStack() else nav.navigate(Routes.contract(id))
            }, onLocation = if (c.s("delivery_mode") != "remote" && status in SHARE_STATUSES) {
                { nav.navigate(Routes.tracking(id)) }
            } else null)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    !loaded && error != null -> ErrorBox(error!!) { scope.launch { try { fetchLatest() } catch (e: ApiException) { error = friendlyError(e) } } }
                    !loaded -> LoadingBox()
                    messages.isEmpty() && pending.isEmpty() -> Welcome()
                    else -> LazyColumn(
                        state = list,
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(pending.reversed().size, key = { "p-" + pending.reversed()[it].key }) { i ->
                            val p = pending.reversed()[i]
                            PendingBubble(p) { deliver(p) }
                        }
                        messages.forEachIndexed { i, m ->
                            item(key = m.s("id") ?: "m$i") {
                                Message(m, mine = m.s("sender_id") == me, onPhoto = { viewer = it }, onLocation = {
                                    if (c?.s("delivery_mode") != "remote") nav.navigate(Routes.tracking(id))
                                })
                            }
                            val day = dayOf(m.s("created_at"))
                            val next = messages.getOrNull(i + 1)
                            if (next == null || dayOf(next.s("created_at")) != day) {
                                item(key = "d-$day-$i") { DayHeader(m.s("created_at")) }
                            }
                        }
                        if (hasOlder) item(key = "older") {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                if (loadingOlder) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Vinko.Primary)
                                else TextButton(onClick = {
                                    scope.launch {
                                        loadingOlder = true
                                        try { fetchOlder() } catch (e: ApiException) { snackbar.showSnackbar(friendlyError(e)) } finally { loadingOlder = false }
                                    }
                                }) { Text("Ver mensajes anteriores", color = Vinko.Primary) }
                            }
                        }
                    }
                }
            }
        }
    }
    viewer?.let { PhotoViewer(it) { viewer = null } }
}

private fun dayOf(iso: String?): LocalDate? = caracas(iso)?.date

@Composable
private fun DayHeader(iso: String?) {
    val day = dayOf(iso)
    val today = todayCaracas()
    val label = when (day) {
        null -> ""
        today -> "Hoy"
        today.minus(1, DateTimeUnit.DAY) -> "Ayer"
        else -> longDayMonth(day)
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Vinko.Muted,
            modifier = Modifier.background(Color.White, RoundedCornerShape(50)).border(1.dp, Vinko.Line, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ChatHeader(c: JsonObject, onClick: () -> Unit, onLocation: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(c.s("title") ?: "Servicio contratado", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Contrato ${folio(c.s("id").orEmpty())}", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
        if (onLocation != null) IconButton(onClick = onLocation) {
            Icon(Icons.Outlined.LocationOn, contentDescription = "Ubicación del servicio", tint = Vinko.Primary)
        }
        StatusBadge(c.s("status"))
    }
    Box(Modifier.fillMaxWidth().size(1.dp).background(Vinko.Line))
}

@Composable
private fun Welcome() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconTile(Icons.Outlined.VerifiedUser, size = 56.dp)
        Spacer(Modifier.size(12.dp))
        Text("Canal oficial de coordinación", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.size(6.dp))
        Text(
            "Confirmen detalles, compartan especificaciones y mantengan aquí el registro de su acuerdo.",
            style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Message(m: JsonObject, mine: Boolean, onPhoto: (String) -> Unit, onLocation: () -> Unit) {
    val kind = m.s("kind")
    if (kind == "location_shared" || kind == "system") {
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            Row(
                Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(14.dp)).background(Vinko.Tint).clickable(enabled = kind == "location_shared", onClick = onLocation)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (kind == "location_shared") Icons.Outlined.LocationOn else Icons.Outlined.Info, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(m.s("body") ?: "Mensaje del sistema", style = MaterialTheme.typography.bodySmall, color = Vinko.Primary)
                    Text(
                        time(m.s("created_at")) + if (kind == "location_shared") " · Toca para ver el mapa" else "",
                        style = MaterialTheme.typography.bodySmall, color = Vinko.Lavender,
                    )
                }
            }
        }
        return
    }
    val file = m.s("file_id")
    val body = m.s("body")
    if (file != null && body == null) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            ApiImage(
                file,
                Modifier.size(width = 230.dp, height = 190.dp).clip(RoundedCornerShape(18.dp))
                    .border(1.dp, if (mine) Vinko.Primary else Vinko.Line, RoundedCornerShape(18.dp)).clickable { onPhoto(file) },
            )
            Text(time(m.s("created_at")), style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle, modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp))
        }
        return
    }
    Bubble(mine, time(m.s("created_at"))) {
        if (file != null) {
            ApiImage(file, Modifier.size(width = 220.dp, height = 180.dp).clip(RoundedCornerShape(14.dp)).clickable { onPhoto(file) })
        }
        if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = if (mine) Color.White else Vinko.Ink)
        if (file == null && body == null) Text(
            "Mensaje retirado por moderación",
            style = MaterialTheme.typography.bodySmall,
            color = if (mine) Vinko.Lilac else Vinko.Subtle,
        )
    }
}

@Composable
private fun Bubble(mine: Boolean, footer: String, footerIcon: androidx.compose.ui.graphics.vector.ImageVector? = null, footerColor: Color = Vinko.Subtle, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (mine) Vinko.Primary else Color.White,
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 4.dp, bottomEnd = if (mine) 4.dp else 18.dp),
                )
                .then(if (mine) Modifier else Modifier.border(1.dp, Vinko.Line, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { content() }
        Row(Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (footerIcon != null) {
                Icon(footerIcon, contentDescription = null, tint = footerColor, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(footer, style = MaterialTheme.typography.bodySmall, color = footerColor)
        }
    }
}

@Composable
private fun PendingBubble(p: Pending, onRetry: () -> Unit) {
    val failed = p.failed
    Box(Modifier.fillMaxWidth().clickable(enabled = failed != null, onClick = onRetry)) {
        Bubble(
            mine = true,
            footer = failed?.let { "No enviado · Toca para reintentar" } ?: "Enviando…",
            footerIcon = if (failed != null) Icons.Outlined.ErrorOutline else Icons.Outlined.Schedule,
            footerColor = if (failed != null) Vinko.Coral else Vinko.Subtle,
        ) {
            if (p.part != null) {
                p.fileId?.let { ApiImage(it, Modifier.size(width = 220.dp, height = 180.dp).clip(RoundedCornerShape(14.dp))) }
                    ?: Box(Modifier.size(width = 220.dp, height = 120.dp).background(Vinko.PrimaryHover, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        if (failed == null) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Color.White)
                    }
            }
            p.body?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White) }
        }
    }
    if (failed != null) Text(
        failed,
        style = MaterialTheme.typography.bodySmall,
        color = Vinko.Coral,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth().padding(start = 60.dp, end = 4.dp),
    )
}

@Composable
private fun Composer(
    text: String,
    onText: (String) -> Unit,
    enabled: Boolean,
    canAttach: Boolean,
    onSend: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    closedReason: String?,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
    ) {
        Box(Modifier.fillMaxWidth().size(1.dp).background(Vinko.Line))
        if (!enabled) {
            Text(
                closedReason ?: "La conversación está cerrada.",
                style = MaterialTheme.typography.bodyMedium,
                color = Vinko.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            return@Column
        }
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
            if (canAttach) {
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Adjuntar foto", tint = Vinko.Primary)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Color.White) {
                        DropdownMenuItem(
                            text = { Text("Elegir de la galería") },
                            leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                            onClick = { menu = false; onGallery() },
                        )
                        DropdownMenuItem(
                            text = { Text("Tomar foto") },
                            leadingIcon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                            onClick = { menu = false; onCamera() },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = onText,
                placeholder = { Text("Escribe un mensaje…") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                shape = RoundedCornerShape(22.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = fieldColors(),
            )
            Spacer(Modifier.width(8.dp))
            val ready = text.isNotBlank()
            IconButton(
                onClick = onSend,
                enabled = ready,
                modifier = Modifier.padding(bottom = 4.dp).size(48.dp).background(if (ready) Vinko.Primary else Vinko.Lilac, CircleShape),
            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar mensaje", tint = Color.White) }
        }
    }
}
