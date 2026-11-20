package com.soyvinko.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.FontResource

/**
 * En Android los recursos multiplataforma van en assets; se cargan desde ahí con el eje de peso
 * fijado (la fuente de recursos de Compose 1.7 no admite variationSettings).
 */
@OptIn(ExperimentalTextApi::class)
@Composable
internal actual fun brandFont(res: FontResource, file: String, weight: Int): Font {
    val assets = LocalContext.current.assets
    return remember(file, weight) {
        Font(
            "composeResources/com.soyvinko.shared.resources/font/$file",
            assets,
            FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    }
}
