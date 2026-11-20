package com.soyvinko.app.ui.feature.admin

import com.soyvinko.app.ui.common.formatInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.common.ErrorBox
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject

/** Indicador de api.admin_overview y la sección a la que lleva. */
private data class Kpi(val key: String, val title: String, val icon: ImageVector, val section: String?, val alert: Boolean = false)

// Mismos indicadores que la web, más los pendientes que también devuelve admin_overview.
private val KPIS = listOf(
    Kpi("contracts", "Servicios Contratados", Icons.Outlined.Handshake, "contracts"),
    Kpi("active_contracts", "Servicios en curso", Icons.Outlined.Shield, "contracts"),
    Kpi("stores", "Comercios Marketplace", Icons.Outlined.Storefront, "stores"),
    Kpi("active_stores", "Comercios activos", Icons.Outlined.Storefront, "stores"),
    Kpi("orders", "Pedidos Marketplace", Icons.Outlined.ShoppingBag, "orders"),
    Kpi("products", "Inventario activo", Icons.Outlined.Inventory2, "products"),
    Kpi("professionals", "Profesionales", Icons.Outlined.Badge, "users"),
    Kpi("users", "Usuarios totales", Icons.Outlined.People, "users"),
    Kpi("pending_requests", "Solicitudes en revisión", Icons.AutoMirrored.Outlined.Assignment, "requests", alert = true),
    Kpi("pending_kyc", "Identidad por revisar", Icons.Outlined.VerifiedUser, "kyc", alert = true),
    Kpi("open_tickets", "Casos abiertos", Icons.Outlined.SupportAgent, "tickets", alert = true),
)

/** Pendientes que se muestran como contador en la tarjeta de cada sección. */
private val PENDING_BY_SECTION = mapOf("requests" to "pending_requests", "kyc" to "pending_kyc", "tickets" to "open_tickets")

private fun count(n: Long?): String = formatInt(n ?: 0L)

/** Inicio del panel: indicadores (platform.read) y las secciones permitidas para la persona. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminHomeScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onCategories: () -> Unit) {
    val app = LocalApp.current
    val user = LocalUser.current
    val canStats = user.can("platform.read")
    val overview = rememberResource(canStats) { if (canStats) app.ops.call("admin_overview").obj() else null }
    val sections = allowedSections(user)
    var pulling by remember { mutableStateOf(false) }
    LaunchedEffect(overview.loading) { if (!overview.loading) pulling = false }

    ScreenScaffold(
        "Administración",
        onBack = onBack,
        actions = {
            if (canStats) IconButton(onClick = overview.reload) { Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar", tint = Vinko.Primary) }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = pulling,
            onRefresh = { pulling = true; overview.reload() },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Hero(user.roles.filter { it in setOf("superadmin", "admin", "support") })

                if (canStats) {
                    SectionLabel("Indicadores de la plataforma")
                    val stats = overview.data
                    when {
                        stats != null -> Stats(stats) { key -> if (sections.any { it.key == key }) onOpen(key) }
                        overview.loading -> LoadingBox()
                        else -> ErrorBox(overview.error ?: "Inténtalo de nuevo.", overview.reload)
                    }
                }

                SectionLabel("Secciones")
                if (sections.isEmpty()) {
                    Banner("Tu rol no tiene secciones administrativas asignadas. Pide acceso a un superadministrador.", BannerTone.Info)
                } else {
                    if (!canStats) Banner(
                        "Tu rol te da acceso a ${sections.size} ${if (sections.size == 1) "sección" else "secciones"} del panel.",
                        BannerTone.Info,
                    )
                    TwoColumns(sections) { s ->
                        SectionTile(s, PENDING_BY_SECTION[s.key]?.let { overview.data.l(it) }) { onOpen(s.key) }
                    }
                }

                if (user.can("catalog.manage")) {
                    SectionLabel("Catálogo")
                    WideTile(Icons.Outlined.Category, "Categorías de servicios", "Crea o edita las categorías del catálogo", onCategories)
                }

                Text(
                    "Las consultas de datos personales, documentos y casos quedan registradas en auditoría con su motivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Subtle,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hero(roles: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Vinko.Dark, Color(0xFF2E155C))), RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("PANEL DE CONTROL GENERAL", style = MaterialTheme.typography.labelSmall, color = Vinko.Lilac)
        Text("Administración & Modelo de Negocio", fontFamily = Manrope, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Text(
            "Supervisa servicios contratados, comercios del marketplace, inventarios, planes de membresía y comisiones.",
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Lilac,
        )
        if (roles.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                roles.forEach { Tag(roleLabel(it), fg = Color.White, bg = Color.White.copy(alpha = 0.14f)) }
            }
        }
    }
}

@Composable
private fun Stats(stats: JsonObject, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TwoColumns(KPIS) { k ->
            val value = stats.l(k.key) ?: 0L
            val hot = k.alert && value > 0
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, if (hot) Vinko.Lilac else Vinko.Line, RoundedCornerShape(16.dp))
                    .clickable(enabled = k.section != null) { k.section?.let(onOpen) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(k.icon, contentDescription = null, tint = if (hot) Vinko.Coral else Vinko.Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(k.title, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(count(value), fontFamily = Manrope, style = MaterialTheme.typography.headlineSmall, color = if (hot) Vinko.Coral else Vinko.Ink)
            }
        }
        Column(
            Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, Vinko.Line, RoundedCornerShape(16.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.TrendingUp, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Volumen transaccionado", style = MaterialTheme.typography.titleSmall)
            }
            Field("Ventas del marketplace", money(stats.l("marketplace_sales_minor") ?: 0L, null), strong = true)
            Field("Servicios acordados", money(stats.l("contracts_volume_minor") ?: 0L, null), strong = true)
            Text("Importes sumados en todas las monedas, sin conversión.", style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle)
        }
    }
}

/** Rejilla de dos columnas con alturas iguales por fila. */
@Composable
private fun <T> TwoColumns(items: List<T>, cell: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { Box(Modifier.weight(1f).fillMaxHeight()) { cell(it) } }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionTile(s: AdminSection, pending: Long?, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconBox(s.icon, size = 38.dp)
            Spacer(Modifier.weight(1f))
            if (pending != null && pending > 0) CountDot(pending)
        }
        Text(s.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(s.subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WideTile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(icon, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Vinko.Lilac)
    }
}
