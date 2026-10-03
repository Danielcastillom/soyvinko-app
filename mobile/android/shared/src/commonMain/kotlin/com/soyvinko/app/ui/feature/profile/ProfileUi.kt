package com.soyvinko.app.ui.feature.profile

import com.soyvinko.app.ui.common.nowInstant
import kotlinx.datetime.Instant
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

// Utilidades visuales y de formularios compartidas por las pantallas de Perfil.

/** Ámbito que sobrevive a la pantalla: el aviso se ve aunque se vuelva atrás tras guardar. */
private val noticeScope = MainScope()

fun SnackbarHostState.notify(message: String) {
    noticeScope.launch { showSnackbar(message) }
}

/** Petición sin respuesta visible que debe terminar aunque se salga de la pantalla (p. ej. marcar como leída). */
fun fireAndForget(block: suspend () -> Unit) {
    noticeScope.launch { runCatching { block() } }
}

/** Envío de formulario con progreso, error visible en la pantalla y aviso de éxito. */
class Submitter(
    val busy: Boolean,
    val error: String?,
    val fieldErrors: Map<String, String>,
    private val start: (String?, suspend () -> Unit) -> Unit,
    val clear: () -> Unit,
) {
    fun run(success: String? = null, block: suspend () -> Unit) = start(success, block)
}

@Composable
fun rememberSubmitter(): Submitter {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var fields by remember { mutableStateOf(emptyMap<String, String>()) }
    return Submitter(
        busy = busy,
        error = error,
        fieldErrors = fields,
        start = { success, block ->
            if (!busy) scope.launch(Dispatchers.Main.immediate) {
                busy = true
                error = null
                fields = emptyMap()
                try {
                    block()
                    success?.let { snackbar.notify(it) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ApiException) {
                    error = e.message
                    fields = e.fieldErrors
                } catch (e: Exception) {
                    error = "No se pudo completar la operación. Inténtalo de nuevo."
                } finally {
                    busy = false
                }
            }
        },
        clear = {
            error = null
            fields = emptyMap()
        },
    )
}

/** Contenido desplazable estándar dentro de un ScreenScaffold. */
@Composable
fun ProfilePage(padding: PaddingValues, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

/** Encabezado de tarjeta: icono, título y explicación breve. */
@Composable
fun CardHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = Vinko.Primary,
    bg: Color = Vinko.Tint,
    trailing: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        com.soyvinko.app.ui.common.IconTile(icon, tint, bg)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
        trailing()
    }
}

/** Fila de menú con icono, textos, contador opcional y flecha. */
@Composable
fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badge: Int? = null,
    tint: Color = Vinko.Primary,
    bg: Color = Vinko.Tint,
    last: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.soyvinko.app.ui.common.IconTile(icon, tint, bg)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (badge != null && badge > 0) {
            CountBadge(badge)
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Vinko.Lilac, modifier = Modifier.size(20.dp))
    }
    if (!last) Divider()
}

@Composable
fun Divider(modifier: Modifier = Modifier) =
    Box(modifier.fillMaxWidth().padding(horizontal = 10.dp).height(1.dp).background(Vinko.Line))

@Composable
fun CountBadge(count: Int, color: Color = Vinko.Coral) {
    Box(
        Modifier.background(color, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (count > 99) "99+" else count.toString(), style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

/** Etiqueta pequeña tipo píldora. */
@Composable
fun Pill(text: String, fg: Color, bg: Color, icon: ImageVector? = null) {
    Row(
        Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

/** Par etiqueta / valor en una fila. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, modifier = Modifier.weight(0.42f))
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(0.58f))
    }
}

/** Círculo con la inicial del nombre. */
@Composable
fun Avatar(name: String, size: Dp = 64.dp, light: Boolean = true) {
    Box(
        Modifier
            .size(size)
            .background(if (light) Color.White.copy(alpha = 0.16f) else Vinko.Tint, CircleShape)
            .border(2.dp, if (light) Color.White.copy(alpha = 0.35f) else Vinko.Lilac, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "V" },
            fontFamily = Manrope,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.headlineSmall,
            color = if (light) Color.White else Vinko.Primary,
        )
    }
}

/** Opciones excluyentes como chips (tipo de contacto, categoría, tipo de documento). */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChoiceChips(options: List<Pair<String, String>>, selected: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    selectedContainerColor = Vinko.Tint,
                    selectedLabelColor = Vinko.Primary,
                    labelColor = Vinko.Ink,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = value == selected,
                    borderColor = Vinko.Line,
                    selectedBorderColor = Vinko.Primary,
                ),
            )
        }
    }
}

@Composable
fun vinkoFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Vinko.Primary,
    unfocusedBorderColor = Vinko.Line,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Vinko.Cream,
    disabledBorderColor = Vinko.Line,
    focusedLabelColor = Vinko.Primary,
)

/** Campo de varias líneas con el estilo de VinkoTextField. */
@Composable
fun AreaField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minLines: Int = 3,
    maxLength: Int = 2000,
    error: String? = null,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = Vinko.Subtle) } },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = 8,
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = vinkoFieldColors(),
    )
}

/** Selector desplegable (estado, municipio, parroquia…). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectField(
    label: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    placeholder: String = "Selecciona una opción",
    supporting: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val open = expanded && enabled && options.isNotEmpty()
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { if (enabled) expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = Vinko.Subtle) },
            supportingText = supporting?.let { { Text(it) } },
            trailingIcon = {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Vinko.Primary, strokeWidth = 2.dp)
                else ExposedDropdownMenuDefaults.TrailingIcon(expanded = open)
            },
            shape = RoundedCornerShape(14.dp),
            colors = vinkoFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { expanded = false }, containerColor = Color.White) {
            options.forEach { (value, name) ->
                DropdownMenuItem(
                    text = { Text(name, fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

/** Confirmación antes de acciones irreversibles. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, color = if (destructive) Vinko.Coral else Vinko.Primary, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Vinko.Muted) } },
    )
}

/** Bloque de texto atenuado. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, modifier = modifier)

/** Objeto obligatorio de la respuesta: si falta, se muestra como error con reintento. */
fun kotlinx.serialization.json.JsonElement.requireObj(): JsonObject =
    this as? JsonObject ?: throw ApiException("Recibimos una respuesta inesperada del servidor.", "bad_response", 200)

fun parseInstant(iso: String?): Instant? = com.soyvinko.app.ui.common.parseInstant(iso)

// --- Estado de verificación de identidad (KYC), común al hub y a la pantalla de verificación ---

enum class KycState { None, Pending, Approved, Expired, Rejected }

fun kycStateOf(latest: JsonObject?): KycState {
    val status = latest.s("status") ?: return KycState.None
    val expires = parseInstant(latest.s("expires_at"))
    return when (status) {
        "approved" -> if (expires != null && expires < nowInstant()) KycState.Expired else KycState.Approved
        "pending", "in_review" -> KycState.Pending
        "expired" -> KycState.Expired
        "rejected", "revoked" -> KycState.Rejected
        else -> KycState.None
    }
}
