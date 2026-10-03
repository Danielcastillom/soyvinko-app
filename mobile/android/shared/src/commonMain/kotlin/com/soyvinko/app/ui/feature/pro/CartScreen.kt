package com.soyvinko.app.ui.feature.pro

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko

/** Carrito y confirmación del pedido (create_order con clave de idempotencia). */
@Composable
fun CartScreen(nav: NavHostController) {
    val app = LocalApp.current
    val form = rememberFormAction()
    val lines = MarketCart.lines
    val storeId = MarketCart.storeId
    val currency = MarketCart.currency

    ScreenScaffold(
        "Tu carrito",
        onBack = { nav.popBackStack() },
        bottomBar = {
            if (lines.isNotEmpty() && storeId != null) {
                Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
                    LineDivider()
                    PrimaryButton(
                        "Confirmar pedido · ${money(MarketCart.total, currency)}",
                        loading = form.busy,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        onClick = {
                            val items = lines.map { mapOf("product_id" to it.productId, "quantity" to it.qty) }
                            val key = MarketCart.checkoutKey
                            form.run(
                                "Pedido enviado. Te avisaremos cuando el vendedor lo acepte.",
                                onSuccess = {
                                    MarketCart.clear()
                                    ProSignals.orders++
                                    nav.navigate(Routes.ORDERS) { popUpTo(CART) { inclusive = true } }
                                },
                            ) {
                                app.ops.call("create_order", "store" to storeId, "items" to items, "key" to key)
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
                    KeyValue("Total a pagar al vendedor", money(MarketCart.total, currency), strong = true, valueColor = Vinko.Primary)
                }

                VCard {
                    Text("¿Cómo sigue tu compra?", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    Step(1, "Confirmas el pedido y las unidades quedan apartadas.")
                    Step(2, "El vendedor acepta tu pedido.")
                    Step(3, "Compartes tus datos de entrega y acuerdan el pago directo (Pago Móvil, transferencia, efectivo o Zelle).")
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
