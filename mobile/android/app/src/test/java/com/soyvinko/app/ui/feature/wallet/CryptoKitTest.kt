package com.soyvinko.app.ui.feature.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoKitTest {

    @Test
    fun testValidTrc20Address() {
        val testAddr = "TXLAQ63Xg1NAzckPwKHvzw7CSEmLMEqcdj"
        assertEquals(34, testAddr.length)
        assertNull(validateCryptoAddress(CryptoNetwork.TRC20, testAddr))
    }

    @Test
    fun testInvalidTrc20Address() {
        // Starts with 0x instead of T
        val errPrefix = validateCryptoAddress(CryptoNetwork.TRC20, "0x71C8A68B4b035105260580979EceA3F0E37785F6")
        assertNotNull(errPrefix)
        assertTrue(errPrefix!!.contains("comenzar con la letra 'T'"))

        // Wrong length
        val errLen = validateCryptoAddress(CryptoNetwork.TRC20, "TYDjhZp64zVinko")
        assertNotNull(errLen)
        assertTrue(errLen!!.contains("34 caracteres"))

        // Invalid characters (0 or O in Base58)
        val errChars = validateCryptoAddress(CryptoNetwork.TRC20, "T0O0000000000000000000000000000000")
        assertNotNull(errChars)
    }

    @Test
    fun testValidEvmAddress() {
        val validEvm = "0x71C8A68B4b035105260580979EceA3F0E37785F6"
        assertEquals(42, validEvm.length)
        assertNull(validateCryptoAddress(CryptoNetwork.BEP20, validEvm))
        assertNull(validateCryptoAddress(CryptoNetwork.POLYGON, validEvm))
    }

    @Test
    fun testInvalidEvmAddress() {
        // Missing 0x prefix
        val errPrefix = validateCryptoAddress(CryptoNetwork.BEP20, "71C8A68B4b035105260580979EceA3F0E37785F6")
        assertNotNull(errPrefix)
        assertTrue(errPrefix!!.contains("comenzar con '0x'"))

        // Non-hex characters
        val errHex = validateCryptoAddress(CryptoNetwork.POLYGON, "0xZZC8A68B4b035105260580979EceA3F0E37785F6")
        assertNotNull(errHex)
    }

    @Test
    fun testBinancePayIdAndEmailValidation() {
        // Valid numeric ID (8-10 digits)
        assertNull(validateCryptoAddress(CryptoNetwork.BINANCE_PAY, "284910284"))
        assertNull(validateCryptoAddress(CryptoNetwork.BINANCE_PAY, "12345678"))

        // Valid email
        assertNull(validateCryptoAddress(CryptoNetwork.BINANCE_PAY, "pagos@soyvinko.com"))

        // Invalid ID
        val errShort = validateCryptoAddress(CryptoNetwork.BINANCE_PAY, "1234")
        assertNotNull(errShort)

        val errInvalid = validateCryptoAddress(CryptoNetwork.BINANCE_PAY, "not_an_email_or_id")
        assertNotNull(errInvalid)
    }

    @Test
    fun testTxHashValidation() {
        val validHash64 = "a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890"
        assertNull(validateTxHash(CryptoNetwork.TRC20, validHash64))
        assertNull(validateTxHash(CryptoNetwork.BEP20, "0x$validHash64"))

        // Empty hash
        assertNotNull(validateTxHash(CryptoNetwork.TRC20, ""))

        // Too short
        assertNotNull(validateTxHash(CryptoNetwork.TRC20, "abc123"))
    }

    @Test
    fun testMaskAddressAndExplorerUrl() {
        val masked = maskCryptoAddress(CryptoNetwork.TRC20, "TYDjhZp64zVinkoDepositTrc20Tron34C")
        assertTrue(masked.startsWith("TRC20 · TYDjhZ"))
        assertTrue(masked.endsWith("n34C"))

        val tronUrl = getExplorerTxUrl(CryptoNetwork.TRC20, "abc123hash")
        assertEquals("https://tronscan.org/#/transaction/abc123hash", tronUrl)

        val bscUrl = getExplorerTxUrl(CryptoNetwork.BEP20, "abc123hash")
        assertEquals("https://bscscan.com/tx/0xabc123hash", bscUrl)
    }
}

