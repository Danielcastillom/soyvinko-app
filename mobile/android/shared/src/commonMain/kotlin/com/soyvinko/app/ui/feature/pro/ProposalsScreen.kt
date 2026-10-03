package com.soyvinko.app.ui.feature.pro

import com.soyvinko.app.ui.common.nowInstant
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.relative
import com.soyvinko.app.ui.common.rememberAction
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject

@Composable
fun ProposalsScreen(nav: NavHostController) {
    val app = LocalApp.current
    val user = LocalUser.current
    if (!user.isProfessional) {
        NotProfessional(nav, "Mis propuestas")
        return
    }
    val data = rememberResource { app.ops.call("my_proposals") }
    val action = rememberAction()
    var filter by rememberSaveable { mutableStateOf("all") }
    var withdrawing by remember { mutableStateOf<JsonObject?>(null) }

    ScreenScaffold("Mis propuestas", onBack = { nav.popBackStack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ResourceView(data) { raw ->
                val all = raw.objects()
                val list = when (filter) {
                    "pending" -> all.filter { it.s("status") == "pending" }
                    "accepted" -> all.filter { it.s("status") == "accepted" }
                    "closed" -> all.filter { it.s("status") == "rejected" || it.s("status") == "withdrawn" }
                    else -> all
                }
                Column {
                    ChoiceChips(
                        listOf(
                            "all" to "Todas (${all.size})",
                            "pending" to "En espera (${all.count { it.s("status") == "pending" }})",
                            "accepted" to "Aceptadas",
                            "closed" to "Cerradas",
                        ),
                        filter,
                        { filter = it },
                        Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                    if (list.isEmpty()) {
                        if (all.isEmpty()) {
                            EmptyBox(
                                "Todavía no has enviado propuestas",
                                "Explora las solicitudes de tu zona y envía tu primera propuesta. Es gratis.",
                                "Ver oportunidades",
                            ) { nav.navigate(Routes.OPPORTUNITIES) }
                        } else {
                            EmptyBox("Sin propuestas en este filtro", "Prueba con otro filtro para ver el resto.")
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(list, key = { it.s("id") ?: it.toString() }) { p ->
                                ProposalCard(
                                    p,
                                    busy = action.busy,
                                    onWithdraw = { withdrawing = p },
                                    onContracts = { nav.navigate(Routes.CONTRACTS) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    withdrawing?.let { p ->
        ConfirmDialog(
            title = "¿Retirar esta propuesta?",
            text = "El cliente dejará de verla y no podrás enviar otra para la misma solicitud.",
            confirm = "Retirar propuesta",
            danger = true,
            onDismiss = { withdrawing = null },
            onConfirm = {
                action.run("Propuesta retirada.") {
                    app.ops.call("withdraw_proposal", "proposal" to p.s("id"))
                    data.reload()
                }
            },
        )
    }
}

@Composable
private fun ProposalCard(p: JsonObject, busy: Boolean, onWithdraw: () -> Unit, onContracts: () -> Unit) {
    val status = p.s("status")
    val expired = status == "pending" && parseInstant(p.s("expires_at"))?.let { it < nowInstant() } == true
    VCard {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                p.s("title") ?: "Solicitud",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            when {
                expired -> Pill("Vencida", Vinko.Coral, Vinko.CoralBg)
                status == "accepted" -> Pill(proposalLabel(status), Vinko.Mint, Vinko.MintBg)
                status == "pending" -> Pill("En espera", Vinko.Amber, Vinko.AmberBg)
                status == "rejected" -> Pill(proposalLabel(status), Vinko.Muted, Vinko.Tint)
                else -> Pill(proposalLabel(status), Vinko.Muted, Vinko.Tint)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(money(p.l("price_minor"), p.s("currency")?.trim()), style = MaterialTheme.typography.titleLarge, color = Vinko.Primary)
        Spacer(Modifier.height(6.dp))
        Text(p.s("message") ?: "", style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text(
            "Enviada ${relative(p.s("created_at")).replaceFirstChar { it.lowercase() }} · Vigente hasta ${date(p.s("expires_at"))}",
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Subtle,
        )
        when (status) {
            "pending" -> if (!expired) {
                Spacer(Modifier.height(12.dp))
                SmallButton("Retirar propuesta", onClick = onWithdraw, icon = Icons.AutoMirrored.Outlined.Undo, tone = Tone.Danger, enabled = !busy)
            }
            "accepted" -> {
                Spacer(Modifier.height(12.dp))
                Text(
                    "¡El cliente te eligió! El contrato digital está en Servicios.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Mint,
                )
                Spacer(Modifier.height(8.dp))
                SmallButton("Ver mis servicios", onClick = onContracts, icon = Icons.Outlined.WorkOutline, tone = Tone.Soft)
            }
        }
    }
}
