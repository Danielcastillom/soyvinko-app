package com.soyvinko.app.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

private class MemoryStorage : TokenStorage {
    override var refreshToken: String? = null
    override var cachedUserJson: String? = null
    override fun clear() {
        refreshToken = null
        cachedUserJson = null
    }
}

class ApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var storage: MemoryStorage
    private lateinit var api: ApiClient
    private lateinit var auth: AuthRepository

    private val user = """{"id":"u1","email":"maria@example.com","name":"María González","email_verified":true,"account_type":"natural","roles":["customer"],"permissions":[]}"""

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        storage = MemoryStorage()
        api = ApiClient(server.url("/").toString(), storage, "test-agent")
        auth = AuthRepository(api, storage)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun loginAsksForCodeThenStoresNativeSession() = runTest {
        server.enqueue(MockResponse().setBody("""{"data":{"status":"code_required","email":"maria@example.com","message":"Código enviado"}}"""))
        val first = auth.login("maria@example.com", "secret-password")
        assertTrue(first is LoginOutcome.CodeRequired)
        val body1 = server.takeRequest().body.readUtf8()
        assertTrue(body1.contains("\"client\":\"native\""))

        server.enqueue(MockResponse().setBody("""{"data":{"access_token":"a1","refresh_token":"r1","expires_in":900,"user":$user}}"""))
        val second = auth.login("maria@example.com", "secret-password", "123456")
        assertTrue(second is LoginOutcome.LoggedIn)
        assertTrue(server.takeRequest().body.readUtf8().contains("\"code\":\"123456\""))
        assertEquals("r1", storage.refreshToken)
        assertEquals("a1", api.accessToken)
    }

    @Test
    fun expiredAccessTokenIsRefreshedOnceAndRetried() = runTest {
        storage.refreshToken = "r1"
        api.accessToken = "old"
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"http_error","message":"Sesión no válida o vencida."}}"""))
        server.enqueue(MockResponse().setBody("""{"data":{"access_token":"new","refresh_token":"r2","user":$user}}"""))
        server.enqueue(MockResponse().setBody("""{"data":{"requests":2,"active_contracts":1,"completed_contracts":4,"credit_balance":7,"unread_notifications":3}}"""))

        val dashboard = auth.dashboard()

        assertEquals(1, dashboard.activeContracts)
        assertEquals("Bearer old", server.takeRequest().getHeader("Authorization"))
        val refresh = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", refresh.path)
        assertTrue(refresh.body.readUtf8().contains("\"refresh_token\":\"r1\""))
        assertEquals("Bearer new", server.takeRequest().getHeader("Authorization"))
        assertEquals("r2", storage.refreshToken)
    }

    @Test
    fun revokedSessionClearsStorageAndNotifies() = runTest {
        storage.refreshToken = "r1"
        api.accessToken = "old"
        var expired = false
        api.onSessionExpired = { expired = true }
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"http_error","message":"La sesión venció o fue revocada."}}"""))
        try {
            auth.dashboard()
            fail("Debía fallar")
        } catch (e: ApiException) {
            assertTrue(e.isUnauthorized)
        }
        assertTrue(expired)
        assertNull(storage.refreshToken)
    }

    @Test
    fun validationErrorsAreMappedToFields() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(422).setBody(
                """{"error":{"code":"validation_error","message":"Revisa los campos del formulario.","details":[{"field":"body.password","message":"Escribe al menos 12 caracteres."}]}}""",
            ),
        )
        try {
            auth.register(RegisterRequest("a@b.co", "short", "short", "Ana", "natural"))
            fail("Debía fallar")
        } catch (e: ApiException) {
            assertEquals("Escribe al menos 12 caracteres.", e.fieldErrors["password"])
            assertEquals(422, e.status)
        }
    }

    @Test
    fun htmlGatewayErrorBecomesFriendlyMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>Bad gateway</html>"))
        try {
            auth.forgotPassword("a@b.co")
            fail("Debía fallar")
        } catch (e: ApiException) {
            assertEquals(502, e.status)
            assertTrue(e.message.contains("no está disponible"))
        }
    }

    @Test
    fun queryValuesAreEncodedExactlyOnce() = runTest {
        server.enqueue(MockResponse().setBody("""{"data":[]}"""))
        api.raw("GET", "/api/v1/contracts/chat-messages", query = mapOf("before" to "2026-10-02T10:00:00+00:00", "q" to "a&b c"))
        val url = server.takeRequest().requestUrl!!
        assertEquals("2026-10-02T10:00:00+00:00", url.queryParameter("before"))
        assertEquals("a&b c", url.queryParameter("q"))
    }
}
