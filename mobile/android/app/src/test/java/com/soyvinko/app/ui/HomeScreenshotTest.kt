package com.soyvinko.app.ui

import android.os.Looper
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.soyvinko.app.SoyVinkoApp
import com.soyvinko.app.ui.screens.HomeScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** "Mi resumen" con la misma información que el panel web. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = SoyVinkoApp::class)
class HomeScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val api = FakeApi()

    @After fun tearDown() = api.shutdown()

    private fun ago(h: Long) = OffsetDateTime.now(ZoneOffset.UTC).minusHours(h).toString()

    @Test
    fun dashboardParity() {
        api.responses["/api/v1/auth/me"] = """{"data":{"id":"u1","email":"maria.gonzalez@example.com","name":"María González","email_verified":true,"roles":["customer"],"permissions":[]}}"""
        api.responses["/api/v1/account/dashboard"] = """{"data":{"requests":1,"active_contracts":2,"completed_contracts":2,"credit_balance":0,"unread_notifications":3}}"""
        api.responses["/api/v1/contracts/my-contracts"] = """{"data":[
          {"id":"c1","title":"Reparación de aire acondicionado split","status":"awaiting_confirmation","is_customer":true,"is_professional":false,"professional_name":"Frío Total C.A.","customer_name":"María González","agreed_price_minor":4500000,"currency":"VES","accepted_at":"${ago(50)}","delivery_mode":"onsite","professional_finished_at":"${ago(3)}","reviews_count":0},
          {"id":"c2","title":"Diseño de logotipo para panadería","status":"in_progress","is_customer":true,"professional_name":"Ana Rivas","agreed_price_minor":12000,"currency":"USD","accepted_at":"${ago(120)}","delivery_mode":"remote","reviews_count":0},
          {"id":"c3","title":"Instalación de tomacorrientes","status":"completed","is_customer":true,"professional_name":"Electro Pérez","agreed_price_minor":1800000,"currency":"VES","accepted_at":"${ago(400)}","delivery_mode":"onsite","reviews_count":0}]}"""
        api.responses["/api/v1/wallet/my-wallet"] = """{"data":{"balance":12,"credits":12,"ledger":[]}}"""
        api.responses["/api/v1/wallet/money-wallet?currency=VES"] = """{"data":{"currency":"VES","available_minor":8750000,"reserved_for_services_minor":4500000,"withdrawal_pending_minor":0}}"""
        api.responses["/api/v1/requests/my-requests"] = """{"data":[{"id":"r1","title":"Pintura de habitación 3x4 m","status":"open","created_at":"${ago(30)}","expires_at":"${OffsetDateTime.now(ZoneOffset.UTC).plusDays(4)}"}]}"""
        api.responses["/api/v1/support/my-kyc"] = """{"data":[]}"""
        api.responses["/api/v1/account/my-account"] = """{"data":{"addresses":[{"id":"a1"}]}}"""
        api.responses["/api/v1/account/my-notifications"] = """{"data":[
          {"id":"n1","topic":"delivery.submitted","aggregate_id":"c2","read_at":null,"created_at":"${ago(1)}"},
          {"id":"n2","topic":"proposal.received","aggregate_id":"r1","read_at":null,"created_at":"${ago(26)}"},
          {"id":"n3","topic":"contract.funded","aggregate_id":"c2","read_at":"${ago(20)}","created_at":"${ago(48)}"}]}"""
        compose.setScreen(api, TestUser.copy(roles = listOf("customer"))) { nav -> HomeScreen(TestUser.copy(roles = listOf("customer")), {}, { nav.navigate(it) }) }
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodes(hasText("Requiere tu atención")).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodes(hasText("Bs 87.500,00")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/home_1.png")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Tus servicios"))
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/home_2.png")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Tus solicitudes"))
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/home_3.png")
    }
}
