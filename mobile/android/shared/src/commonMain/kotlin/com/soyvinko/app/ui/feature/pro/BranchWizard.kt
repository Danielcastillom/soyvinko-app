package com.soyvinko.app.ui.feature.pro

import coil3.compose.LocalPlatformContext
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.MapMarker
import com.soyvinko.app.ui.common.OsmMap
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.currentPosition
import com.soyvinko.app.ui.common.d
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberLocationPermission
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch

/**
 * Alta de sede en Venezuela (add_branch_ve): estado → municipio → parroquia, punto exacto en el
 * mapa (o GPS con locate_parish), dirección y visibilidad. Igual que el asistente de la web.
 */
@Composable
fun BranchWizardScreen(nav: NavHostController) {
    val app = LocalApp.current
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val form = rememberFormAction()
    var name by rememberSaveable { mutableStateOf("") }
    var state by rememberSaveable { mutableStateOf<String?>(null) }
    var municipality by rememberSaveable { mutableStateOf<String?>(null) }
    var parish by rememberSaveable { mutableStateOf<String?>(null) }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lon by rememberSaveable { mutableStateOf<Double?>(null) }
    var line by rememberSaveable { mutableStateOf("") }
    var isPublic by rememberSaveable { mutableStateOf(false) }
    var tried by rememberSaveable { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var locateError by remember { mutableStateOf<String?>(null) }

    val states = rememberResource { app.ops.call("geo_states").objects() }
    val municipalities = rememberResource(state) { state?.let { app.ops.call("geo_municipalities", "state" to it).objects() } ?: emptyList() }
    val parishes = rememberResource(municipality) { municipality?.let { app.ops.call("geo_parishes", "municipality" to it).objects() } ?: emptyList() }
    val area = rememberResource(parish) { parish?.let { app.ops.call("geo_parish", "parish" to it).obj() } }

    val locate = rememberLocationPermission(
        onDenied = { locateError = "Permite el acceso a tu ubicación para usar tu posición actual, o elige la zona manualmente." },
    ) {
        scope.launch {
            locating = true
            locateError = null
            try {
                val pos = currentPosition(context)
                    ?: throw ApiException("No pudimos obtener tu ubicación. Activa el GPS e inténtalo de nuevo.")
                val found = app.ops.call("locate_parish", "lon" to pos.lon, "lat" to pos.lat).obj()
                    ?: throw ApiException("Tu ubicación está fuera de Venezuela. Selecciona la zona manualmente.")
                state = found.s("state_code")
                municipality = found.s("municipality_code")
                parish = found.s("parish_code")
                lat = pos.lat
                lon = pos.lon
            } catch (e: ApiException) {
                locateError = e.message
            } finally {
                locating = false
            }
        }
    }

    val point = if (lat != null && lon != null) GeoPos(lat!!, lon!!) else null
    val areaData = area.data
    val nameError = if (tried && name.isBlank()) "Escribe un nombre para la sede." else null
    val lineError = if (tried && line.trim().length < 3) "Escribe la dirección (al menos 3 caracteres)." else null

    ScreenScaffold("Nueva sede", onBack = { nav.popBackStack() }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Tu sede define desde dónde atiendes servicios presenciales. Solo está disponible en Venezuela.",
                style = MaterialTheme.typography.bodyMedium,
                color = Vinko.Muted,
            )
            form.error?.let { Banner(it) }

            VinkoTextField(name, { name = it.take(80) }, "Nombre de la sede", capitalization = KeyboardCapitalization.Sentences, error = nameError)
            ChoiceChips(listOf("Sede principal", "Taller", "Oficina", "Local").map { it to it }, name, { name = it })

            VCard {
                Text("Ubicación", style = MaterialTheme.typography.titleSmall)
                Text("Usa tu GPS o elige estado, municipio y parroquia.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                Spacer(Modifier.height(10.dp))
                SmallButton(
                    if (locating) "Buscando tu ubicación…" else "Usar mi ubicación actual",
                    onClick = locate,
                    icon = Icons.Outlined.MyLocation,
                    tone = Tone.Soft,
                    loading = locating,
                )
                locateError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Coral)
                }
            }

            PickerField(
                "Estado",
                states.data.orEmpty().mapNotNull { s -> s.s("code")?.let { it to (s.s("name") ?: it) } },
                state,
                {
                    if (it != state) {
                        state = it; municipality = null; parish = null; lat = null; lon = null
                    }
                },
                placeholder = if (states.loading && states.data == null) "Cargando…" else "Selecciona el estado",
                supporting = states.error ?: if (tried && state == null) "Selecciona el estado." else null,
            )
            PickerField(
                "Municipio",
                municipalities.data.orEmpty().filter { it.s("state_code") == state }.mapNotNull { m -> m.s("code")?.let { it to (m.s("name") ?: it) } },
                municipality,
                {
                    if (it != municipality) {
                        municipality = it; parish = null; lat = null; lon = null
                    }
                },
                enabled = state != null,
                placeholder = if (state == null) "Primero elige el estado" else "Selecciona el municipio",
                supporting = if (tried && municipality == null) "Selecciona el municipio." else null,
            )
            PickerField(
                "Parroquia",
                parishes.data.orEmpty().filter { it.s("municipality_code") == municipality }.mapNotNull { p -> p.s("code")?.let { it to (p.s("name") ?: it) } },
                parish,
                {
                    if (it != parish) {
                        parish = it; lat = null; lon = null
                    }
                },
                enabled = municipality != null,
                placeholder = if (municipality == null) "Primero elige el municipio" else "Selecciona la parroquia",
                supporting = if (tried && parish == null) "Selecciona la parroquia." else null,
            )

            if (parish != null) {
                if (areaData == null || areaData.s("code") != parish) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Vinko.Primary, modifier = Modifier.size(28.dp))
                    }
                } else {
                    val center = GeoPos(areaData.d("latitude") ?: 10.48, areaData.d("longitude") ?: -66.90)
                    Text(
                        "${areaData.s("name")}, ${areaData.s("municipality_name")}, ${areaData.s("state_name")}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    OsmMap(
                        markers = point?.let { listOf(MapMarker(it, name.ifBlank { "Sede" })) }.orEmpty(),
                        modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(16.dp)),
                        center = if (point == null) center else null,
                        zoom = 15.0,
                        onTap = { p ->
                            lat = p.lat
                            lon = p.lon
                        },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (point != null) Icons.Outlined.CheckCircle else Icons.Outlined.PinDrop,
                            contentDescription = null,
                            tint = if (point != null) Vinko.Mint else Vinko.Primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (point != null) "Punto marcado. Toca otro lugar del mapa para ajustarlo." else "Toca el mapa en el lugar exacto de tu sede.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (point != null) Vinko.Mint else Vinko.Muted,
                        )
                    }
                    if (point == null) {
                        SecondaryButton("Usar el centro de la parroquia", onClick = {
                            lat = center.lat
                            lon = center.lon
                        })
                    }
                    if (tried && point == null) Text("Marca el punto exacto de la sede.", style = MaterialTheme.typography.bodySmall, color = Vinko.Coral)
                }
            }

            VinkoTextField(
                line, { line = it.take(300) }, "Dirección y número",
                capitalization = KeyboardCapitalization.Sentences,
                supporting = "Av. principal, edificio, piso o número de local",
                error = lineError,
            )
            SwitchRow(
                "Mostrar la dirección en mi perfil",
                if (isPublic) "Los clientes verán la dirección y el punto en el mapa." else "Solo se usará para calcular tu cobertura.",
                isPublic,
                { isPublic = it },
            )
            Banner("Tu dirección exacta solo se muestra si la haces pública.", BannerTone.Info)
            PrimaryButton("Guardar sede", loading = form.busy, onClick = {
                tried = true
                val p = point
                if (name.isBlank() || line.trim().length < 3 || state == null || municipality == null || parish == null || p == null) return@PrimaryButton
                form.run("Sede guardada. Configura ahora su cobertura y horario.", onSuccess = {
                    ProSignals.setup++
                    nav.popBackStack()
                }) {
                    app.ops.call(
                        "add_branch_ve",
                        "name" to name.trim(),
                        "country" to "VE",
                        "state" to state,
                        "municipality" to municipality,
                        "parish" to parish,
                        "line" to line.trim(),
                        "lon" to p.lon,
                        "lat" to p.lat,
                        "public_address" to isPublic,
                    )
                }
            })
            Spacer(Modifier.height(16.dp))
        }
    }
}
