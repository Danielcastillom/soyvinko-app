package com.soyvinko.app.ui.feature.profile

import com.soyvinko.app.ui.common.isValidEmail
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSession
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject

/** Datos de la cuenta (persona o empresa), contactos y preferencia de pago. */
@Composable
fun AccountScreen(onBack: () -> Unit) {
    val app = LocalApp.current
    val account = rememberResource { app.ops.call("my_account").requireObj() }
    val prefs = rememberResource { app.ops.call("payment_preferences").obj() }
    val kyc = rememberResource { app.ops.call("my_kyc").objects() }
    val rifLocked = kyc.data?.firstOrNull()?.let { kycStateOf(it) } == KycState.Approved

    ScreenScaffold("Cuenta", onBack = onBack) { padding ->
        ResourceView(account) { data ->
            ProfilePage(padding) {
                if (data.s("account_type") == "juridico") CompanyCard(data, rifLocked, account.reload)
                else PersonCard(data, rifLocked, account.reload)
                ContactsCard(data.list("contacts"), account.reload)
                PaymentCard(prefs.data, prefs.reload)
            }
        }
    }
}

@Composable
private fun PersonCard(data: JsonObject, rifLocked: Boolean, reload: () -> Unit) {
    val app = LocalApp.current
    val session = LocalSession.current
    val submit = rememberSubmitter()
    var name by rememberSaveable { mutableStateOf(data.s("name").orEmpty()) }
    val nameError = if (name.isNotEmpty() && name.trim().length < 2) "Escribe tu nombre completo." else null
    VCard {
        CardHeader(Icons.Outlined.Person, "Información personal", "Así te verán profesionales y clientes.")
        Spacer(Modifier.height(16.dp))
        VinkoTextField(
            value = name,
            onValueChange = { name = it.take(120); submit.clear() },
            label = "Nombre completo",
            capitalization = KeyboardCapitalization.Words,
            error = nameError ?: submit.fieldErrors["name"],
        )
        Spacer(Modifier.height(8.dp))
        ReadOnlyLine(Icons.Outlined.Email, "Correo", data.s("email").orEmpty())
        data.s("rif")?.let { doc ->
            ReadOnlyLine(Icons.Outlined.Badge, "Documento", doc + if (rifLocked) " · confirmado por tu verificación" else "")
        }
        submit.error?.let {
            Spacer(Modifier.height(10.dp))
            Banner(it)
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            "Guardar cambios",
            loading = submit.busy,
            enabled = name.trim().length >= 2 && name.trim() != data.s("name"),
            onClick = {
                submit.run("Perfil actualizado.") {
                    app.ops.call(
                        "update_account",
                        "name" to name.trim(),
                        "locale" to DEFAULT_LOCALE,
                        "timezone" to DEFAULT_TIMEZONE,
                    )
                    reload()
                    session.reloadUser()
                }
            },
        )
    }
}

@Composable
private fun CompanyCard(data: JsonObject, rifLocked: Boolean, reload: () -> Unit) {
    val app = LocalApp.current
    val session = LocalSession.current
    val submit = rememberSubmitter()
    var business by rememberSaveable { mutableStateOf(data.s("business_name") ?: data.s("name").orEmpty()) }
    var rif by rememberSaveable { mutableStateOf(data.s("rif").orEmpty()) }
    var contact by rememberSaveable { mutableStateOf(data.s("contact_name").orEmpty()) }
    val businessError = if (business.isNotEmpty() && business.trim().length < 2) "Escribe la razón social." else null
    val rifError = if (rif.isNotEmpty() && !rif.trim().matches(RIF_PATTERN)) "Usa el formato J-12345678-9." else null
    val contactError = if (contact.isNotEmpty() && contact.trim().length < 2) "Escribe el nombre de la persona de contacto." else null
    val changed = business.trim() != (data.s("business_name") ?: data.s("name")).orEmpty() ||
        rif.trim().uppercase() != data.s("rif").orEmpty() || contact.trim() != data.s("contact_name").orEmpty()
    val valid = changed && business.trim().length >= 2 && rif.trim().matches(RIF_PATTERN) && contact.trim().length >= 2
    VCard {
        CardHeader(Icons.Outlined.Business, "Información de la empresa", "Datos legales de la empresa en Venezuela.")
        Spacer(Modifier.height(16.dp))
        VinkoTextField(
            business, { business = it.take(120); submit.clear() }, "Razón social",
            capitalization = KeyboardCapitalization.Words,
            error = businessError ?: submit.fieldErrors["business_name"],
            supporting = "Nombre legal de la empresa en Venezuela",
        )
        Spacer(Modifier.height(6.dp))
        VinkoTextField(
            rif, { rif = it.uppercase().take(20); submit.clear() }, "RIF de la empresa",
            capitalization = KeyboardCapitalization.Characters,
            enabled = !rifLocked,
            error = rifError ?: submit.fieldErrors["rif"],
            supporting = if (rifLocked) "Confirmado por tu verificación de identidad. Para cambiarlo, envía una nueva verificación." else "Ejemplo: J-12345678-9",
        )
        Spacer(Modifier.height(6.dp))
        VinkoTextField(
            contact, { contact = it.take(120); submit.clear() }, "Persona de contacto / Representante",
            capitalization = KeyboardCapitalization.Words,
            error = contactError ?: submit.fieldErrors["contact_name"],
        )
        ReadOnlyLine(Icons.Outlined.Email, "Correo corporativo", data.s("email").orEmpty())
        submit.error?.let {
            Spacer(Modifier.height(10.dp))
            Banner(it)
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            "Guardar datos de empresa",
            loading = submit.busy,
            enabled = valid,
            onClick = {
                submit.run("Datos de la empresa actualizados.") {
                    app.ops.call(
                        "update_account",
                        "name" to business.trim(),
                        "business_name" to business.trim(),
                        // Con la verificación aprobada el RIF no se envía: el servidor lo bloquea.
                        "rif" to if (rifLocked) null else rif.trim().uppercase(),
                        "contact_name" to contact.trim(),
                        "locale" to DEFAULT_LOCALE,
                        "timezone" to DEFAULT_TIMEZONE,
                    )
                    reload()
                    session.reloadUser()
                }
            },
        )
    }
}

@Composable
private fun ReadOnlyLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Vinko.Lavender, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
        Icon(Icons.Outlined.Lock, contentDescription = "No editable", tint = Vinko.Subtle, modifier = Modifier.size(14.dp))
    }
}

private val CONTACT_KINDS = listOf("phone" to "Teléfono", "whatsapp" to "WhatsApp", "email" to "Correo")

@Composable
private fun ContactsCard(contacts: List<JsonObject>, reload: () -> Unit) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    var adding by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<JsonObject?>(null) }
    val remover = rememberSubmitter()
    VCard {
        CardHeader(Icons.Outlined.ContactPhone, "Mis contactos", "Se comparten solo con quien contrates, si lo permites.")
        Spacer(Modifier.height(10.dp))
        if (contacts.isEmpty()) {
            Hint("Añade teléfonos, correos o WhatsApp para coordinar tus servicios.", Modifier.padding(vertical = 6.dp))
        }
        contacts.forEachIndexed { i, c ->
            if (i > 0) Divider()
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val icon = when (c.s("kind")) {
                    "email" -> Icons.Outlined.AlternateEmail
                    "whatsapp" -> Icons.AutoMirrored.Outlined.Chat
                    else -> Icons.Outlined.Phone
                }
                com.soyvinko.app.ui.common.IconTile(icon, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.s("label") ?: CONTACT_KINDS.toMap()[c.s("kind")].orEmpty(), style = MaterialTheme.typography.titleSmall)
                    Text(c.s("value").orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (c.b("share_on_contract")) "Compartido solo en contratos" else "Privado",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (c.b("share_on_contract")) Vinko.Mint else Vinko.Muted,
                    )
                }
                IconButton(onClick = { removing = c }, enabled = !remover.busy) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar contacto", tint = Vinko.Coral)
                }
            }
        }
        remover.error?.let { Banner(it) }
        Spacer(Modifier.height(8.dp))
        SecondaryButton("Añadir contacto", onClick = { adding = true })
    }

    if (adding) {
        ContactDialog(
            onDismiss = { adding = false },
            onSaved = {
                adding = false
                snackbar.notify("Contacto guardado.")
                reload()
            },
        )
    }
    removing?.let { c ->
        ConfirmDialog(
            title = "¿Eliminar contacto?",
            text = "Se quitará ${c.s("value").orEmpty()} de tu cuenta. Los contratos ya iniciados conservan los datos compartidos.",
            confirm = "Eliminar",
            destructive = true,
            onConfirm = {
                removing = null
                remover.run("Contacto eliminado.") {
                    app.ops.call("remove_contact", "contact" to c.s("id"))
                    reload()
                }
            },
            onDismiss = { removing = null },
        )
    }
}

@Composable
private fun ContactDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val app = LocalApp.current
    val submit = rememberSubmitter()
    var kind by rememberSaveable { mutableStateOf("whatsapp") }
    var label by rememberSaveable { mutableStateOf("") }
    var value by rememberSaveable { mutableStateOf("") }
    var share by rememberSaveable { mutableStateOf(true) }
    val valueError = when {
        value.isEmpty() -> null
        kind == "email" && !isValidEmail(value) -> "Escribe un correo válido."
        kind != "email" && value.count(Char::isDigit) < 7 -> "Escribe el número con código de área, p. ej. 0414 1234567."
        else -> null
    }
    val valid = label.isNotBlank() && value.trim().length >= 3 && valueError == null
    AlertDialog(
        onDismissRequest = { if (!submit.busy) onDismiss() },
        containerColor = Color.White,
        title = { Text("Añadir contacto", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChips(CONTACT_KINDS, kind, { kind = it; submit.clear() })
                VinkoTextField(label, { label = it.take(60) }, "Nombre del contacto", capitalization = KeyboardCapitalization.Sentences, supporting = "Ej. Personal, Oficina")
                VinkoTextField(
                    value, { value = it.take(254); submit.clear() },
                    if (kind == "email") "Correo electrónico" else "Número",
                    keyboardType = if (kind == "email") KeyboardType.Email else KeyboardType.Phone,
                    error = valueError ?: submit.fieldErrors["value"],
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Compartir después de contratar", style = MaterialTheme.typography.titleSmall)
                        Hint("Solo lo verá la otra parte de un contrato activo.")
                    }
                    Switch(share, { share = it }, colors = SwitchDefaults.colors(checkedTrackColor = Vinko.Primary))
                }
                submit.error?.let { Banner(it) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid && !submit.busy,
                onClick = {
                    submit.run {
                        app.ops.call("save_contact", "kind" to kind, "label" to label.trim(), "value" to value.trim(), "share" to share)
                        onSaved()
                    }
                },
            ) { Text(if (submit.busy) "Guardando…" else "Guardar", color = if (valid) Vinko.Primary else Vinko.Subtle) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !submit.busy) { Text("Cancelar", color = Vinko.Muted) } },
    )
}

@Composable
private fun PaymentCard(prefs: JsonObject?, reload: () -> Unit) {
    val app = LocalApp.current
    val submit = rememberSubmitter()
    var required by remember(prefs) { mutableStateOf(prefs.b("requires_platform_payment")) }
    LaunchedEffect(submit.error) { if (submit.error != null) required = prefs.b("requires_platform_payment") }
    VCard {
        CardHeader(Icons.Outlined.Shield, "Preferencias de pago", "Cómo quieres cobrar y pagar tus servicios.", Vinko.Mint, Vinko.MintBg)
        Spacer(Modifier.height(12.dp))
        Text(
            "Al exigir pago protegido, los servicios que contrates o prestes deben financiarse en SoyVinko.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quiero trabajar con pago protegido", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Switch(
                checked = required,
                enabled = prefs != null && !submit.busy,
                onCheckedChange = { value ->
                    required = value
                    submit.run("Preferencia guardada.") {
                        app.ops.call("set_payment_preference", "required" to value)
                        reload()
                    }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = Vinko.Mint),
            )
        }
        submit.error?.let { Banner(it) }
        Spacer(Modifier.height(10.dp))
        Banner("En remoto el pago siempre es protegido. El dinero se libera cuando el cliente valida la actividad.", BannerTone.Info)
    }
}

const val DEFAULT_LOCALE = "es-VE"
const val DEFAULT_TIMEZONE = "America/Caracas"
private val RIF_PATTERN = Regex("^[VEJPG]-?\\d{6,9}-?\\d?$")
