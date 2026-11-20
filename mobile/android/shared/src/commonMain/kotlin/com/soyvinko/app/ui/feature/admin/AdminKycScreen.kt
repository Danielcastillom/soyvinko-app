package com.soyvinko.app.ui.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.dateTime
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/** Estado de la consulta de un documento del expediente. */
private sealed interface DocState {
    data object Loading : DocState
    data class Ready(val file: ModeratedFile) : DocState
    data class Failed(val message: String) : DocState
}

/**
 * Expediente de identidad (api.kyc_case con el motivo de la consulta): estado, datos del titular y
 * documentos. Cada documento se consulta aparte con /api/v1/files/moderation/{id} (queda en auditoría).
 */
@Composable
fun AdminKycCaseScreen(id: String, reason: String, onBack: () -> Unit) {
    val app = LocalApp.current
    val user = LocalUser.current
    val case = rememberResource(id) {
        app.ops.call("kyc_case", "case" to id, "reason" to reason).obj()
            ?: throw ApiException("No encontramos este expediente.", "not_found", 404)
    }
    val docs = remember { mutableStateMapOf<String, DocState>() }
    var reviewing by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Pair<String, ModeratedFile>?>(null) }
    val scope = rememberCoroutineScope()
    val k = case.data
    val canReview = k != null && user.can("kyc.review") && k.s("user_id") != user.id

    fun load(fileId: String) {
        docs[fileId] = DocState.Loading
        scope.launch(Dispatchers.Main.immediate) {
            docs[fileId] = try {
                DocState.Ready(app.fetchModeratedFile(fileId, KYC_DOC_REASON))
            } catch (e: ApiException) {
                DocState.Failed(adminError(e))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            "Expediente de identidad",
            onBack = onBack,
            actions = { IconButton(onClick = case.reload) { Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar", tint = Vinko.Primary) } },
            bottomBar = {
                if (canReview) Box(Modifier.background(Color.White).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    PrimaryButton("Resolver verificación", onClick = { reviewing = true })
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                ResourceView(case) { c ->
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CaseHeader(c)
                        Holder(c)
                        Banner("Consulta registrada en auditoría. Motivo: “$reason”", BannerTone.Info)
                        val documents = c.list("documents")
                        SectionLabel("Documentos adjuntos (${documents.size})")
                        if (documents.isEmpty()) {
                            Text("El usuario aún no adjuntó documentos.", style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
                        }
                        documents.forEach { d ->
                            val fileId = d.s("file_id").orEmpty()
                            val label = KYC_DOCS[d.s("kind")] ?: d.s("kind") ?: "Documento"
                            DocumentCard(label, fileId, docs[fileId], onLoad = { load(fileId) }, onOpen = { f -> viewing = label to f })
                        }
                    }
                }
            }
        }
        if (reviewing && k != null) ReviewKycSheet(k, onDismiss = { reviewing = false }, onDone = { reviewing = false; case.reload() })
        viewing?.let { (title, file) -> ModeratedViewer(title, file) { viewing = null } }
    }
}

@Composable
private fun CaseHeader(c: JsonObject) {
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(Icons.Outlined.VerifiedUser)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(kycProviderLabel(c.s("provider")), style = MaterialTheme.typography.titleMedium)
                Text("Expediente ${shortId(c.s("id"))}", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
            }
            AdminBadge("kyc", c.s("status"))
        }
        Column(Modifier.padding(top = 10.dp)) {
            Field("Presentado", dateTime(c.s("submitted_at")).ifEmpty { "—" })
            c.s("reviewed_at")?.let { Field("Revisado", dateTime(it)) }
            c.s("expires_at")?.let { Field("Válido hasta", date(it)) }
        }
        c.s("reason")?.let {
                Text(
                "Motivo: $it",
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Ink,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth().background(Vinko.Cream, androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).padding(10.dp),
            )
        }
    }
}

/** "Datos del Titular" leídos de external_reference (JSON de la verificación personal). */
@Composable
private fun Holder(c: JsonObject) {
    val meta = kycMeta(c) ?: return
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(Icons.Outlined.Badge)
            Spacer(Modifier.width(12.dp))
            Text("Datos del Titular", style = MaterialTheme.typography.titleMedium)
        }
        Column(Modifier.padding(top = 10.dp)) {
            val doc = listOfNotNull(meta.s("document_type"), meta.s("document_number")).joinToString("-")
            if (doc.isNotEmpty()) Field("Documento", doc, strong = true)
            meta.s("full_name")?.let { Field("Nombre", it, strong = true) }
            meta.s("birthdate")?.let { Field("Nacimiento", it) }
            meta.s("phone")?.let { Field("Teléfono", it) }
            meta.s("address")?.let { Field("Dirección", it) }
            meta.s("account_type")?.let { Field("Tipo de cuenta", if (it == "juridico") "Empresa (jurídica)" else "Persona natural") }
            meta.s("expiration_date")?.let { Field("Vencimiento del documento", it, valueColor = if (meta.b("is_expired")) Vinko.Coral else Vinko.Ink) }
            if (meta.containsKey("ocr_success")) {
                Field(
                    "Lectura automática",
                    if (meta.b("ocr_success")) "Datos leídos del documento" else "Revisar manualmente",
                    valueColor = if (meta.b("ocr_success")) Vinko.Mint else Vinko.Amber,
                )
            }
        }
        if (meta.b("is_expired")) Banner("El documento presentado está vencido.")
    }
}

@Composable
private fun DocumentCard(label: String, fileId: String, state: DocState?, onLoad: () -> Unit, onOpen: (ModeratedFile) -> Unit) {
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(Icons.Outlined.Description)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text("ID: ${fileId.take(18)}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Vinko.Subtle)
            }
        }
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state) {
                null -> CardAction("Consultar documento", onLoad, Icons.Outlined.Visibility)
                DocState.Loading -> CardAction("Consultando…", {}, loading = true)
                is DocState.Failed -> {
                    Banner(state.message)
                    CardAction("Reintentar", onLoad, Icons.Outlined.Refresh)
                }
                is DocState.Ready -> ModeratedPreview(state.file, onOpen = { onOpen(state.file) })
            }
        }
    }
}
