package com.soyvinko.app.ui.feature.profile

import com.soyvinko.app.ui.common.openExternalUrl
import coil3.compose.LocalPlatformContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSession
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PasswordChecklist
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Correo, contraseña, verificación en dos pasos y dispositivos conectados. */
@Composable
fun SecurityScreen(onBack: () -> Unit) {
    ScreenScaffold("Seguridad", onBack = onBack) { padding ->
        ProfilePage(padding) {
            EmailCard()
            SectionLabel("Acceso")
            PasswordCard()
            MfaCard()
            SectionLabel("Dispositivos conectados")
            SessionsCard()
        }
    }
}

@Composable
private fun EmailCard() {
    val app = LocalApp.current
    val user = LocalUser.current
    val submit = rememberSubmitter()
    var sent by remember { mutableStateOf(false) }
    VCard {
        if (user.emailVerified) {
            CardHeader(Icons.Outlined.MarkEmailRead, "Correo verificado", user.email, Vinko.Mint, Vinko.MintBg)
        } else {
            CardHeader(Icons.Outlined.MarkEmailUnread, "Confirma tu correo", user.email, Vinko.Amber, Vinko.AmberBg)
            Spacer(Modifier.height(10.dp))
            Text(
                "Te enviaremos un enlace para verificar tu cuenta. Confirmar tu correo protege tu cuenta y habilita los retiros.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            if (sent) Banner("Enlace enviado. Revisa tu bandeja de entrada y la carpeta de spam.", BannerTone.Success)
            submit.error?.let { Banner(it) }
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                if (sent) "Reenviar enlace" else "Enviar enlace de verificación",
                loading = submit.busy,
                onClick = {
                    submit.run {
                        app.api.raw("POST", "/api/v1/auth/verification-email")
                        sent = true
                    }
                },
            )
        }
    }
}

@Composable
private fun PasswordCard() {
    val app = LocalApp.current
    val session = LocalSession.current
    val submit = rememberSubmitter()
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }
    var changed by remember { mutableStateOf(false) }
    val mismatch = repeat.isNotEmpty() && repeat != next
    val valid = current.isNotEmpty() && next.length >= 12 && next == repeat && next != current
    VCard {
        CardHeader(Icons.Outlined.Key, "Cambiar contraseña", "Por seguridad se cerrarán todas tus sesiones.")
        Spacer(Modifier.height(14.dp))
        val currentError = submit.fieldErrors["current_password"] ?: submit.error?.takeIf { "actual" in it }
        VinkoTextField(current, { current = it; submit.clear() }, "Contraseña actual", leadingIcon = Icons.Outlined.Lock, password = true, error = currentError)
        Spacer(Modifier.height(4.dp))
        VinkoTextField(
            next, { next = it; submit.clear() }, "Nueva contraseña", leadingIcon = Icons.Outlined.Key, password = true,
            error = submit.fieldErrors["new_password"] ?: if (next.isNotEmpty() && next == current) "Usa una contraseña distinta a la actual." else null,
        )
        PasswordChecklist(next)
        Spacer(Modifier.height(8.dp))
        VinkoTextField(
            repeat, { repeat = it }, "Repite la nueva contraseña", leadingIcon = Icons.Outlined.Key, password = true,
            imeAction = ImeAction.Done,
            error = if (mismatch) "Las contraseñas no coinciden." else null,
        )
        submit.error?.takeIf { it != currentError }?.let {
            Spacer(Modifier.height(8.dp))
            Banner(it)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Actualizar contraseña", loading = submit.busy, enabled = valid, onClick = { confirming = true })
    }
    if (confirming) {
        ConfirmDialog(
            title = "¿Cambiar la contraseña?",
            text = "Cerraremos todas tus sesiones, también la de este teléfono. Después tendrás que iniciar sesión con tu nueva contraseña.",
            confirm = "Cambiar",
            onConfirm = {
                confirming = false
                submit.run {
                    app.api.raw(
                        "POST",
                        "/api/v1/auth/change-password",
                        buildJsonObject {
                            put("current_password", current)
                            put("new_password", next)
                        },
                    )
                    changed = true
                }
            },
            onDismiss = { confirming = false },
        )
    }
    if (changed) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color.White,
            icon = { Icon(Icons.Outlined.Security, contentDescription = null, tint = Vinko.Mint) },
            title = { Text("Contraseña actualizada", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "Cerramos todas tus sesiones para proteger tu cuenta. Inicia sesión de nuevo con tu nueva contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Vinko.Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    changed = false
                    session.logout()
                }) { Text("Iniciar sesión", color = Vinko.Primary, style = MaterialTheme.typography.labelLarge) }
            },
        )
    }
}

@Composable
private fun MfaCard() {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val context = LocalPlatformContext.current
    val clipboard = LocalClipboardManager.current
    val status = rememberResource { app.api.raw("GET", "/api/v1/auth/mfa").obj() }
    val enabled = status.data.b("enabled")
    var dialog by remember { mutableStateOf<String?>(null) } // enroll | confirm | disable
    var enrollment by remember { mutableStateOf<JsonObject?>(null) }
    VCard {
        CardHeader(
            Icons.Outlined.Security,
            "Verificación en dos pasos",
            "Añade un código temporal de tu app de autenticación al iniciar sesión.",
            trailing = {
                if (status.data != null) {
                    Pill(if (enabled) "Activa" else "Inactiva", if (enabled) Vinko.Mint else Vinko.Muted, if (enabled) Vinko.MintBg else Vinko.Tint)
                }
            },
        )
        status.error?.let {
            Spacer(Modifier.height(10.dp))
            Banner(it)
        }
        val e = enrollment
        if (e != null && !enabled) {
            Spacer(Modifier.height(12.dp))
            Text("Agrega esta clave a tu app de autenticación (Google Authenticator, Authy, Microsoft Authenticator):", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(12.dp)).padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    e.s("secret").orEmpty().chunked(4).joinToString(" "),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleSmall,
                    color = Vinko.Primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(e.s("secret").orEmpty()))
                    snackbar.notify("Clave copiada.")
                }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar clave", tint = Vinko.Primary) }
            }
            e.s("uri")?.let { uri ->
                TextButton(onClick = {
                    if (!openExternalUrl(context, uri)) {
                        snackbar.notify("Instala una app de autenticación o copia la clave manualmente.")
                    }
                }) { Text("Abrir en mi app de autenticación", color = Vinko.Primary) }
            }
            Spacer(Modifier.height(6.dp))
            PrimaryButton("Ya la agregué: activar", onClick = { dialog = "confirm" })
        } else if (status.data != null) {
            Spacer(Modifier.height(12.dp))
            if (enabled) SecondaryButton("Desactivar verificación en dos pasos", onClick = { dialog = "disable" })
            else SecondaryButton("Configurar app de autenticación", onClick = { dialog = "enroll" })
        }
    }
    dialog?.let { kind ->
        MfaDialog(
            kind = kind,
            onDismiss = { dialog = null },
            onDone = { result ->
                dialog = null
                when (kind) {
                    "enroll" -> enrollment = result
                    "confirm" -> {
                        enrollment = null
                        snackbar.notify("Verificación en dos pasos activada.")
                        status.reload()
                    }
                    else -> {
                        snackbar.notify("Verificación en dos pasos desactivada.")
                        status.reload()
                    }
                }
            },
        )
    }
}

@Composable
private fun MfaDialog(kind: String, onDismiss: () -> Unit, onDone: (JsonObject?) -> Unit) {
    val app = LocalApp.current
    val submit = rememberSubmitter()
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    val needsCode = kind != "enroll"
    val valid = password.isNotEmpty() && (!needsCode || otp.length == 6)
    AlertDialog(
        onDismissRequest = { if (!submit.busy) onDismiss() },
        containerColor = Color.White,
        title = {
            Text(
                when (kind) {
                    "enroll" -> "Configurar autenticador"
                    "confirm" -> "Activar verificación en dos pasos"
                    else -> "Desactivar verificación en dos pasos"
                },
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (needsCode) "Confirma tu contraseña y escribe el código de 6 dígitos que muestra tu app." else "Confirma tu contraseña para generar tu clave secreta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Vinko.Muted,
                )
                VinkoTextField(password, { password = it; submit.clear() }, "Contraseña", password = true)
                if (needsCode) {
                    VinkoTextField(otp, { otp = it.filter(Char::isDigit).take(6); submit.clear() }, "Código de 6 dígitos", keyboardType = KeyboardType.NumberPassword)
                }
                submit.error?.let { Banner(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = valid && !submit.busy, onClick = {
                submit.run {
                    val body = buildJsonObject {
                        put("password", password)
                        if (needsCode) put("otp", otp)
                    }
                    val path = when (kind) {
                        "enroll" -> "/api/v1/auth/mfa/enroll"
                        "confirm" -> "/api/v1/auth/mfa/confirm"
                        else -> "/api/v1/auth/mfa/disable"
                    }
                    onDone(app.api.raw("POST", path, body).obj())
                }
            }) { Text(if (submit.busy) "Un momento…" else "Continuar", color = if (valid) Vinko.Primary else Vinko.Subtle) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !submit.busy) { Text("Cancelar", color = Vinko.Muted) } },
    )
}

/** Texto de User-Agent → nombre legible del dispositivo. */
fun friendlyDevice(label: String?): Pair<String, Boolean> {
    val ua = label.orEmpty()
    if (ua.isBlank()) return "Dispositivo" to false
    Regex("SoyVinko-Android/([\\w.\\-]+) \\(([^;]+); Android ([^)]+)\\)").find(ua)?.let { m ->
        return "App SoyVinko · ${m.groupValues[2].trim()} (Android ${m.groupValues[3].trim()})" to true
    }
    val mobile = listOf("Android", "iPhone", "iPad", "Mobile").any { it in ua }
    val browser = when {
        "Edg/" in ua -> "Edge"
        "OPR/" in ua || "Opera" in ua -> "Opera"
        "Firefox/" in ua -> "Firefox"
        "Chrome/" in ua -> "Chrome"
        "Safari/" in ua -> "Safari"
        else -> null
    }
    val os = when {
        "Windows" in ua -> "Windows"
        "iPhone" in ua -> "iPhone"
        "iPad" in ua -> "iPad"
        "Mac OS" in ua -> "Mac"
        "Android" in ua -> "Android"
        "Linux" in ua -> "Linux"
        else -> null
    }
    return when {
        browser != null && os != null -> "$browser en $os"
        browser != null -> browser
        else -> ua.take(60)
    } to mobile
}

@Composable
private fun SessionsCard() {
    val app = LocalApp.current
    val session = LocalSession.current
    val sessions = rememberResource { app.ops.call("my_sessions").objects() }
    val submit = rememberSubmitter()
    var revoking by remember { mutableStateOf<JsonObject?>(null) }
    // El servidor guarda como nombre del dispositivo el User-Agent de esta app (ver SoyVinkoApp).
    val thisDevice = app.info.userAgent
    val list = sessions.data
    val currentId = list?.firstOrNull { it.s("device_label") == thisDevice }?.s("id")
    VCard(padding = 6.dp) {
        when {
            list == null && sessions.error != null -> Banner(sessions.error.orEmpty(), modifier = Modifier.padding(10.dp))
            list == null -> com.soyvinko.app.ui.common.LoadingBox()
            list.isEmpty() -> Hint("No hay otras sesiones activas.", Modifier.padding(12.dp))
            else -> list.forEachIndexed { i, s ->
                if (i > 0) Divider()
                val (name, mobile) = friendlyDevice(s.s("device_label"))
                val current = s.s("id") == currentId
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    com.soyvinko.app.ui.common.IconTile(
                        if (mobile) Icons.Outlined.PhoneAndroid else Icons.Outlined.Laptop,
                        if (current) Vinko.Mint else Vinko.Primary,
                        if (current) Vinko.MintBg else Vinko.Tint,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                        Text(
                            (if (current) "Este dispositivo · " else "") + "Desde ${date(s.s("created_at"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (current) Vinko.Mint else Vinko.Muted,
                        )
                    }
                    TextButton(onClick = { revoking = s }, enabled = !submit.busy) { Text("Revocar", color = Vinko.Coral) }
                }
            }
        }
        submit.error?.let { Banner(it, modifier = Modifier.padding(10.dp)) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Icon(Icons.Outlined.Devices, contentDescription = null, tint = Vinko.Subtle, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Hint("Si no reconoces un dispositivo, revócalo y cambia tu contraseña.")
    }
    revoking?.let { s ->
        val current = s.s("id") == currentId
        ConfirmDialog(
            title = "¿Revocar esta sesión?",
            text = if (current) "Es la sesión de este teléfono: tendrás que iniciar sesión de nuevo."
            else "${friendlyDevice(s.s("device_label")).first} tendrá que iniciar sesión de nuevo.",
            confirm = "Revocar",
            destructive = true,
            onConfirm = {
                revoking = null
                submit.run("Sesión revocada.") {
                    app.ops.call("revoke_session", "session" to s.s("id"))
                    if (current) session.logout() else sessions.reload()
                }
            },
            onDismiss = { revoking = null },
        )
    }
}
