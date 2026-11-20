package com.soyvinko.app.ui.feature.pro

import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImage
import coil3.request.crossfade
import coil3.request.ImageRequest
import com.soyvinko.app.ui.common.softwareBitmaps
import com.soyvinko.app.ui.common.decodeImageBitmap
import com.soyvinko.app.ui.common.dialPhone
import com.soyvinko.app.ui.common.isoSeconds
import com.soyvinko.app.ui.common.shortDate
import com.soyvinko.app.ui.common.foldAccents
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import com.soyvinko.app.ui.common.minorToPlain
import com.soyvinko.app.ui.common.toDecimalOrNull
import com.soyvinko.app.ui.common.newUuid
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.soyvinko.app.ui.common.rememberApiImageLoader
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalShellScope
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.Resource
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// ---------------------------------------------------------------------------------------------
// Utilidades de datos (sin UI)
// ---------------------------------------------------------------------------------------------

/** Clave de idempotencia (uuid v4), igual que `newKey()` en la web. */
fun newKey(): String = newUuid()

fun currencyShort(currency: String?): String = if (currency == "USD") "USD" else "Bs"

/**
 * Importe escrito por la persona en formato es-VE ("1.234,56", "1234,5", "1234.50", "1234")
 * convertido a unidades menores (123456). Null si está vacío o no es un número válido.
 */
fun parseMajor(text: String): Long? {
    val t = text.trim().replace(" ", "").removePrefix("Bs").removePrefix("US$").removePrefix("$")
    if (t.isEmpty()) return null
    val normalized = when {
        t.contains(',') -> t.replace(".", "").replace(',', '.')
        t.count { it == '.' } == 1 && t.substringAfter('.').length in 1..2 -> t
        else -> t.replace(".", "")
    }
    val value = normalized.toDecimalOrNull() ?: return null
    if (value.signum < 0) return null
    return value.scaledHalfUp(2)
}

/** Unidades menores a texto editable: 450000 → "4500,00". */
fun majorText(minor: Long?): String =
    minor?.let { minorToPlain(it) } ?: ""

/** Mismo criterio que `professionalSlug` de la web y `normalize_professional_slug` del servidor. */
fun slugify(value: String): String =
    foldAccents(value)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

val SLUG_PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

fun parseInstant(iso: String?): Instant? = com.soyvinko.app.ui.common.parseInstant(iso)

/** Fecha ISO (yyyy-MM-dd) a texto "12 oct 2026". */
fun localDateText(iso: String?): String =
    iso?.let { runCatching { shortDate(LocalDate.parse(it.take(10))) }.getOrNull() } ?: ""

/** "08:30:00" → "08:30". */
fun hhmm(value: String?): String = value?.take(5).orEmpty()

val WEEKDAYS = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
val WEEKDAYS_SHORT = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")

/** Id devuelto por las operaciones que retornan un uuid. */
fun JsonElement.idText(): String? = (this as? JsonPrimitive)?.contentOrNull

/** ISO-8601 en UTC siempre con segundos ("2026-10-10T02:50:00Z"); OffsetDateTime.toString los omite si son :00. */
fun Instant.iso(): String = isoSeconds()

fun dial(context: PlatformContext, phone: String) {
    dialPhone(context, phone)
}

/** Avisos entre pantallas de la sección: al guardar en un formulario se recargan las listas. */
object ProSignals {
    var setup by mutableIntStateOf(0)
    var orders by mutableIntStateOf(0)
    var wallet by mutableIntStateOf(0)
}

/** Categorías activas (catalog) como mapa id → nombre. */
@Composable
fun rememberCategories(): Resource<List<Pair<String, String>>> {
    val app = LocalApp.current
    return rememberResource {
        app.ops.call("catalog").objects().mapNotNull { c -> c.s("id")?.let { it to (c.s("name") ?: "") } }
    }
}

// ---------------------------------------------------------------------------------------------
// Acción de formulario: muestra el error dentro del formulario (las hojas tapan el Snackbar).
// ---------------------------------------------------------------------------------------------

@Stable
class FormAction(
    private val scope: CoroutineScope,
    private val snackbar: SnackbarHostState,
    private val notices: CoroutineScope = NoticeScope,
) {
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var errorCode by mutableStateOf<String?>(null)

    fun reset() {
        error = null
        errorCode = null
    }

    fun fail(message: String) {
        error = message
        errorCode = null
    }

    fun run(success: String?, onSuccess: () -> Unit = {}, block: suspend () -> Unit) = runFor(onSuccess) {
        block()
        success
    }

    /** Como [run], pero el mensaje de éxito depende del resultado (p. ej. "eliminado" o "archivado"). */
    fun runFor(onSuccess: () -> Unit = {}, block: suspend () -> String?) {
        if (busy) return
        scope.launch(Dispatchers.Main.immediate) {
            busy = true
            reset()
            try {
                val message = block()
                onSuccess()
                // Ámbito de la app: el aviso sigue visible aunque el formulario cierre la pantalla.
                message?.let { notices.launch { snackbar.showSnackbar(it) } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                error = e.message
                errorCode = e.code
            } catch (e: Exception) {
                error = "No se pudo completar la operación. Inténtalo de nuevo."
            } finally {
                busy = false
            }
        }
    }
}

/** Respaldo si no hay ámbito del shell (LocalShellScope): avisos que sobreviven a la pantalla que los lanza. */
val NoticeScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

/** Crear en la pantalla (no dentro de la hoja) para que el aviso de éxito sobreviva al cierre. */
@Composable
fun rememberFormAction(): FormAction {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val notices = LocalShellScope.current ?: NoticeScope
    return remember { FormAction(scope, snackbar, notices) }
}

// ---------------------------------------------------------------------------------------------
// Componentes de formulario
// ---------------------------------------------------------------------------------------------

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Vinko.Primary,
    unfocusedBorderColor = Vinko.Line,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    disabledBorderColor = Vinko.Line,
    disabledTextColor = Vinko.Ink,
    disabledLabelColor = Vinko.Muted,
    focusedLabelColor = Vinko.Primary,
)

@Composable
fun AreaField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
    supporting: String? = null,
    error: String? = null,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = Vinko.Subtle) } },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = 8,
        isError = error != null,
        shape = RoundedCornerShape(14.dp),
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = fieldColors(),
    )
}

/** Campo de importe en la moneda indicada (se escribe en unidades mayores, formato es-VE). */
@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    currency: String?,
    label: String,
    supporting: String? = null,
    error: String? = null,
) {
    VinkoTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() || it == ',' || it == '.' }.take(15)) },
        label = "$label (${currencyShort(currency)})",
        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
        supporting = supporting,
        error = error,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerField(
    label: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Selecciona una opción",
    enabled: Boolean = true,
    supporting: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = Vinko.Subtle) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = supporting?.let { { Text(it) } },
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = {
                    onSelect(value)
                    expanded = false
                })
            }
        }
    }
}

/** Selector de fecha nativo (opcional). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: LocalDate?, onChange: (LocalDate?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value?.let(::shortDate) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            placeholder = { Text("Opcional", color = Vinko.Subtle) },
            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Vinko.Lavender) },
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(14.dp)).clickable { open = true })
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(state.selectedDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date })
                    open = false
                }) { Text("Aceptar", color = Vinko.Primary) }
            },
            dismissButton = {
                TextButton(onClick = {
                    onChange(null)
                    open = false
                }) { Text("Quitar fecha", color = Vinko.Muted) }
            },
        ) { DatePicker(state = state) }
    }
}

/** Fila de opciones en forma de píldora (selección única). */
@Composable
fun ChoiceChips(
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) -> Chip(label, value == selected) { onSelect(value) } }
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
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
                )
            }
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onChange(!checked) }
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
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = Vinko.Primary, uncheckedBorderColor = Vinko.Line, uncheckedTrackColor = Vinko.Tint),
        )
    }
}

/** Selector de una foto con vista previa. */
@Composable
fun PhotoSlot(part: UploadPart?, label: String, onPick: () -> Unit, height: Dp = 160.dp) {
    val bitmap = remember(part) { part?.let { decodeImageBitmap(it.bytes, 1280) } }
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(Vinko.Tint)
            .border(1.dp, Vinko.Lilac, RoundedCornerShape(16.dp))
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(height))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = Vinko.Primary, textAlign = TextAlign.Center)
                Text("JPG, PNG o WEBP · máx. 10 MB", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Contenedores, botones y etiquetas
// ---------------------------------------------------------------------------------------------

/** Hoja inferior para formularios; el error de la acción se muestra dentro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSheet(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    error: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Vinko.Cream) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted) }
            error?.let { Banner(it) }
            content()
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text(confirm, style = MaterialTheme.typography.labelLarge, color = if (danger) Vinko.Coral else Vinko.Primary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver", color = Vinko.Muted) } },
    )
}

enum class Tone { Primary, Outline, Soft, Danger }

/** Botón principal para acciones destructivas (eliminar). */
@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Vinko.Coral, disabledContainerColor = Vinko.Coral.copy(alpha = 0.5f), disabledContentColor = Color.White),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Botón cuadrado con papelera; `description` se lee con TalkBack y sirve para las pruebas. */
@Composable
fun DeleteIconButton(description: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 40.dp, overlay: Boolean = false) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(if (overlay) Color.White.copy(alpha = 0.92f) else Color.White)
            .border(1.dp, if (overlay) Color.Transparent else Vinko.CoralBg, RoundedCornerShape(12.dp))
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.DeleteOutline, contentDescription = description, tint = Vinko.Coral, modifier = Modifier.size(size * 0.48f))
    }
}

/**
 * Confirmación de borrado en hoja inferior: el error se muestra dentro y el botón indica el progreso.
 * Llamar a [onConfirm] con `form.run…` y cerrar la hoja en su `onSuccess`.
 */
@Composable
fun DeleteSheet(
    title: String,
    text: String,
    confirm: String,
    form: FormAction,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    FormSheet(title, onDismiss, subtitle = text, error = form.error) {
        Spacer(Modifier.height(4.dp))
        DangerButton(confirm, onClick = onConfirm, loading = form.busy)
        com.soyvinko.app.ui.components.SecondaryButton("Cancelar", onClick = onDismiss, enabled = !form.busy)
    }
}

@Composable
fun SmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: Tone = Tone.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    val padding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
    val inner: @Composable () -> Unit = {
        if (loading) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = if (tone == Tone.Primary) Color.White else Vinko.Primary)
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
    when (tone) {
        Tone.Primary -> Button(
            onClick = onClick, enabled = enabled && !loading, shape = shape, contentPadding = padding,
            modifier = modifier.height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Vinko.Primary, disabledContainerColor = Vinko.Lilac, disabledContentColor = Color.White),
        ) { inner() }
        Tone.Soft -> Button(
            onClick = onClick, enabled = enabled && !loading, shape = shape, contentPadding = padding,
            modifier = modifier.height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Vinko.Tint, contentColor = Vinko.Primary, disabledContainerColor = Vinko.Tint, disabledContentColor = Vinko.Lilac),
        ) { inner() }
        Tone.Outline, Tone.Danger -> OutlinedButton(
            onClick = onClick, enabled = enabled && !loading, shape = shape, contentPadding = padding,
            modifier = modifier.height(40.dp),
            border = BorderStroke(1.dp, if (tone == Tone.Danger) Vinko.CoralBg else Vinko.Line),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = if (tone == Tone.Danger) Vinko.Coral else Vinko.Ink),
        ) { inner() }
    }
}

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

/** Título de sección con acción opcional a la derecha. */
@Composable
fun SectionHeader(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted) }
        }
        action?.invoke()
    }
}

/** Fila de navegación con icono, título, subtítulo y flecha. */
@Composable
fun NavRow(icon: ImageVector, title: String, subtitle: String, last: Boolean = false, badge: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.soyvinko.app.ui.common.IconTile(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
        if (badge != null) {
            Text(
                badge,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.background(Vinko.Primary, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Vinko.Lilac, modifier = Modifier.size(20.dp))
    }
    if (!last) Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(1.dp).background(Vinko.Line))
}

@Composable
fun LineDivider(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(Vinko.Line))

/** Etiqueta y valor en dos columnas. */
@Composable
fun KeyValue(label: String, value: String, strong: Boolean = false, valueColor: Color = Vinko.Ink) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted, modifier = Modifier.weight(1f))
        Text(
            value,
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

/** Línea con icono pequeño y texto. */
@Composable
fun IconLine(icon: ImageVector, text: String, tint: Color = Vinko.Muted, color: Color = Vinko.Muted) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
fun Stars(rating: Double, size: Dp = 16.dp) {
    Row {
        repeat(5) { i ->
            val on = rating >= i + 0.5
            Icon(
                if (on) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (on) Color(0xFFF5A524) else Vinko.Lilac,
                modifier = Modifier.size(size),
            )
        }
    }
}

/**
 * Foto privada (/api/v1/files/{id}) con el cargador compartido de la app. Mientras carga o si falla
 * muestra un icono suave de marca; al cargar marca el nodo con la etiqueta [IMAGE_LOADED_TAG].
 */
@Composable
fun ProImage(file: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop, iconSize: Dp = 28.dp) {
    val app = LocalApp.current
    val url = file?.let {
        when {
            it.startsWith("http") -> it
            it.startsWith("/") -> app.api.absoluteUrl(it)
            else -> app.api.absoluteUrl("/api/v1/files/$it")
        }
    }
    var loaded by remember(url) { mutableStateOf(false) }
    val context = LocalPlatformContext.current
    // Sin fundido (la foto aparece completa al quitar el icono) y en memoria normal: los bitmaps de
    // hardware no se pueden dibujar en lienzos de software (capturas para compartir y pruebas).
    val request = remember(url) { url?.let { ImageRequest.Builder(context).data(it).crossfade(false).softwareBitmaps().build() } }
    Box(modifier.background(Vinko.Tint).testTag(if (loaded) IMAGE_LOADED_TAG else IMAGE_TAG), contentAlignment = Alignment.Center) {
        if (!loaded) Icon(Icons.Outlined.Image, contentDescription = null, tint = Vinko.Lilac, modifier = Modifier.size(iconSize))
        if (request != null) {
            AsyncImage(
                model = request,
                imageLoader = rememberApiImageLoader(),
                contentDescription = null,
                contentScale = contentScale,
                onState = { loaded = it is AsyncImagePainter.State.Success },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

const val IMAGE_TAG = "pro-image"
const val IMAGE_LOADED_TAG = "pro-image-loaded"

/** Monograma redondo con las iniciales. */
@Composable
fun Monogram(name: String, size: Dp = 52.dp, bg: Color = Vinko.Tint, fg: Color = Vinko.Primary) {
    val initials = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1).uppercase() }.ifEmpty { "V" }
    Box(Modifier.size(size).background(bg, CircleShape), contentAlignment = Alignment.Center) {
        Text(initials, style = MaterialTheme.typography.titleLarge, color = fg)
    }
}
