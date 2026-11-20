package com.soyvinko.app.ui.feature.pro

import coil3.compose.LocalPlatformContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.ui.common.AppMode
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalAppMode
import com.soyvinko.app.ui.common.LocalOpenDrawer
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.PollWhileVisible
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.StatusBadge
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.dateTime
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.relative
import com.soyvinko.app.ui.common.rememberAction
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val ACTIVE_ORDER = setOf("pending", "accepted", "dispatched")

@Composable
fun OrdersScreen(nav: NavHostController) {
    val app = LocalApp.current
    val user = LocalUser.current
    val orders = rememberResource(ProSignals.orders) { app.ops.call("my_orders") }
    val catalog = rememberResource { app.ops.call("marketplace", "limit" to 100) }
    val storeNames = catalog.data.objects().associate { it.s("store_id") to it.s("store_name") }
    var filter by rememberSaveable { mutableStateOf("all") }

    // Actualiza periódicamente las órdenes mientras la pantalla está en foco
    PollWhileVisible(15_000) {
        orders.reload()
    }

    val mode = LocalAppMode.current
    val openDrawer = LocalOpenDrawer.current

    ScreenScaffold(
        "Consulta de órdenes",
        onBack = if (mode == AppMode.SELLER) null else { { nav.popBackStack() } },
        navigationIcon = if (mode == AppMode.SELLER) {
            {
                IconButton(onClick = openDrawer) {
                    Icon(Icons.Outlined.Menu, contentDescription = "Menú", tint = Vinko.Ink)
                }
            }
        } else null,
        actions = {
            IconButton(onClick = orders.reload) { Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar", tint = Vinko.Ink) }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ResourceView(orders) { raw ->
                val all = raw.objects()
                val list = when (filter) {
                    "buy" -> all.filter { it.s("customer_id") == user.id }
                    "sell" -> all.filter { it.s("customer_id") != user.id }
                    "active" -> all.filter { it.s("status") in ACTIVE_ORDER }
                    "done" -> all.filter { it.s("status") !in ACTIVE_ORDER }
                    else -> all
                }
                val sales = all.count { it.s("customer_id") != user.id }
                Column {
                    ChoiceChips(
                        buildList {
                            add("all" to "Todos (${all.size})")
                            if (sales > 0) {
                                add("buy" to "Compras")
                                add("sell" to "Ventas ($sales)")
                            }
                            add("active" to "En curso")
                            add("done" to "Finalizados")
                        },
                        filter,
                        { filter = it },
                        Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                    if (list.isEmpty()) {
                        EmptyBox(
                            if (all.isEmpty()) "Aún no tienes pedidos" else "Sin pedidos en este filtro",
                            if (all.isEmpty()) "Cuando compres o vendas productos en el Marketplace, los verás aquí con su seguimiento." else "Prueba con otro filtro.",
                            if (all.isEmpty()) "Explorar productos" else null,
                        ) { nav.navigate(Routes.MARKETPLACE) }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(list, key = { it.s("id") ?: it.toString() }) { o ->
                                OrderCard(o, user.id, storeNames[o.s("store_id")], onChanged = orders.reload)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun paymentLabel(status: String?, method: String? = null): Pair<String, Color> = when {
    method == "wallet" || status == "paid" -> "Pagado con Saldo de Wallet" to Vinko.Mint
    status == "refunded" -> "Reembolsado a tu Wallet" to Vinko.Mint
    status == "buyer_reported" -> "Pago reportado por el comprador" to Vinko.Amber
    status == "seller_confirmed" -> "Pago confirmado por el vendedor" to Vinko.Mint
    status == "disputed" -> "Pago en disputa" to Vinko.Coral
    else -> "Pago por acordar directamente" to Vinko.Muted
}

/** Tarjeta de pedido con seguimiento y las acciones que permite advance_order según el rol. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderCard(o: JsonObject, me: String, storeName: String?, onChanged: () -> Unit) {
    val app = LocalApp.current
    val action = rememberAction()
    val form = rememberFormAction()
    val id = o.s("id").orEmpty()
    val buyer = o.s("customer_id") == me
    val status = o.s("status")
    val currency = o.s("currency")?.trim()
    val paymentMethod = o.s("payment_method")
    val paymentStatus = o.s("payment_status")
    val isPaid = paymentStatus == "paid" || paymentStatus == "seller_confirmed"
    var payWalletConfirm by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var sharing by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    var delivery by remember { mutableStateOf<JsonObject?>(null) }
    var deliveryLoaded by remember { mutableStateOf(false) }
    var showPayments by remember { mutableStateOf(false) }

    val advance: (String, String) -> Unit = { act, message ->
        action.run(message) {
            app.ops.call("advance_order", "order" to id, "action" to act)
            onChanged()
        }
    }

    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (buyer) Pill("Mi compra", icon = Icons.Outlined.ShoppingBag) else Pill("Mi venta", Vinko.Mint, Vinko.MintBg, Icons.Outlined.Sell)
            Spacer(Modifier.weight(1f))
            StatusBadge(status)
        }
        Spacer(Modifier.height(10.dp))
        Text("Pedido #${id.take(8).uppercase()}", style = MaterialTheme.typography.titleMedium)
        Text(
            listOfNotNull(storeName?.takeIf { buyer }?.let { "Tienda $it" }, dateTime(o.s("created_at"))).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Muted,
        )
        Spacer(Modifier.height(14.dp))
        when (status) {
            "cancelled" -> Notice(Icons.Outlined.Cancel, "Pedido cancelado. Las unidades volvieron al inventario.", Vinko.Coral, Vinko.CoralBg)
            "disputed" -> Notice(Icons.Outlined.ErrorOutline, "Pedido en disputa. Soporte está revisando el caso.", Vinko.Amber, Vinko.AmberBg)
            else -> OrderStepper(status)
        }
        Spacer(Modifier.height(14.dp))
        o.list("items").forEach { item ->
            val qty = item.l("quantity") ?: 0L
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.s("name_snapshot") ?: "Producto",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "$qty × ${money(item.l("unit_price_minor"), currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Vinko.Muted,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(money(item.l("subtotal_minor") ?: ((item.l("unit_price_minor") ?: 0L) * qty), currency), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(6.dp))
        LineDivider()
        Spacer(Modifier.height(6.dp))
        KeyValue("Total acordado", money(o.l("total_minor"), currency), strong = true, valueColor = Vinko.Primary)
        if (status != "cancelled" && status != "pending") {
            val (label, color) = paymentLabel(paymentStatus, paymentMethod)
            IconLine(if (isPaid) Icons.Outlined.CheckCircle else Icons.Outlined.Payments, label, tint = color, color = color)
        }
        if (paymentMethod == "wallet" && isPaid) {
            Spacer(Modifier.height(8.dp))
            Notice(
                Icons.Outlined.AccountBalanceWallet,
                "Pagado con Saldo de Wallet SoyVinko. Transacción garantizada.",
                Vinko.Mint,
                Vinko.MintBg
            )
        }

        val hint = when {
            buyer && status == "pending" -> "Esperando que el vendedor acepte tu pedido."
            buyer && status == "accepted" -> "Comparte tus datos de entrega y acuerda el pago con el vendedor."
            buyer && status == "dispatched" -> "El vendedor envió tu pedido. Confirma la recepción cuando lo tengas."
            !buyer && status == "pending" -> "Acepta el pedido para coordinar la entrega o cancélalo si no puedes atenderlo."
            !buyer && status == "accepted" -> "Revisa los datos de entrega y marca el pedido como enviado."
            !buyer && status == "dispatched" -> "Esperando que el comprador confirme la recepción."
            else -> null
        }
        hint?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }

        val buttons = buildList<@Composable () -> Unit> {
            if (buyer && !isPaid && (status == "pending" || status == "accepted")) {
                add {
                    SmallButton(
                        "Pagar con Wallet",
                        onClick = { payWalletConfirm = true },
                        icon = Icons.Outlined.AccountBalanceWallet,
                        tone = Tone.Primary,
                        enabled = !action.busy
                    )
                }
            }
            if (!buyer && status == "pending") add { SmallButton("Aceptar pedido", onClick = { advance("accept", "Pedido aceptado.") }, icon = Icons.Outlined.Check, loading = action.busy) }
            if (!buyer && status == "accepted") add { SmallButton("Marcar como enviado", onClick = { advance("dispatch", "Pedido marcado como enviado.") }, icon = Icons.Outlined.LocalShipping, loading = action.busy) }
            if (buyer && (status == "accepted" || status == "dispatched")) add { SmallButton("Confirmar recepción", onClick = { confirm = "receive" }, icon = Icons.Outlined.CheckCircle, enabled = !action.busy) }
            if (buyer && status == "accepted") add { SmallButton("Datos de entrega", onClick = { form.reset(); sharing = true }, icon = Icons.Outlined.LocalShipping, tone = Tone.Outline) }
            if (status == "accepted" || status == "dispatched" || status == "delivered") add {
                SmallButton("Ver entrega", onClick = {
                    action.run(null) {
                        delivery = app.api.raw("GET", "/api/v1/integrations/orders/$id/delivery").obj()
                        deliveryLoaded = true
                    }
                }, icon = Icons.Outlined.LocationOn, tone = Tone.Outline, enabled = !action.busy)
            }
            if (buyer && !isPaid && (status == "accepted" || status == "dispatched" || status == "delivered")) add {
                SmallButton("Reportar pago", onClick = { form.reset(); reporting = true }, icon = Icons.Outlined.Payments, tone = Tone.Outline)
            }
            if (status == "accepted" || status == "dispatched" || status == "delivered") add {
                SmallButton(if (showPayments) "Ocultar pagos" else "Pagos reportados", onClick = { showPayments = !showPayments }, tone = Tone.Soft)
            }
            if (status == "pending" || status == "accepted") add {
                SmallButton("Cancelar", onClick = { confirm = "cancel" }, tone = Tone.Danger, enabled = !action.busy)
            }
        }
        if (buttons.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                buttons.forEach { it() }
            }
        }
        if (showPayments) {
            Spacer(Modifier.height(12.dp))
            PaymentReports(id, me)
        }
    }

    if (payWalletConfirm) {
        ConfirmDialog(
            "¿Pagar con saldo de tu Wallet?",
            "Se debitarán ${money(o.l("total_minor"), currency)} de tu wallet para pagar al vendedor de forma instantánea y segura. El pedido quedará marcado como pagado.",
            "Pagar ahora",
            onConfirm = {
                payWalletConfirm = false
                action.run("¡Pedido pagado con éxito con saldo de tu Wallet!") {
                    app.ops.call("pay_order_with_wallet", "order" to id)
                    ProSignals.wallet++
                    onChanged()
                }
            },
            onDismiss = { payWalletConfirm = false },
        )
    }

    when (confirm) {
        "receive" -> ConfirmDialog(
            "¿Recibiste tu pedido?",
            "Confirma solo si recibiste los productos a satisfacción. El pedido quedará como entregado.",
            "Sí, lo recibí",
            onConfirm = { advance("receive", "¡Listo! Pedido entregado.") },
            onDismiss = { confirm = null },
        )
        "cancel" -> ConfirmDialog(
            "¿Cancelar el pedido?",
            "Las unidades reservadas volverán al inventario de la tienda. Esta acción no se puede deshacer.",
            "Cancelar pedido",
            danger = true,
            onConfirm = { advance("cancel", "Pedido cancelado.") },
            onDismiss = { confirm = null },
        )
    }

    if (deliveryLoaded) {
        DeliveryDialog(delivery, buyer) {
            deliveryLoaded = false
            delivery = null
        }
    }
    if (sharing) DeliverySheet(id, form, onDismiss = { sharing = false }, onSaved = { sharing = false })
    if (reporting) PaymentSheet(o, form, onDismiss = { reporting = false }, onSaved = { reporting = false; showPayments = true; onChanged() })
}

@Composable
private fun Notice(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, fg: Color, bg: Color) {
    Row(Modifier.fillMaxWidth().background(bg, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = fg)
    }
}

@Composable
private fun OrderStepper(status: String?) {
    val reached = when (status) {
        "accepted" -> 1
        "dispatched" -> 2
        "delivered" -> 3
        else -> 0
    }
    val labels = listOf("Solicitado", "Aceptado", "En camino", "Entregado")
    Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, label ->
            val done = i <= reached
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(2.dp).background(if (i == 0) Color.Transparent else if (done) Vinko.Mint else Vinko.Line))
                    Box(
                        Modifier
                            .size(26.dp)
                            .background(if (done) Vinko.Mint else Color.White, CircleShape)
                            .border(1.5.dp, if (done) Vinko.Mint else Vinko.Line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        else Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = Vinko.Subtle)
                    }
                    Box(Modifier.weight(1f).height(2.dp).background(if (i == labels.lastIndex) Color.Transparent else if (i < reached) Vinko.Mint else Vinko.Line))
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color = if (done) Vinko.Ink else Vinko.Subtle, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DeliveryDialog(d: JsonObject?, buyer: Boolean, onDismiss: () -> Unit) {
    val context = LocalPlatformContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Datos de entrega", style = MaterialTheme.typography.titleLarge) },
        text = {
            if (d == null) {
                Text(
                    if (buyer) "Aún no compartes tus datos de entrega. Usa «Datos de entrega» para enviarlos al vendedor."
                    else "El comprador aún no comparte sus datos de entrega.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Vinko.Muted,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconLine(Icons.Outlined.Person, d.s("recipient_name") ?: "No especificado", tint = Vinko.Primary, color = Vinko.Ink)
                    d.s("phone")?.let { phone ->
                        Row(
                            Modifier.clickable { dial(context, phone) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Phone, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(phone, style = MaterialTheme.typography.bodyMedium, color = Vinko.Primary)
                        }
                    }
                    IconLine(Icons.Outlined.LocationOn, d.s("address") ?: "Pendiente por coordinar", tint = Vinko.Primary, color = Vinko.Ink)
                    Text("Compartido ${relative(d.s("buyer_shared_at")).replaceFirstChar { it.lowercase() }}", style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar", color = Vinko.Primary) } },
    )
}

@Composable
private fun DeliverySheet(orderId: String, form: FormAction, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val app = LocalApp.current
    val user = LocalUser.current
    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    FormSheet(
        "Datos de entrega",
        onDismiss,
        subtitle = "Solo el vendedor de este pedido podrá verlos. La dirección se guarda cifrada.",
        error = form.error,
    ) {
        VinkoTextField(name, { name = it.take(120) }, "Nombre de quien recibe", capitalization = KeyboardCapitalization.Words,
            error = if (tried && name.trim().length < 2) "Escribe el nombre de quien recibe." else null)
        VinkoTextField(phone, { phone = it.take(40) }, "Teléfono de contacto", keyboardType = KeyboardType.Phone,
            error = if (tried && phone.count(Char::isDigit) < 7) "Escribe un teléfono válido, por ejemplo 0414 1234567." else null)
        AreaField(address, { address = it.take(1000) }, "Dirección o punto de encuentro",
            placeholder = "Urbanización, calle, edificio, referencia o punto de encuentro acordado.",
            error = if (tried && address.trim().length < 5) "Escribe la dirección (al menos 5 caracteres)." else null)
        PrimaryButton("Compartir con el vendedor", loading = form.busy, onClick = {
            tried = true
            if (name.trim().length < 2 || phone.count(Char::isDigit) < 7 || address.trim().length < 5) return@PrimaryButton
            form.run("Datos de entrega compartidos.", onSuccess = onSaved) {
                app.api.raw(
                    "POST",
                    "/api/v1/integrations/orders/$orderId/delivery",
                    JsonObject(mapOf("name" to JsonPrimitive(name.trim()), "phone" to JsonPrimitive(phone.trim()), "address" to JsonPrimitive(address.trim()))),
                )
            }
        })
    }
}

private val PAYMENT_METHODS = listOf("Pago Móvil", "Transferencia bancaria", "Efectivo en divisas", "Zelle", "Punto de venta")

@Composable
private fun PaymentSheet(o: JsonObject, form: FormAction, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val app = LocalApp.current
    val currency = o.s("currency")?.trim()
    var amount by remember { mutableStateOf(majorText(o.l("total_minor"))) }
    var method by remember { mutableStateOf(PAYMENT_METHODS.first()) }
    val minor = parseMajor(amount)
    FormSheet(
        "Reportar pago",
        onDismiss,
        subtitle = "Informa el pago que hiciste directamente al vendedor. Él confirmará que lo recibió.",
        error = form.error,
    ) {
        MoneyField(amount, { amount = it }, currency, "Importe pagado", error = if (minor == null || minor <= 0) "Escribe el importe pagado." else null)
        Text("Medio de pago", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
        ChoiceChips(PAYMENT_METHODS.map { it to it }, method, { method = it })
        PrimaryButton("Reportar pago", loading = form.busy, enabled = minor != null && minor > 0, onClick = {
            form.run("Pago reportado. El vendedor debe confirmarlo.", onSuccess = onSaved) {
                app.ops.call(
                    "report_external_payment",
                    "contract" to JsonNull,
                    "order" to o.s("id"),
                    "amount" to minor,
                    "currency" to currency,
                    "method" to method,
                )
            }
        })
    }
}

@Composable
private fun PaymentReports(orderId: String, me: String) {
    val app = LocalApp.current
    val action = rememberAction()
    val data = rememberResource(orderId) { app.ops.call("external_payment_reports", "order" to orderId) }
    val list = data.data.objects()
    when {
        data.data == null && data.loading -> LoadingBox()
        data.data == null -> Text(data.error ?: "No pudimos cargar los pagos.", style = MaterialTheme.typography.bodySmall, color = Vinko.Coral)
        list.isEmpty() -> Text("Aún no hay pagos reportados para este pedido.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        else -> Column(
            Modifier.fillMaxWidth().background(Vinko.Cream, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            list.forEach { r ->
                val confirmed = r.s("confirmed_at") != null
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(money(r.l("amount_minor"), r.s("currency")?.trim()), style = MaterialTheme.typography.titleSmall)
                        Text("${r.s("method")} · ${relative(r.s("reported_at"))}", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                    }
                    if (confirmed) Pill("Confirmado", Vinko.Mint, Vinko.MintBg) else Pill("Por confirmar", Vinko.Amber, Vinko.AmberBg)
                }
                if (!confirmed && r.s("reported_by") != me) {
                    SmallButton("Confirmar que lo recibí", onClick = {
                        action.run("Pago confirmado.") {
                            app.ops.call("confirm_external_payment", "report" to r.s("id"))
                            data.reload()
                        }
                    }, icon = Icons.Outlined.CheckCircle, tone = Tone.Soft, loading = action.busy)
                }
            }
        }
    }
}
