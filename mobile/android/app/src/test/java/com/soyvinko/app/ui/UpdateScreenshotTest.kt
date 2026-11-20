package com.soyvinko.app.ui

import androidx.test.core.app.ApplicationProvider
import com.soyvinko.app.platform.AndroidPlatform
import org.junit.Before
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.soyvinko.app.ui.theme.SoyVinkoTheme
import com.soyvinko.app.update.ReleaseManifest
import com.soyvinko.app.update.UpdateCheck
import com.soyvinko.app.update.UpdatePhase
import com.soyvinko.app.update.UpdateScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = android.app.Application::class)
class UpdateScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val update = UpdateCheck.Available(
        ReleaseManifest(
            versionCode = 3, versionName = "1.2.0", url = "https://soyvinko.com/x.apk", sha256 = "", size = 21_500_000,
            notes = listOf("Wallet completa: recargas, retiros y compra de Vinkos", "Chat y ubicación en vivo en tus servicios", "Correcciones de seguridad"),
        ),
        mandatory = true,
    )

    @Before
    fun setUp() {
        // Igual que SoyVinkoApp al arrancar: los recursos compartidos (logo) necesitan el contexto.
        AndroidPlatform.init(ApplicationProvider.getApplicationContext(), "test")
    }

    private fun shot(name: String, phase: UpdatePhase?) {
        compose.setContent { SoyVinkoTheme { UpdateScreen(update, phase, onRetry = {}) } }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun downloading() = shot("08_actualizacion_descargando", UpdatePhase.Downloading(0.42f))
    @Test fun permission() = shot("09_actualizacion_permiso", UpdatePhase.NeedsPermission)
}
