package com.soyvinko.app.ui.feature.explore

import android.os.Looper
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.NavHost
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.soyvinko.app.SoyVinkoApp
import com.soyvinko.app.ui.FakeApi
import com.soyvinko.app.ui.common.GeoPos
import com.soyvinko.app.ui.setScreen
import com.soyvinko.app.ui.shell.Routes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.soyvinko.app.ui.feature.explore.ExploreFixtures as F

/** Capturas de Explorar y Solicitudes con datos ficticios (app/build/outputs/roborazzi/explore_*.png). */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = SoyVinkoApp::class)
class ExploreScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val api = FakeApi()
    private val caracas = GeoPos(10.4961, -66.8836)

    @Before
    fun setUp() {
        imageCrossfade = false
        F.serveImages(api)
        api.responses["/api/v1/public/catalog"] = F.catalog
        api.responses["/api/v1/account/my-account"] = F.account
        api.responses["/api/v1/public/locate-parish"] = F.parish
        api.responses["/api/v1/public/nearby-professionals"] = F.nearby
        api.responses["/api/v1/public/discover"] = F.discoverRemote
        api.responses["/api/v1/public/professional-map-markers"] = F.markers
        api.responses["/api/v1/professionals/professional-profile"] = F.profile
        api.responses["/api/v1/professionals/professional-reviews"] = F.reviews
        api.responses["/api/v1/professionals/my-favorites"] = F.favorites
        api.responses["/api/v1/requests/my-requests"] = F.requests
        api.responses["/api/v1/requests/request-proposals"] = F.proposals
    }

    @After fun tearDown() = api.shutdown()

    private fun waitForCall(path: String, timeout: Long = 10_000) {
        compose.waitUntil(timeout) {
            shadowOf(Looper.getMainLooper()).idle()
            api.requests.any { it.path!!.substringBefore('?') == path }
        }
        settle(500)
    }

    private fun waitFor(text: String, timeout: Long = 20_000) {
        compose.waitUntil(timeout) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodes(hasText(text, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Da tiempo a que Coil descargue y decodifique las imágenes servidas por el servidor simulado. */
    private fun settle(ms: Long = 3000) {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            // idleFor también avanza el SystemClock de Robolectric, que usa la transición (crossfade) de Coil.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            compose.mainClock.advanceTimeBy(50)
            Thread.sleep(25)
        }
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        settle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/explore_$name.png")
    }

    /** La lista principal de la pantalla (la primera con desplazamiento; las filas horizontales van dentro). */
    private fun scroller() = compose.onAllNodes(hasScrollAction())[0]

    private fun body(path: String) = Json.parseToJsonElement(api.requests.last { it.path!!.substringBefore('?') == path }.body.clone().readUtf8()).jsonObject

    @Test
    fun exploreNearbyList() {
        compose.setScreen(api) { nav -> ExploreScreen(nav, initialLocation = caracas, autoLocate = false) }
        waitFor("Mateo Rodríguez")
        shot("01_explorar")
        scroller().performScrollToIndex(4)
        shot("02_explorar_resultados")
        // Búsqueda cercana con GPS: misma llamada que la web.
        val nearby = body("/api/v1/public/nearby-professionals")
        assertEquals(10.4961, nearby["lat"]!!.jsonPrimitive.content.toDouble(), 0.0001)
        assertEquals("10000", nearby["radius_m"]!!.jsonPrimitive.content)
        assertEquals("100", nearby["limit"]!!.jsonPrimitive.content)
        // Con modo "Todos" se suman los remotos de discover.
        scroller().performScrollToNode(hasText("Andrea Salazar"))
        compose.onNodeWithText("Andrea Salazar").assertExists()
    }

    @Test
    fun exploreWithoutLocation() {
        api.responses["/api/v1/public/discover"] = F.discoverAll
        compose.setScreen(api) { nav -> ExploreScreen(nav, autoLocate = false) }
        waitForCall("/api/v1/public/discover")
        scroller().performScrollToIndex(3)
        waitFor("profesionales disponibles")
        shot("03_explorar_sin_ubicacion")
    }

    @Test
    fun exploreMap() {
        compose.setScreen(api) { nav -> ExploreScreen(nav, initialLocation = caracas, autoLocate = false, startWithMap = true) }
        waitFor("profesionales en el mapa")
        shot("04_explorar_mapa")
        val markers = body("/api/v1/public/professional-map-markers")
        assertEquals(3, markers["professionals"]!!.jsonArray.size)
    }

    @Test
    fun exploreFilters() {
        compose.setScreen(api) { nav -> ExploreScreen(nav, initialLocation = caracas, autoLocate = false, startWithFilters = true) }
        waitFor("Ver resultados")
        waitFor("Oficina")
        settle()
        captureScreenRoboImage("build/outputs/roborazzi/explore_05_filtros.png")
    }

    @Test
    fun professionalProfile() {
        compose.setScreen(api) { nav -> ProfessionalScreen(nav, F.MATEO) }
        waitFor("Conoce mi trabajo")
        waitForCall("/api/v1/professionals/professional-reviews")
        shot("06_perfil")
        scroller().performScrollToNode(hasText("Trabajos realizados"))
        scroller().performScrollToNode(hasText("Opiniones y recomendaciones"))
        shot("07_perfil_opiniones")
    }

    @Test
    fun favorites() {
        compose.setScreen(api) { nav -> FavoritesScreen(nav) }
        waitFor("Andrea Salazar")
        shot("08_favoritos")
    }

    @Test
    fun favoritesEmpty() {
        api.responses["/api/v1/professionals/my-favorites"] = """{"data":[]}"""
        compose.setScreen(api) { nav -> FavoritesScreen(nav) }
        waitFor("Aún no guardas profesionales")
        shot("09_favoritos_vacio")
    }

    @Test
    fun newOpenRequest() {
        compose.setScreen(api) { nav -> NewRequestScreen(nav, "", "") }
        waitFor("¿Qué necesitas resolver?")
        shot("10_nueva_solicitud")
        scroller().performScrollToNode(hasText("Publicar solicitud"))
        shot("11_nueva_solicitud_final")
    }

    @Test
    fun newDirectRequest() {
        compose.setScreen(api) { nav -> NewRequestScreen(nav, F.MATEO, F.MATEO_FUGAS) }
        waitFor("Reparación de fugas y grifería")
        shot("12_solicitud_directa")
    }

    @Test
    fun newDirectRequestPickService() {
        compose.setScreen(api) { nav -> NewRequestScreen(nav, F.MATEO, "") }
        waitFor("Elige el servicio")
        shot("13_solicitud_elegir_servicio")
    }

    @Test
    fun newRequestWithoutAddresses() {
        api.responses["/api/v1/account/my-account"] = F.accountNoAddresses
        compose.setScreen(api) { nav -> NewRequestScreen(nav, "", "") }
        waitFor("Aún no tienes direcciones guardadas", 10_000)
        scroller().performScrollToNode(hasText("Agregar una dirección"))
        shot("14_solicitud_sin_direcciones")
    }

    @Test
    fun requestsList() {
        compose.setScreen(api) { nav -> RequestsScreen(nav) }
        waitFor("Reparar fuga en el baño principal")
        shot("15_mis_solicitudes")
    }

    @Test
    fun requestDetail() {
        compose.setScreen(api) { nav -> RequestDetailScreen(nav, F.REQ_OPEN) }
        waitFor("Lo que necesitas")
        waitForCall("/api/v1/requests/request-proposals")
        shot("16_solicitud_detalle")
        scroller().performScrollToNode(hasText("Carlos Méndez"))
        shot("17_solicitud_propuestas")
    }

    @Test
    fun requestDetailMatched() {
        api.responses["/api/v1/requests/request-proposals"] = """{"data":[]}"""
        compose.setScreen(api) { nav -> RequestDetailScreen(nav, F.REQ_MATCHED) }
        waitFor("Ver servicio contratado")
        shot("18_solicitud_contratada")
    }

    /** Envía una solicitud remota abierta y comprueba el cuerpo exacto que recibe create_customer_request. */
    @Test
    fun submitOpenRemoteRequest() {
        val newId = "4d5e6f70-8192-4a3b-8c4d-5e6f708192a3"
        api.responses["/api/v1/requests/create-customer-request"] = """{"data":"$newId"}"""
        api.responses["/api/v1/requests/my-requests"] = F.requests.replace(F.REQ_REMOTE, newId)
        compose.setScreen(api) { nav ->
            NavHost(nav, startDestination = Routes.NEW_REQUEST) { exploreGraph(nav) }
        }
        waitFor("¿Qué necesitas resolver?")
        compose.onNodeWithText("Remoto").performClick()
        compose.onNodeWithText("Categoría").performClick()
        waitFor("Diseño gráfico")
        compose.onAllNodesWithText("Diseño gráfico")[0].performClick()
        compose.onNodeWithText("Título de tu solicitud").performTextInput("Logo para mi panadería")
        compose.onNodeWithText("Requisitos y resultado esperado").performTextInput("Necesito un logo y una paleta de colores para redes.")
        compose.onNodeWithText("USD · Dólar").performClick()
        compose.onNodeWithText("Presupuesto ofrecido en USD").performScrollTo().performTextInput("1.500,50")
        compose.onNodeWithText("Publicar solicitud").performScrollTo().performClick()
        waitFor("Cotización del profesional")
        val sent = body("/api/v1/requests/create-customer-request")
        assertEquals(F.DISENO, sent["category"]!!.jsonPrimitive.content)
        assertEquals("remote", sent["delivery_mode"]!!.jsonPrimitive.content)
        assertEquals("Logo para mi panadería", sent["title"]!!.jsonPrimitive.content)
        assertEquals("USD", sent["currency"]!!.jsonPrimitive.content)
        assertEquals("150050", sent["budget"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, sent["address"])
        assertEquals(JsonNull, sent["radius_m"])
        assertEquals("true", sent["protect"]!!.jsonPrimitive.content)
        assertEquals(0, sent["files"]!!.jsonArray.size)
        assertTrue(sent["key"]!!.jsonPrimitive.content.length == 36)
        assertTrue(sent.containsKey("expires_at"))
    }

    /** Solicitud directa a un servicio: request_offering con la dirección predeterminada. */
    @Test
    fun submitDirectRequest() {
        api.responses["/api/v1/requests/request-offering"] = """{"data":"${F.REQ_OPEN}"}"""
        compose.setScreen(api) { nav ->
            NavHost(nav, startDestination = Routes.NEW_REQUEST) { exploreGraph(nav) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                nav.navigate(Routes.newRequest(F.MATEO, F.MATEO_FUGAS)) { popUpTo(Routes.NEW_REQUEST) { inclusive = true } }
            }
        }
        waitFor("Cuéntale qué necesitas")
        compose.onNodeWithText("Explica la falla o lo que necesitas").performTextInput("Gotea la llave del lavaplatos desde ayer.")
        compose.onNodeWithText("Enviar solicitud").performScrollTo().performClick()
        waitFor("Propuestas recibidas")
        val sent = body("/api/v1/requests/request-offering")
        assertEquals(F.MATEO_FUGAS, sent["offering"]!!.jsonPrimitive.content)
        assertEquals(F.CASA, sent["address"]!!.jsonPrimitive.content)
        assertEquals("USD", sent["currency"]!!.jsonPrimitive.content)
        assertEquals("false", sent["protect"]!!.jsonPrimitive.content)
        assertTrue(!sent.containsKey("title") && !sent.containsKey("budget"))
    }

    @Test
    fun parsesVenezuelanAmounts() {
        assertEquals(150050L, parseMajorToMinor("1.500,50"))
        assertEquals(150000L, parseMajorToMinor("1.500"))
        assertEquals(1550L, parseMajorToMinor("15.5"))
        assertEquals(3500L, parseMajorToMinor("35"))
        assertEquals(null, parseMajorToMinor("abc"))
        assertEquals(null, parseMajorToMinor(""))
    }

}
