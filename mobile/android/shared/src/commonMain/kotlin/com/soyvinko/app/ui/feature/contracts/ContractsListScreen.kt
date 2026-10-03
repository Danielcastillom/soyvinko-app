package com.soyvinko.app.ui.feature.contracts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.ErrorBox
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.StatusBadge
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.date
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.statusLabel
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject

private enum class Filter(val label: String) { Active("Activos"), Done("Finalizados"), All("Todos") }

private fun Filter.matches(status: String?) = when (this) {
    Filter.Active -> status != "completed" && status != "cancelled"
    Filter.Done -> status == "completed" || status == "cancelled"
    Filter.All -> true
}

/** Pestaña "Servicios": contratos como cliente y como profesional (api.my_contracts). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractsListScreen(nav: NavHostController) {
    val app = LocalApp.current
    val user = LocalUser.current
    val contracts = rememberResource { app.ops.call("my_contracts").objects() }
    PollWhileVisible(20_000) { contracts.reload() }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var pulling by remember { mutableStateOf(false) }
    LaunchedEffect(contracts.loading) { if (!contracts.loading) pulling = false }

    ScreenScaffold("Servicios contratados") { padding ->
        PullToRefreshBox(
            isRefreshing = pulling,
            onRefresh = { pulling = true; contracts.reload() },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            val all = contracts.data
            when {
                all == null && contracts.loading -> LoadingBox()
                all == null -> ErrorBox(contracts.error ?: "Inténtalo de nuevo.", contracts.reload)
                else -> {
                    val current = Filter.entries[filter]
                    val visible = all.filter { current.matches(it.s("status")) }
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            Text(
                                "Acuerdos, conversaciones, ubicación y pagos de tus servicios, en un solo lugar.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Vinko.Muted,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Filter.entries.forEachIndexed { i, f ->
                                    FilterTab("${f.label} · ${all.count { f.matches(it.s("status")) }}", selected = i == filter) { filter = i }
                                }
                            }
                        }
                        if (visible.isEmpty()) item {
                            if (all.isEmpty()) {
                                EmptyBox(
                                    "Aquí empieza el trabajo en equipo",
                                    "Cuando ambas partes acepten una solicitud, encontrarás el servicio, su chat y la ubicación aquí.",
                                    actionLabel = if (user.isProfessional) "Ver oportunidades" else "Explorar profesionales",
                                    onAction = {
                                        if (user.isProfessional) nav.navigate(Routes.OPPORTUNITIES)
                                        else nav.navigate(Routes.EXPLORE) {
                                            popUpTo(nav.graph.findStartDestination().route.orEmpty()) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                )
                            } else {
                                EmptyBox(
                                    if (current == Filter.Active) "Sin servicios en curso" else "Aún no hay servicios finalizados",
                                    if (current == Filter.Active) "Tus servicios activos aparecerán aquí con su avance y próximos pasos."
                                    else "Los servicios completados o cancelados quedarán aquí como historial.",
                                )
                            }
                        }
                        items(visible, key = { it.s("id") ?: it.hashCode().toString() }) { c ->
                            ContractCard(c) { c.s("id")?.let { nav.navigate(Routes.contract(it)) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Color.White else Vinko.Ink,
        modifier = Modifier
            .background(if (selected) Vinko.Primary else Color.White, RoundedCornerShape(50))
            .border(1.dp, if (selected) Vinko.Primary else Vinko.Line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
private fun ContractCard(c: JsonObject, onClick: () -> Unit) {
    val status = c.s("status")
    val customer = c.b("is_customer")
    val counterpart = if (customer) c.s("professional_name") else c.s("customer_name")
    val steps = stepsFor(c.s("delivery_mode"))
    val reached = reachedStep(steps, status) { c.s(it) }
    val progress = if (status == "completed") 1f else (reached + 1f) / steps.size
    val troubled = status == "cancelled" || status == "disputed"
    val remote = c.s("delivery_mode") == "remote"

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(if (remote) Icons.Outlined.Language else Icons.Outlined.Place)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    c.s("title") ?: c.s("work_requirements")?.take(75) ?: "Servicio contratado",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    (if (customer) "Profesional: " else "Cliente: ") + (counterpart ?: "—"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusBadge(status)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Pill(
                if (customer) "Eres cliente" else "Eres profesional",
                fg = Vinko.Ink, bg = Vinko.Cream,
                icon = if (customer) Icons.Outlined.Person else Icons.Outlined.BusinessCenter,
            )
            Pill(modeLabel(c.s("delivery_mode")), fg = Vinko.Cyan, bg = Vinko.CyanBg)
            if (c.s("payment_protection") == "guaranteed") Pill("Pago protegido", fg = Vinko.Mint, bg = Vinko.MintBg, icon = Icons.Outlined.VerifiedUser)
            else Pill("Pago directo", fg = Vinko.Amber, bg = Vinko.AmberBg, icon = Icons.Outlined.Handshake)
        }
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = if (troubled) Vinko.Coral else if (status == "completed") Vinko.Mint else Vinko.Primary,
            trackColor = Vinko.Tint,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (troubled) statusLabel(status) + " · último paso: " + steps[reached].label
            else "Paso ${reached + 1} de ${steps.size} · ${steps[reached].label}",
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Muted,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(money(c.l("agreed_price_minor"), c.s("currency")), fontFamily = Manrope, style = MaterialTheme.typography.titleLarge, color = Vinko.Ink)
                Text("Desde el " + date(c.s("accepted_at")), style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Vinko.Lilac, modifier = Modifier.size(24.dp))
        }
    }
}
