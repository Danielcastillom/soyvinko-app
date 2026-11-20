package com.soyvinko.app.update

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.soyvinko.app.BuildConfig
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.BrandLogo
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.screens.HeroGradient
import com.soyvinko.app.ui.theme.Vinko
import java.util.Locale

private fun megabytes(bytes: Long) = String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)

/**
 * Pantalla de actualización obligatoria: ocupa toda la app hasta que la versión nueva quede instalada.
 * La descarga empieza sola; el usuario solo interviene si Android pide permiso o confirmación.
 */
@Composable
fun UpdateScreen(update: UpdateCheck.Available, phase: UpdatePhase?, onRetry: () -> Unit, onDismiss: (() -> Unit)? = null) {
    val context = LocalContext.current
    val release = update.release
    Column(Modifier.fillMaxSize().background(Vinko.Cream).verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().background(HeroGradient).statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 56.dp),
        ) {
            BrandLogo(light = true)
            Spacer(Modifier.height(36.dp))
            Box(Modifier.size(64.dp).background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                if (update.mandatory) "Actualización obligatoria" else "Nueva versión disponible",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "SoyVinko ${release.versionName} · tienes la ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyLarge,
                color = Vinko.Lilac,
            )
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (phase) {
                    is UpdatePhase.Downloading -> {
                        Text("Descargando actualización…", style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(
                            progress = { phase.progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Vinko.Primary,
                            trackColor = Vinko.Tint,
                        )
                        Text(
                            "${(phase.progress * 100).toInt()} % de ${megabytes(release.size)}. Puedes dejar la app abierta; se instalará sola al terminar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Vinko.Muted,
                        )
                    }
                    UpdatePhase.NeedsPermission -> {
                        Text("Permite que SoyVinko se actualice", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Android necesita tu autorización una sola vez. Activa \"Permitir de esta fuente\" y vuelve: la instalación continuará sola.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Vinko.Muted,
                        )
                        PrimaryButton("Dar permiso", onClick = { context.startActivity(AppInstaller.permissionSettings(context)) })
                    }
                    UpdatePhase.Installing -> {
                        Text("Instalando actualización…", style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(Modifier.fillMaxWidth().height(8.dp), color = Vinko.Primary, trackColor = Vinko.Tint)
                        Text(
                            "Instalando versión ${release.versionName}. Si Android te lo pide, confirma \"Actualizar\". La aplicación se reiniciará automáticamente al terminar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Vinko.Muted,
                        )
                    }
                    is UpdatePhase.Failed -> {
                        Banner(phase.message, BannerTone.Error)
                        PrimaryButton("Reintentar", onClick = onRetry)
                    }
                    null -> PrimaryButton("Actualizar ahora", onClick = onRetry)
                }
            }
            if (release.notes.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Novedades", style = MaterialTheme.typography.titleMedium)
                    release.notes.forEach { note ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (update.mandatory) {
                Banner("Esta versión incluye cambios necesarios para usar SoyVinko con seguridad. La app se habilitará al instalarla.", BannerTone.Info)
            } else if (onDismiss != null) {
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Ahora no", color = Vinko.Muted)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Aviso no bloqueante para actualizaciones opcionales. */
@Composable
fun OptionalUpdateDialog(update: UpdateCheck.Available, onUpdate: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = Vinko.Primary) },
        title = { Text("SoyVinko ${update.release.versionName} disponible") },
        text = { Text(update.release.notes.firstOrNull() ?: "Hay una versión nueva con mejoras.") },
        confirmButton = { TextButton(onClick = onUpdate) { Text("Actualizar", color = Vinko.Primary) } },
        dismissButton = { TextButton(onClick = onLater) { Text("Más tarde") } },
    )
}
