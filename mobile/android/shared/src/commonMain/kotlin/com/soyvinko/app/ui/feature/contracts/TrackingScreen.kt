package com.soyvinko.app.ui.feature.contracts

import com.soyvinko.app.ui.common.toRadians
import com.soyvinko.app.ui.common.oneDecimal
import com.soyvinko.app.ui.common.nowMillis
import coil3.compose.LocalPlatformContext
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShareLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.tracking.LocationTracking
import com.soyvinko.app.tracking.TrackingMode
import com.soyvinko.app.tracking.rememberTrackingStarter
import com.soyvinko.app.ui.common.ErrorBox
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LiveLocation
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.MapMarker
import com.soyvinko.app.ui.common.OsmMap
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.StatusBadge
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.d
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.o
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.openTurnByTurn
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val DEST_COLOR = 0xFF6F3DFF.toInt() // Vinko.Primary
private val PRO_COLOR = 0xFF0C8E72.toInt() // Vinko.Mint

private fun JsonObject?.pos(acc: String = "accuracy_m"): GeoPos? {
    val lat = d("latitude") ?: return null
    val lon = d("longitude") ?: return null
    return GeoPos(lat, lon, l(acc)?.toFloat())
}

/** Datos de mapa de api.navigate (con auditoría en el servidor: solo cada ~30 s y con permiso activo). */
private sealed interface Share {
    data object Loading : Share
    data class Active(val data: JsonObject) : Share {
        private val dest = data.o("destination")
        val destination: GeoPos? = dest.pos()
        val source: String? = dest.s("source") ?: data.o("share").s("source")
        val gps: Boolean get() = source == "gps"
        val destinationUpdatedAt: String? = dest.s("updated_at")
        val destinationAccuracy: Long? = dest.l("accuracy_m")
        val address: String? = dest.s("address")
        val instructions: String? = dest.s("instructions")
        val pro: JsonObject? = data.o("professional_position")
        val proPos: GeoPos? = pro.pos()
    }
    data object Off : Share
    data class Failed(val message: String) : Share
}

@Composable
private fun rememberMap(id: String, enabled: Boolean, intervalMs: Long = 30_000): Pair<Share, () -> Unit> {
    val app = LocalApp.current
    var state by remember(id) { mutableStateOf<Share>(Share.Loading) }
    var version by remember { mutableIntStateOf(0) }
    suspend fun load() {
        if (!enabled) { state = Share.Off; return }
        state = try {
            val data = app.ops.call("navigate", "contract" to id).obj()
            if (data != null && data.o("destination") != null) Share.Active(data) else Share.Off
        } catch (e: ApiException) {
            when {
                e.code == "location_not_shared" -> Share.Off
                e.isNetwork && state is Share.Active -> state // conserva el mapa si se cae la red un momento
                else -> Share.Failed(friendlyError(e))
            }
        }
    }
    LaunchedEffect(id, enabled, version) { load() }
    PollWhileVisible(intervalMs, id, enabled) { if (enabled) load() }
    return state to { version++ }
}

/** Ubicación en vivo: el cliente comparte GPS o dirección; el profesional navega y comparte su trayecto. */
@Composable
fun TrackingScreen(id: String, nav: NavHostController) {
    val app = LocalApp.current
    val me = LocalUser.current.id
    val detail = rememberResource(id) {
        app.ops.call("contract_detail", "contract" to id).obj() ?: throw ApiException("Este servicio no está disponible.", "not_found", 404)
    }
    val c = detail.data
    // Confirmaciones en la raíz para que la hoja cubra toda la pantalla.
    var confirm by remember { mutableStateOf<Pair<Dlg.Confirm, () -> Unit>?>(null) }
    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            if (c != null && c.s("customer_id") != me) "Ruta al servicio" else "Ubicación del servicio",
            onBack = { nav.popBackStack() },
            actions = { if (c != null) Box(Modifier.padding(end = 12.dp)) { StatusBadge(c.s("status")) } },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when {
                    c != null && c.s("delivery_mode") == "remote" -> Column(Modifier.padding(20.dp)) {
                        VCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconTile(Icons.Outlined.Language)
                                Spacer(Modifier.width(12.dp))
                                Text("Este es un servicio remoto: no requiere compartir ubicación.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    c != null && c.s("customer_id") == me -> CustomerTracking(id, c, detail.reload) { d, after -> confirm = d to after }
                    c != null -> ProfessionalTracking(id, c, detail.reload)
                    detail.loading -> LoadingBox()
                    else -> ErrorBox(detail.error ?: "Inténtalo de nuevo.", detail.reload)
                }
            }
        }
        confirm?.let { (d, after) -> ConfirmDialog(d, { confirm = null }) { confirm = null; after() } }
    }
}

// ---------------------------------------------------------------------------------------------
// Cliente: compartir GPS en vivo o la dirección, renovar, revocar y ver al profesional
// ---------------------------------------------------------------------------------------------

@Composable
private fun CustomerTracking(id: String, c: JsonObject, reloadContract: () -> Unit, ask: (Dlg.Confirm, () -> Unit) -> Unit) {
    val app = LocalApp.current
    val context = LocalPlatformContext.current
    val status = rememberLocationStatus(id, 10_000)
    val st = status.data
    val active = st?.active == true
    val (map, reloadMap) = rememberMap(id, enabled = active)
    val mapData = map as? Share.Active
    val contractStatus = c.s("status")
    val pro = c.s("professional_name") ?: "El profesional"
    val session = rememberTrackingFor(id, TrackingMode.CUSTOMER)
    val background = session != null
    val now = rememberNow()
    val startTracking = rememberTrackingStarter(onDenied = { })

    var hours by rememberSaveable { mutableFloatStateOf(8f) } // valor por defecto de api.share_location
    var useGps by rememberSaveable { mutableStateOf(true) }
    var denied by remember { mutableStateOf(false) }
    val canShare = contractStatus in SHARE_STATUSES
    val h = hours.roundToInt()
    val hoursText = "$h ${if (h == 1) "hora" else "horas"}"

    val refreshAll = { status.reload(); reloadMap(); reloadContract() }

    // Con la pantalla abierta y sin servicio en segundo plano, la ubicación GPS se actualiza desde aquí.
    var myPos by remember { mutableStateOf<GeoPos?>(null) }
    var lastSent by remember { mutableLongStateOf(0L) }
    var sendError by remember { mutableStateOf<String?>(null) }
    val liveFromScreen = active && st?.gps == true && !background && canShare && Gps.hasPermission(context)
    LiveLocation(enabled = liveFromScreen) { myPos = it }
    suspend fun update(pos: GeoPos) {
        try {
            app.ops.call(
                "update_shared_location", "contract" to id, "lon" to pos.lon, "lat" to pos.lat,
                "accuracy" to (pos.accuracy?.roundToInt() ?: JsonNull),
            )
            lastSent = nowMillis()
            sendError = null
        } catch (e: ApiException) {
            sendError = friendlyError(e)
            if (e.code == "location_not_shared") status.reload()
        }
    }
    LaunchedEffect(myPos, liveFromScreen) {
        val pos = myPos ?: return@LaunchedEffect
        if (liveFromScreen && nowMillis() - lastSent >= 10_000) update(pos)
    }
    PollWhileVisible(60_000, liveFromScreen) {
        val pos = myPos
        if (liveFromScreen && pos != null && nowMillis() - lastSent >= 55_000) update(pos)
    }

    val shareGps = Dlg.Confirm(
        "Compartir mi ubicación en vivo",
        "Compartiremos con $pro tu ubicación actual como punto de encuentro durante $hoursText. " +
            "Se actualiza mientras compartes y puedes revocarla cuando quieras.",
        "Compartir GPS", "Ubicación en vivo compartida por $hoursText.",
    ) {
        val pos = Gps.current(context)
            ?: throw ApiException("No pudimos obtener tu ubicación. Activa el GPS del teléfono e inténtalo de nuevo.", "no_gps")
        app.ops.call(
            "share_location", "contract" to id, "hours" to h,
            "lon" to pos.lon, "lat" to pos.lat, "accuracy" to (pos.accuracy?.roundToInt() ?: JsonNull),
        )
        myPos = pos
        lastSent = nowMillis()
    }
    val shareAddress = Dlg.Confirm(
        "Compartir la dirección",
        "¿Compartir con $pro la dirección y las indicaciones del servicio durante $hoursText?",
        "Compartir dirección", "Dirección compartida por $hoursText.",
    ) {
        app.ops.call("share_location", "contract" to id, "hours" to h)
        if (LocationTracking.isTracking(id, TrackingMode.CUSTOMER)) LocationTracking.stop(context)
    }
    val askGps = rememberGpsPermission(onDenied = { denied = true }) { denied = false; ask(shareGps, refreshAll) }

    Column(Modifier.fillMaxSize()) {
        val destination = if (liveFromScreen && myPos != null) myPos else mapData?.destination
        MapPanel(
            markers = listOfNotNull(
                mapData?.proPos?.let { MapMarker(it, pro, PRO_COLOR) },
                destination?.let { MapMarker(it, if (mapData?.gps == true) "Tu ubicación" else "Lugar del servicio", DEST_COLOR) },
            ),
            loading = status.loading && st == null || (active && map is Share.Loading),
            emptyText = (map as? Share.Failed)?.message ?: "Comparte tu ubicación para ver el mapa del servicio.",
            legend = listOf((if (mapData?.gps == true) "Tu ubicación" else "Lugar del servicio") to DEST_COLOR, pro to PRO_COLOR),
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShareStatusCard(st, now, pro, mapData)

            if (active && st?.gps == true) VCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.MyLocation, tint = if (background) Vinko.Mint else Vinko.Primary, bg = if (background) Vinko.MintBg else Vinko.Tint)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Actualizar mi ubicación en segundo plano", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Sigue enviando tu posición aunque cierres la app o bloquees el teléfono. Verás una notificación con el botón Detener.",
                            style = MaterialTheme.typography.bodySmall, color = Vinko.Muted,
                        )
                    }
                    Switch(
                        checked = background,
                        onCheckedChange = { on -> if (on) startTracking(id, TrackingMode.CUSTOMER, pro) else LocationTracking.stop(context) },
                        colors = trackingSwitchColors(),
                    )
                }
                val line = when {
                    session?.error != null -> session.error
                    background -> agoText(session?.lastSentAt, now)?.let { "Seguimiento activo · último envío $it" } ?: "Seguimiento activo · esperando señal GPS…"
                    sendError != null -> sendError
                    liveFromScreen -> "Con esta pantalla abierta se actualiza cada 15 s" + (agoText(lastSent.takeIf { it > 0 }, now)?.let { " · último envío $it" } ?: "")
                    else -> "Sin permiso de ubicación: tu punto se queda donde lo compartiste."
                }
                Spacer(Modifier.height(8.dp))
                Text(line.orEmpty(), style = MaterialTheme.typography.bodySmall, color = if (session?.error != null || (sendError != null && !background)) Vinko.Coral else Vinko.Muted)
            }

            if (canShare) {
                VCard {
                    SectionTitle(if (active) "Renovar o cambiar lo que compartes" else "¿Qué quieres compartir?", Icons.Outlined.ShareLocation)
                    Spacer(Modifier.height(10.dp))
                    ShareOption(
                        selected = useGps,
                        icon = Icons.Outlined.MyLocation,
                        title = "Mi ubicación actual (GPS en vivo)",
                        text = "Ideal si no estás en la dirección registrada o es difícil de encontrar.",
                    ) { useGps = true }
                    Spacer(Modifier.height(8.dp))
                    val loc = c["onsite_location"] as? JsonObject
                    ShareOption(
                        selected = !useGps,
                        icon = Icons.Outlined.Home,
                        title = "Dirección del servicio",
                        text = listOfNotNull(loc.s("address_line"), loc.s("city")).joinToString(" · ").ifBlank { "La dirección registrada en tu solicitud." },
                    ) { useGps = false }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Vinko.Muted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Durante $hoursText", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text("hasta ${clockOf(now + h * 3_600_000L)}", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                    }
                    Slider(
                        value = hours,
                        onValueChange = { hours = it },
                        valueRange = 1f..24f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = Vinko.Primary, activeTrackColor = Vinko.Primary, inactiveTrackColor = Vinko.Tint,
                            activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
                        ),
                    )
                    if (denied && useGps) {
                        Banner("Sin permiso de ubicación no podemos usar tu GPS. Comparte la dirección del servicio o activa el permiso en Ajustes > Apps > SoyVinko.")
                        Spacer(Modifier.height(10.dp))
                    }
                    PrimaryButton(
                        when {
                            active -> "Renovar por $hoursText"
                            useGps -> "Compartir mi ubicación"
                            else -> "Compartir la dirección"
                        },
                        enabled = st != null,
                        onClick = { if (useGps) askGps() else ask(shareAddress, refreshAll) },
                    )
                    if (active) {
                        Spacer(Modifier.height(8.dp))
                        OutlineAction("Dejar de compartir", Icons.Outlined.LocationOff, {
                            ask(
                                Dlg.Confirm(
                                    "Dejar de compartir",
                                    "$pro ya no podrá ver tu ubicación ni la dirección del servicio, ni enviarte su trayecto. Podrás volver a compartirla cuando quieras.",
                                    "Dejar de compartir", "Dejaste de compartir la ubicación.", destructive = true,
                                ) {
                                    app.ops.call("revoke_location", "contract" to id)
                                    if (LocationTracking.isTracking(id, TrackingMode.CUSTOMER)) LocationTracking.stop(context)
                                },
                                refreshAll,
                            )
                        }, Modifier.fillMaxWidth(), tint = Vinko.Coral)
                    }
                }
            } else {
                Banner("La ubicación solo se comparte mientras el servicio está activo.", com.soyvinko.app.ui.components.BannerTone.Info)
            }
        }
    }
}

@Composable
private fun trackingSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = Vinko.Mint,
    uncheckedTrackColor = Vinko.Tint,
    uncheckedThumbColor = Vinko.Lavender,
    uncheckedBorderColor = Vinko.Lilac,
)

@Composable
private fun ShareStatusCard(st: LocStatus?, now: Long, pro: String, map: Share.Active?) {
    val active = st?.active == true
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                if (active) Icons.Outlined.ShareLocation else Icons.Outlined.LocationOff,
                tint = if (active) Vinko.Mint else Vinko.Muted,
                bg = if (active) Vinko.MintBg else Vinko.Tint,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        st == null -> "Consultando…"
                        active -> "Ubicación compartida"
                        st.revokedAt != null -> "Dejaste de compartir"
                        st.expired -> "El permiso venció"
                        else -> "Ubicación no compartida"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    when {
                        st == null -> ""
                        active && st.gps -> "$pro ve tu ubicación en vivo como punto de encuentro."
                        active -> "$pro ve la dirección y las indicaciones del servicio."
                        else -> "$pro la necesita para salir en camino."
                    },
                    style = MaterialTheme.typography.bodySmall, color = Vinko.Muted,
                )
            }
            if (st != null) Pill(if (active) "Activa" else "Inactiva", fg = if (active) Vinko.Mint else Vinko.Muted, bg = if (active) Vinko.MintBg else Vinko.Cream)
        }
        if (st != null && st.everShared) {
            Divider()
            KeyValue("Origen", if (st.gps) "GPS en vivo" else "Dirección del servicio")
            KeyValue("Compartida", clockOf(epochMs(st.sharedAt)))
            val expires = epochMs(st.expiresAt)
            if (active) KeyValue("Vence", clockOf(expires) + (countdownText(expires, now)?.let { " · $it" } ?: ""))
            else if (st.revokedAt != null) KeyValue("Revocada", clockOf(epochMs(st.revokedAt)))
            else KeyValue("Venció", clockOf(expires))
            if (active && st.gps) KeyValue("Última actualización", agoText(epochMs(st.locationUpdatedAt), now) ?: "—")
            if (active) {
                val proAt = epochMs(map?.pro.s("recorded_at")) ?: epochMs(st.professionalPositionAt)
                KeyValue(
                    "Posición de $pro",
                    agoText(proAt, now)?.let { ago ->
                        ago + (distanceText(map?.proPos, map?.destination)?.let { " · a $it" } ?: "")
                    } ?: "Sin posición reciente",
                )
            }
        }
    }
}

@Composable
private fun ShareOption(selected: Boolean, icon: ImageVector, title: String, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Vinko.Tint else Color.White)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Vinko.Primary else Vinko.Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Vinko.Primary else Vinko.Muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Vinko.Ink)
            Text(text, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Vinko.Primary))
    }
}

// ---------------------------------------------------------------------------------------------
// Profesional: destino (GPS del cliente o dirección), posición propia, trayecto y avance
// ---------------------------------------------------------------------------------------------

@Composable
private fun ProfessionalTracking(id: String, c: JsonObject, reloadContract: () -> Unit) {
    val app = LocalApp.current
    val context = LocalPlatformContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val status = rememberLocationStatus(id, 10_000)
    val active = status.data?.active == true
    val (map, reloadMap) = rememberMap(id, enabled = active)
    val mapData = map as? Share.Active
    val contractStatus = c.s("status")
    val customer = c.s("customer_name") ?: "El cliente"
    val session = rememberTrackingFor(id, TrackingMode.PROFESSIONAL)
    val background = session != null
    val now = rememberNow()
    val op = rememberOp()

    var granted by remember { mutableStateOf(Gps.hasPermission(context)) }
    var denied by remember { mutableStateOf(false) }
    var myPos by remember { mutableStateOf<GeoPos?>(null) }
    var lastSent by remember { mutableLongStateOf(0L) }
    var sendError by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = Gps.hasPermission(context) }
    val askPermission = rememberGpsPermission(onDenied = { denied = true }) {
        granted = true
        denied = false
        scope.launch { Gps.current(context)?.let { myPos = it } }
    }
    val startTracking = rememberTrackingStarter(onDenied = { denied = true })
    LaunchedEffect(granted) { if (granted) Gps.current(context)?.let { myPos = it } }
    // Posición propia en el mapa mientras la pantalla está visible.
    LiveLocation(enabled = granted) { myPos = it }

    // Envío desde la pantalla solo si el servicio en segundo plano NO sigue este contrato (sin envíos dobles).
    val fromScreen = !background && granted && active && contractStatus == "en_route"
    suspend fun report(pos: GeoPos) {
        try {
            app.ops.call(
                "report_position", "contract" to id, "lon" to pos.lon, "lat" to pos.lat,
                "accuracy" to (pos.accuracy?.roundToInt() ?: JsonNull),
            )
            lastSent = nowMillis()
            sendError = null
        } catch (e: ApiException) {
            sendError = friendlyError(e)
            if (e.code == "position_not_allowed") status.reload()
        }
    }
    LaunchedEffect(myPos, fromScreen) {
        val pos = myPos ?: return@LaunchedEffect
        if (fromScreen && nowMillis() - lastSent >= 10_000) report(pos)
    }
    PollWhileVisible(60_000, fromScreen) {
        val pos = myPos
        if (fromScreen && pos != null && nowMillis() - lastSent >= 55_000) report(pos)
    }

    fun startBackground() {
        if (Gps.hasPermission(context)) LocationTracking.start(context, id, TrackingMode.PROFESSIONAL, customer)
        else startTracking(id, TrackingMode.PROFESSIONAL, customer)
    }

    fun advance(action: String, success: String) = op.run(success, after = {
        reloadContract()
        status.reload()
        reloadMap()
        // Al salir en camino se activa el trayecto en segundo plano (o se piden los permisos).
        if (action == "depart") startBackground()
    }) { app.ops.call("advance_contract", "contract" to id, "action" to action) }

    Column(Modifier.fillMaxSize()) {
        val reported = mapData?.proPos
        MapPanel(
            markers = listOfNotNull(
                (myPos ?: reported)?.let { MapMarker(it, if (myPos != null) "Tú" else "Tu última posición enviada", PRO_COLOR) },
                mapData?.destination?.let { MapMarker(it, if (mapData.gps) "Cliente (GPS en vivo)" else "Lugar del servicio", DEST_COLOR) },
            ),
            loading = (status.loading && status.data == null) || (active && map is Share.Loading),
            emptyText = (map as? Share.Failed)?.message ?: "El mapa aparecerá cuando $customer comparta la ubicación.",
            legend = listOf(
                (if (mapData?.gps == true) "Cliente en vivo" else "Lugar del servicio") to DEST_COLOR,
                (if (myPos != null) "Tu posición" else "Tu última posición") to PRO_COLOR,
            ),
            showMap = mapData != null,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!granted && (active || background)) {
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Outlined.MyLocation, tint = if (denied) Vinko.Coral else Vinko.Primary, bg = if (denied) Vinko.CoralBg else Vinko.Tint)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (denied) "Permiso de ubicación denegado" else "Activa tu ubicación", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (denied) "Sin este permiso no podemos mostrar tu posición ni enviarla a $customer. Puedes activarlo en Ajustes > Apps > SoyVinko > Permisos."
                                else "Para ver tu posición en el mapa y compartir tu trayecto con $customer mientras vas en camino.",
                                style = MaterialTheme.typography.bodySmall, color = Vinko.Muted,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlineAction(if (denied) "Intentar de nuevo" else "Permitir ubicación", Icons.Outlined.MyLocation, askPermission, Modifier.fillMaxWidth(), tint = Vinko.Primary)
                }
            }

            if (status.data != null && !active) {
                val st = status.data
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Outlined.LocationOff, tint = Vinko.Amber, bg = Vinko.AmberBg)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ubicación no disponible", style = MaterialTheme.typography.titleMedium)
                            Text(
                                when {
                                    st?.revokedAt != null -> "$customer dejó de compartir la ubicación ${clockOf(epochMs(st.revokedAt))}. Escríbele por el chat si la necesitas."
                                    st?.expired == true -> "El permiso que dio $customer venció ${clockOf(epochMs(st.expiresAt))}. Pídele que la comparta de nuevo."
                                    else -> "$customer aún no la comparte. Escríbele por el chat; esta pantalla se actualiza sola."
                                },
                                style = MaterialTheme.typography.bodySmall, color = Vinko.Muted,
                            )
                        }
                    }
                }
            }

            if (mapData != null) DestinationCard(mapData, myPos, now) { dest ->
                if (!openTurnByTurn(context, dest, mapData.address ?: "Servicio SoyVinko")) {
                    scope.launch { snackbar.showSnackbar("No encontramos una app de mapas en este teléfono.") }
                }
            }

            VCard {
                SectionTitle("Estado del trayecto", Icons.Outlined.Navigation)
                Spacer(Modifier.height(12.dp))
                val order = listOf("accepted", "en_route", "arrived", "in_progress")
                val at = order.indexOf(contractStatus).let { if (it < 0 && contractStatus in setOf("awaiting_confirmation", "completed")) order.size else it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepButton(Modifier.weight(1f), "En camino", Icons.Outlined.DirectionsCar, done = at >= 1, enabled = contractStatus == "accepted" && active && !op.busy) {
                        advance("depart", "Vas en camino. $customer verá tu trayecto.")
                    }
                    StepButton(Modifier.weight(1f), "Llegué", Icons.Outlined.Flag, done = at >= 2, enabled = contractStatus == "en_route" && !op.busy) {
                        advance("arrive", "Llegada registrada.")
                    }
                    StepButton(Modifier.weight(1f), "Iniciar", Icons.Outlined.PlayArrow, done = at >= 3, enabled = contractStatus == "arrived" && !op.busy) {
                        advance("start", "Actividad iniciada.")
                    }
                }
                if (op.busy) Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Vinko.Primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Actualizando estado…", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
                if (contractStatus == "accepted" && !active) Text(
                    "Podrás marcar \"En camino\" cuando $customer comparta la ubicación.",
                    style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, modifier = Modifier.padding(top = 10.dp),
                )
            }

            VCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.ShareLocation, tint = if (background) Vinko.Mint else Vinko.Primary, bg = if (background) Vinko.MintBg else Vinko.Tint)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Compartir mi trayecto en segundo plano", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "$customer ve tu posición aunque cierres la app o bloquees el teléfono. Verás una notificación con el botón Detener.",
                            style = MaterialTheme.typography.bodySmall, color = Vinko.Muted,
                        )
                    }
                    Switch(
                        checked = background,
                        onCheckedChange = { on -> if (on) startBackground() else LocationTracking.stop(context) },
                        enabled = background || (contractStatus in REPORT_STATUSES && active),
                        colors = trackingSwitchColors(),
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (background) {
                    Row(
                        Modifier.fillMaxWidth().background(Vinko.MintBg, RoundedCornerShape(12.dp)).padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).background(Vinko.Mint, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            agoText(session?.lastSentAt, now)?.let { "Seguimiento activo · último envío $it" } ?: "Seguimiento activo · esperando señal GPS…",
                            style = MaterialTheme.typography.bodySmall, color = Vinko.Mint, modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { LocationTracking.stop(context) }) { Text("Detener", color = Vinko.Coral, style = MaterialTheme.typography.labelLarge) }
                    }
                    session?.error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Coral)
                    }
                } else {
                    Text(
                        when {
                            sendError != null -> sendError!!
                            fromScreen -> "Con esta pantalla abierta se envía cada 15 s" + (agoText(lastSent.takeIf { it > 0 }, now)?.let { " · último envío $it" } ?: " · esperando señal GPS…")
                            contractStatus == "accepted" -> "Se activa al marcar \"En camino\"."
                            contractStatus !in REPORT_STATUSES -> "El trayecto ya no se comparte en este estado del servicio."
                            !active -> "Requiere que $customer comparta la ubicación."
                            else -> "Activa el segundo plano para que $customer vea tu posición."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (sendError != null) Vinko.Coral else Vinko.Muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(map: Share.Active, myPos: GeoPos?, now: Long, onNavigate: (GeoPos) -> Unit) {
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Destino", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (map.gps) Pill("GPS en vivo del cliente", fg = Vinko.Mint, bg = Vinko.MintBg, icon = Icons.Outlined.MyLocation)
            else Pill("Dirección del servicio", fg = Vinko.Primary, bg = Vinko.Tint, icon = Icons.Outlined.Home)
        }
        Spacer(Modifier.height(8.dp))
        if (map.gps) {
            Text(
                "El cliente compartió su ubicación actual como punto de encuentro.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                listOfNotNull(
                    agoText(epochMs(map.destinationUpdatedAt), now)?.let { "Actualizada $it" },
                    map.destinationAccuracy?.let { "precisión ±$it m" },
                ).joinToString(" · ").ifBlank { "Ubicación en vivo" },
                style = MaterialTheme.typography.bodySmall, color = Vinko.Mint,
            )
            map.address?.let {
                Spacer(Modifier.height(6.dp))
                Text("Dirección de referencia: $it", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
            }
        } else {
            Text(map.address ?: "Dirección del servicio", style = MaterialTheme.typography.bodyLarge)
        }
        map.instructions?.takeIf { it.isNotBlank() }?.let {
            Text("Indicaciones: $it", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
        distanceText(myPos, map.destination)?.let {
            Spacer(Modifier.height(6.dp))
            Pill("Estás a $it", fg = Vinko.Cyan, bg = Vinko.CyanBg, icon = Icons.Outlined.MyLocation)
        }
        map.destination?.let { dest ->
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Abrir en Maps", onClick = { onNavigate(dest) })
        }
    }
}

// ---------------------------------------------------------------------------------------------

@Composable
private fun MapPanel(
    markers: List<MapMarker>,
    loading: Boolean,
    emptyText: String,
    legend: List<Pair<String, Int>>,
    showMap: Boolean = markers.isNotEmpty(),
) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp)) {
        val hasMap = showMap && markers.isNotEmpty()
        Box(
            Modifier.fillMaxWidth().height(if (hasMap || loading) 280.dp else 150.dp).clip(RoundedCornerShape(20.dp)).background(Vinko.Tint).border(1.dp, Vinko.Line, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                hasMap -> OsmMap(markers = markers, modifier = Modifier.fillMaxSize(), drawLine = markers.size >= 2)
                loading -> CircularProgressIndicator(color = Vinko.Primary)
                else -> Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconTile(Icons.Outlined.LocationOff, tint = Vinko.Lavender, bg = Color.White, size = 48.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted, textAlign = TextAlign.Center)
                }
            }
        }
        if (hasMap) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            legend.forEach { (label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(Color(color), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
            }
        }
    }
}

@Composable
private fun StepButton(modifier: Modifier, label: String, icon: ImageVector, done: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg = when {
        done -> Vinko.MintBg
        enabled -> Vinko.Primary
        else -> Vinko.Cream
    }
    val fg = when {
        done -> Vinko.Mint
        enabled -> Color.White
        else -> Vinko.Subtle
    }
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, if (enabled || done) Color.Transparent else Vinko.Line, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(if (done) Icons.Outlined.Check else icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/** Distancia en línea recta (haversine) en texto: "850 m" o "2,4 km". */
fun distanceText(a: GeoPos?, b: GeoPos?): String? {
    if (a == null || b == null) return null
    val r = 6_371_000.0
    val dLat = (b.lat - a.lat).toRadians()
    val dLon = (b.lon - a.lon).toRadians()
    val h = sin(dLat / 2) * sin(dLat / 2) + cos((a.lat).toRadians()) * cos((b.lat).toRadians()) * sin(dLon / 2) * sin(dLon / 2)
    val meters = 2 * r * atan2(sqrt(h), sqrt(1 - h))
    return if (meters < 1000) "${(meters / 10).roundToInt() * 10} m" else (oneDecimal(meters / 1000) + " km")
}
