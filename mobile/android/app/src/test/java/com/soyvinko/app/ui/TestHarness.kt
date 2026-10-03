package com.soyvinko.app.ui

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.soyvinko.app.SoyVinkoApp
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSession
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.SessionActions
import com.soyvinko.app.ui.theme.SoyVinkoTheme
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

val TestUser = User(
    id = "u1", email = "maria.gonzalez@example.com", name = "María González",
    emailVerified = true, roles = listOf("customer", "professional"),
)

/**
 * Servidor simulado por ruta: responses["/api/v1/wallet/money-wallet?currency=VES"] o responses["/api/v1/…"] = """{"data": {...}}""".
 * Las rutas sin respuesta devuelven {"data": null}. `requests` guarda lo recibido para comprobarlo.
 */
class FakeApi {
    val server = MockWebServer()
    val responses = mutableMapOf<String, String>()
    val requests = mutableListOf<RecordedRequest>()

    init {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val full = request.path!!
                val path = full.substringBefore('?')
                // Primero la ruta con parámetros exactos ("/x?currency=VES"), luego la ruta sola, luego prefijos "*".
                val body = responses[full] ?: responses[path]
                    ?: responses.entries.firstOrNull { (k, _) -> k.endsWith("*") && full.startsWith(k.dropLast(1)) }?.value
                return MockResponse().setHeader("Content-Type", "application/json").setBody(body ?: """{"data":null}""")
            }
        }
        server.start()
    }

    fun shutdown() = server.shutdown()
}

/** Monta una pantalla con la app configurada contra el servidor simulado y los proveedores del shell. */
fun ComposeContentTestRule.setScreen(api: FakeApi, user: User = TestUser, content: @Composable (NavHostController) -> Unit) {
    resetFileProviderCache()
    val app = ApplicationProvider.getApplicationContext<SoyVinkoApp>()
    app.configure(api.server.url("/").toString())
    app.api.accessToken = "test-token"
    setContent {
        val snackbar = remember { SnackbarHostState() }
        val nav = rememberNavController()
        SoyVinkoTheme {
            CompositionLocalProvider(
                LocalApp provides app.services,
                LocalUser provides user,
                LocalSnackbar provides snackbar,
                LocalSession provides SessionActions({}, {}),
            ) {
                content(nav)
                SnackbarHost(snackbar)
            }
        }
    }
}

/**
 * FileProvider guarda sus rutas en caché estática y cada prueba de Robolectric usa otro directorio
 * de datos: sin limpiarla, la cámara falla en pruebas posteriores. Solo afecta a las pruebas.
 */
fun resetFileProviderCache() {
    runCatching {
        val field = androidx.core.content.FileProvider::class.java.getDeclaredField("sCache")
        field.isAccessible = true
        (field.get(null) as MutableMap<*, *>).clear()
    }
}
