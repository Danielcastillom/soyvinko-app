package com.soyvinko.app.ui.feature.admin

import com.soyvinko.app.ui.common.divHalfUp
import com.soyvinko.app.ui.common.shortDate
import com.soyvinko.app.ui.common.nowInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/** Hojas que se abren desde las tarjetas y encabezados del panel. */
sealed interface AdminDlg {
    data class UserStatus(val row: JsonObject) : AdminDlg
    data class AssignRole(val row: JsonObject) : AdminDlg
    data class ModerateRequest(val row: JsonObject) : AdminDlg
    data class ModerateReview(val row: JsonObject) : AdminDlg
    data class ModerateFile(val row: JsonObject) : AdminDlg
    data class ViewFile(val row: JsonObject) : AdminDlg
    data class OpenKyc(val row: JsonObject) : AdminDlg
    data class ReviewKyc(val row: JsonObject) : AdminDlg
    data class ProfessionalPlan(val row: JsonObject) : AdminDlg
    data class StorePlan(val row: JsonObject) : AdminDlg
    data class StoreStatus(val row: JsonObject) : AdminDlg
    data class AdvanceOrder(val row: JsonObject) : AdminDlg
    data class ConfigurePlan(val row: JsonObject?) : AdminDlg
    data class ConfigureFee(val active: Map<String, Long>) : AdminDlg
    data class ConfigureRate(val active: Map<String, Long>) : AdminDlg
    data class Settle(val row: JsonObject) : AdminDlg
    data class ContractDetail(val row: JsonObject) : AdminDlg
    data class OrderDetail(val row: JsonObject) : AdminDlg
    data class StoreDetail(val row: JsonObject) : AdminDlg
}

/** Muestra la hoja indicada. `onDone` cierra y recarga la lista. */
@Composable
fun AdminDialogHost(
    dlg: AdminDlg,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
    onOpenKyc: (id: String, reason: String) -> Unit,
) {
    when (dlg) {
        is AdminDlg.UserStatus -> UserStatusSheet(dlg.row, onDismiss, onDone)
        is AdminDlg.AssignRole -> AssignRoleSheet(dlg.row, onDismiss, onDone)
        is AdminDlg.ModerateRequest -> ModerateRequestSheet(dlg.row, onDismiss, onDone)
        is AdminDlg.ModerateReview -> ModerateReviewSheet(dlg.row, onDismiss, onDone)
        is AdminDlg.ModerateFile -> ModerateFileSheet(dlg.row, onDismiss, onDone)
        is AdminDlg.ViewFile -> FileViewerLoader(dlg.row.s("id").orEmpty(), filePurposeLabel(dlg.row.s("purpose")), FILE_REVIEW_REASON, onDismiss)
        is AdminDlg.OpenKyc -> OpenKycSheet(dlg.row, onDismiss) { reason -> onDismiss(); onOpenKyc(dlg.row.s("id").orEmpty(), reason) }
        is AdminDlg.ReviewKyc -> ReviewKycSheet(dlg.row, onDismiss, onDone)
        is AdminDlg.ProfessionalPlan -> ProfessionalPlanSheet(dlg.row, onDismiss, onDone)
        is AdminDlg.StorePlan -> StorePlanSheet(dlg.row, onDismiss, onDone)
        is AdminDlg.StoreStatus -> StoreStatusSheet(dlg.row, onDismiss, onDone)
        is AdminDlg.AdvanceOrder -> AdvanceOrderSheet(dlg.row, onDismiss, onDone)
        is AdminDlg.ConfigurePlan -> ConfigurePlanSheet(dlg.row, onDismiss, onDone)
        is AdminDlg.ConfigureFee -> ConfigureFeeSheet(dlg.active, onDismiss, onDone)
        is AdminDlg.ConfigureRate -> ConfigureRateSheet(dlg.active, onDismiss, onDone)
        is AdminDlg.Settle -> SettleSheet(dlg.row, onDismiss, onDone)
        is AdminDlg.ContractDetail -> ContractDetailSheet(dlg.row, onDismiss)
        is AdminDlg.OrderDetail -> OrderDetailSheet(dlg.row, onDismiss)
        is AdminDlg.StoreDetail -> StoreDetailSheet(dlg.row, onDismiss)
    }
}

/**
 * Hoja con motivo obligatorio: confirma la acción, valida el mínimo de caracteres que exige el SQL y
 * muestra el error en línea. `extra` agrega los campos propios de cada acción.
 */
@Composable
fun ReasonSheet(
    title: String,
    confirm: String,
    min: Int,
    success: String?,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
    subtitle: String? = null,
    destructive: Boolean = false,
    initialReason: String = "",
    reasonLabel: String = "Motivo",
    reasonHint: String? = null,
    canConfirm: Boolean = true,
    extra: @Composable ColumnScope.() -> Unit = {},
    action: suspend (String) -> Unit,
) {
    val op = rememberAdminOp()
    var reason by remember { mutableStateOf(initialReason) }
    var error by remember { mutableStateOf<String?>(null) }
    AdminSheet(
        title, onDismiss,
        confirmLabel = confirm,
        onConfirm = { error = null; op.run(success, { error = it }, after = onDone) { action(reason.trim()) } },
        busy = op.busy,
        subtitle = subtitle,
        error = error,
        destructive = destructive,
        confirmEnabled = canConfirm && reason.trim().length >= min,
    ) {
        extra()
        ReasonField(reason, { reason = it; error = null }, min, reasonLabel, reasonHint)
    }
}

// ---------------------------------------------------------------------------------------------
// Usuarios
// ---------------------------------------------------------------------------------------------

@Composable
private fun UserStatusSheet(row: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    var status by remember { mutableStateOf(if (row.s("status") == "suspended") "active" else "suspended") }
    ReasonSheet(
        "Cambiar estado", "Guardar estado", 10, "Estado de la cuenta actualizado.", onDismiss, onDone,
        subtitle = "${row.s("display_name").orEmpty()} · ${row.s("email").orEmpty()}",
        destructive = status == "suspended",
        extra = {
            FieldLabel("Estado")
            RadioList(
                listOf(
                    Triple("active", "Activo", "Puede iniciar sesión y usar la plataforma."),
                    Triple("suspended", "Suspendido", "Cierra sus sesiones abiertas y bloquea el acceso."),
                ),
                status,
            ) { status = it }
        },
    ) { reason -> app.ops.call("set_user_status", "user" to row.s("id"), "status" to status, "reason" to reason) }
}

@Composable
private fun AssignRoleSheet(row: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    var role by remember { mutableStateOf("support") }
    var enable by remember { mutableStateOf(true) }
    ReasonSheet(
        "Asignar rol", if (enable) "Asignar rol" else "Retirar rol", 10, "Roles actualizados.", onDismiss, onDone,
        subtitle = "${row.s("display_name").orEmpty()} · ${row.s("email").orEmpty()}",
        destructive = !enable,
        extra = {
            FieldLabel("Rol")
            ChoiceGroup(listOf("support" to "Soporte", "admin" to "Administrador", "superadmin" to "Superadministrador"), role) { role = it }
            SwitchRow("Habilitar rol", if (enable) "Se otorgará el rol a la cuenta." else "Se retirará el rol de la cuenta.", enable) { enable = it }
        },
    ) { reason -> app.ops.call("assign_role", "user" to row.s("id"), "role" to role, "enable" to enable, "reason" to reason) }
}

// ---------------------------------------------------------------------------------------------
// Moderación
// ---------------------------------------------------------------------------------------------

@Composable
private fun ModerateRequestSheet(row: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    val approve = row.s("moderation_status") != "approved"
    ReasonSheet(
        if (approve) "Autorizar publicación" else "Desautorizar",
        if (approve) "Autorizar publicación" else "Desautorizar",
        5, if (approve) "Solicitud autorizada." else "Solicitud desautorizada.", onDismiss, onDone,
        subtitle = row.s("title"),
        destructive = !approve,
    ) { reason -> app.ops.call("moderate_request", "request" to row.s("id"), "approve" to approve, "reason" to reason) }
}

@Composable
private fun ModerateReviewSheet(row: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    var status by remember { mutableStateOf(if (row.s("moderation_status") == "published") "hidden" else "published") }
    ReasonSheet(
        "Moderar reseña", "Moderar reseña", 5, "Reseña moderada.", onDismiss, onDone,
        subtitle = "La puntuación de la reseña no se puede cambiar.",
        extra = {
            row.s("comment")?.takeIf { it.isNotBlank() }?.let {
                Text("“$it”", style = MaterialTheme.typography.bodyMedium, color = Vinko.Ink,
                    modifier = Modifier.fillMaxWidth().background(Vinko.Cream, RoundedCornerShape(12.dp)).padding(12.dp))
            }
            FieldLabel("Visibilidad")
            ChoiceGroup(listOf("published" to "Publicar", "hidden" to "Ocultar"), status) { status = it }
        },
    ) { reason -> app.ops.call("moderate_review", "review" to row.s("id"), "status" to status, "reason" to reason) }
}

@Composable
private fun ModerateFileSheet(row: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    var approve by remember { mutableStateOf(true) }
    val cleanable = row.b("exif_removed") || row.s("purpose") == "kyc"
    ReasonSheet(
        "Moderar", if (approve) "Aprobar archivo" else "Rechazar archivo", 5,
        if (approve) "Archivo aprobado." else "Archivo rechazado.", onDismiss, onDone,
        subtitle = "${filePurposeLabel(row.s("purpose"))} · ${row.s("mime_type").orEmpty()} · ${bytesText(row.l("bytes"))}",
        destructive = !approve,
        extra = {
            SwitchRow("Aprobar archivo", if (approve) "Quedará disponible para su uso en la plataforma." else "Se marcará como rechazado.", approve) { approve = it }
            if (approve && !cleanable) Banner(
                "Este archivo todavía conserva metadatos EXIF: el servidor solo permite aprobarlo cuando se hayan eliminado.",
                BannerTone.Info,
            )
        },
    ) { reason -> app.ops.call("moderate_file", "file" to row.s("id"), "approve" to approve, "reason" to reason) }
}

// ---------------------------------------------------------------------------------------------
// Identidad (KYC)
// ---------------------------------------------------------------------------------------------

const val KYC_OPEN_REASON = "Revisión del expediente para verificar identidad."

@Composable
private fun OpenKycSheet(row: JsonObject, onDismiss: () -> Unit, onOpen: (String) -> Unit) {
    var reason by remember { mutableStateOf(KYC_OPEN_REASON) }
    AdminSheet(
        "Abrir expediente", onDismiss,
        confirmLabel = "Abrir expediente",
        onConfirm = { onOpen(reason.trim()) },
        subtitle = "Los datos personales y documentos del expediente ${shortId(row.s("id"))} son confidenciales. El motivo de la consulta queda registrado en auditoría.",
        confirmEnabled = reason.trim().length >= 10,
    ) {
        ReasonField(reason, { reason = it }, 10, "Motivo de la consulta")
    }
}

/** Datos del titular guardados por la verificación personal (texto JSON en `external_reference`). */
fun kycMeta(row: JsonObject?): JsonObject? = row.s("external_reference")?.takeIf { it.trim().startsWith("{") }?.let {
    runCatching { kotlinx.serialization.json.Json.parseToJsonElement(it) as? JsonObject }.getOrNull()
}

/** Resolver verificación: aprobada (con vencimiento), rechazada o revocada. Igual que la web. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewKycSheet(row: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    val today = remember { nowInstant().toLocalDateTime(TimeZone.UTC).date }
    val docExpiry = remember(row) {
        kycMeta(row).s("expiration_date")?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }?.takeIf { it > today }
    }
    var status by remember { mutableStateOf("approved") }
    var expiry by remember { mutableStateOf(today.plus(1, DateTimeUnit.YEAR)) }
    var picking by remember { mutableStateOf(false) }
    ReasonSheet(
        "Resolver verificación", "Resolver verificación", 5, "Verificación resuelta.", onDismiss, onDone,
        subtitle = "Expediente ${shortId(row.s("id"))} · ${kycProviderLabel(row.s("provider"))}",
        destructive = status != "approved",
        extra = {
            FieldLabel("Resultado")
            RadioList(
                listOf(
                    Triple("approved", "Aprobada", "La identidad queda verificada hasta la fecha indicada."),
                    Triple("rejected", "Rechazada", "La persona deberá enviar una nueva verificación."),
                    Triple("revoked", "Revocada", "Anula una verificación aprobada anteriormente."),
                ),
                status,
            ) { status = it }
            if (status == "approved") {
                FieldLabel("Válida hasta")
                val options = buildList {
                    add(today.plus(1, DateTimeUnit.YEAR) to "1 año")
                    add(today.plus(2, DateTimeUnit.YEAR) to "2 años")
                    add(today.plus(5, DateTimeUnit.YEAR) to "5 años")
                    docExpiry?.let { add(it to "Vence el documento") }
                }
                ChoiceGroup(options.map { it.first.toString() to it.second } + ("custom" to "Otra fecha"), expiry.toString()) { v ->
                    if (v == "custom") picking = true else expiry = LocalDate.parse(v)
                }
                Text("Vence el ${shortDate(expiry)}", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
            }
        },
    ) { reason ->
        app.ops.call(
            "review_kyc",
            "case" to row.s("id"),
            "status" to status,
            "reason" to reason,
            "expires_at" to if (status == "approved") LocalDateTime(expiry, LocalTime(23, 59, 59)).toInstant(TimeZone.UTC).toString() else null,
        )
    }
    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = expiry.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }
                        ?.takeIf { it > today }?.let { expiry = it }
                    picking = false
                }) { Text("Aceptar", color = Vinko.Primary) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancelar", color = Vinko.Muted) } },
        ) { DatePicker(state = state) }
    }
}

// ---------------------------------------------------------------------------------------------
// Planes, comercios y pedidos
// ---------------------------------------------------------------------------------------------

@Composable
private fun ProfessionalPlanSheet(row: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    var plan by remember { mutableStateOf(row.s("plan_code") ?: "PRO-PROMO") }
    ReasonSheet(
        "Asignar plan proveedor", "Asignar plan", 5, "Plan del profesional actualizado.", onDismiss, onDone,
        subtitle = "Ajusta el plan de ${row.s("professional_name") ?: "el profesional"} para calcular sus comisiones presenciales o remotas.",
        extra = {
            FieldLabel("Plan del Modelo de Negocio")
            RadioList(PRO_PLAN_OPTIONS, plan) { plan = it }
        },
    ) { reason -> app.ops.call("set_professional_plan", "professional" to row.s("professional_id"), "plan" to plan, "reason" to reason) }
}

@Composable
private fun StorePlanSheet(row: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    var plan by remember { mutableStateOf(row.s("plan_code") ?: "STORE-PROMO") }
    ReasonSheet(
        "Cambiar membresía", "Cambiar membresía", 5, "Membresía del comercio actualizada.", onDismiss, onDone,
        subtitle = "Actualiza el plan de membresía de ${row.s("name") ?: "el comercio"} conforme a su volumen y antigüedad.",
        extra = {
            FieldLabel("Plan del Comercio")
            RadioList(STORE_PLAN_OPTIONS, plan) { plan = it }
        },
    ) { reason -> app.ops.call("set_store_plan", "store" to row.s("id"), "plan" to plan, "reason" to reason) }
}

@Composable
private fun StoreStatusSheet(row: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    var status by remember { mutableStateOf(row.s("status") ?: "active") }
    ReasonSheet(
        "Cambiar estado", "Guardar estado", 5, "Estado de la tienda actualizado.", onDismiss, onDone,
        subtitle = row.s("name"),
        destructive = status != "active",
        extra = {
            FieldLabel("Estado de la tienda")
            RadioList(
                listOf(
                    Triple("active", "Activa", "Visible en el marketplace y recibiendo pedidos."),
                    Triple("suspended", "Suspendida", "Oculta del marketplace por una revisión."),
                    Triple("inactive", "Inactiva (cerrada)", "Cerrada; no recibe pedidos."),
                ),
                status,
            ) { status = it }
        },
    ) { reason -> app.ops.call("set_store_status", "store" to row.s("id"), "status" to status, "reason" to reason) }
}

@Composable
private fun AdvanceOrderSheet(row: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    var action by remember { mutableStateOf("accept") }
    ReasonSheet(
        "Intervención orden", "Aplicar intervención", 5, "Pedido actualizado.", onDismiss, onDone,
        subtitle = "Permite al soporte avanzar o cancelar el pedido ${shortId(row.s("id"))} ante incidencias entre comprador y comercio.",
        destructive = action == "cancel",
        extra = {
            FieldLabel("Acción administrativa")
            RadioList(
                listOf(
                    Triple("accept", "Forzar aceptación del comercio", null),
                    Triple("dispatch", "Marcar como despachado", null),
                    Triple("receive", "Confirmar entrega final al comprador", null),
                    Triple("cancel", "Cancelar orden y reintegrar inventario", null),
                ),
                action,
            ) { action = it }
        },
    ) { reason -> app.ops.call("admin_advance_order", "order" to row.s("id"), "action" to action, "reason" to reason) }
}

/** Configurar plan (crea o actualiza por código). El precio se escribe en unidades y se envía en menores. */
@Composable
private fun ConfigurePlanSheet(row: JsonObject?, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    val op = rememberAdminOp()
    var code by remember { mutableStateOf(row.s("code").orEmpty()) }
    var name by remember { mutableStateOf(row.s("name").orEmpty()) }
    var credits by remember { mutableStateOf((row.l("credits") ?: 0L).toString()) }
    var currency by remember { mutableStateOf(row.s("currency")?.trim() ?: "VES") }
    var amount by remember { mutableStateOf(row?.let { minorToInput(it.l("price_minor")) }.orEmpty()) }
    var weight by remember { mutableStateOf((row.l("positioning_weight") ?: 0L).toString()) }
    var positioning by remember { mutableStateOf("30") }
    var marketplace by remember { mutableStateOf("30") }
    var active by remember { mutableStateOf(row?.b("active") ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    val price = parseDecimal(amount)
    val weightN = weight.toIntOrNull()
    val valid = code.isNotBlank() && name.isNotBlank() && credits.toIntOrNull() != null && price != null &&
        weightN != null && weightN in 0..100 && positioning.toIntOrNull() != null && marketplace.toIntOrNull() != null
    val currencies = (CURRENCY_OPTIONS.map { it.first } + listOfNotNull(row.s("currency")?.trim())).distinct()
    AdminSheet(
        if (row == null) "Configurar plan" else "Editar plan", onDismiss,
        confirmLabel = "Configurar plan",
        onConfirm = {
            error = null
            op.run("Plan guardado.", { error = it }, after = onDone) {
                app.ops.call(
                    "configure_plan",
                    "code" to code.trim(),
                    "name" to name.trim(),
                    "credits" to credits.toInt(),
                    "price" to toMinor(price!!),
                    "currency" to currency,
                    "weight" to weightN,
                    "positioning_days" to positioning.toInt(),
                    "marketplace_days" to marketplace.toInt(),
                    "active" to active,
                )
            }
        },
        busy = op.busy,
        subtitle = if (row == null) "Crea un plan nuevo o actualiza uno existente usando su código comercial." else "Se actualizará el plan con código ${row.s("code")}.",
        error = error,
        confirmEnabled = valid,
    ) {
        AdminTextField(code, { code = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' || c == '_' }.take(40) }, "Código comercial (ej: STORE-CUSTOM)",
            capitalization = KeyboardCapitalization.Characters)
        AdminTextField(name, { name = it.take(120) }, "Nombre del plan", capitalization = KeyboardCapitalization.Sentences)
        AdminTextField(credits, { credits = it.filter(Char::isDigit).take(6) }, "Créditos incluidos", keyboardType = KeyboardType.Number)
        FieldLabel("Moneda")
        ChoiceGroup(currencies.map { c -> c to (CURRENCY_OPTIONS.toMap()[c] ?: c) }, currency) { currency = it }
        AdminTextField(
            amount, { v -> amount = v.filter { it.isDigit() || it == ',' || it == '.' }.take(14) }, "Precio mensual",
            keyboardType = KeyboardType.Decimal, prefix = currencyPrefix(currency),
            error = if (amount.isNotBlank() && price == null) "Escribe un importe válido." else null,
        )
        AdminTextField(
            weight, { weight = it.filter(Char::isDigit).take(3) }, "Prioridad de posicionamiento", keyboardType = KeyboardType.Number,
            supporting = "De 0 a 100", error = if (weightN != null && weightN > 100) "Usa un valor de 0 a 100." else null,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminTextField(positioning, { positioning = it.filter(Char::isDigit).take(4) }, "Días posicionamiento", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            AdminTextField(marketplace, { marketplace = it.filter(Char::isDigit).take(4) }, "Días marketplace", Modifier.weight(1f), keyboardType = KeyboardType.Number)
        }
        if (row != null) Banner("Revisa los días de posicionamiento y de marketplace: el listado no los incluye y se guardarán los valores indicados aquí.", BannerTone.Info)
        SwitchRow("Plan disponible", if (active) "Se puede contratar y asignar." else "No se ofrecerá ni podrá asignarse.", active) { active = it }
    }
}

/** Configurar comisión de retiro con vista previa del cobro (simulador). */
@Composable
private fun ConfigureFeeSheet(activeBp: Map<String, Long>, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    var currency by remember { mutableStateOf("VES") }
    var percent by remember { mutableStateOf("") }
    val parsed = parseDecimal(percent)
    val bp = parsed?.let(::toBasisPoints)
    val valid = bp != null && bp in 0..9999
    ReasonSheet(
        "Configurar comisión", "Configurar comisión", 5, "Comisión de retiro actualizada.", onDismiss, onDone,
        subtitle = "Solo aplica al retiro de saldo. Recargas, uso, conservación y liberación del pago siguen sin comisión.",
        canConfirm = valid,
        extra = {
            FieldLabel("Moneda")
            ChoiceGroup(CURRENCY_OPTIONS, currency) { currency = it }
            AdminTextField(
                percent, { v -> percent = v.filter { it.isDigit() || it == ',' || it == '.' }.take(6) }, "Porcentaje de retiro",
                keyboardType = KeyboardType.Decimal, suffix = "%",
                supporting = "Vigente: ${activeBp[currency]?.let(::percentText) ?: "sin tarifa"} · de 0 a 99,99 %",
                error = if (percent.isNotBlank() && !valid) "Usa un porcentaje de 0 a 99,99." else null,
            )
            if (valid) {
                val sample = if (currency == "USD") 10_000L else 100_000L
                val fee = divHalfUp(sample * bp!!, 10_000)
                Column(
                    Modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(14.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("Simulación", style = MaterialTheme.typography.labelMedium, color = Vinko.Primary)
                    Field("Retiro de ejemplo", money(sample, currency))
                    Field("Comisión (${percentText(bp.toLong())})", money(fee, currency), valueColor = Vinko.Coral)
                    Field("Neto a recibir", money(sample - fee, currency), valueColor = Vinko.Mint, strong = true)
                }
            }
        },
    ) { reason -> app.ops.call("configure_withdrawal_fee", "currency" to currency, "basis_points" to bp, "reason" to reason) }
}

@Composable
private fun ConfigureRateSheet(active: Map<String, Long>, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    var currency by remember { mutableStateOf("VES") }
    var rate by remember { mutableStateOf("") }
    val parsed = parseDecimal(rate)
    val minor = parsed?.let(::toMinor)
    val valid = minor != null && minor in 1..1_000_000_000L
    ReasonSheet(
        "Personalizar Tasa de Cambio Vinkos", "Guardar tasa", 5, "Tasa de Vinkos actualizada.", onDismiss, onDone,
        subtitle = "Establece la tasa oficial de conversión para créditos Vinkos. Toda la plataforma, recargas, conversiones y wallet actualizarán sus valores en tiempo real.",
        canConfirm = valid,
        extra = {
            FieldLabel("Moneda")
            ChoiceGroup(CURRENCY_OPTIONS, currency) { currency = it }
            AdminTextField(
                rate, { v -> rate = v.filter { it.isDigit() || it == ',' || it == '.' }.take(14) },
                "Valor de 1 Vinko", keyboardType = KeyboardType.Decimal, prefix = currencyPrefix(currency),
                supporting = "Vigente: ${active[currency]?.let { money(it, currency) } ?: if (currency == "VES") "Bs 50,00" else "US$ 1,00"}",
                error = if (rate.isNotBlank() && !valid) "Escribe un valor mayor que cero." else null,
            )
            if (valid) Text("1 Vinko = ${money(minor, currency)}", style = MaterialTheme.typography.titleMedium, color = Vinko.Primary)
        },
    ) { reason -> app.ops.call("configure_vinko_exchange_rate", "currency" to currency, "rate_minor" to minor, "reason" to reason) }
}

// ---------------------------------------------------------------------------------------------
// Retiros en modo de prueba
// ---------------------------------------------------------------------------------------------

@Composable
private fun SettleSheet(row: JsonObject, onDismiss: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    val op = rememberAdminOp()
    var error by remember { mutableStateOf<String?>(null) }
    val currency = row.s("currency")
    AdminSheet(
        "Simular retiro pagado", onDismiss,
        confirmLabel = "Simular pago",
        onConfirm = {
            error = null
            op.run("Retiro marcado como pagado (simulación).", { error = it }, after = onDone) {
                app.api.raw("POST", "/api/v1/integrations/sandbox/withdrawals/${row.s("id")}/settle", buildJsonObject { })
            }
        },
        busy = op.busy,
        subtitle = "Esta acción simula la confirmación bancaria del retiro. No mueve dinero real.",
        error = error,
    ) {
        Column(Modifier.fillMaxWidth().background(Vinko.Cream, RoundedCornerShape(14.dp)).padding(12.dp)) {
            Field("Titular", row.s("owner_name") ?: "—")
            Field("Importe", money(row.l("amount_minor"), currency))
            Field("Comisión", money(row.l("fee_minor"), currency))
            Field("Neto a pagar", money(row.l("net_minor"), currency), strong = true)
            row.s("destination_label")?.let { Field("Destino", it) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Vistas de detalle (expedientes de la web)
// ---------------------------------------------------------------------------------------------

@Composable
private fun SummaryBox(vararg items: Pair<String, @Composable () -> Unit>) {
    Row(
        Modifier.fillMaxWidth().background(Vinko.Cream, RoundedCornerShape(14.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { (label, content) ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Vinko.Muted)
                content()
            }
        }
    }
}

@Composable
private fun ContractDetailSheet(c: JsonObject, onDismiss: () -> Unit) {
    AdminSheet("Expediente de Servicio ${shortId(c.s("id"))}", onDismiss) {
        SummaryBox(
            "Precio pactado" to { Text(money(c.l("agreed_price_minor"), c.s("currency")), style = MaterialTheme.typography.titleMedium) },
            "Estado" to { AdminBadge("contracts", c.s("status")) },
            "Modalidad" to { Tag(modeLabel(c.s("delivery_mode"))) },
        )
        Column {
            Field("Requerimiento", c.s("request_title") ?: "—")
            Field("Categoría", c.s("category_name") ?: "—")
            Field("Protección de pago", if (c.s("payment_protection") == "guaranteed") "Custodia en Escrow" else "Pago Directo Externo")
            Field("Cliente", listOfNotNull(c.s("customer_name"), c.s("customer_email")).joinToString("\n"))
            Field("Profesional", listOfNotNull(c.s("professional_name"), c.s("professional_email")).joinToString("\n"))
            Field("Plan activo", planLabel(c.s("plan_code")))
            Field("Comisión estimada", contractFeeText(c.s("plan_code"), c.s("delivery_mode"), long = true), valueColor = Vinko.Primary)
            Field("Fecha aceptado", date(c.s("accepted_at")).ifEmpty { "—" })
            c.s("completed_at")?.let { Field("Fecha completado", date(it)) }
            Field("ID", c.s("id").orEmpty(), mono = true)
        }
        if (c.s("cancelled_at") != null) Banner("Cancelado: ${c.s("cancellation_reason") ?: "Sin motivo registrado"}")
    }
}

@Composable
private fun OrderDetailSheet(o: JsonObject, onDismiss: () -> Unit) {
    AdminSheet("Detalle de Pedido ${shortId(o.s("id"))}", onDismiss) {
        SummaryBox(
            "Total" to { Text(money(o.l("total_minor"), o.s("currency")), style = MaterialTheme.typography.titleMedium) },
            "Estado" to { AdminBadge("orders", o.s("status")) },
            "Pago" to { AdminBadge("orders_payment", o.s("payment_status")) },
        )
        Column {
            Field("Comercio vendedor", o.s("store_name") ?: "—")
            Field("Titular de la tienda", listOfNotNull(o.s("seller_name"), o.s("seller_email")).joinToString("\n"))
            Field("Comprador", listOfNotNull(o.s("customer_name"), o.s("customer_email")).joinToString("\n"))
            Field("Artículos incluidos", "${o.l("items_count") ?: 0} producto(s)")
            Field("Despacho / entrega", o.s("delivery_contact") ?: "No especificado")
            Field("Fecha del pedido", date(o.s("created_at")))
            Field("ID de transacción", o.s("id").orEmpty(), mono = true)
        }
    }
}

@Composable
private fun StoreDetailSheet(s: JsonObject, onDismiss: () -> Unit) {
    AdminSheet("Comercio: ${s.s("name").orEmpty()}", onDismiss) {
        SummaryBox(
            "Productos" to { Text("${s.l("products_count") ?: 0}", style = MaterialTheme.typography.titleMedium) },
            "Pedidos" to { Text("${s.l("orders_count") ?: 0}", style = MaterialTheme.typography.titleMedium) },
            "Estado" to { AdminBadge("stores", s.s("status")) },
        )
        Column {
            Field("Titular", listOfNotNull(s.s("owner_name"), s.s("owner_email")).joinToString("\n"))
            Field("Enlace / slug", s.s("slug")?.let { "marketplace/$it" } ?: "—")
            Field("Plan de membresía", planLabel(s.s("plan_code")))
            Field("Fecha de apertura", date(s.s("created_at")))
        }
        s.s("description")?.takeIf { it.isNotBlank() }?.let {
            Text("Descripción de la tienda", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Visor de archivos moderados (carga con motivo y muestra en pantalla completa)
// ---------------------------------------------------------------------------------------------

@Composable
fun FileViewerLoader(fileId: String, title: String, reason: String, onClose: () -> Unit) {
    val app = LocalApp.current
    val file = com.soyvinko.app.ui.common.rememberResource(fileId) { app.fetchModeratedFile(fileId, reason) }
    val data = file.data
    if (data != null) {
        ModeratedViewer(title, data, onClose)
    } else {
        AdminSheet(title, onClose) {
            if (file.loading) com.soyvinko.app.ui.common.LoadingBox()
            else Banner(file.error ?: "No pudimos abrir el archivo.")
        }
    }
}

/** Fila de un usuario actual para ocultar acciones sobre la propia cuenta. */
@Composable
fun isMe(id: String?): Boolean = id != null && id == LocalUser.current.id

