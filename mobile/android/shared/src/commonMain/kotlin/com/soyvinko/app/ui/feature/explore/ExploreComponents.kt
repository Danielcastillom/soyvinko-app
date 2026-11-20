package com.soyvinko.app.ui.feature.explore

import com.soyvinko.app.ui.common.buildApiImageLoader
import coil3.compose.LocalPlatformContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.common.ApiImage
import com.soyvinko.app.ui.common.LocalApp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.soyvinko.app.ui.common.rememberApiImageLoader
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko

val CardShape = RoundedCornerShape(14.dp)

private val Covers = listOf(
    Brush.linearGradient(listOf(Color(0xFF3B1D8F), Vinko.Primary)),
    Brush.linearGradient(listOf(Vinko.Primary, Vinko.Lavender)),
    Brush.linearGradient(listOf(Vinko.Dark, Color(0xFF3B1D8F))),
    Brush.linearGradient(listOf(Color(0xFF5B2FE6), Color(0xFF42C9FF))),
)

fun coverBrush(index: Int): Brush = Covers[(index).mod(Covers.size)]

private val Shade = Brush.verticalGradient(listOf(Color(0x14150D2B), Color(0x00150D2B), Color(0xB3150D2B)))

private var sharedLoader: Pair<Any, ImageLoader>? = null

/** Transición al mostrar imágenes; las pruebas de capturas la desactivan (el reloj de Robolectric no avanza solo). */
var imageCrossfade = true

/** Un único ImageLoader (con la sesión) para toda la sección, así la caché de imágenes se comparte entre pantallas. */
@Composable
fun exploreImageLoader(): ImageLoader {
    val app = LocalApp.current
    val context = LocalPlatformContext.current
    val api = app.api
    return remember(api) {
        sharedLoader?.takeIf { it.first === api }?.second
            ?: buildApiImageLoader(context, app, imageCrossfade)
                .also { sharedLoader = api to it }
    }
}

/**
 * Como ApiImage (ruta pública "/images/..." o id de archivo privado) pero sin fondo propio:
 * si la imagen no carga se ve lo que haya debajo (p. ej. el degradado de la portada).
 */
@Composable
fun NetImage(fileIdOrPath: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop, description: String? = null) {
    val app = LocalApp.current
    val url = when {
        fileIdOrPath == null -> null
        fileIdOrPath.startsWith("http") -> fileIdOrPath
        fileIdOrPath.startsWith("/") -> app.api.absoluteUrl(fileIdOrPath)
        else -> app.api.absoluteUrl("/api/v1/files/$fileIdOrPath")
    }
    AsyncImage(model = url, imageLoader = exploreImageLoader(), contentDescription = description, contentScale = contentScale, modifier = modifier)
}

/** Portada de la tarjeta: ilustración de la categoría o un degradado de marca. */
@Composable
fun Cover(image: String?, index: Int, modifier: Modifier = Modifier) {
    Box(modifier) {
        Box(Modifier.fillMaxSize().background(coverBrush(index)))
        if (image != null) NetImage(image, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Shade))
    }
}

@Composable
fun Avatar(
    initials: String,
    size: Dp = 44.dp,
    border: Boolean = true,
    photoUrl: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val app = LocalApp.current
    var hasError by remember(photoUrl) { mutableStateOf(false) }
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Vinko.Primary, Vinko.Lavender)), CircleShape)
            .then(if (border) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials.trim().take(2).uppercase().ifEmpty { "V" },
            fontFamily = Manrope,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.36f).sp,
            color = Color.White,
        )
        if (!photoUrl.isNullOrBlank() && !hasError) {
            val url = when {
                photoUrl.startsWith("http") -> photoUrl
                photoUrl.startsWith("/") -> app.api.absoluteUrl(photoUrl)
                else -> app.api.absoluteUrl("/api/v1/files/$photoUrl")
            }
            AsyncImage(
                model = url,
                imageLoader = rememberApiImageLoader(),
                contentDescription = initials,
                contentScale = ContentScale.Crop,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Error) {
                        hasError = true
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun Chip(
    text: String,
    icon: ImageVector? = null,
    fg: Color = Vinko.Primary,
    bg: Color = Vinko.Tint,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, fontSize = 11.sp, color = fg, maxLines = 1)
    }
}

@Composable
fun VerifiedChip(long: Boolean = false) =
    Chip(if (long) "Identidad verificada" else "Verificado", Icons.Outlined.Verified, Vinko.Mint, Vinko.MintBg)

@Composable
fun ModeChip(mode: String?, onDark: Boolean = false) {
    val remote = mode == "remote"
    Chip(
        if (remote) "Remoto" else "Presencial",
        if (remote) Icons.Outlined.Public else Icons.Outlined.BusinessCenter,
        fg = if (onDark) Color.White else Vinko.Primary,
        bg = if (onDark) Color.White.copy(alpha = 0.18f) else Vinko.Tint,
    )
}

@Composable
fun RatingRow(rating: Double?, count: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Star, contentDescription = null, tint = Color(0xFFF5A524), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(ratingLabel(rating), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(4.dp))
        Text(reviewsLabel(count), style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
    }
}

@Composable
fun Stars(rating: Int, size: Dp = 16.dp) {
    Row {
        repeat(5) { i ->
            Icon(
                if (i < rating) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (i < rating) Color(0xFFF5A524) else Vinko.Lilac,
                modifier = Modifier.size(size),
            )
        }
    }
}

/** Tarjeta de profesional (equivalente nativo de ProfessionalCard de la web). */
@Composable
fun ProfessionalCard(
    pro: Pro,
    categories: Map<String, Category>,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    topEnd: (@Composable () -> Unit)? = null,
) {
    val first = pro.offerings.firstOrNull()
    val category = first?.categoryId?.let { categories[it] }
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.White)
            .border(1.dp, Vinko.Line, CardShape)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            Cover(serviceImage(category?.slug), index, Modifier.fillMaxSize())
            Row(Modifier.align(Alignment.TopStart).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (category != null) Chip(category.shortName(), fg = Vinko.Ink, bg = Color.White.copy(alpha = 0.92f))
                if (pro.sponsored) Chip("Destacado", Icons.Outlined.WorkspacePremium, Vinko.Amber, Vinko.AmberBg)
            }
            if (topEnd != null) Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) { topEnd() }
            Row(Modifier.align(Alignment.BottomStart).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(pro.initials, 36.dp)
                Spacer(Modifier.width(8.dp))
                ModeChip(first?.deliveryMode, onDark = true)
            }
        }
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pro.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (pro.verified) {
                    Spacer(Modifier.width(6.dp))
                    VerifiedChip()
                }
                Spacer(Modifier.width(6.dp))
                RatingRow(pro.rating, pro.reviewCount)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    first?.title ?: "Servicios profesionales",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = Vinko.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Outlined.Place, contentDescription = null, tint = Vinko.Subtle, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(2.dp))
                Text(
                    pro.city ?: "Remoto",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Vinko.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                distanceLabel(pro.distanceM)?.let {
                    Spacer(Modifier.width(6.dp))
                    Chip("a $it", fg = Vinko.Cyan, bg = Vinko.CyanBg)
                }
            }
            if (!pro.bio.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(pro.bio, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Color(0xFF6B7280), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Precio base / inspección", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Vinko.Muted)
                    Text(money(first?.priceMin, first?.currency ?: "VES"), style = MaterialTheme.typography.titleMedium, fontSize = 15.sp, color = Vinko.Ink, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Vinko.Primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("Solicitar", style = MaterialTheme.typography.labelMedium, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Marcador de carga con la forma de la tarjeta. */
@Composable
fun ProfessionalCardSkeleton() {
    Column(Modifier.fillMaxWidth().clip(CardShape).background(Color.White).border(1.dp, Vinko.Line, CardShape)) {
        Box(Modifier.fillMaxWidth().height(96.dp).background(Vinko.Tint))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth(0.6f).height(14.dp).background(Vinko.Tint, RoundedCornerShape(6.dp)))
            Box(Modifier.fillMaxWidth(0.4f).height(12.dp).background(Vinko.Tint, RoundedCornerShape(6.dp)))
            Box(Modifier.fillMaxWidth(0.3f).height(18.dp).background(Vinko.Tint, RoundedCornerShape(6.dp)))
        }
    }
}

/** Control segmentado de la marca (equivalente a `.segmented` de la web). */
@Composable
fun Segmented(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    disabled: Set<String> = emptySet(),
    icons: Map<String, ImageVector> = emptyMap(),
) {
    Row(
        modifier.background(Vinko.Tint, RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            val enabled = value !in disabled
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) Color.White else Color.Transparent)
                    .then(if (active) Modifier.border(1.dp, Vinko.Line, RoundedCornerShape(10.dp)) else Modifier)
                    .clickable(enabled = enabled) { onSelect(value) }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = when {
                    !enabled -> Vinko.Lilac
                    active -> Vinko.Primary
                    else -> Vinko.Muted
                }
                icons[value]?.let {
                    Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Píldora seleccionable para filtros (radio, orden, estado). */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Vinko.Primary else Color.White)
            .border(1.dp, if (selected) Vinko.Primary else Vinko.Line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) Color.White else Vinko.Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else Vinko.Ink, maxLines = 1)
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, action: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action()
    }
}

/** Campo de texto multilínea con el estilo de VinkoTextField. */
@Composable
fun AreaField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supporting: String? = null,
    maxLength: Int = 10000,
    minLines: Int = 4,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        minLines = minLines,
        maxLines = 10,
        isError = error != null,
        shape = RoundedCornerShape(14.dp),
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = fieldColors(),
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Vinko.Primary,
    unfocusedBorderColor = Vinko.Line,
    focusedContainerColor = Vinko.FieldBg,
    unfocusedContainerColor = Vinko.FieldBg,
    disabledContainerColor = Vinko.FieldBg,
    focusedLabelColor = Vinko.Primary,
    unfocusedLabelColor = Vinko.Primary,
)

/** Lista desplegable (categoría, etc.). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectField(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    placeholder: String = "Selecciona una opción",
    icons: (String) -> ImageVector? = { null },
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = current?.second ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = current?.let { icons(it.first) }?.let { { Icon(it, contentDescription = null, tint = Vinko.Lavender) } },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }, containerColor = Color.White) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = icons(value)?.let { { Icon(it, contentDescription = null, tint = Vinko.Primary) } },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/** Foto local (aún no subida) desde sus bytes. */
@Composable
fun LocalPhoto(part: UploadPart, modifier: Modifier = Modifier) {
    val bitmap = remember(part) { decodeThumb(part.bytes) }
    if (bitmap != null) Image(bitmap, contentDescription = part.fileName, contentScale = ContentScale.Crop, modifier = modifier)
    else Box(modifier.background(Vinko.Tint))
}

/** Fila etiqueta/valor para bloques de detalles. */
@Composable
fun InfoRow(label: String, value: String, icon: ImageVector? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Vinko.Lavender, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
    }
}

/** Punto numerado de "Así funciona". */
@Composable
fun Step(number: Int, text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(24.dp).background(Vinko.Tint, CircleShape), contentAlignment = Alignment.Center) {
            Text("$number", style = MaterialTheme.typography.labelMedium, color = Vinko.Primary)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
    }
}
