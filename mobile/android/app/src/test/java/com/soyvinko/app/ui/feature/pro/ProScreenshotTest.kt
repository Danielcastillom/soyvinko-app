package com.soyvinko.app.ui.feature.pro

import android.os.Looper
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.NavHost
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.soyvinko.app.SoyVinkoApp
import com.soyvinko.app.ui.FakeApi
import com.soyvinko.app.ui.TestUser
import com.soyvinko.app.ui.setScreen
import com.soyvinko.app.ui.shell.Routes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID

/** Capturas de las pantallas reales del panel profesional y del marketplace con datos ficticios. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = SoyVinkoApp::class)
@OptIn(ExperimentalRoborazziApi::class)
class ProScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val api = FakeApi()

    @Before fun setUp() {
        ProFakeData.install(api)
        MarketCart.clear()
    }

    @After fun tearDown() {
        MarketCart.clear()
        api.shutdown()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun waitText(text: String) = compose.waitUntil(10_000) {
        idle()
        compose.onAllNodes(hasText(text, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun shot(name: String) {
        idle()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/pro_$name.png")
    }

    /** Captura todas las ventanas (hojas inferiores y diálogos incluidos). */
    private fun screenShot(name: String) {
        idle()
        compose.waitForIdle()
        captureScreenRoboImage("build/outputs/roborazzi/pro_$name.png")
    }

    private fun body(pathPrefix: String) =
        Json.parseToJsonElement(api.requests.first { it.path!!.startsWith(pathPrefix) }.body.readUtf8()).jsonObject

    private fun waitRequest(pathPrefix: String) = compose.waitUntil(10_000) {
        idle()
        api.requests.any { it.path!!.startsWith(pathPrefix) }
    }

    /** Foto ficticia (degradado con formas) para que las tarjetas muestren imágenes reales. */
    private fun photo(seed: Int): ByteArray {
        val palettes = listOf(
            0xFF6F3DFF.toInt() to 0xFFC6B2FF.toInt(), 0xFF0C8E72.toInt() to 0xFF9FE5D3.toInt(),
            0xFF0284C7.toInt() to 0xFFAEDCF5.toInt(), 0xFFD97706.toInt() to 0xFFFDE2A7.toInt(),
            0xFFD13B59.toInt() to 0xFFF9C2CD.toInt(),
        )
        val (a, b) = palettes[Math.floorMod(seed, palettes.size)]
        val bmp = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, 320f, 320f, a, b, Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, 320f, 320f, paint)
        paint.shader = null
        paint.color = 0xCCFFFFFF.toInt()
        canvas.drawRoundRect(RectF(70f, 120f, 250f, 250f), 24f, 24f, paint)
        paint.color = 0x99FFFFFF.toInt()
        canvas.drawCircle(225f, 90f, 42f, paint)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    /** El servidor simulado responde /api/v1/files/{id} con PNG reales. */
    private fun serveImages() {
        val images = ProFakeData.imageIds.associateWith { photo(it.hashCode()) }
        val original = api.server.dispatcher
        api.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path!!.substringBefore('?')
                val bytes = images[path.removePrefix("/api/v1/files/")]
                if (path.startsWith("/api/v1/files/") && bytes != null) {
                    api.requests += request
                    return MockResponse().setHeader("Content-Type", "image/png").setBody(Buffer().write(bytes))
                }
                return original.dispatch(request)
            }
        }
    }

    /** Espera a que `count` fotos estén decodificadas en pantalla (ProImage marca el nodo al cargar). */
    private fun waitImages(count: Int) {
        compose.waitUntil(15_000) {
            idle()
            Thread.sleep(20)
            compose.onAllNodes(hasTestTag(IMAGE_LOADED_TAG), useUnmergedTree = true).fetchSemanticsNodes().size >= count
        }
        // El fundido de Coil usa SystemClock, que Robolectric solo avanza a demanda.
        repeat(8) {
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(100))
            compose.mainClock.advanceTimeBy(100)
        }
        compose.waitForIdle()
    }

    private fun addToCart(id: String, qty: Int) {
        val all = Json.parseToJsonElement(ProFakeData.marketplace).jsonArray.map { it.jsonObject }
        MarketCart.add(all.first { it["id"]!!.jsonPrimitive.content == id }, "Ferretería El Valle", qty)
    }

    @Test fun activation() {
        compose.setScreen(api, user = TestUser.copy(roles = listOf("customer"))) { nav -> ProScreen(nav) }
        waitText("Activar perfil profesional")
        shot("01_activacion")
    }

    @Test fun panel() {
        compose.setScreen(api) { nav -> ProScreen(nav) }
        waitText("Gestiona tu actividad")
        waitText("Vinkos")
        shot("02_panel")
        waitText("Excelente trabajo")
        compose.onNodeWithText("Buen servicio", substring = true).performScrollTo()
        shot("03_panel_resenas")
    }

    @Test fun setupServices() {
        compose.setScreen(api) { nav -> ProSetupScreen(nav, initialTab = 1) }
        waitText("Instalación y reparación eléctrica")
        waitText("ELECTRICIDAD")
        shot("04_setup_servicios")
    }

    @Test fun setupDeleteServiceConfirmation() {
        compose.setScreen(api) { nav -> ProSetupScreen(nav, initialTab = 1) }
        waitText("Asesoría remota de domótica")
        compose.onNodeWithContentDescription("Eliminar Asesoría remota de domótica").performScrollTo().performClick()
        waitText("Eliminar servicio")
        screenShot("19_eliminar_servicio")
    }

    @Test fun removeOfferingSendsBodyAndShowsArchivedMessage() {
        api.responses["/api/v1/professionals/remove-offering"] = """{"data":"archived"}"""
        compose.setScreen(api) { nav -> ProSetupScreen(nav, initialTab = 1) }
        waitText("Asesoría remota de domótica")
        compose.onNodeWithContentDescription("Eliminar Asesoría remota de domótica").performScrollTo().performClick()
        waitText("Eliminar servicio")
        compose.onNode(hasText("Eliminar servicio") and hasClickAction()).performClick()
        waitRequest("/api/v1/professionals/remove-offering")
        val json = body("/api/v1/professionals/remove-offering")
        assertEquals(setOf("offering"), json.keys)
        assertEquals("o3", json["offering"]!!.jsonPrimitive.content)
        waitText("Servicio archivado: se conserva en tus solicitudes anteriores.")
        // La lista se recarga tras eliminar.
        compose.waitUntil(10_000) {
            idle()
            api.requests.count { it.path!!.startsWith("/api/v1/professionals/my-professional") } >= 2
        }
    }

    @Test fun setupBranchDeleteWarnsAboutCoverageAndHours() {
        api.responses["/api/v1/professionals/remove-branch"] = """{"data":null}"""
        compose.setScreen(api) { nav -> ProSetupScreen(nav, initialTab = 2) }
        waitText("Taller Chacao")
        compose.onNodeWithContentDescription("Eliminar sede Taller Chacao").performScrollTo().performClick()
        waitText("2 zonas de cobertura y 6 horarios")
        screenShot("20_eliminar_sede")
        compose.onNode(hasText("Eliminar sede") and hasClickAction()).performClick()
        waitRequest("/api/v1/professionals/remove-branch")
        assertEquals("b1", body("/api/v1/professionals/remove-branch")["branch"]!!.jsonPrimitive.content)
        waitText("Sede eliminada.")
    }

    @Test fun setupPortfolio() {
        serveImages()
        compose.setScreen(api) { nav -> ProSetupScreen(nav, initialTab = 3) }
        waitText("Tablero trifásico")
        waitImages(2)
        shot("21_setup_portafolio")
    }

    @Test fun setupBranches() {
        compose.setScreen(api) { nav -> ProSetupScreen(nav, initialTab = 2) }
        waitText("Taller Chacao")
        shot("05_setup_sedes")
    }

    @Test fun setupProfileAndContacts() {
        compose.setScreen(api) { nav -> ProSetupScreen(nav, initialTab = 4) }
        waitText("WhatsApp de atención")
        shot("06_setup_contactos")
    }

    @Test fun opportunities() {
        compose.setScreen(api) { nav -> OpportunitiesScreen(nav) }
        waitText("Falla eléctrica en apartamento")
        waitText("Propuesta enviada")
        shot("07_oportunidades")
    }

    @Test fun proposalForm() {
        compose.setScreen(api) { nav -> OpportunityDetailScreen("rq1", "onsite", nav) }
        waitText("Lo que necesita el cliente")
        shot("08_oportunidad_detalle")
        compose.onNodeWithText("Tu precio (USD)").performScrollTo().performTextInput("55,00")
        compose.onNodeWithText("Mensaje para el cliente").performTextInput(
            "Reviso el tablero y el circuito del aire. Llevo breakers de repuesto; garantía de 30 días.",
        )
        compose.onNode(hasText("Enviar propuesta") and hasClickAction()).performScrollTo()
        shot("09_propuesta_formulario")
    }

    @Test fun proposalSendsMinorUnitsAndStableExpiry() {
        api.responses["/api/v1/requests/submit-proposal"] = """{"data":"prop-1"}"""
        compose.setScreen(api) { nav -> OpportunityDetailScreen("rq1", "onsite", nav) }
        waitText("Lo que necesita el cliente")
        compose.onNodeWithText("Tu precio (USD)").performScrollTo().performTextInput("1.234,5")
        compose.onNodeWithText("Mensaje para el cliente").performTextInput("Puedo atenderlo mañana temprano.")
        compose.onNode(hasText("Enviar propuesta") and hasClickAction()).performScrollTo().performClick()
        compose.waitUntil(10_000) {
            idle()
            api.requests.any { it.path!!.startsWith("/api/v1/requests/submit-proposal") }
        }
        val body = api.requests.first { it.path!!.startsWith("/api/v1/requests/submit-proposal") }.body.readUtf8()
        val json = Json.parseToJsonElement(body).jsonObject
        assertEquals("rq1", json["request"]!!.jsonPrimitive.content)
        assertEquals(123450L, json["price_minor"]!!.jsonPrimitive.long)
        val expires = Instant.parse(json["expires_at"]!!.jsonPrimitive.content)
        assertTrue("La propuesta vence en el futuro", expires.isAfter(Instant.now()))
    }

    @Test fun myProposals() {
        compose.setScreen(api) { nav -> ProposalsScreen(nav) }
        waitText("Instalación de lámparas LED")
        shot("10_mis_propuestas")
    }

    @Test fun marketplace() {
        serveImages()
        compose.setScreen(api) { nav -> MarketplaceScreen(nav) }
        waitText("Compra directo a comercios")
        shot("11_marketplace")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(3)
        waitText("Kit de limpieza")
        waitImages(2)
        shot("12_marketplace_productos")
    }

    @Test fun store() {
        serveImages()
        addToCart("pr1", 1)
        compose.setScreen(api) { nav -> StoreScreen("s1", nav) }
        waitText("Llave ajustable")
        waitText("Ver perfil del vendedor")
        waitImages(2)
        shot("13_tienda")
        // Ya no se consulta el catálogo público para completar nombre y fotos.
        assertTrue(api.requests.none { it.path!!.startsWith("/api/v1/public/marketplace") })
    }

    @Test fun cart() {
        addToCart("pr1", 1)
        addToCart("pr5", 2)
        compose.setScreen(api) { nav -> CartScreen(nav) }
        waitText("Confirmar pedido")
        shot("14_carrito")
    }

    @Test fun checkoutSendsOrderWithIdempotencyKey() {
        api.responses["/api/v1/marketplace/create-order"] = """{"data":"a1b2c3d4-0000-4000-8000-000000000009"}"""
        addToCart("pr1", 1)
        addToCart("pr5", 2)
        val key = MarketCart.checkoutKey
        compose.setScreen(api) { nav -> NavHost(nav, startDestination = CART) { proGraph(nav) } }
        waitText("Confirmar pedido")
        compose.onNode(hasText("Confirmar pedido", substring = true) and hasClickAction()).performClick()
        compose.waitUntil(10_000) {
            idle()
            api.requests.any { it.path!!.startsWith("/api/v1/marketplace/create-order") }
        }
        val body = api.requests.first { it.path!!.startsWith("/api/v1/marketplace/create-order") }.body.readUtf8()
        val json = Json.parseToJsonElement(body).jsonObject
        assertEquals("s1", json["store"]!!.jsonPrimitive.content)
        assertEquals(key, json["key"]!!.jsonPrimitive.content)
        UUID.fromString(json["key"]!!.jsonPrimitive.content)
        val items = json["items"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("pr1" to 1L, "pr5" to 2L), items.map { it["product_id"]!!.jsonPrimitive.content to it["quantity"]!!.jsonPrimitive.long })
        // Tras confirmar: carrito vacío y navegación a Mis pedidos.
        compose.waitUntil(10_000) {
            idle()
            MarketCart.count == 0 && compose.onAllNodes(hasText("Mis pedidos"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun orders() {
        compose.setScreen(api) { nav -> OrdersScreen(nav) }
        waitText("Pedido #A1B2C3D4")
        shot("15_pedidos")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
        waitText("Aceptar pedido")
        shot("16_pedidos_venta")
    }

    @Test fun myStore() {
        serveImages()
        api.responses["/api/v1/professionals/my-professional"] = ProFakeData.data(ProFakeData.professional(ProFakeData.store))
        compose.setScreen(api) { nav -> MyStoreScreen(nav) }
        waitText("Bombillo LED 12 W")
        waitImages(2)
        shot("17_mi_tienda")
        assertTrue(api.requests.none { it.path!!.startsWith("/api/v1/public/marketplace") })
    }

    @Test fun productEditorPhotosWithPendingBadgeAndRemoval() {
        serveImages()
        api.responses["/api/v1/professionals/my-professional"] = ProFakeData.data(ProFakeData.professional(ProFakeData.store))
        api.responses["/api/v1/marketplace/remove-product-image"] = """{"data":null}"""
        compose.setScreen(api) { nav -> MyStoreScreen(nav) }
        waitText("Bombillo LED 12 W")
        compose.onAllNodes(hasText("Editar") and hasClickAction())[0].performClick()
        waitText("Editar producto")
        waitImages(4)
        screenShot("22_producto_fotos")
        compose.onNodeWithContentDescription("Quitar foto 2").performClick()
        waitText("¿Quitar esta foto del producto?")
        screenShot("23_quitar_foto")
        compose.onNode(hasText("Quitar foto") and hasClickAction()).performClick()
        waitRequest("/api/v1/marketplace/remove-product-image")
        val json = body("/api/v1/marketplace/remove-product-image")
        assertEquals("mp1", json["product"]!!.jsonPrimitive.content)
        assertEquals("img-mp1b", json["file"]!!.jsonPrimitive.content)
        waitText("Foto quitada.")
    }

    @Test fun myStoreActivation() {
        compose.setScreen(api) { nav -> MyStoreScreen(nav) }
        waitText("Abre tu tienda en SoyVinko")
        shot("18_mi_tienda_activar")
    }

    @Test fun graphRegistersAllRoutes() {
        val routes = listOf(
            Routes.PRO, Routes.OPPORTUNITIES, Routes.MY_PROPOSALS, Routes.PRO_SETUP,
            Routes.MARKETPLACE, Routes.STORE, Routes.ORDERS, Routes.MY_STORE, OPPORTUNITY, BRANCH_NEW, CART,
        )
        var registered = emptySet<String?>()
        compose.setScreen(api) { nav ->
            NavHost(nav, startDestination = Routes.MARKETPLACE) { proGraph(nav) }
            registered = nav.graph.map { it.route }.toSet()
        }
        compose.waitUntil(5_000) { idle(); registered.isNotEmpty() }
        routes.forEach { assertTrue("Falta la ruta $it", it in registered) }
    }
}
