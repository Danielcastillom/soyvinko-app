package com.soyvinko.app.ui.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.ErrorBox
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

const val PAGE_SIZE = 25

/**
 * Listado paginado de api.admin_list (limit/offset, igual que la web con 25 por página). "Cargar más"
 * agrega la página siguiente; recargar vuelve a la primera. Planes no admite paginación en el SQL.
 */
class AdminPager(
    private val scope: CoroutineScope,
    private val paginated: Boolean,
    private val fetch: suspend (offset: Int) -> List<JsonObject>,
) {
    val rows = mutableStateListOf<JsonObject>()
    var loading by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loaded by mutableStateOf(false)
        private set
    var hasMore by mutableStateOf(false)
        private set
    private var fetched = 0
    private var generation = 0

    fun refresh() = load(reset = true)

    fun more() {
        if (!loading && hasMore) load(reset = false)
    }

    private fun load(reset: Boolean) {
        val gen = ++generation
        loading = true
        loadingMore = !reset
        if (reset) error = null
        scope.launch(Dispatchers.Main.immediate) {
            try {
                val page = fetch(if (reset) 0 else fetched)
                if (gen != generation) return@launch
                if (reset) {
                    rows.clear()
                    fetched = 0
                }
                fetched += page.size
                val seen = rows.mapNotNull { it.s("id") }.toHashSet()
                rows.addAll(page.filter { r -> r.s("id")?.let(seen::add) ?: true })
                hasMore = paginated && page.size == PAGE_SIZE
                error = null
                loaded = true
            } catch (e: ApiException) {
                if (gen == generation) error = adminError(e)
            } finally {
                if (gen == generation) {
                    loading = false
                    loadingMore = false
                }
            }
        }
    }
}

/** Campo de estado usado para los filtros de cada sección. */
private fun statusKey(resource: String) = when (resource) {
    "reviews", "files" -> "moderation_status"
    "plans", "fees", "audit" -> null
    else -> "status"
}

private fun matches(row: JsonObject, query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim().lowercase()
    return row.values.any { v -> (v as? JsonPrimitive)?.contentOrNull?.lowercase()?.contains(q) == true }
}

/** Sección del panel: listado con tarjetas, filtros locales, paginación y acciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminResourceScreen(
    resource: String,
    onBack: () -> Unit,
    onOpenKyc: (id: String, reason: String) -> Unit,
    onOpenTicket: (id: String) -> Unit,
) {
    val app = LocalApp.current
    val user = LocalUser.current
    val section = sectionOf(resource)
    if (section == null || !user.can(section.permission)) {
        ScreenScaffold(section?.title ?: "Administración", onBack = onBack) { padding ->
            Box(Modifier.padding(padding)) {
                EmptyBox("Sin acceso a esta sección", "Tu rol no incluye el permiso necesario para ver esta información.")
            }
        }
        return
    }
    val scope = rememberCoroutineScope()
    val pager = remember(resource) {
        AdminPager(scope, paginated = resource != "plans") { offset ->
            app.ops.call("admin_list", "resource" to resource, "limit" to PAGE_SIZE, "offset" to offset).objects()
        }
    }
    LaunchedEffect(pager) { pager.refresh() }
    val config = rememberResource(resource) {
        if (resource == "withdrawals") app.api.raw("GET", "/api/v1/config", auth = false).obj() else null
    }
    val sandbox = config.data.s("payment_mode") == "sandbox"
    var dlg by remember { mutableStateOf<AdminDlg?>(null) }
    var query by rememberSaveable(resource) { mutableStateOf("") }
    var status by rememberSaveable(resource) { mutableStateOf<String?>(null) }
    var pulling by remember { mutableStateOf(false) }
    LaunchedEffect(pager.loading) { if (!pager.loading) pulling = false }
    val key = statusKey(resource)

    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(
            section.title,
            onBack = onBack,
            actions = {
                IconButton(onClick = { pager.refresh() }) { Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar", tint = Vinko.Primary) }
            },
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = pulling,
                onRefresh = { pulling = true; pager.refresh() },
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                val rows = pager.rows
                val statuses = if (key == null) emptyList() else rows.mapNotNull { it.s(key) }.groupingBy { it }.eachCount().toList()
                val visible = rows.filter { r -> (status == null || r.s(key ?: "") == status) && matches(r, query) }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "intro") {
                        Text(section.description, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
                    }
                    item(key = "header") {
                        ResourceHeader(resource, rows, sandbox, user) { dlg = it }
                    }
                    when {
                        !pager.loaded && pager.error != null -> item(key = "error") { ErrorBox(pager.error!!) { pager.refresh() } }
                        !pager.loaded -> item(key = "loading") { LoadingBox() }
                        else -> {
                            if (rows.size > 3 && resource !in setOf("fees", "vinko_rates")) item(key = "search") {
                                SearchField(query) { query = it }
                            }
                            if (statuses.size > 1) item(key = "chips") {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ChoiceChip("Todos · ${rows.size}", status == null) { status = null }
                                    statuses.forEach { (value, count) ->
                                        ChoiceChip("${adminStatusLabel(resource, value)} · $count", status == value) { status = value }
                                    }
                                }
                            }
                            if (pager.error != null) item(key = "page-error") { Banner(pager.error!!) }
                            if (visible.isEmpty()) item(key = "empty") {
                                if (rows.isEmpty()) EmptyBox("La cola está al día", "No hay elementos para mostrar en esta página.")
                                else EmptyBox("Sin coincidencias", "Ningún elemento cargado coincide con el filtro.")
                            }
                            itemsIndexed(visible, key = { i, r -> r.s("id") ?: "row-$i" }) { _, row ->
                                ResourceCard(resource, row, user, sandbox, onDlg = { dlg = it }, onOpenTicket = onOpenTicket)
                            }
                            item(key = "footer") {
                                Footer(pager, rows.size)
                            }
                        }
                    }
                }
            }
        }
        dlg?.let { d -> AdminDialogHost(d, onDismiss = { dlg = null }, onDone = { dlg = null; pager.refresh() }, onOpenKyc = onOpenKyc) }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(80)) },
        placeholder = { Text("Buscar en los resultados cargados", color = Vinko.Subtle) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Vinko.Lavender) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = adminFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Footer(pager: AdminPager, count: Int) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            pager.loadingMore -> CircularProgressIndicator(Modifier.size(28.dp), color = Vinko.Primary, strokeWidth = 3.dp)
            pager.hasMore -> SecondaryButton("Cargar más", onClick = { pager.more() })
            count > 0 -> Text(
                "$count ${if (count == 1) "elemento" else "elementos"} · fin de la lista",
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Subtle,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Contenido propio de cada sección sobre la lista (modelo de negocio, simuladores y botones de configuración). */
@Composable
private fun ResourceHeader(resource: String, rows: List<JsonObject>, sandbox: Boolean, user: User, onDlg: (AdminDlg) -> Unit) {
    when (resource) {
        "plans" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BusinessModelCard()
            CommissionSimulator()
            if (user.can("plans.manage")) PrimaryButton("Configurar plan", onClick = { onDlg(AdminDlg.ConfigurePlan(null)) })
        }
        "fees" -> {
            val active = rows.filter { it.b("active") }.mapNotNull { r -> r.s("currency")?.trim()?.let { c -> r.l("basis_points")?.let { c to it } } }.toMap()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WithdrawalFeeSimulator(active)
                PrimaryButton("Configurar comisión", onClick = { onDlg(AdminDlg.ConfigureFee(active)) })
            }
        }
        "vinko_rates" -> {
            val active = rows.filter { it.b("active") }.mapNotNull { r -> r.s("currency")?.trim()?.let { c -> r.l("rate_minor")?.let { c to it } } }.toMap()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("VES", "USD").forEach { c -> RateHero(c, rows.firstOrNull { it.b("active") && it.s("currency")?.trim() == c }) }
                PrimaryButton("Personalizar Tasa de Cambio Vinkos", onClick = { onDlg(AdminDlg.ConfigureRate(active)) })
            }
        }
        "withdrawals" -> if (sandbox) Banner(
            "Modo de pagos de prueba activo: puedes simular la confirmación bancaria de los retiros pendientes. No mueve dinero real.",
            BannerTone.Info,
        )
        else -> Unit
    }
}

/** Tarjeta destacada con la tasa vigente de una moneda (como la web). */
@Composable
private fun RateHero(currency: String, active: JsonObject?) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF1C0F38), Color(0xFF2E155C))), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CurrencyExchange, contentDescription = null, tint = Vinko.Lilac, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "TASA OFICIAL ${currencyName(currency).uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = Vinko.Lilac,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Vigente",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF4ADE80),
                modifier = Modifier
                    .background(Color(0x2222C55E), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x5522C55E), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("1 Vinko =", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(bottom = 4.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                active?.let { money(it.l("rate_minor"), currency) } ?: if (currency == "VES") "Bs 50,00" else "US$ 1,00",
                fontFamily = Manrope,
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFFFD76A),
            )
        }
        Text(
            active.s("reason")?.let { "Motivo: $it" } ?: "Tasa personalizada configurada para el ecosistema SoyVinko",
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Lilac,
        )
    }
}

