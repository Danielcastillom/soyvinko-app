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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.common.AppMode
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalAppMode
import com.soyvinko.app.ui.common.LocalOpenDrawer
import com.soyvinko.app.ui.common.LocalShellScope
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.o
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberPhotoPicker
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.uploadFile
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.feature.explore.LocalPhoto
import com.soyvinko.app.ui.screens.HeroGradient
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Manrope
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
        else -> Triple("En stock ($stock)", Vinko.Mint, Vinko.MintBg)
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = modifier
            .background(bg.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

/** Tarjeta de producto estilo Facebook Marketplace */
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
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.22f)) {
            ProductImage(p.images().firstOrNull(), p.s("id"), Modifier.fillMaxSize())
            if (stock <= 0) {
                Text(
                    "Agotado",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Vinko.Coral,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .background(Vinko.CoralBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            } else if (stock <= 3) {
                Text(
                    "¡Últimas $stock!",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Vinko.Amber,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .background(Vinko.AmberBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            // Precio en negrita destacado
            Text(
                money(p.l("price_minor"), p.s("currency")?.trim()),
                fontFamily = Manrope,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = Vinko.Ink,
            )
            Spacer(Modifier.height(2.dp))
            // Nombre del producto
            Text(
                p.s("name") ?: "",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF374151),
            )
            Spacer(Modifier.height(4.dp))
            // Fila de tienda y botón de apartado compacto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    p.s("store_name") ?: "Tienda",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = Vinko.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                )
                Surface(
                    onClick = onAdd,
                    enabled = !own && stock > 0 && inCart < stock,
                    shape = RoundedCornerShape(6.dp),
                    color = if (inCart > 0) Vinko.Tint else Vinko.Primary,
                    modifier = Modifier.height(26.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 6.dp),
                    ) {
                        Icon(
                            if (inCart > 0) Icons.Outlined.Check else Icons.Outlined.AddShoppingCart,
                            contentDescription = null,
                            tint = if (inCart > 0) Vinko.Primary else Color.White,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            when {
                                own -> "Tuyo"
                                stock <= 0 -> "Agotado"
                                inCart > 0 -> "($inCart)"
                                else -> "Apartar"
                            },
                            color = if (inCart > 0) Vinko.Primary else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
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

/** Detalle de producto con fotos, inventario y apartado directo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSheet(p: JsonObject, own: Boolean, onDismiss: () -> Unit, onStore: (() -> Unit)?, onAdd: (Int) -> Unit) {
    val stock = (p.l("stock") ?: 0L).toInt()
    val currency = p.s("currency")?.trim()
    var qty by remember { mutableStateOf(1) }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Vinko.Cream) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
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
            // Precio destacado
            Text(
                money(p.l("price_minor"), currency),
                fontFamily = Manrope,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                color = Vinko.Primary,
            )
            Text(p.s("name") ?: "", style = MaterialTheme.typography.titleLarge)
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
            // Detalle de stock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Vinko.Line, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = if (stock > 0) Vinko.Mint else Vinko.Coral, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        if (stock > 0) "$stock ${if (stock == 1) "unidad disponible en stock" else "unidades disponibles en inventario"}" else "Agotado en inventario",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (stock > 0) Vinko.Ink else Vinko.Coral,
                    )
                    Text("El stock se descuenta automáticamente con pedidos confirmados.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
            }
            Banner(
                "Al apartar el artículo, las unidades se descuentan de inventario. El pago y la entrega se acuerdan directamente con el vendedor, sin comisión.",
                BannerTone.Info,
            )
            when {
                own -> Banner("Este producto es de tu tienda.", BannerTone.Info)
                stock > 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantityStepper(qty, { qty = it.coerceIn(1, stock) }, max = stock)
                    Spacer(Modifier.width(12.dp))
                    PrimaryButton(
                        "Apartar · ${money((p.l("price_minor") ?: 0L) * qty, currency)}",
                        onClick = { onAdd(qty) },
                        modifier = Modifier.weight(1f),
                    )
                }
                else -> SecondaryButton("Artículo agotado", enabled = false, onClick = {})
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Categorías y Filtros
// ---------------------------------------------------------------------------------------------

private val CATEGORY_FILTERS = listOf(
    "all" to "Todo",
    "tech" to "Tecnología",
    "home" to "Hogar",
    "tools" to "Herramientas",
    "vehicles" to "Repuestos",
    "fashion" to "Moda",
    "supplies" to "Insumos",
)

private val CATEGORY_WORDS = mapOf(
    "tech" to listOf("ssd", "disco", "comput", "cable", "electr", "audio", "tecnolog", "telefono", "celular", "laptop", "pc"),
    "home" to listOf("limpieza", "hogar", "mueble", "kit", "cocina", "bano", "casa", "decor"),
    "tools" to listOf("herramienta", "taladro", "llave", "maquin", "soldad", "martillo", "metro"),
    "vehicles" to listOf("repuesto", "tuber", "valvula", "válvula", "filtro", "pieza", "carro", "moto", "caucho", "bateria"),
    "fashion" to listOf("accesorio", "moda", "ropa", "zapato", "franela", "pantalon", "camisa", "gorra"),
    "supplies" to listOf("insumo", "material", "tornillo", "pintura", "pega", "cinta"),
)

private fun matchesCategory(p: JsonObject, category: String): Boolean {
    val words = CATEGORY_WORDS[category] ?: return true
    val text = "${p.s("name")} ${p.s("description")}".lowercase()
    return words.any { it in text }
}

// ---------------------------------------------------------------------------------------------
// Marketplace Screen (Estilo Facebook Marketplace)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarketplaceScreen(nav: NavHostController) {
    val app = LocalApp.current
    val user = LocalUser.current
    val data = rememberResource { app.ops.call("marketplace", "limit" to 100) }
    val proData = rememberResource { app.ops.call("my_professional") }

    val myStore = proData.data?.obj()?.o("store")
    val isSeller = myStore?.s("status") == "active"
    val storeId = myStore?.s("id")

    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("all") }
    var sort by rememberSaveable { mutableStateOf("recommended") }
    var open by remember { mutableStateOf<JsonObject?>(null) }
    var showSellerActivation by remember { mutableStateOf(false) }
    var showQuickPublish by remember { mutableStateOf(false) }
    val add = rememberCartAdder(nav)

    val mode = LocalAppMode.current
    val openDrawer = LocalOpenDrawer.current

    ScreenScaffold(
        "Marketplace",
        onBack = if (mode == AppMode.SELLER) null else { { if (!nav.popBackStack()) nav.navigate(Routes.HOME) } },
        navigationIcon = if (mode == AppMode.SELLER) {
            {
                IconButton(onClick = openDrawer) {
                    Icon(Icons.Outlined.Menu, contentDescription = "Menú", tint = Vinko.Ink)
                }
            }
        } else null,
        actions = {
            IconButton(onClick = { nav.navigate(Routes.ORDERS) }) {
                Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = "Mis compras", tint = Vinko.Ink)
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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    full {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Fila 1: Búsqueda compacta + Botón Vender + Inventario
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MarketSearchBar(
                                    value = search,
                                    onValueChange = { search = it.take(80) },
                                    placeholder = "Buscar artículos...",
                                    modifier = Modifier.weight(1f),
                                )

                                Surface(
                                    onClick = {
                                        if (isSeller && storeId != null) {
                                            showQuickPublish = true
                                        } else {
                                            showSellerActivation = true
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = Vinko.Primary,
                                    modifier = Modifier.height(40.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Text("Vender", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                if (isSeller) {
                                    Surface(
                                        onClick = { nav.navigate(Routes.MY_STORE) },
                                        shape = CircleShape,
                                        color = Color(0xFFF5F5F7),
                                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(Icons.Outlined.Inventory2, contentDescription = "Mi inventario", tint = Vinko.Ink, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            // Fila 2: Filtros de categoría en píldoras horizontales compactas
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CATEGORY_FILTERS.forEach { (key, label) ->
                                    CategoryPill(
                                        label = label,
                                        selected = category == key,
                                        onClick = { category = key },
                                    )
                                }
                            }

                            // Fila 3: Conteo y ordenamiento en chips compactos
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "${filtered.size} ${if (filtered.size == 1) "artículo" else "artículos"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Vinko.Muted,
                                )
                                CompactSortChips(
                                    listOf("recommended" to "Recomendados", "price_asc" to "$ Menor", "price_desc" to "$ Mayor", "stock" to "Stock"),
                                    sort,
                                    { sort = it },
                                )
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        full {
                            EmptyBox(
                                if (all.isEmpty()) "Aún no hay artículos publicados" else "No hay resultados para esta búsqueda",
                                if (all.isEmpty()) "Sé el primero en vender: activa tu modo vendedor y publica tu stock." else "Prueba con otra palabra clave o selecciona «Todo».",
                                if (all.isEmpty()) "Vender un artículo" else "Limpiar filtros",
                            ) {
                                if (all.isEmpty()) {
                                    if (isSeller) showQuickPublish = true else showSellerActivation = true
                                } else {
                                    search = ""
                                    category = "all"
                                }
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

    // Modal de detalle de producto
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

    // Modal de Habilitar Modo Vendedor (cuando el usuario no es vendedor)
    if (showSellerActivation) {
        SellerActivationModal(
            onDismiss = { showSellerActivation = false },
            onActivated = {
                showSellerActivation = false
                proData.reload()
                showQuickPublish = true
            },
        )
    }

    // Modal de Publicación Rápida con Inventario (para vendedores habilitados)
    if (showQuickPublish && storeId != null) {
        QuickPublishModal(
            storeId = storeId,
            onDismiss = { showQuickPublish = false },
            onPublished = {
                showQuickPublish = false
                data.reload()
                proData.reload()
            },
        )
    }
}

@Composable
private fun MarketSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar artículos...",
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF5F5F7),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = modifier.height(40.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = Vinko.Muted,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = Vinko.Subtle,
                        fontSize = 13.sp,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
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
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(22.dp),
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Limpiar",
                        tint = Vinko.Muted,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactSortChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Vinko.Tint else Color(0xFFF5F5F7))
                    .border(1.dp, if (isSelected) Vinko.Lavender else Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Vinko.Primary else Color(0xFF6B7280),
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Vinko.Primary else Color(0xFFF5F5F7))
            .border(1.dp, if (selected) Vinko.Primary else Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFF4B5563),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Modal: Habilitar modo vendedor (No viene por defecto)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerActivationModal(
    onDismiss: () -> Unit,
    onActivated: () -> Unit,
) {
    val app = LocalApp.current
    val user = LocalUser.current
    val form = rememberFormAction()
    val base = user.name
    var name by rememberSaveable { mutableStateOf(base) }
    var description by rememberSaveable { mutableStateOf("") }
    var documentId by rememberSaveable { mutableStateOf("") }
    var rif by rememberSaveable { mutableStateOf("") }
    var documentFile by remember { mutableStateOf<UploadPart?>(null) }
    var rifFile by remember { mutableStateOf<UploadPart?>(null) }
    var tried by rememberSaveable { mutableStateOf(false) }
    val pickDoc = rememberPhotoPicker { documentFile = it.firstOrNull() }
    val pickRif = rememberPhotoPicker { rifFile = it.firstOrNull() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Vinko.Cream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Vinko.Primary, RoundedCornerShape(20.dp))
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).background(Color.White.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Modo Vendedor", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text("Publica y gestiona tu inventario", style = MaterialTheme.typography.bodySmall, color = Vinko.Lilac)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Esta sección no viene habilitada por defecto. Al activarla podrás vender artículos, fijar tu stock disponible y recibir pedidos directamente sin comisiones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }

            VCard(padding = 6.dp) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.Percent, size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("0% Comisiones", style = MaterialTheme.typography.titleSmall)
                        Text("Recibes el 100% de tus ventas acordando pago directo.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                    }
                }
                LineDivider(Modifier.padding(horizontal = 10.dp))
                Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.Inventory2, size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Control de Inventario", style = MaterialTheme.typography.titleSmall)
                        Text("Define stock disponible para que se descuente con cada compra.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                    }
                }
            }

            form.error?.let { Banner(it, BannerTone.Error) }

            VinkoTextField(
                name,
                { name = it.take(120) },
                "Nombre comercial o de vendedor",
                capitalization = KeyboardCapitalization.Words,
                error = if (tried && name.isBlank()) "Escribe tu nombre comercial o personal." else null,
            )

            VinkoTextField(
                documentId,
                { documentId = it.take(30) },
                "Documento de Identidad (Cédula / Pasaporte)",
                supporting = "Ejemplo: V-12345678",
                imeAction = ImeAction.Next,
            )
            Text("Adjuntar documento de identidad (PDF o JPG)", style = MaterialTheme.typography.labelMedium, color = Vinko.Ink)
            PhotoSlot(
                documentFile,
                if (documentFile == null) "Subir documento de identidad (PDF o JPG)" else "Documento de identidad seleccionado",
                pickDoc,
                height = 110.dp,
            )

            VinkoTextField(
                rif,
                { rif = it.take(30) },
                "RIF (Registro de Información Fiscal)",
                supporting = "Ejemplo: J-12345678-9 o V-12345678-9",
                imeAction = ImeAction.Next,
            )
            Text("Adjuntar RIF (PDF o JPG)", style = MaterialTheme.typography.labelMedium, color = Vinko.Ink)
            PhotoSlot(
                rifFile,
                if (rifFile == null) "Subir RIF (PDF o JPG)" else "RIF seleccionado",
                pickRif,
                height = 110.dp,
            )

            AreaField(
                description,
                { description = it.take(1000) },
                "Descripción breve o zona de entregas",
                placeholder = "Ej: Artículos nuevos y repuestos en Caracas. Envíos nacionales.",
            )

            PrimaryButton(
                "Habilitar modo vendedor",
                loading = form.busy,
                onClick = {
                    tried = true
                    if (name.isBlank()) return@PrimaryButton
                    val slug = slugify(name.ifBlank { "vendedor-${user.id.take(8)}" })
                    form.run("¡Modo vendedor habilitado!", onSuccess = onActivated) {
                        val docFile = documentFile?.let { app.uploadFile(it, "evidence") }
                        val rifFileId = rifFile?.let { app.uploadFile(it, "evidence") }

                        app.ops.call("activate_store", "slug" to slug, "name" to name.trim(), "description" to description.trim())
                        try {
                            if (documentId.isNotBlank()) app.ops.call("save_contact", "kind" to "document", "label" to "Cédula / Documento", "value" to documentId.trim(), "share" to false)
                            if (rif.isNotBlank()) app.ops.call("save_contact", "kind" to "rif", "label" to "RIF", "value" to rif.trim(), "share" to false)
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Modal: Publicar artículo con fotos y control de inventario
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPublishModal(
    storeId: String,
    onDismiss: () -> Unit,
    onPublished: () -> Unit,
) {
    val app = LocalApp.current
    val form = rememberFormAction()
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("USD") }
    var price by rememberSaveable { mutableStateOf("") }
    var stockCount by rememberSaveable { mutableStateOf("1") }
    val pickedPhotos = remember { mutableStateListOf<UploadPart>() }
    var tried by rememberSaveable { mutableStateOf(false) }

    val pickPhoto = rememberPhotoPicker(max = 5) { parts ->
        pickedPhotos.addAll(parts.take(5 - pickedPhotos.size))
    }

    val minor = parseMajor(price)
    val stock = stockCount.toIntOrNull() ?: 0
    val valid = name.isNotBlank() && minor != null && minor > 0 && stock > 0

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Vinko.Cream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Vender un artículo", style = MaterialTheme.typography.headlineSmall)
            Text("Publica en el Marketplace con control estricto de tu stock.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)

            form.error?.let { Banner(it, BannerTone.Error) }

            // Sección de fotos
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Fotos del artículo", style = MaterialTheme.typography.labelMedium, color = Vinko.Ink)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    pickedPhotos.forEachIndexed { i, part ->
                        Box(Modifier.size(90.dp).clip(RoundedCornerShape(12.dp))) {
                            LocalPhoto(part, Modifier.fillMaxSize())
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .background(Vinko.Ink.copy(alpha = 0.7f), CircleShape)
                                    .clickable { pickedPhotos.removeAt(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Quitar", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    if (pickedPhotos.size < 5) {
                        Box(
                            Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Vinko.Tint)
                                .border(1.dp, Vinko.Lilac, RoundedCornerShape(12.dp))
                                .clickable { pickPhoto() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Añadir", style = MaterialTheme.typography.labelSmall, color = Vinko.Primary)
                            }
                        }
                    }
                }
            }

            VinkoTextField(
                name,
                { name = it.take(140) },
                "Título del artículo",
                capitalization = KeyboardCapitalization.Sentences,
                error = if (tried && name.isBlank()) "Escribe el título del artículo." else null,
            )

            // Moneda y Precio
            Text("Moneda y precio", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
            Segmented(listOf("USD" to "USD · Dólares", "VES" to "Bs · Bolívares"), currency, { currency = it })
            MoneyField(price, { price = it }, currency, "Precio de venta", error = if (tried && (minor == null || minor <= 0)) "Indica un precio válido." else null)

            // CONTROL DE INVENTARIO / STOCK OBLIGATORIO
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, Vinko.Line, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Inventario disponible (Stock)", style = MaterialTheme.typography.titleSmall)
                }
                Text("¿Cuántas unidades tienes disponibles para vender?", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = {
                            val c = (stockCount.toIntOrNull() ?: 1) - 1
                            if (c >= 1) stockCount = c.toString()
                        },
                        enabled = stock > 1,
                        modifier = Modifier.size(38.dp).background(Vinko.Tint, CircleShape),
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Menos", tint = if (stock > 1) Vinko.Primary else Vinko.Lilac, modifier = Modifier.size(18.dp))
                    }

                    VinkoTextField(
                        stockCount,
                        { stockCount = it.filter(Char::isDigit).take(5) },
                        "Unidades",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        error = if (tried && stock <= 0) "Mínimo 1 unidad." else null,
                    )

                    IconButton(
                        onClick = {
                            val c = (stockCount.toIntOrNull() ?: 0) + 1
                            stockCount = c.toString()
                        },
                        modifier = Modifier.size(38.dp).background(Vinko.Tint, CircleShape),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Más", tint = Vinko.Primary, modifier = Modifier.size(18.dp))
                    }
                }

                // Chips rápidos de incremento de stock
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 20, 50).forEach { delta ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Vinko.Tint)
                                .clickable { stockCount = delta.toString() }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("$delta unids.", style = MaterialTheme.typography.labelSmall, color = Vinko.Primary)
                        }
                    }
                }
            }

            AreaField(
                description,
                { description = it.take(3000) },
                "Descripción del artículo",
                placeholder = "Estado (nuevo/usado), marca, medidas y detalles relevantes.",
                error = if (tried && description.isBlank()) "Agrega una breve descripción." else null,
            )

            PrimaryButton(
                "Publicar artículo en Marketplace",
                loading = form.busy,
                onClick = {
                    tried = true
                    if (!valid || description.isBlank()) return@PrimaryButton
                    form.run("¡Artículo publicado con éxito!", onSuccess = onPublished) {
                        val prodId = app.ops.call(
                            "add_product",
                            "store" to storeId,
                            "sku" to "",
                            "name" to name.trim(),
                            "description" to description.trim(),
                            "price" to minor,
                            "currency" to currency,
                            "stock" to stock,
                        ).idText()

                        if (prodId != null && pickedPhotos.isNotEmpty()) {
                            pickedPhotos.forEach { part ->
                                runCatching {
                                    val file = app.uploadFile(part, "product")
                                    app.ops.call("add_product_image", "product" to prodId, "file" to file)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

fun LazyGridScope.full(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

// ---------------------------------------------------------------------------------------------
// Tienda (STORE)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreScreen(storeId: String, nav: NavHostController) {
    val app = LocalApp.current
    val user = LocalUser.current
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
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
