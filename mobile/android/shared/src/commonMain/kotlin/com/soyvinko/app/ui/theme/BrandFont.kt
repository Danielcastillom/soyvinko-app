package com.soyvinko.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import org.jetbrains.compose.resources.FontResource

/** Fuente variable de la marca con el peso aplicado al eje "wght". `file` es el nombre en composeResources/font. */
@Composable
internal expect fun brandFont(res: FontResource, file: String, weight: Int): Font
