package com.soyvinko.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.PlatformContext

data class GeoPos(val lat: Double, val lon: Double, val accuracy: Float? = null)

data class MapMarker(val pos: GeoPos, val title: String, val color: Int = 0xFF6F3DFF.toInt())

/** ¿Hay permiso de ubicación (precisa o aproximada)? */
expect fun hasLocationPermission(context: PlatformContext): Boolean

/** Pide permiso de ubicación (si falta) y luego ejecuta `onGranted`. */
@Composable
expect fun rememberLocationPermission(onDenied: () -> Unit = {}, onGranted: () -> Unit): () -> Unit

/** Posición actual precisa (requiere permiso). Null si el GPS no responde. */
expect suspend fun currentPosition(context: PlatformContext): GeoPos?

/**
 * Posición en vivo mientras la pantalla está visible (p. ej. el profesional en camino).
 * Se detiene sola al salir de la pantalla.
 */
@Composable
expect fun LiveLocation(enabled: Boolean, intervalMs: Long = 15_000, onUpdate: (GeoPos) -> Unit)

/**
 * Mapa (OpenStreetMap en Android, Apple Maps en iOS). Muestra marcadores y opcionalmente una
 * línea entre ellos; `onTap` devuelve el punto tocado (para elegir una ubicación).
 */
@Composable
expect fun OsmMap(
    markers: List<MapMarker>,
    modifier: Modifier = Modifier,
    center: GeoPos? = null,
    zoom: Double = 15.0,
    drawLine: Boolean = false,
    onTap: ((GeoPos) -> Unit)? = null,
)

/** Abre la navegación paso a paso en la app de mapas del teléfono (Google Maps / Apple Maps, Waze…). */
expect fun openTurnByTurn(context: PlatformContext, to: GeoPos, label: String): Boolean
