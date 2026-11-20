package com.soyvinko.app.ui.feature.transport

import com.soyvinko.app.ui.common.GeoPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportKitTest {

    @Test
    fun calculatesStandardTransportFareForBaseDistance() {
        assertEquals(1.0, calculateStandardTransportFare(0.5, "moto"), 0.001)
        assertEquals(1.0, calculateStandardTransportFare(5.0, "moto"), 0.001)
        assertEquals(1.0, calculateStandardTransportFare(10.0, "moto"), 0.001)

        assertEquals(2.0, calculateStandardTransportFare(5.0, "carro"), 0.001)
        assertEquals(4.0, calculateStandardTransportFare(5.0, "pickup"), 0.001)
        assertEquals(8.0, calculateStandardTransportFare(5.0, "camion"), 0.001)
    }

    @Test
    fun calculatesStandardTransportFareForLongDistances() {
        assertEquals(2.0, calculateStandardTransportFare(15.0, "moto"), 0.001)
        assertEquals(3.0, calculateStandardTransportFare(20.0, "moto"), 0.001)
        assertEquals(3.0, calculateStandardTransportFare(12.0, "carro"), 0.001)
        assertEquals(32.0, calculateStandardTransportFare(25.0, "camion"), 0.001)
    }

    @Test
    fun handlesZeroAndNegativeDistancesGracefully() {
        assertEquals(1.0, calculateStandardTransportFare(0.0, "moto"), 0.001)
        assertEquals(1.0, calculateStandardTransportFare(-5.0, "moto"), 0.001)
        assertEquals(2.0, calculateStandardTransportFare(-10.0, "carro"), 0.001)
    }

    @Test
    fun calculatesHaversineDistanceAccurately() {
        val p1 = GeoPos(10.4910, -66.8780)
        assertEquals(0.0, haversineKm(p1, p1), 0.001)

        val p2 = GeoPos(10.4930, -66.8550)
        val dist = haversineKm(p1, p2)
        assertTrue("Distancia esperada entre 2.0 y 3.0 km, fue $dist", dist in 2.0..3.0)
    }

    @Test
    fun vehicleCatalogsAndLabels() {
        assertEquals("Moto", vehicleLabel("moto"))
        assertEquals("Carro", vehicleLabel("carro"))
        assertEquals("Camioneta Pickup", vehicleLabel("pickup"))
        assertEquals("Camión", vehicleLabel("camion"))
        assertEquals("Vehículo", vehicleLabel("otro"))

        assertNotNull(findVehicleTypeInfo("moto"))
        assertNotNull(findVehicleTypeInfo("carro"))
        assertNotNull(findVehicleTypeInfo("pickup"))
        assertNotNull(findVehicleTypeInfo("camion"))
        assertNull(findVehicleTypeInfo("avion"))

        assertEquals(4, VEHICLE_CATALOG.size)
    }

    @Test
    fun shipmentTypeLabels() {
        assertEquals("Viaje de Pasajero", shipmentTypeLabel("ride"))
        assertEquals("Paquete Express", shipmentTypeLabel("package"))
        assertEquals("Flete / Carga", shipmentTypeLabel("freight"))
        assertEquals("Delivery Comida", shipmentTypeLabel("food"))
        assertEquals("Envío", shipmentTypeLabel(null))
        assertEquals("Envío", shipmentTypeLabel("desconocido"))
    }

    @Test
    fun formatsDistanceCorrectly() {
        assertEquals("350 m", formatTransportDistance(0.35))
        assertEquals("999 m", formatTransportDistance(0.999))
        assertEquals("1.0 km", formatTransportDistance(1.0))
        assertEquals("4.5 km", formatTransportDistance(4.56))
        assertEquals("12.3 km", formatTransportDistance(12.34))
    }

    @Test
    fun formatsFareCorrectly() {
        assertEquals("$1.00", formatTransportFare(1.0))
        assertEquals("$5.00", formatTransportFare(5.0))
        assertEquals("$3.5", formatTransportFare(3.5))
        assertEquals("$12.75", formatTransportFare(12.75))
    }

    @Test
    fun validatesLicensePlates() {
        assertTrue(validateLicensePlate("AB123CD").isValid)
        assertTrue(validateLicensePlate("AA123BB").isValid)
        assertTrue(validateLicensePlate("ABC12D").isValid)
        assertTrue(validateLicensePlate("7A1B2C").isValid)

        assertFalse(validateLicensePlate("").isValid)
        assertFalse(validateLicensePlate("123").isValid)
        assertFalse(validateLicensePlate("ABCDEFGHIJK").isValid)
        assertFalse(validateLicensePlate("AB-12-CD").isValid)
        assertFalse(validateLicensePlate("123456").isValid)
        assertFalse(validateLicensePlate("ABCDEF").isValid)
    }

    @Test
    fun validatesVenezuelanPhones() {
        assertTrue(validateVenezuelanPhone("04121234567").isValid)
        assertTrue(validateVenezuelanPhone("04141234567").isValid)
        assertTrue(validateVenezuelanPhone("04241234567").isValid)
        assertTrue(validateVenezuelanPhone("04161234567").isValid)
        assertTrue(validateVenezuelanPhone("04261234567").isValid)
        assertTrue(validateVenezuelanPhone("+584121234567").isValid)
        assertTrue(validateVenezuelanPhone("0412-123-4567").isValid)

        assertFalse(validateVenezuelanPhone("").isValid)
        assertFalse(validateVenezuelanPhone("0412123").isValid)
        assertFalse(validateVenezuelanPhone("04991234567").isValid)
        assertFalse(validateVenezuelanPhone("05001234567").isValid)
    }

    @Test
    fun calculatesRiderEarningsCorrectly() {
        // Tarifa $10.00 (1000 centavos) con 10% de comisión
        val earnings = calculateRiderEarnings(1000L, commissionPercent = 10.0)
        assertEquals(1000L, earnings.grossFareMinor)
        assertEquals(100L, earnings.commissionMinor)
        assertEquals(900L, earnings.netFareMinor)

        // Tarifa $50.00 (5000 centavos) con 12% comisión
        val earnings50 = calculateRiderEarnings(5000L, commissionPercent = 12.0)
        assertEquals(600L, earnings50.commissionMinor)
        assertEquals(4400L, earnings50.netFareMinor)
    }

    @Test
    fun estimatesTripDurationCorrectly() {
        val durationMoto = estimateTripDurationMinutes(10.0, "moto")
        val durationCamion = estimateTripDurationMinutes(10.0, "camion")

        assertTrue("Duración en moto debe ser positiva y razonable", durationMoto in 10..30)
        assertTrue("Camión debe tardar más o igual que moto", durationCamion >= durationMoto)
    }

    @Test
    fun validatesCustomFareThresholds() {
        val standard = 5.0
        assertTrue(validateCustomFare(5.0, standard, "carro").isValid)
        assertTrue(validateCustomFare(4.0, standard, "carro").isValid)
        assertTrue(validateCustomFare(10.0, standard, "carro").isValid)

        assertFalse(validateCustomFare(0.0, standard, "carro").isValid)
        assertFalse(validateCustomFare(-1.0, standard, "carro").isValid)
        assertFalse(validateCustomFare(1.0, standard, "carro").isValid) // muy bajo (< 60%)
        assertFalse(validateCustomFare(100.0, standard, "carro").isValid) // excesivo (> 350%)
    }

    @Test
    fun generatesExternalNavigationUrl() {
        val pos = GeoPos(10.4910, -66.8780)
        val url = getExternalNavigationUrl(pos, "Plaza Venezuela")
        assertTrue(url.contains("10.491"))
        assertTrue(url.contains("-66.878"))
        assertTrue(url.contains("Plaza+Venezuela"))
    }
}
