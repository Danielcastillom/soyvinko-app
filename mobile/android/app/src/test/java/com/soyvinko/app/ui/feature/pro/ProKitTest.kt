package com.soyvinko.app.ui.feature.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Conversión de importes es-VE a unidades menores y normalización de identificadores. */
class ProKitTest {
    @Test fun parsesVenezuelanAmounts() {
        assertEquals(450000L, parseMajor("4.500,00"))
        assertEquals(123450L, parseMajor("1.234,5"))
        assertEquals(2550L, parseMajor("25,50"))
        assertEquals(2550L, parseMajor("25.50"))
        assertEquals(150000L, parseMajor("1.500"))
        assertEquals(1200L, parseMajor("12"))
        assertEquals(1L, parseMajor("0,005"))
        assertNull(parseMajor(""))
        assertNull(parseMajor("abc"))
        assertNull(parseMajor("-5"))
    }

    @Test fun formatsEditableAmounts() {
        assertEquals("4500,00", majorText(450000))
        assertEquals("0,05", majorText(5))
        assertEquals("", majorText(null))
        assertEquals(450000L, parseMajor(majorText(450000)))
    }

    @Test fun slugsMatchServerNormalization() {
        assertEquals("maria-gonzalez-electricidad", slugify("María González  Electricidad"))
        assertEquals("ferreteria-el-valle", slugify("¡Ferretería El Valle!"))
        assertTrue(SLUG_PATTERN.matches(slugify("Plomería 24/7 Caracas")))
    }

    @Test fun isoAlwaysIncludesSeconds() {
        assertEquals("2026-10-10T02:50:00Z", kotlinx.datetime.Instant.parse("2026-10-10T02:50:00.700Z").iso())
    }

    @Test fun budgetPrefersRangeThenOffer() {
        val json = { s: String -> kotlinx.serialization.json.Json.parseToJsonElement(s) as kotlinx.serialization.json.JsonObject }
        assertEquals("US$ 60,00", budgetText(json("""{"currency":"USD","budget_max_minor":6000}""")))
        assertEquals("A cotizar en Bs", budgetText(json("""{"currency":"VES"}""")))
        assertEquals("Bs 3.600,00", budgetText(json("""{"currency":"VES","offered_price_minor":360000}""")))
    }
}
