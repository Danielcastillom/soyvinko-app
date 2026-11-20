package com.soyvinko.app.ui.feature.contracts

import com.soyvinko.app.ui.common.encodeUrlComponent
import com.soyvinko.app.ui.common.openExternalUrl
import com.soyvinko.app.ui.common.dialPhone
import com.soyvinko.app.ui.common.minorToPlain
import com.soyvinko.app.ui.common.toDecimalOrNull
import com.soyvinko.app.ui.common.newUuid
import com.soyvinko.app.ui.common.PlatformBackHandler
import coil3.PlatformContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.ApiImage
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.statusLabel
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Reglas de negocio (espejo de las funciones SQL api.* y de frontend/src/pages/Contracts.tsx)
// ---------------------------------------------------------------------------------------------

/** Estados en los que el servicio sigue abierto (la web los llama "active"). */
val OPEN_STATUSES = setOf("accepted", "en_route", "arrived", "in_progress", "awaiting_confirmation")

/** Estados en los que el cliente puede compartir la ubicación (api.share_location / api.navigate). */
val SHARE_STATUSES = OPEN_STATUSES

/** Estados en los que el profesional puede enviar su posición (api.report_position). */
val REPORT_STATUSES = setOf("en_route", "arrived", "in_progress")

/** Estados en los que se pueden proponer/aceptar condiciones (api.propose_terms / accept_terms). */
val TERMS_STATUSES = setOf("accepted", "en_route", "arrived", "in_progress")

/** Estados en los que se puede cancelar (api.cancel_contract). Después hay que reportar un problema. */
val CANCEL_STATUSES = setOf("accepted", "en_route", "arrived")

data class Step(val status: String, val label: String, val atKey: String)

/** Línea de tiempo de estados según la modalidad (los remotos no pasan por "en camino" ni "en el lugar"). */
fun stepsFor(mode: String?): List<Step> = if (mode == "remote") listOf(
    Step("accepted", "Acuerdo aceptado", "accepted_at"),
    Step("in_progress", "Trabajo en curso", "started_at"),
    Step("awaiting_confirmation", "Entrega por confirmar", "professional_finished_at"),
    Step("completed", "Finalizado", "completed_at"),
) else listOf(
    Step("accepted", "Acuerdo aceptado", "accepted_at"),
    Step("en_route", "En camino", "en_route_at"),
    Step("arrived", "En el lugar", "arrived_at"),
    Step("in_progress", "Servicio en curso", "started_at"),
    Step("awaiting_confirmation", "Por confirmar", "professional_finished_at"),
    Step("completed", "Finalizado", "completed_at"),
)

/** Índice del paso alcanzado. En cancelados o disputas, el último paso con fecha registrada. */
fun reachedStep(steps: List<Step>, status: String?, at: (String) -> String?): Int {
    val i = steps.indexOfFirst { it.status == status }
    if (i >= 0) return i
    return steps.indexOfLast { at(it.atKey) != null }.coerceAtLeast(0)
}

fun modeLabel(mode: String?) = if (mode == "remote") "Remoto" else "Presencial"
fun protectionLabel(p: String?) = if (p == "guaranteed") "Pago protegido" else "Pago directo"

fun paymentMethodLabel(m: String?): String = when (m?.lowercase()) {
    null, "" -> "A convenir"
    "wallet", "platform_wallet" -> "Saldo SoyVinko (custodia)"
    else -> m
}

fun escrowLabel(status: String?): String = when (status) {
    "unfunded" -> "Pendiente de reserva"
    "funded" -> "Fondos en custodia"
    "disputed" -> "Retenidos por disputa"
    "released" -> "Pago liberado al profesional"
    "refunded" -> "Devuelto al cliente"
    else -> statusLabel(status)
}

fun eventLabel(type: String?, toStatus: String?): String = when (type) {
    "mutual_acceptance" -> "Acuerdo formalizado por ambas partes"
    "depart" -> "El profesional salió en camino"
    "arrive" -> "El profesional llegó al lugar"
    "start" -> "Servicio iniciado"
    "remote_start" -> "Proyecto iniciado"
    "finish" -> if (toStatus == "completed") "Finalización confirmada por ambas partes" else "Finalización confirmada por una parte"
    "funds_reserved" -> "Pago reservado en custodia"
    "delivery_submitted" -> "Entrega presentada"
    "delivery_changes_requested" -> "Correcciones solicitadas"
    "customer_released_payment" -> "Pago liberado al profesional"
    "cancel" -> "Servicio cancelado"
    "dispute" -> "Problema reportado a soporte"
    "escrow_refunded" -> "Fondos devueltos al cliente"
    null -> statusLabel(toStatus)
    else -> type.replace('_', ' ').replaceFirstChar(Char::uppercase)
}

/** "#VNK-1A2B3C4D", igual que el folio de la web. */
fun folio(id: String) = "#VNK-" + id.take(8).uppercase()

fun newKey(): String = newUuid()

/**
 * Convierte lo que escribe la persona ("1.250,50", "45.5", "300") a unidades menores.
 * Devuelve null si no es un importe positivo válido.
 */
fun parseAmountMinor(raw: String): Long? {
    var t = raw.trim().replace(" ", "").replace("Bs", "", ignoreCase = true).replace("$", "")
    if (t.isEmpty()) return null
    if (t.contains(',') && t.contains('.')) {
        t = if (t.lastIndexOf(',') > t.lastIndexOf('.')) t.replace(".", "").replace(',', '.') else t.replace(",", "")
    } else if (t.contains(',')) {
        t = t.replace(',', '.')
    } else if (t.count { it == '.' } > 1 || (t.contains('.') && t.substringAfterLast('.').length == 3)) {
        t = t.replace(".", "")
    }
    if (t.count { it == '.' } > 1) return null
    val value = t.toDecimalOrNull() ?: return null
    if (value.signum <= 0) return null
    return value.scaledHalfUp(2)
}

/** Importe en unidades menores para un campo editable: 450000 -> "4500,00". */
fun minorToInput(minor: Long?): String =
    minor?.let { minorToPlain(it) } ?: ""

// ---------------------------------------------------------------------------------------------
// Errores de la API con mensajes claros (el servidor devuelve un texto genérico para estos códigos)
// ---------------------------------------------------------------------------------------------

private val ERRORS = mapOf(
    "contract_access_denied" to "No tienes acceso a este servicio.",
    "contract_cancelled" to "Este servicio fue cancelado.",
    "location_not_shared" to "El cliente aún no ha compartido la ubicación del servicio, o el permiso venció o fue revocado.",
    "position_not_allowed" to "No podemos enviar tu ubicación: el servicio no está en camino o el cliente dejó de compartir la ubicación.",
    "location_share_not_allowed" to "Solo puedes compartir la ubicación mientras el servicio está activo, por 1 a 24 horas.",
    "only_customer_can_share" to "Solo el cliente puede compartir la ubicación del servicio.",
    "only_customer_can_revoke" to "Solo el cliente puede dejar de compartir la ubicación.",
    "invalid_transition" to "Esta acción no aplica al estado actual del servicio. Actualiza e inténtalo de nuevo.",
    "invalid_remote_transition" to "Esta acción no aplica al estado actual del proyecto. Actualiza e inténtalo de nuevo.",
    "only_professional_can_advance" to "Solo el profesional puede avanzar el estado del servicio.",
    "only_professional_can_start" to "Solo el profesional puede iniciar el proyecto.",
    "use_delivery_and_customer_release" to "Con pago protegido, el servicio finaliza cuando el cliente valida la entrega y libera el pago.",
    "reason_required" to "Describe el motivo con al menos 5 caracteres.",
    "open_dispute_instead" to "El servicio ya comenzó: en lugar de cancelarlo, reporta un problema.",
    "invalid_dispute" to "Describe el problema con al menos 10 caracteres. Si ya hay una disputa abierta, soporte la está atendiendo.",
    "terms_not_allowed" to "Las condiciones ya no se pueden modificar en este estado del servicio.",
    "terms_superseded" to "Hay una versión más reciente de las condiciones. Revisa la última propuesta.",
    "terms_not_found" to "Esa propuesta de condiciones ya no existe.",
    "only_customer_can_fund" to "Solo el cliente puede reservar el pago.",
    "escrow_already_funded" to "El pago de este servicio ya está reservado.",
    "funding_quote_stale" to "La cotización venció o las condiciones cambiaron. Vuelve a solicitar la reserva.",
    "funding_quote_not_owned" to "Esta cotización no te pertenece. Solicita una nueva.",
    "wallet_frozen" to "Tu wallet está congelada. Escríbenos a soporte para revisarla.",
    "professional_unavailable" to "El profesional no está disponible en este momento.",
    "only_professional_can_deliver" to "Solo el profesional puede presentar entregas.",
    "delivery_not_allowed" to "Solo puedes presentar entregas con el servicio en curso y el pago reservado.",
    "invalid_delivery_files" to "Adjunta como máximo 20 archivos distintos.",
    "delivery_idempotency_conflict" to "Esta entrega ya se envió con otro contenido. Actualiza la pantalla.",
    "unsafe_delivery_file" to "Alguna evidencia aún está en revisión de seguridad. Quítala o espera a que sea aprobada.",
    "only_customer_can_release" to "Solo el cliente puede liberar el pago.",
    "funds_not_releasable" to "El pago no se puede liberar ahora: debe haber una entrega pendiente de validar.",
    "delivery_not_found" to "No encontramos esa entrega.",
    "only_customer_can_review" to "Solo el cliente puede revisar las entregas.",
    "delivery_not_reviewable" to "Describe las correcciones con al menos 10 caracteres. Solo aplica a la última entrega pendiente.",
    "delivery_superseded" to "Hay una entrega más reciente. Revisa la última versión.",
    "only_customer_can_request_refund" to "Solo el cliente puede solicitar la devolución.",
    "escrow_refund_not_available" to "Describe el motivo con al menos 10 caracteres. La devolución solo aplica a fondos en custodia.",
    "only_professional_can_approve_refund" to "Solo el profesional puede aprobar la devolución.",
    "customer_refund_request_required" to "El cliente aún no ha solicitado la devolución.",
    "one_transaction_required" to "No pudimos asociar el pago a este servicio.",
    "invalid_payment_report" to "No se puede registrar el pago: revisa la moneda o el estado del servicio.",
    "counterparty_confirmation_required" to "La recepción del pago debe confirmarla la otra parte.",
    "contract_not_completed" to "Podrás calificar cuando el servicio esté finalizado.",
    "chat_closed" to "La conversación de este servicio está cerrada.",
    "chat_unavailable" to "La conversación de este servicio no está disponible.",
    "invalid_coordinates" to "La ubicación del GPS no es válida. Vuelve a intentarlo con buena señal.",
    "file_unavailable" to "Tu foto se subió y está en revisión de seguridad. Podrás enviarla cuando sea aprobada.",
    "idempotency_conflict" to "Este mensaje ya se envió con otro contenido.",
    "invalid_pagination" to "No pudimos cargar más mensajes.",
)

fun friendlyError(e: ApiException): String =
    ERRORS[e.code] ?: e.fieldErrors.values.firstOrNull()?.takeIf { e.code == "validation_error" } ?: e.message

/** Acción (POST) con progreso, mensaje de éxito y errores claros. Si hay `onError`, el error va ahí (p. ej. dentro de un diálogo). */
class OpRunner(
    val busy: Boolean,
    private val start: (String?, ((String) -> Unit)?, () -> Unit, suspend () -> Unit) -> Unit,
) {
    fun run(success: String? = null, onError: ((String) -> Unit)? = null, after: () -> Unit = {}, block: suspend () -> Unit) =
        start(success, onError, after, block)
}

@Composable
fun rememberOp(): OpRunner {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    return OpRunner(busy) { success, onError, after, block ->
        if (!busy) scope.launch(Dispatchers.Main.immediate) {
            busy = true
            try {
                block()
                after()
                success?.let { launch { snackbar.showSnackbar(it) } }
            } catch (e: ApiException) {
                val msg = friendlyError(e)
                if (onError != null) onError(msg) else launch { snackbar.showSnackbar(msg) }
            } finally {
                busy = false
            }
        }
    }
}
/** Repite `block` cada `intervalMs` solo mientras la pantalla está visible (se pausa en segundo plano). */
@Composable
fun PollWhileVisible(intervalMs: Long, vararg keys: Any?, block: suspend () -> Unit) {
    com.soyvinko.app.ui.common.PollWhileVisible(intervalMs, *keys, block = block)
}

// ---------------------------------------------------------------------------------------------
// Contacto directo: solo marcador telefónico, WhatsApp y correo (sin navegador)
// ---------------------------------------------------------------------------------------------

/** Número internacional para WhatsApp, igual que la web: 0412… -> 58412… */
fun whatsappNumber(raw: String?): String? {
    val digits = raw?.filter(Char::isDigit).orEmpty()
    if (digits.isEmpty()) return null
    return when {
        digits.startsWith("0") -> "58" + digits.drop(1)
        digits.startsWith("58") -> digits
        else -> "58$digits"
    }
}

fun dial(context: PlatformContext, phone: String): Boolean = dialPhone(context, phone)

/** Solo la app de WhatsApp (esquema whatsapp://): nunca el navegador. */
fun openWhatsApp(context: PlatformContext, phone: String, text: String): Boolean {
    val number = whatsappNumber(phone) ?: return false
    return openExternalUrl(context, "whatsapp://send?phone=$number&text=" + encodeUrlComponent(text))
}

fun sendEmail(context: PlatformContext, email: String, subject: String): Boolean =
    com.soyvinko.app.ui.common.sendEmail(context, email.trim(), subject)

// ---------------------------------------------------------------------------------------------
// Piezas visuales de la sección
// ---------------------------------------------------------------------------------------------

@Composable
fun Pill(text: String, fg: Color = Vinko.Primary, bg: Color = Vinko.Tint, icon: ImageVector? = null) {
    Row(
        Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

@Composable
fun SectionTitle(text: String, icon: ImageVector? = null, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun KeyValue(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted, modifier = Modifier.weight(1f))
        Text(value, style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, color = Vinko.Ink)
    }
}

@Composable
fun Divider() = Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(Vinko.Line))

/** Botón de contorno compacto con icono (acciones secundarias en tarjetas). */
@Composable
fun OutlineAction(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Vinko.Ink,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Vinko.Line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = tint),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun MultilineField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    error: String? = null,
    minLines: Int = 3,
    maxLength: Int = 4000,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = 8,
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = fieldColors(),
    )
}

@Composable
fun AmountField(value: String, onValueChange: (String) -> Unit, label: String, currency: String?, error: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { it.isDigit() || it == ',' || it == '.' }.take(15)) },
        label = { Text(label) },
        prefix = { Text(if (currency == "USD") "US$ " else "Bs ") },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = fieldColors(),
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Vinko.Primary,
    unfocusedBorderColor = Vinko.Line,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedLabelColor = Vinko.Primary,
)

/**
 * Formulario en hoja inferior dentro de la misma ventana (no un Dialog aparte): se adapta al teclado,
 * se cierra con Atrás o tocando fuera, y muestra el error en línea.
 */
@Composable
fun FormDialog(
    title: String,
    confirmLabel: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    error: String? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    PlatformBackHandler { if (!busy) onDismiss() }
    val noRipple = remember { MutableInteractionSource() }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = noRipple, indication = null) { if (!busy) onDismiss() }
            .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.92f)
                .background(Color.White, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .padding(top = 10.dp, bottom = 16.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp).background(Vinko.Line, RoundedCornerShape(2.dp)))
            Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { if (!busy) onDismiss() }) { Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = Vinko.Muted) }
            }
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
                if (error != null) com.soyvinko.app.ui.components.Banner(error)
            }
            Row(Modifier.padding(horizontal = 20.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !busy,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Vinko.Line),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Vinko.Ink),
                ) { Text("Volver", style = MaterialTheme.typography.labelLarge) }
                Button(
                    onClick = onConfirm,
                    enabled = !busy && confirmEnabled,
                    modifier = Modifier.weight(1.4f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (destructive) Vinko.Coral else Vinko.Primary,
                        disabledContainerColor = if (destructive) Vinko.CoralBg else Vinko.Lilac,
                        disabledContentColor = if (destructive) Vinko.Coral else Color.White,
                    ),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text(confirmLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

/** Visor de foto a pantalla completa (adjuntos del chat, evidencias, fotos de referencia). */
@Composable
fun PhotoViewer(fileId: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            ApiImage(fileId, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            IconButton(
                onClick = onClose,
                modifier = Modifier.statusBarsPadding().padding(12.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) { Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = Color.White) }
        }
    }
}

@Composable
fun Thumb(fileId: String, size: androidx.compose.ui.unit.Dp = 84.dp, onClick: () -> Unit) {
    ApiImage(fileId, Modifier.size(size).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick))
}
