package com.soyvinko.app.ui.feature.transport

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.LocalPlatformContext
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.IconTile
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalOpenDrawer
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.MapMarker
import com.soyvinko.app.ui.common.OsmMap
import com.soyvinko.app.ui.common.PollWhileVisible
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.d
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.openTurnByTurn
import com.soyvinko.app.ui.common.relative
import com.soyvinko.app.ui.common.rememberAction
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.shareViaWhatsApp
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.feature.profile.Avatar
import com.soyvinko.app.ui.screens.HeroGradient
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Majesticons
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/** Datos del vehículo del rider/conductor. */
data class VehicleConfig(
    val type: String = "moto", // "moto", "carro", "pickup", "camion"
    val plate: String = "",
    val model: String = "",
    val color: String = "",
)

/**
 * Pantalla para Conductores y Riders (Modo Vendedor):
 * 1. Resumen de Ganancias y Métricas del Día.
 * 2. Configuración de vehículo con validación de placa.
 * 3. Solicitudes disponibles con filtro por categorías, estimación de tiempo (ETA) y desglose de ganancias netas.
 * 4. Navegación GPS turno a turno hacia el origen o destino.
 */
@Composable
fun RiderScreen(nav: NavHostController) {
    val app = LocalApp.current
    val user = LocalUser.current
    val openDrawer = LocalOpenDrawer.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val action = rememberAction()

    var vehicleType by rememberSaveable { mutableStateOf("moto") }
    var vehiclePlate by rememberSaveable { mutableStateOf("AB123CD") }
    var vehicleModel by rememberSaveable { mutableStateOf("Moto 150cc") }
    var vehicleColor by rememberSaveable { mutableStateOf("Negro") }
    var isOnline by rememberSaveable { mutableStateOf(true) }
    var showVehicleDialog by remember { mutableStateOf(false) }

    val vehicle = remember(vehicleType, vehiclePlate, vehicleModel, vehicleColor) {
        VehicleConfig(vehicleType, vehiclePlate, vehicleModel, vehicleColor)
    }

    var currentTab by rememberSaveable { mutableStateOf("available") } // "available", "active", "done"
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("all") } // "all", "ride", "package", "freight"
    var activeViewMode by rememberSaveable { mutableStateOf("map") } // "map" or "list"
    var selectedActiveTripIndex by rememberSaveable { mutableStateOf(0) }

    // Solicitudes disponibles de clientes en la zona
    val availableResource = rememberResource(vehicleType, isOnline) {
        if (isOnline) {
            app.ops.call("available_shipments", "vehicle_type" to vehicleType)
        } else {
            kotlinx.serialization.json.JsonArray(emptyList())
        }
    }

    // Mis viajes tomados como conductor
    val myShipmentsResource = rememberResource {
        app.ops.call("my_shipments", "role" to "driver")
    }

    // Actualización automática cada 8 s mientras el rider está activo
    PollWhileVisible(8_000, isOnline, vehicleType) {
        if (isOnline) {
            availableResource.reload()
            myShipmentsResource.reload()
        }
    }

    ScreenScaffold(
        title = "Panel Rider & Envíos",
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Outlined.Menu, contentDescription = "Menú", tint = Vinko.Ink)
            }
        },
        actions = {
            IconButton(onClick = {
                availableResource.reload()
                myShipmentsResource.reload()
            }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar", tint = Vinko.Ink)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Tarjeta principal: Vehículo configurado y Switch de Disponibilidad
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Line),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Vinko.Tint, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = vehicleIcon(vehicle.type),
                                contentDescription = null,
                                tint = Vinko.Primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = vehicleLabel(vehicle.type),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Vinko.Ink,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "· ${vehicle.plate.ifBlank { "Sin placa" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Vinko.Muted,
                                )
                            }
                            Text(
                                text = "${vehicle.model.ifBlank { "Vehículo registrado" }} ${if (vehicle.color.isNotBlank()) "(${vehicle.color})" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Vinko.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { showVehicleDialog = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar vehículo", tint = Vinko.Primary)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    androidx.compose.material3.HorizontalDivider(color = Vinko.Line)
                    Spacer(Modifier.height(12.dp))

                    // Switch Online / Offline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (isOnline) Vinko.Mint else Color(0xFF9CA3AF), CircleShape),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isOnline) "Disponible para viajes" else "Desconectado",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOnline) Vinko.Ink else Color(0xFF6B7280),
                                )
                                Text(
                                    text = if (isOnline) "Recibes solicitudes de clientes en tu zona" else "No recibirás solicitudes de viaje",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Vinko.Muted,
                                )
                            }
                        }
                        Switch(
                            checked = isOnline,
                            onCheckedChange = { isOnline = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Vinko.Mint,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFD1D5DB),
                            ),
                        )
                    }
                }
            }

            val myShipmentsList = myShipmentsResource.data?.objects().orEmpty()
            val activeTrips = myShipmentsList.filter { it.s("status") in setOf("accepted", "picking_up", "in_transit") }
            val completedTrips = myShipmentsList.filter { it.s("status") == "delivered" }

            // 2. NUEVO PANEL DE RENDIMIENTO Y RESUMEN FINANCIERO DEL RIDER
            val completedEarningsMinor = completedTrips.sumOf { it.l("fare_minor") ?: 0L }
            val riderEarnings = calculateRiderEarnings(completedEarningsMinor, commissionPercent = 10.0)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Line),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Resumen de Hoy",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Vinko.Ink,
                            )
                            Text(
                                text = "Métricas y ganancias del conductor",
                                style = MaterialTheme.typography.bodySmall,
                                color = Vinko.Muted,
                            )
                        }
                        TextButton(onClick = { nav.navigate(Routes.WALLET) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Billetera", color = Vinko.Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(14.dp),
                            color = Vinko.MintBg,
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Ganancia Neta", fontSize = 11.sp, color = Vinko.Mint, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(money(riderEarnings.netFareMinor, "USD"), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Vinko.Ink)
                                Text("Comisión 10% deducida", fontSize = 9.sp, color = Vinko.Muted)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Vinko.Tint,
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Viajes", fontSize = 11.sp, color = Vinko.Primary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("${completedTrips.size}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Vinko.Ink)
                                Text("${activeTrips.size} en curso", fontSize = 9.sp, color = Vinko.Muted)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEF3C7),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Calificación", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("★ 4.9", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Vinko.Ink)
                                Text("Nivel Oro", fontSize = 9.sp, color = Vinko.Muted)
                            }
                        }
                    }
                }
            }

            // 3. Selector de pestañas: Solicitudes disponibles vs Mis viajes activos
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Line),
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    val tabs = listOf(
                        "available" to "Solicitudes",
                        "active" to "En curso (${activeTrips.size})",
                        "done" to "Historial (${completedTrips.size})",
                    )
                    tabs.forEach { (key, label) ->
                        val selected = currentTab == key
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) Vinko.Primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentTab = key },
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else Color(0xFF4B5563),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }

            // 4. Contenido según la pestaña seleccionada
            when (currentTab) {
                "available" -> {
                    // Pestaña Solicitudes Disponibles de Clientes
                    if (!isOnline) {
                        VCard {
                            Text(
                                "Estás desconectado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Activa el interruptor «Disponible para viajes» arriba para ver solicitudes de clientes en tu zona.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Vinko.Muted,
                            )
                        }
                    } else {
                        // Filtros por categoría
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val filterOptions = listOf(
                                "all" to "Todos",
                                "ride" to "Pasajeros",
                                "package" to "Paquetes",
                                "freight" to "Carga",
                            )
                            filterOptions.forEach { (catKey, catLabel) ->
                                val isSelected = selectedCategoryFilter == catKey
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Vinko.Primary else Color(0xFFF3F4F6),
                                    modifier = Modifier.clickable { selectedCategoryFilter = catKey },
                                ) {
                                    Text(
                                        text = catLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF4B5563),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }

                        ResourceView(availableResource) { raw ->
                            val allShipments = raw.objects()
                            val shipments = allShipments.filter { s ->
                                if (selectedCategoryFilter == "all") true else s.s("shipment_type") == selectedCategoryFilter
                            }

                            if (shipments.isEmpty()) {
                                EmptyBox(
                                    title = "No hay solicitudes pendientes",
                                    text = if (selectedCategoryFilter != "all") "No hay solicitudes en esta categoría. Prueba seleccionando 'Todos'." else "Cuando un cliente solicite un viaje o envío para ${vehicleLabel(vehicle.type).lowercase()}, aparecerá aquí al instante.",
                                    actionLabel = "Actualizar",
                                    onAction = { availableResource.reload() },
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "${shipments.size} solicitudes encontradas",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Vinko.Muted,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    shipments.forEach { s ->
                                        AvailableShipmentCard(
                                            shipment = s,
                                            onAccept = { id ->
                                                action.run("¡Viaje aceptado! Despacho iniciado con éxito.") {
                                                    app.ops.call("take_shipment", "shipment" to id)
                                                    availableResource.reload()
                                                    myShipmentsResource.reload()
                                                    currentTab = "active"
                                                }
                                            },
                                            busy = action.busy,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "active" -> {
                    // Pestaña Mis viajes activos y aprobados con vista de Mapa interactivo
                    if (activeTrips.isEmpty()) {
                        EmptyBox(
                            title = "No tienes viajes en curso",
                            text = "Acepta solicitudes disponibles para empezar a ganar con tu vehículo.",
                            actionLabel = "Ver solicitudes",
                            onAction = { currentTab = "available" },
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Selector de Modo: Mapa de Rutas vs Lista de Viajes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(8.dp).background(Vinko.Mint, CircleShape),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${activeTrips.size} viaje${if (activeTrips.size > 1) "s" else ""} en curso",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Vinko.Ink,
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF3F4F6),
                                ) {
                                    Row(modifier = Modifier.padding(2.dp)) {
                                        val isMap = activeViewMode == "map"
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isMap) Vinko.Primary else Color.Transparent,
                                            modifier = Modifier.clickable { activeViewMode = "map" },
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(Icons.Outlined.Map, contentDescription = null, tint = if (isMap) Color.White else Color(0xFF4B5563), modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Mapa de Ruta", fontSize = 12.sp, fontWeight = if (isMap) FontWeight.Bold else FontWeight.Medium, color = if (isMap) Color.White else Color(0xFF4B5563))
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (!isMap) Vinko.Primary else Color.Transparent,
                                            modifier = Modifier.clickable { activeViewMode = "list" },
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(Icons.Outlined.ViewList, contentDescription = null, tint = if (!isMap) Color.White else Color(0xFF4B5563), modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Lista", fontSize = 12.sp, fontWeight = if (!isMap) FontWeight.Bold else FontWeight.Medium, color = if (!isMap) Color.White else Color(0xFF4B5563))
                                            }
                                        }
                                    }
                                }
                            }

                            if (activeViewMode == "map") {
                                RiderActiveTripsMapView(
                                    activeTrips = activeTrips,
                                    selectedIndex = selectedActiveTripIndex.coerceIn(0, (activeTrips.size - 1).coerceAtLeast(0)),
                                    onSelectIndex = { selectedActiveTripIndex = it },
                                    onAdvance = { id, nextAction, msg ->
                                        action.run(msg) {
                                            app.ops.call("advance_shipment", "shipment" to id, "action" to nextAction)
                                            myShipmentsResource.reload()
                                        }
                                    },
                                    onViewTracking = { id ->
                                        nav.navigate(Routes.shipmentTracking(id))
                                    },
                                    busy = action.busy,
                                )
                            } else {
                                activeTrips.forEach { s ->
                                    ActiveTripCard(
                                        shipment = s,
                                        onAdvance = { id, nextAction, msg ->
                                            action.run(msg) {
                                                app.ops.call("advance_shipment", "shipment" to id, "action" to nextAction)
                                                myShipmentsResource.reload()
                                            }
                                        },
                                        onViewTracking = { id ->
                                            nav.navigate(Routes.shipmentTracking(id))
                                        },
                                        busy = action.busy,
                                    )
                                }
                            }
                        }
                    }
                }

                "done" -> {
                    // Pestaña Historial de viajes completados
                    if (completedTrips.isEmpty()) {
                        EmptyBox(
                            title = "Sin viajes completados",
                            text = "Tus viajes y entregas finalizadas quedarán registradas aquí.",
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            completedTrips.forEach { s ->
                                CompletedTripCard(
                                    shipment = s,
                                    onViewDetail = { id ->
                                        nav.navigate(Routes.shipmentTracking(id))
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // 5. Banner auxiliar para pedir envío como cliente
            VCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTile(Icons.Outlined.Navigation, tint = Vinko.Primary, bg = Vinko.Tint)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("¿Necesitas enviar algo tú mismo?", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Puedes solicitar viajes o envíos de paquetes como cliente en cualquier momento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Vinko.Muted,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { nav.navigate(Routes.TRANSPORT_NEW) }) {
                        Text("Pedir", color = Vinko.Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal para configurar el vehículo con validación
    if (showVehicleDialog) {
        VehicleSetupDialog(
            current = vehicle,
            onDismiss = { showVehicleDialog = false },
            onSave = { updated ->
                vehicleType = updated.type
                vehiclePlate = updated.plate
                vehicleModel = updated.model
                vehicleColor = updated.color
                showVehicleDialog = false
                availableResource.reload()
            },
        )
    }
}

/** Tarjeta de Solicitud de Viaje disponible para que el Rider la acepte. */
@Composable
private fun AvailableShipmentCard(
    shipment: JsonObject,
    onAccept: (String) -> Unit,
    busy: Boolean,
) {
    val id = shipment.s("id").orEmpty()
    val type = shipment.s("shipment_type")
    val vehicleType = shipment.s("vehicle_type").orEmpty()
    val title = shipment.s("title").orEmpty()
    val origin = shipment.s("origin_address").orEmpty()
    val destination = shipment.s("destination_address").orEmpty()
    val distance = shipment.l("distance_km") ?: 5L
    val fare = shipment.l("fare_minor") ?: 0L
    val currency = shipment.s("currency") ?: "USD"
    val senderName = shipment.s("sender_name") ?: "Cliente"
    val paymentMethod = shipment.s("payment_method") ?: "wallet"

    val riderEarnings = calculateRiderEarnings(fare, currency = currency)
    val etaMinutes = estimateTripDurationMinutes(distance.toDouble(), vehicleType)
    var showRoutePreview by remember { mutableStateOf(false) }

    VCard(padding = 14.dp) {
        // Cabecera de la solicitud
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Vinko.Tint, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        vehicleIcon(vehicleType),
                        contentDescription = null,
                        tint = Vinko.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        shipmentTypeLabel(type),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Vinko.Primary,
                    )
                    Text(
                        title.ifBlank { "Envío solicitado" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = money(fare, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Vinko.Mint,
                )
                Text(
                    text = "Ganas: ${money(riderEarnings.netFareMinor, currency)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Vinko.Ink,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Badges informativos: Método de pago y Tiempo estimado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (payText, payColor, payBg) = when (paymentMethod) {
                "wallet" -> Triple("Billetera Protegida 🔒", Vinko.Mint, Vinko.MintBg)
                "cash" -> Triple("Efectivo al recibir 💵", Color(0xFFD97706), Color(0xFFFEF3C7))
                else -> Triple("Pago Móvil 📱", Color(0xFF2563EB), Color(0xFFEFF6FF))
            }
            Surface(
                color = payBg,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = payText,
                    fontSize = 11.sp,
                    color = payColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }

            Surface(
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = "⏱️ ~$etaMinutes min · $distance km",
                    fontSize = 11.sp,
                    color = Color(0xFF4B5563),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Ruta: Origen y Destino
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.padding(top = 2.dp).size(12.dp).background(Vinko.Primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.size(5.dp).background(Color.White, CircleShape))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Desde: $origin",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.padding(top = 2.dp).size(12.dp).background(Vinko.Coral, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.size(5.dp).background(Color.White, CircleShape))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Hasta: $destination",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Previsualización de ruta en mapa interactivo
        if (showRoutePreview) {
            val origLat = shipment.d("origin_lat") ?: 10.4806
            val origLon = shipment.d("origin_lon") ?: -66.9036
            val dstLat = shipment.d("destination_lat") ?: 10.5000
            val dstLon = shipment.d("destination_lon") ?: -66.9167
            val oPos = GeoPos(origLat, origLon)
            val dPos = GeoPos(dstLat, dstLon)
            val cPos = GeoPos((origLat + dstLat) / 2.0, (origLon + dstLon) / 2.0)

            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Vinko.Line, RoundedCornerShape(14.dp)),
            ) {
                OsmMap(
                    markers = listOf(
                        MapMarker(pos = oPos, title = "Recoger: $origin", color = 0xFF0C8E72.toInt()),
                        MapMarker(pos = dPos, title = "Llevar a: $destination", color = 0xFFEF4444.toInt()),
                    ),
                    center = cPos,
                    zoom = 12.5,
                    drawLine = true,
                    routePoints = listOf(oPos, dPos),
                    modifier = Modifier.fillMaxSize(),
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Line),
                ) {
                    Text(
                        text = "Recoger ➔ Entregar · $distance km",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Vinko.Ink,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Información de cliente y botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cliente: $senderName",
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Muted,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { showRoutePreview = !showRoutePreview },
                    modifier = Modifier.height(38.dp),
                ) {
                    Icon(Icons.Outlined.Map, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (showRoutePreview) "Ocultar" else "Ver ruta",
                        fontSize = 12.sp,
                        color = Vinko.Primary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.width(6.dp))

                PrimaryButton(
                    text = "Aceptar",
                    loading = busy,
                    onClick = { onAccept(id) },
                    modifier = Modifier.height(38.dp),
                )
            }
        }
    }
}

/**
 * Vista de Mapa Interactivo para el Rider:
 * Visualiza en el mapa la ruta exacta de la solicitud aprobada / viaje en curso,
 * mostrando claramente desde dónde recogerá (punto de origen) y a dónde llevará (punto de destino),
 * con línea de trayectoria, botones de navegación GPS nativa (Google Maps / Waze) y contacto directo de WhatsApp.
 */
@Composable
private fun RiderActiveTripsMapView(
    activeTrips: List<JsonObject>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onAdvance: (String, String, String) -> Unit,
    onViewTracking: (String) -> Unit,
    busy: Boolean,
) {
    val ctx = LocalPlatformContext.current
    val selectedTrip = activeTrips.getOrNull(selectedIndex) ?: activeTrips.firstOrNull() ?: return
    val id = selectedTrip.s("id").orEmpty()
    val status = selectedTrip.s("status").orEmpty()
    val type = selectedTrip.s("shipment_type").orEmpty()
    val title = selectedTrip.s("title").orEmpty()
    val originAddress = selectedTrip.s("origin_address").orEmpty()
    val destAddress = selectedTrip.s("destination_address").orEmpty()
    val distance = selectedTrip.l("distance_km") ?: 5L
    val fare = selectedTrip.l("fare_minor") ?: 0L
    val currency = selectedTrip.s("currency") ?: "USD"
    val vehicleType = selectedTrip.s("vehicle_type") ?: "moto"
    val paymentMethod = selectedTrip.s("payment_method") ?: "wallet"

    val originContactName = selectedTrip.s("origin_contact_name") ?: selectedTrip.s("sender_name") ?: "Remitente"
    val originContactPhone = selectedTrip.s("origin_contact_phone")
    val destContactName = selectedTrip.s("destination_contact_name") ?: "Receptor"
    val destContactPhone = selectedTrip.s("destination_contact_phone")

    val originLat = selectedTrip.d("origin_lat") ?: 10.4806
    val originLon = selectedTrip.d("origin_lon") ?: -66.9036
    val destLat = selectedTrip.d("destination_lat") ?: 10.5000
    val destLon = selectedTrip.d("destination_lon") ?: -66.9167
    val originPos = GeoPos(originLat, originLon)
    val destPos = GeoPos(destLat, destLon)
    val centerPos = GeoPos((originLat + destLat) / 2.0, (originLon + destLon) / 2.0)

    val targetPos = if (status == "in_transit") destPos else originPos
    val targetLabel = if (status == "in_transit") "Destino ($destAddress)" else "Origen ($originAddress)"

    val riderEarnings = calculateRiderEarnings(fare, currency = currency)

    val (statusLabel, nextAction, actionLabel, actionSuccessMsg) = when (status) {
        "accepted" -> Quad("1. Aceptado · Dirígete al punto de recogida", "arrive_origin", "Llegué al origen", "Marcado como llegado al origen")
        "picking_up" -> Quad("2. En el origen · Carga a bordo / Pasajero", "picked_up", "Iniciar ruta al destino", "Carga recogida, viaje en curso")
        "in_transit" -> Quad("3. En camino · Traslado hacia el destino", "delivered", "Finalizar / Entregado", "¡Viaje entregado con éxito!")
        else -> Quad(status, "", "", "")
    }

    val markers = remember(originPos, destPos, originAddress, destAddress) {
        listOf(
            MapMarker(
                pos = originPos,
                title = "📍 Recoger: ${originAddress.take(28)}",
                color = 0xFF0C8E72.toInt(), // Vinko.Mint Verde
            ),
            MapMarker(
                pos = destPos,
                title = "🏁 Entregar: ${destAddress.take(28)}",
                color = 0xFFEF4444.toInt(), // Coral Rojo
            ),
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Selector horizontal si hay varios viajes aprobados
        if (activeTrips.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                activeTrips.forEachIndexed { index, trip ->
                    val isCurrent = index == selectedIndex
                    val tripFolio = trip.s("id")?.take(6)?.uppercase() ?: "$index"
                    val tripStatus = trip.s("status").orEmpty()
                    val badge = when (tripStatus) {
                        "accepted" -> "1. Recoger"
                        "picking_up" -> "2. En origen"
                        "in_transit" -> "3. En camino"
                        else -> tripStatus
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isCurrent) Vinko.Primary else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrent) Vinko.Primary else Vinko.Line),
                        modifier = Modifier.clickable { onSelectIndex(index) },
                    ) {
                        Text(
                            text = "#$tripFolio · $badge",
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) Color.White else Vinko.Ink,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }

        // 1. MAPA INTERACTIVO DE RUTA RIDER (DESDE DÓNDE RECOGERÁ HASTA DÓNDE LLEVARÁ)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Vinko.Line, RoundedCornerShape(20.dp)),
        ) {
            OsmMap(
                markers = markers,
                center = centerPos,
                zoom = 13.0,
                drawLine = true,
                routePoints = listOf(originPos, destPos),
                modifier = Modifier.fillMaxSize(),
            )

            // Pill flotante superior: Trayectoria Recogida -> Entrega
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp, start = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Line),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).background(Vinko.Mint, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("Recoger", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Vinko.Mint)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Vinko.Muted, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(8.dp).background(Color(0xFFEF4444), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("Llevar a destino", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    Spacer(Modifier.width(8.dp))
                    Text("· $distance km", fontSize = 11.sp, color = Vinko.Muted)
                }
            }

            // Botón flotante inferior derecho: Navegación Turn-by-Turn GPS
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clickable { openTurnByTurn(ctx, targetPos, targetLabel) },
                shape = RoundedCornerShape(12.dp),
                color = Vinko.Mint,
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Abrir GPS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // 2. PANEL DE DETALLE Y CONTROL DE LA RUTA SELECCIONADA
        VCard(padding = 16.dp) {
            // Cabecera del viaje
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Vinko.Tint,
                        ) {
                            Text(
                                text = "Folio #${id.take(8).uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Vinko.Primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = shipmentTypeLabel(type),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Vinko.Muted,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = title.ifBlank { "Despacho asignado" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Vinko.Ink,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = money(fare, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Vinko.Mint,
                    )
                    Text(
                        text = "Ganas: ${money(riderEarnings.netFareMinor, currency)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Vinko.Ink,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Paso actual en el ciclo de vida
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = when (status) {
                    "accepted" -> Color(0xFFEFF6FF)
                    "picking_up" -> Color(0xFFFEF3C7)
                    "in_transit" -> Color(0xFFEDE9FE)
                    else -> Color(0xFFF3F4F6)
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val stepIcon = when (status) {
                        "accepted" -> Icons.Outlined.LocationOn
                        "picking_up" -> Icons.Outlined.CheckCircle
                        "in_transit" -> Icons.Outlined.LocalShipping
                        else -> Icons.Outlined.CheckCircle
                    }
                    val stepColor = when (status) {
                        "accepted" -> Color(0xFF2563EB)
                        "picking_up" -> Color(0xFFD97706)
                        "in_transit" -> Vinko.Primary
                        else -> Vinko.Mint
                    }
                    Icon(stepIcon, contentDescription = null, tint = stepColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = statusLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = stepColor,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // TARJETA 1: DESDE DÓNDE RECOGERÁ (PUNTO DE RECOGIDA / ORIGEN)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(Vinko.Mint, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "DESDE DÓNDE RECOGERÁS (ORIGEN)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF166534),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = originAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Vinko.Ink,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Contacto: $originContactName ${if (!originContactPhone.isNullOrBlank()) "· $originContactPhone" else ""}",
                            fontSize = 11.sp,
                            color = Color(0xFF15803D),
                        )
                        if (!originContactPhone.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF25D366),
                                modifier = Modifier.clickable {
                                    shareViaWhatsApp(ctx, "Hola $originContactName, soy tu conductor de SoyVinko. Estoy en camino a recoger el pedido/viaje.", originContactPhone)
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("WhatsApp", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // TARJETA 2: A DÓNDE LO LLEVARÁS (PUNTO DE ENTREGA / DESTINO)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF2F2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(Color(0xFFEF4444), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "A DÓNDE LO LLEVARÁS (DESTINO)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF991B1B),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = destAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Vinko.Ink,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Receptor: $destContactName ${if (!destContactPhone.isNullOrBlank()) "· $destContactPhone" else ""}",
                            fontSize = 11.sp,
                            color = Color(0xFFB91C1C),
                        )
                        if (!destContactPhone.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF25D366),
                                modifier = Modifier.clickable {
                                    shareViaWhatsApp(ctx, "Hola $destContactName, soy tu conductor de SoyVinko. Llevo tu despacho en camino.", destContactPhone)
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("WhatsApp", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // BOTONES DE ACCIÓN PRINCIPALES
            if (nextAction.isNotBlank()) {
                PrimaryButton(
                    text = actionLabel,
                    loading = busy,
                    onClick = { onAdvance(id, nextAction, actionSuccessMsg) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Vinko.MintBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Mint.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { openTurnByTurn(ctx, targetPos, targetLabel) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Outlined.Navigation, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Navegar GPS", color = Vinko.Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3F4F6),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { onViewTracking(id) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Vinko.Ink, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ver Detalle", color = Vinko.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Tarjeta de Viaje en Curso asignado al Conductor con acciones de Navegación GPS. */
@Composable
private fun ActiveTripCard(
    shipment: JsonObject,
    onAdvance: (String, String, String) -> Unit,
    onViewTracking: (String) -> Unit,
    busy: Boolean,
) {
    val ctx = LocalPlatformContext.current
    val id = shipment.s("id").orEmpty()
    val status = shipment.s("status").orEmpty()
    val type = shipment.s("shipment_type")
    val title = shipment.s("title").orEmpty()
    val origin = shipment.s("origin_address").orEmpty()
    val destination = shipment.s("destination_address").orEmpty()
    val fare = shipment.l("fare_minor") ?: 0L
    val currency = shipment.s("currency") ?: "USD"
    val vehicleType = shipment.s("vehicle_type") ?: "moto"
    val distance = shipment.l("distance_km") ?: 5L
    val paymentMethod = shipment.s("payment_method") ?: "wallet"

    val originLat = shipment.d("origin_lat") ?: 10.4806
    val originLon = shipment.d("origin_lon") ?: -66.9036
    val destLat = shipment.d("destination_lat") ?: 10.5000
    val destLon = shipment.d("destination_lon") ?: -66.9167
    val targetPos = if (status == "in_transit") GeoPos(destLat, destLon) else GeoPos(originLat, originLon)
    val targetLabel = if (status == "in_transit") "Destino ($destination)" else "Origen ($origin)"

    val (statusLabel, nextAction, actionLabel, actionSuccessMsg) = when (status) {
        "accepted" -> Quad("1. Aceptado · Dirígete al origen", "arrive_origin", "Llegué al origen", "Marcado como llegado al origen")
        "picking_up" -> Quad("2. En el origen · Carga a bordo", "picked_up", "Iniciar ruta al destino", "Carga recogida, viaje en curso")
        "in_transit" -> Quad("3. En camino al destino", "delivered", "Finalizar / Entregado", "¡Viaje entregado con éxito!")
        else -> Quad(status, "", "", "")
    }

    VCard(padding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                statusLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Vinko.Primary,
            )
            Text(
                money(fare, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Vinko.Mint,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(title.ifBlank { "Despacho en curso" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text("Desde: $origin", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 1)
        Text("Hasta: $destination", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 1)

        Spacer(Modifier.height(12.dp))

        // Botones de acción: GPS turn-by-turn + Detalle + Avanzar estado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Vinko.MintBg,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { openTurnByTurn(ctx, targetPos, targetLabel) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 6.dp),
                ) {
                    Icon(Icons.Outlined.Navigation, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Abrir GPS", color = Vinko.Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            SecondaryButton(
                text = "Ver mapa",
                onClick = { onViewTracking(id) },
                modifier = Modifier.weight(1f).height(40.dp),
            )

            if (nextAction.isNotBlank()) {
                PrimaryButton(
                    text = actionLabel,
                    loading = busy,
                    onClick = { onAdvance(id, nextAction, actionSuccessMsg) },
                    modifier = Modifier.weight(1.5f).height(40.dp),
                )
            }
        }
    }
}

/** Tarjeta de Viaje Completado. */
@Composable
private fun CompletedTripCard(
    shipment: JsonObject,
    onViewDetail: (String) -> Unit,
) {
    val id = shipment.s("id").orEmpty()
    val title = shipment.s("title").orEmpty()
    val fare = shipment.l("fare_minor") ?: 0L
    val currency = shipment.s("currency") ?: "USD"
    val destination = shipment.s("destination_address").orEmpty()
    val earnings = calculateRiderEarnings(fare, currency = currency)

    VCard(padding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Entregado con éxito", style = MaterialTheme.typography.labelSmall, color = Vinko.Mint, fontWeight = FontWeight.Bold)
                }
                Text(title.ifBlank { "Entrega realizada" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Destino: $destination", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted, maxLines = 1)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    money(fare, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Vinko.Ink,
                )
                Text(
                    "Ganas: ${money(earnings.netFareMinor, currency)}",
                    fontSize = 11.sp,
                    color = Vinko.Mint,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Diálogo interactivo para configurar el vehículo del Rider con validación de placa. */
@Composable
private fun VehicleSetupDialog(
    current: VehicleConfig,
    onDismiss: () -> Unit,
    onSave: (VehicleConfig) -> Unit,
) {
    var type by remember { mutableStateOf(current.type) }
    var plate by remember { mutableStateOf(current.plate) }
    var model by remember { mutableStateOf(current.model) }
    var color by remember { mutableStateOf(current.color) }

    val plateValidation = remember(plate) { validateLicensePlate(plate) }
    val plateError = if (plate.isNotBlank() && !plateValidation.isValid) plateValidation.errorMessage else null
    val isFormValid = plateValidation.isValid && model.isNotBlank()

    val vehicleTypes = listOf(
        "moto" to ("Moto" to Icons.Outlined.TwoWheeler),
        "carro" to ("Carro" to Icons.Outlined.DirectionsCar),
        "pickup" to ("Pickup" to Icons.Outlined.LocalShipping),
        "camion" to ("Camión" to Icons.Outlined.LocalShipping),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar mi vehículo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Tipo de vehículo", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    vehicleTypes.forEach { (t, pair) ->
                        val (lbl, icon) = pair
                        val selected = type == t
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Vinko.Tint else Color(0xFFF3F4F6),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Vinko.Primary else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { type = t },
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (selected) Vinko.Primary else Color(0xFF6B7280),
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    lbl,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Vinko.Primary else Color(0xFF4B5563),
                                )
                            }
                        }
                    }
                }

                VinkoTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase().take(12) },
                    label = "Placa del vehículo",
                    supporting = "Ej. AB123CD (sin guiones)",
                    error = plateError,
                )

                VinkoTextField(
                    value = model,
                    onValueChange = { model = it.take(40) },
                    label = "Marca y modelo",
                    supporting = "Ej. Empire Horse 150 / Toyota Yaris",
                    error = if (model.isBlank()) "Indica el modelo de tu vehículo" else null,
                )

                VinkoTextField(
                    value = color,
                    onValueChange = { color = it.take(20) },
                    label = "Color",
                    supporting = "Ej. Negro, Blanco, Azul",
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Guardar",
                enabled = isFormValid,
                onClick = {
                    onSave(VehicleConfig(type, plate, model, color))
                },
                modifier = Modifier.width(110.dp),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Vinko.Muted)
            }
        },
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
