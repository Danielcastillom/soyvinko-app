package com.soyvinko.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.FontResource

@Composable
internal actual fun brandFont(res: FontResource, file: String, weight: Int): Font =
    org.jetbrains.compose.resources.Font(res, FontWeight(weight))
