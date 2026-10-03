package com.soyvinko.app.ui.feature.explore

import com.soyvinko.app.ui.common.fullScreenDialogProperties
import coil3.compose.LocalPlatformContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.data.Ops
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.currentPosition
import com.soyvinko.app.ui.common.hasLocationPermission
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.rememberLocationPermission
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject

enum class GeoStatus { Idle, Locating, Ready, Denied, Unavailable, Outside }

/** Ubicación del teléfono y su parroquia (equivalente a useDeviceLocation de la web). */
class DeviceLocation {
    var point by mutableStateOf<GeoPos?>(null)
    var area by mutableStateOf<JsonObject?>(null)
    var status by mutableStateOf(GeoStatus.Idle)
    var error by mutableStateOf("")
    var locate: () -> Unit = {}
}

/** Recuerda la última ubicación durante la sesión para no pedirla en cada cambio de pestaña. */
private object LocationMemory {
    var point: GeoPos? = null
    var area: JsonObject? = null
    var asked = false
    var manualPoint: GeoPos? = null
    var manualArea: JsonObject? = null
}

class ManualZone(point: GeoPos?, area: JsonObject?) {
    var point by mutableStateOf(point)
    var area by mutableStateOf(area)
}

@Composable
fun rememberManualZone(persist: Boolean): ManualZone {
    val zone = remember { if (persist) ManualZone(LocationMemory.manualPoint, LocationMemory.manualArea) else ManualZone(null, null) }
    LaunchedEffect(zone.point, zone.area) {
        if (persist) {
            LocationMemory.manualPoint = zone.point
            LocationMemory.manualArea = zone.area
        }
    }
    return zone
}

suspend fun locateParish(ops: Ops, pos: GeoPos): JsonObject? =
    ops.call("locate_parish", "lat" to pos.lat, "lon" to pos.lon).obj()

fun JsonObject.areaLabel(): String = listOfNotNull(s("parish_name"), s("state_name")).joinToString(", ")

/**
 * Pide permiso (si `auto` y no se pidió antes en la sesión), obtiene la posición por GPS y la ubica en una parroquia.
 * `initial` permite fijar una posición (pruebas o reanudación).
 */
@Composable
fun rememberDeviceLocation(initial: GeoPos?, auto: Boolean): DeviceLocation {
    val app = LocalApp.current
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val state = remember {
        DeviceLocation().apply {
            if (initial == null && auto && LocationMemory.point != null) {
                point = LocationMemory.point
                area = LocationMemory.area
                status = if (area != null) GeoStatus.Ready else GeoStatus.Outside
                if (area == null) error = "Los servicios presenciales están disponibles actualmente en Venezuela."
            }
        }
    }
    fun resolve(pos: GeoPos) {
        state.point = pos
        state.status = GeoStatus.Locating
        scope.launch(Dispatchers.Main.immediate) {
            try {
                val area = locateParish(app.ops, pos)
                state.area = area
                state.status = if (area != null) GeoStatus.Ready else GeoStatus.Outside
                state.error = if (area != null) "" else "Los servicios presenciales están disponibles actualmente en Venezuela."
                if (auto) {
                    LocationMemory.point = pos
                    LocationMemory.area = area
                }
            } catch (e: ApiException) {
                state.area = null
                state.status = GeoStatus.Unavailable
                state.error = "No pudimos identificar tu zona. Puedes elegirla manualmente."
            }
        }
    }
    val request = rememberLocationPermission(
        onDenied = {
            state.status = GeoStatus.Denied
            state.error = "Permite la ubicación en los ajustes del teléfono o elige una zona en el mapa."
        },
    ) {
        state.status = GeoStatus.Locating
        state.error = ""
        scope.launch(Dispatchers.Main.immediate) {
            val pos = withTimeoutOrNull(20_000) { currentPosition(context) }
            if (pos == null) {
                state.status = GeoStatus.Unavailable
                state.error = "No pudimos obtener tu ubicación. Puedes reintentar o elegir una zona."
            } else resolve(pos)
        }
    }
    state.locate = request
    LaunchedEffect(Unit) {
        when {
            initial != null -> resolve(initial)
            !auto || state.point != null -> Unit
            hasLocationPermission(context) -> request()
            !LocationMemory.asked -> {
                LocationMemory.asked = true
                request()
            }
        }
    }
    return state
}

/** Elegir una zona tocando el mapa (equivalente nativo de AddressWizard kind="search"). */
@Composable
fun ZonePickerDialog(start: GeoPos?, onDismiss: () -> Unit, onPicked: (GeoPos, JsonObject) -> Unit) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf<GeoPos?>(null) }
    var area by remember { mutableStateOf<JsonObject?>(null) }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = fullScreenDialogProperties()) {
        Column(Modifier.fillMaxSize().background(Vinko.Cream)) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Cerrar") }
                Text("Elige la zona de búsqueda", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "Toca el mapa para marcar el punto alrededor del cual quieres buscar profesionales.",
                style = MaterialTheme.typography.bodyMedium,
                color = Vinko.Muted,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                ExploreMapView(
                    pins = listOfNotNull(picked?.let { Pin("picked", it, PinKind.Picked, "Zona elegida") }),
                    modifier = Modifier.fillMaxSize(),
                    initialCenter = start,
                    initialZoom = if (start != null) 13.0 else 7.0,
                    fitKey = "static",
                    onMapTap = { pos ->
                        picked = pos
                        area = null
                        error = null
                        checking = true
                        scope.launch(Dispatchers.Main.immediate) {
                            try {
                                val found = locateParish(app.ops, pos)
                                if (picked == pos) {
                                    area = found
                                    if (found == null) error = "Los servicios presenciales están disponibles actualmente en Venezuela."
                                }
                            } catch (e: ApiException) {
                                if (picked == pos) error = e.message
                            } finally {
                                if (picked == pos) checking = false
                            }
                        }
                    },
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .navigationBarsPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    when {
                        checking -> {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Vinko.Primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Identificando la zona…", style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
                        }
                        area != null -> Text(area!!.areaLabel(), style = MaterialTheme.typography.titleSmall)
                        error != null -> Text(error!!, style = MaterialTheme.typography.bodyMedium, color = Vinko.Coral)
                        else -> Text("Aún no has marcado un punto.", style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
                    }
                }
                PrimaryButton(
                    "Buscar en esta zona",
                    onClick = {
                        val p = picked
                        val a = area
                        if (p != null && a != null) {
                            onPicked(p, a)
                            onDismiss()
                        }
                    },
                    enabled = picked != null && area != null && !checking,
                )
            }
        }
    }
}
