package com.soyvinko.app.ui.feature.explore

import com.soyvinko.app.ui.common.fullScreenDialogProperties
import kotlin.math.roundToLong
import coil3.compose.LocalPlatformContext
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.openTurnByTurn
import com.soyvinko.app.ui.common.relative
import com.soyvinko.app.ui.common.rememberAction
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Resultado que puede no existir (perfil inactivo, solicitud ajena) sin confundirlo con un error. */
class Found<T>(val value: T?)

/** Perfil de un profesional: servicios, sedes, trabajos, opiniones, favorito y "Contratar". */
@Composable
fun ProfessionalScreen(nav: NavHostController, id: String) {
    val app = LocalApp.current
    val user = LocalUser.current
    val own = user.id == id
    val profile = rememberResource(id) { Found(app.ops.call("professional_profile", "professional" to id).obj()?.toPro()) }
    val reviews = rememberResource(id) { app.ops.call("professional_reviews", "professional" to id, "limit" to 30).objects() }
    val cats = rememberResource { app.ops.call("catalog").categories() }
    val favorites = rememberResource(id) {
        if (own) emptySet() else app.ops.call("my_favorites").objects().mapNotNull { it.s("id") }.toSet()
    }
    var favorite by remember { mutableStateOf<Boolean?>(null) }
    val isFavorite = favorite ?: (favorites.data?.contains(id) == true)
    val action = rememberAction()
    val byId = remember(cats.data) { cats.data.orEmpty().associateBy { it.id } }
    val pro = profile.data?.value
    var viewing by remember { mutableStateOf<JsonObject?>(null) }

    ScreenScaffold(
        title = "Perfil profesional",
        onBack = { nav.popBackStack() },
        actions = {
            if (!own && pro != null) {
                IconButton(
                    enabled = !action.busy && favorites.data != null,
                    onClick = {
                        val next = !isFavorite
                        action.run(if (next) "Guardamos a ${pro.name} en tus favoritos." else "Quitamos a ${pro.name} de tus favoritos.") {
                            app.ops.call("set_favorite", "professional" to id, "enabled" to next)
                            favorite = next
                        }
                    },
                ) {
                    Icon(
                        if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Guardar en favoritos",
                        tint = if (isFavorite) Vinko.Coral else Vinko.Ink,
                    )
                }
            }
        },
        bottomBar = {
            if (!own && pro != null && pro.offerings.isNotEmpty()) {
                val first = pro.offerings.first()
                Surface(color = Color.White, shadowElevation = 10.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Presupuesto desde", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                            Text(money(first.priceMin, first.currency), style = MaterialTheme.typography.titleMedium)
                        }
                        PrimaryButton(
                            "Contratar",
                            onClick = { nav.navigate(Routes.newRequest(id, pro.offerings.singleOrNull()?.id)) },
                            modifier = Modifier.width(170.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            ResourceView(profile) { found ->
                val p = found.value
                if (p == null) {
                    EmptyBox("Perfil no disponible", "Este profesional no está activo en este momento.", "Explorar profesionales") { goExplore(nav) }
                } else {
                    ProfileContent(
                        pro = p,
                        own = own,
                        categories = byId,
                        reviews = reviews.data,
                        onHire = { offering -> nav.navigate(Routes.newRequest(id, offering)) },
                        onView = { viewing = it },
                    )
                }
            }
        }
    }
    viewing?.let { item -> PortfolioViewer(item) { viewing = null } }
}

fun goExplore(nav: NavHostController) {
    nav.navigate(Routes.EXPLORE) {
        popUpTo(nav.graph.findStartDestination().route.orEmpty()) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileContent(
    pro: Pro,
    own: Boolean,
    categories: Map<String, Category>,
    reviews: List<JsonObject>?,
    onHire: (String) -> Unit,
    onView: (JsonObject) -> Unit,
) {
    val context = LocalPlatformContext.current
    val cover = serviceImage(pro.offerings.firstOrNull()?.categoryId?.let { categories[it] }?.slug)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (own) item { Banner("Así ven los clientes tu perfil profesional.", BannerTone.Info) }
        item {
            Column(Modifier.fillMaxWidth().clip(CardShape).background(Color.White).border(1.dp, Vinko.Line, CardShape)) {
                Box(Modifier.fillMaxWidth().height(132.dp)) {
                    Cover(cover, 0, Modifier.fillMaxSize())
                    Box(Modifier.align(Alignment.BottomStart).padding(14.dp)) { Avatar(pro.initials, 64.dp) }
                    if (pro.sponsored) {
                        Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                            Chip("Destacado", Icons.Outlined.WorkspacePremium, Vinko.Amber, Vinko.AmberBg)
                        }
                    }
                }
                Column(Modifier.padding(16.dp)) {
                    SectionLabel("Profesional SoyVinko")
                    Text(pro.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        pro.years?.takeIf { it > 0 }?.let { "$it ${if (it == 1L) "año" else "años"} de experiencia" } ?: "Disponible para nuevos proyectos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Vinko.Muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (pro.verified) VerifiedChip(long = true)
                        Chip(
                            if (pro.rating != null) "${ratingLabel(pro.rating)} · ${pro.reviewCount} ${if (pro.reviewCount == 1L) "opinión" else "opiniones"}" else "Sin calificaciones aún",
                            Icons.Outlined.Star,
                            Vinko.Amber,
                            Vinko.AmberBg,
                        )
                        if (pro.recommendations > 0) Chip("${pro.recommendations} lo recomiendan", Icons.Outlined.ThumbUp, Vinko.Mint, Vinko.MintBg)
                    }
                }
            }
        }
        item {
            VCard {
                Text("Conoce mi trabajo", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(pro.bio ?: "Este profesional aún no ha escrito su presentación.", style = MaterialTheme.typography.bodyMedium, color = if (pro.bio != null) Vinko.Ink else Vinko.Muted)
                Spacer(Modifier.height(12.dp))
                Fact(Icons.Outlined.Shield, if (pro.requiresPlatformPayment) "Trabaja con pago protegido" else "Acuerda contigo la forma de pago")
                Fact(Icons.Outlined.Handshake, "Condiciones acordadas antes de empezar")
                Fact(Icons.Outlined.ChatBubbleOutline, "Chat directo al aceptar su cotización")
            }
        }
        item { SectionTitle("Servicios", Modifier.padding(top = 6.dp)) }
        if (pro.offerings.isEmpty()) item {
            VCard { Text("Este profesional no tiene servicios publicados por ahora.", style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted) }
        }
        items(pro.offerings, key = { it.id }) { o -> OfferingCard(o, categories[o.categoryId], own) { onHire(o.id) } }
        if (pro.portfolio.isNotEmpty()) {
            item { SectionTitle("Trabajos realizados", Modifier.padding(top = 6.dp)) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(pro.portfolio, key = { it.s("id") ?: it.hashCode().toString() }) { item ->
                        Column(Modifier.width(210.dp).clickable { onView(item) }) {
                            NetImage(item.s("file_id"), Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(Vinko.Tint), description = item.s("title"))
                            Spacer(Modifier.height(6.dp))
                            Text(item.s("title") ?: "Trabajo realizado", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            item.s("description")?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        val remote = pro.offerings.any { it.deliveryMode == "remote" }
        if (pro.branches.isNotEmpty() || remote) item {
            VCard {
                Text("Sedes y zona de atención", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                pro.branches.forEach { b ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Outlined.Place, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.s("name") ?: "Sede", style = MaterialTheme.typography.titleSmall)
                            Text(
                                listOfNotNull(b.s("city"), b.s("address")).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Vinko.Muted,
                            )
                        }
                        val pos = b.geo()
                        if (pos != null) {
                            TextButton(onClick = { runCatching { openTurnByTurn(context, pos, b.s("name") ?: pro.name) } }) {
                                Icon(Icons.Outlined.Directions, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Cómo llegar", color = Vinko.Primary)
                            }
                        }
                    }
                }
                if (remote) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Outlined.Public, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("También atiende proyectos remotos.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (pro.branches.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Atiende servicios presenciales alrededor de sus sedes. Al solicitar, comprobamos que tu dirección esté dentro de su cobertura.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Vinko.Muted,
                    )
                }
            }
        }
        item { SectionTitle("Opiniones y recomendaciones", Modifier.padding(top = 6.dp)) }
        when {
            reviews == null -> item { Text("Cargando opiniones…", style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted) }
            reviews.isEmpty() -> item {
                VCard {
                    Text("Una nueva historia por escribir", style = MaterialTheme.typography.titleSmall)
                    Text("Las opiniones aparecen después de un servicio terminado y revisado.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                }
            }
            else -> {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ratingLabel(pro.rating), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Stars((pro.rating ?: 0.0).roundToLong().toInt(), 18.dp)
                            Text(
                                "${pro.reviewCount} ${if (pro.reviewCount == 1L) "opinión" else "opiniones"}" +
                                    if (pro.recommendations > 0) " · ${pro.recommendations} lo recomiendan" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Vinko.Muted,
                            )
                        }
                    }
                }
                items(reviews, key = { it.s("id") ?: it.hashCode().toString() }) { r -> ReviewCard(r) }
            }
        }
        item {
            VCard(Modifier.padding(top = 6.dp)) {
                Text("Así funciona", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Step(1, "Describe lo que necesitas.")
                Step(2, "Recibe su cotización y confirma el precio.")
                Step(3, "Asigna el trabajo y conversa con el profesional.")
                Step(4, "Valida el trabajo y califica tu experiencia.")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tus datos de contacto se comparten después de aceptar. Tu ubicación exacta se envía solo cuando tú lo decides.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Muted,
                )
            }
        }
    }
}

@Composable
private fun Fact(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfferingCard(o: Offering, category: Category?, own: Boolean, onHire: () -> Unit) {
    VCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ModeChip(o.deliveryMode)
            if (category != null) Chip(category.shortName(), categoryIcon(category.slug), Vinko.Muted, Vinko.Cream)
        }
        Spacer(Modifier.height(10.dp))
        Text(o.title, style = MaterialTheme.typography.titleMedium)
        o.description?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pricingCaption(o.pricingType), style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
                Text(o.priceLabel(), style = MaterialTheme.typography.titleMedium, color = Vinko.Primary)
            }
            if (!own) {
                Button(
                    onClick = onHire,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Vinko.Primary),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                ) { Text("Contratar", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun ReviewCard(r: JsonObject) {
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stars((r.l("rating") ?: 0L).toInt())
            Spacer(Modifier.weight(1f))
            Text(relative(r.s("created_at")), style = MaterialTheme.typography.bodySmall, color = Vinko.Subtle)
        }
        val recommends = r["recommends"]?.let { runCatching { it.jsonPrimitive.booleanOrNull }.getOrNull() }
        if (recommends == true) {
            Spacer(Modifier.height(8.dp))
            Chip("Lo recomienda", Icons.Outlined.ThumbUp, Vinko.Mint, Vinko.MintBg)
        }
        r.s("comment")?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        r.s("response")?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(12.dp)).padding(12.dp)) {
                Text("Respuesta del profesional", style = MaterialTheme.typography.labelMedium, color = Vinko.Primary)
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PortfolioViewer(item: JsonObject, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = fullScreenDialogProperties()) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            NetImage(item.s("file_id"), Modifier.fillMaxSize(), contentScale = ContentScale.Fit, description = item.s("title"))
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = Color.White)
            }
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).navigationBarsPadding().padding(20.dp),
            ) {
                Text(item.s("title") ?: "Trabajo realizado", style = MaterialTheme.typography.titleMedium, color = Color.White)
                item.s("description")?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f)) }
            }
        }
    }
}

/** Favoritos del cliente (`my_favorites`). */
@Composable
fun FavoritesScreen(nav: NavHostController) {
    val app = LocalApp.current
    val favorites = rememberResource { app.ops.call("my_favorites").pros() }
    val cats = rememberResource { app.ops.call("catalog").categories() }
    val byId = remember(cats.data) { cats.data.orEmpty().associateBy { it.id } }
    var removed by remember { mutableStateOf(setOf<String>()) }
    val action = rememberAction()
    ScreenScaffold("Favoritos", onBack = { nav.popBackStack() }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            ResourceView(favorites) { list ->
                val visible = list.filter { it.id !in removed }
                if (visible.isEmpty()) {
                    EmptyBox(
                        "Aún no guardas profesionales",
                        "Toca el corazón en un perfil para tenerlo a mano cuando lo necesites.",
                        "Explorar profesionales",
                    ) { goExplore(nav) }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        item {
                            Text(
                                "${visible.size} ${if (visible.size == 1) "profesional guardado" else "profesionales guardados"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Vinko.Muted,
                            )
                        }
                        items(visible.size, key = { visible[it].id }) { i ->
                            val p = visible[i]
                            ProfessionalCard(
                                p,
                                byId,
                                i,
                                onClick = { nav.navigate(Routes.professional(p.id)) },
                                topEnd = {
                                    IconButton(
                                        enabled = !action.busy,
                                        onClick = {
                                            action.run("Quitamos a ${p.name} de tus favoritos.") {
                                                app.ops.call("set_favorite", "professional" to p.id, "enabled" to false)
                                                removed = removed + p.id
                                            }
                                        },
                                    ) {
                                        Box(Modifier.size(36.dp).background(Color.White, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.Favorite, contentDescription = "Quitar de favoritos", tint = Vinko.Coral, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
