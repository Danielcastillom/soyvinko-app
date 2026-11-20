package com.soyvinko.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import org.jetbrains.compose.resources.FontResource
import com.soyvinko.shared.resources.Res
import com.soyvinko.shared.resources.dm_sans
import com.soyvinko.shared.resources.manrope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta oficial de la Guía Visual de Marca Vinko.
object Vinko {
    // 2. Paleta de colores oficial
    val DeepPurple = Color(0xFF4B1FAE)    // Morado Profundo (#4B1FAE)
    val Primary = Color(0xFF6F3DFF)       // Violeta Principal (#6F3DFF)
    val PrimaryHover = Color(0xFF4B1FAE)  // Morado Profundo (#4B1FAE)
    val Lavender = Color(0xFF9B7BFF)      // Lavanda Dinámica (#9B7BFF)
    val Lilac = Color(0xFFC6B2FF)         // Lila Suave (#C6B2FF)
    val LightGray = Color(0xFFF5F5F7)     // Gris Claro (#F5F5F7)
    val Cream = Color(0xFFFFFFFF)         // Blanco (#FFFFFF)

    // 3. Colores complementarios sugeridos
    val MintLight = Color(0xFF6EE7C8)     // Verde Menta (#6EE7C8)
    val Mint = Color(0xFF0C8E72)          // Verde Menta accesible para texto y acentos
    val MintBg = Color(0xFFE6FAF5)
    val CyanLight = Color(0xFF42C9FF)     // Azul Cian (#42C9FF)
    val Cyan = Color(0xFF0284C7)          // Azul Cian contrastado
    val CyanBg = Color(0xFFEBF8FF)
    val YellowSoft = Color(0xFFFFD76A)    // Amarillo Suave (#FFD76A)
    val Amber = Color(0xFF9E7300)
    val AmberBg = Color(0xFFFFF6DB)
    val CoralLight = Color(0xFFFF8FA3)    // Coral Claro (#FF8FA3)
    val Coral = Color(0xFFD13B59)         // Coral para avisos
    val CoralBg = Color(0xFFFFF0F3)

    // Neutros y superficies
    val Ink = Color(0xFF150D2B)           // Tipografía principal
    val Dark = Color(0xFF150D2B)
    val DarkSurface = Color(0xFF1E1538)
    val Muted = Color(0xFF6B7280)
    val Subtle = Color(0xFF9CA3AF)
    val Line = Color(0xFFE5E7EB)
    val Tint = Color(0xFFF3F0FF)          // Tinte lavanda suave
    val FieldBg = Color(0xFFF5F5F7)       // Gris Claro (#F5F5F7)
}

@Composable
private fun variable(res: FontResource, weight: Int) = brandFont(res, if (res == Res.font.manrope) "manrope.ttf" else "dm_sans.ttf", weight)

/** Fuentes variables de la marca (recursos multiplataforma: Android e iOS). */
val Manrope: FontFamily
    @Composable get() = FontFamily(
        variable(Res.font.manrope, 600),
        variable(Res.font.manrope, 700),
        variable(Res.font.manrope, 800),
    )
val DmSans: FontFamily
    @Composable get() = FontFamily(
        variable(Res.font.dm_sans, 400),
        variable(Res.font.dm_sans, 500),
        variable(Res.font.dm_sans, 600),
        variable(Res.font.dm_sans, 700),
    )

@Composable
private fun vinkoTypography(): Typography {
    val manrope = Manrope
    val dmSans = DmSans
    return Typography(
    displaySmall = TextStyle(fontFamily = manrope, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = manrope, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 33.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = dmSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp),
)
}

private val VinkoColors = lightColorScheme(
    primary = Vinko.Primary,
    onPrimary = Color.White,
    primaryContainer = Vinko.Tint,
    onPrimaryContainer = Vinko.Primary,
    secondary = Vinko.Lavender,
    background = Vinko.Cream,
    onBackground = Vinko.Ink,
    surface = Color.White,
    onSurface = Vinko.Ink,
    surfaceVariant = Vinko.FieldBg,
    onSurfaceVariant = Vinko.Muted,
    outline = Vinko.Line,
    outlineVariant = Vinko.Line,
    error = Vinko.Coral,
    errorContainer = Vinko.CoralBg,
)

@Composable
fun SoyVinkoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VinkoColors, typography = vinkoTypography(), content = content)
}
