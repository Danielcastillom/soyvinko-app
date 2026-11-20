package com.soyvinko.app.ui.feature.pro

import androidx.compose.foundation.background
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.ui.common.AppMode
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalAppMode
import com.soyvinko.app.ui.common.LocalOpenDrawer
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.o
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberAction
import com.soyvinko.app.ui.common.rememberPhotoPicker
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.uploadFile
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.screens.HeroGradient
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

@Composable
fun MyStoreScreen(nav: NavHostController) {
    val app = LocalApp.current
    val mode = LocalAppMode.current
    val openDrawer = LocalOpenDrawer.current
    val pro = rememberResource { app.ops.call("my_professional") }
    ScreenScaffold(
        "Mi tienda y catálogo",
        onBack = if (mode == AppMode.SELLER) null else { { nav.popBackStack() } },
        navigationIcon = if (mode == AppMode.SELLER) {
            {
                IconButton(onClick = openDrawer) {
                    Icon(Icons.Outlined.Menu, contentDescription = "Menú", tint = Vinko.Ink)
                }
            }
        } else null,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            ResourceView(pro) { data ->
                val p = data.obj()
                val store = p?.o("store")
                when {
                    store?.s("status") == "suspended" -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Banner("Tu tienda está suspendida y no aparece en el Marketplace. Escríbenos para revisar tu caso.")
                        SecondaryButton("Contactar a soporte", onClick = { nav.navigate(Routes.SUPPORT) })
                    }
                    store == null || store.s("status") != "active" -> StoreActivation(p ?: JsonObject(emptyMap()), nav, onActivated = pro.reload)
                    else -> StoreDashboard(store, nav)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Activación de la tienda
// ---------------------------------------------------------------------------------------------

@Composable
private fun StoreActivation(p: JsonObject, nav: NavHostController, onActivated: () -> Unit) {
    val app = LocalApp.current
    val form = rememberFormAction()
    val user = LocalUser.current
    val base = p.o("store")?.s("name") ?: p.s("business_name") ?: user.name
    var name by rememberSaveable { mutableStateOf(base) }
    var slug by rememberSaveable { mutableStateOf(p.o("store")?.s("slug") ?: slugify(base.ifBlank { "tienda-${user.firstName.ifBlank { "vendedor" }}" })) }
    var slugEdited by rememberSaveable { mutableStateOf(false) }
    var description by rememberSaveable { mutableStateOf(p.o("store")?.s("description") ?: "") }
    var tried by rememberSaveable { mutableStateOf(false) }
    val slugOk = SLUG_PATTERN.matches(slug) && slug.length <= 80

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.fillMaxWidth().background(Vinko.Primary, RoundedCornerShape(24.dp)).padding(22.dp)) {
            Box(Modifier.size(52.dp).background(Color.White.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Abre tu tienda en SoyVinko", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(
                "Vende herramientas, repuestos, insumos o cualquier artículo a clientes de toda la red. Recibes el 100% de tus ventas.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        VCard(padding = 6.dp) {
            StoreBenefit(Icons.Outlined.Percent, "0% comisión", "El dinero de tus ventas es íntegro para ti.")
            StoreBenefit(Icons.Outlined.Category, "Cualquier producto", "Publica artículos de cualquier categoría.")
            StoreBenefit(Icons.Outlined.Inventory2, "Control de inventario", "Precios, stock y pedidos desde tu teléfono.", last = true)
        }
        Text("Datos de tu tienda", style = MaterialTheme.typography.titleLarge)
        form.error?.let { Banner(it) }
        VinkoTextField(
            name,
            {
                name = it.take(120)
                if (!slugEdited) slug = slugify(it).take(80)
            },
            "Nombre comercial de la tienda",
            capitalization = KeyboardCapitalization.Words,
            error = if (tried && name.isBlank()) "Escribe el nombre de tu tienda." else null,
        )
        VinkoTextField(
            slug,
            {
                slugEdited = true
                slug = it.lowercase().replace(' ', '-').filter { c -> c.isLetterOrDigit() || c == '-' }.take(80)
            },
            "Identificador de la tienda",
            supporting = "Letras minúsculas, números y guiones. Ejemplo: ferreteria-el-valle",
            error = if (tried && !slugOk) "Usa solo letras minúsculas, números y guiones." else null,
        )
        AreaField(description, { description = it.take(2000) }, "Descripción de tu tienda", placeholder = "Qué vendes, zonas de entrega y formas de pago.")
        PrimaryButton("Activar mi tienda", loading = form.busy, onClick = {
            tried = true
            val clean = slugify(slug)
            if (name.isBlank() || !SLUG_PATTERN.matches(clean)) return@PrimaryButton
            slug = clean
            form.run("¡Tu tienda está activa!", onSuccess = onActivated) {
                app.ops.call("activate_store", "slug" to clean, "name" to name.trim(), "description" to description.trim())
            }
        })
        Text(
            "Abrir tu tienda en el Marketplace es 100% gratuito, con 0% de comisión en ventas y control de inventario directo.",
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Muted,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StoreBenefit(icon: ImageVector, title: String, text: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
    }
    if (!last) LineDivider(Modifier.padding(horizontal = 10.dp))
}

// ---------------------------------------------------------------------------------------------
// Panel de la tienda
// ---------------------------------------------------------------------------------------------

@Composable
private fun StoreDashboard(store: JsonObject, nav: NavHostController) {
    val app = LocalApp.current
    val user = LocalUser.current
    val products = rememberResource { app.ops.call("my_products").objects() }
    val orders = rememberResource(ProSignals.orders) { app.ops.call("my_orders").objects() }
    val form = rememberFormAction()
    val upload = rememberAction()
    var tab by rememberSaveable { mutableStateOf("products") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pickFor by remember { mutableStateOf<String?>(null) }
    var uploadingFor by remember { mutableStateOf<String?>(null) }
    val pick = rememberPhotoPicker { parts ->
        val productId = pickFor
        val part = parts.firstOrNull()
        if (productId != null && part != null) {
            upload.run("Foto añadida al producto.") {
                uploadingFor = productId
                try {
                    val file = app.uploadFile(part, "product")
                    app.ops.call("add_product_image", "product" to productId, "file" to file)
                    products.reload()
                } finally {
                    uploadingFor = null
                }
            }
        }
    }
    val list = products.data.orEmpty()
    val sales = orders.data.orEmpty().filter { it.s("customer_id") != user.id }
    val pendingSales = sales.count { it.s("status") == "pending" }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Monogram(store.s("name") ?: "Tienda", size = 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(store.s("name") ?: "Mi tienda", style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Vinko.Mint, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("Tienda activa", style = MaterialTheme.typography.labelMedium, color = Vinko.Mint)
                    }
                }
                store.s("id")?.let { id -> SmallButton("Ver tienda", onClick = { nav.navigate(Routes.store(id)) }, tone = Tone.Soft) }
            }
            store.s("description")?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                Metric("${list.count { it.s("status") == "active" }}", "Publicados", Modifier.weight(1f))
                Metric("${list.sumOf { it.l("stock") ?: 0L }}", "Unidades", Modifier.weight(1f))
                Metric("0%", "Comisión", Modifier.weight(1f), Vinko.Mint)
            }
        }

        Segmented(
            listOf(
                "products" to "Inventario (${list.size})",
                "orders" to when (pendingSales) { 0 -> "Pedidos (${sales.size})"; 1 -> "Pedidos (1 nuevo)"; else -> "Pedidos ($pendingSales nuevos)" },
            ),
            tab,
            { tab = it },
        )

        if (tab == "products") {
            SectionHeader("Catálogo e inventario") {
                SmallButton("Añadir", onClick = { form.reset(); creating = true }, icon = Icons.Outlined.Add)
            }
            ResourceView(products) { items ->
                if (items.isEmpty()) {
                    EmptyBox(
                        "Aún no tienes productos",
                        "Publica lo que quieres vender. Tus productos activos aparecen en el Marketplace al instante.",
                        "Añadir producto",
                    ) { form.reset(); creating = true }
                } else {
                    val scope = rememberCoroutineScope()
                    fun adjustStock(prod: JsonObject, delta: Int) {
                        val current = (prod.l("stock") ?: 0L).toInt()
                        val next = (current + delta).coerceAtLeast(0)
                        scope.launch {
                            runCatching {
                                app.ops.call(
                                    "update_product",
                                    "product" to prod.s("id"),
                                    "name" to (prod.s("name") ?: ""),
                                    "description" to (prod.s("description") ?: ""),
                                    "price" to (prod.l("price_minor") ?: 0L),
                                    "stock" to next,
                                    "status" to (prod.s("status") ?: "active"),
                                )
                                products.reload()
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items.forEach { p ->
                            StoreProductRow(
                                p,
                                uploading = uploadingFor == p.s("id"),
                                onEdit = { form.reset(); editingId = p.s("id") },
                                onPhoto = {
                                    pickFor = p.s("id")
                                    pick()
                                },
                                onAdjustStock = { delta -> adjustStock(p, delta) },
                            )
                        }
                        Text(
                            "Gestiona las fotos de cada producto desde «Editar». Las fotos «En revisión» aún no se muestran en el catálogo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Vinko.Muted,
                        )
                    }
                }
            }
        } else {
            SectionHeader("Pedidos recibidos", "Acepta, envía y coordina la entrega")
            ResourceView(orders) {
                if (sales.isEmpty()) {
                    EmptyBox("Aún no tienes ventas", "Cuando un cliente haga un pedido a tu tienda, aparecerá aquí para que lo aceptes.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        sales.forEach { o -> OrderCard(o, user.id, store.s("name"), onChanged = { orders.reload(); products.reload() }) }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // Se busca en la lista actual para que las fotos se refresquen al añadir o quitar.
    val current = editingId?.let { id -> list.firstOrNull { it.s("id") == id } }
    if (creating || current != null) {
        ProductFormSheet(
            storeId = store.s("id").orEmpty(),
            existing = if (creating) null else current,
            form = form,
            onDismiss = { creating = false; editingId = null },
            onSaved = { creating = false; editingId = null; products.reload() },
            onCreated = { id ->
                // Tras publicar, se abre el editor para añadir fotos.
                creating = false
                editingId = id
                products.reload()
            },
            onPhotosChanged = products.reload,
        )
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier, color: Color = Vinko.Ink) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
    }
}

private fun productStatus(status: String?): Triple<String, Color, Color> = when (status) {
    "active" -> Triple("Activo", Vinko.Mint, Vinko.MintBg)
    "draft" -> Triple("Borrador", Vinko.Amber, Vinko.AmberBg)
    else -> Triple("Archivado", Vinko.Muted, Vinko.Tint)
}

/** Foto de un producto propio con su estado de moderación (my_products). */
data class ProductPhoto(val file: String, val approved: Boolean)

fun JsonObject.photos(): List<ProductPhoto> =
    (this["images"] as? JsonArray)?.mapNotNull { e ->
        when (e) {
            is JsonObject -> e.s("file_id")?.let { ProductPhoto(it, e.s("moderation_status") == "approved") }
            is JsonPrimitive -> e.contentOrNull?.let { ProductPhoto(it, true) }
            else -> null
        }
    }.orEmpty()

@Composable
private fun StoreProductRow(
    p: JsonObject,
    uploading: Boolean,
    onEdit: () -> Unit,
    onPhoto: () -> Unit,
    onAdjustStock: ((Int) -> Unit)? = null,
) {
    val stock = p.l("stock") ?: 0L
    val photos = p.photos()
    VCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box {
                ProductImage(photos.firstOrNull()?.file, p.s("id"), Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)), iconSize = 30.dp)
                if (photos.size > 1) {
                    Text(
                        "${photos.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Vinko.Ink.copy(alpha = 0.7f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(p.s("name") ?: "", style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    val (label, fg, bg) = productStatus(p.s("status"))
                    Pill(label, fg, bg)
                }
                p.s("sku")?.takeIf { it.isNotBlank() }?.let { Text("SKU $it", style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle) }
                Spacer(Modifier.height(4.dp))
                Text(money(p.l("price_minor"), p.s("currency")?.trim()), style = MaterialTheme.typography.titleSmall, color = Vinko.Primary)
                if (photos.any { !it.approved }) {
                    Spacer(Modifier.height(4.dp))
                    Pill("Foto en revisión", Vinko.Amber, Vinko.AmberBg)
                } else if (photos.isEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Pill("Sin fotos", Vinko.Muted, Vinko.Tint)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Vinko.Tint, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (stock <= 0) "Inventario: Agotado" else "Inventario: $stock unids.",
                style = MaterialTheme.typography.labelMedium,
                color = if (stock <= 0) Vinko.Coral else Vinko.Ink,
                modifier = Modifier.weight(1f),
            )
            if (onAdjustStock != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (stock > 0) Color.White else Color.Transparent)
                            .clickable(enabled = stock > 0) { onAdjustStock(-1) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Menos", tint = if (stock > 0) Vinko.Primary else Vinko.Lilac, modifier = Modifier.size(14.dp))
                    }
                    Text("$stock", style = MaterialTheme.typography.titleSmall, color = Vinko.Primary)
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onAdjustStock(1) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Más", tint = Vinko.Primary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallButton("Editar", onClick = onEdit, icon = Icons.Outlined.Edit, tone = Tone.Outline)
            SmallButton("Añadir foto", onClick = onPhoto, icon = Icons.Outlined.AddPhotoAlternate, tone = Tone.Soft, loading = uploading)
        }
    }
}

/** Fotos del producto en el editor: estado de moderación, añadir y quitar con confirmación. */
@Composable
private fun ProductPhotos(productId: String, photos: List<ProductPhoto>, onChanged: () -> Unit) {
    val app = LocalApp.current
    val photoForm = rememberFormAction()
    var confirming by remember { mutableStateOf<String?>(null) }
    val pick = rememberPhotoPicker { parts ->
        parts.firstOrNull()?.let { part ->
            photoForm.run("Foto añadida al producto.", onSuccess = onChanged) {
                val file = app.uploadFile(part, "product")
                app.ops.call("add_product_image", "product" to productId, "file" to file)
            }
        }
    }
    Text("Fotos (${photos.size})", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
    photoForm.error?.let { Banner(it) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        photos.forEachIndexed { i, photo ->
            Box(Modifier.size(108.dp)) {
                ProImage(photo.file, Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)))
                if (!photo.approved) {
                    Box(Modifier.align(Alignment.BottomStart).padding(6.dp)) { Pill("En revisión", Vinko.Amber, Vinko.AmberBg) }
                }
                DeleteIconButton(
                    "Quitar foto ${i + 1}",
                    onClick = { photoForm.reset(); confirming = photo.file },
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    size = 30.dp,
                    overlay = true,
                )
            }
        }
        Box(
            Modifier
                .size(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Vinko.Tint)
                .border(1.dp, Vinko.Lilac, RoundedCornerShape(14.dp))
                .clickable(enabled = !photoForm.busy) { pick() },
            contentAlignment = Alignment.Center,
        ) {
            if (photoForm.busy && confirming == null) {
                CircularProgressIndicator(color = Vinko.Primary, strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Añadir foto", style = MaterialTheme.typography.labelMedium, color = Vinko.Primary)
                }
            }
        }
    }
    if (photos.any { !it.approved }) {
        Text("Las fotos «En revisión» aún no se muestran en el catálogo.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
    }
    confirming?.let { file ->
        Column(
            Modifier.fillMaxWidth().background(Vinko.CoralBg, RoundedCornerShape(14.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("¿Quitar esta foto del producto?", style = MaterialTheme.typography.titleSmall, color = Vinko.Coral)
            Text("Dejará de mostrarse en el catálogo y se borrará de forma definitiva.", style = MaterialTheme.typography.bodySmall, color = Vinko.Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("Quitar foto", tone = Tone.Danger, loading = photoForm.busy, onClick = {
                    photoForm.run("Foto quitada.", onSuccess = { confirming = null; onChanged() }) {
                        app.ops.call("remove_product_image", "product" to productId, "file" to file)
                    }
                })
                SmallButton("Cancelar", tone = Tone.Outline, enabled = !photoForm.busy, onClick = { confirming = null })
            }
        }
    }
}

@Composable
private fun ProductFormSheet(
    storeId: String,
    existing: JsonObject?,
    form: FormAction,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onCreated: (String) -> Unit,
    onPhotosChanged: () -> Unit,
) {
    val app = LocalApp.current
    var sku by remember { mutableStateOf(existing?.s("sku") ?: "") }
    var name by remember { mutableStateOf(existing?.s("name") ?: "") }
    var description by remember { mutableStateOf(existing?.s("description") ?: "") }
    var currency by remember { mutableStateOf(existing?.s("currency")?.trim() ?: "VES") }
    var price by remember { mutableStateOf(majorText(existing?.l("price_minor"))) }
    var stock by remember { mutableStateOf(existing?.l("stock")?.toString() ?: "") }
    var status by remember { mutableStateOf(existing?.s("status") ?: "active") }
    var tried by remember { mutableStateOf(false) }
    val minor = parseMajor(price)
    val stockValue = stock.toIntOrNull()
    val errors = mapOf(
        "sku" to (existing == null && sku.isBlank()),
        "name" to name.isBlank(),
        "description" to description.isBlank(),
        "price" to (minor == null || minor <= 0),
        "stock" to (stockValue == null || stockValue < 0),
    )
    FormSheet(
        if (existing == null) "Nuevo producto" else "Editar producto",
        onDismiss,
        subtitle = if (existing == null) "Se publica en el Marketplace al guardarlo. Después podrás añadir fotos." else null,
        error = form.error,
    ) {
        if (existing != null) {
            existing.s("id")?.let { ProductPhotos(it, existing.photos(), onPhotosChanged) }
            LineDivider()
        }
        if (existing == null) {
            VinkoTextField(sku, { sku = it.take(60) }, "Código SKU o referencia",
                capitalization = KeyboardCapitalization.Characters,
                error = if (tried && errors["sku"] == true) "Escribe un código para identificar el producto." else null)
        } else {
            KeyValue("SKU", existing.s("sku") ?: "—")
        }
        VinkoTextField(name, { name = it.take(160) }, "Nombre del producto", capitalization = KeyboardCapitalization.Sentences,
            error = if (tried && errors["name"] == true) "Escribe el nombre del producto." else null)
        AreaField(description, { description = it.take(4000) }, "Descripción", placeholder = "Marca, modelo, estado, garantía y medidas.",
            error = if (tried && errors["description"] == true) "Describe el producto." else null)
        if (existing == null) {
            Text("Moneda", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
            Segmented(listOf("VES" to "Bs · Bolívar", "USD" to "USD · Dólar"), currency, { currency = it })
        } else {
            KeyValue("Moneda", if (currency == "USD") "USD · Dólar" else "Bs · Bolívar")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1.2f)) {
                MoneyField(price, { price = it }, currency, "Precio", error = if (tried && errors["price"] == true) "Precio inválido." else null)
            }
            Box(Modifier.weight(1f)) {
                VinkoTextField(stock, { stock = it.filter(Char::isDigit).take(6) }, "Stock", keyboardType = KeyboardType.Number,
                    error = if (tried && errors["stock"] == true) "Indica unidades." else null)
            }
        }
        if (existing != null) {
            Text("Estado de publicación", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
            Segmented(listOf("active" to "Activo", "draft" to "Borrador", "archived" to "Archivado"), status, { status = it })
            Text(
                when (status) {
                    "active" -> "Visible en el Marketplace."
                    "draft" -> "Oculto mientras lo preparas."
                    else -> "Retirado del catálogo."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Muted,
            )
        }
        PrimaryButton(if (existing == null) "Publicar producto" else "Guardar cambios", loading = form.busy, onClick = {
            tried = true
            if (errors.values.any { it }) return@PrimaryButton
            if (existing == null) {
                form.runFor {
                    val id = app.ops.call(
                        "add_product",
                        "store" to storeId,
                        "sku" to sku.trim(),
                        "name" to name.trim(),
                        "description" to description.trim(),
                        "price" to minor,
                        "currency" to currency,
                        "stock" to stockValue,
                    ).idText()
                    if (id != null) onCreated(id) else onSaved()
                    "Producto publicado. Añade sus fotos."
                }
                return@PrimaryButton
            }
            form.run("Producto actualizado.", onSuccess = onSaved) {
                app.ops.call(
                    "update_product",
                    "product" to existing.s("id"),
                    "name" to name.trim(),
                    "description" to description.trim(),
                    "price" to minor,
                    "stock" to stockValue,
                    "status" to status,
                )
            }
        })
    }
}
