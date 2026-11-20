package com.soyvinko.app.ui

import android.os.Looper
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.soyvinko.app.SoyVinkoApp
import com.soyvinko.app.ui.common.SessionActions
import com.soyvinko.app.ui.shell.MainShell
import com.soyvinko.app.ui.theme.SoyVinkoTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** La app con sesión: las 5 pestañas abren sus secciones nativas (ninguna queda "En construcción"). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = SoyVinkoApp::class)
class ShellNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val api = FakeApi()

    @After fun tearDown() = api.shutdown()

    private fun idleUntil(text: String) = compose.waitUntil(10_000) {
        shadowOf(Looper.getMainLooper()).idle()
        compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun allTabsOpenNativeSections() {
        resetFileProviderCache()
        api.responses["/api/v1/auth/me"] = """{"data":{"id":"u1","email":"maria.gonzalez@example.com","name":"María González","email_verified":true,"account_type":"natural","roles":["customer","professional"],"permissions":[]}}"""
        api.responses["/api/v1/account/dashboard"] = """{"data":{"requests":1,"active_contracts":2,"completed_contracts":5,"credit_balance":7,"unread_notifications":3}}"""
        api.responses["/api/v1/public/catalog"] = """{"data":[]}"""
        api.responses["/api/v1/contracts/my-contracts"] = """{"data":[]}"""
        api.responses["/api/v1/wallet/my-wallet"] = """{"data":{"balance":7,"credits":7,"ledger":[]}}"""
        api.responses["/api/v1/wallet/money-wallet*"] = """{"data":{"currency":"VES","available_minor":1845000,"reserved_for_services_minor":0,"withdrawal_pending_minor":0}}"""
        api.responses["/api/v1/wallet/get-vinko-exchange-rates"] = """{"data":{"VES":{"currency":"VES","rate_minor":95000},"USD":{"currency":"USD","rate_minor":100}}}"""
        val app = ApplicationProvider.getApplicationContext<SoyVinkoApp>()
        app.configure(api.server.url("/").toString())
        app.api.accessToken = "test-token"
        compose.setContent { SoyVinkoTheme { MainShell(app.services, TestUser, SessionActions({}, {}), onUserUpdated = {}) } }

        idleUntil("María")
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/shell_1_inicio.png")
        listOf("Explorar" to "2_explorar", "Servicios" to "3_servicios", "Wallet" to "4_wallet", "Perfil" to "5_perfil").forEach { (tab, name) ->
            compose.onAllNodesWithText(tab).fetchSemanticsNodes().let { }
            compose.onAllNodes(hasText(tab)).let { nodes -> nodes[nodes.fetchSemanticsNodes().size - 1].performClick() }
            compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); true }
            Thread.sleep(300)
            shadowOf(Looper.getMainLooper()).idle()
            compose.waitForIdle()
            check(compose.onAllNodes(hasText("En construcción")).fetchSemanticsNodes().isEmpty()) { "$tab sigue en construcción" }
            compose.onRoot().captureRoboImage("build/outputs/roborazzi/shell_$name.png")
        }
    }
}
