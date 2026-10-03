package com.soyvinko.app.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.IntentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

sealed interface InstallStatus {
    data object Idle : InstallStatus
    data object WaitingForUser : InstallStatus
    data object Installing : InstallStatus
    data class Failed(val message: String) : InstallStatus
}

/**
 * Instala el APK descargado con PackageInstaller.
 * En Android 12+ pide instalar sin confirmación (USER_ACTION_NOT_REQUIRED): el sistema lo permite
 * cuando SoyVinko ya es quien instaló la versión actual (a partir de la primera autoactualización).
 * Si no, Android muestra su ventana de confirmación con un solo botón "Actualizar".
 */
object AppInstaller {
    private val _status = MutableStateFlow<InstallStatus>(InstallStatus.Idle)
    val status: StateFlow<InstallStatus> = _status.asStateFlow()

    /** Android exige que el usuario permita "Instalar apps desconocidas" para SoyVinko una sola vez. */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun permissionSettings(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    suspend fun install(context: Context, apk: File) = withContext(Dispatchers.IO) {
        _status.value = InstallStatus.Installing
        try {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                setSize(apk.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite("soyvinko.apk", 0, apk.length()).use { out ->
                    apk.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
                val callback = Intent(context, InstallResultReceiver::class.java).setAction(ACTION_RESULT)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                session.commit(PendingIntent.getBroadcast(context, sessionId, callback, flags).intentSender)
            }
        } catch (e: Exception) {
            _status.value = InstallStatus.Failed("No se pudo iniciar la instalación: ${e.localizedMessage ?: "error desconocido"}.")
        }
    }

    internal fun onResult(context: Context, intent: Intent) {
        when (val code = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                _status.value = InstallStatus.WaitingForUser
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)?.let {
                    context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            // Al terminar con éxito, Android reemplaza la app y cierra este proceso.
            PackageInstaller.STATUS_SUCCESS -> _status.value = InstallStatus.Idle
            PackageInstaller.STATUS_FAILURE_ABORTED -> _status.value =
                InstallStatus.Failed("Cancelaste la instalación. Esta actualización es obligatoria para seguir usando SoyVinko.")
            PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> _status.value =
                InstallStatus.Failed("Tu versión instalada no es compatible con esta actualización. Desinstala SoyVinko e instala la versión nueva desde soyvinko.com.")
            PackageInstaller.STATUS_FAILURE_STORAGE -> _status.value =
                InstallStatus.Failed("No hay espacio suficiente en el teléfono para actualizar.")
            else -> _status.value = InstallStatus.Failed(
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)?.let { "La instalación falló ($code): $it" }
                    ?: "La instalación falló. Inténtalo de nuevo.",
            )
        }
    }

    fun reset() {
        _status.value = InstallStatus.Idle
    }

    const val ACTION_RESULT = "com.soyvinko.app.INSTALL_RESULT"
}

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AppInstaller.ACTION_RESULT) AppInstaller.onResult(context, intent)
    }
}
