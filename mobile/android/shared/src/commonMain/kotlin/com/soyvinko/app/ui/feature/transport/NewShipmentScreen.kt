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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.LocalPlatformContext
import com.soyvinko.app.data.MapboxRouting
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.MapMarker
import com.soyvinko.app.ui.common.OsmMap
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.currentPosition
import com.soyvinko.app.ui.common.formatNumber
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.money
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.rememberPhotoPicker
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.common.toRadians
import com.soyvinko.app.ui.common.uploadFile
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.feature.explore.LocalPhoto
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// VehicleTypeInfo, VEHICLE_CATALOG, haversineKm y calculateStandardTransportFare viven en TransportKit.kt

/** Formulario mejorado en 4 pasos para solicitar un envío / transporte. */
@Composable
fun NewShipmentScreen(nav: NavHostController) {
    val app = LocalApp.current
    val ctx = LocalPlatformContext.current
    val user = LocalUser.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    // Control de paso actual (0: Ventana Principal Vehículo, 1: Carga/Personas, 2: Ruta en Mapa, 3: Resumen y Tarifa)
    var currentStep by remember { mutableIntStateOf(0) }

    // Paso 0: Tipo de vehículo (deslizable en marco completo)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { VEHICLE_CATALOG.size })
    val selectedVehicle = VEHICLE_CATALOG[pagerState.currentPage]
    val vehicleType = selectedVehicle.id

    // Paso 1: ¿Qué se va a transportar?
    var transportCategory by remember { mutableStateOf("cargo") } // "cargo" (mercancía) o "passengers" (personas)
    var passengerCount by remember { mutableIntStateOf(1) }
    var cargoTitle by remember { mutableStateOf("") }
    var cargoDescription by remember { mutableStateOf("") }
    var cargoPhoto by remember { mutableStateOf<UploadPart?>(null) }

    // Selector de foto de la mercancía
    val pickCargoPhoto = rememberPhotoPicker(max = 1) { parts ->
        cargoPhoto = parts.firstOrNull()
    }

    // Paso 2: Ruta en mapa
    var originPos by remember { mutableStateOf<GeoPos?>(null) }
    var destPos by remember { mutableStateOf<GeoPos?>(null) }
    var editingPoint by remember { mutableStateOf("origin") } // "origin" o "dest"
    var originAddress by remember { mutableStateOf("") }
    var destAddress by remember { mutableStateOf("") }
    var originContact by remember { mutableStateOf("") }
    var originPhone by remember { mutableStateOf("") }
    var destContact by remember { mutableStateOf("") }
    var destPhone by remember { mutableStateOf("") }

    // Herramientas para moverse en el mapa (zoom, centrado dinámico y expansión)
    var mapCenter by remember { mutableStateOf<GeoPos?>(null) }
    var mapZoom by remember { mutableDoubleStateOf(14.5) }
    var isMapExpanded by remember { mutableStateOf(false) }

    // Cálculo de ruta real con Mapbox Directions
    var routePoints by remember { mutableStateOf<List<GeoPos>>(emptyList()) }
    var drivingDistanceKm by remember { mutableDoubleStateOf(0.0) }
    var drivingDurationMin by remember { mutableIntStateOf(0) }
    var isLoadingRoute by remember { mutableStateOf(false) }

    // Inicializar origen por GPS si está disponible
    LaunchedEffect(Unit) {
        val cur = currentPosition(ctx)
        if (cur != null) {
            originPos = cur
            mapCenter = cur
            mapZoom = 15.5
            if (originAddress.isBlank()) originAddress = "Mi ubicación actual (GPS)"
            editingPoint = "dest"
        }
    }

    // Geocodificación inversa automática para eliminar coordenadas numéricas
    LaunchedEffect(originPos) {
        val o = originPos
        if (o != null && (originAddress.isBlank() || originAddress.contains("("))) {
            val resolved = MapboxRouting.reverseGeocode(o)
            if (!resolved.isNullOrBlank()) originAddress = resolved
            else if (originAddress.contains("(")) originAddress = "Mi ubicación actual (GPS)"
        }
    }
    LaunchedEffect(destPos) {
        val d = destPos
        if (d != null && (destAddress.isBlank() || destAddress.contains("("))) {
            val resolved = MapboxRouting.reverseGeocode(d)
            if (!resolved.isNullOrBlank()) destAddress = resolved
            else if (destAddress.contains("(")) destAddress = "Punto de entrega seleccionado"
        }
    }

    // Calcular ruta de carretera al cambiar puntos
    LaunchedEffect(originPos, destPos) {
        val o = originPos
        val d = destPos
        if (o != null && d != null) {
            isLoadingRoute = true
            val route = MapboxRouting.calculateRoute(o, d)
            if (route != null && route.points.isNotEmpty()) {
                routePoints = route.points
                drivingDistanceKm = route.distanceKm
                drivingDurationMin = route.durationMinutes
            } else {
                val fallback = (haversineKm(o, d) * 10).roundToInt() / 10.0
                routePoints = listOf(o, d)
                drivingDistanceKm = fallback
                drivingDurationMin = (fallback * 2.5).roundToInt()
            }
            isLoadingRoute = false
        } else {
            routePoints = emptyList()
            drivingDistanceKm = 0.0
            drivingDurationMin = 0
        }
    }

    // Paso 3: Tarifas, Costo Medio y Función Regatear
    val currency = "USD"
    val standardFare = remember(drivingDistanceKm, vehicleType) {
        calculateStandardTransportFare(drivingDistanceKm, vehicleType)
    }
    var isBargain by remember { mutableStateOf(false) }
    var bargainAmount by remember { mutableStateOf("") }

    LaunchedEffect(standardFare) {
        if (bargainAmount.isBlank() || !isBargain) {
            bargainAmount = standardFare.roundToInt().toString()
        }
    }

    var isSubmitting by remember { mutableStateOf(false) }

    // Método de pago: "wallet" o "cash"
    var paymentMethod by remember { mutableStateOf("wallet") }
    val walletRes = rememberResource { app.ops.call("money_wallet", "currency" to "USD").obj() }

    // Foto de perfil de la cuenta del usuario (Dave Bastidas)
    val avatarId = user?.avatarFileId ?: app.avatarFileId

    // Marcadores del mapa interactivo con foto de perfil en origen e icono de vehículo en destino
    val markers = remember(originPos, destPos, originAddress, destAddress, vehicleType, avatarId) {
        val list = mutableListOf<MapMarker>()
        originPos?.let {
            list.add(
                MapMarker(
                    pos = it,
                    title = "Desde: ${originAddress.ifBlank { "Origen" }}",
                    color = 0xFF0C8E72.toInt(), // Verde Menta (#0C8E72) de la guía Vinko
                    iconType = "avatar",
                    avatarUrl = avatarId,
                )
            )
        }
        destPos?.let {
            list.add(
                MapMarker(
                    pos = it,
                    title = "Hasta: ${destAddress.ifBlank { "Destino" }}",
                    color = 0xFF4B1FAE.toInt(), // Morado Profundo (#4B1FAE) de la guía Vinko
                    iconType = vehicleType,
                )
            )
        }
        list
    }

    ScreenScaffold(
        title = when (currentStep) {
            0 -> "Transportes y Envíos"
            1 -> "1. ¿Qué Transportas?"
            2 -> "2. Ruta en Mapa"
            else -> "3. Resumen y Tarifa"
        },
        onBack = {
            if (currentStep > 0) currentStep-- else nav.popBackStack()
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Barra de progreso de los 3 pasos de la solicitud (visible al avanzar)
            if (currentStep >= 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (1..3).forEach { stepNum ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (stepNum <= currentStep) Vinko.Primary else Color(0xFFE5E7EB)
                                ),
                        )
                    }
                }
            }

            // CONTENIDO SEGÚN EL PASO ACTIVO
            when (currentStep) {
                // ==========================================
                // VENTANA PRINCIPAL: SELECCIÓN DE VEHÍCULO
                // ==========================================
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                "Selecciona el vehículo",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Desliza horizontalmente para elegir la opción adecuada",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                            Spacer(Modifier.height(16.dp))

                            // Marco completo deslizable con HorizontalPager
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                            ) { page ->
                                val v = VEHICLE_CATALOG[page]
                                Card(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp)
                                        .border(2.dp, if (pagerState.currentPage == page) v.color else Color(0xFFE5E7EB), RoundedCornerShape(24.dp)),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        // Badge superior
                                        Box(
                                            modifier = Modifier
                                                .background(v.color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                        ) {
                                            Text(v.capacity, color = v.color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        // Emoji y nombre
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(v.emoji, fontSize = 72.sp)
                                            Spacer(Modifier.height(8.dp))
                                            Text(v.name, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF1F2937))
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                v.subtitle,
                                                fontSize = 13.sp,
                                                color = Color.Gray,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 12.dp),
                                            )
                                        }

                                        // Tarifa base sugerida
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(v.baseRateSummary, fontSize = 12.sp, color = Color(0xFF4B5563), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Indicador de puntos (dots)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                VEHICLE_CATALOG.indices.forEach { index ->
                                    val isSelected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(if (isSelected) 10.dp else 8.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) selectedVehicle.color else Color(0xFFD1D5DB)),
                                    )
                                }
                            }
                        }

                        // Botón de continuar limpio en la parte inferior
                        PrimaryButton(
                            text = "Continuar al Paso 1: ¿Qué Transportas?",
                            onClick = { currentStep = 1 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        )
                    }
                }

                // ==========================================
                // PASO 1: ¿QUÉ DESEAS TRANSPORTAR? + FOTO
                // ==========================================
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "¿Qué deseas transportar?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )

                            // Selector Carga vs Personas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { transportCategory = "cargo" }
                                        .border(
                                            2.dp,
                                            if (transportCategory == "cargo") Vinko.Primary else Color(0xFFE5E7EB),
                                            RoundedCornerShape(16.dp),
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (transportCategory == "cargo") Vinko.Primary.copy(alpha = 0.08f) else Color.White,
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(36.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text("Mercancía / Carga", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Paquetes, mudanzas", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { transportCategory = "passengers" }
                                        .border(
                                            2.dp,
                                            if (transportCategory == "passengers") Vinko.Primary else Color(0xFFE5E7EB),
                                            RoundedCornerShape(16.dp),
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (transportCategory == "passengers") Vinko.Primary.copy(alpha = 0.08f) else Color.White,
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Icon(Icons.Outlined.Group, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(36.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text("Personas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Traslado de pasajeros", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }

                            // CASO A: TRANSPORTE DE PERSONAS
                            if (transportCategory == "passengers") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        Text("¿Cuántas personas viajarán?", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Cantidad de pasajeros:", color = Color.Gray, fontSize = 14.sp)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            ) {
                                                IconButton(
                                                    onClick = { if (passengerCount > 1) passengerCount-- },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(Color(0xFFF3F4F6), CircleShape),
                                                ) {
                                                    Icon(Icons.Outlined.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                                                }
                                                Text(
                                                    "$passengerCount",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    color = Vinko.Primary,
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val maxAllowed = if (vehicleType == "moto") 1 else 4
                                                        if (passengerCount < maxAllowed) passengerCount++
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(Color(0xFFF3F4F6), CircleShape),
                                                ) {
                                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        if (vehicleType == "moto" && passengerCount > 1) {
                                            Text(
                                                "⚠️ En moto solo se permite 1 pasajero por seguridad.",
                                                color = Vinko.Coral,
                                                fontSize = 12.sp,
                                            )
                                        }

                                        OutlinedTextField(
                                            value = cargoDescription,
                                            onValueChange = { cargoDescription = it },
                                            label = { Text("Indicaciones adicionales (opcional)") },
                                            placeholder = { Text("Ej. Llevamos equipaje, maletas o niños...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                    }
                                }
                            } else {
                                // CASO B: TRANSPORTE DE MERCANCÍA / CARGA CON FOTO
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        OutlinedTextField(
                                            value = cargoTitle,
                                            onValueChange = { cargoTitle = it },
                                            label = { Text("¿Qué deseas transportar? *") },
                                            placeholder = { Text("Ej. Televisor y maletas, Repuestos, Cajas...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                        )

                                        OutlinedTextField(
                                            value = cargoDescription,
                                            onValueChange = { cargoDescription = it },
                                            label = { Text("Descripción o dimensiones aproximadas") },
                                            placeholder = { Text("Ej. 2 cajas medianas de 15 kg, es frágil...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            minLines = 2,
                                            shape = RoundedCornerShape(12.dp),
                                        )

                                        // Ranura de foto de la mercancía
                                        Text("Foto de lo que deseas transportar (Recomendado):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                        val photo = cargoPhoto
                                        if (photo != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp)),
                                            ) {
                                                LocalPhoto(photo, modifier = Modifier.fillMaxSize())

                                                Row(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                                            .clickable { pickCargoPhoto() }
                                                            .padding(8.dp),
                                                    ) {
                                                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Vinko.Coral, CircleShape)
                                                            .clickable { cargoPhoto = null }
                                                            .padding(8.dp),
                                                    ) {
                                                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(Color(0xFFF9FAFB))
                                                    .border(2.dp, Color(0xFFD1D5DB), RoundedCornerShape(16.dp))
                                                    .clickable { pickCargoPhoto() },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(36.dp))
                                                    Spacer(Modifier.height(6.dp))
                                                    Text("+ Agregar Foto de la Carga", fontWeight = FontWeight.Bold, color = Vinko.Primary, fontSize = 13.sp)
                                                    Text("Ayuda al transportista a conocer el volumen", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Botones de navegación
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            SecondaryButton(
                                text = "Atrás",
                                onClick = { currentStep = 0 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                            )
                            PrimaryButton(
                                text = "Continuar al Paso 2: Ruta en Mapa",
                                onClick = {
                                    if (transportCategory == "cargo" && cargoTitle.trim().length < 3) {
                                        scope.launch { snackbar.showSnackbar("Por favor ingresa qué deseas transportar.") }
                                    } else {
                                        currentStep = 2
                                    }
                                },
                                modifier = Modifier
                                    .weight(2f)
                                    .height(52.dp),
                            )
                        }
                    }
                }

                // ==========================================
                // PASO 2: RUTA DESDE Y HASTA EN EL MAPA
                // ==========================================
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // Mapa interactivo con herramientas de navegación y ruta Mapbox
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isMapExpanded) 420.dp else 290.dp)
                                .animateContentSize(),
                        ) {
                            OsmMap(
                                markers = markers,
                                modifier = Modifier.fillMaxSize(),
                                center = mapCenter ?: originPos ?: destPos ?: GeoPos(10.4806, -66.9036),
                                zoom = mapZoom,
                                drawLine = markers.size >= 2,
                                routePoints = routePoints,
                                onTap = { tapped ->
                                    if (editingPoint == "origin") {
                                        originPos = tapped
                                        originAddress = "Buscando dirección..."
                                        scope.launch {
                                            val resolved = MapboxRouting.reverseGeocode(tapped)
                                            originAddress = resolved ?: "Punto de recogida seleccionado"
                                        }
                                        editingPoint = "dest"
                                    } else {
                                        destPos = tapped
                                        destAddress = "Buscando dirección..."
                                        scope.launch {
                                            val resolved = MapboxRouting.reverseGeocode(tapped)
                                            destAddress = resolved ?: "Punto de entrega seleccionado"
                                        }
                                    }
                                },
                            )

                            // Indicador sobre el mapa
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 10.dp)
                                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    if (editingPoint == "origin") "📍 Toca el mapa para fijar ORIGEN (Desde)"
                                    else "🎯 Toca el mapa para fijar DESTINO (Hasta)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }

                            // Panel de herramientas flotantes para moverse en el mapa
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // 1. Acercar mapa (+)
                                MapFloatingButton(
                                    icon = Icons.Outlined.Add,
                                    contentDescription = "Acercar mapa",
                                    onClick = { mapZoom = (mapZoom + 1.0).coerceAtMost(19.0) },
                                )
                                // 2. Alejar mapa (-)
                                MapFloatingButton(
                                    icon = Icons.Outlined.Remove,
                                    contentDescription = "Alejar mapa",
                                    onClick = { mapZoom = (mapZoom - 1.0).coerceAtLeast(6.0) },
                                )
                                // 3. Centrar en mi GPS actual
                                MapFloatingButton(
                                    icon = Icons.Outlined.MyLocation,
                                    contentDescription = "Centrar en mi GPS",
                                    tint = Color(0xFF10B981),
                                    onClick = {
                                        scope.launch {
                                            val cur = currentPosition(ctx)
                                            if (cur != null) {
                                                mapCenter = cur
                                                mapZoom = 16.5
                                                if (editingPoint == "origin") {
                                                    originPos = cur
                                                    originAddress = "Mi ubicación actual (GPS)"
                                                }
                                            } else {
                                                snackbar.showSnackbar("No se pudo obtener la señal GPS.")
                                            }
                                        }
                                    },
                                )
                                // 4. Centrar ruta completa si ambos puntos están fijados
                                if (originPos != null && destPos != null) {
                                    MapFloatingButton(
                                        icon = Icons.Outlined.NearMe,
                                        contentDescription = "Centrar toda la ruta",
                                        tint = Vinko.Primary,
                                        onClick = {
                                            val o = originPos!!
                                            val d = destPos!!
                                            mapCenter = GeoPos((o.lat + d.lat) / 2.0, (o.lon + d.lon) / 2.0)
                                            val dist = haversineKm(o, d)
                                            mapZoom = when {
                                                dist > 40 -> 9.5
                                                dist > 15 -> 11.5
                                                dist > 5 -> 13.0
                                                else -> 14.5
                                            }
                                        },
                                    )
                                }
                                // 5. Expandir o reducir tamaño del mapa para explorar cómodamente
                                MapFloatingButton(
                                    icon = Icons.Outlined.ZoomOutMap,
                                    contentDescription = if (isMapExpanded) "Reducir mapa" else "Expandir mapa",
                                    tint = if (isMapExpanded) Vinko.Primary else Color(0xFF4B5563),
                                    onClick = { isMapExpanded = !isMapExpanded },
                                )
                            }

                            // Resumen flotante de distancia
                            if (drivingDistanceKm > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp)
                                        .background(Vinko.Primary, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isLoadingRoute) {
                                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(
                                            if (drivingDurationMin > 0) "Ruta: $drivingDistanceKm km (~$drivingDurationMin min)"
                                            else "Ruta: $drivingDistanceKm km",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            // Selector de punto activo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                FilterChip(
                                    selected = editingPoint == "origin",
                                    onClick = { editingPoint = "origin" },
                                    label = { Text("1. Origen (Foto 👤)", fontWeight = if (editingPoint == "origin") FontWeight.Bold else FontWeight.Medium) },
                                    leadingIcon = {
                                        Box(Modifier.size(10.dp).background(Color(0xFF0C8E72), CircleShape))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.White,
                                        labelColor = Color(0xFF374151),
                                        selectedContainerColor = Color(0xFFE6FAF5),
                                        selectedLabelColor = Color(0xFF065F46),
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color(0xFFE5E7EB),
                                        selectedBorderColor = Color(0xFF0C8E72),
                                        borderWidth = 1.dp,
                                        selectedBorderWidth = 1.5.dp,
                                        enabled = true,
                                        selected = editingPoint == "origin",
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                                FilterChip(
                                    selected = editingPoint == "dest",
                                    onClick = { editingPoint = "dest" },
                                    label = { Text("2. Destino (${selectedVehicle.emoji})", fontWeight = if (editingPoint == "dest") FontWeight.Bold else FontWeight.Medium) },
                                    leadingIcon = {
                                        Box(Modifier.size(10.dp).background(Color(0xFF4B1FAE), CircleShape))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.White,
                                        labelColor = Color(0xFF374151),
                                        selectedContainerColor = Color(0xFFF3F0FF),
                                        selectedLabelColor = Color(0xFF4B1FAE),
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color(0xFFE5E7EB),
                                        selectedBorderColor = Color(0xFF4B1FAE),
                                        borderWidth = 1.dp,
                                        selectedBorderWidth = 1.5.dp,
                                        enabled = true,
                                        selected = editingPoint == "dest",
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            // Campos de dirección
                            OutlinedTextField(
                                value = originAddress,
                                onValueChange = { originAddress = it },
                                label = { Text("Punto de Recogida (Desde)") },
                                placeholder = { Text("Dirección o punto de partida") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        scope.launch {
                                            val cur = currentPosition(ctx)
                                            if (cur != null) {
                                                originPos = cur
                                                originAddress = "Mi ubicación actual (GPS)"
                                            } else {
                                                snackbar.showSnackbar("No se pudo obtener la ubicación GPS.")
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Outlined.MyLocation, contentDescription = null, tint = Vinko.Primary)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                            )

                            OutlinedTextField(
                                value = destAddress,
                                onValueChange = { destAddress = it },
                                label = { Text("Punto de Entrega (Hasta)") },
                                placeholder = { Text("Dirección o lugar de entrega") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            )

                            // Contactos opcionales
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OutlinedTextField(
                                    value = destContact,
                                    onValueChange = { destContact = it },
                                    label = { Text("Contacto Destino") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                OutlinedTextField(
                                    value = destPhone,
                                    onValueChange = { destPhone = it },
                                    label = { Text("Teléfono Destino") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Botones de navegación
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                SecondaryButton(
                                    text = "Atrás",
                                    onClick = { currentStep = 1 },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                )
                                PrimaryButton(
                                    text = "Continuar al Paso 3: Tarifa y Resumen",
                                    onClick = {
                                        if (originPos == null || destPos == null) {
                                            scope.launch { snackbar.showSnackbar("Fija los puntos de origen y destino en el mapa.") }
                                        } else {
                                            currentStep = 3
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(52.dp),
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // PASO 3: RESUMEN, COSTO MEDIO Y REGATEAR
                // ==========================================
                3 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            "Resumen y Tarifa del Envío",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )

                        // 1. TARJETA RESUMEN
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(selectedVehicle.emoji, fontSize = 28.sp)
                                    Column {
                                        Text(selectedVehicle.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            if (transportCategory == "passengers") "Traslado de $passengerCount personas"
                                            else cargoTitle.ifBlank { "Envío de mercancía" },
                                            fontSize = 13.sp,
                                            color = Color.Gray,
                                        )
                                    }
                                }

                                // Si hay foto de la mercancía, mostrar miniatura en el resumen
                                val summaryPhoto = cargoPhoto
                                if (summaryPhoto != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(10.dp)),
                                        ) {
                                            LocalPhoto(summaryPhoto, modifier = Modifier.fillMaxSize())
                                        }
                                        Text("Foto de carga adjunta para el transportista", fontSize = 12.sp, color = Color(0xFF4B5563))
                                    }
                                }

                                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))

                                // Ruta y distancia
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text("Distancia por carretera:", fontSize = 12.sp, color = Color.Gray)
                                        Text("$drivingDistanceKm km", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Vinko.Primary)
                                    }
                                    if (drivingDurationMin > 0) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Tiempo estimado:", fontSize = 12.sp, color = Color.Gray)
                                            Text("~$drivingDurationMin min", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. COSTO MEDIO ESTIMADO
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                            elevation = CardDefaults.cardElevation(1.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Costo medio estándar:", color = Color(0xFF4B5563), fontWeight = FontWeight.Medium)
                                    Text(
                                        "$${standardFare.roundToInt()}.00 USD",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color(0xFF1F2937),
                                    )
                                }
                                Text(
                                    "Basado en la tabla de los transportistas (0-10 km $1.00 base + $0.20/km adicional, redondeado al entero superior).",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                )
                            }
                        }

                        // 3. FUNCIÓN REGATEAR
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    2.dp,
                                    if (isBargain) Color(0xFF10B981) else Color(0xFFE5E7EB),
                                    RoundedCornerShape(18.dp),
                                ),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBargain) Color(0xFFECFDF5) else Color.White,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(Icons.Outlined.Handshake, contentDescription = null, tint = if (isBargain) Color(0xFF10B981) else Color.Gray)
                                        Column {
                                            Text("Activar función Regatear", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Proponer mi propia tarifa", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    Switch(
                                        checked = isBargain,
                                        onCheckedChange = { isBargain = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF10B981),
                                        ),
                                    )
                                }

                                if (isBargain) {
                                    Text(
                                        "Coloca el valor que deseas ofrecer. Los transportistas verán tu propuesta y decidirán si la aprueban.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF047857),
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val curr = bargainAmount.toDoubleOrNull() ?: standardFare
                                                if (curr > 1.0) bargainAmount = (curr - 1.0).roundToInt().toString()
                                            },
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(Color.White, CircleShape)
                                                .border(1.dp, Color(0xFFE5E7EB), CircleShape),
                                        ) {
                                            Icon(Icons.Outlined.Remove, contentDescription = null)
                                        }

                                        OutlinedTextField(
                                            value = bargainAmount,
                                            onValueChange = { bargainAmount = it },
                                            label = { Text("Tu oferta ($)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                        )

                                        IconButton(
                                            onClick = {
                                                val curr = bargainAmount.toDoubleOrNull() ?: standardFare
                                                bargainAmount = (curr + 1.0).roundToInt().toString()
                                            },
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(Color.White, CircleShape)
                                                .border(1.dp, Color(0xFFE5E7EB), CircleShape),
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val finalFareNum = if (isBargain) {
                            bargainAmount.toDoubleOrNull() ?: standardFare
                        } else standardFare
                        val availableUsd = (walletRes.data?.l("available_minor") ?: 0L) / 100.0
                        val hasEnoughWallet = availableUsd >= finalFareNum

                        // 4. SELECCIÓN DE MÉTODO DE PAGO
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Método de pago",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1F2937),
                                    )
                                    Text(
                                        "Seguro y garantizado",
                                        fontSize = 11.sp,
                                        color = Vinko.Mint,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }

                                // Opción 1: Saldo de Wallet
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (paymentMethod == "wallet") Vinko.Tint else Color(0xFFF9FAFB))
                                        .border(
                                            2.dp,
                                            if (paymentMethod == "wallet") Vinko.Primary else Color(0xFFE5E7EB),
                                            RoundedCornerShape(14.dp),
                                        )
                                        .clickable { paymentMethod = "wallet" }
                                        .padding(14.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .background(
                                                        if (paymentMethod == "wallet") Vinko.Primary else Color(0xFFE5E7EB),
                                                        RoundedCornerShape(10.dp),
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    Icons.Outlined.AccountBalanceWallet,
                                                    contentDescription = null,
                                                    tint = if (paymentMethod == "wallet") Color.White else Color.Gray,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    "Saldo de mi Wallet",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF111827),
                                                )
                                                Text(
                                                    if (walletRes.loading) "Consultando saldo…"
                                                    else "Disponible: ${money(walletRes.data?.l("available_minor") ?: 0L, "USD")}",
                                                    fontSize = 12.sp,
                                                    color = if (hasEnoughWallet) Vinko.Mint else Vinko.Coral,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                            }
                                        }

                                        if (paymentMethod == "wallet") {
                                            Icon(
                                                Icons.Outlined.CheckCircle,
                                                contentDescription = "Seleccionado",
                                                tint = Vinko.Primary,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                }

                                // Opción 2: Efectivo / Al Conductor
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (paymentMethod == "cash") Vinko.Tint else Color(0xFFF9FAFB))
                                        .border(
                                            2.dp,
                                            if (paymentMethod == "cash") Vinko.Primary else Color(0xFFE5E7EB),
                                            RoundedCornerShape(14.dp),
                                        )
                                        .clickable { paymentMethod = "cash" }
                                        .padding(14.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .background(
                                                        if (paymentMethod == "cash") Vinko.Primary else Color(0xFFE5E7EB),
                                                        RoundedCornerShape(10.dp),
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Handshake,
                                                    contentDescription = null,
                                                    tint = if (paymentMethod == "cash") Color.White else Color.Gray,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    "Efectivo / Al Conductor",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF111827),
                                                )
                                                Text(
                                                    "Pagas al momento de la entrega o traslado",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray,
                                                )
                                            }
                                        }

                                        if (paymentMethod == "cash") {
                                            Icon(
                                                Icons.Outlined.CheckCircle,
                                                contentDescription = "Seleccionado",
                                                tint = Vinko.Primary,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                }

                                if (paymentMethod == "wallet" && !hasEnoughWallet && !walletRes.loading) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Vinko.CoralBg, RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            "Saldo insuficiente para cubrir esta tarifa.",
                                            fontSize = 11.sp,
                                            color = Vinko.Coral,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            "Recargar",
                                            fontSize = 12.sp,
                                            color = Vinko.Primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { nav.navigate(Routes.WALLET) },
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // BOTONES FINALES DE PUBLICACIÓN
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            SecondaryButton(
                                text = "Atrás",
                                onClick = { currentStep = 2 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                            )
                            PrimaryButton(
                                text = when {
                                    isBargain -> "Publicar Oferta ($${finalFareNum.roundToInt()}.00)"
                                    paymentMethod == "wallet" -> "Pagar con Wallet ($${standardFare.roundToInt()}.00)"
                                    else -> "Solicitar Envío ($${standardFare.roundToInt()}.00)"
                                },
                                loading = isSubmitting,
                                onClick = {
                                    if (paymentMethod == "wallet" && !hasEnoughWallet) {
                                        scope.launch { snackbar.showSnackbar("Saldo insuficiente en Wallet ($${availableUsd.roundToInt()}.00 USD). Por favor recarga o selecciona Efectivo.") }
                                        return@PrimaryButton
                                    }
                                    val oPos = originPos ?: GeoPos(10.4806, -66.9036)
                                    val dPos = destPos ?: GeoPos(10.5000, -66.9167)
                                    val fareMinor = (finalFareNum * 100).toLong()

                                    val title = if (transportCategory == "passengers") {
                                        "Traslado de $passengerCount pasajeros"
                                    } else {
                                        cargoTitle.ifBlank { "Envío de carga" }
                                    }

                                    isSubmitting = true
                                    scope.launch {
                                        try {
                                            // 1. Subir foto de la mercancía si fue seleccionada
                                            var cargoFileId: String? = null
                                            val submitPhoto = cargoPhoto
                                            if (submitPhoto != null) {
                                                cargoFileId = app.uploadFile(submitPhoto, "request")
                                            }

                                            // 2. Crear el despacho en la base de datos
                                            val res = app.ops.call(
                                                "create_shipment",
                                                "shipment_type" to if (transportCategory == "passengers") "ride" else if (vehicleType == "camion") "freight" else "package",
                                                "vehicle_type" to vehicleType,
                                                "title" to title.trim(),
                                                "description" to cargoDescription.trim(),
                                                "origin_address" to originAddress.trim(),
                                                "origin_lon" to oPos.lon,
                                                "origin_lat" to oPos.lat,
                                                "origin_contact_name" to originContact.trim(),
                                                "origin_contact_phone" to originPhone.trim(),
                                                "origin_instructions" to "",
                                                "destination_address" to destAddress.trim(),
                                                "destination_lon" to dPos.lon,
                                                "destination_lat" to dPos.lat,
                                                "destination_contact_name" to destContact.trim(),
                                                "destination_contact_phone" to destPhone.trim(),
                                                "destination_instructions" to "",
                                                "fare_minor" to fareMinor,
                                                "currency" to currency,
                                                "cargo_image_file_id" to cargoFileId,
                                                "passenger_count" to passengerCount,
                                                "is_bargain" to isBargain,
                                                "estimated_fare_minor" to (standardFare * 100).toLong(),
                                                "payment_method" to paymentMethod,
                                            ).obj()

                                            val newId = res?.s("id")
                                            snackbar.showSnackbar("¡Envío publicado! Buscando conductores en la zona.")
                                            if (newId != null) {
                                                nav.navigate(Routes.shipmentTracking(newId)) {
                                                    popUpTo(Routes.TRANSPORT)
                                                }
                                            } else {
                                                nav.popBackStack()
                                            }
                                        } catch (e: Exception) {
                                            snackbar.showSnackbar(e.message ?: "Error al solicitar el envío.")
                                        } finally {
                                            isSubmitting = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(2f)
                                    .height(54.dp),
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MapFloatingButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF1F2937),
    containerColor: Color = Color.White,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = modifier.size(38.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
