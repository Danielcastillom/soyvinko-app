package com.soyvinko.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko

/**
 * Chip selector de ubicación geográfica del Home:
 * Muestra la zona actual (ej. Caracas, Chacao) y permite elegir otra ubicación en el mapa.
 */
@Composable
fun HomeLocationBar(
    currentLocation: String = "Caracas, Chacao",
    onSelectOther: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Vinko.Tint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Vinko.Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Ubicación del servicio",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Vinko.Subtle,
                )
                Text(
                    text = currentLocation,
                    style = MaterialTheme.typography.titleSmall,
                    color = Vinko.Ink,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        OutlinedButton(
            onClick = onSelectOther,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Vinko.Primary),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "Otra ubicación",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Barra de búsqueda interactiva en formato píldora:
 * Coincide con "¿Qué servicio necesitas hoy?" del prototipo FlutterFlow.
 */
@Composable
fun HomeSearchPill(
    placeholder: String = "¿Qué servicio necesitas hoy?",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color.White, RoundedCornerShape(26.dp))
            .border(1.dp, Vinko.Line, RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "Buscar",
                tint = Vinko.Primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = Vinko.Muted,
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Vinko.Tint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = Vinko.Primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

data class CategoryItem(val id: String, val name: String, val icon: ImageVector, val color: Color, val bg: Color)

/**
 * Carrusel horizontal de rubros destacados ("Servicios más solicitados").
 */
@Composable
fun HomeCategoriesSection(
    onCategorySelected: (String) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = listOf(
        CategoryItem("plomeria", "Plomería", Icons.Outlined.Build, Color(0xFF0284C7), Color(0xFFEBF8FF)),
        CategoryItem("electricidad", "Electricidad", Icons.Outlined.Bolt, Color(0xFFD97706), Color(0xFFFFFBEB)),
        CategoryItem("climatizacion", "Aire Acondicionado", Icons.Outlined.Construction, Color(0xFF0C8E72), Color(0xFFE6FAF5)),
        CategoryItem("cerrajeria", "Cerrajería", Icons.Outlined.Key, Color(0xFF6F3DFF), Color(0xFFF0EBFF)),
        CategoryItem("pintura", "Pintura", Icons.Outlined.FormatPaint, Color(0xFFD13B59), Color(0xFFFFF0F3)),
        CategoryItem("limpieza", "Limpieza", Icons.Outlined.CleaningServices, Color(0xFF0D9488), Color(0xFFF0FDFA)),
        CategoryItem("reparaciones", "Albañilería", Icons.Outlined.Construction, Color(0xFF475569), Color(0xFFF1F5F9)),
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Servicios más solicitados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Vinko.Ink,
            )
            TextButton(onClick = onSeeAll) {
                Text("Ver todos", color = Vinko.Primary, style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            categories.forEach { cat ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(88.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, Vinko.Line, RoundedCornerShape(16.dp))
                        .clickable { onCategorySelected(cat.name) }
                        .padding(vertical = 14.dp, horizontal = 6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(cat.bg, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(cat.icon, contentDescription = cat.name, tint = cat.color, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Vinko.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

data class VerifiedProviderDemo(
    val name: String,
    val specialty: String,
    val rating: String,
    val reviews: String,
    val distance: String,
    val initial: String,
)

/**
 * Sección de proveedores verificados destacados.
 */
@Composable
fun HomeVerifiedProvidersSection(
    onProviderClick: (String) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val providers = listOf(
        VerifiedProviderDemo("Carlos Mendoza", "Técnico en Refrigeración y AA", "4.9", "34", "A 1.2 km", "C"),
        VerifiedProviderDemo("Alejandro Silva", "Electricista Residencial e Industrial", "5.0", "48", "A 2.5 km", "A"),
        VerifiedProviderDemo("Roberto Gómez", "Plomero Certificado y Destapes", "4.8", "29", "A 3.1 km", "R"),
    )

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Proveedores verificados cerca de ti",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Vinko.Ink,
            )
            TextButton(onClick = onSeeAll) {
                Text("Explorar", color = Vinko.Primary, style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            providers.forEach { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, Vinko.Line, RoundedCornerShape(16.dp))
                        .clickable { onProviderClick(p.name) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Vinko.Tint, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = p.initial,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Vinko.Primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = p.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Vinko.Ink,
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Outlined.Verified,
                                contentDescription = "Verificado",
                                tint = Vinko.Mint,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = p.specialty,
                            style = MaterialTheme.typography.bodySmall,
                            color = Vinko.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "${p.rating} (${p.reviews})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Vinko.Ink,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "· ${p.distance}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Vinko.Subtle,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onProviderClick(p.name) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Vinko.Primary),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("Ver", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/**
 * Banner de confianza y protección Vinko.
 */
@Composable
fun HomeProtectionBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Vinko.Tint, RoundedCornerShape(16.dp))
            .border(1.dp, Vinko.Lilac.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Shield, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Garantía de Pago Protegido",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Vinko.Ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Tu dinero se mantiene en custodia segura y solo se libera cuando confirmes que el servicio fue realizado satisfactoriamente.",
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Muted,
                lineHeight = 16.sp,
            )
        }
    }
}
