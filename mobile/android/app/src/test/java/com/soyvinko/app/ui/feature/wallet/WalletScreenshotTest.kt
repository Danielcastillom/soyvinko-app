package com.soyvinko.app.ui.feature.wallet

import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.soyvinko.app.SoyVinkoApp
import com.soyvinko.app.ui.FakeApi
import com.soyvinko.app.ui.setScreen
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
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
import java.time.OffsetDateTime
import java.time.ZoneOffset

private const val W = "/api/v1/wallet"

/** Capturas de la wallet con datos ficticios realistas (app/build/outputs/roborazzi/wallet_*.png). */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = SoyVinkoApp::class)
class WalletScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val api = FakeApi()

    /** Respuestas que dependen de la consulta (?currency=VES / USD): FakeApi solo distingue la ruta. */
    private val byQuery = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        val base = api.server.dispatcher
        api.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val hit = byQuery.entries.firstOrNull { request.path!!.startsWith(it.key) }
                    ?: return base.dispatch(request)
                api.requests += request
                return MockResponse().setHeader("Content-Type", "application/json").setBody(hit.value)
            }
        }
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        fun ago(hours: Long) = "\"${now.minusHours(hours)}\""

        api.responses["/api/v1/config"] = """{"data":{"payment_mode":"sandbox","currency":"VES","locale":"es-VE"}}"""
        api.responses["$W/my-wallet"] = """{"data":{"balance":24,"credits":24,"ledger":[
            {"id":61,"delta":10,"balance_after":24,"kind":"vinko_conversion","reason":"Conversión a Vinkos: 10 Vinkos (9500 VES)","created_at":${ago(2)}},
            {"id":58,"delta":-1,"balance_after":14,"kind":"contract_debit","reason":"Contratación aceptada por ambas partes","created_at":${ago(26)}},
            {"id":44,"delta":12,"balance_after":15,"kind":"purchase","reason":"Paquete Membresía Profesional","created_at":${ago(50)}},
            {"id":12,"delta":3,"balance_after":3,"kind":"welcome","reason":"Activación inicial de perfil profesional","created_at":${ago(200)}}
        ]}}"""
        byQuery["$W/money-wallet?currency=VES"] = """{"data":{"id":"w1","currency":"VES","status":"active","available_minor":1845000,"reserved_for_services_minor":450000,"withdrawal_pending_minor":0}}"""
        byQuery["$W/money-wallet?currency=USD"] = """{"data":{"id":"w2","currency":"USD","status":"active","available_minor":4250,"reserved_for_services_minor":0,"withdrawal_pending_minor":1500}}"""
        api.responses["$W/get-vinko-exchange-rates"] = """{"data":{
            "VES":{"currency":"VES","rate_minor":95000,"created_at":"2026-10-01T13:00:00Z","reason":"Tasa oficial"},
            "USD":{"currency":"USD","rate_minor":100,"created_at":"2026-10-01T13:00:00Z","reason":"Tasa oficial"}}}"""
        api.responses["$W/my-topups"] = """{"data":[
            {"id":"6f1c0f3e-0000-4000-8000-000000000001","amount_minor":500000,"fee_minor":0,"status":"pending","currency":"VES","created_at":${ago(3)}},
            {"id":"6f1c0f3e-0000-4000-8000-000000000002","amount_minor":2000000,"fee_minor":0,"status":"settled","currency":"VES","created_at":${ago(30)}}
        ]}"""
        byQuery["$W/money-history?currency=VES"] = """{"data":[
            {"entry_id":120,"kind":"vinko_purchase","account_kind":"available","currency":"VES","amount_minor":-950000,"balance_after_minor":1845000,"created_at":${ago(2)}},
            {"entry_id":118,"kind":"contract_funded","account_kind":"contract_hold","currency":"VES","amount_minor":450000,"balance_after_minor":450000,"created_at":${ago(5)}},
            {"entry_id":117,"kind":"contract_funded","account_kind":"available","currency":"VES","amount_minor":-450000,"balance_after_minor":2795000,"created_at":${ago(5)}},
            {"entry_id":110,"kind":"contract_released","account_kind":"available","currency":"VES","amount_minor":1245000,"balance_after_minor":3245000,"created_at":${ago(28)}},
            {"entry_id":101,"kind":"topup","account_kind":"available","currency":"VES","amount_minor":2000000,"balance_after_minor":2000000,"created_at":${ago(30)}},
            {"entry_id":96,"kind":"withdrawal_cancelled","account_kind":"available","currency":"VES","amount_minor":300000,"balance_after_minor":300000,"created_at":${ago(80)}},
            {"entry_id":95,"kind":"withdrawal_reserved","account_kind":"available","currency":"VES","amount_minor":-300000,"balance_after_minor":0,"created_at":${ago(81)}}
        ]}"""
        byQuery["$W/money-history?currency=USD"] = """{"data":[]}"""
        api.responses["$W/my-payout-methods"] = """{"data":[
            {"id":"m1","provider":"ve_bank","label":"Banesco (Corriente)","masked_account":"0134 •••• 4821 · V-****5678","currency":"VES","status":"verified"},
            {"id":"m2","provider":"pago_movil","label":"Pago Móvil · Banco de Venezuela (BDV)","masked_account":"0414 •••• 567 · V-****5678","currency":"VES","status":"verified"},
            {"id":"m3","provider":"ve_bank","label":"Mercantil (Ahorro)","masked_account":"0105 •••• 1180 · V-****5678","currency":"VES","status":"disabled"}
        ]}"""
        api.responses["$W/my-withdrawals"] = """{"data":[
            {"id":"wd1","amount_minor":1000000,"fee_minor":20000,"net_minor":980000,"destination_label":"0134 •••• 4821 · V-****5678","status":"pending","currency":"VES","created_at":${ago(1)}},
            {"id":"wd2","amount_minor":600000,"fee_minor":12000,"net_minor":588000,"destination_label":"0414 •••• 567 · V-****5678","status":"paid","currency":"VES","created_at":${ago(120)}}
        ]}"""
        api.responses["$W/quote-withdrawal"] = """{"data":{"id":"q1","amount_minor":500000,"basis_points":200,"fee_minor":10000,"net_minor":490000,"expires_at":"${now.plusMinutes(15)}"}}"""
        api.responses["$W/convert-money-to-vinkos"] = """{"data":{"credits":34,"debited_minor":950000,"currency":"VES"}}"""
        api.responses["$W/active-plans"] = """{"data":[
            {"id":"p1","code":"PRO-BASE","name":"Modalidad Base Sin Suscripción (Desde Mes 4)","affiliate_type":"provider","credits":5,"price_minor":0,"currency":"USD","positioning_days":0,"marketplace_days":0,"max_products":0,"fee_summary":"8% Presencial / 12% Remoto","benefits_summary":"• No paga mensualidad.\n• Ideal para trabajadores ocasionales o remotos con menos costos físicos."},
            {"id":"p2","code":"PRO-MEMBERSHIP-15","name":"Membresía Profesional (Desde Mes 4)","affiliate_type":"provider","credits":30,"price_minor":1500,"currency":"USD","positioning_days":30,"marketplace_days":0,"max_products":0,"fee_summary":"Tasa Mínima: 3% Presencial / 6% Remoto","benefits_summary":"• Prioridad para recibir solicitudes directas."}
        ]}"""
        api.responses["$W/my-credit-purchases"] = """{"data":[{"id":"cp1","credits":15,"amount_minor":0,"currency":"USD","status":"pending","created_at":${ago(4)}}]}"""
    }

    @After fun tearDown() = api.shutdown()

    private fun idleUntil(condition: () -> Boolean) = compose.waitUntil(10_000) {
        shadowOf(Looper.getMainLooper()).idle()
        condition()
    }

    private fun waitText(text: String, substring: Boolean = false) =
        idleUntil { compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }

    private fun shot(name: String) {
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/wallet_$name.png")
    }

    /** Incluye hojas inferiores y diálogos (otras ventanas). */
    private fun screen(name: String) {
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        captureScreenRoboImage("build/outputs/roborazzi/wallet_$name.png")
    }

    private fun openWallet() {
        compose.setScreen(api) { _ -> WalletScreen(onNavigate = {}) }
        waitText("Bs 18.450,00")
        waitText("1 Vinko = Bs 950,00", substring = true)
        waitText("Simular pago")
    }

    @Test
    fun main() {
        openWallet()
        shot("main")
    }

    @Test
    fun mainBottom() {
        openWallet()
        compose.onNodeWithText("Presencia y posicionamiento").performScrollTo()
        shot("main_bottom")
    }

    @Test
    fun converterInsufficientFunds() {
        openWallet()
        compose.onNodeWithText("Comprar").performClick()
        compose.onNodeWithText("25").performScrollTo().performClick()
        waitText("Te faltan", substring = true)
        compose.onNodeWithText("Comprar").performClick()
        shot("converter_insufficient")
        compose.onNodeWithText("Comprar 25 Vinkos").assertExistsDisabled()
    }

    @Test
    fun confirmAndConvertSendsKey() {
        openWallet()
        compose.onNodeWithText("Comprar 10 Vinkos").performScrollTo().performClick()
        waitText("Confirma tu compra")
        screen("confirm")

        compose.onNodeWithText("Confirmar compra").performClick()
        idleUntil { api.requests.any { it.path!!.startsWith("$W/convert-money-to-vinkos") } }
        val request = api.requests.first { it.path!!.startsWith("$W/convert-money-to-vinkos") }
        assertEquals("POST", request.method)
        val body = request.body.readUtf8()
        assertTrue(body, body.contains("\"currency\":\"VES\""))
        assertTrue(body, body.contains("\"vinkos\":10"))
        assertTrue(body, Regex("\"key\":\"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\"").containsMatchIn(body))

        waitText("¡Compra realizada!")
        screen("convert_success")
    }

    @Test
    fun topupPrefilledWithMissingAmount() {
        openWallet()
        compose.onNodeWithText("Comprar").performClick()
        compose.onNodeWithText("25").performScrollTo().performClick()
        waitText("Te faltan", substring = true)
        compose.onAllNodesWithText("Recargar").fetchSemanticsNodes().size.let { assertTrue(it >= 3) }
        compose.onAllNodesWithText("Recargar")[2].performClick()
        waitText("Recargar saldo en bolívares")
        waitText("Recargar Bs 5.300,00")
        screen("topup")
    }

    @Test
    fun history() {
        compose.setScreen(api) { _ -> WalletHistoryScreen(onBack = {}) }
        waitText("Recarga de saldo")
        waitText("Bs 18.450,00")
        shot("history")
        compose.onNodeWithText("Vinkos").performClick()
        waitText("Bono de bienvenida", substring = true)
        shot("history_vinkos")
    }

    @Test
    fun withdraw() {
        compose.setScreen(api) { _ -> WithdrawScreen(onBack = {}, onAddMethod = {}) }
        waitText("Banesco (Corriente)")
        waitText("Retiro de Bs 10.000,00")
        shot("withdraw")

        compose.onNodeWithText("Monto a retirar").performScrollTo().performTextInput("5000")
        compose.onNodeWithText("Ver cuánto recibo").performScrollTo().performClick()
        waitText("Recibirás")
        compose.onNodeWithText("Confirmar retiro").performScrollTo()
        shot("withdraw_quote")
        assertTrue(api.requests.any { it.path!!.startsWith("$W/quote-withdrawal") && it.body.readUtf8().let { b -> b.contains("\"amount\":500000") && b.contains("\"method\":\"m1\"") } })
    }

    @Test
    fun addPayoutMethod() {
        compose.setScreen(api) { _ -> AddPayoutMethodScreen(onBack = {}, onSaved = {}) }
        waitText("Número de cuenta (20 dígitos)")
        compose.onNodeWithText("Número de cuenta (20 dígitos)").performTextInput("01340211")
        waitText("8/20 dígitos")
        shot("add_payout")
    }

    @Test
    fun plans() {
        compose.setScreen(api) { _ -> VinkoPlansScreen(onBack = {}) }
        waitText("Membresía Profesional (Desde Mes 4)")
        waitText("Simular activación")
        shot("plans")
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsDisabled() {
        assertExists()
        assertTrue(fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled))
    }
}
