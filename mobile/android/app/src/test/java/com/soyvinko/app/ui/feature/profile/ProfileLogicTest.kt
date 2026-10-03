package com.soyvinko.app.ui.feature.profile

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.soyvinko.app.ui.shell.Routes
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.OffsetDateTime

/** Reglas de la sección Perfil: rutas de novedades, dispositivos, estado KYC y textos legales. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ProfileLogicTest {
    @Test
    fun notificationsLeadToTheRightSection() {
        assertEquals(Routes.contract("c1"), notificationRoute("delivery.submitted", "c1"))
        assertEquals(Routes.contract("c1"), notificationRoute("contract.funded", "c1"))
        assertEquals(Routes.contract("c9"), notificationRoute("review.requested", "c9"))
        assertEquals(Routes.request("r1"), notificationRoute("proposal.received", "r1"))
        assertEquals(Routes.OPPORTUNITIES, notificationRoute("request.available", "r1"))
        assertEquals(Routes.CONTRACTS, notificationRoute("chat.message", "c1"))
        assertNull(notificationRoute("system.unknown", "x"))
        assertEquals("Hay novedades en tu cuenta", notificationInfo("system.unknown").title)
    }

    @Test
    fun devicesHaveReadableNames() {
        assertEquals(
            "App SoyVinko · samsung SM-A546E (Android 14)" to true,
            friendlyDevice("SoyVinko-Android/1.4.0 (samsung SM-A546E; Android 14)"),
        )
        assertEquals(
            "Chrome en Windows" to false,
            friendlyDevice("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Safari/537.36"),
        )
        assertEquals("Safari en iPhone", friendlyDevice("Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) Version/17.5 Mobile/15E148 Safari/604.1").first)
        assertEquals("Dispositivo" to false, friendlyDevice(null))
    }

    @Test
    fun kycStateFollowsStatusAndExpiry() {
        fun case(status: String, expires: OffsetDateTime? = null) = buildJsonObject {
            put("status", status)
            put("expires_at", expires?.toString()?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull)
        }
        assertEquals(KycState.None, kycStateOf(null))
        assertEquals(KycState.Approved, kycStateOf(case("approved", OffsetDateTime.now().plusDays(30))))
        assertEquals(KycState.Expired, kycStateOf(case("approved", OffsetDateTime.now().minusDays(1))))
        assertEquals(KycState.Pending, kycStateOf(case("in_review")))
        assertEquals(KycState.Rejected, kycStateOf(case("revoked")))
    }

    @Test
    fun legalDocumentsArePorted() {
        assertEquals(listOf("terminos", "privacidad", "cookies", "aviso-legal"), LEGAL_DOCS.map { it.slug })
        LEGAL_DOCS.forEach { doc ->
            assertTrue(doc.sections.isNotEmpty())
            doc.sections.forEach { s ->
                assertTrue("${doc.slug} ${s.number} sin contenido", s.blocks.isNotEmpty())
                assertFalse(s.title.isBlank())
            }
        }
        val text = rich("Escríbenos a **privacidad@soyvinko.com** o usa `chamba_csrf`.", null).text
        assertEquals("Escríbenos a privacidad@soyvinko.com o usa chamba_csrf.", text)
    }

    @Test
    fun birthdateDisplay() {
        assertEquals("21/05/1990", isoToDisplay("1990-05-21"))
    }
}
