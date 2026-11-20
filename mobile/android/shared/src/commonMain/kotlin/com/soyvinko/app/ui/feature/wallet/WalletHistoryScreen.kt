package com.soyvinko.app.ui.feature.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.ErrorBox
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.time
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private const val PAGE = 30

/** Texto claro para cada movimiento de dinero (movementLabel de MoneyWallet.tsx). */
fun movementLabel(e: JsonObject): String {
    val credit = (e.l("amount_minor") ?: 0) > 0
    val kind = e.s("kind").orEmpty()
    return when (kind) {
        "topup" -> if (credit) "Recarga de saldo" else "Compra de Vinkos"
        "vinko_purchase" -> "Compra de Vinkos"
        "order_payment" -> if (credit) "Venta en Marketplace (Cobro)" else "Pago de pedido en Marketplace"
        "order_refund" -> if (credit) "Reembolso de pedido cancelado" else "Reembolso debitado"
        "contract_funded" -> if (credit) "Devolución de custodia" else "Pago reservado en custodia"
        "contract_released" -> if (credit) "Pago recibido por un servicio" else "Pago liberado al profesional"
        "contract_refunded", "escrow_refund" -> "Reembolso de servicio"
        else -> if (kind.startsWith("withdrawal")) (if (credit) "Retiro devuelto" else "Retiro solicitado") else "Movimiento"
    }
}

/** Tipo de movimiento de Vinkos (tabla de historial de pages/Wallet.tsx). */
fun ledgerKindLabel(kind: String?): String = when (kind) {
    "welcome" -> "Bono de bienvenida"
    "contract_usage", "service_contact", "contract_debit" -> "Contratación de servicio"
    "package_purchase", "vinko_purchase", "purchase" -> "Paquete adquirido"
    "vinko_conversion" -> "Compra con saldo"
    null -> "Movimiento"
    else -> kind.replace('_', ' ').replaceFirstChar(Char::uppercase)
}

/** Historial de dinero (por moneda) y de Vinkos. */
@Composable
fun WalletHistoryScreen(onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    ScreenScaffold("Historial", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Segmented(
                options = listOf(0 to "Dinero", 1 to "Vinkos"),
                selected = tab,
                onSelect = { tab = it },
                icons = { if (it == 0) Icons.Outlined.Payments else Icons.Outlined.Toll },
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
            )
            if (tab == 0) MoneyHistory() else VinkoLedger()
        }
    }
}

@Composable
private fun MoneyHistory() {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var currency by rememberSaveable { mutableStateOf(Cur.VES) }
    val wallet = rememberResource(currency) { app.ops.call("money_wallet", "currency" to currency.code).obj() }

    var items by remember(currency) { mutableStateOf<List<JsonObject>?>(null) }
    var cursor by remember(currency) { mutableLongStateOf(BEFORE_ALL) }
    var hasMore by remember(currency) { mutableStateOf(false) }
    var loading by remember(currency) { mutableStateOf(false) }
    var error by remember(currency) { mutableStateOf<String?>(null) }
    var version by remember { mutableIntStateOf(0) }

    suspend fun load(reset: Boolean) {
        loading = true
        try {
            val page = friendly {
                app.ops.call(
                    "money_history",
                    "currency" to currency.code,
                    "limit" to PAGE,
                    "before" to if (reset) BEFORE_ALL else cursor,
                ).objects()
            }
            hasMore = page.size == PAGE
            page.lastOrNull()?.l("entry_id")?.let { cursor = it }
            items = (if (reset) emptyList() else items.orEmpty()) + page
            error = null
        } catch (e: ApiException) {
            error = e.message
        } finally {
            loading = false
        }
    }
    LaunchedEffect(currency, version) { load(reset = true) }

    // Solo la cuenta disponible: las retenciones internas no son movimientos para el usuario.
    val visible = items.orEmpty().filter { it.s("account_kind") == "available" }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Disponible", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
                    Text(
                        wallet.data?.let { money(it.l("available_minor") ?: 0, currency.code) } ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                Segmented(
                    options = Cur.entries.map { it to it.symbol },
                    selected = currency,
                    onSelect = { currency = it },
                    modifier = Modifier.width(150.dp),
                )
            }
        }
        when {
            items == null && error != null -> item { ErrorBox(error!!) { version++ } }
            items == null -> item { LoadingBox() }
            visible.isEmpty() && !hasMore -> item {
                EmptyBox("Sin movimientos todavía", "Tus recargas, pagos y compras de Vinkos en ${currency.lower} aparecerán aquí.")
            }
            else -> {
                groupedByDay(visible) { e ->
                    val amount = e.l("amount_minor") ?: 0
                    MoveRow(
                        credit = amount > 0,
                        title = movementLabel(e),
                        subtitle = "${time(e.s("created_at"))} · Saldo: ${money(e.l("balance_after_minor") ?: 0, e.s("currency") ?: currency.code)}",
                        amount = signedMoney(amount, e.s("currency") ?: currency.code),
                    )
                }
                if (hasMore) item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (loading) CircularProgressIndicator(Modifier.size(28.dp), color = Vinko.Primary, strokeWidth = 2.5.dp)
                        else TextButton(onClick = { scope.launch { load(reset = false) } }) { Text("Ver movimientos anteriores", color = Vinko.Primary) }
                    }
                }
                if (error != null && items != null) item { Banner(error!!) }
            }
        }
    }
}

@Composable
private fun VinkoLedger() {
    val app = LocalApp.current
    val wallet = rememberResource { app.ops.call("my_wallet").obj() ?: JsonObject(emptyMap()) }
    val data = wallet.data
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            data == null && wallet.loading -> item { LoadingBox() }
            data == null -> item { ErrorBox(wallet.error ?: "Inténtalo de nuevo.", wallet.reload) }
            else -> {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CoinBadge(40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Saldo de Vinkos", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
                            Text(vinkosText(data.l("balance") ?: data.l("credits") ?: 0), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
                val ledger = data.list("ledger")
                if (ledger.isEmpty()) {
                    item {
                        EmptyBox(
                            "Tu saldo de Vinkos está listo",
                            "Aquí aparecerán tus paquetes, bonificaciones y los Vinkos utilizados en contrataciones.",
                        )
                    }
                } else {
                    groupedByDay(ledger) { l ->
                        val delta = l.l("delta") ?: 0
                        val after = l.l("balance_after")
                        MoveRow(
                            credit = delta > 0,
                            title = l.s("reason")?.takeIf { it.isNotBlank() } ?: "Movimiento de Vinkos",
                            subtitle = "${ledgerKindLabel(l.s("kind"))} · ${time(l.s("created_at"))}",
                            amount = (if (delta > 0) "+" else "−") + vinkosText(kotlin.math.abs(delta)),
                            amountNote = after?.let { "Saldo ${count(it)}" },
                            leading = {
                                Box(
                                    Modifier.size(38.dp).background(if (delta > 0) Vinko.MintBg else Vinko.Tint, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Outlined.Toll, contentDescription = null, tint = if (delta > 0) Vinko.Mint else Vinko.Primary, modifier = Modifier.size(18.dp))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Agrupa por día (fecha local de Caracas) en tarjetas con un encabezado. */
private fun LazyListScope.groupedByDay(rows: List<JsonObject>, row: @Composable (JsonObject) -> Unit) {
    val groups = rows.groupBy { date(it.s("created_at")) }
    groups.forEach { (day, entries) ->
        item(key = "day-$day-${entries.first().hashCode()}") {
            Column {
                Text(day.ifEmpty { "Sin fecha" }, style = MaterialTheme.typography.labelMedium, color = Vinko.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                VCard(padding = 14.dp) {
                    entries.forEachIndexed { i, e ->
                        if (i > 0) Divider()
                        row(e)
                    }
                }
            }
        }
    }
}
