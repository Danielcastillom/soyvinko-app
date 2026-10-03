package com.soyvinko.app.ui

import androidx.test.core.app.ApplicationProvider
import com.soyvinko.app.platform.AndroidPlatform
import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.soyvinko.app.data.ApiClient
import com.soyvinko.app.data.AuthRepository
import com.soyvinko.app.data.TokenStorage
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.screens.ForgotPasswordScreen
import com.soyvinko.app.ui.screens.LoginScreen
import com.soyvinko.app.ui.screens.RegisterScreen
import com.soyvinko.app.ui.screens.WelcomeScreen
import com.soyvinko.app.ui.theme.SoyVinkoTheme
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.Shadows.shadowOf

private class NoStorage : TokenStorage {
    override var refreshToken: String? = null
    override var cachedUserJson: String? = null
    override fun clear() = Unit
}

/** Genera capturas de las pantallas reales en app/build/outputs/roborazzi (datos ficticios). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = android.app.Application::class)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val server = MockWebServer()
    private lateinit var auth: AuthRepository
    private val userJson = """{"id":"u1","email":"maria.gonzalez@example.com","name":"María González","email_verified":false,"account_type":"natural","roles":["customer","professional"],"permissions":[]}"""
    private val user = User("u1", "maria.gonzalez@example.com", "María González", roles = listOf("customer", "professional"))

    @Before
    fun setUp() {
        // Igual que SoyVinkoApp al arrancar: los recursos compartidos (logo) necesitan el contexto.
        AndroidPlatform.init(ApplicationProvider.getApplicationContext(), "test")
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = when (request.path?.substringBefore('?')) {
                "/api/v1/auth/login" -> MockResponse().setBody("""{"data":{"status":"code_required","message":"Te enviamos un código de 6 dígitos a tu correo."}}""")
                "/api/v1/auth/me" -> MockResponse().setBody("""{"data":$userJson}""")
                "/api/v1/account/dashboard" -> MockResponse().setBody("""{"data":{"requests":2,"active_contracts":3,"completed_contracts":8,"credit_balance":12,"unread_notifications":4}}""")
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
        auth = AuthRepository(ApiClient(server.url("/").toString(), NoStorage(), "test"), NoStorage())
    }

    @After
    fun tearDown() = server.shutdown()

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun welcome() {
        compose.setContent { SoyVinkoTheme { WelcomeScreen({}, {}, expired = false) } }
        shot("01_bienvenida")
    }

    @Test
    fun loginAndCode() {
        val vm = LoginViewModel(auth)
        compose.setContent { SoyVinkoTheme { LoginScreen(vm, {}, {}, {}, {}) } }
        shot("02_login")
        vm.email = "maria.gonzalez@example.com"
        vm.password = "contraseña-segura"
        vm.submitCredentials {}
        // Las corrutinas del ViewModel vuelven al hilo principal por su Looper, pausado en Robolectric.
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            vm.step == LoginViewModel.Step.Code
        }
        vm.code = "4821"
        shot("03_codigo")
    }

    @Test
    fun registerWithErrors() {
        val vm = RegisterViewModel(auth)
        compose.setContent { SoyVinkoTheme { RegisterScreen(vm, {}, {}, {}) } }
        shot("04_registro")
        vm.accountType = "juridico"
        vm.businessName = "Inversiones Ávila C.A."
        vm.rif = "J-1234"
        vm.email = "contacto@avila"
        vm.password = "corta"
        vm.submit {}
        shot("05_registro_empresa_errores")
    }

    @Test
    fun forgot() {
        compose.setContent { SoyVinkoTheme { ForgotPasswordScreen(ForgotViewModel(auth), {}) } }
        shot("06_recuperar")
    }

}
