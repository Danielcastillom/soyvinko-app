package com.soyvinko.app.ui.feature.wallet

import androidx.compose.ui.text.AnnotatedString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Montos en unidades mayores con formato es-VE y textos de movimientos. */
class WalletKitTest {
    @Test
    fun sanitizesTypedAmounts() {
        assertEquals("1234,56", sanitizeAmount("1234,567"))
        assertEquals("1234,5", sanitizeAmount("1234.5"))
        assertEquals("0,5", sanitizeAmount(",5"))
        assertEquals("15", sanitizeAmount("0015"))
        assertEquals("12,34", sanitizeAmount("12,3,4"))
        assertEquals("", sanitizeAmount("abc"))
    }

    @Test
    fun parsesMajorUnitsToMinor() {
        assertEquals(123456L, parseMinor("1234,56"))
        assertEquals(475050L, parseMinor("4750,5"))
        assertEquals(500000L, parseMinor("5000"))
        assertNull(parseMinor(""))
        assertEquals("4750", rawFromMinor(475000))
        assertEquals("4750,05", rawFromMinor(475005))
        assertEquals(475005L, parseMinor(rawFromMinor(475005)))
    }

    @Test
    fun drawsThousandsSeparators() {
        val t = AmountTransformation.filter(AnnotatedString("1234567,8"))
        assertEquals("1.234.567,8", t.text.text)
        assertEquals(0, t.offsetMapping.originalToTransformed(0))
        assertEquals(11, t.offsetMapping.originalToTransformed(9))
        assertEquals(9, t.offsetMapping.transformedToOriginal(11))
        assertEquals("950", AmountTransformation.filter(AnnotatedString("950")).text.text)
    }

    @Test
    fun labelsMovementsLikeTheWeb() {
        fun e(kind: String, amount: Long) = buildJsonObject { put("kind", kind); put("amount_minor", JsonPrimitive(amount)) }
        assertEquals("Recarga de saldo", movementLabel(e("topup", 100)))
        assertEquals("Compra de Vinkos", movementLabel(e("vinko_purchase", -100)))
        assertEquals("Pago reservado en custodia", movementLabel(e("contract_funded", -100)))
        assertEquals("Pago recibido por un servicio", movementLabel(e("contract_released", 100)))
        assertEquals("Retiro solicitado", movementLabel(e("withdrawal_reserved", -100)))
        assertEquals("Retiro devuelto", movementLabel(e("withdrawal_cancelled", 100)))
        assertEquals("Bono de bienvenida", ledgerKindLabel("welcome"))
        assertEquals("Contratación de servicio", ledgerKindLabel("contract_debit"))
    }

    @Test
    fun validatesPayoutFormLikeTheWeb() {
        assertEquals(null, validatePayout(true, "01340211000000004821", "0134", "", "0414", "12345678", "María González"))
        assertEquals(
            "Los primeros 4 dígitos de la cuenta (0105) no coinciden con el banco seleccionado (0134).",
            validatePayout(true, "01050211000000004821", "0134", "", "0414", "12345678", "María González"),
        )
        assertEquals(
            "El número de celular debe tener 7 dígitos después del prefijo 0414.",
            validatePayout(false, "", "0102", "12345", "0414", "12345678", "María González"),
        )
    }
}
