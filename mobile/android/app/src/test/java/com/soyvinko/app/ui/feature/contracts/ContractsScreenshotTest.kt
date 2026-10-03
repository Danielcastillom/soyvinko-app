package com.soyvinko.app.ui.feature.contracts

import android.os.Looper
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.soyvinko.app.SoyVinkoApp
import com.soyvinko.app.ui.FakeApi
import com.soyvinko.app.ui.setScreen
import org.junit.After
import com.soyvinko.app.tracking.LocationTracking
import com.soyvinko.app.tracking.TrackingMode
import com.soyvinko.app.tracking.TrackingSession
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.soyvinko.app.ui.common.obj
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** Capturas de "Servicios contratados" con datos ficticios con la forma real de las funciones SQL. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = SoyVinkoApp::class)
class ContractsScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val api = FakeApi()

    @After fun tearDown() {
        api.shutdown()
        LocationTracking.update { null }
        Gps.reset()
    }

    private fun ago(minutes: Long): String = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(minutes).toString()
    private fun ahead(minutes: Long): String = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(minutes).toString()
    private fun agoSeconds(seconds: Long): String = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(seconds).toString()


    private fun ctxOf(detailJson: String, escrowJson: String? = null): ContractCtx {
        val c = Json.parseToJsonElement(detailJson).obj()!!["data"].obj()!!
        val e = escrowJson?.let { Json.parseToJsonElement(it).obj()!!["data"].obj() }
        return ContractCtx(c["id"].toString().trim('"'), c, e, "u1")
    }

    private fun waitFor(text: String) = compose.waitUntil(10_000) {
        shadowOf(Looper.getMainLooper()).idle()
        compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun shot(name: String) {
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/contracts_$name.png")
    }

    private fun scrollTo(text: String) {
        compose.onNodeWithText(text, substring = true).performScrollTo()
        shadowOf(Looper.getMainLooper()).idle()
    }

    // ------------------------------------------------------------------ datos

    private val myContracts get() = """{"data":[
      {"id":"c0a1b2c3-1111-4aaa-8bbb-000000000001","request_id":"r1","customer_id":"u1","professional_id":"p9","status":"en_route",
       "agreed_price_minor":4500000,"currency":"VES","payment_method":"Pago Móvil","payment_terms":null,"terms_version":1,
       "accepted_at":"${ago(300)}","en_route_at":"${ago(12)}","arrived_at":null,"started_at":null,"completed_at":null,
       "delivery_mode":"onsite","payment_protection":"external","work_requirements":"Filtración debajo del lavamanos del baño principal.",
       "title":"Reparación de filtración en baño","customer_name":"María González","professional_name":"Plomería Rodríguez",
       "is_customer":true,"is_professional":false,"reviews_count":0},
      {"id":"c0a1b2c3-2222-4aaa-8bbb-000000000002","request_id":"r2","customer_id":"u7","professional_id":"u1","status":"in_progress",
       "agreed_price_minor":25000,"currency":"USD","accepted_at":"${ago(4000)}","started_at":"${ago(3000)}",
       "delivery_mode":"remote","payment_protection":"guaranteed","work_requirements":"Logo, paleta y tipografía.",
       "title":"Diseño de logo e identidad para panadería","customer_name":"Carlos Pérez","professional_name":"María González",
       "is_customer":false,"is_professional":true,"reviews_count":0},
      {"id":"c0a1b2c3-3333-4aaa-8bbb-000000000003","request_id":"r3","customer_id":"u1","professional_id":"p4","status":"accepted",
       "agreed_price_minor":18000000,"currency":"VES","accepted_at":"${ago(60)}",
       "delivery_mode":"onsite","payment_protection":"guaranteed","title":"Instalación de aire acondicionado split 12.000 BTU",
       "customer_name":"María González","professional_name":"Frío Total C.A.","is_customer":true,"is_professional":false,"reviews_count":0},
      {"id":"c0a1b2c3-4444-4aaa-8bbb-000000000004","request_id":"r4","customer_id":"u1","professional_id":"p5","status":"completed",
       "agreed_price_minor":12000,"currency":"USD","accepted_at":"${ago(20000)}","started_at":"${ago(19000)}","professional_finished_at":"${ago(15000)}","completed_at":"${ago(14000)}",
       "delivery_mode":"remote","payment_protection":"guaranteed","title":"Traducción de documentos legales",
       "customer_name":"María González","professional_name":"Ana Torres","is_customer":true,"is_professional":false,"reviews_count":1}
    ]}"""

    private val customerDetail get() = """{"data":{
      "id":"c0a1b2c3-5555-4aaa-8bbb-000000000005","request_id":"r5","customer_id":"u1","professional_id":"p7","proposal_id":"pp1",
      "status":"awaiting_confirmation","agreed_price_minor":35000,"currency":"USD","payment_method":"platform_wallet","payment_terms":"50% de anticipo en custodia, entrega en 7 días.",
      "terms_version":1,"accepted_at":"${ago(9000)}","started_at":"${ago(8000)}","professional_finished_at":"${ago(90)}",
      "delivery_mode":"remote","payment_protection":"guaranteed","platform_fee_minor":0,
      "work_requirements":"Tienda en línea con catálogo de 40 productos, pago móvil y panel de pedidos.",
      "title":"Tienda en línea para mi emprendimiento","customer_name":"María González","customer_email":"maria.gonzalez@example.com",
      "professional_name":"Estudio Web Caracas","offering_title":"Desarrollo de tiendas en línea","request_description":"Necesito una tienda en línea.",
      "terms":[{"id":"t1","contract_id":"c5","version":1,"proposed_by":"p7","price_minor":35000,"currency":"USD","payment_method":"wallet",
        "payment_terms":"Entrega en 7 días con 2 rondas de ajustes.","customer_accepted_at":"${ago(8900)}","professional_accepted_at":"${ago(8950)}","created_at":"${ago(8950)}"}],
      "events":[{"id":1,"contract_id":"c5","actor_id":"u1","event_type":"mutual_acceptance","from_status":null,"to_status":"accepted","metadata":{},"created_at":"${ago(9000)}"},
        {"id":2,"contract_id":"c5","actor_id":"u1","event_type":"funds_reserved","from_status":null,"to_status":null,"metadata":{},"created_at":"${ago(8500)}"},
        {"id":3,"contract_id":"c5","actor_id":"p7","event_type":"remote_start","from_status":"accepted","to_status":"in_progress","metadata":{},"created_at":"${ago(8000)}"},
        {"id":4,"contract_id":"c5","actor_id":"p7","event_type":"delivery_submitted","from_status":null,"to_status":null,"metadata":{},"created_at":"${ago(90)}"}],
      "reference_photos":[{"file_id":"f1","mime_type":"image/jpeg"},{"file_id":"f2","mime_type":"image/jpeg"}],
      "reviews":[],"onsite_location":null}}"""

    private val customerEscrow get() = """{"data":{
      "contract_id":"c5","status":"funded","quote_id":"q1","funded_at":"${ago(8500)}","released_at":null,"refunded_at":null,
      "refund_requested_at":null,"refund_reason":null,"price_minor":35000,"currency":"USD","service_fee_minor":0,"professional_receives_minor":35000,
      "requirements":"Tienda en línea","funds_guaranteed":true,"funding_on_hold":false,
      "deliveries":[{"id":"d1","contract_id":"c5","professional_id":"p7","version":1,"summary":"Tienda publicada con 40 productos, pasarela de Pago Móvil y panel de pedidos. Credenciales enviadas por el chat.",
        "status":"submitted","terms_version":1,"created_at":"${ago(90)}","reviewed_at":null,"review_note":null,"files":["f3","f4"]}]}}"""

    private val contacts = """{"data":{"user_id":"p7","name":"Estudio Web Caracas","display_name":"Luis Mendoza","business_name":"Estudio Web Caracas",
      "email":"hola@estudiowebccs.com","primary_phone":"0412-5551234","primary_whatsapp":"0412-5551234",
      "contacts":[{"id":"k1","kind":"whatsapp","label":"Principal","value":"0412-5551234"}],"professional_contacts":[]}}"""

    private val proDetail get() = """{"data":{
      "id":"c0a1b2c3-6666-4aaa-8bbb-000000000006","request_id":"r6","customer_id":"u8","professional_id":"u1",
      "status":"en_route","agreed_price_minor":6000000,"currency":"VES","payment_method":"Pago Móvil","payment_terms":"Pago al finalizar.",
      "terms_version":1,"accepted_at":"${ago(240)}","en_route_at":"${ago(15)}","delivery_mode":"onsite","payment_protection":"external",
      "work_requirements":"Cambiar 3 tomacorrientes y revisar el breaker de la cocina que salta al usar el microondas.",
      "title":"Revisión eléctrica de cocina","customer_name":"Carlos Pérez","professional_name":"María González",
      "terms":[{"id":"t2","version":2,"proposed_by":"u8","price_minor":6500000,"currency":"VES","payment_method":"Pago Móvil",
          "payment_terms":"Incluye un tomacorriente adicional en el lavadero.","customer_accepted_at":"${ago(20)}","professional_accepted_at":null,"created_at":"${ago(20)}"},
        {"id":"t1","version":1,"proposed_by":"u1","price_minor":6000000,"currency":"VES","payment_method":"Pago Móvil",
          "payment_terms":"Pago al finalizar.","customer_accepted_at":"${ago(240)}","professional_accepted_at":"${ago(241)}","created_at":"${ago(241)}"}],
      "events":[{"id":1,"actor_id":"u8","event_type":"mutual_acceptance","to_status":"accepted","created_at":"${ago(240)}"},
        {"id":2,"actor_id":"u1","event_type":"depart","from_status":"accepted","to_status":"en_route","created_at":"${ago(15)}"}],
      "reference_photos":[],"reviews":[],
      "onsite_location":{"address_line":"Av. Francisco de Miranda, Edif. Parque Cristal, piso 4","instructions":"Tocar el intercomunicador 4B","city":"Caracas","country_code":"VE"}}}"""

    private val proPayments get() = """{"data":[{"id":"e1","contract_id":"c6","order_id":null,"reported_by":"u8","amount_minor":3000000,"currency":"VES",
      "method":"Pago Móvil","confirmed_by":null,"reported_at":"${ago(30)}","confirmed_at":null}]}"""

    private val chat get() = """{"data":[
      {"id":"m7","sender_id":"u1","kind":"text","body":"Perfecto, voy saliendo. Llego en unos 20 minutos.","file_id":null,"created_at":"${ago(14)}"},
      {"id":"m6","sender_id":"u8","kind":"location_shared","body":"El cliente ha compartido la ubicación del servicio.","file_id":null,"created_at":"${ago(30)}"},
      {"id":"m5","sender_id":"u8","kind":"attachment","body":null,"file_id":"f9","created_at":"${ago(32)}"},
      {"id":"m4","sender_id":"u8","kind":"text","body":"Te dejo foto del breaker para que traigas el repuesto correcto.","file_id":null,"created_at":"${ago(33)}"},
      {"id":"m3","sender_id":"u1","kind":"text","body":"¡Hola Carlos! Sí, mañana puedo a primera hora.","file_id":null,"created_at":"${ago(1500)}"},
      {"id":"m2","sender_id":"u8","kind":"text","body":"Buenas tardes, ¿podría venir mañana temprano?","file_id":null,"created_at":"${ago(1510)}"}
    ]}"""

    /** api.navigate (migración 041) con el GPS en vivo del cliente como destino. */
    private val navigateGps get() = """{"data":{
      "destination":{"latitude":10.4961,"longitude":-66.8486,"address":"Av. Francisco de Miranda, Edif. Parque Cristal, piso 4",
        "instructions":"Tocar el intercomunicador 4B","source":"gps","accuracy_m":9,"updated_at":"${agoSeconds(40)}"},
      "share":{"shared_at":"${ago(45)}","expires_at":"${ahead(435)}","source":"gps"},
      "professional_position":{"latitude":10.4880,"longitude":-66.8790,"recorded_at":"${agoSeconds(20)}","accuracy_m":12}}}"""

    private val statusGps get() = """{"data":{"active":true,"shared_at":"${ago(45)}","expires_at":"${ahead(435)}","revoked_at":null,
      "source":"gps","location_updated_at":"${agoSeconds(40)}","professional_position_at":"${agoSeconds(20)}","remote":false}}"""

    private val statusNone = """{"data":{"active":false,"shared_at":null,"expires_at":null,"revoked_at":null,
      "source":null,"location_updated_at":null,"professional_position_at":null,"remote":false}}"""

    private val asCustomer get() = proDetail.replace("\"customer_id\":\"u8\",\"professional_id\":\"u1\"", "\"customer_id\":\"u1\",\"professional_id\":\"u8\"")
        .replace("\"professional_name\":\"María González\"", "\"professional_name\":\"Electricidad Pérez\"")

    private val contractId = "c0a1b2c3-6666-4aaa-8bbb-000000000006"

    // ------------------------------------------------------------------ pruebas

    @Test
    fun list() {
        api.responses["/api/v1/contracts/my-contracts"] = myContracts
        compose.setScreen(api) { nav -> ContractsListScreen(nav) }
        waitFor("Reparación de filtración")
        shot("list")
    }

    @Test
    fun detailAsCustomer() {
        api.responses["/api/v1/contracts/contract-detail"] = customerDetail
        api.responses["/api/v1/contracts/escrow-details"] = customerEscrow
        api.responses["/api/v1/contracts/contract-contacts"] = contacts
        compose.setScreen(api) { nav -> ContractDetailScreen("c0a1b2c3-5555-4aaa-8bbb-000000000005", nav) }
        waitFor("Revisa la entrega")
        shot("detail_customer")
        scrollTo("Entregas del trabajo")
        shot("detail_customer_2")
        // Los contactos llegan en una consulta aparte (asíncrona).
        waitFor("Enviar correo")
        scrollTo("Enviar correo")
        shot("detail_customer_3")
    }

    @Test
    fun detailAsProfessional() {
        api.responses["/api/v1/contracts/contract-detail"] = proDetail
        api.responses["/api/v1/contracts/external-payment-reports"] = proPayments
        api.responses["/api/v1/contracts/contract-contacts"] = contacts.replace("Estudio Web Caracas", "Carlos Pérez")
        compose.setScreen(api) { nav -> ContractDetailScreen("c0a1b2c3-6666-4aaa-8bbb-000000000006", nav) }
        waitFor("Marcar llegada")
        waitFor("Confirmar recepción del pago")
        shot("detail_professional")
        scrollTo("Pago directo en persona")
        shot("detail_professional_2")
        scrollTo("Aceptar condiciones")
        shot("detail_professional_3")
        scrollTo("Historial del contrato")
        shot("detail_professional_4")
    }

    @Test
    fun chat() {
        api.responses["/api/v1/contracts/contract-detail"] = proDetail
        api.responses["/api/v1/contracts/chat-messages"] = chat
        compose.setScreen(api) { nav -> ChatScreen("c0a1b2c3-6666-4aaa-8bbb-000000000006", nav) }
        waitFor("voy saliendo")
        shot("chat")
    }

    @Test
    fun trackingProfessionalGpsDestination() {
        api.responses["/api/v1/contracts/contract-detail"] = proDetail
        api.responses["/api/v1/contracts/location-status"] = statusGps
        api.responses["/api/v1/contracts/navigate"] = navigateGps
        // Seguimiento en segundo plano activo para este contrato: la pantalla no debe enviar report_position.
        LocationTracking.update { TrackingSession(contractId, TrackingMode.PROFESSIONAL, "Carlos Pérez", lastSentAt = System.currentTimeMillis() - 8_000) }
        compose.setScreen(api) { nav -> TrackingScreen(contractId, nav) }
        waitFor("GPS en vivo del cliente")
        shot("tracking")
        scrollTo("Seguimiento activo")
        shot("tracking_2")
        assertTrue(api.requests.toList().none { it.path!!.startsWith("/api/v1/contracts/report-position") })
    }

    @Test
    fun trackingCustomerLiveStatus() {
        api.responses["/api/v1/contracts/contract-detail"] = asCustomer
        api.responses["/api/v1/contracts/location-status"] = statusGps
        api.responses["/api/v1/contracts/navigate"] = navigateGps
        compose.setScreen(api) { nav -> TrackingScreen(contractId, nav) }
        waitFor("Ubicación compartida")
        waitFor("Vence")
        shot("tracking_customer")
        scrollTo("Actualizar mi ubicación en segundo plano")
        shot("tracking_customer_2")
        scrollTo("Dejar de compartir")
        shot("tracking_customer_3")
    }

    @Test
    fun trackingCustomerSharesGps() {
        api.responses["/api/v1/contracts/contract-detail"] = asCustomer.replace("\"status\":\"en_route\"", "\"status\":\"accepted\"")
        api.responses["/api/v1/contracts/location-status"] = statusNone
        api.responses["/api/v1/contracts/share-location"] = """{"data":"s1"}"""
        Gps.hasPermission = { true }
        Gps.current = { com.soyvinko.app.ui.common.GeoPos(10.4961, -66.8486, 9.4f) }
        compose.setScreen(api) { nav -> TrackingScreen(contractId, nav) }
        waitFor("Mi ubicación actual (GPS en vivo)")
        shot("tracking_customer_share")
        compose.onNodeWithText("Compartir mi ubicación").performScrollTo().performClick()
        waitFor("Compartir GPS")
        shot("gps_share_sheet")
        compose.onNodeWithText("Compartir GPS").performClick()
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            runCatching { api.requests.toList() }.getOrDefault(emptyList()).any { it.path == "/api/v1/contracts/share-location" }
        }
        val body = api.requests.toList().first { it.path == "/api/v1/contracts/share-location" }.body.clone().readUtf8()
        val json = Json.parseToJsonElement(body).obj()!!
        assertEquals(contractId, json["contract"].toString().trim('"'))
        assertEquals(8, json["hours"].toString().toInt())
        assertEquals(10.4961, json["lat"].toString().toDouble(), 1e-9)
        assertEquals(-66.8486, json["lon"].toString().toDouble(), 1e-9)
        assertEquals(9, json["accuracy"].toString().toInt())
    }

    @Test
    fun trackingNotShared() {
        api.responses["/api/v1/contracts/contract-detail"] = proDetail.replace("\"status\":\"en_route\"", "\"status\":\"accepted\"")
        api.responses["/api/v1/contracts/location-status"] = statusNone
        compose.setScreen(api) { nav -> TrackingScreen(contractId, nav) }
        waitFor("Ubicación no disponible")
        shot("tracking_not_shared")
        // Sin permiso activo no se pide el mapa (navigate audita cada lectura en el servidor).
        assertTrue(api.requests.toList().none { it.path!!.startsWith("/api/v1/contracts/navigate") })
    }

    @Test
    fun detailTrackingBanner() {
        api.responses["/api/v1/contracts/contract-detail"] = proDetail
        api.responses["/api/v1/contracts/external-payment-reports"] = proPayments
        api.responses["/api/v1/contracts/location-status"] = statusGps
        LocationTracking.update { TrackingSession(contractId, TrackingMode.PROFESSIONAL, "Carlos Pérez", lastSentAt = System.currentTimeMillis() - 25_000) }
        compose.setScreen(api) { nav -> ContractDetailScreen(contractId, nav) }
        waitFor("Compartiendo tu trayecto en segundo plano")
        shot("detail_tracking_banner")
    }

    @Test
    fun fundDialog() {
        api.responses["/api/v1/wallet/money-wallet"] = """{"data":{"id":"w1","currency":"VES","status":"active","available_minor":25000000,"reserved_for_services_minor":0,"withdrawal_pending_minor":0}}"""
        val detail = myContracts.let { Json.parseToJsonElement(it).obj()!!["data"]!! as kotlinx.serialization.json.JsonArray }[2].obj()!!
        val ctx = ContractCtx(detail["id"].toString().trim('"'), detail, null, "u1")
        val quote = Json.parseToJsonElement(
            """{"id":"q9","contract_id":"c3","price_minor":18000000,"currency":"VES","fee_minor":0,"professional_receives_minor":18000000,"expires_at":"${ahead(15)}"}""",
        ).obj()!!
        compose.setScreen(api) { FundDialog(ctx, quote, {}, {}, {}) }
        waitFor("250.000,00")
        shot("fund_dialog")
    }

    @Test
    fun rateDialog() {
        val ctx = ctxOf(customerDetail.replace("\"status\":\"awaiting_confirmation\"", "\"status\":\"completed\""), customerEscrow)
        compose.setScreen(api) { RateDialog(ctx, Dlg.Rate(4, true, "Muy buen trabajo, puntual y ordenado."), {}, {}) }
        waitFor("Recomiendo a esta persona")
        shot("rate_dialog")
    }

    @Test
    fun deliveryDialog() {
        api.responses["/api/v1/support/my-files"] = """{"data":[{"id":"f21","purpose":"evidence","moderation_status":"approved","mime_type":"image/jpeg","bytes":120000},
          {"id":"f22","purpose":"chat","moderation_status":"approved","mime_type":"image/jpeg","bytes":90000},
          {"id":"f23","purpose":"kyc","moderation_status":"approved","mime_type":"image/jpeg","bytes":90000}]}"""
        val ctx = ctxOf(customerDetail.replace("\"customer_id\":\"u1\",\"professional_id\":\"p7\"", "\"customer_id\":\"u9\",\"professional_id\":\"u1\""), customerEscrow)
        compose.setScreen(api) { DeliveryDialog(ctx, {}, {}) }
        waitFor("Tus archivos aprobados")
        shot("delivery_dialog")
    }

    @Test
    fun proposeTermsDialog() {
        val ctx = ctxOf(proDetail)
        compose.setScreen(api) { ProposeTermsDialog(ctx, {}, {}) }
        waitFor("o escribe otra")
        shot("propose_dialog")
    }

    @Test
    fun reasonDialog() {
        compose.setScreen(api) {
            ReasonDialog(
                Dlg.Reason("Reportar un problema", "Cuéntanos qué pasó. Se abrirá un caso con soporte.", "Reportar", 10, null, destructive = true) {},
                {}, {},
            )
        }
        waitFor("Mínimo 10 caracteres")
        shot("reason_dialog")
    }

    // ------------------------------------------------------------------ reglas puras

    @Test
    fun amountsAndHelpers() {
        assertEquals(125050L, parseAmountMinor("1.250,50"))
        assertEquals(4550L, parseAmountMinor("45.5"))
        assertEquals(30000L, parseAmountMinor("300"))
        assertEquals(125000L, parseAmountMinor("1.250"))
        assertNull(parseAmountMinor("0"))
        assertNull(parseAmountMinor("abc"))
        assertEquals("4500,00", minorToInput(450000))
        assertEquals("584125551234", whatsappNumber("0412-5551234"))
        assertEquals("584125551234", whatsappNumber("+58 412 555 1234"))
        assertEquals("#VNK-C0A1B2C3", folio("c0a1b2c3-6666"))
        assertEquals("en 7 h 15 min", countdownText(1_000_000L + (7 * 3600 + 15 * 60) * 1000L, 1_000_000L))
        assertEquals("en 4 min 05 s", countdownText(1_000_000L + 245_000L, 1_000_000L))
        assertNull(countdownText(1_000L, 2_000L))
        assertEquals("hace 3 min", agoText(0L, 190_000L))
        assertTrue(LocStatus(active = false, sharedAt = "2026-01-01T00:00:00Z", expiresAt = "2026-01-01T08:00:00Z").expired)
        assertEquals(3, reachedStep(stepsFor("onsite"), "in_progress") { null })
        assertEquals(1, reachedStep(stepsFor("onsite"), "cancelled") { if (it == "en_route_at" || it == "accepted_at") "x" else null })
        assertTrue(distanceText(com.soyvinko.app.ui.common.GeoPos(10.4961, -66.8486), com.soyvinko.app.ui.common.GeoPos(10.4880, -66.8790))!!.endsWith("km"))
    }
}
