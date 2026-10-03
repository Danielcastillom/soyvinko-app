package com.soyvinko.app.ui.feature.explore

import kotlin.time.Duration.Companion.days
import com.soyvinko.app.ui.common.isoSeconds
import com.soyvinko.app.ui.common.nowInstant
import com.soyvinko.app.ui.common.newUuid
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.ErrorBox
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.rememberPhotoPicker
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.uploadFile
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive

private const val MAX_PHOTOS = 6

/** Ejecuta `block` al volver a la pantalla (p. ej. después de agregar una dirección), no en la primera visita. */
@Composable
fun OnResume(block: () -> Unit) {
    var first by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (first) first = false else block()
    }
}

/**
 * Crear una solicitud, igual que NewRequest de la web (frontend/src/pages/Requests.tsx):
 * - directa a un servicio de un profesional → `request_offering`;
 * - abierta (presencial o remota) → `create_customer_request`.
 * La clave de idempotencia y el vencimiento se fijan una vez por formulario.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewRequestScreen(nav: NavHostController, professional: String, offering: String) {
    val app = LocalApp.current
    val user = LocalUser.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val target = professional.ifBlank { null }
    val offeringId = offering.ifBlank { null }
    val direct = target != null || offeringId != null

    val cats = rememberResource { app.ops.call("catalog").categories() }
    val account = rememberResource { app.ops.call("my_account").obj() }
    val profile = rememberResource(target) { Found(target?.let { app.ops.call("professional_profile", "professional" to it).obj()?.toPro() }) }
    OnResume { account.reload() }

    var openMode by rememberSaveable { mutableStateOf("onsite") }
    var picked by rememberSaveable { mutableStateOf(offeringId.orEmpty()) }
    var category by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var currencyChoice by rememberSaveable { mutableStateOf<String?>(null) }
    var amount by rememberSaveable { mutableStateOf("") }
    var addressChoice by rememberSaveable { mutableStateOf<String?>(null) }
    var radius by rememberSaveable { mutableIntStateOf(25000) }
    var protect by rememberSaveable { mutableStateOf(false) }
    val key = rememberSaveable { newUuid() }
    val expiresAt = rememberSaveable { (nowInstant() + 7.days).isoSeconds() }
    val photos = remember { mutableStateListOf<UploadPart>() }
    val uploaded = remember { mutableMapOf<UploadPart, String>() }
    var photoError by remember { mutableStateOf<String?>(null) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    var saving by remember { mutableStateOf(false) }

    val pickPhotos = rememberPhotoPicker(max = (MAX_PHOTOS - photos.size).coerceAtLeast(1)) { parts ->
        scope.launch(Dispatchers.Main.immediate) {
            val ready = withContext(Dispatchers.Default) { parts.map { normalizePhoto(it) } }
            val room = MAX_PHOTOS - photos.size
            photoError = when {
                ready.any { it == null } -> "Usa fotos JPG, PNG o WEBP de hasta 10 MB cada una."
                ready.size > room -> "Puedes adjuntar hasta 6 fotos."
                else -> null
            }
            photos.addAll(ready.filterNotNull().take(room))
        }
    }

    val pro = profile.data?.value
    val choices = pro?.offerings.orEmpty().filter { offeringId == null || it.id == offeringId }
    val service = choices.singleOrNull() ?: choices.firstOrNull { it.id == picked }
    val mode = if (direct) service?.deliveryMode else openMode
    val currency = currencyChoice ?: if (direct) service?.currency ?: "VES" else "VES"
    val addresses = account.data?.list("addresses").orEmpty().filter { it.s("country_code") == "VE" }
    val address = addressChoice?.takeIf { id -> addresses.any { it.s("id") == id } }
        ?: (addresses.firstOrNull { it.b("is_default") } ?: addresses.firstOrNull())?.s("id")
    val budget = parseMajorToMinor(amount)
    val categories = cats.data.orEmpty()

    fun validate(): Map<String, String> = buildMap {
        if (!direct && category.isBlank()) put("category", "Elige una categoría.")
        if (!direct && title.trim().length !in 5..160) put("title", "Escribe un título de 5 a 160 caracteres.")
        if (description.trim().length < 10) put("description", "Describe lo que necesitas con al menos 10 caracteres.")
        if (!direct && amount.isNotBlank() && (budget == null || budget <= 0)) put("amount", "Escribe un monto válido mayor que cero, por ejemplo 1.500,00.")
        if (mode == "onsite" && address == null) put("address", "Elige la dirección donde necesitas el servicio.")
    }

    fun submit() {
        val found = validate()
        errors = found
        if (found.isNotEmpty()) {
            scope.launch(Dispatchers.Main.immediate) { snackbar.showSnackbar("Revisa los campos marcados.") }
            return
        }
        scope.launch(Dispatchers.Main.immediate) {
            saving = true
            try {
                val files = photos.map { part -> uploaded[part] ?: app.uploadFile(part, "request").also { uploaded[part] = it } }
                val common = mapOf(
                    "key" to key,
                    "expires_at" to expiresAt,
                    "description" to description.trim(),
                    "currency" to currency,
                    "address" to if (mode == "onsite") address else JsonNull,
                    "files" to files,
                    "protect" to (mode == "remote" || protect),
                )
                val result = if (direct) {
                    app.ops.call("request_offering", common + ("offering" to service!!.id))
                } else {
                    app.ops.call(
                        "create_customer_request",
                        common + mapOf(
                            "category" to category,
                            "delivery_mode" to mode,
                            "title" to title.trim(),
                            "budget" to (budget ?: JsonNull),
                            "radius_m" to if (mode == "onsite") radius else JsonNull,
                        ),
                    )
                }
                val id = result.jsonPrimitive.content
                nav.navigate(Routes.request(id)) { popUpTo(Routes.NEW_REQUEST) { inclusive = true } }
            } catch (e: ApiException) {
                if (e.fieldErrors.isNotEmpty()) errors = e.fieldErrors
                snackbar.showSnackbar(e.message)
            } finally {
                saving = false
            }
        }
    }

    ScreenScaffold(if (direct) "Solicitar servicio" else "Nueva solicitud", onBack = { nav.popBackStack() }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                cats.data == null || (direct && profile.data == null) -> {
                    val error = cats.error ?: profile.error
                    if (error != null && !cats.loading && !profile.loading) ErrorBox(error) { cats.reload(); profile.reload() } else LoadingBox()
                }
                target != null && target == user.id -> EmptyBox(
                    "Este servicio es tuyo",
                    "No puedes solicitar tus propios servicios. Así lo verán tus clientes en tu perfil.",
                    "Ver mi perfil",
                ) { nav.navigate(Routes.professional(target)) }
                direct && choices.isEmpty() -> EmptyBox(
                    "Selecciona el servicio que deseas contratar",
                    "Abre el perfil y elige un servicio disponible para conservar sus datos.",
                    "Ver servicios",
                ) { if (target != null) nav.navigate(Routes.professional(target)) else goExplore(nav) }
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column {
                        SectionLabel(if (direct) "Solicitar un servicio" else "Publicar una necesidad")
                        Text(if (direct) "Cuéntale qué necesitas" else "¿Qué necesitas resolver?", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            if (direct) "Los datos del servicio ya están seleccionados." else "Recibe propuestas de varios profesionales y elige quién te ayuda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Vinko.Muted,
                        )
                    }

                    if (direct && service == null) {
                        VCard {
                            Text("Elige el servicio", style = MaterialTheme.typography.titleMedium)
                            Text("${pro?.name ?: "El profesional"} ofrece varios servicios. ¿Cuál necesitas?", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                            Spacer(Modifier.height(8.dp))
                            choices.forEach { o ->
                                OptionRow(selected = false, onClick = { picked = o.id }) {
                                    Text(o.title, style = MaterialTheme.typography.titleSmall)
                                    Text("${statusLabelMode(o.deliveryMode)} · ${o.priceLabel()}", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                                }
                            }
                        }
                        return@Column
                    }

                    if (direct && service != null) {
                        VCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ModeChip(service.deliveryMode)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    categories.firstOrNull { it.id == service.categoryId }?.name.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Vinko.Muted,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (choices.size > 1) TextButton(onClick = { picked = "" }) { Text("Cambiar", color = Vinko.Primary) }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(service.title, style = MaterialTheme.typography.titleMedium)
                            if (pro != null) {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Avatar(pro.initials, 28.dp, border = false)
                                    Spacer(Modifier.width(8.dp))
                                    Text(pro.name, style = MaterialTheme.typography.bodyMedium)
                                    if (pro.verified) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Outlined.Verified, contentDescription = "Verificado", tint = Vinko.Mint, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Precio anunciado: ${service.priceLabel().replaceFirstChar { it.lowercase() }}", style = MaterialTheme.typography.titleSmall, color = Vinko.Primary)
                            Text(
                                "El importe final depende de lo que necesitas. No se convierte automáticamente entre Bs y USD.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Vinko.Muted,
                            )
                        }
                    } else {
                        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ModeCard(Icons.Outlined.Place, "Presencial", "Un profesional va al lugar", openMode == "onsite", Modifier.weight(1f).fillMaxHeight(), !saving) { openMode = "onsite" }
                            ModeCard(Icons.Outlined.Public, "Remoto", "Trabajan desde cualquier lugar", openMode == "remote", Modifier.weight(1f).fillMaxHeight(), !saving) { openMode = "remote" }
                        }
                    }

                    VCard {
                        Text(if (direct) "Tu solicitud al profesional" else "Detalles de tu necesidad", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        if (!direct) {
                            SelectField(
                                label = "Categoría",
                                options = categories.ordered().map { it.id to it.name },
                                selected = category,
                                onSelect = {
                                    category = it
                                    errors = errors - "category"
                                },
                                error = errors["category"],
                                placeholder = "Elige una categoría",
                                icons = { id -> categoryIcon(categories.firstOrNull { it.id == id }?.slug) },
                                enabled = !saving,
                            )
                            Spacer(Modifier.height(8.dp))
                            VinkoTextField(
                                value = title,
                                onValueChange = {
                                    title = it.take(160)
                                    errors = errors - "title"
                                },
                                label = "Título de tu solicitud",
                                error = errors["title"],
                                supporting = "Ejemplo: necesito reparar una fuga en la cocina.",
                                capitalization = KeyboardCapitalization.Sentences,
                                enabled = !saving,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        AreaField(
                            value = description,
                            onValueChange = {
                                description = it
                                errors = errors - "description"
                            },
                            label = when {
                                direct -> "Explica la falla o lo que necesitas"
                                mode == "remote" -> "Requisitos y resultado esperado"
                                else -> "Describe lo que necesitas"
                            },
                            error = errors["description"],
                            supporting = "Describe el problema y el resultado que esperas. Añade fotos debajo si te ayudan a explicarlo.",
                            enabled = !saving,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Moneda de pago", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        Segmented(
                            options = listOf("VES" to "Bs · Bolívar", "USD" to "USD · Dólar"),
                            selected = currency,
                            onSelect = { if (!saving) currencyChoice = it },
                        )
                        Text(
                            "El profesional cotizará en la moneda elegida. Confirmarás el importe antes de asignar el trabajo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Vinko.Muted,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        if (!direct) {
                            Spacer(Modifier.height(12.dp))
                            VinkoTextField(
                                value = amount,
                                onValueChange = {
                                    amount = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(18)
                                    errors = errors - "amount"
                                },
                                label = "${if (mode == "remote") "Presupuesto ofrecido" else "Presupuesto máximo"} en ${currencyShort(currency)}",
                                error = errors["amount"],
                                supporting = if (budget != null && budget > 0) "Se publicará como ${money(budget, currency)}."
                                else "Opcional. Déjalo vacío si prefieres recibir cotizaciones.",
                                keyboardType = KeyboardType.Decimal,
                                enabled = !saving,
                            )
                        }
                    }

                    if (mode == "onsite") {
                        VCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Dirección del servicio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                if (addresses.isNotEmpty()) TextButton(onClick = { nav.navigate(Routes.ADDRESSES) }) { Text("Gestionar", color = Vinko.Primary) }
                            }
                            Text("Elige o guarda el lugar donde necesitas el servicio.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                            Spacer(Modifier.height(10.dp))
                            when {
                                account.data == null && account.loading -> Text("Cargando tus direcciones…", style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
                                account.data == null -> ErrorBox(account.error ?: "Inténtalo de nuevo.", account.reload)
                                addresses.isEmpty() -> {
                                    Banner(
                                        "Aún no tienes direcciones guardadas en Venezuela. Agrega una para que el profesional sepa dónde atenderte; " +
                                            "tu dirección exacta solo se comparte cuando lo autorizas dentro del contrato.",
                                        BannerTone.Info,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    SecondaryButton("Agregar una dirección", onClick = { nav.navigate(Routes.ADDRESSES) })
                                }
                                else -> addresses.forEach { a ->
                                    val id = a.s("id").orEmpty()
                                    OptionRow(
                                        selected = id == address,
                                        onClick = {
                                            addressChoice = id
                                            errors = errors - "address"
                                        },
                                        icon = Icons.Outlined.Home,
                                    ) {
                                        Text(a.s("label") ?: "Dirección", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            listOfNotNull(a.s("parish_name") ?: a.s("city"), a.s("address_line")).joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Vinko.Muted,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            errors["address"]?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Coral, modifier = Modifier.padding(top = 6.dp)) }
                            if (!direct) {
                                Spacer(Modifier.height(14.dp))
                                Text("Radio de búsqueda", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(5000, 10000, 25000, 50000).forEach { r -> Pill("${r / 1000} km", radius == r, { if (!saving) radius = r }) }
                                }
                                Text(
                                    "Los profesionales con cobertura en esa distancia recibirán tu solicitud.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Vinko.Muted,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }

                    VCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(Icons.Outlined.AddPhotoAlternate)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Fotos de referencia", style = MaterialTheme.typography.titleSmall)
                                Text("Opcional · hasta 6 fotos JPG, PNG o WEBP de 10 MB cada una.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                            }
                        }
                        if (photos.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            PhotoGrid(photos.size) { i ->
                                Box(Modifier.fillMaxSize()) {
                                    LocalPhoto(photos[i], Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                                    if (!saving) {
                                        Box(
                                            Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.55f))
                                                .clickable {
                                                    photos.removeAt(i)
                                                    photoError = null
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) { Icon(Icons.Outlined.Close, contentDescription = "Quitar foto ${i + 1}", tint = Color.White, modifier = Modifier.size(16.dp)) }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        SecondaryButton(
                            if (photos.isEmpty()) "Seleccionar fotos" else "Añadir fotos (${photos.size}/$MAX_PHOTOS)",
                            onClick = pickPhotos,
                            enabled = photos.size < MAX_PHOTOS && !saving,
                        )
                        Text(
                            "Muestra la falla o el trabajo que necesitas. Evita rostros, documentos y datos de contacto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Vinko.Muted,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        photoError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Coral, modifier = Modifier.padding(top = 4.dp)) }
                    }

                    if (!direct && mode == "onsite") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .border(1.dp, if (protect) Vinko.Primary else Vinko.Line, RoundedCornerShape(18.dp))
                                .clickable(enabled = !saving) { protect = !protect }
                                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = protect, onCheckedChange = { protect = it }, enabled = !saving, colors = CheckboxDefaults.colors(checkedColor = Vinko.Primary))
                            Column(Modifier.weight(1f)) {
                                Text("Quiero trabajar con pago protegido", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "El importe acordado se reserva en SoyVinko y se libera cuando confirmas el trabajo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Vinko.Muted,
                                )
                            }
                        }
                    }

                    Column(
                        Modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(18.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Shield, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Tú eliges a quién contratar", style = MaterialTheme.typography.titleSmall, color = Vinko.Primary)
                        }
                        Text(
                            if (mode == "onsite") "Tu dirección exacta solo se comparte cuando lo autorizas dentro del contrato."
                            else "Este servicio es remoto: no necesitas dirección ni radio de búsqueda.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            if (direct) "El profesional recibe tu solicitud y te envía una cotización en Bs o USD, según tu elección."
                            else "Varios profesionales pueden enviarte propuestas para esta necesidad.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Cuando eliges una propuesta, la solicitud queda asignada y deja de recibir ofertas.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (mode == "remote") {
                            Text("El pago remoto queda protegido hasta que revises y liberes la entrega.", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    PrimaryButton(
                        if (direct) "Enviar solicitud" else "Publicar solicitud",
                        onClick = ::submit,
                        loading = saving,
                        enabled = !(mode == "onsite" && account.data != null && addresses.isEmpty()),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun statusLabelMode(mode: String) = if (mode == "remote") "Remoto" else "Presencial"

@Composable
private fun ModeCard(icon: ImageVector, title: String, subtitle: String, selected: Boolean, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Vinko.Tint else Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) Vinko.Primary else Vinko.Line, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
    ) {
        IconTile(icon, bg = if (selected) Color.White else Vinko.Tint)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = if (selected) Vinko.Primary else Vinko.Ink)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
    }
}

/** Opción seleccionable con radio (direcciones, servicios). */
@Composable
fun OptionRow(selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null, content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) Vinko.Primary else Vinko.Line, RoundedCornerShape(14.dp))
            .background(if (selected) Vinko.Tint else Color.White)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) Vinko.Primary else Vinko.Lavender, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) { content() }
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Vinko.Primary))
    }
}

/** Cuadrícula de 3 columnas para fotos (sin listas perezosas anidadas). */
@Composable
fun PhotoGrid(count: Int, cell: @Composable (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (0 until count).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { i -> Box(Modifier.weight(1f).aspectRatio(1f)) { cell(i) } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
