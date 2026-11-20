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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
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
import androidx.compose.material3.Surface
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.soyvinko.app.data.Ops
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.ErrorBox
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.theme.Manrope
import androidx.compose.ui.text.style.TextAlign
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.BrandLogo
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.feature.pro.MarketCart
import com.soyvinko.app.ui.screens.HeroGradient
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Majesticons
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
    onOpenDrawer: (() -> Unit)? = null,
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

    val navEntry = nav.currentBackStackEntry
    val savedCat by navEntry?.savedStateHandle?.getStateFlow<String?>("selected_category", null)?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    LaunchedEffect(savedCat) {
        if (!savedCat.isNullOrBlank()) {
            category = savedCat!!
            navEntry?.savedStateHandle?.remove<String>("selected_category")
        }
    }

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
            HomeTopBar(
                onOpenDrawer = onOpenDrawer,
                onFavorites = { nav.navigate(Routes.FAVORITES) },
                onCart = { nav.navigate(Routes.CART) },
            )

            val listState = rememberLazyListState()
            val resultsIndex = 3
            PullToRefreshBox(
                isRefreshing = data.loading && rows != null,
                onRefresh = { data.reload() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                    item(key = "header_search") {
                        HeaderSearchBlock(
                            geo = geo,
                            search = search,
                            onSearch = { search = it },
                            onSubmit = {
                                focus.clearFocus()
                                scope.launch { listState.animateScrollToItem(resultsIndex) }
                            },
                            onProfile = { nav.navigate(Routes.PROFILE) },
                            onChooseZone = { chooseZone = true },
                        )
                    }
                    item(key = "service_buttons") {
                        HomeServiceButtons(
                            onDelivery = { nav.navigate(Routes.TRANSPORT_NEW) },
                            onRider = { nav.navigate(Routes.TRANSPORT) },
                            onMarketplace = { nav.navigate(Routes.MARKETPLACE) },
                            onGrua = { nav.navigate(Routes.TRANSPORT) },
                        )
                    }
                    item(key = "categories") {
                        CategoriesRow(
                            categories = categories.ordered().take(4),
                            loading = cats.loading && cats.data == null,
                            selected = category,
                            onViewAll = { nav.navigate(Routes.CATEGORIES) },
                            onSelect = { category = it },
                        )
                    }
                    item(key = "verified_carousel") {
                        VerifiedProvidersCarousel(profiles, onViewAll = { nav.navigate(Routes.CATEGORIES) }) { nav.navigate(Routes.professional(it)) }
                    }
                    item(key = "cta") { PublishCta { nav.navigate(Routes.newRequest()) } }
                    item(key = "ad_banner") { AdBanner() }
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
private fun HomeTopBar(
    onOpenDrawer: (() -> Unit)?,
    onFavorites: () -> Unit,
    onCart: () -> Unit,
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onOpenDrawer != null) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, contentDescription = "Menú", tint = Vinko.Primary)
                    }
                } else {
                    Spacer(Modifier.width(44.dp))
                }

                Spacer(Modifier.weight(1f))
                Text(
                    "Vinko",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = Vinko.Primary,
                )
                Spacer(Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (MarketCart.count > 0) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = onCart) {
                                Icon(Icons.Outlined.ShoppingCart, contentDescription = "Carrito", tint = Vinko.Primary)
                            }
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp, end = 4.dp)
                                    .background(Vinko.Coral, CircleShape)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    "${MarketCart.count}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    IconButton(onClick = onFavorites) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Favoritos", tint = Vinko.Primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSearchBlock(
    geo: DeviceLocation,
    search: String,
    onSearch: (String) -> Unit,
    onSubmit: () -> Unit,
    onProfile: (() -> Unit)? = null,
    onChooseZone: () -> Unit = {},
) {
    val user = LocalUser.current
    val app = LocalApp.current
    val avatarId = user.avatarFileId ?: app.avatarFileId

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        // Fila de Ubicación compacta, "Otra ubicación" alineada con la ubicación y Avatar a la derecha
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { geo.locate() }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Place,
                    contentDescription = null,
                    tint = Vinko.Primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        "Ubicación",
                        fontSize = 9.sp,
                        color = Vinko.Subtle,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        when {
                            geo.area != null -> geo.area!!.areaLabel()
                            geo.status == GeoStatus.Locating -> "Ubicando…"
                            else -> "Caracas, Chacao"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Vinko.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Surface(
                onClick = onChooseZone,
                shape = RoundedCornerShape(10.dp),
                color = Vinko.Tint,
                modifier = Modifier.height(30.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.Map,
                        contentDescription = "Otra ubicación",
                        tint = Vinko.Primary,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        "Otra ubicación",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = Vinko.Primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Avatar(
                initials = user.name,
                size = 34.dp,
                border = false,
                photoUrl = avatarId,
                onClick = onProfile,
            )
        }

        Spacer(Modifier.height(6.dp))

        // Barra de búsqueda compacta con bordes redondeados y fondo gris suave sin cortes de texto
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Vinko.FieldBg,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Vinko.Subtle,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (search.isEmpty()) {
                        Text(
                            "¿Qué servicio necesitas hoy?",
                            color = Vinko.Subtle,
                            fontFamily = Manrope,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = search,
                        onValueChange = onSearch,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                        textStyle = TextStyle(
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Vinko.Ink,
                        ),
                        cursorBrush = SolidColor(Vinko.Primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (search.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearch("") },
                        modifier = Modifier.size(26.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Borrar búsqueda",
                            tint = Vinko.Muted,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoriesRow(
    categories: List<Category>,
    loading: Boolean,
    selected: String,
    onViewAll: () -> Unit = {},
    onSelect: (String) -> Unit,
) {
    Column(Modifier.padding(top = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Servicios más solicitados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onViewAll, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text("Ver todas", color = Vinko.Primary, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                repeat(4) {
                    Box(Modifier.size(64.dp, 64.dp).background(Vinko.FieldBg, RoundedCornerShape(12.dp)))
                }
            } else {
                val fixedCats = categories.take(4)
                fixedCats.forEach { c ->
                    CategoryTile(c, c.shortName(), selected == c.id) { onSelect(if (selected == c.id) "" else c.id) }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(category: Category?, label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(66.dp).clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(shape)
                .background(if (selected) Vinko.Tint else Vinko.FieldBg)
                .then(if (selected) Modifier.border(2.dp, Vinko.Primary, shape) else Modifier.border(1.dp, Vinko.Line, shape)),
            contentAlignment = Alignment.Center,
        ) {
            if (category == null) {
                Icon(Icons.Outlined.Apps, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(24.dp))
            } else {
                val img = serviceImage(category.slug)
                if (img != null) {
                    Cover(img, category.slug.length, Modifier.fillMaxSize())
                } else {
                    Icon(categoryIcon(category.slug), contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(26.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Vinko.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FilterButton(count: Int, onClick: () -> Unit) {
    Box {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Vinko.Line, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Tune, contentDescription = "Filtros", tint = Vinko.Primary, modifier = Modifier.size(20.dp)) }
        if (count > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).size(16.dp).background(Vinko.Coral, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("$count", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White) }
        }
    }
}

@Composable
private fun ResultsHeader(count: Int, loading: Boolean, search: String, mapDisabled: Boolean, onMap: () -> Unit) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
        SectionLabel("Encuentra tu proveedor de confianza")
        Text("Proveedores verificados cerca de ti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            when {
                loading -> "Buscando profesionales…"
                else -> "$count ${if (count == 1) "profesional disponible" else "profesionales disponibles"}" +
                    if (search.isNotBlank()) " para “${search.trim()}”" else ""
            },
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = Vinko.Muted,
        )
        Spacer(Modifier.height(8.dp))
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
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Anuncia gratis tu requerimiento y encuentra tu proveedor ideal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Vinko.Primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Vinko.Primary, contentColor = Color.White),
        ) {
            Text("Publicar solicitud", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun HomeServiceButtons(
    onDelivery: () -> Unit,
    onRider: () -> Unit,
    onMarketplace: () -> Unit,
    onGrua: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ServiceShortcutButton(
                title = "Delivery",
                subtitle = "Envía y recibe de forma segura",
                icon = Icons.Outlined.LocalShipping,
                modifier = Modifier.weight(1f),
                onClick = onDelivery,
            )
            ServiceShortcutButton(
                title = "Rider",
                subtitle = "Viaja con nuestros conductores",
                icon = Icons.Outlined.DirectionsCar,
                modifier = Modifier.weight(1f),
                onClick = onRider,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ServiceShortcutButton(
                title = "Marketplace",
                subtitle = "Compra y vende localmente",
                icon = Icons.Outlined.Storefront,
                modifier = Modifier.weight(1f),
                onClick = onMarketplace,
            )
            ServiceShortcutButton(
                title = "Grúa",
                subtitle = "Recibe atención inmediata con la grúa más cercana",
                icon = Icons.Outlined.Build,
                modifier = Modifier.weight(1f),
                onClick = onGrua,
            )
        }
    }
}

@Composable
private fun ServiceShortcutButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Vinko.Tint,
        border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Primary),
        modifier = modifier.height(72.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Vinko.Primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = Vinko.Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Vinko.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 12.sp,
                    )
                }
            }
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

@Composable
private fun VerifiedProvidersCarousel(profiles: List<Pro>, onViewAll: () -> Unit = {}, onOpen: (String) -> Unit) {
    val verifiedPros = remember(profiles) {
        profiles.filter { it.verified }.take(6).ifEmpty { profiles.take(6) }
    }
    if (verifiedPros.isEmpty()) return
    Column(Modifier.padding(top = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Proveedores verificados cerca de ti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onViewAll, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text("Ver más", color = Vinko.Primary, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(verifiedPros, key = { p -> p.id }) { p ->
                VerifiedProCard(p, onClick = { onOpen(p.id) })
            }
        }
    }
}

@Composable
private fun VerifiedProCard(p: Pro, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .width(118.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(initials = p.name, size = 52.dp, photoUrl = null)
            Spacer(Modifier.height(8.dp))
            Text(
                text = p.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Verified,
                    contentDescription = "Verificado",
                    tint = Vinko.Mint,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Verificado",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Vinko.Mint,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(6.dp))
            RatingRow(p.rating, p.reviewCount)
        }
    }
}

@Composable
private fun AdBanner() {
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "ESPACIO PUBLICITARIO",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Vinko.Subtle,
            )
            Box(
                modifier = Modifier
                    .background(Vinko.Tint, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    "Anuncio",
                    fontSize = 8.sp,
                    color = Vinko.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Impulsa tu negocio con Vinko",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Vinko.Ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Anuncia tus servicios o productos ante miles de clientes potenciales en tu zona.",
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Muted,
            textAlign = TextAlign.Center,
        )
    }
}
