package com.soyvinko.app.ui.feature.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.o
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.StatusBadge
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.screens.HeroGradient
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private val QUICK_VINKOS = listOf(5, 10, 25, 50)
private const val MAX_VINKOS = 100_000

/** Datos fijados al abrir la confirmación: lo que se ve es exactamente lo que se envía. */
private data class Purchase(val key: String, val currency: Cur, val vinkos: Int, val rate: Long, val cost: Long)

private data class TopupFor(val currency: Cur, val suggestedMinor: Long? = null)

/**
 * Pestaña Wallet: Vinkos, saldo en bolívares y dólares, compra de Vinkos con saldo,
 * recargas y accesos a historial, retiros y paquetes. (Web: pages/Wallet.tsx + MoneyWallet.tsx)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onNavigate: (String) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    val config = rememberResource { app.api.raw("GET", "/api/v1/config", auth = false).obj() ?: JsonObject(emptyMap()) }
    val credits = rememberResource { app.ops.call("my_wallet").obj() ?: JsonObject(emptyMap()) }
    val ves = rememberResource { app.ops.call("money_wallet", "currency" to "VES").obj() ?: JsonObject(emptyMap()) }
    val usd = rememberResource { app.ops.call("money_wallet", "currency" to "USD").obj() ?: JsonObject(emptyMap()) }
    val rates = rememberResource { app.ops.call("get_vinko_exchange_rates").obj() ?: JsonObject(emptyMap()) }
    val topups = rememberResource { app.ops.call("my_topups").objects() }

    val sandbox = config.data.s("payment_mode") == "sandbox"
    val vinkoBalance = credits.data?.let { it.l("balance") ?: it.l("credits") } ?: 0L
    val wallets = mapOf(Cur.VES to ves.data, Cur.USD to usd.data)

    fun reloadMoney() {
        ves.reload(); usd.reload(); topups.reload()
    }
    fun reloadAll() {
        credits.reload(); rates.reload(); config.reload(); reloadMoney()
    }
    OnReturn { reloadAll() }

    // Conversor
    var currency by rememberSaveable { mutableStateOf(Cur.VES) }
    var vinkos by rememberSaveable { mutableIntStateOf(10) }
    LaunchedEffect(rates.data) {
        val r = rates.data ?: return@LaunchedEffect
        if (r.o(currency.code) == null) Cur.entries.firstOrNull { r.o(it.code) != null }?.let { currency = it }
    }

    var purchase by remember { mutableStateOf<Purchase?>(null) }
    var topupFor by remember { mutableStateOf<TopupFor?>(null) }
    var info by remember { mutableStateOf(false) }

    val loading = credits.loading || ves.loading || usd.loading || rates.loading
    var pulled by remember { mutableStateOf(false) }
    LaunchedEffect(loading) { if (!loading) pulled = false }

    val scroll = rememberScrollState()
    var converterY by remember { mutableFloatStateOf(0f) }

    ScreenScaffold(
        title = "Mi wallet",
        onBack = onBack,
        actions = {
            IconButton(onClick = { info = true }) {
                Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "¿Qué son los Vinkos?", tint = Vinko.Primary)
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = pulled && loading,
            onRefresh = { pulled = true; reloadAll() },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val firstError = listOf(credits, ves, usd).firstOrNull { it.error != null && it.data == null }?.error
                if (firstError != null) {
                    Banner(firstError)
                    TextButton(onClick = { reloadAll() }) { Text("Reintentar", color = Vinko.Primary) }
                }

                VinkoHero(
                    balance = credits.data?.let { vinkoBalance },
                    onBuy = { scope.launch { scroll.animateScrollTo(converterY.toInt()) } },
                    onPlans = { onNavigate(WalletRoutes.PLANS) },
                    onHistory = { onNavigate(Routes.WALLET_HISTORY) },
                    onWithdraw = { onNavigate(Routes.WITHDRAW) },
                )

                SectionLabel("Saldo en dinero")
                Cur.entries.forEach { c ->
                    BalanceCard(c, wallets[c], onTopup = { topupFor = TopupFor(c) })
                }

                Box(Modifier.onGloballyPositioned { converterY = it.positionInParent().y }) {
                    Converter(
                        currency = currency,
                        onCurrency = { currency = it },
                        vinkos = vinkos,
                        onVinkos = { vinkos = it },
                        rates = rates.data,
                        ratesLoading = rates.loading,
                        available = wallets[currency].l("available_minor") ?: 0L,
                        vinkoBalance = vinkoBalance,
                        onTopup = { missing -> topupFor = TopupFor(currency, missing) },
                        onBuy = { rate ->
                            purchase = Purchase(newKey(), currency, vinkos, rate, vinkos * rate)
                        },
                    )
                }

                val pending = topups.data.orEmpty().filter { it.s("status") != "settled" }
                if (pending.isNotEmpty()) PendingTopups(pending, sandbox, onSettled = { reloadMoney() })

                SectionLabel("Más opciones")
                VCard(padding = 6.dp) {
                    Shortcut(Icons.AutoMirrored.Outlined.ReceiptLong, "Historial de movimientos", "Dinero y Vinkos, con todos los detalles") { onNavigate(Routes.WALLET_HISTORY) }
                    Shortcut(Icons.Outlined.AccountBalance, "Retirar saldo", "A tu cuenta bancaria o Pago Móvil") { onNavigate(Routes.WITHDRAW) }
                    Shortcut(Icons.Outlined.WorkspacePremium, "Paquetes de Vinkos", "Planes oficiales y beneficios", last = true) { onNavigate(WalletRoutes.PLANS) }
                }

                Pillars()
            }
        }
    }

    purchase?.let { p ->
        ConfirmPurchaseSheet(
            purchase = p,
            onDismiss = { purchase = null },
            onDone = { reloadAll() },
        )
    }

    topupFor?.let { t ->
        TopupSheet(
            request = t,
            sandbox = sandbox,
            onDismiss = { topupFor = null },
            onCredited = { msg ->
                reloadMoney()
                scope.launch { snackbar.showSnackbar(msg) }
            },
            onPending = { reloadMoney() },
        )
    }

    if (info) VinkoInfoSheet(onDismiss = { info = false })
}

// ---------------------------------------------------------------------------------------------
// Tarjeta principal de Vinkos

@Composable
private fun VinkoHero(balance: Long?, onBuy: () -> Unit, onPlans: () -> Unit, onHistory: () -> Unit, onWithdraw: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(HeroGradient, RoundedCornerShape(24.dp)).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinBadge(40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Créditos Vinkos", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("Moneda oficial de SoyVinko", style = MaterialTheme.typography.bodySmall, color = Vinko.Lilac)
            }
            Row(
                Modifier.background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(7.dp).background(Color(0xFF4ADE80), CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("Activa", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Text(
                balance?.let { count(it) } ?: "—",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp, lineHeight = 46.sp),
                color = Color.White,
            )
            Spacer(Modifier.width(8.dp))
            Text("Vinkos", style = MaterialTheme.typography.titleMedium, color = Vinko.Lilac, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text(
            "Saldo disponible para contratar y pactar servicios",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.78f),
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroAction(Icons.Outlined.ShoppingCart, "Comprar", Modifier.weight(1f), onBuy)
            HeroAction(Icons.Outlined.WorkspacePremium, "Paquetes", Modifier.weight(1f), onPlans)
            HeroAction(Icons.AutoMirrored.Outlined.ReceiptLong, "Historial", Modifier.weight(1f), onHistory)
            HeroAction(Icons.Outlined.AccountBalance, "Retirar", Modifier.weight(1f), onWithdraw)
        }
    }
}

@Composable
private fun HeroAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// Saldos en dinero

@Composable
private fun BalanceCard(c: Cur, wallet: JsonObject?, onTopup: () -> Unit) {
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Outlined.AccountBalanceWallet)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Saldo en ${c.lower}", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
                Text(
                    wallet?.let { money(it.l("available_minor") ?: 0, c.code) } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            SmallButton("Recargar", onTopup, icon = Icons.Outlined.Add)
        }
        val custody = wallet.l("reserved_for_services_minor") ?: 0
        val pending = wallet.l("withdrawal_pending_minor") ?: 0
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Meta(Icons.Outlined.Shield, "${money(custody, c.code)} en custodia")
            if (pending > 0) Meta(Icons.Outlined.Schedule, "${money(pending, c.code)} en retiro")
        }
    }
}

@Composable
private fun Meta(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Vinko.Subtle, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
    }
}

// ---------------------------------------------------------------------------------------------
// Conversor de saldo a Vinkos

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Converter(
    currency: Cur,
    onCurrency: (Cur) -> Unit,
    vinkos: Int,
    onVinkos: (Int) -> Unit,
    rates: JsonObject?,
    ratesLoading: Boolean,
    available: Long,
    vinkoBalance: Long,
    onTopup: (Long) -> Unit,
    onBuy: (rate: Long) -> Unit,
) {
    val rateInfo = rates.o(currency.code)
    val rate = rateInfo.l("rate_minor")?.takeIf { it > 0 }
    val valid = vinkos in 1..MAX_VINKOS
    val cost = if (rate != null && valid) vinkos * rate else 0L
    val maxVinkos = if (rate != null) (available / rate).coerceAtMost(MAX_VINKOS.toLong()).toInt() else 0
    val missing = (cost - available).coerceAtLeast(0)

    VCard(modifier = Modifier.testTag("converter"), padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinBadge(44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Compra Vinkos con tu saldo", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Conversión inmediata y sin comisiones. Los Vinkos se acreditan al instante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Muted,
                )
            }
        }
        Spacer(Modifier.height(18.dp))

        FieldLabel("Pagar con")
        Segmented(
            options = Cur.entries.map { it to "${it.symbol} · ${it.label}" },
            selected = currency,
            onSelect = onCurrency,
            enabled = { rates.o(it.code) != null },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            buildAnnotatedString {
                append("Disponible: ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Vinko.Ink)) { append(money(available, currency.code)) }
            },
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Muted,
        )
        Spacer(Modifier.height(18.dp))

        FieldLabel("¿Cuántos Vinkos quieres?")
        Stepper(vinkos, onVinkos)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_VINKOS.forEach { n -> QuickChip("$n", selected = vinkos == n, onClick = { onVinkos(n) }) }
            QuickChip(
                "Máximo (${count(maxVinkos.toLong())})",
                selected = maxVinkos > 0 && vinkos == maxVinkos && vinkos !in QUICK_VINKOS,
                onClick = { onVinkos(maxVinkos) },
                enabled = maxVinkos >= 1,
            )
        }
        Spacer(Modifier.height(18.dp))

        SummaryBox {
            Text(
                when {
                    rate != null -> buildAnnotatedString {
                        append("1 Vinko = ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Vinko.Primary)) { append(money(rate, currency.code)) }
                        rateInfo.s("created_at")?.let { append(" · Tasa del ${date(it)}") }
                    }
                    ratesLoading -> buildAnnotatedString { append("Cargando tasa…") }
                    else -> buildAnnotatedString { append("Tasa no disponible para esta moneda") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Muted,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            Divider(Modifier.padding(vertical = 4.dp))
            SummaryRow("Recibes", vinkosText(if (valid) vinkos.toLong() else 0))
            SummaryRow("Comisión", "Gratis", valueColor = Vinko.Mint)
            Divider(Modifier.padding(vertical = 4.dp))
            SummaryRow("Total a pagar", money(cost, currency.code), strong = true)
            Divider(Modifier.padding(vertical = 4.dp))
            SummaryRow("Saldo después", money((available - cost).coerceAtLeast(0), currency.code))
            SummaryRow("Vinkos después", vinkosText(vinkoBalance + if (valid) vinkos else 0))
        }

        if (missing > 0 && valid && rate != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().background(Vinko.AmberBg, RoundedCornerShape(14.dp)).padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Vinko.Amber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    buildAnnotatedString {
                        append("Te faltan ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(money(missing, currency.code)) }
                        append(" para esta compra.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Vinko.Amber,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onTopup(missing) }) { Text("Recargar", color = Vinko.Primary, style = MaterialTheme.typography.labelLarge) }
            }
        }

        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            "Comprar ${vinkosText(if (valid) vinkos.toLong() else 0)}",
            onClick = { rate?.let(onBuy) },
            enabled = rate != null && valid && missing == 0L,
        )
    }
}

@Composable
private fun Stepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(60.dp).background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Vinko.Line, RoundedCornerShape(14.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(Icons.Outlined.Remove, "Restar un Vinko", enabled = value > 1) { onChange((value - 1).coerceAtLeast(1)) }
        BasicTextField(
            value = if (value > 0) value.toString() else "",
            onValueChange = { raw -> onChange(raw.filter(Char::isDigit).take(6).toIntOrNull()?.coerceAtMost(MAX_VINKOS) ?: 0) },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center, color = Vinko.Ink, fontFamily = Manrope),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).semantics { contentDescription = "Cantidad de Vinkos" },
        )
        StepButton(Icons.Outlined.Add, "Sumar un Vinko", enabled = value < MAX_VINKOS) { onChange((value + 1).coerceAtMost(MAX_VINKOS)) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.padding(6.dp).size(46.dp).background(Vinko.Tint, RoundedCornerShape(11.dp))) {
        Icon(icon, contentDescription = description, tint = if (enabled) Vinko.Primary else Vinko.Lilac)
    }
}

// ---------------------------------------------------------------------------------------------
// Confirmación de compra

@Composable
private fun ConfirmPurchaseSheet(purchase: Purchase, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf<JsonObject?>(null) }
    val c = purchase.currency

    WalletSheet(onDismiss = { if (!busy) onDismiss() }) { close ->
        val result = done
        if (result != null) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).background(Vinko.MintBg, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("¡Compra realizada!", style = MaterialTheme.typography.titleLarge)
                Text("+${vinkosText(purchase.vinkos.toLong())}", style = MaterialTheme.typography.headlineMedium, color = Vinko.Primary)
                Spacer(Modifier.height(8.dp))
                Text(
                    buildAnnotatedString {
                        append("Pagaste ${money(result.l("debited_minor") ?: purchase.cost, c.code)}. Ahora tienes ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Vinko.Ink)) { append(vinkosText(result.l("credits") ?: 0)) }
                        append(" disponibles.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Vinko.Muted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Listo", onClick = close)
            }
        } else {
            Text("Confirma tu compra", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CoinBadge(36.dp)
                Spacer(Modifier.width(12.dp))
                Text(vinkosText(purchase.vinkos.toLong()), style = MaterialTheme.typography.headlineMedium, color = Vinko.Primary)
            }
            Spacer(Modifier.height(12.dp))
            SummaryRow("Se descontará de tu saldo", money(purchase.cost, c.code))
            SummaryRow("Tasa aplicada", "1 Vinko = ${money(purchase.rate, c.code)}")
            SummaryRow("Comisión", "Gratis", valueColor = Vinko.Mint)
            Spacer(Modifier.height(8.dp))
            Text(
                "Los Vinkos no son reembolsables a dinero. Se usan para contrataciones y beneficios en SoyVinko.",
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Muted,
            )
            error?.let {
                Spacer(Modifier.height(12.dp))
                Banner(it)
            }
            Spacer(Modifier.height(20.dp))
            SheetActions(
                primary = "Confirmar compra",
                loading = busy,
                secondary = "Cancelar",
                onSecondary = close,
                onPrimary = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            done = friendly {
                                app.ops.call(
                                    "convert_money_to_vinkos",
                                    "currency" to c.code,
                                    "vinkos" to purchase.vinkos,
                                    "key" to purchase.key,
                                ).obj() ?: JsonObject(emptyMap())
                            }
                            onDone()
                        } catch (e: ApiException) {
                            error = e.message
                        } finally {
                            busy = false
                        }
                    }
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Recargas

@Composable
private fun TopupSheet(
    request: TopupFor,
    sandbox: Boolean,
    onDismiss: () -> Unit,
    onCredited: (String) -> Unit,
    onPending: () -> Unit,
) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val c = request.currency
    var amount by remember { mutableStateOf(request.suggestedMinor?.let(::rawFromMinor) ?: "") }
    val minor = parseMinor(amount) ?: 0L
    val valid = minor >= 100
    // Una clave por monto: reintentar el mismo monto no duplica la recarga.
    val key = remember(minor) { newKey() }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(false) }
    val quick = if (c == Cur.VES) listOf(5_000L, 10_000L, 25_000L, 50_000L) else listOf(10L, 25L, 50L, 100L)

    var payWithCrypto by remember { mutableStateOf(c == Cur.USD) }
    var cryptoNetwork by remember { mutableStateOf(CryptoNetwork.TRC20) }
    var txHash by remember { mutableStateOf("") }
    var copiedAddress by remember { mutableStateOf(false) }

    WalletSheet(onDismiss = { if (!busy) onDismiss() }) { close ->
        if (pending) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).background(Vinko.AmberBg, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Vinko.Amber, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("Recarga registrada", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (payWithCrypto && c == Cur.USD)
                        "Tu recarga de ${money(minor, c.code)} en USDT (${cryptoNetwork.displayName}) quedó registrada. Se acreditará en cuanto se verifique la transacción en blockchain."
                    else
                        "Tu recarga de ${money(minor, c.code)} quedó pendiente. La acreditaremos en cuanto se confirme el pago.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Vinko.Muted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Entendido", onClick = close)
            }
        } else {
            Text("Recargar saldo en ${c.lower}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                if (request.suggestedMinor != null) "Te sugerimos recargar lo que te falta para tu compra de Vinkos."
                else "El saldo recargado queda disponible para comprar Vinkos o pagar servicios.",
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Muted,
            )
            Spacer(Modifier.height(16.dp))

            if (c == Cur.USD) {
                Segmented(
                    options = listOf(true to "Cripto (USDT)", false to "Internacional"),
                    selected = payWithCrypto,
                    onSelect = { payWithCrypto = it },
                )
                Spacer(Modifier.height(14.dp))
            }

            AmountField(
                value = amount,
                onValueChange = { amount = it; error = null },
                currency = c,
                label = "Monto a recargar",
                supporting = if (amount.isNotEmpty() && !valid) "El monto mínimo es ${money(100, c.code)}." else null,
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quick.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { n ->
                            QuickChip(
                                "${c.symbol} ${count(n)}",
                                selected = minor == n * 100,
                                onClick = { amount = n.toString(); error = null },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            if (c == Cur.USD && payWithCrypto) {
                Spacer(Modifier.height(14.dp))
                FieldLabel("Red de depósito USDT")
                Dropdown(
                    label = "Red de criptomoneda",
                    options = CryptoNetwork.entries,
                    selected = cryptoNetwork,
                    text = { it.displayName },
                    onSelect = {
                        cryptoNetwork = it
                        copiedAddress = false
                    },
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (cryptoNetwork == CryptoNetwork.BINANCE_PAY) "Binance Pay ID Oficial" else "Dirección oficial de depósito", fontSize = 12.sp, color = Vinko.Muted)
                        Text("1 USD = 1.00 USDT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Vinko.Mint)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            cryptoNetwork.depositAddress,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(cryptoNetwork.depositAddress))
                                copiedAddress = true
                            },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar", tint = if (copiedAddress) Vinko.Mint else Vinko.Primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (copiedAddress) {
                        Text("¡Dirección copiada al portapapeles!", fontSize = 11.sp, color = Vinko.Mint, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedTextField(
                        value = txHash,
                        onValueChange = { txHash = it.trim(); error = null },
                        label = { Text("Hash TxID o comprobante") },
                        placeholder = { Text("Pega el hash de la transacción", color = Vinko.Subtle) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (sandbox) {
                Spacer(Modifier.height(14.dp))
                Banner("Portal de demostración: la recarga se simula y no mueve dinero real.", BannerTone.Info)
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Banner(it)
            }
            Spacer(Modifier.height(20.dp))
            SheetActions(
                primary = if (valid) "Recargar ${money(minor, c.code)}" else "Recargar",
                loading = busy,
                enabled = valid,
                secondary = "Cancelar",
                onSecondary = close,
                onPrimary = {
                    if (c == Cur.USD && payWithCrypto && txHash.isNotBlank()) {
                        val hashErr = validateTxHash(cryptoNetwork, txHash)
                        if (hashErr != null) {
                            error = hashErr
                            return@SheetActions
                        }
                    }
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val id = friendly {
                                app.ops.call("create_topup", "currency" to c.code, "amount" to minor, "key" to key).str()
                            }
                            if (sandbox) {
                                friendly { app.api.raw("POST", "/api/v1/integrations/sandbox/topup/$id/settle") }
                                onCredited("Recarga de ${money(minor, c.code)} acreditada (modo de prueba).")
                                close()
                            } else {
                                pending = true
                                onPending()
                            }
                        } catch (e: ApiException) {
                            error = e.message
                        } finally {
                            busy = false
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun PendingTopups(items: List<JsonObject>, sandbox: Boolean, onSettled: () -> Unit) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var busyId by remember { mutableStateOf<String?>(null) }
    SectionLabel("Recargas en proceso")
    VCard(padding = 14.dp) {
        items.forEachIndexed { i, t ->
            if (i > 0) Divider()
            MoveRow(
                credit = true,
                title = "Recarga de ${money(t.l("amount_minor"), t.s("currency"))}",
                subtitle = "${date(t.s("created_at"))} · " + if (sandbox) "Pago de prueba sin confirmar" else "Esperando confirmación del pago",
                amount = "",
                leading = { IconTile(Icons.Outlined.Schedule, tint = Vinko.Amber, bg = Vinko.AmberBg, size = 38.dp) },
                trailing = {
                    if (sandbox) {
                        SmallButton("Simular pago", loading = busyId == t.s("id"), onClick = {
                            val id = t.s("id") ?: return@SmallButton
                            busyId = id
                            scope.launch {
                                try {
                                    friendly { app.api.raw("POST", "/api/v1/integrations/sandbox/topup/$id/settle") }
                                    onSettled()
                                    snackbar.showSnackbar("Recarga acreditada (modo de prueba).")
                                } catch (e: ApiException) {
                                    snackbar.showSnackbar(e.message)
                                } finally {
                                    busyId = null
                                }
                            }
                        })
                    } else {
                        StatusBadge(t.s("status"))
                    }
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Información

@Composable
private fun Shortcut(icon: ImageVector, title: String, subtitle: String, last: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Vinko.Lilac, modifier = Modifier.size(20.dp))
    }
    if (!last) Divider(Modifier.padding(horizontal = 10.dp))
}

private val PILLARS = listOf(
    Triple(Icons.Outlined.AutoAwesome, "1 Vinko por contratación", "Cada contratación aceptada o servicio acordado descuenta 1 Vinko. Sin comisiones bancarias ni cobros ocultos."),
    Triple(Icons.Outlined.VerifiedUser, "Garantía y respaldo", "Tus Vinkos respaldan solicitudes protegidas con verificación de identidad, soporte prioritario y mediación de SoyVinko."),
    Triple(Icons.AutoMirrored.Outlined.TrendingUp, "Presencia y posicionamiento", "Los paquetes incluyen beneficios como días de tienda en el marketplace y posicionamiento preferencial."),
)

@Composable
private fun Pillars() {
    SectionLabel("Por qué usar Vinkos")
    VCard {
        PILLARS.forEachIndexed { i, (icon, title, text) ->
            if (i > 0) Divider(Modifier.padding(vertical = 12.dp))
            Row {
                IconTile(icon)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(text, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
            }
        }
    }
}

private val INFO_STEPS = listOf(
    Triple(Icons.Outlined.AccountBalanceWallet, "Moneda oficial unificada", "Los Vinkos son el activo interno de SoyVinko: clientes y profesionales gestionan sus acuerdos sin complicaciones de divisas."),
    Triple(Icons.Outlined.Bolt, "1 Vinko = 1 contratación", "Cada vez que inicias o aceptas una contratación directa se descuenta 1 Vinko de tu saldo disponible."),
    Triple(Icons.Outlined.Shield, "Protección integral", "Las contrataciones con Vinkos cuentan con verificación de identidad, valoraciones reales y mediación de SoyVinko."),
    Triple(Icons.Outlined.WorkspacePremium, "Paquetes y beneficios extra", "Los paquetes oficiales suman presencia en el buscador, insignias destacadas y días de vitrina en el marketplace."),
)

@Composable
private fun VinkoInfoSheet(onDismiss: () -> Unit) {
    WalletSheet(onDismiss = onDismiss) { close ->
        Text("¿Qué son los Vinkos?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        INFO_STEPS.forEach { (icon, title, text) ->
            Row(Modifier.padding(bottom = 16.dp)) {
                IconTile(icon)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(text, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
            }
        }
        PrimaryButton("Entendido", onClick = close)
    }
}
