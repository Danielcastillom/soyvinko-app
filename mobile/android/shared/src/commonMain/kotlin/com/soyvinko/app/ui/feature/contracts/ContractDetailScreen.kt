package com.soyvinko.app.ui.feature.contracts

import coil3.compose.LocalPlatformContext
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.tracking.LocationTracking
import com.soyvinko.app.tracking.TrackingMode
import com.soyvinko.app.tracking.rememberTrackingStarter
import com.soyvinko.app.ui.common.ErrorBox
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.StatusBadge
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.dateTime
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.shareViaWhatsApp
import com.soyvinko.app.ui.common.statusLabel
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Lectura cómoda del contrato (api.contract_detail) y de la custodia (api.escrow_details) para el usuario actual. */
class ContractCtx(val id: String, val c: JsonObject, val escrow: JsonObject?, val me: String) {
    val status: String? = c.s("status")
    val isCustomer = c.s("customer_id") == me
    val isProfessional = c.s("professional_id") == me
    val guaranteed = c.s("payment_protection") == "guaranteed"
    val onsite = c.s("delivery_mode") != "remote"
    val open = status in OPEN_STATUSES
    val currency: String = c.s("currency")?.trim() ?: "VES"
    val price: Long? = c.l("agreed_price_minor")
    val counterpart: String = (if (isCustomer) c.s("professional_name") else c.s("customer_name")) ?: "la otra parte"
    val escrowStatus: String? = escrow?.s("status")
    val deliveries: List<JsonObject> = escrow.list("deliveries")
    val lastDelivery: JsonObject? = deliveries.maxByOrNull { it.l("version") ?: 0 }
    val terms: List<JsonObject> = c.list("terms").sortedByDescending { it.l("version") ?: 0 }
    val latestTerms: JsonObject? = terms.firstOrNull()
    val myReview: JsonObject? = c.list("reviews").firstOrNull { it.s("author_id") == me }
    val theirReview: JsonObject? = c.list("reviews").firstOrNull { it.s("author_id") != me }
    val myFinishedAt: String? = c.s(if (isCustomer) "customer_finished_at" else "professional_finished_at")

    fun nameOf(userId: String?) = when (userId) {
        me -> "Tú"
        c.s("customer_id") -> c.s("customer_name") ?: "Cliente"
        c.s("professional_id") -> c.s("professional_name") ?: "Profesional"
        else -> "SoyVinko"
    }

    /** Hay condiciones nuevas que este usuario debe aceptar. */
    val termsPendingForMe: Boolean
        get() {
            val t = latestTerms ?: return false
            if (status !in TERMS_STATUSES) return false
            return if (isCustomer) t.s("customer_accepted_at") == null else t.s("professional_accepted_at") == null
        }
}

/** Detalle del servicio: estado, próximos pasos, pagos, entregas, condiciones, contacto y calificación. */
@Composable
fun ContractDetailScreen(id: String, nav: NavHostController) {
    val app = LocalApp.current
    val me = LocalUser.current.id
    val detail = rememberResource(id) {
        app.ops.call("contract_detail", "contract" to id).obj() ?: throw ApiException("Este servicio no está disponible.", "not_found", 404)
    }
    val c = detail.data
    val guaranteed = c?.s("payment_protection") == "guaranteed"
    val external = c?.s("payment_protection") == "external"
    val cancelled = c?.s("status") == "cancelled"
    val escrow = rememberResource(id, guaranteed) { if (guaranteed) app.ops.call("escrow_details", "contract" to id).obj() else null }
    val payments = rememberResource(id, external) { if (external) app.ops.call("external_payment_reports", "contract" to id).objects() else emptyList() }
    val contacts = rememberResource(id, c != null && !cancelled) {
        if (c != null && !cancelled) app.ops.call("contract_contacts", "contract" to id).obj() else null
    }
    val onsiteOpen = c != null && c.s("delivery_mode") != "remote" && c.s("status") in SHARE_STATUSES
    // Solo el estado del permiso (sin coordenadas ni auditoría); el mapa vive en la pantalla de ubicación.
    val location = rememberLocationStatus(id, 15_000, enabled = onsiteOpen)
    val refresh = { detail.reload(); escrow.reload(); payments.reload(); location.reload() }
    PollWhileVisible(15_000, id) { refresh() }

    var dialog by remember { mutableStateOf<Dlg?>(null) }
    val close = { dialog = null }
    val doneAndRefresh = { dialog = null; refresh() }

    Box(Modifier.fillMaxSize()) {
    ScreenScaffold(
        "Detalle del servicio",
        onBack = { nav.popBackStack() },
        actions = {
            if (c != null) IconButton(onClick = { nav.navigate(Routes.chat(id)) }) {
                Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "Abrir chat", tint = Vinko.Primary)
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                c != null -> {
                    val ctx = ContractCtx(id, c, escrow.data, me)
                    DetailContent(ctx, payments.data.orEmpty(), contacts.data, location.data.takeIf { onsiteOpen }, nav, refresh) { dialog = it }
                }
                detail.loading -> LoadingBox()
                else -> ErrorBox(detail.error ?: "Inténtalo de nuevo.", detail.reload)
            }
        }
    }

    val ctx = c?.let { ContractCtx(id, it, escrow.data, me) }
    when (val d = dialog) {
        null -> Unit
        is Dlg.Confirm -> ConfirmDialog(d, close, doneAndRefresh)
        is Dlg.Reason -> ReasonDialog(d, close, doneAndRefresh)
        is Dlg.Photo -> PhotoViewer(d.fileId, close)
        else -> if (ctx != null) when (d) {
            Dlg.Propose -> ProposeTermsDialog(ctx, close, doneAndRefresh)
            is Dlg.Fund -> FundDialog(ctx, d.quote, close, doneAndRefresh) {
                dialog = null
                nav.navigate(Routes.WALLET) {
                    popUpTo(nav.graph.findStartDestination().route.orEmpty()) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            Dlg.Delivery -> DeliveryDialog(ctx, close, doneAndRefresh)
            is Dlg.Rate -> RateDialog(ctx, d, close, doneAndRefresh)
            Dlg.ExternalPay -> ExternalPaymentDialog(ctx, close, doneAndRefresh)
            else -> Unit
        }
    }
    }
}

@Composable
private fun DetailContent(
    ctx: ContractCtx,
    payments: List<JsonObject>,
    contacts: JsonObject?,
    location: LocStatus?,
    nav: NavHostController,
    refresh: () -> Unit,
    open: (Dlg) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeaderCard(ctx)
        when (ctx.status) {
            "cancelled" -> Banner(
                "Servicio cancelado" + (ctx.c.s("cancelled_at")?.let { " el ${date(it)}" } ?: "") +
                    (ctx.c.s("cancellation_reason")?.let { ". Motivo: $it" } ?: "."),
            )
            "disputed" -> Banner("Se reportó un problema en este servicio. Soporte está revisando el caso y te escribirá por tus canales registrados.")
            "completed" -> Banner("Servicio finalizado el ${date(ctx.c.s("completed_at"))}. ¡Gracias por confiar en SoyVinko!", BannerTone.Success)
        }
        if (ctx.status == "completed") {
            val platformContext = LocalPlatformContext.current
            VCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Comprobante Digital Oficial", style = MaterialTheme.typography.titleMedium, color = Vinko.Mint)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Comparte el resumen y comprobante verificado de este servicio con tu cliente o contraparte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Vinko.Muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(
                        text = "Enviar Comprobante por WhatsApp",
                        onClick = {
                            val receipt = """
🧾 *COMPROBANTE DE CONTRATO FINALIZADO*
*SOYVINKO VENEZUELA*
──────────────────────
📌 *Contrato Folio:* #${ctx.id.take(8).uppercase()}
📋 *Estado:* ✅ FINALIZADO CON ÉXITO
🛠️ *Servicio:* ${ctx.c.s("title").orEmpty().ifBlank { "Servicio Profesional" }}
👤 *Cliente:* ${ctx.c.s("customer_name").orEmpty().ifBlank { "Cliente" }}
👷 *Profesional:* ${ctx.c.s("professional_name") ?: ctx.counterpart}
💰 *Monto Acordado:* ${money(ctx.latestTerms.l("amount_minor") ?: 0L, ctx.latestTerms.s("currency") ?: "USD")}
🛡️ *Garantía:* Contrato formalizado y verificado
──────────────────────
✅ _Operación digital respaldada por SoyVinko._
🌐 _https://soyvinko.com_
                            """.trimIndent()
                            shareViaWhatsApp(platformContext, receipt)
                        },
                    )
                }
            }
        }
        if (ctx.termsPendingForMe) Banner(
            "${ctx.nameOf(ctx.latestTerms.s("proposed_by"))} propuso nuevas condiciones. Revísalas en \"Alcance y condiciones\".",
            BannerTone.Info,
        )
        TrackingActiveBanner(ctx.id)
        QuickLinks(ctx, nav)
        NextStepCard(ctx, location, nav, refresh, open)
        ProgressCard(ctx)
        if (ctx.guaranteed) EscrowCard(ctx) else ExternalPaymentCard(ctx, payments, open)
        if (ctx.guaranteed && (ctx.deliveries.isNotEmpty() || ctx.status in OPEN_STATUSES)) DeliveriesCard(ctx, open)
        TermsCard(ctx, open)
        if (ctx.status != "cancelled") ContactsCard(ctx, contacts)
        if (ctx.status == "completed") RatingCard(ctx, open)
        ManageCard(ctx, open)
        HistoryCard(ctx)
    }
}

private val HeroGradient = Brush.linearGradient(listOf(Vinko.Dark, Color(0xFF3B1F8F)))

@Composable
private fun HeaderCard(ctx: ContractCtx) {
    Column(
        Modifier.fillMaxWidth().background(HeroGradient, RoundedCornerShape(22.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("CONTRATO ${folio(ctx.id)}", style = MaterialTheme.typography.labelSmall, color = Vinko.Lilac, maxLines = 1, modifier = Modifier.weight(1f))
            StatusBadge(ctx.status)
        }
        Text(ctx.c.s("title") ?: "Servicio contratado", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        ctx.c.s("offering_title")?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Vinko.Lilac) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Pill(modeLabel(ctx.c.s("delivery_mode")), fg = Color.White, bg = Color.White.copy(alpha = 0.14f), icon = if (ctx.onsite) Icons.Outlined.LocationOn else Icons.Outlined.Language)
            Pill(protectionLabel(ctx.c.s("payment_protection")), fg = Color.White, bg = Color.White.copy(alpha = 0.14f), icon = if (ctx.guaranteed) Icons.Outlined.VerifiedUser else Icons.Outlined.Handshake)
            Pill(money(ctx.price, ctx.currency), fg = Color.White, bg = Color.White.copy(alpha = 0.14f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Party(Modifier.weight(1f), "Cliente", ctx.c.s("customer_name"), Icons.Outlined.Person, ctx.isCustomer)
            Party(Modifier.weight(1f), "Profesional", ctx.c.s("professional_name"), Icons.Outlined.BusinessCenter, ctx.isProfessional)
        }
    }
}

@Composable
private fun Party(modifier: Modifier, role: String, name: String?, icon: ImageVector, me: Boolean) {
    Row(
        modifier.background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Vinko.Lilac, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(if (me) "$role · Tú" else role, style = MaterialTheme.typography.bodySmall, color = Vinko.Lilac)
            Text(name ?: "—", style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 2)
        }
    }
}

@Composable
private fun QuickLinks(ctx: ContractCtx, nav: NavHostController) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlineAction("Chat", Icons.Outlined.Forum, { nav.navigate(Routes.chat(ctx.id)) }, Modifier.weight(1f), tint = Vinko.Primary)
        if (ctx.onsite && ctx.status in SHARE_STATUSES) {
            OutlineAction(
                if (ctx.isCustomer) "Ubicación" else "Ruta y mapa",
                Icons.Outlined.Route,
                { nav.navigate(Routes.tracking(ctx.id)) },
                Modifier.weight(1f),
                tint = Vinko.Primary,
            )
        }
    }
}

/** Acción principal según rol y estado (espejo de la web, con las reglas del SQL). */
@Composable
private fun NextStepCard(ctx: ContractCtx, location: LocStatus?, nav: NavHostController, refresh: () -> Unit, open: (Dlg) -> Unit) {
    val app = LocalApp.current
    val context = LocalPlatformContext.current
    val op = rememberOp()
    val s = ctx.status
    val startTracking = rememberTrackingStarter()
    val now = rememberNow(30_000)
    val shared = location?.active == true
    val tracking = rememberTrackingFor(ctx.id, TrackingMode.PROFESSIONAL)
    fun advance(action: String, success: String, after: () -> Unit = {}) = op.run(success, after = { refresh(); after() }) {
        app.ops.call("advance_contract", "contract" to ctx.id, "action" to action)
    }
    // Sin acción para este rol/estado: no se muestra la tarjeta.
    var title: String? = null
    var text: String? = null
    var content: (@Composable () -> Unit)? = null

    val finishExternal = Dlg.Confirm(
        "Confirmar finalización",
        "¿Confirmas que el servicio concluyó y que el pago acordado en persona (${money(ctx.price, ctx.currency)}) se realizó? " +
            "El servicio finaliza cuando ambas partes lo confirman.",
        "Confirmar", "Confirmación registrada.",
    ) { app.ops.call("advance_contract", "contract" to ctx.id, "action" to "finish") }

    when {
        !ctx.open -> Unit
        ctx.isProfessional && s == "accepted" && ctx.guaranteed && ctx.escrowStatus == "unfunded" -> {
            title = "Esperando la reserva del pago"
            text = "${ctx.counterpart} aún no ha reservado el pago en custodia. Te avisaremos cuando los fondos estén garantizados para comenzar."
        }
        ctx.isProfessional && ctx.onsite && s == "accepted" -> {
            title = "Cuando salgas hacia el lugar"
            text = if (location != null && !shared) "${ctx.counterpart} aún no ha compartido la ubicación. Podrás salir en camino cuando lo haga; te avisaremos aquí."
            else "Marca que vas en camino: ${ctx.counterpart} verá tu trayecto" +
                (if (location?.gps == true) " y tú su ubicación en vivo." else " y tú tendrás la ruta a la dirección del servicio.")
            content = {
                PrimaryButton("Voy en camino", loading = op.busy, enabled = location == null || shared, onClick = {
                    advance("depart", "¡Buen viaje! El cliente verá tu trayecto.") {
                        // Trayecto en segundo plano: directo si ya hay permiso, si no se piden los permisos.
                        val label = ctx.c.s("customer_name").orEmpty()
                        if (Gps.hasPermission(context)) LocationTracking.start(context, ctx.id, TrackingMode.PROFESSIONAL, label)
                        else startTracking(ctx.id, TrackingMode.PROFESSIONAL, label)
                        nav.navigate(Routes.tracking(ctx.id))
                    }
                })
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Ver ubicación del servicio", onClick = { nav.navigate(Routes.tracking(ctx.id)) })
            }
        }
        ctx.isProfessional && ctx.onsite && s == "en_route" -> {
            title = "Vas en camino"
            text = if (tracking != null) "Tu trayecto se comparte en segundo plano. Marca tu llegada cuando estés en el lugar."
            else "Comparte tu trayecto desde el mapa y marca tu llegada cuando estés en el lugar."
            content = {
                PrimaryButton("Marcar llegada", loading = op.busy, onClick = { advance("arrive", "Llegada registrada.") })
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Abrir mapa y trayecto", onClick = { nav.navigate(Routes.tracking(ctx.id)) })
            }
        }
        ctx.isProfessional && ctx.onsite && s == "arrived" -> {
            title = "Llegaste al lugar"
            text = "Cuando comiences el trabajo, marca el inicio de la actividad."
            content = { PrimaryButton("Iniciar actividad", loading = op.busy, onClick = { advance("start", "Actividad iniciada.") }) }
        }
        ctx.isProfessional && !ctx.onsite && s == "accepted" -> {
            title = "Inicia el proyecto"
            text = "Cuando comiences a trabajar, marca el inicio para que el cliente vea el avance."
            content = {
                PrimaryButton("Iniciar actividad del proyecto", loading = op.busy, onClick = { advance("start", "Proyecto iniciado.") })
            }
        }
        ctx.guaranteed && ctx.isProfessional && s == "in_progress" -> {
            title = "Presenta la entrega"
            text = ctx.lastDelivery?.takeIf { it.s("status") == "changes_requested" }?.let {
                "${ctx.counterpart} pidió correcciones: \"${it.s("review_note").orEmpty()}\". Presenta una nueva entrega cuando estén listas."
            } ?: "Al terminar, describe el trabajo realizado y adjunta evidencias. El cliente validará la entrega para liberar tu pago."
            content = { PrimaryButton("Presentar entrega", onClick = { open(Dlg.Delivery) }) }
        }
        ctx.guaranteed && ctx.isProfessional && s == "awaiting_confirmation" -> {
            title = "Entrega enviada"
            text = "Esperando que ${ctx.counterpart} valide la entrega y libere el pago de ${money(ctx.price, ctx.currency)}."
        }
        ctx.guaranteed && ctx.isCustomer && s == "accepted" && ctx.escrowStatus == "unfunded" -> {
            title = "Reserva el pago en custodia"
            text = "Reserva ${money(ctx.price, ctx.currency)} desde tu wallet. El dinero queda protegido y solo se libera a ${ctx.counterpart} cuando valides el trabajo."
            content = {
                PrimaryButton("Reservar pago", loading = op.busy, onClick = {
                    op.run(null) {
                        val quote = app.ops.call("quote_contract_funding", "contract" to ctx.id).obj()
                            ?: throw ApiException("No pudimos preparar la reserva. Inténtalo de nuevo.")
                        open(Dlg.Fund(quote))
                    }
                })
            }
        }
        ctx.isCustomer && ctx.onsite && s == "accepted" && shared -> {
            title = "Ubicación compartida"
            text = "${ctx.counterpart} ya puede ver " + (if (location?.gps == true) "tu ubicación en vivo" else "la dirección del servicio") +
                (countdownText(epochMs(location?.expiresAt), now)?.let { ". El permiso vence $it." } ?: ".")
            content = { SecondaryButton("Ver ubicación y permiso", onClick = { nav.navigate(Routes.tracking(ctx.id)) }) }
        }
        ctx.isCustomer && ctx.onsite && s == "accepted" -> {
            title = "Comparte tu ubicación"
            text = "${ctx.counterpart} la necesita para salir en camino. Elige tu GPS en vivo o la dirección del servicio, por cuánto tiempo, y revócala cuando quieras."
            content = { PrimaryButton("Compartir ubicación", onClick = { nav.navigate(Routes.tracking(ctx.id)) }) }
        }
        ctx.isCustomer && ctx.onsite && s == "en_route" -> {
            title = "${ctx.counterpart} va en camino"
            text = "Sigue su trayecto en el mapa mientras la ubicación esté compartida."
            content = { PrimaryButton("Ver en el mapa", onClick = { nav.navigate(Routes.tracking(ctx.id)) }) }
        }
        ctx.isCustomer && ctx.onsite && s == "arrived" -> {
            title = "${ctx.counterpart} llegó al lugar"
            text = "El profesional marcó su llegada. Pronto iniciará la actividad."
        }
        ctx.guaranteed && ctx.isCustomer && s == "in_progress" -> {
            title = "Trabajo en curso"
            text = "Cuando ${ctx.counterpart} presente la entrega, podrás validarla y liberar el pago, o pedir correcciones."
        }
        ctx.guaranteed && ctx.isCustomer && s == "awaiting_confirmation" -> {
            val d = ctx.lastDelivery?.takeIf { it.s("status") == "submitted" }
            title = "Revisa la entrega"
            text = if (d != null) "¿El trabajo se realizó conforme a lo acordado? Al validar, se liberan ${money(ctx.price, ctx.currency)} a la wallet de ${ctx.counterpart}."
            else "Esperando la entrega del profesional."
            if (d != null) content = {
                PrimaryButton("Validar y liberar pago", onClick = {
                    val key = newKey()
                    open(
                        Dlg.Confirm(
                            "Liberar pago",
                            "Confirmo que el trabajo cumple lo acordado. Se liberarán ${money(ctx.price, ctx.currency)} a ${ctx.counterpart} y el contrato finalizará. Esta acción no se puede deshacer.",
                            "Liberar pago", "¡Pago liberado al profesional!",
                        ) { app.ops.call("release_contract_funds", "contract" to ctx.id, "delivery" to d.s("id"), "key" to key) },
                    )
                })
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Solicitar correcciones", onClick = {
                    open(
                        Dlg.Reason(
                            "Solicitar correcciones",
                            "Explica qué falta o qué debe corregirse. El servicio volverá a estar en curso.",
                            "Enviar solicitud", 10, "Solicitud de correcciones enviada.",
                        ) { app.ops.call("request_delivery_changes", "delivery" to d.s("id"), "reason" to it) },
                    )
                })
            }
        }
        !ctx.guaranteed && (s == "in_progress" || s == "awaiting_confirmation") -> {
            if (ctx.myFinishedAt != null) {
                title = "Finalización confirmada"
                text = "Confirmaste la finalización el ${dateTime(ctx.myFinishedAt)}. Falta la confirmación de ${ctx.counterpart}."
            } else {
                title = "Finalización y pago en persona"
                text = "Al terminar el trabajo y recibir o entregar el pago acordado (${money(ctx.price, ctx.currency)}), confirma la finalización. El servicio finaliza cuando ambas partes confirman."
                content = { PrimaryButton("Confirmar finalización y pago", onClick = { open(finishExternal) }) }
            }
        }
    }
    if (title == null) return
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Outlined.TaskAlt)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("PRÓXIMO PASO", style = MaterialTheme.typography.labelSmall, color = Vinko.Primary)
                Text(title!!, style = MaterialTheme.typography.titleMedium)
            }
        }
        text?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
        }
        content?.let {
            Spacer(Modifier.height(14.dp))
            it()
        }
    }
}

@Composable
private fun ProgressCard(ctx: ContractCtx) {
    val steps = stepsFor(ctx.c.s("delivery_mode"))
    val reached = reachedStep(steps, ctx.status) { ctx.c.s(it) }
    val troubled = ctx.status == "cancelled" || ctx.status == "disputed"
    VCard {
        SectionTitle("Avance del servicio", Icons.Outlined.Route) { StatusBadge(ctx.status) }
        Spacer(Modifier.height(12.dp))
        steps.forEachIndexed { i, step ->
            val done = i < reached || ctx.status == "completed"
            val current = i == reached && ctx.status != "completed"
            val at = if (step.status == "awaiting_confirmation") ctx.c.s("professional_finished_at") ?: ctx.c.s("customer_finished_at") else ctx.c.s(step.atKey)
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(24.dp).background(
                            when {
                                done -> Vinko.Mint
                                current && troubled -> Vinko.Coral
                                current -> Vinko.Primary
                                else -> Vinko.Tint
                            },
                            CircleShape,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        else Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = if (current) Color.White else Vinko.Subtle)
                    }
                    if (i < steps.lastIndex) Box(Modifier.width(2.dp).height(26.dp).background(if (done) Vinko.Mint else Vinko.Line))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.padding(top = 2.dp)) {
                    Text(
                        step.label,
                        style = if (current) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                        color = if (done || current) Vinko.Ink else Vinko.Subtle,
                    )
                    if (at != null && (done || current)) Text(dateTime(at), style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
            }
        }
        if (troubled) {
            Spacer(Modifier.height(4.dp))
            Pill(statusLabel(ctx.status), fg = Vinko.Coral, bg = Vinko.CoralBg, icon = if (ctx.status == "cancelled") Icons.Outlined.Cancel else Icons.Outlined.ReportProblem)
        }
    }
}

@Composable
private fun EscrowCard(ctx: ContractCtx) {
    val e = ctx.escrow
    val (dot, label) = when {
        e == null -> Vinko.Subtle to "Consultando custodia…"
        e.b("funding_on_hold") -> Vinko.Coral to "Fondos retenidos temporalmente"
        else -> (if (ctx.escrowStatus in setOf("funded", "released")) Vinko.Mint else if (ctx.escrowStatus == "refunded") Vinko.Cyan else Vinko.Amber) to escrowLabel(ctx.escrowStatus)
    }
    VCard {
        SectionTitle("Pago protegido", Icons.Outlined.VerifiedUser)
        Spacer(Modifier.height(8.dp))
        Text(money(ctx.price, ctx.currency), fontFamily = Manrope, style = MaterialTheme.typography.headlineMedium, color = Vinko.Primary)
        Text(
            "El cliente paga por la plataforma. El dinero queda en custodia de SoyVinko y se libera a la wallet del profesional cuando el cliente valida la entrega.",
            style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().background(Vinko.Cream, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).background(dot, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Estado de los fondos", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
                Text(label, style = MaterialTheme.typography.titleSmall)
            }
            e?.s("funded_at")?.takeIf { ctx.escrowStatus == "funded" }?.let { Text(date(it), style = MaterialTheme.typography.bodySmall, color = Vinko.Muted) }
            e?.s("released_at")?.let { Text(date(it), style = MaterialTheme.typography.bodySmall, color = Vinko.Muted) }
        }
        e?.s("refund_requested_at")?.let {
            Spacer(Modifier.height(10.dp))
            Banner(
                "El cliente solicitó la devolución el ${date(it)}" + (e.s("refund_reason")?.let { r -> ": \"$r\"" } ?: "."),
                BannerTone.Info,
            )
        }
        if (ctx.isCustomer && ctx.status == "accepted" && ctx.escrowStatus == "unfunded") {
            Spacer(Modifier.height(10.dp))
            Text("Reserva el pago desde \"Próximo paso\" para que el profesional pueda comenzar.", style = MaterialTheme.typography.bodySmall, color = Vinko.Amber)
        }
    }
}

@Composable
private fun ExternalPaymentCard(ctx: ContractCtx, payments: List<JsonObject>, open: (Dlg) -> Unit) {
    val app = LocalApp.current
    VCard {
        SectionTitle("Pago directo en persona", Icons.Outlined.Handshake)
        Spacer(Modifier.height(8.dp))
        Text(money(ctx.price, ctx.currency), fontFamily = Manrope, style = MaterialTheme.typography.headlineMedium, color = Vinko.Cyan)
        Text(
            "Acordaron pagar directamente al realizar el trabajo (efectivo, Pago Móvil o transferencia). SoyVinko no retiene fondos ni cobra comisiones.",
            style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted,
        )
        if (payments.isNotEmpty()) {
            Divider()
            Text("Pagos registrados", style = MaterialTheme.typography.titleSmall)
            payments.forEach { r ->
                val confirmed = r.s("confirmed_at") != null
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.Payments, tint = if (confirmed) Vinko.Mint else Vinko.Amber, bg = if (confirmed) Vinko.MintBg else Vinko.AmberBg, size = 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(money(r.l("amount_minor"), r.s("currency")?.trim()), style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${r.s("method").orEmpty()} · ${ctx.nameOf(r.s("reported_by"))} · ${date(r.s("reported_at"))}",
                            style = MaterialTheme.typography.bodySmall, color = Vinko.Muted,
                        )
                    }
                    Pill(if (confirmed) "Confirmado" else "Por confirmar", fg = if (confirmed) Vinko.Mint else Vinko.Amber, bg = if (confirmed) Vinko.MintBg else Vinko.AmberBg)
                }
                if (!confirmed && r.s("reported_by") != ctx.me) {
                    SecondaryButton("Confirmar recepción del pago", onClick = {
                        open(
                            Dlg.Confirm(
                                "Confirmar recepción",
                                "¿Confirmas que recibiste ${money(r.l("amount_minor"), r.s("currency")?.trim())} por ${r.s("method").orEmpty()}?",
                                "Confirmar", "Recepción del pago confirmada.",
                            ) { app.ops.call("confirm_external_payment", "report" to r.s("id")) },
                        )
                    })
                }
            }
        }
        if (ctx.status !in setOf("cancelled", "disputed")) {
            Spacer(Modifier.height(12.dp))
            OutlineAction("Registrar pago realizado", Icons.Outlined.Payments, { open(Dlg.ExternalPay) }, Modifier.fillMaxWidth(), tint = Vinko.Primary)
        }
    }
}

@Composable
private fun DeliveriesCard(ctx: ContractCtx, open: (Dlg) -> Unit) {
    VCard {
        SectionTitle("Entregas del trabajo", Icons.Outlined.Inventory2)
        Spacer(Modifier.height(8.dp))
        if (ctx.deliveries.isEmpty()) {
            Text(
                "Las entregas del profesional y las validaciones de pago aparecerán aquí.",
                style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted,
            )
        }
        ctx.deliveries.sortedByDescending { it.l("version") ?: 0 }.forEachIndexed { i, d ->
            if (i > 0) Divider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Entrega ${d.l("version") ?: ""}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                StatusBadge(d.s("status"))
            }
            Text(dateTime(d.s("created_at")), style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle)
            Spacer(Modifier.height(6.dp))
            Text(d.s("summary").orEmpty(), style = MaterialTheme.typography.bodyMedium)
            val files = (d["files"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
            if (files.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    files.forEach { f -> Thumb(f, 72.dp) { open(Dlg.Photo(f)) } }
                }
            }
            d.s("review_note")?.takeIf { d.s("status") == "changes_requested" }?.let {
                Spacer(Modifier.height(8.dp))
                Text("Correcciones pedidas: $it", style = MaterialTheme.typography.bodySmall, color = Vinko.Amber)
            }
        }
    }
}

@Composable
private fun TermsCard(ctx: ContractCtx, open: (Dlg) -> Unit) {
    val app = LocalApp.current
    VCard {
        SectionTitle("Alcance y condiciones", Icons.Outlined.Description)
        Spacer(Modifier.height(8.dp))
        KeyValue("Importe acordado", money(ctx.price, ctx.currency), strong = true)
        KeyValue("Forma de pago", if (ctx.guaranteed) "Saldo SoyVinko (custodia)" else paymentMethodLabel(ctx.c.s("payment_method")))
        ctx.c.s("payment_terms")?.takeIf { it.isNotBlank() }?.let {
            Text("Condiciones: $it", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = Vinko.Muted)
        }
        val loc = ctx.c.o2("onsite_location")
        if (ctx.onsite && loc != null) {
            Divider()
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Lugar del servicio", style = MaterialTheme.typography.titleSmall)
                    Text(listOfNotNull(loc.s("address_line"), loc.s("city")).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                    loc.s("instructions")?.takeIf { it.isNotBlank() }?.let {
                        Text("Indicaciones: $it", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                    }
                }
            }
        }
        val requirements = ctx.c.s("work_requirements") ?: ctx.c.s("request_description")
        if (!requirements.isNullOrBlank()) {
            Divider()
            Text("Lo que se necesita", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(requirements, style = MaterialTheme.typography.bodyMedium)
        }
        val photos = ctx.c.list("reference_photos").mapNotNull { it.s("file_id") }
        if (photos.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.forEach { f -> Thumb(f) { open(Dlg.Photo(f)) } }
            }
        }
        if (ctx.terms.isNotEmpty()) {
            Divider()
            Text("Versiones de las condiciones", style = MaterialTheme.typography.titleSmall)
            ctx.terms.forEachIndexed { i, t ->
                val both = t.s("customer_accepted_at") != null && t.s("professional_accepted_at") != null
                val mine = if (ctx.isCustomer) t.s("customer_accepted_at") else t.s("professional_accepted_at")
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp).background(if (i == 0) Vinko.Tint else Vinko.Cream, RoundedCornerShape(14.dp)).padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Versión ${t.l("version")}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        when {
                            both -> Pill("Aceptadas", fg = Vinko.Mint, bg = Vinko.MintBg)
                            i > 0 -> Pill("Reemplazadas", fg = Vinko.Subtle, bg = Color.White)
                            else -> Pill("Pendientes", fg = Vinko.Amber, bg = Vinko.AmberBg)
                        }
                    }
                    Text(
                        "Propuesta por ${ctx.nameOf(t.s("proposed_by"))} · ${date(t.s("created_at"))}",
                        style = MaterialTheme.typography.bodySmall, color = Vinko.Muted,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${money(t.l("price_minor"), t.s("currency")?.trim())} · ${paymentMethodLabel(t.s("payment_method"))}", style = MaterialTheme.typography.titleSmall)
                    t.s("payment_terms")?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    if (i == 0 && !both) {
                        Spacer(Modifier.height(6.dp))
                        CheckLine("Cliente", t.s("customer_accepted_at") != null)
                        CheckLine("Profesional", t.s("professional_accepted_at") != null)
                    }
                    if (i == 0 && mine == null && ctx.status in TERMS_STATUSES) {
                        Spacer(Modifier.height(10.dp))
                        PrimaryButton("Aceptar condiciones", onClick = {
                            open(
                                Dlg.Confirm(
                                    "Aceptar condiciones",
                                    "¿Aceptas ${money(t.l("price_minor"), t.s("currency")?.trim())} y las condiciones de la versión ${t.l("version")}? Pasan a ser las del contrato cuando ambas partes las aceptan.",
                                    "Aceptar", "Condiciones aceptadas.",
                                ) { app.ops.call("accept_terms", "terms" to t.s("id")) },
                            )
                        })
                    }
                }
            }
        }
        if (ctx.status in TERMS_STATUSES) {
            Spacer(Modifier.height(12.dp))
            OutlineAction("Proponer nuevas condiciones", Icons.Outlined.Tune, { open(Dlg.Propose) }, Modifier.fillMaxWidth(), tint = Vinko.Primary)
        }
    }
}

@Composable
private fun ContactsCard(ctx: ContractCtx, contacts: JsonObject?) {
    val context = LocalPlatformContext.current
    val snackbar = LocalSnackbar.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    fun fail(msg: String) = scope.launch { snackbar.showSnackbar(msg) }
    val all = contacts.list("contacts") + contacts.list("professional_contacts")
    val whatsapp = all.firstOrNull { it.s("kind") == "whatsapp" }?.s("value") ?: contacts.s("primary_whatsapp")
    val phone = all.firstOrNull { it.s("kind") == "phone" }?.s("value") ?: contacts.s("primary_phone") ?: whatsapp
    val email = contacts.s("email")
    VCard {
        SectionTitle("Contacto directo", Icons.Outlined.Phone)
        Spacer(Modifier.height(4.dp))
        Text(
            if (contacts == null) "Cargando los datos de contacto…"
            else "Contacto habilitado con ${contacts.s("name") ?: ctx.counterpart} porque ambas partes aceptaron el acuerdo.",
            style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted,
        )
        if (contacts != null && phone == null && whatsapp == null && email == null) {
            Spacer(Modifier.height(6.dp))
            Text("No registró teléfono ni correo. Usa el chat del servicio.", style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle)
        }
        val message = "Hola ${contacts.s("name").orEmpty()}, te contacto por el contrato ${folio(ctx.id)} en SoyVinko."
        if (whatsapp != null || phone != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (whatsapp ?: phone)?.let { wa ->
                    OutlineAction("WhatsApp", Icons.AutoMirrored.Outlined.Chat, {
                        if (!openWhatsApp(context, wa, message)) fail("WhatsApp no está instalado en este teléfono.")
                    }, Modifier.weight(1f), tint = Vinko.Mint)
                }
                phone?.let { p ->
                    OutlineAction("Llamar", Icons.Outlined.Phone, {
                        if (!dial(context, p)) fail("No encontramos una app para llamar.")
                    }, Modifier.weight(1f), tint = Vinko.Primary)
                }
            }
            Text(listOfNotNull(phone, whatsapp?.takeIf { it != phone }).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, modifier = Modifier.padding(top = 6.dp))
        }
        email?.let { mail ->
            Spacer(Modifier.height(10.dp))
            OutlineAction("Enviar correo", Icons.Outlined.Email, {
                if (!sendEmail(context, mail, "Contrato SoyVinko ${folio(ctx.id)}")) fail("No encontramos una app de correo.")
            }, Modifier.fillMaxWidth())
            Text(mail, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun RatingCard(ctx: ContractCtx, open: (Dlg) -> Unit) {
    VCard {
        SectionTitle("Calificación del servicio", Icons.Outlined.Star)
        Spacer(Modifier.height(8.dp))
        val mine = ctx.myReview
        if (mine == null) {
            Text(
                "¿Cómo fue tu experiencia con ${ctx.counterpart}? Tu calificación se publica de inmediato y fortalece la confianza de la comunidad.",
                style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted,
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Calificar a ${ctx.counterpart}", onClick = { open(Dlg.Rate(0, true, "")) })
        } else {
            ReviewBlock("Tu calificación", mine)
            Spacer(Modifier.height(8.dp))
            OutlineAction("Editar calificación", Icons.Outlined.Star, {
                open(Dlg.Rate((mine.l("rating") ?: 0).toInt(), mine.b("recommends"), mine.s("comment").orEmpty()))
            }, Modifier.fillMaxWidth())
        }
        ctx.theirReview?.let {
            Divider()
            ReviewBlock("Calificación de ${ctx.counterpart}", it)
        }
    }
}

@Composable
private fun ReviewBlock(title: String, r: JsonObject) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Row(verticalAlignment = Alignment.CenterVertically) {
        StarPicker((r.l("rating") ?: 0).toInt(), size = 22.dp)
        Spacer(Modifier.width(8.dp))
        Text("${r.l("rating") ?: 0} de 5", style = MaterialTheme.typography.labelLarge)
    }
    r.s("comment")?.takeIf { it.isNotBlank() }?.let { Text("\"$it\"", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic) }
    Text(if (r.b("recommends")) "✓ Recomienda a esta persona" else "No recomienda a esta persona", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
}

@Composable
private fun ManageCard(ctx: ContractCtx, open: (Dlg) -> Unit) {
    val app = LocalApp.current
    val canDispute = ctx.open
    val canCancel = ctx.status in CANCEL_STATUSES && (!ctx.guaranteed || ctx.escrowStatus == "unfunded")
    val canRefund = ctx.isCustomer && ctx.guaranteed && ctx.escrowStatus == "funded" && ctx.escrow?.s("refund_requested_at") == null
    val canApproveRefund = ctx.isProfessional && ctx.guaranteed && ctx.escrow?.s("refund_requested_at") != null && ctx.escrowStatus in setOf("funded", "disputed")
    if (!canDispute && !canCancel && !canRefund && !canApproveRefund) return
    VCard {
        SectionTitle("Gestión del servicio", Icons.Outlined.Tune)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canApproveRefund) OutlineAction("Aprobar devolución al cliente", Icons.AutoMirrored.Outlined.Undo, {
                open(
                    Dlg.Confirm(
                        "Aprobar devolución",
                        "Se devolverán los fondos en custodia (${money(ctx.price, ctx.currency)}) a la wallet de ${ctx.counterpart} y el servicio quedará cancelado.",
                        "Aprobar devolución", "Devolución aprobada.", destructive = true,
                    ) { app.ops.call("approve_escrow_refund", "contract" to ctx.id) },
                )
            }, Modifier.fillMaxWidth(), tint = Vinko.Primary)
            if (canRefund) OutlineAction("Solicitar devolución", Icons.AutoMirrored.Outlined.Undo, {
                open(
                    Dlg.Reason(
                        "Solicitar devolución",
                        "Explica por qué pides la devolución de los fondos en custodia. Se abrirá un caso con soporte y ${ctx.counterpart} podrá aprobarla.",
                        "Solicitar devolución", 10, "Solicitud de devolución enviada.", destructive = true,
                    ) { app.ops.call("request_escrow_refund", "contract" to ctx.id, "reason" to it) },
                )
            }, Modifier.fillMaxWidth())
            if (canDispute) OutlineAction("Reportar un problema", Icons.Outlined.ReportProblem, {
                open(
                    Dlg.Reason(
                        "Reportar un problema",
                        "Cuéntanos qué pasó. Se abrirá un caso con soporte y el servicio quedará en disputa mientras se revisa.",
                        "Reportar", 10, "Reportamos el problema. Soporte te contactará.", destructive = true,
                    ) { app.ops.call("dispute_contract", "contract" to ctx.id, "reason" to it) },
                )
            }, Modifier.fillMaxWidth(), tint = Vinko.Coral)
            if (canCancel) OutlineAction("Cancelar servicio", Icons.Outlined.Cancel, {
                open(
                    Dlg.Reason(
                        "Cancelar servicio",
                        "La cancelación es definitiva y se notificará a ${ctx.counterpart}. Si el trabajo ya comenzó, reporta un problema en su lugar.",
                        "Cancelar servicio", 5, "Servicio cancelado.", destructive = true,
                    ) { app.ops.call("cancel_contract", "contract" to ctx.id, "reason" to it) },
                )
            }, Modifier.fillMaxWidth(), tint = Vinko.Coral)
        }
    }
}

@Composable
private fun HistoryCard(ctx: ContractCtx) {
    val events = ctx.c.list("events")
    if (events.isEmpty()) return
    VCard {
        SectionTitle("Historial del contrato", Icons.Outlined.History)
        Spacer(Modifier.height(8.dp))
        events.forEach { ev ->
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 6.dp).size(8.dp).background(Vinko.Lavender, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(eventLabel(ev.s("event_type"), ev.s("to_status")), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        listOfNotNull(dateTime(ev.s("created_at")), ev.s("actor_id")?.let { ctx.nameOf(it) }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle,
                    )
                }
            }
        }
    }
}

private fun JsonObject.o2(key: String): JsonObject? = this[key] as? JsonObject
