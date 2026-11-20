package com.soyvinko.app.ui.feature.profile

import android.os.Looper
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.soyvinko.app.SoyVinkoApp
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.FakeApi
import com.soyvinko.app.ui.TestUser
import com.soyvinko.app.ui.setScreen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.OffsetDateTime

/** Capturas de la sección Perfil con datos ficticios (app/build/outputs/roborazzi/profile_*.png). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = SoyVinkoApp::class)
class ProfileScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val api = FakeApi()

    @After fun tearDown() = api.shutdown()

    private fun ago(minutes: Long) = OffsetDateTime.now().minusMinutes(minutes).toString()

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun waitFor(text: String, substring: Boolean = false) {
        compose.waitUntil(10_000) {
            idle()
            compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty()
        }
        idle()
    }

    private fun shot(name: String) {
        idle()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/profile_$name.png")
    }

    private fun scrollDown() {
        compose.onRoot().performTouchInput { swipeUp(startY = bottom * 0.85f, endY = top + bottom * 0.15f) }
        idle()
        compose.waitForIdle()
    }

    private val account = """{"data":{"id":"u1","email":"maria.gonzalez@example.com","name":"María González","status":"active",
        "account_type":"natural","business_name":null,"rif":"V-18456789","contact_name":null,"roles":["customer","professional"],
        "contacts":[
          {"id":"c1","kind":"whatsapp","label":"Personal","value":"0414-5551234","share_on_contract":true},
          {"id":"c2","kind":"email","label":"Oficina","value":"maria@estudiogonzalez.com.ve","share_on_contract":false}],
        "addresses":[
          {"id":"a1","label":"Casa","address_line":"Av. Francisco de Miranda, Edif. Parque Cristal, piso 8, apto 8-B",
           "instructions":"Entrada por la torre oeste, conserjería en planta baja","city":"Libertador",
           "state_name":"Distrito Capital","municipality_name":"Libertador","parish_name":"El Recreo","latitude":10.4963,"longitude":-66.8718},
          {"id":"a2","label":"Trabajo","address_line":"Calle Madrid, Qta. Los Robles, Las Mercedes","instructions":null,"city":"Baruta",
           "state_name":"Miranda","municipality_name":"Baruta","parish_name":"Nuestra Señora del Rosario de Baruta","latitude":10.4805,"longitude":-66.8572}]}}"""

    private fun kycApproved() = """{"data":[{"id":"9b0c7a51-2f44-4c1e-9d0a-1f2b3c4d5e6f","status":"approved","submitted_at":"${ago(60L * 24 * 40)}",
        "reviewed_at":"${ago(60L * 24 * 39)}","expires_at":"${OffsetDateTime.now().plusYears(1)}","reason":null,
        "external_reference":"{\"document_type\":\"V\",\"document_number\":\"18456789\",\"full_name\":\"MARÍA ALEJANDRA GONZÁLEZ PÉREZ\",\"birthdate\":\"1990-05-21\"}",
        "documents":[{"kind":"identity_front","file_id":"f1"},{"kind":"selfie","file_id":"f2"}]}]}"""

    private fun kycPending() = """{"data":[
        {"id":"6f1c2d3e-7a8b-4c5d-9e0f-112233445566","status":"in_review","submitted_at":"${ago(60L * 26)}","expires_at":null,"reason":null,
         "external_reference":"{\"document_type\":\"V\",\"document_number\":\"18456789\",\"full_name\":\"MARÍA ALEJANDRA GONZÁLEZ PÉREZ\"}",
         "documents":[{"kind":"identity_front","file_id":"f1"},{"kind":"selfie","file_id":"f2"},{"kind":"proof_of_address","file_id":"f3"}]},
        {"id":"1a2b3c4d-0000-4000-8000-000000000001","status":"rejected","submitted_at":"${ago(60L * 24 * 12)}",
         "reason":"La foto de la cédula está borrosa y no se leen los datos.","documents":[]}]}"""

    @Test
    fun hub() {
        api.responses["/api/v1/account/dashboard"] = """{"data":{"requests":2,"active_contracts":3,"completed_contracts":8,"credit_balance":12,"unread_notifications":4}}"""
        api.responses["/api/v1/support/my-kyc"] = kycApproved()
        compose.setScreen(api) { nav -> ProfileHubScreen(nav) }
        waitFor("Identidad verificada")
        waitFor("4 sin leer")
        shot("hub")
        scrollDown()
        shot("hub_bottom")
    }

    @Test
    fun hubNewCustomer() {
        api.responses["/api/v1/account/dashboard"] = """{"data":{"requests":0,"active_contracts":0,"completed_contracts":0,"unread_notifications":0}}"""
        api.responses["/api/v1/support/my-kyc"] = """{"data":[]}"""
        val user = User("u2", "jose.perez@example.com", "José Pérez", emailVerified = false, roles = listOf("customer"))
        compose.setScreen(api, user) { nav -> ProfileHubScreen(nav) }
        waitFor("Sin verificar")
        scrollDown()
        waitFor("Activa tu perfil profesional")
        shot("hub_customer")
    }

    @Test
    fun account() {
        api.responses["/api/v1/account/my-account"] = account
        api.responses["/api/v1/account/payment-preferences"] = """{"data":{"requires_platform_payment":true}}"""
        api.responses["/api/v1/support/my-kyc"] = """{"data":[]}"""
        compose.setScreen(api) { AccountScreen(onBack = {}) }
        waitFor("Información personal")
        shot("account")
        scrollDown()
        waitFor("Preferencias de pago")
        shot("account_bottom")
    }

    @Test
    fun accountCompany() {
        api.responses["/api/v1/account/my-account"] = """{"data":{"id":"u3","email":"administracion@avila.com.ve","name":"Inversiones Ávila C.A.",
            "account_type":"juridico","business_name":"Inversiones Ávila C.A.","rif":"J-40123456-7","contact_name":"Carlos Rengifo","contacts":[],"addresses":[]}}"""
        api.responses["/api/v1/account/payment-preferences"] = """{"data":{"requires_platform_payment":false}}"""
        api.responses["/api/v1/support/my-kyc"] = kycApproved()
        val user = User("u3", "administracion@avila.com.ve", "Inversiones Ávila C.A.", accountType = "juridico", businessName = "Inversiones Ávila C.A.")
        compose.setScreen(api, user) { AccountScreen(onBack = {}) }
        waitFor("Información de la empresa")
        waitFor("Confirmado por tu verificación", substring = true)
        shot("account_company")
    }

    @Test
    fun addresses() {
        api.responses["/api/v1/account/my-account"] = account
        compose.setScreen(api) { AddressesScreen(onBack = {}, onAdd = {}) }
        waitFor("Casa")
        shot("addresses")
    }

    @Test
    fun addAddress() {
        api.responses["/api/v1/public/geo-states"] = """{"data":[{"code":"VE02","name":"Amazonas"},{"code":"VE03","name":"Anzoátegui"},
            {"code":"VE01","name":"Distrito Capital"},{"code":"VE15","name":"Miranda"},{"code":"VE23","name":"Zulia"}]}"""
        api.responses["/api/v1/public/geo-municipalities"] = """{"data":[{"code":"VE0101","name":"Libertador","state_code":"VE01"}]}"""
        api.responses["/api/v1/public/geo-parishes"] = """{"data":[{"code":"VE010108","name":"El Paraíso","municipality_code":"VE0101"},
            {"code":"VE010109","name":"El Recreo","municipality_code":"VE0101"},{"code":"VE010120","name":"Santa Teresa","municipality_code":"VE0101"}]}"""
        api.responses["/api/v1/public/geo-parish"] = """{"data":{"code":"VE010109","name":"El Recreo","municipality_code":"VE0101","state_code":"VE01",
            "state_name":"Distrito Capital","municipality_name":"Libertador","country_code":"VE","latitude":10.516828,"longitude":-66.877321,
            "bbox":[-66.897104,10.487339,-66.861346,10.546072],
            "geometry":{"type":"Polygon","coordinates":[[[-66.86135,10.51258],[-66.87271,10.54607],[-66.87988,10.54159],[-66.89048,10.54565],
            [-66.89066,10.51445],[-66.8971,10.50428],[-66.89424,10.4974],[-66.8881,10.49752],[-66.88412,10.49117],[-66.87362,10.48734],
            [-66.869,10.48751],[-66.86939,10.49828],[-66.86135,10.51258]]]}}}"""
        compose.setScreen(api) { AddAddressScreen(onBack = {}, onSaved = {}) }
        waitFor("Usar mi ubicación actual")
        shot("add_address_empty")
        compose.onNodeWithText("Casa").performClick()
        compose.onNodeWithText("Estado").performClick()
        waitFor("Distrito Capital")
        compose.onNodeWithText("Distrito Capital").performClick()
        idle()
        compose.onNodeWithText("Municipio").performClick()
        waitFor("Libertador")
        compose.onNodeWithText("Libertador").performClick()
        idle()
        compose.onNodeWithText("Parroquia").performClick()
        waitFor("El Recreo")
        compose.onNodeWithText("El Recreo").performClick()
        idle()
        shot("add_address")
        compose.onNodeWithText("Continuar").performClick()
        waitFor("Dirección y número")
        shot("add_address_map")
        assertTrue(api.requests.any { it.path!!.startsWith("/api/v1/public/geo-parish?parish=VE010109") })
    }

    @Test
    fun kycPendingStatus() {
        api.responses["/api/v1/support/my-kyc"] = kycPending()
        api.responses["/api/v1/account/my-account"] = account
        compose.setScreen(api) { KycScreen(onBack = {}, onStart = {}) }
        waitFor("Verificación en revisión")
        shot("kyc")
        scrollDown()
        shot("kyc_bottom")
    }

    @Test
    fun kycApprovedStatus() {
        api.responses["/api/v1/support/my-kyc"] = kycApproved()
        api.responses["/api/v1/account/my-account"] = account
        compose.setScreen(api) { KycScreen(onBack = {}, onStart = {}) }
        waitFor("Identidad verificada")
        shot("kyc_approved")
    }

    @Test
    fun kycIntroAndSubmit() {
        api.responses["/api/v1/support/my-kyc"] = """{"data":[]}"""
        api.responses["/api/v1/account/my-account"] = account
        compose.setScreen(api) { KycScreen(onBack = {}, onStart = {}) }
        waitFor("Comenzar verificación")
        shot("kyc_intro")
    }

    @Test
    fun kycSubmit() {
        api.responses["/api/v1/account/my-account"] = account
        compose.setScreen(api) { KycSubmitScreen(onBack = {}, onDone = {}) }
        waitFor("María González")
        shot("kyc_submit")
        scrollDown()
        shot("kyc_submit_form")
    }

    @Test
    fun security() {
        api.responses["/api/v1/account/my-sessions"] = """{"data":[
            {"id":"s1","device_label":"SoyVinko-Android/1.4.0 (samsung SM-A546E; Android 14)","created_at":"${ago(30)}","expires_at":"${OffsetDateTime.now().plusDays(29)}"},
            {"id":"s2","device_label":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Safari/537.36","created_at":"${ago(60L * 24 * 3)}","expires_at":"${OffsetDateTime.now().plusDays(20)}"},
            {"id":"s3","device_label":"Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1","created_at":"${ago(60L * 24 * 9)}","expires_at":"${OffsetDateTime.now().plusDays(10)}"}]}"""
        api.responses["/api/v1/auth/mfa"] = """{"data":{"enabled":false}}"""
        val user = TestUser.copy(emailVerified = false)
        compose.setScreen(api, user) { SecurityScreen(onBack = {}) }
        waitFor("Confirma tu correo")
        shot("security")
        scrollDown()
        scrollDown()
        waitFor("Chrome en Windows")
        shot("security_sessions")
    }

    @Test
    fun notifications() {
        api.responses["/api/v1/account/my-notifications"] = """{"data":[
            {"id":"n1","topic":"delivery.submitted","aggregate_id":"c1","read_at":null,"created_at":"${ago(12)}"},
            {"id":"n2","topic":"proposal.received","aggregate_id":"r1","read_at":null,"created_at":"${ago(95)}"},
            {"id":"n3","topic":"chat.message","aggregate_id":"c2","read_at":null,"created_at":"${ago(60L * 5)}"},
            {"id":"n4","topic":"contract.funded","aggregate_id":"c1","read_at":"${ago(60L * 20)}","created_at":"${ago(60L * 26)}"},
            {"id":"n5","topic":"review.requested","aggregate_id":"c3","read_at":"${ago(60L * 40)}","created_at":"${ago(60L * 24 * 3)}"},
            {"id":"n6","topic":"request.available","aggregate_id":"r9","read_at":"${ago(60L * 24 * 5)}","created_at":"${ago(60L * 24 * 6)}"},
            {"id":"n7","topic":"contract.accepted","aggregate_id":"c3","read_at":"${ago(60L * 24 * 9)}","created_at":"${ago(60L * 24 * 10)}"}]}"""
        compose.setScreen(api) { NotificationsScreen(onBack = {}, onOpen = {}) }
        waitFor("Tienes una entrega por revisar")
        shot("notifications")
    }

    @Test
    fun support() {
        api.responses["/api/v1/support/my-tickets"] = """{"data":[
            {"id":"t1","category":"service","subject":"El profesional no llegó a la cita del sábado","status":"waiting_user","created_at":"${ago(60L * 30)}"},
            {"id":"t2","category":"credits","subject":"No veo reflejada mi recarga por Pago Móvil","status":"in_progress","created_at":"${ago(60L * 24 * 4)}"},
            {"id":"t3","category":"account","subject":"Cambiar el correo de mi cuenta","status":"resolved","created_at":"${ago(60L * 24 * 20)}"}]}"""
        compose.setScreen(api) { SupportScreen(onBack = {}, onNew = {}, onOpen = {}) }
        waitFor("El profesional no llegó a la cita del sábado")
        shot("support")
    }

    @Test
    fun newTicket() {
        compose.setScreen(api) { NewTicketScreen(onBack = {}, onCreated = {}) }
        waitFor("¿Con qué te ayudamos?")
        shot("new_ticket")
    }

    @Test
    fun ticketThread() {
        api.responses["/api/v1/support/ticket-thread"] = """{"data":{"id":"t1","category":"service","status":"waiting_user",
            "subject":"El profesional no llegó a la cita del sábado","created_at":"${ago(60L * 30)}",
            "description":"Contraté una reparación de aire acondicionado para el sábado a las 9 a. m. y el profesional no se presentó ni respondió el chat.",
            "messages":[
              {"id":"m1","author_id":"staff1","body":"Hola, María. Lamentamos lo ocurrido. Ya contactamos al profesional. ¿Prefieres reprogramar o que liberemos el reembolso del pago en custodia?","created_at":"${ago(60L * 20)}"},
              {"id":"m2","author_id":"u1","body":"Prefiero el reembolso, gracias.","created_at":"${ago(60L * 19)}"},
              {"id":"m3","author_id":"staff1","body":"Listo. El reembolso quedará disponible en tu wallet en las próximas 24 horas.","created_at":"${ago(45)}"}]}}"""
        compose.setScreen(api) { TicketScreen(id = "t1", onBack = {}) }
        waitFor("Prefiero el reembolso, gracias.")
        shot("ticket")
    }

    @Test
    fun legal() {
        compose.setScreen(api) { LegalScreen(initial = "terminos", onBack = {}) }
        waitFor("Términos y Condiciones de Uso de SoyVinko")
        shot("legal")
        compose.onNodeWithText("Privacidad").performClick()
        waitFor("Política de Privacidad y Protección de Datos")
        scrollDown()
        shot("legal_privacy")
    }
}
