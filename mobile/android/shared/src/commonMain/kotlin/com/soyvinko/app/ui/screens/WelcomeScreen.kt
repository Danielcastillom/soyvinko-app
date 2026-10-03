package com.soyvinko.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.BrandLogo
import com.soyvinko.app.ui.theme.Vinko

@Composable
fun WelcomeScreen(onLogin: () -> Unit, onRegister: () -> Unit, expired: Boolean) {
    Box(Modifier.fillMaxSize().background(HeroGradient)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                BrandLogo(light = true)
                Spacer(Modifier.height(48.dp))
                Text("TALENTO REAL · PERSONAS QUE CONECTAN", style = MaterialTheme.typography.labelSmall, color = Vinko.Lilac)
                Spacer(Modifier.height(14.dp))
                Text(
                    buildAnnotatedString {
                        append("Personas de confianza para ")
                        withStyle(SpanStyle(color = Vinko.Lilac)) { append("hacerlo realidad.") }
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Contrata profesionales verificados en Venezuela, con trato directo y pagos protegidos.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(28.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Point(Icons.Outlined.VerifiedUser, "Perfiles con identidad verificada")
                    Point(Icons.Outlined.Shield, "Pagos en custodia hasta tu conformidad")
                    Point(Icons.Outlined.Handshake, "Presupuesto claro antes de empezar")
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (expired) Banner("Tu sesión terminó. Inicia sesión de nuevo para continuar.", BannerTone.Info)
                Button(
                    onClick = onRegister,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Vinko.Primary),
                ) { Text("Crear cuenta gratis", style = MaterialTheme.typography.labelLarge) }
                OutlinedButton(
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) { Text("Ya tengo cuenta", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun Point(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}
