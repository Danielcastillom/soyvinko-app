package com.soyvinko.app.ui.feature.wallet

import com.soyvinko.app.ui.common.formatNumber
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.StatusBadge
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.time
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/** Retiros: cuentas de destino, cotización con comisión, confirmación e historial. */
@Composable
fun WithdrawScreen(onBack: () -> Unit, onAddMethod: () -> Unit, notice: String? = null, onNoticeShown: () -> Unit = {}) {
    val app = LocalApp.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    val methods = rememberResource { app.ops.call("my_payout_methods").objects() }
    val withdrawals = rememberResource { app.ops.call("my_withdrawals").objects() }
    val ves = rememberResource { app.ops.call("money_wallet", "currency" to "VES").obj() }
    val usd = rememberResource { app.ops.call("money_wallet", "currency" to "USD").obj() }
    val wallets = mapOf(Cur.VES to ves.data, Cur.USD to usd.data)
    fun refresh() {
        methods.reload(); withdrawals.reload(); ves.reload(); usd.reload()
    }
    OnReturn { refresh() }
    LaunchedEffect(notice) {
        // El aviso se muestra en el scope de la pantalla: limpiar el aviso reinicia este efecto.
        notice?.let {
            scope.launch { snackbar.showSnackbar(it) }
            onNoticeShown()
        }
    }

    val active = methods.data.orEmpty().filter { it.s("status") != "disabled" }
    val verified = active.filter { it.s("status") == "verified" }

    var disabling by remember { mutableStateOf<JsonObject?>(null) }
    var cancelling by remember { mutableStateOf<JsonObject?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun run(success: String, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                friendly { block() }
                refresh()
                snackbar.showSnackbar(success)
            } catch (e: ApiException) {
                snackbar.showSnackbar(e.message)
            } finally {
                busy = false
            }
        }
    }

    ScreenScaffold("Retirar saldo", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Banner("La única comisión de SoyVinko se cobra al retirar. Verás el monto exacto antes de confirmar.", BannerTone.Info)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Cur.entries.forEach { c ->
                    VCard(Modifier.weight(1f), padding = 14.dp) {
                        Text("Disponible en ${c.symbol}", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
                        Text(
                            wallets[c]?.let { money(it.l("available_minor") ?: 0, c.code) } ?: "—",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val held = wallets[c].l("withdrawal_pending_minor") ?: 0
                        if (held > 0) Text("${money(held, c.code)} en retiro", style = MaterialTheme.typography.bodySmall, color = Vinko.Amber)
                    }
                }
            }

            // Cuentas de destino
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { SectionLabel("Cuentas de retiro") }
                TextButton(onClick = onAddMethod) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir cuenta", color = Vinko.Primary, style = MaterialTheme.typography.labelLarge)
                }
            }
            when {
                methods.data == null && methods.loading -> LoadingBox()
                methods.data == null -> Banner(methods.error ?: "No pudimos cargar tus cuentas.")
                active.isEmpty() -> VCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Outlined.AccountBalance)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Añade una cuenta bancaria o de Pago Móvil a tu nombre para poder retirar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Vinko.Muted,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Añadir cuenta de retiro", onClick = onAddMethod)
                }
                else -> VCard(padding = 14.dp) {
                    active.forEachIndexed { i, m ->
                        if (i > 0) Divider()
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconTile(if (m.s("provider") == "pago_movil") Icons.Outlined.PhoneAndroid else Icons.Outlined.AccountBalance, size = 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.s("label") ?: "Cuenta", style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${m.s("masked_account").orEmpty()} · ${m.s("currency").orEmpty()}", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                                Spacer(Modifier.height(4.dp))
                                StatusBadge(m.s("status"))
                            }
                            IconButton(onClick = { disabling = m }, enabled = !busy) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Desactivar cuenta", tint = Vinko.Muted)
                            }
                        }
                    }
                }
            }

            // Nuevo retiro
            if (verified.isNotEmpty()) {
                SectionLabel("Nuevo retiro")
                WithdrawForm(verified, wallets, onDone = { msg ->
                    refresh()
                    scope.launch { snackbar.showSnackbar(msg) }
                })
            } else if (active.isNotEmpty()) {
                Banner("Tus cuentas aún no están verificadas. Podrás retirar en cuanto una quede verificada.", BannerTone.Info)
            }

            // Historial
            val list = withdrawals.data.orEmpty()
            if (list.isNotEmpty()) {
                SectionLabel("Historial de retiros")
                VCard(padding = 14.dp) {
                    list.forEachIndexed { i, w ->
                        if (i > 0) Divider()
                        val currency = w.s("currency")
                        MoveRow(
                            credit = false,
                            title = "Retiro de ${money(w.l("amount_minor"), currency)}",
                            subtitle = "${date(w.s("created_at"))} · Recibes ${money(w.l("net_minor"), currency)} · ${w.s("destination_label").orEmpty()}",
                            amount = "",
                            leading = { IconTile(Icons.Outlined.Schedule, size = 38.dp) },
                            trailing = {
                                StatusBadge(w.s("status"))
                                if (w.s("status") == "pending") {
                                    Text(
                                        "Cancelar",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Vinko.Coral,
                                        modifier = Modifier.padding(top = 6.dp).clickable(enabled = !busy) { cancelling = w }.padding(4.dp),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    disabling?.let { m ->
        AlertDialog(
            onDismissRequest = { disabling = null },
            containerColor = Color.White,
            title = { Text("¿Desactivar esta cuenta?") },
            text = { Text("${m.s("label").orEmpty()} (${m.s("masked_account").orEmpty()}) dejará de estar disponible para retiros. Los retiros ya solicitados no se ven afectados.") },
            confirmButton = {
                TextButton(onClick = {
                    disabling = null
                    run("Cuenta desactivada.") { app.ops.call("disable_payout_method", "method" to m.s("id")) }
                }) { Text("Desactivar", color = Vinko.Coral) }
            },
            dismissButton = { TextButton(onClick = { disabling = null }) { Text("Volver", color = Vinko.Ink) } },
        )
    }
    cancelling?.let { w ->
        AlertDialog(
            onDismissRequest = { cancelling = null },
            containerColor = Color.White,
            title = { Text("¿Cancelar este retiro?") },
            text = { Text("El monto de ${money(w.l("amount_minor"), w.s("currency"))} volverá a tu saldo disponible.") },
            confirmButton = {
                TextButton(onClick = {
                    cancelling = null
                    run("Retiro cancelado; el saldo volvió a tu cuenta.") { app.ops.call("cancel_withdrawal", "withdrawal" to w.s("id")) }
                }) { Text("Cancelar retiro", color = Vinko.Coral) }
            },
            dismissButton = { TextButton(onClick = { cancelling = null }) { Text("Volver", color = Vinko.Ink) } },
        )
    }
}

@Composable
private fun WithdrawForm(methods: List<JsonObject>, wallets: Map<Cur, JsonObject?>, onDone: (String) -> Unit) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var methodId by remember { mutableStateOf(methods.first().s("id")) }
    LaunchedEffect(methods) { if (methods.none { it.s("id") == methodId }) methodId = methods.first().s("id") }
    val selected = methods.firstOrNull { it.s("id") == methodId } ?: methods.first()
    val currency = Cur.of(selected.s("currency"))
    val available = wallets[currency].l("available_minor") ?: 0L

    var amount by remember { mutableStateOf("") }
    val minor = parseMinor(amount) ?: 0L
    var quote by remember { mutableStateOf<JsonObject?>(null) }
    var key by remember { mutableStateOf(newKey()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun run(block: suspend () -> Unit) {
        busy = true
        error = null
        scope.launch {
            try {
                friendly { block() }
            } catch (e: ApiException) {
                if (e.code == "withdrawal_quote_expired") quote = null
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    VCard(modifier = Modifier.testTag("withdraw_form"), padding = 18.dp) {
        FieldLabel("Cuenta de destino")
        methods.forEach { m ->
            val on = m.s("id") == methodId
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(if (on) Vinko.Tint else Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, if (on) Vinko.Primary else Vinko.Line, RoundedCornerShape(14.dp))
                    .clickable(enabled = !busy) {
                        methodId = m.s("id")
                        quote = null
                        error = null
                    }
                    .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = on, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Vinko.Primary), modifier = Modifier.padding(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.s("label") ?: "Cuenta", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${m.s("masked_account").orEmpty()} · ${m.s("currency").orEmpty()}", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        AmountField(
            value = amount,
            onValueChange = {
                amount = it
                quote = null
                error = null
            },
            currency = currency,
            label = "Monto a retirar",
            supporting = "Disponible: ${money(available, currency.code)}",
            error = if (minor > available) "Supera tu saldo disponible." else null,
            trailing = {
                TextButton(onClick = {
                    amount = rawFromMinor(available)
                    quote = null
                }, enabled = available > 0) { Text("Todo", color = Vinko.Primary) }
            },
        )

        val q = quote
        if (q != null) {
            Spacer(Modifier.height(12.dp))
            SummaryBox {
                SummaryRow("Monto", money(q.l("amount_minor"), currency.code))
                val pct = formatNumber((q.l("basis_points") ?: 0) / 100.0, 2, trim = true)
                SummaryRow("Comisión de retiro ($pct %)", "−" + money(q.l("fee_minor"), currency.code), valueColor = Vinko.Coral)
                Divider(Modifier.padding(vertical = 4.dp))
                SummaryRow("Recibirás", money(q.l("net_minor"), currency.code), strong = true, valueColor = Vinko.Mint)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Vinko.Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Cotización válida hasta las ${time(q.s("expires_at"))} · Se enviará a ${selected.s("label").orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Muted,
                )
            }
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Banner(it)
        }
        Spacer(Modifier.height(16.dp))
        if (q != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Cambiar", onClick = { quote = null }, modifier = Modifier.weight(1f), enabled = !busy)
                PrimaryButton(
                    "Confirmar retiro",
                    loading = busy,
                    modifier = Modifier.weight(1.4f),
                    onClick = {
                        run {
                            app.ops.call("request_withdrawal", "quote" to q.s("id"), "key" to key)
                            quote = null
                            amount = ""
                            onDone("Retiro solicitado. Te avisaremos cuando se procese.")
                        }
                    },
                )
            }
        } else {
            PrimaryButton(
                "Ver cuánto recibo",
                loading = busy,
                enabled = minor > 0 && minor <= available,
                onClick = {
                    run {
                        quote = app.ops.call("quote_withdrawal", "currency" to currency.code, "method" to methodId, "amount" to minor).obj()
                        key = newKey()
                    }
                },
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}
