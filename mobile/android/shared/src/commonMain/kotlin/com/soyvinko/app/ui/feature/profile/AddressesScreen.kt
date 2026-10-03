package com.soyvinko.app.ui.feature.profile

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.ime
import com.soyvinko.app.ui.common.PlatformBackHandler
import coil3.compose.LocalPlatformContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.currentPosition
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberLocationPermission
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/** Direcciones de servicio guardadas. */
@Composable
fun AddressesScreen(onBack: () -> Unit, onAdd: () -> Unit) {
    val app = LocalApp.current
    val account = rememberResource { app.ops.call("my_account").requireObj() }
    val submit = rememberSubmitter()
    var archiving by remember { mutableStateOf<JsonObject?>(null) }
    ScreenScaffold(
        "Direcciones",
        onBack = onBack,
        floatingAction = {
            if (account.data?.list("addresses")?.isNotEmpty() == true) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    containerColor = Vinko.Primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Añadir dirección") },
                )
            }
        },
    ) { padding ->
        ResourceView(account) { data ->
            val addresses = data.list("addresses")
            ProfilePage(padding) {
                Hint("Guarda los lugares donde recibes servicios. La ubicación exacta solo se comparte con el profesional que contrates.")
                submit.error?.let { Banner(it) }
                if (addresses.isEmpty()) {
                    EmptyBox(
                        "Aún no tienes direcciones",
                        "Añade tu casa, oficina o negocio para pedir servicios más rápido.",
                        "Añadir dirección",
                        onAdd,
                    )
                }
                addresses.forEach { a ->
                    AddressCard(a, onArchive = { archiving = a })
                }
                Spacer(Modifier.height(64.dp))
            }
        }
    }
    archiving?.let { a ->
        ConfirmDialog(
            title = "¿Archivar «${a.s("label").orEmpty()}»?",
            text = "Ya no aparecerá al pedir servicios. Los servicios en curso con esta dirección no cambian.",
            confirm = "Archivar",
            destructive = true,
            onConfirm = {
                archiving = null
                submit.run("Dirección archivada.") {
                    app.ops.call("archive_address", "address" to a.s("id"))
                    account.reload()
                }
            },
            onDismiss = { archiving = null },
        )
    }
}

fun addressArea(a: JsonObject): String =
    listOfNotNull(a.s("parish_name"), a.s("municipality_name") ?: a.s("city"), a.s("state_name"))
        .filter { it.isNotBlank() }
        .joinToString(", ")

@Composable
private fun AddressCard(a: JsonObject, onArchive: () -> Unit) {
    VCard {
        Row(verticalAlignment = Alignment.Top) {
            com.soyvinko.app.ui.common.IconTile(labelIcon(a.s("label")))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(a.s("label").orEmpty(), style = MaterialTheme.typography.titleMedium)
                Text(a.s("address_line").orEmpty(), style = MaterialTheme.typography.bodyMedium)
                Text(addressArea(a), style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                a.s("instructions")?.takeIf { it.isNotBlank() }?.let {
                    Text("Indicaciones: $it", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ubicación privada", style = MaterialTheme.typography.labelMedium, color = Vinko.Mint, modifier = Modifier.weight(1f))
                    TextButton(onClick = onArchive) {
                        Icon(Icons.Outlined.Archive, contentDescription = null, tint = Vinko.Muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Archivar", color = Vinko.Muted)
                    }
                }
            }
        }
    }
}

private fun labelIcon(label: String?) = when (label?.trim()?.lowercase()) {
    "casa", "hogar" -> Icons.Outlined.Home
    "trabajo", "oficina" -> Icons.Outlined.Work
    "negocio", "tienda", "local" -> Icons.Outlined.Store
    else -> Icons.Outlined.LocationOn
}

private val PRESETS = listOf("Casa", "Trabajo", "Negocio")

/** Nueva dirección en dos pasos: zona (estado, municipio y parroquia) y punto exacto en el mapa. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddAddressScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val app = LocalApp.current
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val submit = rememberSubmitter()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var label by rememberSaveable { mutableStateOf("") }
    var state by rememberSaveable { mutableStateOf("") }
    var municipality by rememberSaveable { mutableStateOf("") }
    var parish by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lon by rememberSaveable { mutableStateOf<Double?>(null) }
    var line by rememberSaveable { mutableStateOf("") }
    var instructions by rememberSaveable { mutableStateOf("") }
    var locating by remember { mutableStateOf(false) }
    var locError by remember { mutableStateOf<String?>(null) }

    val states = rememberResource { app.ops.call("geo_states").objects() }
    val municipalities = rememberResource(state) {
        if (state.isEmpty()) emptyList() else app.ops.call("geo_municipalities", "state" to state).objects()
    }
    val parishes = rememberResource(municipality) {
        if (municipality.isEmpty()) emptyList() else app.ops.call("geo_parishes", "municipality" to municipality).objects()
    }
    val detail = rememberResource(parish) { if (parish.isEmpty()) null else app.ops.call("geo_parish", "parish" to parish).obj() }
    val area = detail.data?.takeIf { it.s("code") == parish }
    val point = if (lat != null && lon != null) GeoPos(lat!!, lon!!) else null

    val locate = rememberLocationPermission(
        onDenied = { locError = "Permite el acceso a tu ubicación para usar esta opción, o elige la zona manualmente." },
    ) {
        scope.launch {
            locating = true
            locError = null
            try {
                val p = currentPosition(context)
                if (p == null) {
                    locError = "No pudimos obtener tu ubicación. Activa el GPS e inténtalo de nuevo."
                } else {
                    val found = app.ops.call("locate_parish", "lat" to p.lat, "lon" to p.lon).obj()
                    if (found == null) {
                        locError = "Tu ubicación está fuera de Venezuela. Selecciona la zona manualmente."
                    } else {
                        state = found.s("state_code").orEmpty()
                        municipality = found.s("municipality_code").orEmpty()
                        parish = found.s("parish_code").orEmpty()
                        lat = p.lat
                        lon = p.lon
                        if (label.isNotBlank()) step = 1
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: com.soyvinko.app.data.ApiException) {
                locError = e.message
            } catch (e: Exception) {
                locError = "No pudimos obtener tu ubicación. Elige la zona manualmente."
            } finally {
                locating = false
            }
        }
    }

    val back = { if (step == 1) step = 0 else onBack() }
    PlatformBackHandler(enabled = step == 1) { step = 0 }

    ScreenScaffold("Nueva dirección", onBack = back) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            StepHeader(step)
            if (step == 0) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    LocateButton(locating) { locate() }
                    locError?.let { Banner(it) }
                    VCard {
                        Text("Nombre para esta dirección", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        ChoiceChips(PRESETS.map { it to it }, label.takeIf { it in PRESETS }, { label = it })
                        Spacer(Modifier.height(4.dp))
                        VinkoTextField(label, { label = it.take(80) }, "Nombre", capitalization = KeyboardCapitalization.Sentences, supporting = "Ej. Casa, Trabajo, Negocio")
                    }
                    VCard {
                        Text("Zona", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))
                        SelectField(
                            "País", listOf("VE" to "Venezuela"), "VE", {}, enabled = false,
                            supporting = "Actualmente disponible únicamente en Venezuela.",
                        )
                        Spacer(Modifier.height(6.dp))
                        SelectField(
                            "Estado",
                            states.data.orEmpty().map { it.s("code").orEmpty() to it.s("name").orEmpty() },
                            state,
                            {
                                if (it != state) {
                                    state = it; municipality = ""; parish = ""; lat = null; lon = null
                                }
                            },
                            loading = states.loading,
                            placeholder = "Selecciona tu estado",
                        )
                        Spacer(Modifier.height(10.dp))
                        SelectField(
                            "Municipio",
                            municipalities.data.orEmpty().filter { it.s("state_code") == state }.map { it.s("code").orEmpty() to it.s("name").orEmpty() },
                            municipality,
                            {
                                if (it != municipality) {
                                    municipality = it; parish = ""; lat = null; lon = null
                                }
                            },
                            enabled = state.isNotEmpty(),
                            loading = state.isNotEmpty() && municipalities.loading,
                            placeholder = "Selecciona tu municipio",
                        )
                        Spacer(Modifier.height(10.dp))
                        SelectField(
                            "Parroquia",
                            parishes.data.orEmpty().filter { it.s("municipality_code") == municipality }.map { it.s("code").orEmpty() to it.s("name").orEmpty() },
                            parish,
                            {
                                if (it != parish) {
                                    parish = it; lat = null; lon = null
                                }
                            },
                            enabled = municipality.isNotEmpty(),
                            loading = municipality.isNotEmpty() && parishes.loading,
                            placeholder = "Selecciona tu parroquia",
                        )
                        listOfNotNull(states.error, municipalities.error, parishes.error).firstOrNull()?.let {
                            Spacer(Modifier.height(8.dp))
                            Banner(it)
                        }
                    }
                }
                Box(Modifier.background(Vinko.Cream).padding(horizontal = 20.dp, vertical = 12.dp)) {
                    PrimaryButton(
                        "Continuar",
                        enabled = label.isNotBlank() && parish.isNotEmpty(),
                        onClick = { step = 1 },
                    )
                }
            } else {
                val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                Text(
                    listOfNotNull(
                        label.trim().takeIf { it.isNotEmpty() },
                        area?.let { "${it.s("name")}, ${it.s("municipality_name")}, ${it.s("state_name")}, Venezuela" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                Box(
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .height(if (imeVisible) 150.dp else 300.dp)
                        .background(Vinko.Tint, RoundedCornerShape(18.dp)),
                ) {
                    when {
                        area != null -> ParishMap(
                            area = area,
                            point = point,
                            onPick = {
                                lat = it.lat
                                lon = it.lon
                                submit.clear()
                            },
                            modifier = Modifier.fillMaxSize().clipRounded(),
                        )
                        detail.loading -> LoadingBox()
                        else -> Banner(detail.error ?: "No pudimos cargar el mapa de la parroquia.", modifier = Modifier.padding(12.dp))
                    }
                    SmallFloatingActionButton(
                        onClick = { if (!locating) locate() },
                        containerColor = Color.White,
                        contentColor = Vinko.Primary,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    ) {
                        if (locating) CircularProgressIndicator(Modifier.size(18.dp), color = Vinko.Primary, strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.MyLocation, contentDescription = "Usar mi ubicación actual")
                    }
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PinStatus(point != null)
                    locError?.let { Banner(it) }
                    VinkoTextField(
                        line, { line = it.take(300); submit.clear() }, "Dirección y número",
                        leadingIcon = Icons.Outlined.Place,
                        capitalization = KeyboardCapitalization.Sentences,
                        supporting = "Av. principal, edificio, piso o número de casa",
                        error = if (line.isNotEmpty() && line.trim().length < 3) "Escribe al menos 3 caracteres." else null,
                    )
                    AreaField(
                        instructions, { instructions = it }, "Indicaciones para llegar (opcional)",
                        placeholder = "Punto de referencia, entrada, local…",
                        minLines = 2,
                        maxLength = 1000,
                    )
                    submit.error?.let { Banner(it) }
                    Spacer(Modifier.height(4.dp))
                }
                Box(Modifier.background(Vinko.Cream).padding(horizontal = 20.dp, vertical = 12.dp)) {
                    PrimaryButton(
                        "Guardar dirección",
                        loading = submit.busy,
                        enabled = point != null && area != null && line.trim().length >= 3 && label.isNotBlank(),
                        onClick = {
                            submit.run("Dirección guardada.") {
                                app.ops.call(
                                    "add_address_ve",
                                    "label" to label.trim(),
                                    "country" to "VE",
                                    "state" to state,
                                    "municipality" to municipality,
                                    "parish" to parish,
                                    "line" to line.trim(),
                                    "lon" to lon,
                                    "lat" to lat,
                                    "instructions" to instructions.trim().ifEmpty { null },
                                )
                                onSaved()
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun Modifier.clipRounded() = this.clip(RoundedCornerShape(18.dp))

@Composable
private fun StepHeader(step: Int) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        Text(
            "PASO ${step + 1} DE 2 · ${if (step == 0) "ZONA" else "UBICACIÓN EXACTA"}",
            style = MaterialTheme.typography.labelSmall,
            color = Vinko.Primary,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (step + 1) / 2f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Vinko.Primary,
            trackColor = Vinko.Tint,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun LocateButton(locating: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !locating,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Lilac),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Vinko.Tint, contentColor = Vinko.Primary),
    ) {
        if (locating) CircularProgressIndicator(Modifier.size(18.dp), color = Vinko.Primary, strokeWidth = 2.dp)
        else Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (locating) "Buscando tu ubicación…" else "Usar mi ubicación actual", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PinStatus(selected: Boolean) {
    if (selected) {
        Banner("Punto seleccionado. Puedes tocar otro lugar o mantener presionado el marcador para moverlo.", BannerTone.Success)
    } else {
        Row(
            Modifier.fillMaxWidth().background(Vinko.AmberBg, RoundedCornerShape(14.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = Vinko.Amber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Toca el lugar exacto en el mapa. La zona marcada corresponde a la parroquia elegida.", color = Vinko.Amber, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
