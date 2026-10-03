package com.soyvinko.app.ui.feature.contracts

import com.soyvinko.app.ui.common.epochMillis
import com.soyvinko.app.ui.common.shortDateTime
import com.soyvinko.app.ui.common.hour12
import com.soyvinko.app.ui.common.todayCaracas
import com.soyvinko.app.ui.common.Caracas
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant
import com.soyvinko.app.ui.common.nowMillis
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShareLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.Resource
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.currentPosition
import com.soyvinko.app.ui.common.hasLocationPermission
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.rememberLocationPermission
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.tracking.LocationTracking
import com.soyvinko.app.tracking.TrackingMode
import com.soyvinko.app.tracking.TrackingSession
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject

/** api.location_status: estado del permiso de ubicación (sin coordenadas ni auditoría: se puede consultar seguido). */
data class LocStatus(
    val active: Boolean = false,
    val sharedAt: String? = null,
    val expiresAt: String? = null,
    val revokedAt: String? = null,
    val source: String? = null,
    val locationUpdatedAt: String? = null,
    val professionalPositionAt: String? = null,
    val remote: Boolean = false,
) {
    val gps: Boolean get() = source == "gps"
    val everShared: Boolean get() = sharedAt != null
    val expired: Boolean get() = !active && revokedAt == null && (epochMs(expiresAt) ?: Long.MAX_VALUE) <= nowMillis()
}

fun JsonObject?.toLocStatus() = LocStatus(
    active = b("active"),
    sharedAt = s("shared_at"),
    expiresAt = s("expires_at"),
    revokedAt = s("revoked_at"),
    source = s("source"),
    locationUpdatedAt = s("location_updated_at"),
    professionalPositionAt = s("professional_position_at"),
    remote = b("remote"),
)

/** Consulta location_status al entrar y cada `intervalMs` mientras la pantalla está visible. */
@Composable
fun rememberLocationStatus(id: String, intervalMs: Long, enabled: Boolean = true): Resource<LocStatus> {
    val app = LocalApp.current
    val res = rememberResource(id, enabled) {
        if (enabled) app.ops.call("location_status", "contract" to id).obj().toLocStatus() else LocStatus()
    }
    PollWhileVisible(intervalMs, id, enabled) { if (enabled) res.reload() }
    return res
}

/**
 * Acceso al GPS de esta sección. Por defecto usa los ayudantes comunes; las pruebas lo sustituyen
 * (Robolectric no tiene servicios de Google Play).
 */
object Gps {
    var hasPermission: (PlatformContext) -> Boolean = ::hasLocationPermission
    var current: suspend (PlatformContext) -> GeoPos? = { withTimeoutOrNull(25_000) { currentPosition(it) } }
    fun reset() {
        hasPermission = ::hasLocationPermission
        current = { withTimeoutOrNull(25_000) { currentPosition(it) } }
    }
}

/** Igual que rememberLocationPermission, pero respetando [Gps.hasPermission]. */
@Composable
fun rememberGpsPermission(onDenied: () -> Unit, onGranted: () -> Unit): () -> Unit {
    val context = LocalPlatformContext.current
    val ask = rememberLocationPermission(onDenied, onGranted)
    return { if (Gps.hasPermission(context)) onGranted() else ask() }
}

/** Hora actual que se actualiza cada segundo mientras la pantalla está visible (cuentas regresivas, "hace X"). */
@Composable
fun rememberNow(tickMs: Long = 1_000): Long {
    var now by remember { mutableLongStateOf(nowMillis()) }
    PollWhileVisible(tickMs) { now = nowMillis() }
    return now
}

fun epochMs(iso: String?): Long? = epochMillis(iso)

/** "hace 12 s", "hace 3 min", "hace 2 h". */
fun agoText(epoch: Long?, now: Long): String? {
    epoch ?: return null
    val s = ((now - epoch) / 1000).coerceAtLeast(0)
    return when {
        s < 5 -> "ahora"
        s < 60 -> "hace $s s"
        s < 3600 -> "hace ${s / 60} min"
        s < 86_400 -> "hace ${s / 3600} h"
        else -> "hace ${s / 86_400} d"
    }
}

/** "en 7 h 42 min", "en 4 min 05 s", null si ya venció. */
fun countdownText(epoch: Long?, now: Long): String? {
    epoch ?: return null
    val s = (epoch - now) / 1000
    if (s <= 0) return null
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return when {
        h > 0 -> "en $h h ${m.toString().padStart(2, '0')} min"
        m > 0 -> "en $m min ${sec.toString().padStart(2, '0')} s"
        else -> "en $sec s"
    }
}

/** "3:45 p. m." (hoy) o "4 oct, 3:45 p. m.". */
fun clockOf(epoch: Long?): String {
    epoch ?: return ""
    val t = Instant.fromEpochMilliseconds(epoch).toLocalDateTime(Caracas)
    return if (t.date == todayCaracas()) hour12(t) else shortDateTime(t)
}

fun sourceLabel(source: String?) = if (source == "gps") "GPS en vivo del cliente" else "Dirección del servicio"

/** Sesión de seguimiento en segundo plano si corresponde a este contrato (y modo). */
@Composable
fun rememberTrackingFor(id: String, mode: TrackingMode? = null): TrackingSession? {
    val session by LocationTracking.session.collectAsState()
    return session?.takeIf { it.contractId == id && (mode == null || it.mode == mode) }
}

/** Aviso compacto: el teléfono está compartiendo la ubicación de este servicio en segundo plano. */
@Composable
fun TrackingActiveBanner(id: String, modifier: Modifier = Modifier) {
    val session = rememberTrackingFor(id) ?: return
    val context = LocalPlatformContext.current
    val now = rememberNow()
    Row(
        modifier.fillMaxWidth().background(Vinko.MintBg, RoundedCornerShape(14.dp)).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(androidx.compose.ui.graphics.Color.White, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ShareLocation, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                if (session.mode == TrackingMode.PROFESSIONAL) "Compartiendo tu trayecto en segundo plano" else "Compartiendo tu ubicación en vivo",
                style = MaterialTheme.typography.titleSmall, color = Vinko.Mint,
            )
            Text(
                session.error ?: (agoText(session.lastSentAt, now)?.let { "Seguimiento activo · último envío $it" } ?: "Seguimiento activo · esperando señal GPS…"),
                style = MaterialTheme.typography.bodySmall,
                color = if (session.error != null) Vinko.Coral else Vinko.Muted,
            )
        }
        TextButton(onClick = { LocationTracking.stop(context) }) {
            Text("Detener", color = Vinko.Coral, style = MaterialTheme.typography.labelLarge)
        }
    }
}
