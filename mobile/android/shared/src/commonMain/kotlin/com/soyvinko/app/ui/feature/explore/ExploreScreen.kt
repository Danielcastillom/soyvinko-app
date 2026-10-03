package com.soyvinko.app.ui.feature.explore

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.data.Ops
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.ErrorBox
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.screens.HeroGradient
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private val RADII = listOf(5000, 10000, 25000, 50000)

/** Últimos resultados de la sesión, por filtros. */
private object ResultsMemory {
    private var key: List<Any?>? = null
    private var rows: List<Pro>? = null
    fun save(k: List<Any?>, value: List<Pro>) {
        key = k
        rows = value
    }
    fun get(k: List<Any?>): List<Pro>? = rows?.takeIf { key == k }
}

/**
 * Misma lógica que la web (frontend/src/pages/Explore.tsx):
 * - con categoría, presencial y una dirección guardada: search_professionals;
 * - con categoría y remoto: search_remote_professionals;
 * - con un punto de búsqueda (GPS, zona elegida o dirección): nearby_professionals (+ remotos de discover si "Todos");
 * - sin ubicación: discover.
 */
private suspend fun loadProfessionals(
    ops: Ops,
    category: String,
    mode: String,
    address: String,
    sort: String,
    radius: Int,
    verified: Boolean,
    point: GeoPos?,
): List<Pro> {
    val cat = category.ifEmpty { null }
    if (cat != null && mode == "onsite" && address.isNotEmpty()) {
        return ops.call(
            "search_professionals",
            "category" to cat, "address" to address, "radius_m" to radius, "sort" to sort, "verified_only" to verified,
        ).objects().mapNotNull { row ->
            row["profile"].obj()?.toPro()?.copy(distanceM = row.l("distance_m"), sponsored = row.b("sponsored"))
        }
    }
    if (cat != null && mode == "remote") {
        return ops.call("search_remote_professionals", "category" to cat, "verified_only" to verified).pros()
    }
    if (mode != "remote" && point != null) {
        var nearby = ops.call(
            "nearby_professionals",
            "lat" to point.lat, "lon" to point.lon, "category" to cat, "radius_m" to radius, "verified_only" to verified, "limit" to 100,
        ).pros()
        if (sort == "recommended") {
            nearby = nearby.sortedWith(
                compareByDescending<Pro> { it.verified }.thenByDescending { it.rating ?: 0.0 }.thenBy { it.distanceM ?: Long.MAX_VALUE },
            )
        }
        if (mode == "onsite") return nearby
        val remote = ops.call("discover", "limit" to 100, "category" to cat, "mode" to "remote").pros()
        val ids = nearby.map { it.id }.toSet()
        return nearby + remote.filter { it.id !in ids }
    }
    return ops.call("discover", "limit" to 100, "category" to cat, "mode" to mode.ifEmpty { null }).pros()
}

/** Pestaña Explorar: buscar profesionales por categoría, texto, ubicación y filtros, en lista o mapa. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    nav: NavHostController,
    initialLocation: GeoPos? = null,
    autoLocate: Boolean = true,
    startWithMap: Boolean = false,
    startWithFilters: Boolean = false,
) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var category by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("distance") }
    var radius by rememberSaveable { mutableIntStateOf(10000) }
    var verified by rememberSaveable { mutableStateOf(false) }
    var showMap by rememberSaveable { mutableStateOf(startWithMap) }
    var showFilters by remember { mutableStateOf(startWithFilters) }
    var chooseZone by remember { mutableStateOf(false) }

    val geo = rememberDeviceLocation(initialLocation, autoLocate)
    val zone = rememberManualZone(persist = autoLocate && initialLocation == null)

    val cats = rememberResource { app.ops.call("catalog").categories() }
    val account = rememberResource { app.ops.call("my_account").obj() }
    val categories = cats.data.orEmpty()
    val byId = remember(categories) { categories.associateBy { it.id } }
    val addresses = account.data?.list("addresses").orEmpty().filter { it.s("country_code") == "VE" }
    val savedAddress = addresses.firstOrNull { it.s("id") == address }
    val searchPoint = savedAddress?.geo() ?: zone.point ?: geo.point.takeIf { geo.area != null }

    val loadKey = listOf(category, mode, address, sort, radius, verified, searchPoint)
    val useCache = autoLocate && initialLocation == null
    val data = rememberResource(*loadKey.toTypedArray()) {
        loadProfessionals(app.ops, category, mode, address, sort, radius, verified, searchPoint)
            .also { if (useCache) ResultsMemory.save(loadKey, it) }
    }
    // Al volver de un perfil se muestran los últimos resultados mientras se actualizan (no se pierde la posición).
    val rows = data.data ?: if (useCache) ResultsMemory.get(loadKey) else null
    val profiles = remember(rows, verified, search, byId) {
        rows.orEmpty().filter { (!verified || it.verified) && it.matches(search, byId) }
    }
    LaunchedEffect(mode) { if (mode == "remote") showMap = false }

    val filterCount = listOf(verified, radius != 10000, sort != "distance", address.isNotEmpty() || zone.point != null).count { it }
    val locationTitle = when {
        savedAddress != null -> savedAddress.s("label") ?: "Dirección guardada"
        zone.area != null -> zone.area!!.areaLabel()
        geo.area != null -> "Cerca de ti · ${geo.area.s("parish_name").orEmpty()}"
        else -> "Encuentra servicios cerca de ti"
    }

    Column(Modifier.fillMaxSize().background(Vinko.Cream)) {
        if (showMap) {
            MapMode(
                profiles = profiles,
                loading = data.loading && rows == null,
                error = data.error,
                onRetry = data.reload,
                category = category,
                categoryName = byId[category]?.shortName(),
                radius = radius,
                searchPoint = searchPoint,
                geo = geo,
                locationTitle = locationTitle,
                filterCount = filterCount,
                onFilters = { showFilters = true },
                onList = { showMap = false },
                onOpen = { nav.navigate(Routes.professional(it)) },
            )
        } else {
            val listState = rememberLazyListState()
            val resultsIndex = if (mode != "remote") 4 else 3
            PullToRefreshBox(
                isRefreshing = data.loading && rows != null,
                onRefresh = { data.reload() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                    item(key = "hero") {
                        Hero(
                            geo = geo,
                            search = search,
                            onSearch = { search = it },
                            onSubmit = {
                                focus.clearFocus()
                                scope.launch { listState.animateScrollToItem(resultsIndex) }
                            },
                            onFavorites = { nav.navigate(Routes.FAVORITES) },
                            onRequests = { nav.navigate(Routes.REQUESTS) },
                        )
                    }
                    item(key = "categories") {
                        CategoriesRow(categories.ordered(), cats.loading && cats.data == null, category) {
                            category = it
                        }
                    }
                    item(key = "filters") {
                        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Segmented(
                                options = listOf("" to "Todos", "onsite" to "Presenciales", "remote" to "Remotos"),
                                selected = mode,
                                onSelect = { mode = it },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(10.dp))
                            FilterButton(filterCount) { showFilters = true }
                        }
                    }
                    if (mode != "remote") item(key = "location") {
                        LocationCard(
                            title = locationTitle,
                            subtitle = when {
                                searchPoint != null -> "Ordenados por distancia · radio de ${radius / 1000} km"
                                geo.status == GeoStatus.Locating -> "Buscando tu ubicación…"
                                else -> geo.error.ifEmpty { "Permite tu ubicación o elige una zona en el mapa." }
                            },
                            located = searchPoint != null,
                            locating = geo.status == GeoStatus.Locating,
                            onLocate = {
                                address = ""
                                zone.point = null
                                zone.area = null
                                geo.locate()
                            },
                            onZone = { chooseZone = true },
                            onChange = { showFilters = true },
                        )
                    }
                    item(key = "results") {
                        ResultsHeader(
                            count = profiles.size,
                            loading = rows == null && data.loading,
                            search = search,
                            mapDisabled = mode == "remote",
                            onMap = { showMap = true },
                        )
                    }
                    when {
                        rows == null && data.loading -> items(2) {
                            Box(Modifier.padding(horizontal = 20.dp, vertical = 7.dp)) { ProfessionalCardSkeleton() }
                        }
                        rows == null -> item { ErrorBox(data.error ?: "Inténtalo de nuevo.", data.reload) }
                        profiles.isEmpty() -> item {
                            EmptyBox(
                                "Aún no encontramos un profesional",
                                if (search.isNotBlank()) "No hay resultados para “${search.trim()}”. Prueba con otras palabras o categorías."
                                else "Prueba otra categoría o amplía la zona de búsqueda.",
                                "Publicar una solicitud",
                            ) { nav.navigate(Routes.newRequest()) }
                        }
                        else -> itemsIndexed(profiles, key = { _, p -> p.id }) { i, p ->
                            ProfessionalCard(
                                p,
                                byId,
                                i,
                                onClick = { nav.navigate(Routes.professional(p.id)) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp),
                            )
                        }
                    }
                    item(key = "cta") { PublishCta { nav.navigate(Routes.newRequest()) } }
                }
            }
        }
    }

    if (showFilters) {
        FiltersSheet(
            mode = mode,
            verified = verified,
            onVerified = { verified = it },
            radius = radius,
            onRadius = { radius = it },
            sort = sort,
            onSort = { sort = it },
            address = address,
            addresses = addresses,
            zone = zone,
            geo = geo,
            onUseCurrent = {
                address = ""
                zone.point = null
                zone.area = null
                if (geo.point == null || geo.area == null) geo.locate()
            },
            onUseZone = { address = "" },
            onAddress = { address = it },
            onChooseZone = {
                showFilters = false
                chooseZone = true
            },
            onAddresses = {
                showFilters = false
                nav.navigate(Routes.ADDRESSES)
            },
            onReset = {
                verified = false
                radius = 10000
                sort = "distance"
                address = ""
                zone.point = null
                zone.area = null
            },
            onDismiss = { showFilters = false },
        )
    }
    if (chooseZone) {
        ZonePickerDialog(
            start = searchPoint ?: geo.point,
            onDismiss = { chooseZone = false },
            onPicked = { point, area ->
                address = ""
                zone.point = point
                zone.area = area
            },
        )
    }
}

@Composable
private fun Hero(
    geo: DeviceLocation,
    search: String,
    onSearch: (String) -> Unit,
    onSubmit: () -> Unit,
    onFavorites: () -> Unit,
    onRequests: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(HeroGradient, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(enabled = geo.area == null && geo.status != GeoStatus.Locating) { geo.locate() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = Vinko.Lilac, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            geo.area != null -> geo.area!!.areaLabel()
                            geo.status == GeoStatus.Locating -> "Buscando tu ubicación…"
                            else -> "Venezuela · Usar mi ubicación"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onFavorites) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Favoritos", tint = Color.White)
            }
            IconButton(onClick = onRequests) {
                Icon(Icons.Outlined.Description, contentDescription = "Mis solicitudes", tint = Color.White)
            }
        }
        Column(Modifier.padding(end = 12.dp)) {
            Spacer(Modifier.height(14.dp))
            Text(
                buildAnnotatedString {
                    append("Personas de confianza para ")
                    withStyle(SpanStyle(color = Vinko.Lilac)) { append("hacerlo realidad.") }
                },
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Profesionales verificados, trato humano y pagos protegidos.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(16.dp))
            TextField(
                value = search,
                onValueChange = onSearch,
                placeholder = { Text("¿Qué servicio o profesional necesitas?", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Vinko.Primary) },
                trailingIcon = if (search.isNotEmpty()) {
                    { IconButton(onClick = { onSearch("") }) { Icon(Icons.Outlined.Close, contentDescription = "Borrar búsqueda", tint = Vinko.Muted) } }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Vinko.Primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CategoriesRow(categories: List<Category>, loading: Boolean, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.padding(top = 22.dp)) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            SectionLabel("Explora por especialidad")
            Text("¿Por dónde empezamos hoy?", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (loading) items(4) { Box(Modifier.size(112.dp, 84.dp).background(Vinko.Tint, RoundedCornerShape(16.dp))) }
            else {
                item(key = "all") { CategoryTile(null, "Todas", selected.isEmpty()) { onSelect("") } }
                items(categories, key = { it.id }) { c ->
                    CategoryTile(c, c.shortName(), selected == c.id) { onSelect(if (selected == c.id) "" else c.id) }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(category: Category?, label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .size(112.dp, 84.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .then(if (selected) Modifier.border(3.dp, Vinko.Primary, shape) else Modifier),
    ) {
        if (category == null) {
            Box(Modifier.fillMaxSize().background(HeroGradient))
            Icon(Icons.Outlined.Apps, contentDescription = null, tint = Vinko.Lilac, modifier = Modifier.padding(12.dp).size(22.dp))
        } else {
            Cover(serviceImage(category.slug), category.slug.length, Modifier.fillMaxSize())
            if (serviceImage(category.slug) == null) {
                Icon(categoryIcon(category.slug), contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp).size(22.dp))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 8.dp),
        )
        if (selected) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp).background(Vinko.Primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Check, contentDescription = "Seleccionada", tint = Color.White, modifier = Modifier.size(14.dp)) }
        }
    }
}

@Composable
private fun FilterButton(count: Int, onClick: () -> Unit) {
    Box {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, Vinko.Line, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Tune, contentDescription = "Filtros", tint = Vinko.Primary) }
        if (count > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(18.dp).background(Vinko.Coral, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("$count", style = MaterialTheme.typography.labelSmall, color = Color.White) }
        }
    }
}

@Composable
private fun LocationCard(
    title: String,
    subtitle: String,
    located: Boolean,
    locating: Boolean,
    onLocate: () -> Unit,
    onZone: () -> Unit,
    onChange: () -> Unit,
) {
    VCard(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(if (located) Icons.Outlined.MyLocation else Icons.Outlined.Place)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 2)
            }
            when {
                locating -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Vinko.Primary)
                located -> TextButton(onClick = onChange) { Text("Cambiar", color = Vinko.Primary) }
            }
        }
        if (!located && !locating) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Usar mi ubicación", selected = true, onClick = onLocate, icon = Icons.Outlined.MyLocation)
                Pill("Elegir en el mapa", selected = false, onClick = onZone, icon = Icons.Outlined.Map)
            }
        }
    }
}

@Composable
private fun ResultsHeader(count: Int, loading: Boolean, search: String, mapDisabled: Boolean, onMap: () -> Unit) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 8.dp)) {
        SectionLabel("Encuentra tu match")
        Text("Talento para lo que necesitas", style = MaterialTheme.typography.titleLarge)
        Text(
            when {
                loading -> "Buscando profesionales…"
                else -> "$count ${if (count == 1) "profesional disponible" else "profesionales disponibles"}" +
                    if (search.isNotBlank()) " para “${search.trim()}”" else ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Muted,
        )
        Spacer(Modifier.height(12.dp))
        Segmented(
            options = listOf("list" to "Listado", "map" to "Ver mapa"),
            selected = "list",
            onSelect = { if (it == "map") onMap() },
            disabled = if (mapDisabled) setOf("map") else emptySet(),
            icons = mapOf("list" to Icons.AutoMirrored.Outlined.List, "map" to Icons.Outlined.Map),
        )
        if (mapDisabled) {
            Text(
                "Los servicios remotos no se muestran en el mapa.",
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Subtle,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun PublishCta(onClick: () -> Unit) {
    Column(
        Modifier
            .padding(start = 20.dp, end = 20.dp, top = 18.dp)
            .fillMaxWidth()
            .background(HeroGradient, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text("¿No encuentras lo que buscas?", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(
            "Publica lo que necesitas y recibe propuestas de profesionales calificados. Es gratis.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Vinko.Primary),
        ) {
            Text("Publicar una solicitud", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FiltersSheet(
    mode: String,
    verified: Boolean,
    onVerified: (Boolean) -> Unit,
    radius: Int,
    onRadius: (Int) -> Unit,
    sort: String,
    onSort: (String) -> Unit,
    address: String,
    addresses: List<JsonObject>,
    zone: ManualZone,
    geo: DeviceLocation,
    onUseCurrent: () -> Unit,
    onUseZone: () -> Unit,
    onAddress: (String) -> Unit,
    onChooseZone: () -> Unit,
    onAddresses: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filtros", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onReset) { Text("Restablecer", color = Vinko.Primary) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Outlined.Verified, Vinko.Mint, Vinko.MintBg)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Solo verificados", style = MaterialTheme.typography.titleSmall)
                    Text("Profesionales con identidad revisada (KYC).", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
                Switch(
                    checked = verified,
                    onCheckedChange = onVerified,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Vinko.Primary,
                        uncheckedThumbColor = Vinko.Subtle,
                        uncheckedTrackColor = Vinko.Tint,
                        uncheckedBorderColor = Vinko.Lilac,
                    ),
                )
            }
            if (mode != "remote") {
                SectionLabel("Buscar cerca de")
                Column(Modifier.fillMaxWidth().border(1.dp, Vinko.Line, RoundedCornerShape(16.dp))) {
                    val currentSelected = address.isEmpty() && zone.point == null
                    PlaceOption(
                        Icons.Outlined.MyLocation,
                        "Mi ubicación actual",
                        geo.area?.areaLabel() ?: if (geo.status == GeoStatus.Locating) "Buscando tu ubicación…" else geo.error.ifEmpty { "Usa el GPS del teléfono" },
                        currentSelected,
                        onUseCurrent,
                    )
                    if (zone.point != null) {
                        PlaceOption(Icons.Outlined.Map, "Zona elegida en el mapa", zone.area?.areaLabel() ?: "", address.isEmpty(), onUseZone)
                    }
                    addresses.forEach { a ->
                        PlaceOption(
                            Icons.Outlined.Home,
                            a.s("label") ?: "Dirección",
                            listOfNotNull(a.s("parish_name"), a.s("city")).distinct().joinToString(" · "),
                            address == a.s("id"),
                        ) { onAddress(a.s("id").orEmpty()) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Elegir zona en el mapa", selected = false, onClick = onChooseZone, icon = Icons.Outlined.Map)
                    Pill("Mis direcciones", selected = false, onClick = onAddresses, icon = Icons.Outlined.Home)
                }
                SectionLabel("Radio de búsqueda")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RADII.forEach { r -> Pill("${r / 1000} km", radius == r, { onRadius(r) }) }
                }
                SectionLabel("Ordenar resultados")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Más cercanos primero", sort == "distance", { onSort("distance") })
                    Pill("Verificados y destacados", sort == "recommended", { onSort("recommended") })
                }
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton("Ver resultados", onClick = onDismiss)
        }
    }
}

@Composable
private fun PlaceOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Vinko.Primary else Vinko.Lavender, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Vinko.Primary))
    }
}

/** Vista de mapa (equivalente nativo de ExploreMap.tsx). */
@Composable
private fun MapMode(
    profiles: List<Pro>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    category: String,
    categoryName: String?,
    radius: Int,
    searchPoint: GeoPos?,
    geo: DeviceLocation,
    locationTitle: String,
    filterCount: Int,
    onFilters: () -> Unit,
    onList: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val app = LocalApp.current
    val handle = remember { MapHandle() }
    var selected by remember { mutableStateOf<String?>(null) }
    val ids = remember(profiles, category) {
        profiles.filter { p -> p.offerings.any { it.deliveryMode == "onsite" && (category.isEmpty() || it.categoryId == category) } }
            .map { it.id }.sorted()
    }
    val markers = rememberResource(ids, searchPoint, category, radius, loading) {
        if (loading || ids.isEmpty()) emptyList()
        else ids.chunked(100).flatMap { chunk ->
            app.ops.call(
                "professional_map_markers",
                "professionals" to chunk, "category" to category.ifEmpty { null },
                "lat" to searchPoint?.lat, "lon" to searchPoint?.lon, "radius_m" to radius,
            ).objects()
        }
    }
    val byPro = remember(profiles) { profiles.associateBy { it.id } }
    val groups = remember(markers.data, ids) {
        markers.data.orEmpty()
            .mapNotNull { m -> m.geo("lat", "lon")?.let { m to it } }
            .filter { (m, _) -> m.s("professional_id") in ids }
            .groupBy { (_, pos) -> "${pos.lat},${pos.lon}" }
            .mapValues { (_, list) -> list.distinctBy { it.first.s("professional_id") } }
    }
    val shown = groups.values.flatten().map { it.first.s("professional_id") }.toSet().size
    val pins = buildList {
        groups.forEach { (key, list) ->
            val title = if (list.size > 1) "${list.size} profesionales en esta zona" else byPro[list[0].first.s("professional_id")]?.name.orEmpty()
            add(Pin(key, list[0].second, PinKind.Service, title, list.size))
        }
        val me = geo.point
        if (searchPoint != null && (me == null || kotlin.math.abs(me.lat - searchPoint.lat) + kotlin.math.abs(me.lon - searchPoint.lon) > 0.00001)) {
            add(Pin("search", searchPoint, PinKind.Search, "Zona de búsqueda"))
        }
        if (me != null) add(Pin("me", me, PinKind.Me, "Tu ubicación actual"))
    }
    val busy = loading || markers.loading

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mapa de servicios", style = MaterialTheme.typography.titleLarge)
                    Text(
                        listOfNotNull(categoryName ?: "Todas las categorías", "${radius / 1000} km", locationTitle).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Vinko.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(10.dp))
                FilterButton(filterCount, onFilters)
            }
            Spacer(Modifier.height(10.dp))
            Segmented(
                options = listOf("list" to "Listado", "map" to "Ver mapa"),
                selected = "map",
                onSelect = { if (it == "list") onList() },
                icons = mapOf("list" to Icons.AutoMirrored.Outlined.List, "map" to Icons.Outlined.Map),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(Vinko.Primary, "Servicios")
                LegendDot(Vinko.Cyan, "Tu ubicación")
                if (searchPoint != null) LegendDot(Vinko.Lavender, "Zona de búsqueda")
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            ExploreMapView(
                pins = pins,
                modifier = Modifier.fillMaxSize(),
                circle = searchPoint?.let { it to radius },
                fitKey = listOf(groups.keys, searchPoint, radius, geo.point != null),
                initialCenter = searchPoint ?: geo.point,
                initialZoom = if (searchPoint != null || geo.point != null) 12.0 else 6.0,
                handle = handle,
                onPinTap = { pin -> selected = if (pin.kind == PinKind.Service) pin.key else null },
                onMapTap = { selected = null },
            )
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .shadow(4.dp, RoundedCornerShape(14.dp))
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Vinko.Primary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        when {
                            busy -> "Cargando ubicaciones…"
                            error != null || markers.error != null -> "Ubicaciones no disponibles"
                            shown > 0 -> "$shown ${if (shown == 1) "profesional en el mapa" else "profesionales en el mapa"}"
                            else -> "No hay servicios con ubicación para estos filtros."
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (!busy && groups.values.flatten().any { it.first.b("approximate") }) {
                    Text("Las sedes privadas muestran una zona aproximada.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
                if (!busy && (error != null || markers.error != null)) {
                    TextButton(onClick = { if (error != null) onRetry() else markers.reload() }) { Text("Reintentar", color = Vinko.Primary) }
                }
            }
            Column(
                Modifier.align(Alignment.TopEnd).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapButton(Icons.Outlined.ZoomOutMap, "Ver todos los marcadores") { handle.fit() }
                MapButton(Icons.Outlined.MyLocation, "Centrar en mi ubicación") {
                    val me = geo.point
                    if (me != null) handle.center(me) else geo.locate()
                }
            }
            val group = selected?.let { groups[it] }
            if (group != null) {
                LazyRow(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(group, key = { it.first.s("professional_id").orEmpty() }) { (marker, _) ->
                        val pro = byPro[marker.s("professional_id")]
                        if (pro != null) MapProCard(pro, marker, category) { onOpen(pro.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
    }
}

@Composable
private fun MapButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = description, tint = Vinko.Primary) }
}

@Composable
private fun MapProCard(pro: Pro, marker: JsonObject, category: String, onOpen: () -> Unit) {
    val service = pro.offerings.firstOrNull { it.deliveryMode == "onsite" && (category.isEmpty() || it.categoryId == category) }
    Column(
        Modifier
            .width(292.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .background(Color.White, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(pro.initials, 42.dp, border = false)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(pro.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(service?.title ?: "Servicio presencial", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (pro.verified) Icon(Icons.Outlined.Verified, contentDescription = "Verificado", tint = Vinko.Mint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${if (marker.b("approximate")) "Zona aproximada" else "Dirección pública"} · ${marker.s("label").orEmpty()}",
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Subtle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RatingRow(pro.rating, pro.reviewCount)
            Spacer(Modifier.weight(1f))
            Text(money(service?.priceMin, service?.currency ?: "VES"), style = MaterialTheme.typography.titleSmall, color = Vinko.Primary)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ver perfil", style = MaterialTheme.typography.labelLarge, color = Vinko.Primary)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(16.dp))
        }
    }
}
