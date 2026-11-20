package com.soyvinko.app.ui.feature.admin

import com.soyvinko.app.ui.common.oneDecimal
import com.soyvinko.app.ui.common.minorToPlain
import com.soyvinko.app.ui.common.toDecimalOrNull
import com.soyvinko.app.ui.common.Decimal
import com.soyvinko.app.ui.common.PlatformBackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.LocalShellScope
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Errores de la API con mensajes claros para el equipo (los códigos vienen de las funciones SQL api.*)
// ---------------------------------------------------------------------------------------------

private val ADMIN_ERRORS = mapOf(
    "invalid_user_action" to "No se pudo cambiar el estado: no puedes modificar tu propia cuenta y el motivo debe tener al menos 10 caracteres.",
    "invalid_role_action" to "No se pudo cambiar el rol: no puedes modificar tus propios roles y el motivo debe tener al menos 10 caracteres.",
    "user_not_found" to "No encontramos esa cuenta.",
    "kyc_review_not_allowed" to "No puedes resolver este expediente: no existe o corresponde a tu propia cuenta.",
    "invalid_kyc_decision" to "Revisa la decisión: el motivo necesita al menos 5 caracteres y una aprobación requiere una fecha de vencimiento futura.",
    "request_not_moderatable" to "Esta solicitud ya no se puede moderar: solo aplica a borradores o solicitudes publicadas.",
    "invalid_moderation" to "Elige publicar u ocultar e indica un motivo de al menos 5 caracteres.",
    "file_not_moderatable" to "Este archivo no se puede aprobar: fue eliminado o todavía conserva metadatos EXIF.",
    "file_unavailable" to "El archivo ya no está disponible.",
    "invalid_store_status" to "Estado de tienda no válido.",
    "invalid_plan_code" to "Ese plan no existe o está inactivo.",
    "invalid_support_assignee" to "La persona asignada debe ser personal de soporte activo.",
    "note_required" to "Escribe una nota interna de al menos 5 caracteres.",
    "ticket_not_found" to "No encontramos ese caso de soporte.",
    "refund_not_allowed" to "Solo se puede devolver el crédito de servicios cancelados o en disputa.",
    "related_ticket_and_reason_required" to "El caso debe estar vinculado al servicio y el motivo debe tener al menos 10 caracteres.",
    "debit_not_found" to "Este servicio no tiene un débito de crédito que devolver.",
    "dispute_ticket_and_reason_required" to "La devolución requiere un servicio en disputa o cancelado vinculado a este caso y un motivo de al menos 10 caracteres.",
    "invalid_rate" to "Indica una tasa mayor que cero.",
    "invalid_currency" to "Moneda no válida. Usa bolívares o dólares.",
    "reason_required" to "Describe el motivo con al menos 5 caracteres.",
    "access_reason_required" to "Describe el motivo de acceso con al menos 10 caracteres.",
    "unknown_resource" to "Esta sección no está disponible.",
    "invalid_pagination" to "Página no válida.",
    "already_exists" to "Ya existe un registro con ese código.",
    "payment_provider_unavailable" to "El modo de pagos de prueba no está activo en este entorno.",
    "invalid_message" to "Escribe un mensaje de 1 a 10.000 caracteres.",
)

fun adminError(e: ApiException): String = when {
    e.status == 403 -> "No tienes permiso para esta operación."
    else -> ADMIN_ERRORS[e.code] ?: e.fieldErrors.values.firstOrNull()?.takeIf { e.code == "validation_error" } ?: e.message
}

/** Acción (POST) con progreso; el error se muestra dentro de la hoja que la lanzó. */
class AdminOp(
    val busy: Boolean,
    private val start: (String?, (String) -> Unit, () -> Unit, suspend () -> Unit) -> Unit,
) {
    fun run(success: String?, onError: (String) -> Unit, after: () -> Unit = {}, block: suspend () -> Unit) =
        start(success, onError, after, block)
}

@Composable
fun rememberAdminOp(): AdminOp {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val messages = LocalShellScope.current ?: scope
    var busy by remember { mutableStateOf(false) }
    return AdminOp(busy) { success, onError, after, block ->
        if (!busy) scope.launch(Dispatchers.Main.immediate) {
            busy = true
            try {
                block()
                success?.let { messages.launch { snackbar.showSnackbar(it) } }
                after()
            } catch (e: ApiException) {
                onError(adminError(e))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("No se pudo completar la operación. Inténtalo de nuevo.")
            } finally {
                busy = false
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Importes y porcentajes escritos por el equipo
// ---------------------------------------------------------------------------------------------

/** "1.250,50", "45.5" o "300" a decimal (admite cero). Null si no es un número válido. */
fun parseDecimal(raw: String): Decimal? {
    var t = raw.trim().replace(" ", "").replace("Bs", "", ignoreCase = true).replace("US$", "").replace("$", "").replace("%", "")
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
    return if (value.signum < 0) null else value
}

/** Importe en unidades mayores a menores: 12,5 -> 1250. */
fun toMinor(value: Decimal): Long = value.scaledHalfUp(2) ?: 0L

/** Porcentaje a puntos básicos: 2,5 % -> 250. */
fun toBasisPoints(percent: Decimal): Int = (percent.scaledHalfUp(2) ?: 0L).toInt()

/** Unidades menores a texto editable: 150000 -> "1500,00". */
fun minorToInput(minor: Long?): String =
    minor?.let { minorToPlain(it) } ?: ""

/** Puntos básicos a texto: 250 -> "2,5 %". */
fun percentText(basisPoints: Long?): String {
    if (basisPoints == null) return "—"
    return minorToPlain(basisPoints).trimEnd('0').trimEnd(',') + " %"
}

/** Tamaño legible: 2048 -> "2 KB". */
fun bytesText(bytes: Long?): String = when {
    bytes == null -> "—"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${(bytes + 512) / 1024} KB"
    else -> oneDecimal(bytes / 1048576.0) + " MB"
}

fun shortId(id: String?): String = if (id.isNullOrBlank()) "—" else "#" + id.take(8)

// ---------------------------------------------------------------------------------------------
// Hoja inferior en la misma ventana (formularios y confirmaciones)
// ---------------------------------------------------------------------------------------------

/**
 * Formulario en hoja inferior dentro de la ventana de la pantalla (no un Dialog aparte): se adapta al
 * teclado, se cierra con Atrás o tocando fuera y muestra el error en línea. Sin `confirmLabel` solo
 * muestra "Cerrar" (vistas de detalle).
 */
@Composable
fun AdminSheet(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
    onConfirm: () -> Unit = {},
    busy: Boolean = false,
    subtitle: String? = null,
    error: String? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
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
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
                content()
                if (error != null) Banner(error)
            }
            Row(Modifier.padding(horizontal = 20.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !busy,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Vinko.Line),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Vinko.Ink),
                ) { Text(if (confirmLabel == null) "Cerrar" else "Cancelar", style = MaterialTheme.typography.labelLarge) }
                if (confirmLabel != null) Button(
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
                    else Text(confirmLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Campos de formulario
// ---------------------------------------------------------------------------------------------

@Composable
fun adminFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Vinko.Primary,
    unfocusedBorderColor = Vinko.Line,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedLabelColor = Vinko.Primary,
)

/** Motivo obligatorio con contador y mínimo de caracteres (lo exige la función SQL). */
@Composable
fun ReasonField(value: String, onChange: (String) -> Unit, min: Int, label: String = "Motivo", hint: String? = null) {
    val n = value.trim().length
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(1000)) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 6,
        isError = n in 1 until min,
        supportingText = { Text((hint?.let { "$it · " } ?: "") + "Mínimo $min caracteres · $n") },
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = adminFieldColors(),
    )
}

@Composable
fun AdminTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    suffix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = if (singleLine) 1 else 8,
        isError = error != null,
        prefix = prefix?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
        colors = adminFieldColors(),
    )
}

@Composable
fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = Vinko.Ink)
}

/** Opciones en píldoras que saltan de línea (selección única). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceGroup(options: List<Pair<String, String>>, selected: String?, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) -> ChoiceChip(label, value == selected) { onSelect(value) } }
    }
}

@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Color.White else Vinko.Ink,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Vinko.Primary else Color.White)
            .border(1.dp, if (selected) Vinko.Primary else Vinko.Line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** Lista de opciones con título y descripción (planes, acciones con explicación). */
@Composable
fun RadioList(options: List<Triple<String, String, String?>>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, title, subtitle) ->
            val on = value == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) Vinko.Tint else Color.White)
                    .border(if (on) 1.5.dp else 1.dp, if (on) Vinko.Primary else Vinko.Line, RoundedCornerShape(14.dp))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (on) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (on) Vinko.Primary else Vinko.Lilac,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = if (on) Vinko.Primary else Vinko.Ink)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
            }
        }
    }
}

/** Control segmentado de marca (2–3 opciones). */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(14.dp)).padding(4.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) Color.White else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) Vinko.Primary else Vinko.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(14.dp))
            .clickable { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted) }
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Vinko.Primary, uncheckedBorderColor = Vinko.Line, uncheckedTrackColor = Vinko.Tint),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Piezas visuales
// ---------------------------------------------------------------------------------------------

@Composable
fun Tag(text: String, fg: Color = Vinko.Primary, bg: Color = Vinko.Tint, icon: ImageVector? = null) {
    Row(
        Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Fila etiqueta / valor alineada a los extremos. */
@Composable
fun Field(label: String, value: String, valueColor: Color = Vinko.Ink, strong: Boolean = false, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, modifier = Modifier.weight(0.42f).padding(top = 1.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            style = if (strong) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) FontFamily.Monospace else null,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f),
        )
    }
}

@Composable
fun LineDivider(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(Vinko.Line))

/** Botón compacto para las acciones de cada tarjeta. */
@Composable
fun CardAction(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    primary: Boolean = false,
    danger: Boolean = false,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    val padding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    val inner: @Composable () -> Unit = {
        if (loading) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = if (primary) Color.White else Vinko.Primary)
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
    if (primary) {
        Button(
            onClick = onClick, enabled = !loading, shape = shape, contentPadding = padding,
            modifier = Modifier.height(38.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Vinko.Primary, disabledContainerColor = Vinko.Lilac, disabledContentColor = Color.White),
        ) { inner() }
    } else {
        OutlinedButton(
            onClick = onClick, enabled = !loading, shape = shape, contentPadding = padding,
            modifier = Modifier.height(38.dp),
            border = BorderStroke(1.dp, if (danger) Vinko.CoralBg else Vinko.Line),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = if (danger) Vinko.Coral else Vinko.Ink),
        ) { inner() }
    }
}

@Composable
fun CountDot(count: Long, color: Color = Vinko.Coral) {
    Box(Modifier.background(color, CircleShape).padding(horizontal = 7.dp, vertical = 1.dp), contentAlignment = Alignment.Center) {
        Text(if (count > 99) "99+" else count.toString(), style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
fun IconBox(icon: ImageVector, tint: Color = Vinko.Primary, bg: Color = Vinko.Tint, size: Dp = 40.dp) {
    Box(Modifier.size(size).background(bg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size / 2))
    }
}
