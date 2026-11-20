package com.soyvinko.app.ui

import android.os.Looper
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.soyvinko.app.SoyVinkoApp
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.ResourceView
import com.soyvinko.app.ui.common.l
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.rememberResource
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = SoyVinkoApp::class)
class HarnessTest {
    @get:Rule val compose = createComposeRule()
    private val api = FakeApi()

    @After fun tearDown() = api.shutdown()

    @Test
    fun opsReachFakeServerWithSession() {
        api.responses["/api/v1/account/dashboard"] = """{"data":{"active_contracts":7}}"""
        compose.setScreen(api) {
            val app = LocalApp.current
            val r = rememberResource { app.ops.call("dashboard") }
            ResourceView(r) { Text("activos=${it.obj().l("active_contracts")}") }
        }
        compose.waitUntil(5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodes(androidx.compose.ui.test.hasText("activos=7")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("activos=7").assertExists()
        assertTrue(api.requests.any { it.getHeader("Authorization") == "Bearer test-token" })
    }
}
