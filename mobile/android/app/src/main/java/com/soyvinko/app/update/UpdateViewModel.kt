package com.soyvinko.app.update

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soyvinko.app.BuildConfig
import com.soyvinko.app.data.ApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** Fase de una actualización disponible. */
sealed interface UpdatePhase {
    data class Downloading(val progress: Float) : UpdatePhase
    data object NeedsPermission : UpdatePhase
    data object Installing : UpdatePhase
    data class Failed(val message: String) : UpdatePhase
}

/**
 * Busca versión nueva al abrir la app y al volver a ella (cada 30 min como máximo).
 * Si es obligatoria, la descarga sola y lanza la instalación sin esperar al usuario.
 */
class UpdateViewModel(
    private val app: Application,
    private val repo: UpdateRepository = UpdateRepository(
        BuildConfig.UPDATE_MANIFEST_URL,
        BuildConfig.VERSION_CODE,
        File(app.cacheDir, "updates"),
    ),
) : ViewModel() {
    var checking by mutableStateOf(true)
        private set
    var available by mutableStateOf<UpdateCheck.Available?>(null)
        private set
    var phase by mutableStateOf<UpdatePhase?>(null)
        private set
    /** Las opcionales se pueden posponer hasta el próximo inicio; las obligatorias no. */
    var dismissed by mutableStateOf(false)
        private set

    private var lastCheck = 0L
    private var apk: File? = null
    private var job: Job? = null

    init {
        viewModelScope.launch {
            AppInstaller.status.collect { s ->
                if (available == null) return@collect
                phase = when (s) {
                    is InstallStatus.Failed -> UpdatePhase.Failed(s.message)
                    InstallStatus.Installing, InstallStatus.WaitingForUser -> UpdatePhase.Installing
                    InstallStatus.Idle -> phase
                }
            }
        }
        check(initial = true)
    }

    private fun check(initial: Boolean = false) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            lastCheck = System.currentTimeMillis()
            // Al abrir: como mucho 5 s de espera; sin red la app sigue y se reintenta al volver.
            val result = try {
                if (initial) withTimeoutOrNull(5_000) { repo.check() } else repo.check()
            } catch (e: ApiException) {
                null
            }
            checking = false
            if (result is UpdateCheck.Available) {
                val isNew = available?.release?.versionCode != result.release.versionCode
                available = result
                if (isNew) dismissed = false
                if (result.mandatory && (isNew || phase == null)) start()
            }
        }
    }

    /** Descarga e instala. Automático en obligatorias; manual (botón) en opcionales. */
    fun start() {
        val release = available?.release ?: return
        AppInstaller.reset()
        viewModelScope.launch {
            try {
                phase = UpdatePhase.Downloading(0f)
                val file = repo.download(release) { p -> phase = UpdatePhase.Downloading(p) }
                apk = file
                install()
            } catch (e: ApiException) {
                phase = UpdatePhase.Failed(e.message)
            }
        }
    }

    private fun install() {
        val file = apk ?: return start()
        if (!AppInstaller.canInstall(app)) {
            phase = UpdatePhase.NeedsPermission
            return
        }
        phase = UpdatePhase.Installing
        viewModelScope.launch { AppInstaller.install(app, file) }
    }

    /** Al volver a la app: reintenta tras conceder el permiso, o vuelve a buscar versiones. */
    fun onResume() {
        when {
            phase == UpdatePhase.NeedsPermission && AppInstaller.canInstall(app) -> install()
            available == null && System.currentTimeMillis() - lastCheck > 30 * 60_000 -> check()
        }
    }

    fun retry() {
        if (apk?.exists() == true) install() else start()
    }

    fun dismiss() {
        if (available?.mandatory == false) dismissed = true
    }
}
