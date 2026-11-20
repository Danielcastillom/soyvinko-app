package com.soyvinko.app.ui.feature.admin

import android.os.Looper
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.navigation.compose.NavHost
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.soyvinko.app.SoyVinkoApp
import com.soyvinko.app.ui.FakeApi
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.setScreen
import com.soyvinko.app.ui.shell.Routes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneOffset
import com.soyvinko.app.ui.feature.admin.AdminFixtures as F

/** Capturas del panel de administración con datos ficticios (app/build/outputs/roborazzi/admin_*.png). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = SoyVinkoApp::class)
class AdminScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val api = FakeApi()

    @After fun tearDown() = api.shutdown()

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun waitFor(text: String, substring: Boolean = false) {
        compose.waitUntil(10_000) {
            idle()
            compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty()
        }
        idle()
    }

    private fun requests(): List<RecordedRequest> = runCatching { api.requests.toList() }.getOrDefault(emptyList())

    private fun waitRequest(path: String): RecordedRequest {
        compose.waitUntil(10_000) {
            idle()
            requests().any { it.path!!.startsWith(path) }
        }
        return requests().first { it.path!!.startsWith(path) }
    }

    private fun body(path: String): JsonObject = Json.parseToJsonElement(waitRequest(path).body.clone().readUtf8()).obj()!!

    private fun shot(name: String) {
        idle()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/admin_$name.png")
    }

    private fun scrollDown() {
        compose.onRoot().performTouchInput { swipeUp(startY = bottom * 0.85f, endY = top + bottom * 0.15f) }
        idle()
        compose.waitForIdle()
    }

    /** Escribe en el campo editable con esa etiqueta (evita confundirlo con textos de las tarjetas). */
    private fun input(label: String, text: String) =
        compose.onNode(hasSetTextAction() and hasText(label)).performTextInput(text)

    /** Desplaza la lista perezosa hasta el elemento (los que están fuera de pantalla no existen aún). */
    private fun scrollToItem(text: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        idle()
        compose.waitForIdle()
    }

    private fun resource(key: String, onKyc: (String, String) -> Unit = { _, _ -> }, onTicket: (String) -> Unit = {}) =
        compose.setScreen(api, F.superadmin) { AdminResourceScreen(key, onBack = {}, onOpenKyc = onKyc, onOpenTicket = onTicket) }

    // ------------------------------------------------------------------ inicio

    @Test
    fun home() {
        api.responses["/api/v1/admin/admin-overview"] = F.overview
        api.responses[F.list("users")] = F.users
        compose.setScreen(api, F.superadmin) { nav ->
            NavHost(nav, startDestination = Routes.ADMIN) { adminGraph(nav) }
        }
        waitFor("Servicios Contratados")
        waitFor("1.284")
        shot("home")
        scrollDown()
        shot("home_2")
        scrollDown()
        shot("home_3")
        // La rejilla navega a la sección (Routes.adminResource).
        compose.onNodeWithText("Usuarios").performScrollTo().performClick()
        waitFor("ana.morales@gmail.com")
        assertTrue(requests().any { it.path == F.list("users") })
    }

    @Test
    fun homeSupportOnly() {
        compose.setScreen(api, F.support) { AdminHomeScreen(onBack = {}, onOpen = {}, onCategories = {}) }
        waitFor("Soporte")
        shot("home_support")
        assertEquals(0, compose.onAllNodesWithText("Usuarios").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("Retiros").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("Categorías de servicios").fetchSemanticsNodes().size)
        // Sin platform.read no se piden los indicadores.
        assertFalse(requests().any { it.path!!.startsWith("/api/v1/admin/admin-overview") })
    }

    // ------------------------------------------------------------------ usuarios

    @Test
    fun usersAndStatusChange() {
        api.responses[F.list("users")] = F.users
        api.responses["/api/v1/admin/set-user-status"] = """{"data":null}"""
        resource("users")
        waitFor("ana.morales@gmail.com")
        shot("users")
        compose.onAllNodesWithText("Cambiar estado").onFirst().performClick()
        waitFor("Guardar estado")
        input("Motivo", "Reportes reiterados de cobros fuera de la plataforma.")
        idle()
        shot("user_status")
        compose.onNodeWithText("Guardar estado").performClick()
        val sent = body("/api/v1/admin/set-user-status")
        assertEquals(F.USER_ANA, sent["user"]!!.jsonPrimitive.content)
        assertEquals("suspended", sent["status"]!!.jsonPrimitive.content)
        assertEquals("Reportes reiterados de cobros fuera de la plataforma.", sent["reason"]!!.jsonPrimitive.content)
        assertEquals(setOf("user", "status", "reason"), sent.keys)
    }

    @Test
    fun assignRoleSheet() {
        api.responses[F.list("users")] = F.users
        resource("users")
        waitFor("ana.morales@gmail.com")
        compose.onAllNodesWithText("Asignar rol").onFirst().performClick()
        waitFor("Habilitar rol")
        shot("assign_role")
    }

    // ------------------------------------------------------------------ identidad

    @Test
    fun kycListOpensCaseWithReason() {
        api.responses[F.list("kyc")] = F.kycList
        var opened: Pair<String, String>? = null
        resource("kyc", onKyc = { id, reason -> opened = id to reason })
        waitFor("Verificación personal (Venezuela)")
        shot("kyc")
        compose.onAllNodesWithText("Abrir expediente").onFirst().performClick()
        waitFor("Motivo de la consulta")
        shot("kyc_open")
        compose.onAllNodesWithText("Abrir expediente").onLast().performClick()
        idle()
        assertEquals(F.CASE_1 to KYC_OPEN_REASON, opened)
    }

    @Test
    fun kycCaseReview() {
        api.responses["/api/v1/admin/kyc-case"] = F.kycCase
        api.responses["/api/v1/files/moderation/${F.DOC_FRONT}"] = """{"data":{"content":"${F.idCardBase64()}","mime":"image/jpeg"}}"""
        api.responses["/api/v1/admin/review-kyc"] = """{"data":null}"""
        compose.setScreen(api, F.superadmin) { AdminKycCaseScreen(F.CASE_1, "Revisión del expediente para verificar identidad.", onBack = {}) }
        waitFor("Datos del Titular")
        val opened = body("/api/v1/admin/kyc-case")
        assertEquals(F.CASE_1, opened["case"]!!.jsonPrimitive.content)
        assertEquals("Revisión del expediente para verificar identidad.", opened["reason"]!!.jsonPrimitive.content)
        shot("kyc_case")
        compose.onAllNodesWithText("Consultar documento").onFirst().performScrollTo().performClick()
        waitFor("Ampliar")
        assertEquals(KYC_DOC_REASON, body("/api/v1/files/moderation/${F.DOC_FRONT}")["reason"]!!.jsonPrimitive.content)
        shot("kyc_case_document")
        compose.onNodeWithText("Ampliar").performClick()
        idle()
        shot("kyc_document_viewer")
        compose.onAllNodes(androidx.compose.ui.test.hasContentDescription("Cerrar")).onLast().performClick()
        idle()

        compose.onAllNodesWithText("Resolver verificación").onFirst().performClick()
        waitFor("Válida hasta")
        input("Motivo", "Cédula legible, datos coinciden con la selfie.")
        idle()
        shot("kyc_review")
        compose.onAllNodesWithText("Resolver verificación").onLast().performClick()
        val sent = body("/api/v1/admin/review-kyc")
        assertEquals(F.CASE_1, sent["case"]!!.jsonPrimitive.content)
        assertEquals("approved", sent["status"]!!.jsonPrimitive.content)
        assertEquals("Cédula legible, datos coinciden con la selfie.", sent["reason"]!!.jsonPrimitive.content)
        val expected = LocalDate.now(ZoneOffset.UTC).plusYears(1).atTime(23, 59, 59).atOffset(ZoneOffset.UTC).toString()
        assertEquals(expected, sent["expires_at"]!!.jsonPrimitive.content)
    }

    @Test
    fun kycRejectSendsNoExpiry() {
        api.responses["/api/v1/admin/kyc-case"] = F.kycCase
        api.responses["/api/v1/admin/review-kyc"] = """{"data":null}"""
        compose.setScreen(api, F.superadmin) { AdminKycCaseScreen(F.CASE_1, KYC_OPEN_REASON, onBack = {}) }
        waitFor("Datos del Titular")
        compose.onAllNodesWithText("Resolver verificación").onFirst().performClick()
        waitFor("Válida hasta")
        compose.onNodeWithText("Rechazada").performClick()
        input("Motivo", "Selfie no coincide")
        idle()
        compose.onAllNodesWithText("Resolver verificación").onLast().performClick()
        val sent = body("/api/v1/admin/review-kyc")
        assertEquals("rejected", sent["status"]!!.jsonPrimitive.content)
        assertFalse(sent.containsKey("expires_at"))
    }

    // ------------------------------------------------------------------ retiros

    @Test
    fun withdrawalsSandbox() {
        api.responses[F.list("withdrawals")] = F.withdrawals
        api.responses["/api/v1/config"] = """{"data":{"environment":"development","payment_mode":"sandbox","currency":"VES"}}"""
        api.responses["/api/v1/integrations/sandbox/withdrawals/${F.W1}/settle"] = """{"data":{"status":"paid","mode":"sandbox"}}"""
        resource("withdrawals")
        waitFor("Carlos Pérez")
        waitFor("Simular retiro pagado")
        shot("withdrawals")
        compose.onAllNodesWithText("Simular retiro pagado").onFirst().performClick()
        waitFor("Simular pago")
        shot("withdrawal_settle")
        compose.onNodeWithText("Simular pago").performClick()
        val req = waitRequest("/api/v1/integrations/sandbox/withdrawals/${F.W1}/settle")
        assertEquals("POST", req.method)
    }

    @Test
    fun withdrawalsWithoutSandboxHideSimulation() {
        api.responses[F.list("withdrawals")] = F.withdrawals
        api.responses["/api/v1/config"] = """{"data":{"payment_mode":"disabled"}}"""
        resource("withdrawals")
        waitFor("Carlos Pérez")
        waitRequest("/api/v1/config")
        idle()
        assertEquals(0, compose.onAllNodesWithText("Simular retiro pagado").fetchSemanticsNodes().size)
    }

    // ------------------------------------------------------------------ soporte

    @Test
    fun tickets() {
        api.responses[F.list("tickets")] = F.tickets
        var opened: String? = null
        resource("tickets", onTicket = { opened = it })
        waitFor("El plomero no se presentó")
        shot("tickets")
        compose.onAllNodesWithText("Atender caso").onFirst().performClick()
        idle()
        assertEquals(F.T1, opened)
    }

    @Test
    fun ticketThreadAndManage() {
        api.responses["/api/v1/admin/support-ticket"] = F.ticketThread
        api.responses["/api/v1/admin/manage-ticket"] = """{"data":null}"""
        compose.setScreen(api, F.superadmin) { AdminTicketScreen(F.T1, onBack = {}) }
        waitFor("Nota interna")
        assertEquals(TICKET_REASON, body("/api/v1/admin/support-ticket")["reason"]!!.jsonPrimitive.content)
        shot("ticket_thread")
        compose.onNodeWithText("Gestionar caso").performClick()
        waitFor("Asignarme este caso")
        input("Nota interna de gestión", "Se contactó al profesional por teléfono.")
        idle()
        shot("ticket_manage")
        compose.onNodeWithText("Guardar gestión").performClick()
        val sent = body("/api/v1/admin/manage-ticket")
        assertEquals(F.T1, sent["ticket"]!!.jsonPrimitive.content)
        assertEquals("in_progress", sent["status"]!!.jsonPrimitive.content)
        assertEquals(F.superadmin.id, sent["assignee"]!!.jsonPrimitive.content)
    }

    // ------------------------------------------------------------------ modelo de negocio y dinero

    @Test
    fun plansAndForm() {
        api.responses[F.list("plans")] = F.plans
        resource("plans")
        waitFor("Modelo de Negocio Oficial de Vinko")
        shot("plans")
        compose.onNodeWithText("Simulador Interactivo del Modelo de Negocio").performScrollTo()
        idle()
        shot("plans_simulator")
        scrollToItem("Membresía Profesional (Desde Mes 4)")
        shot("plans_list")
        // La lista es perezosa: el botón de cabecera sale de la composición al bajar; se vuelve a él desplazando.
        compose.onAllNodes(androidx.compose.ui.test.hasScrollToNodeAction()).onFirst()
            .performScrollToNode(androidx.compose.ui.test.hasText("Configurar plan"))
        idle()
        compose.onAllNodesWithText("Configurar plan").onFirst().performClick()
        waitFor("Código comercial (ej: STORE-CUSTOM)")
        input("Código comercial (ej: STORE-CUSTOM)", "store-premium-80")
        input("Nombre del plan", "Membresía Comercio Premium")
        input("Precio mensual", "80")
        idle()
        shot("plan_form")
    }

    @Test
    fun feesSimulatorAndForm() {
        api.responses[F.list("fees")] = F.fees
        api.responses["/api/v1/admin/configure-withdrawal-fee"] = """{"data":"fe9"}"""
        resource("fees")
        waitFor("Simulador de comisión de retiro")
        shot("fees")
        scrollDown()
        shot("fees_2")
        compose.onAllNodesWithText("Configurar comisión").onFirst().performScrollTo().performClick()
        waitFor("Porcentaje de retiro")
        input("Porcentaje de retiro", "2,75")
        input("Motivo", "Nuevo costo de transferencias interbancarias.")
        idle()
        shot("fee_form")
        compose.onAllNodesWithText("Configurar comisión").onLast().performClick()
        val sent = body("/api/v1/admin/configure-withdrawal-fee")
        assertEquals("VES", sent["currency"]!!.jsonPrimitive.content)
        assertEquals(275, sent["basis_points"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun vinkoRates() {
        api.responses[F.list("vinko_rates")] = F.vinkoRates
        resource("vinko_rates")
        waitFor("Ajuste según tasa oficial BCV", substring = true)
        shot("vinko_rates")
    }

    @Test
    fun contracts() {
        api.responses[F.list("contracts")] = F.contracts
        resource("contracts")
        waitFor("Reparación de fuga en baño principal")
        shot("contracts")
        compose.onAllNodesWithText("Ver expediente").onFirst().performClick()
        waitFor("Comisión estimada")
        shot("contract_detail")
    }

    @Test
    fun storesAndOrders() {
        api.responses[F.list("stores")] = F.stores
        resource("stores")
        waitFor("Ferretería El Ávila")
        shot("stores")
        compose.onAllNodesWithText("Cambiar membresía").onFirst().performClick()
        waitFor("Plan del Comercio")
        shot("store_plan")
    }

    @Test
    fun orders() {
        api.responses[F.list("orders")] = F.orders
        resource("orders")
        waitFor("Ferretería El Ávila")
        shot("orders")
        compose.onAllNodesWithText("Intervención orden").onFirst().performClick()
        waitFor("Acción administrativa")
        shot("order_action")
    }

    @Test
    fun paginationLoadsNextPage() {
        val page = (1..25).joinToString(",") { i ->
            """{"id":"x-$i","action":"user.registered","entity_type":"user","entity_id":"5f1d2c3b-0000-4000-8000-0000000000$i","created_at":"${F.ago(i * 7L)}"}"""
        }
        api.responses[F.list("audit")] = """{"data":[$page]}"""
        api.responses["/api/v1/admin/admin-list?resource=audit&limit=25&offset=25"] =
            """{"data":[{"id":"x-26","action":"kyc.read","entity_type":"kyc","entity_id":"c1a2b3c4-2222-4d5e-8f90-000000000001","actor_id":"${F.superadmin.id}","created_at":"${F.ago(500)}"}]}"""
        resource("audit")
        waitFor("Usuario registrado")
        shot("audit")
        scrollToItem("Cargar más")
        compose.onNodeWithText("Cargar más").performClick()
        waitFor("Consulta de expediente KYC")
        assertTrue(requests().any { it.path == "/api/v1/admin/admin-list?resource=audit&limit=25&offset=25" })
        scrollToItem("26 elementos · fin de la lista")
        shot("audit_end")
    }

    // ------------------------------------------------------------------ reglas puras

    @Test
    fun rules() {
        assertEquals(275, toBasisPoints(parseDecimal("2,75")!!))
        assertEquals(150050L, toMinor(parseDecimal("1.500,50")!!))
        assertEquals(0L, toMinor(parseDecimal("0")!!))
        assertEquals(null, parseDecimal("abc"))
        assertEquals("2,5 %", percentText(250))
        assertEquals("1500,00", minorToInput(150000))
        assertEquals(6, simulateCommission("service", "remote", "pro", "standard").percent)
        assertEquals(8, simulateCommission("service", "onsite", "base", "standard").percent)
        assertEquals(10, simulateCommission("store", "onsite", "pro", "base").percent)
        assertEquals("12% Remoto", contractFeeText("PRO-BASE", "remote"))
        assertEquals(listOf("tickets"), allowedSections(F.support).map { it.key })
        assertEquals(ADMIN_SECTIONS.size, allowedSections(F.superadmin).size)
        assertEquals("Esperando al usuario", adminStatusLabel("tickets", "waiting_user"))
        assertEquals("Histórica", adminStatusLabel("vinko_rates", "archived"))
        assertTrue(adminKyc("abc", "Revisión del expediente").startsWith("admin/kyc/abc?reason="))
    }
}
