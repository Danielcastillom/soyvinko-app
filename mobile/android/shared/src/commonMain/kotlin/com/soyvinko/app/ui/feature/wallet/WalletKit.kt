package com.soyvinko.app.ui.feature.wallet

import com.soyvinko.app.ui.common.formatInt
import com.soyvinko.app.ui.common.newUuid
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch

/** Monedas de la wallet de dinero (igual que CURRENCIES en MoneyWallet.tsx). */
enum class Cur(val code: String, val symbol: String, val label: String) {
    VES("VES", "Bs", "Bolívares"),
    USD("USD", "US$", "Dólares");

    val lower: String get() = label.lowercase()

    companion object {
        fun of(code: String?): Cur = entries.firstOrNull { it.code == code } ?: VES
    }
}

/** `before` inicial de money_history: trae desde el movimiento más reciente. */
const val BEFORE_ALL = 9_000_000_000_000_000L

/** Rutas internas de la sección (prefijo "wallet/"). */
object WalletRoutes {
    const val PLANS = "wallet/plans"
    const val ADD_PAYOUT_METHOD = "wallet/payout-method"
}

/** Valor simple de la respuesta (los uuid que devuelven create_topup o start_purchase). */
fun kotlinx.serialization.json.JsonElement.str(): String =
    (this as? kotlinx.serialization.json.JsonPrimitive)?.content ?: throw ApiException("Recibimos una respuesta inesperada del servidor.", "bad_response", 200)

fun newKey(): String = newUuid()

fun count(n: Long): String = formatInt(n)

fun vinkosText(n: Long): String = "${count(n)} ${if (n == 1L) "Vinko" else "Vinkos"}"

fun signedMoney(minor: Long, currency: String?): String =
    (if (minor > 0) "+" else "−") + money(kotlin.math.abs(minor), currency)

// ---------- Importes en unidades mayores con formato es-VE (1.234,56) ----------

/**
 * Texto crudo del campo de monto: solo dígitos y, como mucho, una coma con 2 decimales.
 * El punto que escribe el usuario se toma como separador decimal; los puntos de miles se dibujan
 * con [AmountTransformation], nunca se guardan.
 */
fun sanitizeAmount(input: String): String {
    val intPart = StringBuilder()
    val decimals = StringBuilder()
    var comma = false
    for (c in input) {
        when {
            c.isDigit() && !comma -> intPart.append(c)
            c.isDigit() && decimals.length < 2 -> decimals.append(c)
            (c == ',' || c == '.') && !comma -> comma = true
        }
    }
    var whole = intPart.toString().trimStart('0').take(12)
    if (whole.isEmpty() && (intPart.isNotEmpty() || comma)) whole = "0"
    return if (comma) "$whole,$decimals" else whole
}

/** "4.750,5" (crudo "4750,5") → 475050. Null si está vacío o no es un número. */
fun parseMinor(raw: String): Long? {
    if (raw.isBlank()) return null
    val whole = raw.substringBefore(',').ifEmpty { "0" }.toLongOrNull() ?: return null
    val dec = raw.substringAfter(',', "").padEnd(2, '0').take(2).toLongOrNull() ?: return null
    return whole * 100 + dec
}

/** 475000 → "4750"; 475050 → "4750,50" (crudo, se muestra como 4.750,50). */
fun rawFromMinor(minor: Long): String {
    val whole = minor / 100
    val frac = minor % 100
    return if (frac == 0L) "$whole" else "$whole," + frac.toString().padStart(2, '0')
}

/** Dibuja los separadores de miles de es-VE sobre el texto crudo. */
object AmountTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val n = raw.substringBefore(',').length
        val out = StringBuilder()
        raw.forEachIndexed { i, c ->
            if (i in 1 until n && (n - i) % 3 == 0) out.append('.')
            out.append(c)
        }
        fun toOut(o: Int): Int = o + (1 until n).count { i -> (n - i) % 3 == 0 && i < o }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = toOut(offset)
            override fun transformedToOriginal(offset: Int): Int =
                (raw.length downTo 0).firstOrNull { toOut(it) <= offset } ?: 0
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}

// ---------- Errores ----------

/** Textos para los códigos que el servidor devuelve sin traducir. */
private val CODES = mapOf(
    "verified_identity_required_for_withdrawal" to "Para retirar necesitas tu verificación de identidad (KYC) aprobada y vigente.",
    "withdrawal_amount_too_small" to "El monto es demasiado bajo: la comisión sería igual o mayor que lo que recibirías.",
    "withdrawal_quote_expired" to "La cotización venció. Vuelve a calcular cuánto recibes.",
    "withdrawal_quote_not_owned" to "No encontramos esa cotización. Vuelve a calcularla.",
    "withdrawal_idempotency_conflict" to "Este retiro ya se registró con otra cotización. Actualiza e inténtalo de nuevo.",
    "withdrawal_already_processing" to "Este retiro ya se está procesando y no se puede cancelar.",
    "withdrawal_not_owned" to "No encontramos ese retiro.",
    "wallet_frozen" to "Tu wallet en esta moneda está congelada. Escríbenos a soporte para revisarla.",
    "exchange_rate_not_configured" to "La tasa de Vinkos para esta moneda no está disponible por ahora.",
    "invalid_vinkos_amount" to "Indica una cantidad de Vinkos válida.",
    "invalid_currency" to "Selecciona bolívares (Bs) o dólares (US$).",
    "topup_idempotency_conflict" to "Ya registraste una recarga con otro monto. Cierra y vuelve a intentarlo.",
    "idempotency_conflict" to "Esta compra ya se registró. Actualiza la página.",
    "plan_unavailable" to "Este paquete ya no está disponible.",
    "payout_method_not_owned" to "No encontramos esa cuenta de retiro.",
    "invalid_pagination" to "No pudimos cargar más movimientos.",
)

fun friendlyMessage(e: ApiException): String {
    val code = e.message.takeIf { Regex("[a-z][a-z0-9_]{2,90}").matches(it) } ?: e.code
    return CODES[code] ?: CODES[e.message] ?: e.message
}

/** Ejecuta una llamada y traduce los códigos sin texto a mensajes claros. */
suspend fun <T> friendly(block: suspend () -> T): T = try {
    block()
} catch (e: ApiException) {
    throw ApiException(friendlyMessage(e), e.code, e.status, e.fieldErrors)
}

// ---------- Ciclo de vida ----------

/** Llama a [action] cada vez que la pantalla vuelve a primer plano (no en la primera apertura). */
@Composable
fun OnReturn(action: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val current by rememberUpdatedState(action)
    DisposableEffect(owner) {
        var first = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (first) first = false else current()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

// ---------- Componentes ----------

val CoinGradient = Brush.linearGradient(listOf(Color(0xFFFFD76A), Color(0xFFFFA800)))

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = Vinko.Ink, modifier = modifier.padding(bottom = 8.dp))
}

/** Selector segmentado (moneda, tipo de cuenta, pestañas). */
@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: (T) -> Boolean = { true },
    icons: (T) -> ImageVector? = { null },
) {
    Row(
        modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            val on = enabled(value)
            Row(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(if (active) Color.White else Color.Transparent, RoundedCornerShape(11.dp))
                    .then(if (active) Modifier.border(1.dp, Vinko.Line, RoundedCornerShape(11.dp)) else Modifier)
                    .clickable(enabled = on && !active) { onSelect(value) }
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = when {
                    !on -> Vinko.Subtle.copy(alpha = 0.6f)
                    active -> Vinko.Primary
                    else -> Vinko.Muted
                }
                icons(value)?.let {
                    Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Chip de cantidad rápida. */
@Composable
fun QuickChip(text: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(36.dp)
            .background(if (selected) Vinko.Primary else Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) Vinko.Primary else Vinko.Line, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium,
            color = when {
                selected -> Color.White
                !enabled -> Vinko.Subtle.copy(alpha = 0.6f)
                else -> Vinko.Ink
            },
        )
    }
}

/** Fila etiqueta / valor de un resumen. */
@Composable
fun SummaryRow(label: String, value: String, strong: Boolean = false, valueColor: Color = Vinko.Ink) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = if (strong) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (strong) Vinko.Ink else Vinko.Muted,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = if (strong) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Vinko.Line))
}

/** Bloque de resumen con fondo lila suave. */
@Composable
fun SummaryBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().background(Vinko.Cream, RoundedCornerShape(16.dp)).border(1.dp, Vinko.Line, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        content = content,
    )
}

/** Moneda dorada de Vinkos. */
@Composable
fun CoinBadge(size: Dp = 44.dp) {
    Box(Modifier.size(size).background(CoinGradient, CircleShape), contentAlignment = Alignment.Center) {
        Text("V", fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4A2600), fontSize = (size.value * 0.45f).sp)
    }
}

/** Botón pequeño con borde (Recargar, Simular pago). */
@Composable
fun SmallButton(text: String, onClick: () -> Unit, icon: ImageVector? = null, enabled: Boolean = true, loading: Boolean = false) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Vinko.Line),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Vinko.Primary),
        modifier = Modifier.height(36.dp),
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), color = Vinko.Primary, strokeWidth = 2.dp)
        } else {
            icon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Icono redondo de entrada / salida de un movimiento. */
@Composable
fun MoveIcon(credit: Boolean) {
    Box(
        Modifier.size(38.dp).background(if (credit) Vinko.MintBg else Vinko.Tint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (credit) Icons.Outlined.SouthWest else Icons.Outlined.NorthEast,
            contentDescription = if (credit) "Entrada" else "Salida",
            tint = if (credit) Vinko.Mint else Vinko.Primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Fila de movimiento: icono, concepto, detalle y monto. */
@Composable
fun MoveRow(
    credit: Boolean,
    title: String,
    subtitle: String,
    amount: String,
    amountNote: String? = null,
    leading: @Composable () -> Unit = { MoveIcon(credit) },
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        leading()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (amount.isNotEmpty()) Text(amount, style = MaterialTheme.typography.titleSmall, color = if (credit) Vinko.Mint else Vinko.Ink)
            amountNote?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted) }
            trailing?.invoke()
        }
    }
}

@Composable
fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Vinko.Primary,
    unfocusedBorderColor = Vinko.Line,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    focusedLabelColor = Vinko.Primary,
)

/** Campo de monto en unidades mayores (Bs 1.234,56) que entrega el texto crudo. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    currency: Cur,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    error: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(sanitizeAmount(it)) },
        label = { Text(label) },
        prefix = { Text("${currency.symbol} ", color = Vinko.Muted, style = MaterialTheme.typography.titleMedium) },
        placeholder = { Text("0,00", style = MaterialTheme.typography.titleMedium, color = Vinko.Subtle) },
        textStyle = MaterialTheme.typography.titleMedium,
        singleLine = true,
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        trailingIcon = trailing,
        visualTransformation = AmountTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Lista desplegable de solo lectura (bancos, prefijos, tipo de documento). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Dropdown(
    label: String,
    options: List<T>,
    selected: T,
    text: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    menuText: (T) -> String = text,
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            value = text(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(menuText(option), style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                )
            }
        }
    }
}

/** Acciones de una hoja inferior: principal a todo el ancho y, debajo, la secundaria. */
@Composable
fun SheetActions(
    primary: String,
    onPrimary: () -> Unit,
    secondary: String?,
    onSecondary: () -> Unit,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PrimaryButton(primary, onClick = onPrimary, loading = loading, enabled = enabled)
        if (secondary != null) SecondaryButton(secondary, onClick = onSecondary, enabled = !loading)
    }
}

/**
 * Hoja inferior de la wallet. `content` recibe `close()`, que anima el cierre antes de avisar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { state.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Color.White) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding(),
        ) { content(close) }
    }
}
