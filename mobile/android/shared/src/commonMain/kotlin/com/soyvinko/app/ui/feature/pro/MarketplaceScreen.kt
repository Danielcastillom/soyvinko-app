package com.soyvinko.app.ui.feature.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalShellScope
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.screens.HeroGradient
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// ---------------------------------------------------------------------------------------------
// Carrito (una sola tienda y una sola moneda por pedido, como exige create_order)
// ---------------------------------------------------------------------------------------------

data class CartLine(
    val productId: String,
    val name: String,
    val unitMinor: Long,
    val currency: String,
    val stock: Int,
    val image: String?,
    val qty: Int,
)

enum class AddResult { Added, OtherStore, OtherCurrency, NoStock }

/** Ids de las fotos: `images` es una lista de ids (catálogo, tienda) o de objetos {file_id, moderation_status} (mis productos). */
fun JsonObject.images(): List<String> =
    (this["images"] as? JsonArray)?.mapNotNull { e ->
        when (e) {
            is JsonObject -> e.s("file_id")
            is JsonPrimitive -> e.contentOrNull
            else -> null
        }
    }.orEmpty()

object MarketCart {
    var storeId by mutableStateOf<String?>(null)
        private set
    var storeName by mutableStateOf("")
        private set
    val lines = mutableStateListOf<CartLine>()

    /** Clave de idempotencia del pedido: cambia con el contenido para no chocar con un pedido anterior. */
    var checkoutKey by mutableStateOf(newKey())
        private set

    val count: Int get() = lines.sumOf { it.qty }
    val total: Long get() = lines.sumOf { it.unitMinor * it.qty }
    val currency: String? get() = lines.firstOrNull()?.currency

    fun qtyOf(productId: String?): Int = lines.firstOrNull { it.productId == productId }?.qty ?: 0

    fun check(product: JsonObject): AddResult = when {
        (product.l("stock") ?: 0L) <= 0L -> AddResult.NoStock
        lines.isNotEmpty() && product.s("store_id") != storeId -> AddResult.OtherStore
        lines.isNotEmpty() && product.s("currency")?.trim() != currency -> AddResult.OtherCurrency
        else -> AddResult.Added
    }

    fun add(product: JsonObject, store: String, quantity: Int): AddResult {
        val result = check(product)
        if (result != AddResult.Added) return result
        val id = product.s("id") ?: return AddResult.NoStock
        val stock = (product.l("stock") ?: 0L).toInt()
        if (lines.isEmpty()) {
            storeId = product.s("store_id")
            storeName = store
        }
        val i = lines.indexOfFirst { it.productId == id }
        if (i >= 0) {
            lines[i] = lines[i].copy(qty = (lines[i].qty + quantity).coerceIn(1, stock), stock = stock)
        } else {
            lines += CartLine(
                productId = id,
                name = product.s("name") ?: "",
                unitMinor = product.l("price_minor") ?: 0L,
                currency = product.s("currency")?.trim() ?: "VES",
                stock = stock,
                image = product.images().firstOrNull(),
                qty = quantity.coerceIn(1, stock),
            )
        }
        checkoutKey = newKey()
        return AddResult.Added
    }

    fun setQty(productId: String, qty: Int) {
        val i = lines.indexOfFirst { it.productId == productId }
        if (i < 0) return
        if (qty <= 0) lines.removeAt(i) else lines[i] = lines[i].copy(qty = qty.coerceAtMost(lines[i].stock))
        if (lines.isEmpty()) clear() else checkoutKey = newKey()
    }

    fun clear() {
        lines.clear()
        storeId = null
        storeName = ""
        checkoutKey = newKey()
    }
}

/** Agrega al carrito y resuelve los conflictos de tienda o moneda con una confirmación. */
@Composable
fun rememberCartAdder(nav: NavHostController): (JsonObject, String, Int) -> Unit {
    val snackbar = LocalSnackbar.current
    val notices = LocalShellScope.current ?: NoticeScope
    var pending by remember { mutableStateOf<Triple<JsonObject, String, Int>?>(null) }
    var reason by remember { mutableStateOf(AddResult.Added) }
    val notify: (String) -> Unit = { name ->
        notices.launch {
            val r = snackbar.showSnackbar("$name se agregó al carrito.", actionLabel = "Ver carrito", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) runCatching { nav.navigate(CART) }
        }
    }
    pending?.let { (product, store, qty) ->
        ConfirmDialog(
            title = if (reason == AddResult.OtherStore) "¿Empezar un pedido nuevo?" else "Moneda diferente",
            text = if (reason == AddResult.OtherStore) {
                "Tu carrito tiene productos de ${MarketCart.storeName}. Cada pedido es para una sola tienda. ¿Vaciarlo y agregar este producto?"
            } else {
                "Un pedido solo puede tener productos en una moneda. ¿Vaciar el carrito y agregar este producto?"
            },
            confirm = "Vaciar y agregar",
            onDismiss = { pending = null },
            onConfirm = {
                MarketCart.clear()
                if (MarketCart.add(product, store, qty) == AddResult.Added) notify(product.s("name") ?: "El producto")
            },
        )
    }
    return { product, store, qty ->
        when (val r = MarketCart.add(product, store, qty)) {
            AddResult.Added -> notify(product.s("name") ?: "El producto")
            AddResult.OtherStore, AddResult.OtherCurrency -> {
                reason = r
                pending = Triple(product, store, qty)
            }
            AddResult.NoStock -> Unit
        }
    }
}

@Composable
fun CartButton(nav: NavHostController) {
    IconButton(onClick = { nav.navigate(CART) }) {
        BadgedBox(badge = {
            if (MarketCart.count > 0) Badge(containerColor = Vinko.Primary, contentColor = Color.White) { Text("${MarketCart.count}") }
        }) {
            Icon(Icons.Outlined.ShoppingCart, contentDescription = "Carrito", tint = Vinko.Ink)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Piezas visuales de producto
// ---------------------------------------------------------------------------------------------

private val COVERS = listOf(
    listOf(Color(0xFFF0EBFF), Color(0xFFE2D8FF)),
    listOf(Color(0xFFE6FAF5), Color(0xFFCDEFE5)),
    listOf(Color(0xFFEBF8FF), Color(0xFFD3ECFA)),
    listOf(Color(0xFFFFF6DB), Color(0xFFFCE9B4)),
)

@Composable
fun ProductImage(file: String?, seed: String?, modifier: Modifier = Modifier, iconSize: Dp = 44.dp) {
    if (file != null) {
        ProImage(file, modifier, iconSize = iconSize)
    } else {
        val colors = COVERS[((seed?.hashCode() ?: 0) and 0x7fffffff) % COVERS.size]
        Box(modifier.background(Brush.linearGradient(colors)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Vinko.Lavender, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
private fun StockTag(stock: Long, modifier: Modifier = Modifier) {
    val (text, fg, bg) = when {
        stock <= 0 -> Triple("Agotado", Vinko.Coral, Vinko.CoralBg)
        stock <= 3 -> Triple("¡Últimas $stock!", Vinko.Amber, Vinko.AmberBg)
        else -> Triple("En stock", Vinko.Mint, Vinko.MintBg)
    }
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun ProductCard(
    p: JsonObject,
    own: Boolean,
    onOpen: () -> Unit,
    onStore: (() -> Unit)?,
    onAdd: () -> Unit,
) {
    val stock = p.l("stock") ?: 0L
    val inCart = MarketCart.qtyOf(p.s("id"))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Vinko.Line, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.15f)) {
            ProductImage(p.images().firstOrNull(), p.s("id"), Modifier.fillMaxSize())
            StockTag(stock, Modifier.align(Alignment.TopStart).padding(8.dp))
        }
        Column(Modifier.padding(12.dp)) {
            if (onStore != null) {
                Row(
                    Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onStore),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        p.s("store_name") ?: "Tienda",
                        style = MaterialTheme.typography.labelMedium,
                        color = Vinko.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            Text(p.s("name") ?: "", style = MaterialTheme.typography.titleSmall, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(money(p.l("price_minor"), p.s("currency")?.trim()), style = MaterialTheme.typography.titleMedium, color = Vinko.Ink)
            Spacer(Modifier.height(10.dp))
            SmallButton(
                text = when {
                    own -> "Tu producto"
                    stock <= 0 -> "Agotado"
                    inCart > 0 -> "En carrito ($inCart)"
                    else -> "Agregar"
                },
                onClick = onAdd,
                icon = if (!own && stock > 0) Icons.Outlined.AddShoppingCart else null,
                tone = if (inCart > 0) Tone.Soft else Tone.Primary,
                enabled = !own && stock > 0 && inCart < stock,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun QuantityStepper(value: Int, onChange: (Int) -> Unit, max: Int, min: Int = 1) {
    Row(
        Modifier.background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, Vinko.Line, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange(value - 1) }, enabled = value > min, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Outlined.Remove, contentDescription = "Menos", tint = if (value > min) Vinko.Primary else Vinko.Lilac, modifier = Modifier.size(18.dp))
        }
        Text("$value", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 28.dp))
        IconButton(onClick = { onChange(value + 1) }, enabled = value < max, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = "Más", tint = if (value < max) Vinko.Primary else Vinko.Lilac, modifier = Modifier.size(18.dp))
        }
    }
}

/** Detalle de producto con cantidad y botón para agregar al carrito. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSheet(p: JsonObject, own: Boolean, onDismiss: () -> Unit, onStore: (() -> Unit)?, onAdd: (Int) -> Unit) {
    val stock = (p.l("stock") ?: 0L).toInt()
    val currency = p.s("currency")?.trim()
    var qty by remember { mutableStateOf(1) }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Vinko.Cream) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val images = p.images()
            if (images.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    images.forEach { ProductImage(it, p.s("id"), Modifier.size(width = 260.dp, height = 200.dp).clip(RoundedCornerShape(16.dp))) }
                }
            } else {
                ProductImage(images.firstOrNull(), p.s("id"), Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)), iconSize = 60.dp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StockTag(stock.toLong())
                p.s("sku")?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.width(8.dp))
                    Text("SKU $it", style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle)
                }
            }
            Text(p.s("name") ?: "", style = MaterialTheme.typography.headlineSmall)
            Text(money(p.l("price_minor"), currency), style = MaterialTheme.typography.headlineSmall, color = Vinko.Primary)
            if (onStore != null) {
                Row(
                    Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp)).border(1.dp, Vinko.Line, RoundedCornerShape(14.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    com.soyvinko.app.ui.common.IconTile(Icons.Outlined.Storefront, size = 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Vendido por", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                        Text(p.s("store_name") ?: "Tienda", style = MaterialTheme.typography.titleSmall)
                    }
                    SmallButton("Ver tienda", onClick = onStore, tone = Tone.Soft)
                }
            }
            p.s("description")?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(
                if (stock > 0) "$stock ${if (stock == 1) "unidad disponible" else "unidades disponibles"}" else "Sin unidades disponibles por ahora.",
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Muted,
            )
            Banner(
                "Al confirmar el pedido, las unidades quedan apartadas. El pago y la entrega se acuerdan directamente con el vendedor, sin comisiones.",
                BannerTone.Info,
            )
            when {
                own -> Banner("Este producto es de tu tienda.", BannerTone.Info)
                stock > 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantityStepper(qty, { qty = it.coerceIn(1, stock) }, max = stock)
                    Spacer(Modifier.width(12.dp))
                    PrimaryButton(
                        "Agregar · ${money((p.l("price_minor") ?: 0L) * qty, currency)}",
                        onClick = { onAdd(qty) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Catálogo (MARKETPLACE)
// ---------------------------------------------------------------------------------------------

private val CATEGORY_FILTERS = listOf(
    "all" to "Todos",
    "tech" to "Tecnología",
    "home" to "Hogar y limpieza",
    "tools" to "Herramientas",
    "parts" to "Repuestos",
    "supplies" to "Insumos y moda",
)

// Mismas palabras clave que la web (Marketplace.tsx): los productos aún no tienen categoría propia.
private val CATEGORY_WORDS = mapOf(
    "tech" to listOf("ssd", "disco", "comput", "cable", "electr", "audio", "tecnolog"),
    "home" to listOf("limpieza", "hogar", "mueble", "kit"),
    "tools" to listOf("herramienta", "taladro", "llave", "maquin"),
    "parts" to listOf("repuesto", "tuber", "valvula", "válvula", "filtro", "pieza"),
    "supplies" to listOf("insumo", "accesorio", "moda", "ropa"),
)

private fun matchesCategory(p: JsonObject, category: String): Boolean {
    val words = CATEGORY_WORDS[category] ?: return true
    val text = "${p.s("name")} ${p.s("description")}".lowercase()
    return words.any { it in text }
}

@Composable
fun MarketplaceScreen(nav: NavHostController) {
    val app = LocalApp.current
    val user = LocalUser.current
    val data = rememberResource { app.ops.call("marketplace", "limit" to 100) }
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("all") }
    var sort by rememberSaveable { mutableStateOf("recommended") }
    var open by remember { mutableStateOf<JsonObject?>(null) }
    val add = rememberCartAdder(nav)

    ScreenScaffold(
        "Marketplace",
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { nav.navigate(Routes.ORDERS) }) {
                Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = "Mis pedidos", tint = Vinko.Ink)
            }
            CartButton(nav)
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            ResourceView(data) { raw ->
                val all = raw.objects()
                val q = search.trim().lowercase()
                val filtered = all
                    .filter { p -> q.isEmpty() || "${p.s("name")} ${p.s("description")} ${p.s("store_name")} ${p.s("sku")}".lowercase().contains(q) }
                    .filter { matchesCategory(it, category) }
                    .let { list ->
                        when (sort) {
                            "price_asc" -> list.sortedBy { it.l("price_minor") ?: 0L }
                            "price_desc" -> list.sortedByDescending { it.l("price_minor") ?: 0L }
                            "stock" -> list.sortedByDescending { it.l("stock") ?: 0L }
                            else -> list
                        }
                    }
                val stores = all.groupBy { it.s("store_id") }.mapNotNull { (id, items) -> id?.let { Triple(it, items.first().s("store_name") ?: "Tienda", items.size) } }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    full {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            MarketHero()
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmallButton("Mis pedidos", onClick = { nav.navigate(Routes.ORDERS) }, icon = Icons.AutoMirrored.Outlined.ReceiptLong, tone = Tone.Outline, modifier = Modifier.weight(1f))
                                SmallButton(
                                    if (user.isProfessional) "Mi tienda" else "Vender aquí",
                                    onClick = { nav.navigate(Routes.MY_STORE) },
                                    icon = Icons.Outlined.Storefront,
                                    tone = Tone.Soft,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            VinkoTextField(
                                search, { search = it.take(80) }, "Buscar productos o tiendas",
                                leadingIcon = Icons.Outlined.Search,
                            )
                            ChoiceChips(CATEGORY_FILTERS, category, { category = it })
                            ChoiceChips(
                                listOf("recommended" to "Recomendados", "price_asc" to "Menor precio", "price_desc" to "Mayor precio", "stock" to "Mayor stock"),
                                sort,
                                { sort = it },
                            )
                            if (stores.size > 1 && q.isEmpty() && category == "all") {
                                Text("Tiendas", style = MaterialTheme.typography.titleMedium)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    stores.forEach { (id, name, count) -> StoreChip(name, count) { nav.navigate(Routes.store(id)) } }
                                }
                            }
                            Text(
                                "${filtered.size} ${if (filtered.size == 1) "producto disponible" else "productos disponibles"}" + if (q.isNotEmpty()) " para «${search.trim()}»" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Vinko.Muted,
                            )
                        }
                    }
                    if (filtered.isEmpty()) {
                        full {
                            EmptyBox(
                                if (all.isEmpty()) "Aún no hay productos publicados" else "No encontramos productos con ese criterio",
                                if (all.isEmpty()) "Vuelve pronto: los comercios de SoyVinko están cargando su catálogo." else "Prueba con otra búsqueda o con «Todos».",
                                if (all.isEmpty()) null else "Limpiar filtros",
                            ) {
                                search = ""
                                category = "all"
                            }
                        }
                    }
                    items(filtered, key = { it.s("id") ?: it.toString() }) { p ->
                        val own = p.s("professional_id") == user.id
                        ProductCard(
                            p,
                            own = own,
                            onOpen = { open = p },
                            onStore = { p.s("store_id")?.let { nav.navigate(Routes.store(it)) } },
                            onAdd = { add(p, p.s("store_name") ?: "Tienda", 1) },
                        )
                    }
                }
            }
        }
    }

    open?.let { p ->
        ProductSheet(
            p,
            own = p.s("professional_id") == user.id,
            onDismiss = { open = null },
            onStore = {
                open = null
                p.s("store_id")?.let { nav.navigate(Routes.store(it)) }
            },
            onAdd = { qty ->
                open = null
                add(p, p.s("store_name") ?: "Tienda", qty)
            },
        )
    }
}

fun LazyGridScope.full(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarketHero() {
    Column(Modifier.fillMaxWidth().background(HeroGradient, RoundedCornerShape(22.dp)).padding(20.dp)) {
        Text("MERCADO ABIERTO Y TIENDAS OFICIALES", style = MaterialTheme.typography.labelSmall, color = Vinko.Lilac)
        Spacer(Modifier.height(6.dp))
        Text("Compra directo a comercios de confianza", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("0% comisión", Color.White, Color.White.copy(alpha = 0.14f), Icons.Outlined.Percent)
            Pill("Entrega acordada", Color.White, Color.White.copy(alpha = 0.14f), Icons.Outlined.Handshake)
            Pill("Pago en Bs o divisas", Color.White, Color.White.copy(alpha = 0.14f), Icons.Outlined.Payments)
        }
    }
}

@Composable
private fun StoreChip(name: String, count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Vinko.Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Monogram(name, size = 32.dp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text("$count ${if (count == 1) "producto" else "productos"}", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tienda (STORE)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreScreen(storeId: String, nav: NavHostController) {
    val app = LocalApp.current
    val user = LocalUser.current
    // store_products ya trae store_name, professional_id e images (fotos aprobadas, en orden).
    val products = rememberResource(storeId) { app.ops.call("store_products", "store" to storeId).objects() }
    var open by remember { mutableStateOf<JsonObject?>(null) }
    val add = rememberCartAdder(nav)
    val first = products.data?.firstOrNull()
    val storeName = first?.s("store_name") ?: MarketCart.storeName.takeIf { MarketCart.storeId == storeId && it.isNotBlank() } ?: "Tienda"
    val sellerId = first?.s("professional_id")
    val own = sellerId != null && sellerId == user.id
    val showBar = MarketCart.storeId == storeId && MarketCart.count > 0

    ScreenScaffold(
        storeName,
        onBack = { nav.popBackStack() },
        actions = { CartButton(nav) },
        bottomBar = {
            if (showBar) {
                Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
                    LineDivider()
                    PrimaryButton(
                        "Ver carrito (${MarketCart.count}) · ${money(MarketCart.total, MarketCart.currency)}",
                        onClick = { nav.navigate(CART) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            ResourceView(products) { list ->
                val enriched = list
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    full {
                        Column(
                            Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).border(1.dp, Vinko.Line, RoundedCornerShape(20.dp)).padding(16.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Monogram(storeName, size = 52.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(storeName, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${enriched.size} ${if (enriched.size == 1) "producto disponible" else "productos disponibles"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Vinko.Muted,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Pill("0% comisión", Vinko.Mint, Vinko.MintBg, Icons.Outlined.Percent)
                                Pill("Pago directo", icon = Icons.Outlined.Payments)
                                Pill("Entrega acordada", icon = Icons.Outlined.Handshake)
                            }
                            if (sellerId != null) {
                                Spacer(Modifier.height(12.dp))
                                SmallButton(
                                    "Ver perfil del vendedor",
                                    onClick = { nav.navigate(Routes.professional(sellerId)) },
                                    icon = Icons.Outlined.Person,
                                    tone = Tone.Outline,
                                )
                            }
                            if (MarketCart.count > 0 && MarketCart.storeId != storeId) {
                                Spacer(Modifier.height(12.dp))
                                Banner("Tu carrito tiene productos de ${MarketCart.storeName}. Cada pedido es para una sola tienda.", BannerTone.Info)
                            }
                        }
                    }
                    if (enriched.isEmpty()) {
                        full { EmptyBox("Esta tienda no tiene productos disponibles", "Vuelve más tarde o explora otras tiendas del Marketplace.") }
                    }
                    items(enriched, key = { it.s("id") ?: it.toString() }) { p ->
                        ProductCard(p, own = own, onOpen = { open = p }, onStore = null, onAdd = { add(p, storeName, 1) })
                    }
                }
            }
        }
    }

    open?.let { p ->
        ProductSheet(p, own = own, onDismiss = { open = null }, onStore = null, onAdd = { qty ->
            open = null
            add(p, storeName, qty)
        })
    }
}

@Composable
fun CartIconTile() = Box(
    Modifier.size(40.dp).background(Vinko.Tint, CircleShape),
    contentAlignment = Alignment.Center,
) { Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(20.dp)) }

@Composable
fun RemoveButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Outlined.Close, contentDescription = "Quitar", tint = Vinko.Muted, modifier = Modifier.size(18.dp))
    }
}
