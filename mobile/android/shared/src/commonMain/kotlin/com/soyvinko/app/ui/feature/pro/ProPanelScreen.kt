package com.soyvinko.app.ui.feature.pro

import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.common.rememberPhotoPicker
import com.soyvinko.app.ui.common.uploadFile
import com.soyvinko.app.ui.common.oneDecimal
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.ui.common.AppMode
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalAppMode
import com.soyvinko.app.ui.common.LocalOpenDrawer
import com.soyvinko.app.ui.common.LocalSession
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.d
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.relative
import com.soyvinko.app.ui.common.rememberAction
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.feature.contracts.OPEN_STATUSES
import com.soyvinko.app.ui.screens.HeroGradient
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject

/** Ruta PRO: activa el perfil profesional o muestra el panel. */
@Composable
fun ProScreen(nav: NavHostController) {
    val user = LocalUser.current
    var activated by rememberSaveable { mutableStateOf(false) }
    var forceActivation by rememberSaveable { mutableStateOf(false) }
    if ((user.isProfessional || activated) && !forceActivation) {
        ProPanelScreen(nav, onNeedsActivation = { forceActivation = true })
    } else {
        ProActivationScreen(onBack = { nav.popBackStack() }, onActivated = {
            activated = true
            forceActivation = false
        })
    }
}

// ---------------------------------------------------------------------------------------------
// Activación
// ---------------------------------------------------------------------------------------------

@Composable
fun ProActivationScreen(onBack: () -> Unit, onActivated: () -> Unit) {
    val app = LocalApp.current
    val user = LocalUser.current
    val session = LocalSession.current
    val form = rememberFormAction()
    var name by rememberSaveable { mutableStateOf(user.businessName ?: user.name) }
    var slug by rememberSaveable { mutableStateOf(slugify(user.businessName ?: user.name)) }
    var slugEdited by rememberSaveable { mutableStateOf(false) }
    var documentId by rememberSaveable { mutableStateOf("") }
    var rif by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var documentFile by remember { mutableStateOf<UploadPart?>(null) }
    var rifFile by remember { mutableStateOf<UploadPart?>(null) }
    var tried by rememberSaveable { mutableStateOf(false) }
    val pickDoc = rememberPhotoPicker { documentFile = it.firstOrNull() }
    val pickRif = rememberPhotoPicker { rifFile = it.firstOrNull() }
    val nameError = if (tried && name.isBlank()) "Escribe el nombre con el que te conocerán tus clientes." else null
    val slugError = if (tried && (!SLUG_PATTERN.matches(slug) || slug.length > 80)) "Usa letras minúsculas, números y guiones. Ejemplo: plomeria-caracas." else null
    val documentError = if (tried && documentId.isBlank()) "Escribe tu número de documento de identidad." else null
    val rifError = if (tried && rif.isBlank()) "Escribe tu número de RIF." else null

    ScreenScaffold("Registro de Proveedor", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().background(Vinko.Primary, RoundedCornerShape(24.dp)).padding(22.dp),
            ) {
                Row(
                    Modifier.background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Toll, contentDescription = null, tint = Color(0xFFFFD66B), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("3 Vinkos de bienvenida", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
                Spacer(Modifier.height(14.dp))
                Text("Registro de Proveedor", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Completa tus datos profesionales, documento de identidad y RIF para registrarte como proveedor en Vinko.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }

            VCard(padding = 6.dp) {
                Benefit(Icons.Outlined.RocketLaunch, "Publicación inmediata", "Tus servicios aparecen en el catálogo sin esperar aprobaciones.")
                Benefit(Icons.Outlined.Handshake, "Contratación directa", "El cliente y tú validan el contrato digital, sin intermediarios.")
                Benefit(Icons.Outlined.Payments, "Pagos claros", "Pago protegido en la plataforma o pago directo acordado.")
                Benefit(Icons.Outlined.Forum, "Canales desbloqueados", "WhatsApp y llamadas con el cliente al formalizar el contrato.", last = true)
            }

            Banner(
                "Enviar propuestas es gratis. Se descuenta 1 Vinko solo cuando un cliente te contrata y ambos aceptan el contrato digital.",
                BannerTone.Info,
            )

            Text("Datos del Proveedor", style = MaterialTheme.typography.titleLarge)
            form.error?.let { Banner(it) }
            VinkoTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (!slugEdited) slug = slugify(it).take(80)
                },
                label = "Nombre comercial o profesional",
                capitalization = KeyboardCapitalization.Words,
                error = nameError,
                supporting = "El nombre con el que te identificarán tus clientes.",
            )
            VinkoTextField(
                value = slug,
                onValueChange = {
                    slugEdited = true
                    slug = it.lowercase().replace(' ', '-').filter { c -> c.isLetterOrDigit() || c == '-' }.take(80)
                },
                label = "Enlace de tu perfil",
                error = slugError,
                supporting = "soyvinko.com/p/${slug.ifBlank { "tu-nombre" }}",
                imeAction = ImeAction.Next,
            )
            VinkoTextField(
                value = documentId,
                onValueChange = {
                    documentId = it.take(30)
                    tried = false
                },
                label = "Documento de Identidad (Cédula / Pasaporte)",
                error = documentError,
                supporting = "Ejemplo: V-12345678",
                imeAction = ImeAction.Next,
            )
            Text("Adjuntar documento de identidad (PDF o JPG)", style = MaterialTheme.typography.labelMedium, color = Vinko.Ink)
            PhotoSlot(
                documentFile,
                if (documentFile == null) "Subir documento de identidad (PDF o JPG)" else "Documento de identidad seleccionado",
                pickDoc,
                height = 110.dp,
            )
            VinkoTextField(
                value = rif,
                onValueChange = {
                    rif = it.take(30)
                    tried = false
                },
                label = "RIF (Registro de Información Fiscal)",
                error = rifError,
                supporting = "Ejemplo: J-12345678-9 o V-12345678-9",
                imeAction = ImeAction.Next,
            )
            Text("Adjuntar RIF (PDF o JPG)", style = MaterialTheme.typography.labelMedium, color = Vinko.Ink)
            PhotoSlot(
                rifFile,
                if (rifFile == null) "Subir RIF (PDF o JPG)" else "RIF seleccionado",
                pickRif,
                height = 110.dp,
            )
            AreaField(
                value = bio,
                onValueChange = { bio = it.take(4000) },
                label = "Descripción de tus servicios (opcional)",
                placeholder = "Tu especialidad, experiencia y lo que ofreces.",
            )
            PrimaryButton(
                text = "Registrarme como Proveedor",
                loading = form.busy,
                onClick = {
                    tried = true
                    val clean = slugify(slug)
                    if (name.isBlank() || documentId.isBlank() || rif.isBlank() || !SLUG_PATTERN.matches(clean) || clean.length > 80) return@PrimaryButton
                    slug = clean
                    form.run(
                        success = "¡Registro de proveedor completado! Recibiste 3 Vinkos de bienvenida.",
                        onSuccess = {
                            session.reloadUser()
                            onActivated()
                        },
                    ) {
                        val docFile = documentFile?.let { app.uploadFile(it, "evidence") }
                        val rifFileId = rifFile?.let { app.uploadFile(it, "evidence") }

                        app.ops.call(
                            "activate_professional",
                            "slug" to clean,
                            "business_name" to name.trim(),
                            "bio" to bio.trim(),
                        )
                        try {
                            app.ops.call("save_contact", "kind" to "document", "label" to "Cédula / Documento", "value" to documentId.trim(), "share" to false)
                            app.ops.call("save_contact", "kind" to "rif", "label" to "RIF", "value" to rif.trim(), "share" to false)
                        } catch (_: Exception) {}
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, text: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
    }
    if (!last) LineDivider(Modifier.padding(horizontal = 10.dp))
}

// ---------------------------------------------------------------------------------------------
// Panel profesional
// ---------------------------------------------------------------------------------------------

@Composable
fun ProPanelScreen(nav: NavHostController, onNeedsActivation: () -> Unit = {}) {
    val app = LocalApp.current
    val user = LocalUser.current
    val mode = LocalAppMode.current
    val openDrawer = LocalOpenDrawer.current
    val signal = ProSignals.setup
    val pro = rememberResource(signal) { app.ops.call("my_professional") }
    val profile = rememberResource(signal) { app.ops.call("professional_profile", "professional" to user.id) }
    val wallet = rememberResource { app.ops.call("my_wallet") }
    val moneyWalletUsd = rememberResource(signal) { app.ops.call("money_wallet", "currency" to "USD").obj() ?: JsonObject(emptyMap()) }
    val moneyWalletVes = rememberResource(signal) { app.ops.call("money_wallet", "currency" to "VES").obj() ?: JsonObject(emptyMap()) }
    val myContracts = rememberResource(signal) { app.ops.call("my_contracts").objects() }
    val reviews = rememberResource { app.ops.call("professional_reviews", "professional" to user.id, "limit" to 30) }
    val action = rememberAction()
    val form = rememberFormAction()
    var replyTo by remember { mutableStateOf<JsonObject?>(null) }

    ScreenScaffold(
        "Panel profesional y servicios",
        onBack = if (mode == AppMode.SELLER) null else { { nav.popBackStack() } },
        navigationIcon = if (mode == AppMode.SELLER) {
            {
                IconButton(onClick = openDrawer) {
                    Icon(Icons.Outlined.Menu, contentDescription = "Menú", tint = Vinko.Ink)
                }
            }
        } else null,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            ResourceView(pro) { data ->
                val p = data.obj()
                if (p == null) {
                    EmptyBox(
                        "Activa tu perfil profesional",
                        "Crea tu perfil para publicar servicios y recibir 3 Vinkos de bienvenida.",
                        "Activar perfil",
                        onNeedsActivation,
                    )
                    return@ResourceView
                }
                val publicProfile = profile.data.obj()
                val verified = publicProfile.b("verified")
                val rating = publicProfile.d("rating")
                val reviewCount = publicProfile.l("review_count") ?: 0
                val offerings = p.list("offerings")
                val active = offerings.count { it.b("active") }
                val accepts = p.b("accepts_requests")
                val usdAvailable = moneyWalletUsd.data.l("available_minor") ?: 0L
                val usdCustody = moneyWalletUsd.data.l("reserved_for_services_minor") ?: 0L
                val vesAvailable = moneyWalletVes.data.l("available_minor") ?: 0L
                val vesCustody = moneyWalletVes.data.l("reserved_for_services_minor") ?: 0L

                val contractsList = myContracts.data.orEmpty()
                val activeContracts = contractsList.filter { it.s("status") in OPEN_STATUSES }
                val completedContracts = contractsList.count { it.s("status") == "completed" }

                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Encabezado
                    Column(Modifier.fillMaxWidth().background(HeroGradient, RoundedCornerShape(24.dp)).padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Monogram(p.s("business_name") ?: user.name, size = 56.dp, bg = Color.White.copy(alpha = 0.16f), fg = Color.White)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    p.s("business_name") ?: user.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text("soyvinko.com/p/${p.s("slug").orEmpty()}", style = MaterialTheme.typography.bodySmall, color = Vinko.Lilac, maxLines = 1)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (verified) Pill("Identidad verificada", Vinko.Mint, Vinko.MintBg, Icons.Outlined.Verified)
                            else Pill("Sin verificar", Vinko.Amber, Vinko.AmberBg, Icons.Outlined.VerifiedUser)
                            Pill(
                                if (rating != null && reviewCount > 0) "★ ${oneDecimal(rating)} · $reviewCount ${if (reviewCount == 1L) "reseña" else "reseñas"}" else "Sin reseñas aún",
                                Color.White,
                                Color.White.copy(alpha = 0.14f),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(10.dp).background(if (accepts) Color(0xFF34D399) else Color(0xFFFBBF24), CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (accepts) "Disponible para nuevos clientes" else "Disponibilidad en pausa",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                )
                                Text(
                                    if (accepts) "Recibes solicitudes y oportunidades." else "No recibirás nuevas solicitudes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.75f),
                                )
                            }
                            Switch(
                                checked = accepts,
                                enabled = !action.busy,
                                onCheckedChange = { on ->
                                    action.run(if (on) "Disponibilidad activada." else "Disponibilidad en pausa.") {
                                        app.ops.call(
                                            "update_professional",
                                            "name" to (p.s("business_name") ?: user.name),
                                            "bio" to (p.s("bio") ?: ""),
                                            "years" to (p.l("years_experience") ?: 0L),
                                            "accepts" to on,
                                        )
                                        pro.reload()
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF34D399), checkedThumbColor = Color.White, uncheckedTrackColor = Color.White.copy(alpha = 0.2f)),
                            )
                        }
                    }

                    // Panel de Finanzas Profesionales y Escrow
                    VCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconTile(Icons.Outlined.AccountBalanceWallet, Vinko.Mint, Vinko.MintBg)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Finanzas Profesionales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Ganancias y saldo en garantía", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                                }
                            }
                            SmallButton(
                                text = "Retirar",
                                onClick = { nav.navigate(Routes.WITHDRAW) },
                                icon = Icons.Outlined.AccountBalance,
                                tone = Tone.Soft,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                            ) {
                                Text("Disponible USD", style = MaterialTheme.typography.labelSmall, color = Vinko.Muted)
                                Spacer(Modifier.height(4.dp))
                                Text(money(usdAvailable, "USD"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Vinko.Ink)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                            ) {
                                Text("Disponible VES", style = MaterialTheme.typography.labelSmall, color = Vinko.Muted)
                                Spacer(Modifier.height(4.dp))
                                Text(money(vesAvailable, "VES"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Vinko.Ink)
                            }
                        }
                        if (usdCustody > 0 || vesCustody > 0) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Vinko.MintBg, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.Shield, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                val custodyText = buildString {
                                    append("En garantía Escrow: ")
                                    if (usdCustody > 0) append(money(usdCustody, "USD"))
                                    if (usdCustody > 0 && vesCustody > 0) append(" · ")
                                    if (vesCustody > 0) append(money(vesCustody, "VES"))
                                }
                                Text(
                                    custodyText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Vinko.Mint,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { nav.navigate(Routes.WALLET) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Gestionar billetera y métodos de pago", style = MaterialTheme.typography.labelMedium, color = Vinko.Primary, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Alerta de Contratos Activos en curso
                    if (activeContracts.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.CONTRACTS) },
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(Color(0xFFDCFCE7), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Outlined.Handshake, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "⚡ ${activeContracts.size} ${if (activeContracts.size == 1) "servicio en curso" else "servicios en curso"}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF14532D),
                                    )
                                    Text(
                                        "Revisa avances, ubicación y chat con clientes para liberar los pagos garantizados.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF166534),
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Indicadores y Métricas Operacionales
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Stat(
                                Modifier.weight(1f),
                                Icons.Outlined.Toll,
                                "Vinkos",
                                wallet.data.obj().l("balance")?.toString() ?: "—",
                                "1 por contrato",
                                Vinko.Amber,
                                Vinko.AmberBg,
                                onClick = { nav.navigate(Routes.WALLET) },
                            )
                            Stat(
                                Modifier.weight(1f),
                                Icons.Outlined.Handshake,
                                "En curso",
                                "${activeContracts.size}",
                                if (completedContracts > 0) "$completedContracts completados" else "Servicios activos",
                                Color(0xFF10B981),
                                Color(0xFFECFDF5),
                                onClick = { nav.navigate(Routes.CONTRACTS) },
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Stat(
                                Modifier.weight(1f),
                                Icons.Outlined.WorkOutline,
                                "Servicios",
                                "$active",
                                "${offerings.size} en total",
                                Vinko.Primary,
                                Vinko.Tint,
                                onClick = { nav.navigate(Routes.PRO_SETUP) },
                            )
                            Stat(
                                Modifier.weight(1f),
                                Icons.Outlined.RateReview,
                                "Valoración",
                                rating?.let { oneDecimal(it) } ?: "—",
                                "$reviewCount reseñas",
                                Vinko.Mint,
                                Vinko.MintBg,
                            )
                        }
                    }

                    // Acciones principales para Freelancers: Solicitudes de clientes y Configurar servicios
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Line),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { nav.navigate(Routes.OPPORTUNITIES) },
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(Vinko.Tint, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Outlined.Search, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("Solicitudes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Ver y cotizar solicitudes de clientes", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 2)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Line),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { nav.navigate(Routes.PRO_SETUP) },
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(Vinko.MintBg, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Outlined.Tune, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("Mis Servicios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Tarifas, sedes y cobertura", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 2)
                            }
                        }
                    }

                    if (!verified && publicProfile != null) {
                        VCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconTile(Icons.Outlined.Badge, Vinko.Amber, Vinko.AmberBg)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Verifica tu identidad", style = MaterialTheme.typography.titleSmall)
                                    Text("Los clientes eligen más a los perfiles verificados.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                                }
                                SmallButton("Verificar", onClick = { nav.navigate(Routes.KYC) }, tone = Tone.Soft)
                            }
                        }
                    }
                    if (active == 0) {
                        VCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconTile(Icons.Outlined.Bolt)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Publica tu primer servicio", style = MaterialTheme.typography.titleSmall)
                                    Text("Necesitas un servicio activo y cobertura para recibir oportunidades.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                                }
                                SmallButton("Configurar", onClick = { nav.navigate(Routes.PRO_SETUP) }, tone = Tone.Soft)
                            }
                        }
                    }

                    Text("Gestiona tu actividad", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
                    VCard(padding = 6.dp) {
                        NavRow(Icons.Outlined.Search, "Oportunidades", "Solicitudes de clientes en tu zona y remotas") { nav.navigate(Routes.OPPORTUNITIES) }
                        NavRow(Icons.Outlined.Description, "Mis propuestas", "Estado de tus cotizaciones enviadas") { nav.navigate(Routes.MY_PROPOSALS) }
                        NavRow(Icons.Outlined.Tune, "Configurar perfil", "Servicios, sedes, cobertura, portafolio y contactos") { nav.navigate(Routes.PRO_SETUP) }
                        NavRow(Icons.Outlined.Storefront, "Mi tienda", "Vende productos en el Marketplace") { nav.navigate(Routes.MY_STORE) }
                        NavRow(Icons.Outlined.Toll, "Vinkos y planes", "Saldo, compras y movimientos") { nav.navigate(Routes.WALLET) }
                        NavRow(Icons.Outlined.Person, "Ver mi perfil público", "Así te ven los clientes", last = true) { nav.navigate(Routes.professional(user.id)) }
                    }

                    VCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(Icons.Outlined.Toll, Vinko.Amber, Vinko.AmberBg)
                            Spacer(Modifier.width(12.dp))
                            Text("¿Cómo funcionan los Vinkos?", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Enviar propuestas y cotizaciones es gratis. Cuando un cliente te contrata y ambos aceptan el contrato digital, se descuenta 1 Vinko de tu saldo y se desbloquean el chat y los datos de contacto.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Vinko.Muted,
                        )
                    }

                    // Reseñas
                    SectionHeader("Reseñas de clientes", if (reviewCount > 0) "$reviewCount publicadas" else null)
                    val list = reviews.data.objects()
                    when {
                        reviews.data == null && reviews.loading -> LoadingBox()
                        reviews.data == null -> Banner(reviews.error ?: "No pudimos cargar tus reseñas.")
                        list.isEmpty() -> VCard {
                            Text("Aún no tienes reseñas", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Cuando finalices un servicio, el cliente podrá calificarte. Las reseñas aparecen en tu perfil público.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Vinko.Muted,
                            )
                        }
                        else -> list.forEach { r -> ReviewCard(r) { form.reset(); replyTo = r } }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    replyTo?.let { review ->
        var text by remember(review) { mutableStateOf("") }
        FormSheet(
            title = "Responder reseña",
            subtitle = "Tu respuesta será pública en tu perfil. Agradece y aclara con respeto.",
            error = form.error,
            onDismiss = { replyTo = null },
        ) {
            review.s("comment")?.takeIf { it.isNotBlank() }?.let {
                Text("“$it”", style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
            }
            AreaField(text, { text = it.take(4000) }, "Tu respuesta")
            PrimaryButton("Publicar respuesta", loading = form.busy, enabled = text.isNotBlank(), onClick = {
                form.run("Respuesta publicada.", onSuccess = {
                    replyTo = null
                    reviews.reload()
                }) {
                    app.ops.call("respond_review", "review" to review.s("id"), "response" to text.trim())
                }
            })
        }
    }
}

@Composable
private fun Stat(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    hint: String,
    tint: Color,
    bg: Color,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
    ) {
        IconTile(icon, tint, bg, size = 36.dp)
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Vinko.Ink, maxLines = 1)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReviewCard(r: JsonObject, onReply: () -> Unit) {
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stars((r.l("rating") ?: 0L).toDouble())
            Spacer(Modifier.width(8.dp))
            if (r.b("recommends")) Pill("Te recomienda", Vinko.Mint, Vinko.MintBg, Icons.Outlined.ThumbUp)
            Spacer(Modifier.weight(1f))
            Text(relative(r.s("created_at")), style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle)
        }
        r.s("comment")?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(10.dp))
        val response = r.s("response")
        if (response.isNullOrBlank()) {
            SmallButton("Responder", onClick = onReply, icon = Icons.AutoMirrored.Outlined.Reply, tone = Tone.Soft)
        } else {
            Column(Modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(12.dp)).padding(12.dp)) {
                Text("Tu respuesta", style = MaterialTheme.typography.labelMedium, color = Vinko.Primary)
                Text(response, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
