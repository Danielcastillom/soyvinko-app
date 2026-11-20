package com.soyvinko.app.ui.feature.transport

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalAtm
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.LocalPlatformContext
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.data.MapboxRouting
import com.soyvinko.app.ui.common.ErrorBox
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.LiveLocation
import com.soyvinko.app.ui.common.LoadingBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.PollWhileVisible
import com.soyvinko.app.ui.common.MapMarker
import com.soyvinko.app.ui.common.OsmMap
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.d
import com.soyvinko.app.ui.common.dialPhone
import com.soyvinko.app.ui.common.formatNumber
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.o
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.openTurnByTurn
import com.soyvinko.app.ui.common.relative
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.shareViaWhatsApp
import com.soyvinko.app.ui.feature.explore.Avatar
import com.soyvinko.app.ui.feature.pro.ProImage
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.feature.transport.buildShipmentReceiptText
import com.soyvinko.app.ui.feature.transport.calculateRiderEarnings
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/** Pantalla de Seguimiento de Ruta en Vivo y Gestión de Despacho (tipo Uber). */
@Composable
fun ShipmentTrackingScreen(id: String, nav: NavHostController) {
    val app = LocalApp.current
    val ctx = LocalPlatformContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var refreshTick by remember { mutableIntStateOf(0) }
    var isActionInProgress by remember { mutableStateOf(false) }

    // Auto-refrescar cada 6 segundos de forma reactiva mientras la pantalla está activa
    PollWhileVisible(6_000, id) {
        refreshTick++
    }

    val res = rememberResource(id, refreshTick) {
        app.ops.call("shipment_tracking", "shipment" to id).obj()
            ?: throw ApiException("Envío no encontrado", "not_found", 404)
    }

    val s = res.data
    val status = s?.s("status").orEmpty()
    val isDriver = s?.b("is_driver") == true
    val isSender = s?.b("is_sender") == true
    val canTake = s?.b("can_take") == true

    // Si el usuario es el conductor y el envío está activo, transmitir GPS en segundo plano
    val shouldBroadcastLocation = isDriver && status in listOf("accepted", "picking_up", "in_transit")
    LiveLocation(enabled = shouldBroadcastLocation, intervalMs = 8_000) { currentPos ->
        scope.launch {
            try {
                app.ops.call(
                    "update_shipment_location",
                    "shipment" to id,
                    "lon" to currentPos.lon,
                    "lat" to currentPos.lat,
                    "accuracy" to currentPos.accuracy?.toInt(),
                )
            } catch (_: Exception) {}
        }
    }

    ScreenScaffold(
        title = "Rastreo de Despacho",
        onBack = { nav.popBackStack() },
    ) { padding ->
        when {
            res.loading && s == null -> LoadingBox()
            res.error != null && s == null -> ErrorBox(res.error.orEmpty()) { res.reload() }
            s != null -> {
                TrackingContent(
                    shipment = s,
                    isDriver = isDriver,
                    isSender = isSender,
                    canTake = canTake,
                    isActionInProgress = isActionInProgress,
                    onTakeShipment = {
                        isActionInProgress = true
                        scope.launch {
                            try {
                                app.ops.call("take_shipment", "shipment" to id)
                                snackbar.showSnackbar("¡Envío tomado! Despacho iniciado.")
                                refreshTick++
                            } catch (e: Exception) {
                                snackbar.showSnackbar(e.message ?: "No se pudo tomar el envío.")
                            } finally {
                                isActionInProgress = false
                            }
                        }
                    },
                    onAdvance = { action ->
                        isActionInProgress = true
                        scope.launch {
                            try {
                                app.ops.call("advance_shipment", "shipment" to id, "action" to action)
                                val msg = when (action) {
                                    "arrive_origin" -> "Llegada al origen confirmada."
                                    "picked_up" -> "Carga recogida. En camino al destino."
                                    "delivered" -> "¡Entrega confirmada con éxito!"
                                    "cancel" -> "Envío cancelado."
                                    else -> "Estado actualizado."
                                }
                                snackbar.showSnackbar(msg)
                                refreshTick++
                            } catch (e: Exception) {
                                snackbar.showSnackbar(e.message ?: "Error al actualizar estado.")
                            } finally {
                                isActionInProgress = false
                            }
                        }
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun TrackingContent(
    shipment: JsonObject,
    isDriver: Boolean,
    isSender: Boolean,
    canTake: Boolean,
    isActionInProgress: Boolean,
    onTakeShipment: () -> Unit,
    onAdvance: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalPlatformContext.current
    val title = shipment.s("title").orEmpty()
    val status = shipment.s("status").orEmpty()
    val vehicleType = shipment.s("vehicle_type").orEmpty()
    val fare = shipment.l("fare_minor") ?: 0L
    val currency = shipment.s("currency") ?: "VES"
    val distKm = shipment.d("distance_km") ?: 0.0

    val origin = shipment.o("origin")
    val dest = shipment.o("destination")
    val driverLoc = shipment.o("driver_location")

    val originPos = origin?.let { GeoPos(it.d("latitude") ?: 0.0, it.d("longitude") ?: 0.0) }
    val destPos = dest?.let { GeoPos(it.d("latitude") ?: 0.0, it.d("longitude") ?: 0.0) }
    val driverPos = driverLoc?.let { GeoPos(it.d("latitude") ?: 0.0, it.d("longitude") ?: 0.0) }

    val driver = shipment.o("driver")
    val sender = shipment.o("sender")

    val cargoImageId = shipment.s("cargo_image_file_id")
    val passengers = shipment.l("passenger_count") ?: 1L
    val isBargain = shipment.b("is_bargain") == true

    // Marcadores para el mapa en vivo (foto de perfil en origen e icono de vehículo en destino)
    val markers = remember(originPos, destPos, driverPos, vehicleType, sender) {
        val list = mutableListOf<MapMarker>()
        originPos?.let {
            list.add(
                MapMarker(
                    pos = it,
                    title = "Origen: ${origin?.s("address").orEmpty()}",
                    color = 0xFF0C8E72.toInt(), // Verde Menta (#0C8E72) de la guía Vinko
                    iconType = "avatar",
                    avatarUrl = sender?.s("avatar_file_id"),
                )
            )
        }
        destPos?.let {
            list.add(
                MapMarker(
                    pos = it,
                    title = "Destino: ${dest?.s("address").orEmpty()}",
                    color = 0xFF4B1FAE.toInt(), // Morado Profundo (#4B1FAE) de la guía Vinko
                    iconType = vehicleType,
                )
            )
        }
        driverPos?.let {
            list.add(
                MapMarker(
                    pos = it,
                    title = "Conductor en movimiento",
                    color = 0xFF6F3DFF.toInt(),
                    iconType = vehicleType,
                )
            )
        }
        list
    }

    // Ruta real por carretera (Mapbox Directions)
    var routePoints by remember { mutableStateOf<List<GeoPos>>(emptyList()) }
    LaunchedEffect(originPos, destPos) {
        val o = originPos
        val d = destPos
        if (o != null && d != null) {
            val route = MapboxRouting.calculateRoute(o, d)
            if (route != null && route.points.isNotEmpty()) {
                routePoints = route.points
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // 1. Mapa interactivo de ruta y rastreo en vivo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        ) {
            OsmMap(
                markers = markers,
                modifier = Modifier.fillMaxSize(),
                center = driverPos ?: originPos ?: destPos,
                zoom = if (driverPos != null) 15.0 else 14.0,
                drawLine = markers.size >= 2,
                routePoints = routePoints,
            )

            // Pill flotante de estado en el mapa
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusDotColor = when (status) {
                        "requested" -> Color(0xFFF59E0B)
                        "accepted" -> Vinko.Primary
                        "picking_up" -> Color(0xFF3B82F6)
                        "in_transit" -> Color(0xFF8B5CF6)
                        "delivered" -> Color(0xFF10B981)
                        else -> Color.Gray
                    }
                    Box(Modifier.size(8.dp).background(statusDotColor, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (status) {
                            "requested" -> "Esperando conductor (Despacho no iniciado)"
                            "accepted" -> "¡Despacho iniciado! Conductor asignado"
                            "picking_up" -> "Conductor en el origen"
                            "in_transit" -> "Carga en camino al destino"
                            "delivered" -> "Envío entregado"
                            else -> "Cancelado"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF1F2937),
                    )
                }
            }

            // Botón de navegación externa hacia Google Maps / Waze
            if (isDriver && (status == "accepted" || status == "picking_up" || status == "in_transit")) {
                val targetNavPos = if (status == "in_transit") destPos else originPos
                val targetLabel = if (status == "in_transit") "Destino" else "Origen"
                if (targetNavPos != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(Vinko.Mint, RoundedCornerShape(12.dp))
                            .clickable { openTurnByTurn(ctx, targetNavPos, targetLabel) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Abrir GPS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. Banner de ciclo de vida del pedido
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Caso A: El envío está disponible y el usuario puede tomarlo (como Uber)
            if (canTake) {
                VCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Vinko.Primary.copy(alpha = 0.08f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "⚡ Pedido Disponible para Tomar",
                            fontWeight = FontWeight.Bold,
                            color = Vinko.Primary,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Al tomar este envío iniciará inmediatamente el despacho y podrás ver todos los detalles de recogida.",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 18.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        PrimaryButton(
                            text = if (isActionInProgress) "Iniciando Despacho..." else "Tomar Envío (Iniciar Despacho)",
                            onClick = onTakeShipment,
                            enabled = !isActionInProgress,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        )
                    }
                }
            }

            // Caso C: Despacho completado con éxito
            if (status == "delivered") {
                VCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0FDF4), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier.size(54.dp).background(Color(0xFFDCFCE7), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("¡Entrega Finalizada con Éxito!", fontWeight = FontWeight.Bold, color = Color(0xFF14532D), fontSize = 17.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "El despacho se ha completado y la operación está registrada en SoyVinko.",
                            fontSize = 13.sp,
                            color = Color(0xFF166534),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(14.dp))
                        PrimaryButton(
                            text = "Enviar Comprobante por WhatsApp",
                            onClick = {
                                val receipt = buildShipmentReceiptText(
                                    shipmentId = shipment.s("id").orEmpty(),
                                    title = title,
                                    fareMinor = fare,
                                    currency = currency,
                                    vehicleType = vehicleType,
                                    originAddress = origin?.s("address").orEmpty(),
                                    destAddress = dest?.s("address").orEmpty(),
                                    status = status,
                                    driverName = driver?.s("name"),
                                    paymentMethod = shipment.s("payment_method"),
                                    distanceKm = distKm,
                                )
                                val targetPhone = if (isDriver) dest?.s("contact_phone") ?: origin?.s("contact_phone") else driver?.s("phone")
                                shareViaWhatsApp(ctx, receipt, targetPhone)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        )
                    }
                }
            }
            if (isDriver && status in listOf("accepted", "picking_up", "in_transit")) {
                VCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Acciones del Conductor", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        when (status) {
                            "accepted" -> {
                                PrimaryButton(
                                    text = if (isActionInProgress) "Actualizando..." else "1. Llegué al Origen",
                                    onClick = { onAdvance("arrive_origin") },
                                    enabled = !isActionInProgress,
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                )
                            }
                            "picking_up" -> {
                                PrimaryButton(
                                    text = if (isActionInProgress) "Actualizando..." else "2. Carga / Pasajero a Bordo",
                                    onClick = { onAdvance("picked_up") },
                                    enabled = !isActionInProgress,
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                )
                            }
                            "in_transit" -> {
                                PrimaryButton(
                                    text = if (isActionInProgress) "Confirmando..." else "3. Confirmar Entrega Final",
                                    onClick = { onAdvance("delivered") },
                                    enabled = !isActionInProgress,
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 3. Tarjeta de Contacto / Conductor
            if (driver != null) {
                val driverPhone = driver.s("phone") ?: driver.s("contact_phone")
                VCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(initials = driver.s("name").orEmpty().take(2).uppercase(), photoUrl = driver.s("avatar_file_id"), size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Conductor Asignado", fontSize = 11.sp, color = Vinko.Mint, fontWeight = FontWeight.Bold)
                            Text(driver.s("name").orEmpty(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Vehículo: ${vehicleLabel(vehicleType)}", fontSize = 12.sp, color = Color.Gray)
                        }
                        if (isSender && !driverPhone.isNullOrBlank()) {
                            IconButton(
                                onClick = { dialPhone(ctx, driverPhone) },
                                modifier = Modifier.background(Vinko.MintBg, CircleShape).size(36.dp),
                            ) {
                                Icon(Icons.Outlined.Call, contentDescription = "Llamar conductor", tint = Vinko.Mint, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // 4. Detalle de Ruta: Origen -> Destino
            VCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Detalles de la Ruta", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(money(fare, currency), fontWeight = FontWeight.Bold, color = Vinko.Mint, fontSize = 16.sp)
                    }

                    if (isBargain || passengers > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isBargain) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFECFDF5), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text("🤝 Tarifa Regateada", color = Color(0xFF047857), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            if (passengers > 1) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text("👥 $passengers Pasajeros", color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (!cargoImageId.isNullOrBlank()) {
                        Column {
                            Text("Foto de la Carga:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            ProImage(
                                file = cargoImageId,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                            )
                        }
                    }

                    // Desde (Origen)
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(top = 4.dp).size(12.dp).background(Color(0xFF10B981), CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Punto de Recogida (Desde)", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Text(origin?.s("address").orEmpty(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            val oContact = origin?.s("contact_name")
                            val oPhone = origin?.s("contact_phone")
                            if (!oContact.isNullOrBlank() || !oPhone.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.padding(top = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Contacto: ${oContact.orEmpty()} ${oPhone.orEmpty()}".trim(), fontSize = 12.sp, color = Color.DarkGray)
                                    if (!oPhone.isNullOrBlank()) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Outlined.Call,
                                            contentDescription = "Llamar origen",
                                            tint = Vinko.Mint,
                                            modifier = Modifier.size(16.dp).clickable { dialPhone(ctx, oPhone) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Hasta (Destino)
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(top = 4.dp).size(12.dp).background(Color(0xFFEF4444), CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Punto de Entrega (Hasta)", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Text(dest?.s("address").orEmpty(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            val dContact = dest?.s("contact_name")
                            val dPhone = dest?.s("contact_phone")
                            if (!dContact.isNullOrBlank() || !dPhone.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.padding(top = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Contacto: ${dContact.orEmpty()} ${dPhone.orEmpty()}".trim(), fontSize = 12.sp, color = Color.DarkGray)
                                    if (!dPhone.isNullOrBlank()) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Outlined.Call,
                                            contentDescription = "Llamar destino",
                                            tint = Vinko.Mint,
                                            modifier = Modifier.size(16.dp).clickable { dialPhone(ctx, dPhone) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Métricas adicionales
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Distancia estimada:", fontSize = 13.sp, color = Color.Gray)
                        Text("$distKm km", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. Garantía de Pago y Rendimiento Financiero
            val paymentMethod = shipment.s("payment_method") ?: "wallet"
            VCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (paymentMethod) {
                                "wallet" -> Icons.Outlined.Shield
                                "pago_movil" -> Icons.Outlined.Payments
                                else -> Icons.Outlined.LocalAtm
                            },
                            contentDescription = null,
                            tint = if (paymentMethod == "wallet") Vinko.Mint else Vinko.Primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (paymentMethod) {
                                "wallet" -> "Garantía de Pago Protegido Vinko"
                                "pago_movil" -> "Pago Móvil Directo"
                                else -> "Cobro en Efectivo"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                    Text(
                        when (paymentMethod) {
                            "wallet" -> "El monto (${money(fare, currency)}) se encuentra en custodia digital segura. Se transferirá al conductor automáticamente tras confirmar la entrega final."
                            "pago_movil" -> "El monto acordado es ${money(fare, currency)}. Verifica la captura y referencia bancaria antes de entregar la carga."
                            else -> "El monto a cobrar en efectivo es ${money(fare, currency)}. Cobro presencial al momento de la entrega o recogida."
                        },
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        lineHeight = 17.sp,
                    )

                    if (isDriver) {
                        val earnings = calculateRiderEarnings(fare)
                        Spacer(Modifier.height(4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9FAFB), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tarifa total de la carrera", fontSize = 12.sp, color = Color.Gray)
                                Text(money(earnings.grossFareMinor, currency), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Comisión de servicio Vinko (10%)", fontSize = 12.sp, color = Color.Gray)
                                Text("- ${money(earnings.commissionMinor, currency)}", fontSize = 12.sp, color = Color(0xFFDC2626))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tu ganancia neta", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Vinko.Mint)
                                Text(money(earnings.netFareMinor, currency), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Vinko.Mint)
                            }
                        }
                    }
                }
            }

            // 6. Botón para compartir comprobante digital por WhatsApp
            OutlinedButton(
                onClick = {
                    val receipt = buildShipmentReceiptText(
                        shipmentId = shipment.s("id").orEmpty(),
                        title = title,
                        fareMinor = fare,
                        currency = currency,
                        vehicleType = vehicleType,
                        originAddress = origin?.s("address").orEmpty(),
                        destAddress = dest?.s("address").orEmpty(),
                        status = status,
                        driverName = driver?.s("name"),
                        paymentMethod = shipment.s("payment_method"),
                        distanceKm = distKm,
                    )
                    val targetPhone = if (isDriver) dest?.s("contact_phone") ?: origin?.s("contact_phone") else driver?.s("phone")
                    shareViaWhatsApp(ctx, receipt, targetPhone)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366)),
            ) {
                Icon(Icons.Outlined.Chat, contentDescription = null, tint = Color(0xFF128C7E), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Compartir Comprobante por WhatsApp", color = Color(0xFF128C7E), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Botón de cancelación si aún no fue entregado
            if ((isSender || isDriver) && status in listOf("requested", "accepted")) {
                OutlinedButton(
                    onClick = { onAdvance("cancel") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActionInProgress,
                ) {
                    Text("Cancelar Envío", color = Color(0xFFEF4444))
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
