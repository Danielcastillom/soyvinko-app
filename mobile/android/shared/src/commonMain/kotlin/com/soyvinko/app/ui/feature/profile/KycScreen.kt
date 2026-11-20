package com.soyvinko.app.ui.feature.profile

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiJson
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.StatusBadge
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject

val KYC_DOC_LABELS = mapOf(
    "identity_front" to "Cédula (anverso)",
    "identity_back" to "Cédula (reverso)",
    "selfie" to "Selfie con documento",
    "proof_of_address" to "Comprobante de domicilio",
    "other" to "Documento adicional",
)

/** Datos que la verificación guarda en external_reference (JSON con lo leído por OCR). */
fun kycMeta(case: JsonObject): JsonObject? =
    case.s("external_reference")?.takeIf { it.trimStart().startsWith("{") }?.let { runCatching { ApiJson.parseToJsonElement(it).obj() }.getOrNull() }

/** Estado de la verificación de identidad e historial. */
@Composable
fun KycScreen(onBack: () -> Unit, onStart: () -> Unit) {
    val app = LocalApp.current
    val cases = rememberResource { app.ops.call("my_kyc").objects() }
    val account = rememberResource { app.ops.call("my_account").obj() }
    ScreenScaffold("Verificación de identidad", onBack = onBack) { padding ->
        ResourceView(cases) { list ->
            val latest = list.firstOrNull()
            ProfilePage(padding) {
                when (kycStateOf(latest)) {
                    KycState.None -> IntroCard(onStart)
                    KycState.Approved -> ApprovedCard(latest!!, account.data)
                    KycState.Pending -> PendingCard(latest!!)
                    KycState.Expired -> RetryCard(
                        latest!!, Icons.Outlined.WarningAmber, "Tu verificación venció",
                        "Envía una nueva verificación con tu cédula vigente para seguir retirando y conservar la insignia.",
                        Vinko.Amber, Vinko.AmberBg, "Renovar verificación", onStart,
                    )
                    KycState.Rejected -> RetryCard(
                        latest!!, Icons.Outlined.ErrorOutline,
                        if (latest.s("status") == "revoked") "Verificación revocada" else "Verificación no aprobada",
                        "Tu solicitud anterior no pudo ser aprobada por el equipo de cumplimiento.",
                        Vinko.Coral, Vinko.CoralBg, "Reenviar verificación", onStart,
                    )
                }
                if (list.size > 1) {
                    SectionLabel("Solicitudes anteriores")
                    VCard(padding = 6.dp) {
                        list.drop(1).forEachIndexed { i, k ->
                            if (i > 0) Divider()
                            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Solicitud del ${date(k.s("submitted_at"))}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    StatusBadge(k.s("status"))
                                }
                                k.s("reason")?.let {
                                    Spacer(Modifier.height(4.dp))
                                    Hint("Motivo: $it")
                                }
                            }
                        }
                    }
                }
                if (kycStateOf(latest) != KycState.Approved) RequirementsCard()
            }
        }
    }
}

@Composable
private fun StatusHeader(icon: ImageVector, title: String, text: String, fg: Color, bg: Color, status: String?) {
    Row(verticalAlignment = Alignment.Top) {
        com.soyvinko.app.ui.common.IconTile(icon, fg, bg, size = 52.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(2.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
            if (status != null) {
                Spacer(Modifier.height(8.dp))
                StatusBadge(status)
            }
        }
    }
}

@Composable
private fun IntroCard(onStart: () -> Unit) {
    VCard {
        StatusHeader(
            Icons.Outlined.VerifiedUser,
            "Verifica tu identidad",
            "Solo necesitas tu cédula vigente y una selfie sosteniéndola. Toma unos 3 minutos.",
            Vinko.Primary, Vinko.Tint, null,
        )
        Spacer(Modifier.height(16.dp))
        Benefit(Icons.Outlined.Payments, "Retiros a bancos venezolanos", "Retira tus ganancias por Pago Móvil y transferencia (Banesco, Mercantil, BDV y más).", Vinko.Mint, Vinko.MintBg)
        Benefit(Icons.Outlined.VerifiedUser, "Insignia de perfil verificado", "Genera más confianza en clientes y profesionales con tu identidad validada.", Vinko.Primary, Vinko.Tint)
        Benefit(Icons.Outlined.Shield, "Lectura automática de tu cédula", "Leemos tus datos de la foto para que no tengas que escribirlos.", Vinko.Cyan, Vinko.CyanBg)
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Comenzar verificación", onClick = onStart)
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, text: String, fg: Color, bg: Color) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        com.soyvinko.app.ui.common.IconTile(icon, fg, bg, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Hint(text)
        }
    }
}

@Composable
private fun ApprovedCard(case: JsonObject, account: JsonObject?) {
    val meta = kycMeta(case)
    VCard {
        StatusHeader(
            Icons.Outlined.VerifiedUser,
            "Identidad verificada",
            "Tu cuenta cumple con la validación de identidad para Venezuela.",
            Vinko.Mint, Vinko.MintBg, "approved",
        )
        Spacer(Modifier.height(14.dp))
        InfoRow("Titular", meta?.s("full_name") ?: account?.s("name").orEmpty())
        val document = meta?.let { "${it.s("document_type") ?: "V"}-${it.s("document_number").orEmpty()}" } ?: account?.s("rif")
        document?.let { InfoRow("Documento", it) }
        meta?.s("birthdate")?.let { InfoRow("Nacimiento", isoToDisplay(it)) }
        case.s("expires_at")?.let { InfoRow("Válido hasta", date(it)) }
        Spacer(Modifier.height(10.dp))
        listOf(
            "Retiros bancarios nacionales y Pago Móvil en VES habilitados",
            "Distintivo de verificación activo en tu perfil",
            "Límites de custodia y transferencias ampliados",
        ).forEach { Perk(it) }
        Documents(case)
    }
}

@Composable
private fun Perk(text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.padding(top = 3.dp).size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PendingCard(case: JsonObject) {
    VCard {
        StatusHeader(
            Icons.Outlined.HourglassTop,
            "Verificación en revisión",
            "Recibimos tus datos y documentos. El equipo de cumplimiento está revisando tu expediente.",
            Vinko.Amber, Vinko.AmberBg, case.s("status"),
        )
        Spacer(Modifier.height(14.dp))
        InfoRow("Fecha de envío", date(case.s("submitted_at")))
        InfoRow("Tiempo estimado", "24 a 48 horas hábiles")
        InfoRow("Expediente", case.s("id")?.take(8)?.uppercase().orEmpty())
        kycMeta(case)?.let { meta ->
            meta.s("full_name")?.let { InfoRow("Titular", it) }
            meta.s("document_number")?.let { InfoRow("Documento", "${meta.s("document_type") ?: "V"}-$it") }
        }
        Documents(case)
        Spacer(Modifier.height(12.dp))
        Banner("Te avisaremos por correo cuando revisemos tu documentación. No necesitas hacer nada más.", BannerTone.Info)
    }
}

@Composable
private fun RetryCard(
    case: JsonObject,
    icon: ImageVector,
    title: String,
    text: String,
    fg: Color,
    bg: Color,
    action: String,
    onStart: () -> Unit,
) {
    VCard {
        StatusHeader(icon, title, text, fg, bg, case.s("status"))
        case.s("reason")?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(14.dp))
            Banner("Motivo indicado: $it")
        }
        case.s("submitted_at")?.let {
            Spacer(Modifier.height(8.dp))
            InfoRow("Solicitud enviada", date(it))
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(action, onClick = onStart)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Documents(case: JsonObject) {
    val docs = case.list("documents")
    if (docs.isEmpty()) return
    Spacer(Modifier.height(12.dp))
    Text("Documentos en el expediente", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
    Spacer(Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        docs.forEach { d -> Pill(KYC_DOC_LABELS[d.s("kind")] ?: "Documento", Vinko.Ink, Vinko.Tint, Icons.Outlined.Description) }
    }
}

@Composable
private fun RequirementsCard() {
    VCard {
        CardHeader(Icons.Outlined.Badge, "Requisitos de la verificación")
        Spacer(Modifier.height(10.dp))
        listOf(
            "Documento de identidad" to "Cédula venezolana (V o E) vigente o pasaporte.",
            "Calidad de imagen" to "Fotos legibles, sin flash sobre los datos, con los 4 bordes visibles.",
            "Selfie de autenticidad" to "Sostén tu documento a la altura del pecho, mirando a la cámara.",
            "Confidencialidad" to "Solo personal de cumplimiento autorizado revisa tus datos para prevenir fraudes.",
        ).forEachIndexed { i, (title, text) ->
            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                Text("${i + 1}.", style = MaterialTheme.typography.titleSmall, color = Vinko.Primary, modifier = Modifier.width(22.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Hint(text)
                }
            }
        }
    }
}

/** "1990-05-21" → "21/05/1990". */
fun isoToDisplay(iso: String): String {
    val parts = iso.take(10).split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else iso
}
