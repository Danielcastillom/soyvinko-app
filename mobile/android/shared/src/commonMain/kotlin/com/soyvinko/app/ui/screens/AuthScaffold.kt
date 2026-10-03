package com.soyvinko.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.components.BrandLogo
import com.soyvinko.app.ui.theme.Vinko

val HeroGradient = Brush.linearGradient(listOf(Vinko.Dark, Color(0xFF3B1D8F), Vinko.Primary))

/** Encabezado morado de marca y una hoja blanca desplazable con el formulario. */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Fondo crema en toda la pantalla y encabezado morado: la hoja del formulario siempre llega al borde inferior.
    Column(
        Modifier
            .fillMaxSize()
            .background(Vinko.Cream)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(HeroGradient)
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 56.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                }
                BrandLogo(light = true)
            }
            Spacer(Modifier.height(28.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Vinko.Lilac)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = (-28).dp)
                .background(Vinko.Cream, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}
