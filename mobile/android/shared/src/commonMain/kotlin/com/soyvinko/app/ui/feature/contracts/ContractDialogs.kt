package com.soyvinko.app.ui.feature.contracts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.common.ApiImage
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberCameraCapture
import com.soyvinko.app.ui.common.rememberPhotoPicker
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.time
import com.soyvinko.app.ui.common.uploadFile
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/** Diálogos abiertos desde el detalle del contrato. */
sealed interface Dlg {
    data class Confirm(
        val title: String,
        val text: String,
        val label: String,
        val success: String?,
        val destructive: Boolean = false,
        val action: suspend () -> Unit,
    ) : Dlg

    data class Reason(
        val title: String,
        val text: String,
        val label: String,
        val minChars: Int,
        val success: String?,
        val destructive: Boolean = false,
        val action: suspend (String) -> Unit,
    ) : Dlg

    data object Propose : Dlg
    data class Fund(val quote: JsonObject) : Dlg
    data object Delivery : Dlg
    data class Rate(val rating: Int, val recommends: Boolean, val comment: String) : Dlg
    data object ExternalPay : Dlg
    data class Photo(val fileId: String) : Dlg
}

@Composable
fun ConfirmDialog(d: Dlg.Confirm, onDismiss: () -> Unit, onDone: () -> Unit) {
    val op = rememberOp()
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        d.title, d.label, op.busy, onDismiss,
        onConfirm = { error = null; op.run(d.success, { error = it }, after = onDone, block = d.action) },
        error = error, destructive = d.destructive,
    ) { Text(d.text, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted) }
}

@Composable
fun ReasonDialog(d: Dlg.Reason, onDismiss: () -> Unit, onDone: () -> Unit) {
    val op = rememberOp()
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val short = reason.trim().length < d.minChars
    FormDialog(
        d.title, d.label, op.busy, onDismiss,
        onConfirm = { error = null; op.run(d.success, { error = it }, after = onDone) { d.action(reason.trim()) } },
        error = error, destructive = d.destructive, confirmEnabled = !short,
    ) {
        Text(d.text, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
        MultilineField(
            reason, { reason = it }, "Motivo",
            supporting = "Mínimo ${d.minChars} caracteres · ${reason.trim().length}",
            maxLength = 2000,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choices(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val on = o.equals(selected, ignoreCase = true)
            Text(
                o,
                style = MaterialTheme.typography.labelMedium,
                color = if (on) Color.White else Vinko.Ink,
                modifier = Modifier
                    .background(if (on) Vinko.Primary else Color.White, RoundedCornerShape(50))
                    .border(1.dp, if (on) Vinko.Primary else Vinko.Line, RoundedCornerShape(50))
                    .clickable { onSelect(o) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

private val METHODS = listOf("Efectivo", "Pago Móvil", "Transferencia", "Divisas en efectivo", "Zelle")

/** propose_terms: nuevo precio, forma de pago y alcance. La otra parte debe aceptarlas. */
@Composable
fun ProposeTermsDialog(ctx: ContractCtx, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    val op = rememberOp()
    var amount by remember { mutableStateOf(minorToInput(ctx.price)) }
    var method by remember { mutableStateOf(ctx.c.s("payment_method")?.takeUnless { ctx.guaranteed || it == "platform_wallet" } ?: "Efectivo") }
    var terms by remember { mutableStateOf(ctx.c.s("payment_terms").orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val minor = parseAmountMinor(amount)
    FormDialog(
        "Proponer nuevas condiciones", "Enviar propuesta", op.busy, onDismiss,
        onConfirm = {
            error = null
            op.run("Propuesta enviada. ${ctx.counterpart} debe aceptarla.", { error = it }, after = onDone) {
                app.ops.call(
                    "propose_terms",
                    "contract" to ctx.id,
                    "price" to minor,
                    // Igual que la web: con pago protegido la forma de pago es siempre la wallet.
                    "method" to if (ctx.guaranteed) "wallet" else method.trim(),
                    "terms" to terms.trim(),
                )
            }
        },
        error = error,
        confirmEnabled = minor != null && terms.isNotBlank() && (ctx.guaranteed || method.isNotBlank()),
    ) {
        Text(
            "Tu propuesta queda aceptada por ti. Las condiciones cambian cuando ${ctx.counterpart} también las acepte.",
            style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted,
        )
        AmountField(amount, { amount = it }, "Importe", ctx.currency, error = if (amount.isNotBlank() && minor == null) "Escribe un importe mayor que cero." else null)
        if (ctx.guaranteed) {
            Banner("Forma de pago: saldo SoyVinko en custodia (pago protegido).", BannerTone.Info)
        } else {
            Text("Forma de pago", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
            Choices(METHODS, method) { method = it }
            VinkoTextField(method, { method = it.take(80) }, "Forma de pago (o escribe otra)")
        }
        MultilineField(terms, { terms = it }, "Condiciones y alcance", supporting = "Describe qué incluye el trabajo, plazos y forma de pago.")
    }
}

/** quote_contract_funding + fund_contract: reserva el saldo de la wallet en custodia (clave de idempotencia por cotización). */
@Composable
fun FundDialog(ctx: ContractCtx, quote: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit, onTopUp: () -> Unit) {
    val app = LocalApp.current
    val op = rememberOp()
    val currency = quote.s("currency") ?: ctx.currency
    val wallet = rememberResource(currency) { app.ops.call("money_wallet", "currency" to currency).obj() }
    val key = remember(quote) { newKey() }
    var error by remember { mutableStateOf<String?>(null) }
    val price = quote.l("price_minor") ?: 0L
    val available = wallet.data?.l("available_minor")
    val insufficient = available != null && available < price
    FormDialog(
        "Reservar el pago del servicio", "Confirmar reserva", op.busy, onDismiss,
        onConfirm = {
            error = null
            op.run("Pago reservado en custodia.", { error = it }, after = onDone) {
                app.ops.call("fund_contract", "quote" to quote.s("id"), "key" to key)
            }
        },
        error = error,
        confirmEnabled = !insufficient && !wallet.loading,
    ) {
        Column {
            KeyValue("Total a reservar", money(price, currency), strong = true)
            KeyValue("Comisión de servicio", money(quote.l("fee_minor") ?: 0L, currency))
            KeyValue("Recibirá ${ctx.c.s("professional_name") ?: "el profesional"}", money(quote.l("professional_receives_minor") ?: price, currency))
            Divider()
            KeyValue(
                "Tu saldo disponible",
                when {
                    available != null -> money(available, currency)
                    wallet.loading -> "Consultando…"
                    else -> "No disponible"
                },
            )
        }
        if (insufficient) {
            Banner("Tu saldo disponible no alcanza para reservar este pago. Recarga tu wallet y vuelve a intentarlo.")
            TextButton(onClick = onTopUp) { Text("Ir a mi wallet", color = Vinko.Primary, style = MaterialTheme.typography.labelLarge) }
        }
        Banner(
            (quote.s("expires_at")?.let { "Cotización válida hasta las ${time(it)} " } ?: "") +
                "Este importe dejará de estar disponible en tu wallet y solo se libera cuando valides la entrega.",
            BannerTone.Info,
        )
    }
}

private class Evidence(val id: String, val approved: Boolean)

/** submit_delivery: resumen + evidencias (subidas con purpose "evidence" o archivos propios ya aprobados). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeliveryDialog(ctx: ContractCtx, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    val op = rememberOp()
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableIntStateOf(0) }
    val selected = remember { mutableStateListOf<Evidence>() }
    val key = remember { newKey() }
    val approved = rememberResource {
        app.ops.call("my_files").objects().filter {
            it.s("purpose") in setOf("chat", "evidence", "portfolio") && it.s("moderation_status") == "approved" && it.s("mime_type")?.startsWith("image/") != false
        }
    }
    fun upload(parts: List<UploadPart>) = parts.forEach { part ->
        scope.launch {
            uploading++
            try {
                selected += Evidence(app.uploadFile(part, "evidence"), approved = false)
            } catch (e: ApiException) {
                error = friendlyError(e)
            } finally {
                uploading--
            }
        }
    }
    val pick = rememberPhotoPicker(max = 6) { upload(it) }
    val camera = rememberCameraCapture { upload(listOf(it)) }

    FormDialog(
        "Presentar entrega", "Enviar entrega", op.busy || uploading > 0, onDismiss,
        onConfirm = {
            error = null
            op.run("Entrega enviada. ${ctx.counterpart} la revisará.", { error = it }, after = onDone) {
                app.ops.call(
                    "submit_delivery",
                    "contract" to ctx.id,
                    "summary" to summary.trim(),
                    "key" to key,
                    "files" to selected.map { it.id },
                )
            }
        },
        error = error,
        confirmEnabled = summary.trim().length >= 10 && selected.size <= 20,
    ) {
        Text(
            "Describe el trabajo concluido. ${ctx.counterpart} podrá validarlo y liberar el pago, o pedir correcciones.",
            style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted,
        )
        MultilineField(summary, { summary = it }, "Trabajo realizado", supporting = "Mínimo 10 caracteres · ${summary.trim().length}", maxLength = 10_000)
        Text("Evidencias (opcional)", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineAction("Galería", Icons.Outlined.AddPhotoAlternate, pick, Modifier.weight(1f))
            OutlineAction("Cámara", Icons.Outlined.PhotoCamera, camera, Modifier.weight(1f))
        }
        if (uploading > 0) Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Vinko.Primary)
            Spacer(Modifier.width(8.dp))
            Text("Subiendo foto…", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
        if (selected.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            selected.forEach { ev ->
                Box {
                    ApiImage(ev.id, Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)))
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(3.dp).size(22.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape)
                            .clickable { selected.remove(ev) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Close, contentDescription = "Quitar", tint = Color.White, modifier = Modifier.size(14.dp)) }
                    if (!ev.approved) Text(
                        "En revisión",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Vinko.Amber.copy(alpha = 0.85f)).padding(2.dp),
                    )
                }
            }
        }
        if (selected.any { !it.approved }) Text(
            "Las fotos nuevas pasan por una revisión de seguridad. Si aún no están aprobadas, el servidor rechazará la entrega: quítalas o espera la aprobación.",
            style = MaterialTheme.typography.bodySmall, color = Vinko.Amber,
        )
        val pool = approved.data.orEmpty().filter { f -> selected.none { it.id == f.s("id") } }
        if (pool.isNotEmpty()) {
            Text("Tus archivos aprobados", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pool.take(20).forEach { f ->
                    ApiImage(
                        f.s("id"),
                        Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, Vinko.Line, RoundedCornerShape(10.dp))
                            .clickable { f.s("id")?.let { selected += Evidence(it, approved = true) } },
                    )
                }
            }
        }
    }
}

private val RATING_TAGS = listOf("Puntual", "Muy profesional", "Excelente atención", "Buen precio", "Trabajo limpio", "100% Recomendado")

/** rate_contract: 1 a 5 estrellas, recomendación y comentario (se publica de inmediato). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RateDialog(ctx: ContractCtx, initial: Dlg.Rate, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    val op = rememberOp()
    var rating by remember { mutableIntStateOf(initial.rating) }
    var recommends by remember { mutableStateOf(initial.recommends) }
    var comment by remember { mutableStateOf(initial.comment) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(
        if (initial.rating > 0) "Editar tu calificación" else "Calificar a ${ctx.counterpart}",
        "Publicar calificación", op.busy, onDismiss,
        onConfirm = {
            error = null
            op.run("¡Gracias! Tu calificación ya está publicada.", { error = it }, after = onDone) {
                app.ops.call(
                    "rate_contract",
                    "contract" to ctx.id,
                    "rating" to rating,
                    "recommends" to recommends,
                    "comment" to (comment.trim().ifEmpty { null } ?: JsonNull),
                )
            }
        },
        error = error,
        confirmEnabled = rating in 1..5,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StarPicker(rating, size = 44.dp) { rating = it }
            Spacer(Modifier.height(8.dp))
            Text(
                when (rating) {
                    1 -> "⭐ Muy insatisfecho"
                    2 -> "⭐⭐ Necesita mejorar"
                    3 -> "⭐⭐⭐ Regular"
                    4 -> "⭐⭐⭐⭐ Buen servicio"
                    5 -> "⭐⭐⭐⭐⭐ ¡Excelente trabajo!"
                    else -> "Toca las estrellas para calificar"
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (rating == 0) Vinko.Muted else Vinko.Primary,
            )
        }

        Spacer(Modifier.height(4.dp))
        Text("Aspectos destacados", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RATING_TAGS.forEach { tag ->
                val included = comment.contains(tag, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (included) Vinko.Tint else Color.White)
                        .border(1.dp, if (included) Vinko.Primary else Vinko.Line, RoundedCornerShape(50))
                        .clickable {
                            comment = if (included) {
                                comment.replace(tag, "").replace("  ", " ").trim()
                            } else {
                                if (comment.isBlank()) tag else "$comment · $tag"
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (included) Vinko.Primary else Vinko.Ink,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Vinko.Cream)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Recomiendo este profesional", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(recommends, { recommends = it }, colors = SwitchDefaults.colors(checkedTrackColor = Vinko.Primary))
        }

        MultilineField(
            comment,
            { comment = it },
            "Cuéntanos tu experiencia",
            supporting = "Opcional · se mostrará en el perfil público del profesional",
            maxLength = 4000,
        )
    }
}

@Composable
fun StarPicker(rating: Int, size: androidx.compose.ui.unit.Dp = 40.dp, onPick: ((Int) -> Unit)? = null) {
    Row {
        (1..5).forEach { s ->
            val on = s <= rating
            val mod = if (onPick != null) Modifier.size(size + 4.dp).clip(CircleShape).clickable { onPick(s) } else Modifier.size(size)
            Box(mod, contentAlignment = Alignment.Center) {
                Icon(
                    if (on) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = if (onPick != null) "$s ${if (s == 1) "estrella" else "estrellas"}" else null,
                    tint = if (on) Color(0xFFF59E0B) else Vinko.Lilac,
                    modifier = Modifier.size(size),
                )
            }
        }
    }
}

/** report_external_payment: registra un pago hecho fuera de la plataforma; la otra parte lo confirma. */
@Composable
fun ExternalPaymentDialog(ctx: ContractCtx, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    val op = rememberOp()
    var amount by remember { mutableStateOf(minorToInput(ctx.price)) }
    var method by remember { mutableStateOf("Efectivo") }
    var error by remember { mutableStateOf<String?>(null) }
    val minor = parseAmountMinor(amount)
    FormDialog(
        "Registrar pago realizado", "Registrar pago", op.busy, onDismiss,
        onConfirm = {
            error = null
            op.run("Pago registrado. ${ctx.counterpart} debe confirmar la recepción.", { error = it }, after = onDone) {
                app.ops.call(
                    "report_external_payment",
                    "contract" to ctx.id,
                    "order" to JsonNull, // obligatorio en la API: nulo cuando es un contrato
                    "amount" to minor,
                    "currency" to ctx.currency,
                    "method" to method.trim(),
                )
            }
        },
        error = error,
        confirmEnabled = minor != null && method.isNotBlank(),
    ) {
        Text(
            "El pago se hace directamente entre ustedes, sin comisiones. Regístralo para dejar constancia; ${ctx.counterpart} confirmará que lo recibió.",
            style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted,
        )
        AmountField(amount, { amount = it }, "Importe pagado", ctx.currency, error = if (amount.isNotBlank() && minor == null) "Escribe un importe mayor que cero." else null)
        Text("Medio de pago", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
        Choices(METHODS, method) { method = it }
        VinkoTextField(method, { method = it.take(80) }, "Medio de pago (o escribe otro)")
    }
}

@Composable
fun CheckLine(text: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = if (ok) Vinko.Mint else Vinko.Lilac, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = if (ok) Vinko.Mint else Vinko.Muted)
    }
}
