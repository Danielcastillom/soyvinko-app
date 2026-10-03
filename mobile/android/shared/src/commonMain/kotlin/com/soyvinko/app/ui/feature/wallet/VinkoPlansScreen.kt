package com.soyvinko.app.ui.feature.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ResourceView
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
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private data class StartedPurchase(val id: String, val plan: JsonObject)

/** Beneficios de un plan en frases cortas. */
private fun planBenefits(p: JsonObject): List<String> = buildList {
    val credits = p.l("credits") ?: 0
    if (credits > 0) add("${vinkosText(credits)} oficiales incluidos")
    (p.l("positioning_days") ?: 0).takeIf { it > 0 }?.let { add("$it días de posicionamiento destacado") }
    (p.l("marketplace_days") ?: 0).takeIf { it > 0 }?.let { add("$it días de tienda en el marketplace") }
    (p.l("max_products") ?: 0).takeIf { it > 0 }?.let { add("Catálogo de hasta $it productos") }
    p.s("fee_summary")?.takeIf { it.isNotBlank() }?.let { add("Comisión: $it") }
    p.s("benefits_summary")?.split("\n", "\\n")?.map { it.trim().removePrefix("•").trim() }?.filter { it.isNotEmpty() }?.let(::addAll)
}.distinct()

/** Planes y paquetes oficiales de Vinkos (sección "planes-vinkos" de pages/Wallet.tsx). */
@Composable
fun VinkoPlansScreen(onBack: () -> Unit) {
    val app = LocalApp.current
    val user = LocalUser.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    val config = rememberResource { app.api.raw("GET", "/api/v1/config", auth = false).obj() ?: JsonObject(emptyMap()) }
    val plans = rememberResource { app.ops.call("active_plans").objects() }
    val purchases = rememberResource { app.ops.call("my_credit_purchases").objects() }
    val credits = rememberResource { app.ops.call("my_wallet").obj() ?: JsonObject(emptyMap()) }
    val sandbox = config.data.s("payment_mode") == "sandbox"
    fun refresh() {
        credits.reload(); purchases.reload(); plans.reload()
    }

    var audience by rememberSaveable { mutableStateOf(if (user.isCompany) "store" else "provider") }
    var started by remember { mutableStateOf<StartedPurchase?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }

    fun settle(id: String, credits: Long, after: () -> Unit = {}) {
        busyId = id
        scope.launch {
            try {
                friendly { app.api.raw("POST", "/api/v1/integrations/sandbox/purchase/$id/settle") }
                refresh()
                after()
                snackbar.showSnackbar(
                    if (credits > 0) "¡Paquete acreditado! Se sumaron ${vinkosText(credits)} a tu saldo." else "¡Plan activado en tu cuenta!",
                )
            } catch (e: ApiException) {
                snackbar.showSnackbar(e.message)
            } finally {
                busyId = null
            }
        }
    }

    ScreenScaffold("Paquetes de Vinkos", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoinBadge(36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Vinkos disponibles", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
                    Text(credits.data?.let { vinkosText(it.l("balance") ?: it.l("credits") ?: 0) } ?: "—", style = MaterialTheme.typography.titleLarge, color = Vinko.Primary)
                }
            }
            Text(
                "Elige el paquete que mejor se adapte a tu volumen de contrataciones y potencia tu alcance en SoyVinko.",
                style = MaterialTheme.typography.bodyMedium,
                color = Vinko.Muted,
            )

            val pending = purchases.data.orEmpty().filter { it.s("status") == "pending" }
            if (pending.isNotEmpty()) {
                SectionLabel("Paquetes en proceso")
                VCard(padding = 14.dp) {
                    pending.forEachIndexed { i, p ->
                        if (i > 0) Divider()
                        MoveRow(
                            credit = true,
                            title = if ((p.l("credits") ?: 0) > 0) vinkosText(p.l("credits") ?: 0) else "Activación de plan",
                            subtitle = "Paquete registrado el ${date(p.s("created_at"))}",
                            amount = "",
                            leading = { IconTile(Icons.Outlined.Schedule, tint = Vinko.Amber, bg = Vinko.AmberBg, size = 38.dp) },
                            trailing = {
                                if (sandbox) {
                                    SmallButton("Simular activación", loading = busyId == p.s("id"), onClick = {
                                        p.s("id")?.let { settle(it, p.l("credits") ?: 0) }
                                    })
                                } else {
                                    StatusBadge("pending")
                                }
                            },
                        )
                    }
                }
            }

            Segmented(
                options = listOf("provider" to "Profesionales", "store" to "Comercios"),
                selected = audience,
                onSelect = { audience = it },
                icons = { if (it == "provider") Icons.Outlined.Person else Icons.Outlined.Storefront },
            )

            ResourceView(plans) { all ->
                val shown = all.filter { (it.s("affiliate_type") ?: "provider") == audience }
                if (shown.isEmpty()) {
                    EmptyBox("No hay planes disponibles", "Vuelve pronto: publicaremos nuevos paquetes de Vinkos.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        shown.forEach { p ->
                            PlanCard(p, busy = busyId == p.s("id")) {
                                val id = p.s("id") ?: return@PlanCard
                                busyId = id
                                scope.launch {
                                    try {
                                        val purchase = friendly { app.ops.call("start_purchase", "plan" to id, "key" to newKey()).str() }
                                        started = StartedPurchase(purchase, p)
                                        purchases.reload()
                                    } catch (e: ApiException) {
                                        snackbar.showSnackbar(e.message)
                                    } finally {
                                        busyId = null
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    started?.let { s ->
        val creditsOf = s.plan.l("credits") ?: 0
        WalletSheet(onDismiss = { started = null }) { close ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                CoinBadge(60.dp)
                Spacer(Modifier.height(12.dp))
                Text(s.plan.s("name").orEmpty(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                if (creditsOf > 0) Text("+${vinkosText(creditsOf)}", style = MaterialTheme.typography.headlineMedium, color = Vinko.Primary)
            }
            Spacer(Modifier.height(14.dp))
            SummaryBox {
                Text("Beneficios incluidos", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 6.dp))
                planBenefits(s.plan).take(5).forEach { Benefit(it) }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(16.dp))
            if (sandbox) {
                Banner("Portal de demostración: la activación se simula y no mueve dinero real.", BannerTone.Info)
                Spacer(Modifier.height(16.dp))
                SheetActions(
                    primary = if (creditsOf > 0) "Confirmar y activar ${vinkosText(creditsOf)}" else "Confirmar y activar plan",
                    loading = busyId == s.id,
                    onPrimary = { settle(s.id, creditsOf, after = close) },
                    secondary = "Más tarde",
                    onSecondary = close,
                )
            } else {
                Banner("El pedido fue iniciado. Acreditaremos los Vinkos en cuanto se valide el pago.", BannerTone.Info)
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Entendido", onClick = close)
            }
        }
    }
}

@Composable
private fun PlanCard(p: JsonObject, busy: Boolean, onChoose: () -> Unit) {
    val credits = p.l("credits") ?: 0
    val price = p.l("price_minor") ?: 0
    VCard(padding = 18.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("PAQUETE OFICIAL", style = MaterialTheme.typography.labelSmall, color = Vinko.Primary)
                Spacer(Modifier.height(4.dp))
                Text(p.s("name").orEmpty(), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                if (price > 0) money(price, p.s("currency")) else "Gratis",
                style = MaterialTheme.typography.titleLarge,
                color = if (price > 0) Vinko.Ink else Vinko.Mint,
            )
        }
        if (credits > 0) {
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().background(Vinko.Cream, RoundedCornerShape(14.dp)).border(1.dp, Vinko.Line, RoundedCornerShape(14.dp)).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("INCLUYE", style = MaterialTheme.typography.labelSmall, color = Vinko.Primary)
                Text("+${vinkosText(credits)}", style = MaterialTheme.typography.headlineMedium, color = Vinko.Primary)
                Text("Para contrataciones y acuerdos", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        planBenefits(p).filterNot { credits > 0 && it.endsWith("oficiales incluidos") }.forEach { Benefit(it) }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(if (credits > 0) "Elegir plan (+${count(credits)} Vinkos)" else "Elegir plan", onClick = onChoose, loading = busy)
    }
}

@Composable
private fun Benefit(text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 2.dp).size(18.dp).background(Vinko.MintBg, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Vinko.Ink)
    }
}
