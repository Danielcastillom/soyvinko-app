package com.soyvinko.app.tracking

import androidx.compose.runtime.Composable
import coil3.PlatformContext
import com.soyvinko.app.AppGraph
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.hasLocationPermission
import com.soyvinko.app.ui.common.nowInstant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonNull

/** Quién comparte: el profesional su trayecto (report_position) o el cliente su GPS (update_shared_location). */
enum class TrackingMode { PROFESSIONAL, CUSTOMER }

data class TrackingSession(
    val contractId: String,
    val mode: TrackingMode,
    val label: String,
    val lastSentAt: Long? = null,
    val error: String? = null,
)

/**
 * Comparte la ubicación de un servicio también con la app en segundo plano o la pantalla apagada.
 * En Android es un servicio en primer plano con notificación "Detener"; en iOS, actualizaciones de
 * ubicación en segundo plano (indicador azul en la barra de estado). Se detiene solo cuando el
 * servidor deja de aceptar posiciones (servicio terminado, permiso del cliente vencido) o al cerrar sesión.
 *
 * Uso desde la UI: `LocationTracking.start(context, contractId, TrackingMode.PROFESSIONAL, "Ana Pérez")`,
 * `LocationTracking.stop(context)`, y `LocationTracking.session` para mostrar el estado.
 */
object LocationTracking {
    private val _session = MutableStateFlow<TrackingSession?>(null)
    val session: StateFlow<TrackingSession?> = _session.asStateFlow()

    fun update(transform: (TrackingSession?) -> TrackingSession?) {
        _session.value = transform(_session.value)
    }

    fun isTracking(contractId: String, mode: TrackingMode? = null) =
        _session.value?.let { it.contractId == contractId && (mode == null || it.mode == mode) } == true

    /** Requiere permiso de ubicación concedido (con la app en primer plano). */
    fun start(context: PlatformContext, contractId: String, mode: TrackingMode, label: String) {
        if (!hasLocationPermission(context)) return
        startPlatformTracking(context, contractId, mode, label)
    }

    fun stop(context: PlatformContext) = stopPlatformTracking(context)
}

internal expect fun startPlatformTracking(context: PlatformContext, contractId: String, mode: TrackingMode, label: String)

internal expect fun stopPlatformTracking(context: PlatformContext)

/**
 * Lanza el seguimiento pidiendo antes los permisos necesarios (ubicación y, en Android 13+,
 * notificaciones; sin ese permiso el seguimiento funciona igual, pero la notificación no se ve).
 */
@Composable
expect fun rememberTrackingStarter(onDenied: () -> Unit = {}): (contractId: String, mode: TrackingMode, label: String) -> Unit

internal fun nowMs(): Long = nowInstant().toEpochMilliseconds()

/**
 * Envía una posición del seguimiento. Devuelve el motivo para detenerlo si el servidor ya no acepta
 * posiciones; null si hay que seguir (también sin red: se reintenta con la próxima posición).
 */
internal suspend fun reportTrackingFix(contract: String, mode: TrackingMode, pos: GeoPos): String? = try {
    val ops = AppGraph.services.ops
    val accuracy: Any = pos.accuracy?.toInt() ?: JsonNull
    when (mode) {
        TrackingMode.PROFESSIONAL -> ops.call("report_position", "contract" to contract, "lon" to pos.lon, "lat" to pos.lat, "accuracy" to accuracy)
        TrackingMode.CUSTOMER -> ops.call("update_shared_location", "contract" to contract, "lon" to pos.lon, "lat" to pos.lat, "accuracy" to accuracy)
    }
    LocationTracking.update { it?.copy(lastSentAt = nowMs(), error = null) }
    null
} catch (e: ApiException) {
    // El servicio terminó o el cliente dejó de compartir: no tiene sentido seguir.
    if (e.code in setOf("position_not_allowed", "location_not_shared", "session_expired") || e.isUnauthorized || e.status == 403) {
        e.message ?: "Seguimiento detenido"
    } else {
        LocationTracking.update { it?.copy(error = e.message) }
        null
    }
}
