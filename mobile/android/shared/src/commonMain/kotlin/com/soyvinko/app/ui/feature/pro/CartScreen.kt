package com.soyvinko.app.ui.feature.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonPrimitive

/** Carrito y confirmación del pedido con selección de método de pago (Wallet o Acordar con vendedor). */
@Composable
fun CartScreen(nav: NavHostController) {
    val app = LocalApp.current
    val form = rememberFormAction()
    val lines = MarketCart.lines
    val storeId = MarketCart.storeId
    val currency = MarketCart.currency ?: "USD"

    val walletRes = rememberResource(currency, ProSignals.wallet) {
        app.ops.call("money_wallet", "currency" to currency)
    }
    val availableMinor = walletRes.data.obj()?.l("available_minor") ?: 0L
    val hasEnoughBalance = availableMinor >= MarketCart.total
    var paymentMethod by rememberSaveable { mutableStateOf("wallet") } // "wallet" o "agreement"

    val isWallet = paymentMethod == "wallet"
    val canSubmit = if (isWallet) hasEnoughBalance else true
    val buttonTitle = when {
        isWallet && hasEnoughBalance -> "Pagar con Wallet · ${money(MarketCart.total, currency)}"
        isWallet && !hasEnoughBalance -> "Saldo insuficiente en Wallet"
        else -> "Confirmar pedido · ${money(MarketCart.total, currency)}"
    }

    ScreenScaffold(
        "Tu carrito",
        onBack = { nav.popBackStack() },
        bottomBar = {
            if (lines.isNotEmpty() && storeId != null) {
                Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
                    LineDivider()
                    PrimaryButton(
                        buttonTitle,
                        loading = form.busy,
                        enabled = canSubmit && !form.busy,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        onClick = {
                            val items = lines.map { mapOf("product_id" to it.productId, "quantity" to it.qty) }
                            val key = MarketCart.checkoutKey
                            val successMsg = if (isWallet) {
                                "¡Pedido pagado con éxito con saldo de tu Wallet!"
                            } else {
                                "Pedido enviado. Te avisaremos cuando el vendedor lo acepte."
                            }
                            form.run(
                                successMsg,
                                onSuccess = {
                                    MarketCart.clear()
                                    ProSignals.orders++
                                    ProSignals.wallet++
                                    nav.navigate(Routes.ORDERS) { popUpTo(CART) { inclusive = true } }
                                },
                            ) {
                                val orderRes = app.ops.call("create_order", "store" to storeId, "items" to items, "key" to key)
                                val orderId = (orderRes as? JsonPrimitive)?.content ?: orderRes.toString().trim('"')
                                if (isWallet && orderId.isNotBlank()) {
                                    app.ops.call("pay_order_with_wallet", "order" to orderId, "key" to key)
                                }
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (lines.isEmpty() || storeId == null) {
                EmptyBox(
                    "Tu carrito está vacío",
                    "Agrega productos de una tienda del Marketplace para hacer tu pedido.",
                    "Explorar productos",
                ) { nav.navigate(Routes.MARKETPLACE) { popUpTo(CART) { inclusive = true } } }
                return@Box
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                form.error?.let { Banner(it) }
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Outlined.Storefront)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Pedido a ${MarketCart.storeName}", style = MaterialTheme.typography.titleSmall)
                            Text("Cada pedido es para una sola tienda.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                        }
                        SmallButton("Ver tienda", onClick = { nav.navigate(Routes.store(storeId)) }, tone = Tone.Soft)
                    }
                }

                VCard(padding = 12.dp) {
                    lines.forEachIndexed { i, line ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProductImage(line.image, line.productId, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), iconSize = 24.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(line.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${money(line.unitMinor, line.currency)} c/u", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                                Spacer(Modifier.height(6.dp))
                                QuantityStepper(line.qty, { MarketCart.setQty(line.productId, it) }, max = line.stock)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                RemoveButton { MarketCart.setQty(line.productId, 0) }
                                Spacer(Modifier.height(10.dp))
                                Text(money(line.unitMinor * line.qty, line.currency), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        if (i < lines.lastIndex) LineDivider()
                    }
                }

                VCard {
                    val units = MarketCart.count
                    KeyValue("Subtotal ($units ${if (units == 1) "unidad" else "unidades"})", money(MarketCart.total, currency))
                    KeyValue("Comisión de la plataforma", "Gratis (0%)", valueColor = Vinko.Mint)
                    Spacer(Modifier.height(6.dp))
                    LineDivider()
                    Spacer(Modifier.height(6.dp))
                    KeyValue("Total a pagar", money(MarketCart.total, currency), strong = true, valueColor = Vinko.Primary)
                }

                // Selector de Método de Pago
                VCard {
                    Text("Método de pago", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))

                    // Opción 1: Saldo de Wallet
                    val isWalletSelected = paymentMethod == "wallet"
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isWalletSelected) 2.dp else 1.dp,
                                color = if (isWalletSelected) Vinko.Primary else Vinko.Line,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(if (isWalletSelected) Vinko.Tint.copy(alpha = 0.5f) else Color.White)
                            .clickable { paymentMethod = "wallet" }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(22.dp)
                                    .border(2.dp, if (isWalletSelected) Vinko.Primary else Vinko.Muted, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isWalletSelected) {
                                    Box(Modifier.size(12.dp).background(Vinko.Primary, CircleShape))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            IconTile(Icons.Outlined.AccountBalanceWallet, tint = Vinko.Primary, bg = Vinko.Tint)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Saldo de mi Wallet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        Modifier
                                            .background(Vinko.MintBg, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Recomendado", style = MaterialTheme.typography.labelSmall, color = Vinko.Mint, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Disponible: ${money(availableMinor, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (hasEnoughBalance) Vinko.Mint else Vinko.Coral
                                )
                            }
                        }

                        if (isWalletSelected) {
                            Spacer(Modifier.height(10.dp))
                            if (hasEnoughBalance) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(Vinko.MintBg, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Saldo suficiente. El pago se descontará de forma instantánea y segura.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Vinko.Mint,
                                    )
                                }
                            } else {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(Vinko.CoralBg, RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        "Saldo insuficiente para pagar este pedido (${money(MarketCart.total, currency)}). Te faltan ${money(MarketCart.total - availableMinor, currency)}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Vinko.Coral,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    SmallButton(
                                        "Recargar saldo de Wallet",
                                        onClick = { nav.navigate(Routes.WALLET) },
                                        tone = Tone.Soft,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Opción 2: Acordar con el vendedor
                    val isAgreementSelected = paymentMethod == "agreement"
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isAgreementSelected) 2.dp else 1.dp,
                                color = if (isAgreementSelected) Vinko.Primary else Vinko.Line,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(if (isAgreementSelected) Vinko.Tint.copy(alpha = 0.5f) else Color.White)
                            .clickable { paymentMethod = "agreement" }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(22.dp)
                                    .border(2.dp, if (isAgreementSelected) Vinko.Primary else Vinko.Muted, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isAgreementSelected) {
                                    Box(Modifier.size(12.dp).background(Vinko.Primary, CircleShape))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            IconTile(Icons.Outlined.Handshake, tint = Vinko.Ink, bg = Vinko.LightGray)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Acordar con el vendedor", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text("Pago Móvil, Efectivo, Transferencia o Zelle", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                            }
                        }
                        if (isAgreementSelected) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Coordinarás los detalles y cuentas bancarias directamente con el comercio una vez acepte el pedido.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Vinko.Muted,
                            )
                        }
                    }
                }

                VCard {
                    Text("¿Cómo sigue tu compra?", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    Step(1, "Confirmas el pedido y las unidades quedan apartadas.")
                    Step(2, "El vendedor acepta tu pedido.")
                    Step(
                        3,
                        if (isWallet) {
                            "Tu pedido queda 100% pagado y protegido con el saldo de tu Wallet."
                        } else {
                            "Compartes tus datos de entrega y acuerdan el pago directo (Pago Móvil, efectivo o Zelle)."
                        }
                    )
                    Step(4, "Recibes los productos y confirmas la recepción en Mis pedidos.", last = true)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun Step(n: Int, text: String, last: Boolean = false) {
    Row(Modifier.padding(bottom = if (last) 0.dp else 10.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(24.dp).background(Vinko.Tint, CircleShape), contentAlignment = Alignment.Center) {
            Text("$n", style = MaterialTheme.typography.labelMedium, color = Vinko.Primary)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted, modifier = Modifier.padding(top = 2.dp))
    }
}
