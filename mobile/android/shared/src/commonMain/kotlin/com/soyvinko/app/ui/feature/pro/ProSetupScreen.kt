package com.soyvinko.app.ui.feature.pro

import kotlinx.datetime.LocalDate
import com.soyvinko.app.ui.common.oneDecimal
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Whatsapp
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.Resource
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.rememberAction
import com.soyvinko.app.ui.common.rememberPhotoPicker
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.statusLabel
import com.soyvinko.app.ui.common.uploadFile
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

private val SETUP_TABS = listOf("Perfil", "Servicios", "Sedes y cobertura", "Portafolio", "Contactos")

@Composable
fun ProSetupScreen(nav: NavHostController, initialTab: Int = 0) {
    val app = LocalApp.current
    val signal = ProSignals.setup
    val pro = rememberResource(signal) { app.ops.call("my_professional") }
    val assets = rememberResource(signal) { app.ops.call("professional_assets") }
    val cats = rememberCategories()
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }

    ScreenScaffold("Configurar perfil", onBack = { nav.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = tab,
                containerColor = Vinko.Cream,
                contentColor = Vinko.Primary,
                edgePadding = 16.dp,
                divider = { LineDivider() },
            ) {
                SETUP_TABS.forEachIndexed { i, title ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        selectedContentColor = Vinko.Primary,
                        unselectedContentColor = Vinko.Muted,
                        text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                    )
                }
            }
            ResourceView(pro) { data ->
                val p = data.obj()
                if (p == null) {
                    EmptyBox("Activa tu perfil profesional", "Primero crea tu perfil profesional para configurarlo.", "Ir al panel") { nav.navigate(Routes.PRO) }
                    return@ResourceView
                }
                val categories = cats.data.orEmpty()
                val catName = { id: String? -> categories.firstOrNull { it.first == id }?.second ?: "Servicio profesional" }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (tab) {
                        0 -> ProfileTab(p, onSaved = pro.reload)
                        1 -> OfferingsTab(p, categories, catName, onChanged = pro.reload)
                        2 -> BranchesTab(p, assets, categories, catName, nav, onChanged = { pro.reload(); assets.reload() })
                        3 -> PortfolioTab(assets)
                        else -> ContactsTab(p, onChanged = pro.reload)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Perfil
// ---------------------------------------------------------------------------------------------

@Composable
private fun ProfileTab(p: JsonObject, onSaved: () -> Unit) {
    val app = LocalApp.current
    val action = rememberAction()
    var name by remember(p) { mutableStateOf(p.s("business_name") ?: "") }
    var bio by remember(p) { mutableStateOf(p.s("bio") ?: "") }
    var years by remember(p) { mutableStateOf(p.l("years_experience")?.toString() ?: "") }
    var accepts by remember(p) { mutableStateOf(p.b("accepts_requests")) }
    var tried by remember { mutableStateOf(false) }
    val yearsValue = years.toIntOrNull()
    val yearsError = if (tried && (years.isNotBlank() && (yearsValue == null || yearsValue !in 0..90))) "Indica un número entre 0 y 90." else null
    val nameError = if (tried && name.isBlank()) "Escribe tu nombre comercial." else null

    SectionHeader("Presentación", "Así te verán los clientes en tu perfil público.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("soyvinko.com/p/${p.s("slug").orEmpty()}", icon = Icons.Outlined.Public)
        p.s("status")?.let { status ->
            if (status == "active") Pill(statusLabel(status), Vinko.Mint, Vinko.MintBg) else Pill(statusLabel(status), Vinko.Amber, Vinko.AmberBg)
        }
    }
    VinkoTextField(name, { name = it.take(120) }, "Nombre comercial o profesional", capitalization = KeyboardCapitalization.Words, error = nameError)
    AreaField(bio, { bio = it.take(4000) }, "Acerca de ti y tu experiencia", minLines = 4, placeholder = "Especialidad, años de oficio, garantías, zonas donde trabajas…")
    VinkoTextField(
        years,
        { years = it.filter(Char::isDigit).take(2) },
        "Años de experiencia",
        keyboardType = KeyboardType.Number,
        error = yearsError,
    )
    SwitchRow(
        "Recibir nuevas solicitudes",
        if (accepts) "Apareces en búsquedas y oportunidades." else "Tu perfil queda en pausa para nuevos clientes.",
        accepts,
        { accepts = it },
    )
    PrimaryButton("Guardar cambios", loading = action.busy, onClick = {
        tried = true
        if (name.isBlank() || (years.isNotBlank() && (yearsValue == null || yearsValue !in 0..90))) return@PrimaryButton
        action.run("Perfil actualizado.") {
            app.ops.call(
                "update_professional",
                "name" to name.trim(),
                "bio" to bio.trim(),
                "years" to (yearsValue ?: 0),
                "accepts" to accepts,
            )
            onSaved()
        }
    })
}

// ---------------------------------------------------------------------------------------------
// Servicios (offerings)
// ---------------------------------------------------------------------------------------------

@Composable
private fun OfferingsTab(p: JsonObject, categories: List<Pair<String, String>>, catName: (String?) -> String, onChanged: () -> Unit) {
    val app = LocalApp.current
    val action = rememberAction()
    val form = rememberFormAction()
    var editing by remember { mutableStateOf<JsonObject?>(null) }
    var creating by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<JsonObject?>(null) }
    val offerings = p.list("offerings").sortedWith(compareByDescending<JsonObject> { it.b("active") }.thenByDescending { it.s("created_at") })

    SectionHeader("Tus servicios", "${offerings.count { it.b("active") }} activos de ${offerings.size}") {
        SmallButton("Añadir", onClick = { form.reset(); creating = true }, icon = Icons.Outlined.Add)
    }
    Banner(
        "Tus servicios se publican de inmediato. Para recibir solicitudes presenciales configura una sede con cobertura para la misma categoría.",
        BannerTone.Info,
    )
    if (offerings.isEmpty()) {
        EmptyBox("¿Qué servicios ofreces?", "Añade tu primer servicio para aparecer en las búsquedas y recibir solicitudes.", "Añadir servicio") {
            form.reset(); creating = true
        }
    }
    offerings.forEach { o ->
        val active = o.b("active")
        VCard {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(catName(o.s("category_id")).uppercase(), style = MaterialTheme.typography.labelSmall, color = Vinko.Primary)
                    Spacer(Modifier.height(2.dp))
                    Text(o.s("title") ?: "", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(8.dp))
                if (active) Pill("Activo", Vinko.Mint, Vinko.MintBg) else Pill("En pausa", Vinko.Amber, Vinko.AmberBg)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (o.s("delivery_mode") == "remote") Pill("Remoto", icon = Icons.Outlined.Public) else Pill("Presencial", icon = Icons.Outlined.LocationOn)
                Spacer(Modifier.weight(1f))
                Text(
                    o.l("price_min_minor")?.let { "Desde ${money(it, o.s("currency"))}" } ?: "Precio a convenir",
                    style = MaterialTheme.typography.titleSmall,
                    color = Vinko.Ink,
                )
            }
            o.s("description")?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("Editar", onClick = { form.reset(); editing = o }, icon = Icons.Outlined.Edit, tone = Tone.Outline)
                SmallButton(
                    if (active) "Pausar" else "Activar",
                    icon = if (active) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                    tone = Tone.Soft,
                    enabled = !action.busy,
                    onClick = {
                        action.run(if (active) "Servicio en pausa." else "Servicio activado en el catálogo.") {
                            app.ops.call(
                                "update_offering",
                                "offering" to o.s("id"),
                                "title" to (o.s("title") ?: ""),
                                "description" to (o.s("description") ?: ""),
                                "price" to (o.l("price_min_minor") ?: JsonNull),
                                "active" to !active,
                            )
                            onChanged()
                        }
                    },
                )
                Spacer(Modifier.weight(1f))
                DeleteIconButton("Eliminar ${o.s("title") ?: "servicio"}", onClick = { form.reset(); removing = o })
            }
        }
    }

    removing?.let { o ->
        DeleteSheet(
            title = "¿Eliminar «${o.s("title") ?: "este servicio"}»?",
            text = "Dejará de aparecer en el catálogo y en tu perfil. Si ya tiene solicitudes o propuestas, se archivará para conservar ese historial.",
            confirm = "Eliminar servicio",
            form = form,
            onDismiss = { removing = null },
            onConfirm = {
                form.runFor(onSuccess = { removing = null; onChanged() }) {
                    val result = app.ops.call("remove_offering", "offering" to o.s("id")).idText()
                    if (result == "archived") "Servicio archivado: se conserva en tus solicitudes anteriores." else "Servicio eliminado."
                }
            },
        )
    }

    if (creating || editing != null) {
        OfferingSheet(
            existing = editing,
            categories = categories,
            form = form,
            onDismiss = { creating = false; editing = null },
            onSaved = { creating = false; editing = null; onChanged() },
        )
    }
}

@Composable
private fun OfferingSheet(
    existing: JsonObject?,
    categories: List<Pair<String, String>>,
    form: FormAction,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val app = LocalApp.current
    var category by remember { mutableStateOf(existing?.s("category_id")) }
    var title by remember { mutableStateOf(existing?.s("title") ?: "") }
    var description by remember { mutableStateOf(existing?.s("description") ?: "") }
    var currency by remember { mutableStateOf(existing?.s("currency")?.trim() ?: "VES") }
    var price by remember { mutableStateOf(majorText(existing?.l("price_min_minor"))) }
    var mode by remember { mutableStateOf(existing?.s("delivery_mode") ?: "onsite") }
    var active by remember { mutableStateOf(existing?.b("active") ?: true) }
    var tried by remember { mutableStateOf(false) }
    val priceError = if (price.isNotBlank() && parseMajor(price) == null) "Escribe un importe válido, por ejemplo 25,00." else null

    FormSheet(
        title = if (existing == null) "Nuevo servicio" else "Editar servicio",
        subtitle = if (existing == null) "Se publicará de inmediato en el catálogo." else null,
        error = form.error,
        onDismiss = onDismiss,
    ) {
        if (existing == null) {
            PickerField(
                "Categoría", categories, category, { category = it },
                supporting = if (tried && category == null) "Selecciona una categoría." else null,
            )
        } else {
            KeyValue("Categoría", categories.firstOrNull { it.first == category }?.second ?: "—")
        }
        VinkoTextField(
            title, { title = it.take(160) }, "Título del servicio",
            capitalization = KeyboardCapitalization.Sentences,
            error = if (tried && title.trim().length < 3) "Escribe un título de al menos 3 caracteres." else null,
            supporting = "Ejemplo: Instalación y mantenimiento de aires acondicionados",
        )
        AreaField(description, { description = it.take(4000) }, "Descripción", placeholder = "Qué incluye, materiales, garantía y alcance.")
        if (existing == null) {
            Text("Moneda", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
            Segmented(listOf("VES" to "Bs · Bolívar", "USD" to "USD · Dólar"), currency, { currency = it })
        } else {
            KeyValue("Moneda", if (currency == "USD") "USD · Dólar" else "Bs · Bolívar")
        }
        MoneyField(
            price, { price = it }, currency, "Precio desde",
            supporting = "Opcional. Déjalo vacío si cotizas cada solicitud.",
            error = priceError,
        )
        Text("Modalidad", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
        Segmented(listOf("onsite" to "Presencial", "remote" to "Remoto"), mode, { mode = it })
        Text(
            if (mode == "onsite") "Atiendes en tu sede o en la dirección del cliente, dentro de tu cobertura." else "Trabajas a distancia. El pago queda protegido en la plataforma.",
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Muted,
        )
        if (existing != null) SwitchRow("Servicio activo", "Visible en búsquedas y en tu perfil.", active, { active = it })
        PrimaryButton(if (existing == null) "Publicar servicio" else "Guardar cambios", loading = form.busy, onClick = {
            tried = true
            if ((existing == null && category == null) || title.trim().length < 3 || priceError != null) return@PrimaryButton
            val minor = parseMajor(price)
            form.run(if (existing == null) "Servicio publicado." else "Servicio actualizado.", onSuccess = onSaved) {
                if (existing == null) {
                    val id = app.ops.call(
                        "add_offering",
                        "category" to category,
                        "title" to title.trim(),
                        "currency" to currency,
                        "price_minor" to minor,
                        "description" to description.trim(),
                    ).idText()
                    if (id != null) app.ops.call("set_offering_delivery", "offering" to id, "mode" to mode)
                } else {
                    app.ops.call(
                        "update_offering",
                        "offering" to existing.s("id"),
                        "title" to title.trim(),
                        "description" to description.trim(),
                        "price" to (minor ?: JsonNull),
                        "active" to active,
                    )
                    if (mode != existing.s("delivery_mode")) app.ops.call("set_offering_delivery", "offering" to existing.s("id"), "mode" to mode)
                }
            }
        })
    }
}

// ---------------------------------------------------------------------------------------------
// Sedes, horarios y cobertura
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BranchesTab(
    p: JsonObject,
    assets: Resource<kotlinx.serialization.json.JsonElement>,
    categories: List<Pair<String, String>>,
    catName: (String?) -> String,
    nav: NavHostController,
    onChanged: () -> Unit,
) {
    val form = rememberFormAction()
    var hoursFor by remember { mutableStateOf<JsonObject?>(null) }
    var coverageFor by remember { mutableStateOf<JsonObject?>(null) }
    var removing by remember { mutableStateOf<JsonObject?>(null) }
    val app = LocalApp.current
    val branches = p.list("branches")
    val coverage = p.list("coverage")
    val hours = assets.data.obj().list("hours")
    val offeredCategories = p.list("offerings").mapNotNull { it.s("category_id") }.distinct()

    SectionHeader("Sedes de atención", if (branches.size == 1) "1 registrada" else "${branches.size} registradas") {
        SmallButton("Añadir sede", onClick = { nav.navigate(BRANCH_NEW) }, icon = Icons.Outlined.Add)
    }
    Banner(
        "Los clientes dentro del radio de cobertura de tus sedes verán tus servicios presenciales y te llegarán sus solicitudes. Los servicios remotos no necesitan sede.",
        BannerTone.Info,
    )
    if (branches.isEmpty()) {
        EmptyBox("Sin sedes registradas", "Registra tu sede, taller u oficina para definir la zona donde atiendes.", "Añadir sede") {
            nav.navigate(BRANCH_NEW)
        }
    }
    branches.forEach { b ->
        val id = b.s("id")
        val branchHours = hours.filter { it.s("branch_id") == id }.sortedWith(compareBy({ (it.l("weekday") ?: 0L).let { d -> if (d == 0L) 7L else d } }, { it.s("opens_at") }))
        val branchCoverage = coverage.filter { it.s("branch_id") == id && (it.s("active") == null || it.b("active")) }
        VCard {
            Row(verticalAlignment = Alignment.Top) {
                IconTile(Icons.Outlined.Storefront)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(b.s("name") ?: "Sede", style = MaterialTheme.typography.titleSmall)
                    Text(b.s("address_line") ?: "", style = MaterialTheme.typography.bodySmall)
                    Text(
                        listOfNotNull(b.s("parish_name"), b.s("municipality_name") ?: b.s("city"), b.s("state_name")).joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Vinko.Muted,
                    )
                }
                if (b.b("public_address")) Pill("Pública", Vinko.Mint, Vinko.MintBg) else Pill("Privada", Vinko.Muted, Vinko.Tint)
            }
            Spacer(Modifier.height(12.dp))
            LineDivider()
            Spacer(Modifier.height(10.dp))
            Text("Horario de atención", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
            Spacer(Modifier.height(6.dp))
            if (branchHours.isEmpty()) {
                Text("Sin horario registrado.", style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    branchHours.forEach { h ->
                        Pill("${WEEKDAYS_SHORT.getOrElse((h.l("weekday") ?: 0L).toInt()) { "" }} ${hhmm(h.s("opens_at"))}–${hhmm(h.s("closes_at"))}", Vinko.Ink, Vinko.Cream)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Cobertura", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
            Spacer(Modifier.height(6.dp))
            if (branchCoverage.isEmpty()) {
                Text(
                    "Sin cobertura configurada: no recibirás solicitudes presenciales desde esta sede.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Amber,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    branchCoverage.forEach { c ->
                        Pill("${catName(c.s("category_id"))} · ${kmText(c.l("radius_m"))}", icon = Icons.Outlined.Map)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("Horario", onClick = { form.reset(); hoursFor = b }, icon = Icons.Outlined.Schedule, tone = Tone.Outline)
                SmallButton("Cobertura", onClick = { form.reset(); coverageFor = b }, icon = Icons.Outlined.Map, tone = Tone.Soft)
                Spacer(Modifier.weight(1f))
                DeleteIconButton("Eliminar sede ${b.s("name") ?: ""}".trim(), onClick = { form.reset(); removing = b })
            }
        }
    }

    removing?.let { b ->
        val id = b.s("id")
        val areas = coverage.count { it.s("branch_id") == id }
        val days = hours.count { it.s("branch_id") == id }
        val linked = p.list("contacts").count { it.s("branch_id") == id }
        DeleteSheet(
            title = "¿Eliminar la sede «${b.s("name") ?: "Sede"}»?",
            text = buildString {
                append("También se eliminarán sus ")
                append(if (areas == 1) "1 zona de cobertura" else "$areas zonas de cobertura")
                append(" y ")
                append(if (days == 1) "1 horario" else "$days horarios")
                append(". Dejarás de recibir solicitudes presenciales desde esta sede.")
                if (linked > 0) append(if (linked == 1) " El contacto asociado quedará como general." else " Los $linked contactos asociados quedarán como generales.")
            },
            confirm = "Eliminar sede",
            form = form,
            onDismiss = { removing = null },
            onConfirm = {
                form.run("Sede eliminada.", onSuccess = { removing = null; onChanged() }) {
                    app.ops.call("remove_branch", "branch" to id)
                }
            },
        )
    }
    hoursFor?.let { b -> HoursSheet(b, form, onDismiss = { hoursFor = null }, onSaved = { hoursFor = null; onChanged() }) }
    coverageFor?.let { b ->
        val ordered = categories.sortedBy { if (it.first in offeredCategories) 0 else 1 }
        CoverageSheet(b, ordered, offeredCategories.firstOrNull(), form, onDismiss = { coverageFor = null }, onSaved = { coverageFor = null; onChanged() })
    }
}

private fun kmText(meters: Long?): String {
    val m = meters ?: return ""
    return if (m % 1000 == 0L) "${m / 1000} km" else (oneDecimal(m / 1000.0) + " km")
}

private val TIME = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

@Composable
private fun HoursSheet(branch: JsonObject, form: FormAction, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val app = LocalApp.current
    var days by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    var opens by remember { mutableStateOf("08:00") }
    var closes by remember { mutableStateOf("17:00") }
    val valid = TIME.matches(opens) && TIME.matches(closes) && closes > opens && days.isNotEmpty()
    FormSheet(
        title = "Horario de ${branch.s("name") ?: "la sede"}",
        subtitle = "Elige los días y el horario. Si un día ya tiene ese horario de apertura, se actualiza el cierre.",
        error = form.error,
        onDismiss = onDismiss,
    ) {
        Text("Días", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 2, 3, 4, 5, 6, 0).forEach { d ->
                val on = d in days
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) Vinko.Primary else Color.White)
                        .border(1.dp, if (on) Vinko.Primary else Vinko.Line, RoundedCornerShape(10.dp))
                        .clickable { days = if (on) days - d else days + d }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(WEEKDAYS_SHORT[d], style = MaterialTheme.typography.labelMedium, color = if (on) Color.White else Vinko.Ink)
                }
            }
        }
        ChoiceChips(
            listOf("08:00-17:00" to "08:00–17:00", "09:00-18:00" to "09:00–18:00", "07:00-15:00" to "07:00–15:00", "08:00-12:00" to "08:00–12:00"),
            "$opens-$closes",
            { v -> opens = v.substringBefore('-'); closes = v.substringAfter('-') },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                VinkoTextField(opens, { opens = timeInput(it) }, "Abre (HH:MM)", keyboardType = KeyboardType.Number, error = if (!TIME.matches(opens)) "Formato 08:00" else null)
            }
            Box(Modifier.weight(1f)) {
                VinkoTextField(
                    closes, { closes = timeInput(it) }, "Cierra (HH:MM)", keyboardType = KeyboardType.Number,
                    error = when {
                        !TIME.matches(closes) -> "Formato 17:00"
                        TIME.matches(opens) && closes <= opens -> "Debe ser después"
                        else -> null
                    },
                )
            }
        }
        PrimaryButton("Guardar horario", loading = form.busy, enabled = valid, onClick = {
            form.run("Horario guardado.", onSuccess = onSaved) {
                days.sorted().forEach { d ->
                    app.ops.call("set_branch_hours", "branch" to branch.s("id"), "weekday" to d, "opens" to opens, "closes" to closes)
                }
            }
        })
    }
}

/** Acepta "0830" o "08:30" mientras se escribe. */
private fun timeInput(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(4)
    return if (digits.length <= 2) digits else digits.take(2) + ":" + digits.drop(2)
}

@Composable
private fun CoverageSheet(
    branch: JsonObject,
    categories: List<Pair<String, String>>,
    preselected: String?,
    form: FormAction,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val app = LocalApp.current
    var category by remember { mutableStateOf(preselected) }
    var km by remember { mutableStateOf("10") }
    FormSheet(
        title = "Cobertura de ${branch.s("name") ?: "la sede"}",
        subtitle = "Recibirás solicitudes presenciales de esta categoría dentro del radio elegido alrededor de la sede.",
        error = form.error,
        onDismiss = onDismiss,
    ) {
        PickerField("Categoría", categories, category, { category = it })
        Text("Radio de atención", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
        ChoiceChips(listOf("2", "5", "10", "25", "50", "100", "200").map { it to "$it km" }, km, { km = it })
        PrimaryButton("Guardar cobertura", loading = form.busy, enabled = category != null, onClick = {
            form.run("Cobertura guardada.", onSuccess = onSaved) {
                app.ops.call("set_coverage", "branch" to branch.s("id"), "category" to category, "radius_m" to km.toInt() * 1000)
            }
        })
    }
}

// ---------------------------------------------------------------------------------------------
// Portafolio y credenciales
// ---------------------------------------------------------------------------------------------

@Composable
private fun PortfolioTab(assets: Resource<kotlinx.serialization.json.JsonElement>) {
    val form = rememberFormAction()
    var addingWork by remember { mutableStateOf(false) }
    var addingCredential by remember { mutableStateOf(false) }
    var removingWork by remember { mutableStateOf<JsonObject?>(null) }
    var removingCredential by remember { mutableStateOf<JsonObject?>(null) }
    val app = LocalApp.current
    ResourceView(assets) { data ->
        val a = data.obj()
        val portfolio = a.list("portfolio").sortedByDescending { it.s("created_at") }
        val credentials = a.list("credentials")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader("Trabajos realizados", "Fotos de tus mejores resultados") {
                SmallButton("Añadir", onClick = { form.reset(); addingWork = true }, icon = Icons.Outlined.Add)
            }
            Text(
                "Las fotos se muestran en tu perfil público cuando son aprobadas por moderación.",
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Muted,
            )
            if (portfolio.isEmpty()) {
                VCard {
                    Text("Aún no tienes trabajos en tu portafolio", style = MaterialTheme.typography.titleSmall)
                    Text("Los clientes confían más cuando ven resultados reales.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
            }
            portfolio.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { item ->
                        Column(Modifier.weight(1f)) {
                            Box {
                                ProImage(item.s("file_id"), Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)))
                                DeleteIconButton(
                                    "Eliminar trabajo ${item.s("title") ?: ""}".trim(),
                                    onClick = { form.reset(); removingWork = item },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                    size = 34.dp,
                                    overlay = true,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(item.s("title") ?: "", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            item.s("description")?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(4.dp))
            SectionHeader("Credenciales", "Títulos, cursos y certificaciones") {
                SmallButton("Añadir", onClick = { form.reset(); addingCredential = true }, icon = Icons.Outlined.Add)
            }
            if (credentials.isEmpty()) {
                VCard {
                    Text("Sin credenciales registradas", style = MaterialTheme.typography.titleSmall)
                    Text("Añade tus certificados para destacar tu preparación.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
            }
            credentials.forEach { c ->
                VCard {
                    Row(verticalAlignment = Alignment.Top) {
                        IconTile(Icons.Outlined.WorkspacePremium, Vinko.Amber, Vinko.AmberBg)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.s("title") ?: "", style = MaterialTheme.typography.titleSmall)
                            Text(c.s("issuer") ?: "", style = MaterialTheme.typography.bodySmall)
                            val dates = listOfNotNull(
                                c.s("issued_on")?.let { "Expedida ${localDateText(it)}" },
                                c.s("expires_on")?.let { "Vence ${localDateText(it)}" },
                            ).joinToString(" · ")
                            if (dates.isNotEmpty()) Text(dates, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (c.s("verified_at") != null) Pill("Verificada", Vinko.Mint, Vinko.MintBg) else Pill("En revisión", Vinko.Amber, Vinko.AmberBg)
                            Spacer(Modifier.height(8.dp))
                            DeleteIconButton("Eliminar credencial ${c.s("title") ?: ""}".trim(), onClick = { form.reset(); removingCredential = c }, size = 36.dp)
                        }
                    }
                }
            }
        }
    }
    removingWork?.let { item ->
        DeleteSheet(
            title = "¿Eliminar «${item.s("title") ?: "este trabajo"}»?",
            text = "La foto dejará de mostrarse en tu perfil público y se borrará de forma definitiva.",
            confirm = "Eliminar trabajo",
            form = form,
            onDismiss = { removingWork = null },
            onConfirm = {
                form.run("Trabajo eliminado del portafolio.", onSuccess = { removingWork = null; assets.reload() }) {
                    app.ops.call("remove_portfolio_item", "item" to item.s("id"))
                }
            },
        )
    }
    removingCredential?.let { c ->
        DeleteSheet(
            title = "¿Eliminar «${c.s("title") ?: "esta credencial"}»?",
            text = "Se eliminará la credencial y, si adjuntaste un documento, también se borrará.",
            confirm = "Eliminar credencial",
            form = form,
            onDismiss = { removingCredential = null },
            onConfirm = {
                form.run("Credencial eliminada.", onSuccess = { removingCredential = null; assets.reload() }) {
                    app.ops.call("remove_credential", "credential" to c.s("id"))
                }
            },
        )
    }
    if (addingWork) PortfolioSheet(form, onDismiss = { addingWork = false }, onSaved = { addingWork = false; assets.reload() })
    if (addingCredential) CredentialSheet(form, onDismiss = { addingCredential = false }, onSaved = { addingCredential = false; assets.reload() })
}

@Composable
private fun PortfolioSheet(form: FormAction, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val app = LocalApp.current
    var photo by remember { mutableStateOf<UploadPart?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val pick = rememberPhotoPicker { photo = it.firstOrNull() }
    FormSheet("Añadir trabajo", onDismiss, subtitle = "Sube una foto del resultado y descríbelo brevemente.", error = form.error) {
        PhotoSlot(photo, if (photo == null) "Elegir foto del trabajo" else "Cambiar foto", pick)
        if (tried && photo == null) Text("Selecciona una foto.", style = MaterialTheme.typography.bodySmall, color = Vinko.Coral)
        VinkoTextField(title, { title = it.take(160) }, "Título del trabajo", capitalization = KeyboardCapitalization.Sentences,
            error = if (tried && title.isBlank()) "Escribe un título." else null)
        AreaField(description, { description = it.take(2000) }, "Descripción del resultado", minLines = 2)
        PrimaryButton("Publicar en mi portafolio", loading = form.busy, onClick = {
            tried = true
            val part = photo ?: return@PrimaryButton
            if (title.isBlank()) return@PrimaryButton
            form.run("Trabajo añadido. Se mostrará cuando la foto sea aprobada.", onSuccess = onSaved) {
                val file = app.uploadFile(part, "portfolio")
                app.ops.call("add_portfolio", "title" to title.trim(), "description" to description.trim(), "file" to file)
            }
        })
    }
}

@Composable
private fun CredentialSheet(form: FormAction, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val app = LocalApp.current
    var title by remember { mutableStateOf("") }
    var issuer by remember { mutableStateOf("") }
    var issued by remember { mutableStateOf<LocalDate?>(null) }
    var expires by remember { mutableStateOf<LocalDate?>(null) }
    var evidence by remember { mutableStateOf<UploadPart?>(null) }
    var tried by remember { mutableStateOf(false) }
    val pick = rememberPhotoPicker { evidence = it.firstOrNull() }
    val datesError = if (issued != null && expires != null && expires!! <= issued!!) "La fecha de vencimiento debe ser posterior a la de expedición." else null
    FormSheet("Añadir credencial", onDismiss, subtitle = "Certificados, títulos o cursos que respalden tu oficio.", error = form.error) {
        VinkoTextField(title, { title = it.take(200) }, "Título o certificación", capitalization = KeyboardCapitalization.Sentences,
            error = if (tried && title.isBlank()) "Escribe el nombre de la credencial." else null)
        VinkoTextField(issuer, { issuer = it.take(200) }, "Institución emisora", capitalization = KeyboardCapitalization.Words,
            error = if (tried && issuer.isBlank()) "Indica quién la emitió." else null)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { DateField("Expedición", issued) { issued = it } }
            Box(Modifier.weight(1f)) { DateField("Vencimiento", expires) { expires = it } }
        }
        datesError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Coral) }
        PhotoSlot(evidence, if (evidence == null) "Foto del documento (opcional)" else "Cambiar documento", pick, height = 120.dp)
        PrimaryButton("Guardar credencial", loading = form.busy, onClick = {
            tried = true
            if (title.isBlank() || issuer.isBlank() || datesError != null) return@PrimaryButton
            form.run("Credencial añadida.", onSuccess = onSaved) {
                val file = evidence?.let { app.uploadFile(it, "evidence") }
                app.ops.call(
                    "add_credential",
                    "title" to title.trim(),
                    "issuer" to issuer.trim(),
                    "file" to file,
                    "issued" to issued?.toString(),
                    "expires" to expires?.toString(),
                )
            }
        })
    }
}

// ---------------------------------------------------------------------------------------------
// Contactos
// ---------------------------------------------------------------------------------------------

private val CONTACT_KINDS = listOf("whatsapp" to "WhatsApp", "phone" to "Teléfono", "email" to "Correo", "website" to "Sitio web")

private fun contactIcon(kind: String?) = when (kind) {
    "whatsapp" -> Icons.Outlined.Whatsapp
    "email" -> Icons.Outlined.Email
    "website" -> Icons.Outlined.Language
    else -> Icons.Outlined.Phone
}

@Composable
private fun ContactsTab(p: JsonObject, onChanged: () -> Unit) {
    val form = rememberFormAction()
    var adding by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<JsonObject?>(null) }
    val app = LocalApp.current
    val contacts = p.list("contacts")
    val branches = p.list("branches")
    SectionHeader("Canales de contacto", "Para coordinar con tus clientes") {
        SmallButton("Añadir", onClick = { form.reset(); adding = true }, icon = Icons.Outlined.Add)
    }
    Banner(
        "Tus canales se comparten con el cliente solo cuando se formaliza un contrato digital, para coordinar el servicio.",
        BannerTone.Info,
    )
    if (contacts.isEmpty()) {
        EmptyBox("Sin canales de contacto", "Añade al menos un WhatsApp o teléfono para que tus clientes puedan contactarte al iniciar un contrato.", "Añadir contacto") {
            form.reset(); adding = true
        }
    } else {
        VCard(padding = 6.dp) {
            contacts.forEachIndexed { i, c ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(contactIcon(c.s("kind")))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.s("label") ?: "", style = MaterialTheme.typography.titleSmall)
                        Text(c.s("value") ?: "", style = MaterialTheme.typography.bodyMedium)
                        val branch = branches.firstOrNull { it.s("id") == c.s("branch_id") }?.s("name")
                        Text(
                            listOfNotNull(CONTACT_KINDS.firstOrNull { it.first == c.s("kind") }?.second, branch?.let { "Sede $it" }).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Vinko.Muted,
                        )
                    }
                    if (c.s("verified_at") != null) {
                        Pill("Verificado", Vinko.Mint, Vinko.MintBg)
                        Spacer(Modifier.width(8.dp))
                    }
                    DeleteIconButton("Eliminar contacto ${c.s("label") ?: ""}".trim(), onClick = { form.reset(); removing = c }, size = 36.dp)
                }
                if (i < contacts.lastIndex) LineDivider(Modifier.padding(horizontal = 10.dp))
            }
        }
    }
    removing?.let { c ->
        DeleteSheet(
            title = "¿Eliminar «${c.s("label") ?: "este contacto"}»?",
            text = "Los clientes de nuevos contratos ya no verán ${c.s("value") ?: "este canal"}. Los contratos anteriores no cambian.",
            confirm = "Eliminar contacto",
            form = form,
            onDismiss = { removing = null },
            onConfirm = {
                form.run("Contacto eliminado.", onSuccess = { removing = null; onChanged() }) {
                    app.ops.call("remove_professional_contact", "contact" to c.s("id"))
                }
            },
        )
    }
    if (adding) ContactSheet(branches, form, onDismiss = { adding = false }, onSaved = { adding = false; onChanged() })
}

@Composable
private fun ContactSheet(branches: List<JsonObject>, form: FormAction, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val app = LocalApp.current
    var kind by remember { mutableStateOf("whatsapp") }
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val valueError = when {
        !tried -> null
        kind == "email" && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(value.trim()) -> "Escribe un correo válido."
        (kind == "phone" || kind == "whatsapp") && value.count(Char::isDigit) < 7 -> "Escribe un número con código de área, por ejemplo 0414 1234567."
        kind == "website" && !value.contains('.') -> "Escribe una dirección web válida."
        else -> null
    }
    FormSheet("Nuevo canal de contacto", onDismiss, error = form.error) {
        ChoiceChips(CONTACT_KINDS, kind, { kind = it })
        VinkoTextField(label, { label = it.take(80) }, "Nombre del canal", capitalization = KeyboardCapitalization.Sentences,
            supporting = "Ejemplo: WhatsApp de atención, Oficina",
            error = if (tried && label.isBlank()) "Escribe un nombre para el canal." else null)
        VinkoTextField(
            value, { value = it.take(200) },
            when (kind) { "email" -> "Correo electrónico"; "website" -> "Dirección web"; else -> "Número de teléfono" },
            keyboardType = when (kind) { "email" -> KeyboardType.Email; "website" -> KeyboardType.Uri; else -> KeyboardType.Phone },
            error = valueError,
        )
        if (branches.isNotEmpty()) {
            PickerField(
                "Sede asociada (opcional)",
                listOf("" to "Sin sede (general)") + branches.mapNotNull { b -> b.s("id")?.let { it to (b.s("name") ?: "Sede") } },
                branch,
                { branch = it },
            )
        }
        PrimaryButton("Guardar contacto", loading = form.busy, onClick = {
            tried = true
            val v = value.trim()
            val ok = label.isNotBlank() && when (kind) {
                "email" -> Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(v)
                "website" -> v.contains('.')
                else -> v.count(Char::isDigit) >= 7
            }
            if (!ok) return@PrimaryButton
            form.run("Contacto guardado.", onSuccess = onSaved) {
                app.ops.call(
                    "add_professional_contact",
                    "branch" to (branch.ifEmpty { null } ?: JsonNull),
                    "kind" to kind,
                    "label" to label.trim(),
                    "value" to v,
                )
            }
        })
    }
}
