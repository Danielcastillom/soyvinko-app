package com.soyvinko.app.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BiometricAuthTest {
    private lateinit var server: MockWebServer
    private lateinit var storage: MemoryTokenStorage
    private lateinit var api: ApiClient
    private lateinit var auth: AuthRepository

    private val userJson = """{"id":"u1","email":"carlos@example.com","name":"Carlos Pérez","email_verified":true,"account_type":"natural","roles":["customer"],"permissions":[]}"""

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        storage = MemoryTokenStorage()
        api = ApiClient(server.url("/").toString(), storage, "test-agent")
        auth = AuthRepository(api, storage)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun enableAndDisableBiometricsStoresSettings() {
        assertFalse(auth.isBiometricEnabled())
        assertNull(auth.biometricEmail())

        storage.refreshToken = "valid-refresh-token"
        auth.enableBiometric("carlos@example.com", lockOnLeave = true)

        assertTrue(auth.isBiometricEnabled())
        assertEquals("carlos@example.com", auth.biometricEmail())
        assertEquals("valid-refresh-token", storage.biometricRefreshToken)
        assertTrue(storage.biometricLockOnLeave)

        auth.disableBiometric()
        assertFalse(auth.isBiometricEnabled())
        assertNull(storage.biometricRefreshToken)
    }

    @Test
    fun logoutKeepsBiometricsConfiguredByDefault() = runTest {
        storage.refreshToken = "session-token"
        auth.enableBiometric("carlos@example.com")

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok"}"""))
        auth.logout()

        // Normal session cleared
        assertNull(storage.refreshToken)
        assertNull(storage.cachedUserJson)

        // Biometrics remain configured for quick login on return
        assertTrue(auth.isBiometricEnabled())
        assertEquals("carlos@example.com", auth.biometricEmail())
        assertEquals("session-token", storage.biometricRefreshToken)
    }

    @Test
    fun loginWithBiometricsRestoresSessionAndFetchesUser() = runTest {
        storage.biometricEnabled = true
        storage.biometricEmail = "carlos@example.com"
        storage.biometricRefreshToken = "saved-bio-refresh-token"

        // Mock refresh endpoint returning new access and refresh tokens
        server.enqueue(
            MockResponse().setBody(
                """{"data":{"token":"new-access-token","refresh_token":"rotated-refresh-token"}}""",
            ),
        )
        // Mock me endpoint returning user data
        server.enqueue(
            MockResponse().setBody(
                """{"data":$userJson}""",
            ),
        )

        val restoredUser = auth.loginWithBiometrics()
        assertNotNull(restoredUser)
        assertEquals("carlos@example.com", restoredUser?.email)
        assertEquals("rotated-refresh-token", storage.refreshToken)
        assertEquals("rotated-refresh-token", storage.biometricRefreshToken)
    }
}
