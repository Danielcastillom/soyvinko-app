package com.soyvinko.app

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.soyvinko.app.ui.AppViewModels
import com.soyvinko.app.ui.SessionState
import com.soyvinko.app.ui.SessionViewModel
import com.soyvinko.app.ui.root.SoyVinkoRoot
import com.soyvinko.app.ui.theme.SoyVinkoTheme
import com.soyvinko.app.update.OptionalUpdateDialog
import com.soyvinko.app.update.UpdateScreen
import com.soyvinko.app.update.UpdateViewModel

/** Actualizaciones obligatorias del APK: solo Android (en iOS las publica el App Store / TestFlight). */
private val UpdaterFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { UpdateViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!) }
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAppUpdated()
        setContent {
            SoyVinkoTheme {
                val session: SessionViewModel = viewModel(factory = AppViewModels)
                val updater: UpdateViewModel = viewModel(factory = UpdaterFactory)
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { updater.onResume() }
                // La marca se mantiene mientras se recupera la sesión y se consulta si hay versión nueva.
                splash.setKeepOnScreenCondition { session.state.value is SessionState.Loading || updater.checking }
                val update = updater.available
                // Obligatoria (o una opcional ya iniciada): ocupa toda la app hasta instalarse.
                if (update != null && (update.mandatory || updater.phase != null) && !updater.dismissed) {
                    UpdateScreen(
                        update = update,
                        phase = updater.phase,
                        onRetry = updater::retry,
                        onDismiss = if (update.mandatory) null else updater::dismiss,
                    )
                    return@SoyVinkoTheme
                }
                if (update != null && !updater.dismissed) {
                    OptionalUpdateDialog(update, onUpdate = updater::start, onLater = updater::dismiss)
                }
                // Misma raíz que iOS; mientras carga se ve la pantalla de arranque del sistema.
                SoyVinkoRoot(session = session, loading = {})
            }
        }
    }

    private fun checkAppUpdated() {
        val prefs = getSharedPreferences("soyvinko_app_meta", Context.MODE_PRIVATE)
        val lastCode = prefs.getInt("last_installed_version_code", 0)
        val currentCode = BuildConfig.VERSION_CODE
        val justUpdated = intent.getBooleanExtra("just_updated", false) || (lastCode in 1 until currentCode)

        prefs.edit().putInt("last_installed_version_code", currentCode).apply()

        if (justUpdated) {
            Toast.makeText(
                this,
                "✨ ¡SoyVinko se actualizó exitosamente a la versión ${BuildConfig.VERSION_NAME}!",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
