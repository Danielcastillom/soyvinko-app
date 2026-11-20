package com.soyvinko.app.ui.feature.wallet

/**
 * Redes de criptomonedas y stablecoins (USDT) soportadas en la plataforma Vinko.
 */
enum class CryptoNetwork(
    val id: String,
    val symbol: String,
    val displayName: String,
    val networkName: String,
    val explorerName: String,
    val depositAddress: String,
    val estimatedFeeUsd: Double,
    val confirmationTime: String,
) {
    TRC20(
        id = "trc20",
        symbol = "USDT-TRC20",
        displayName = "USDT · Tron (TRC-20)",
        networkName = "Tron (TRC-20)",
        explorerName = "TronScan",
        depositAddress = "TXLAQ63Xg1NAzckPwKHvzw7CSEmLMEqcdj",
        estimatedFeeUsd = 1.50,
        confirmationTime = "~3-5 min",
    ),
    BEP20(
        id = "bep20",
        symbol = "USDT-BEP20",
        displayName = "USDT · BNB Chain (BEP-20)",
        networkName = "BNB Smart Chain (BEP-20)",
        explorerName = "BscScan",
        depositAddress = "0x71C8A68B4b035105260580979EceA3F0E37785F6",
        estimatedFeeUsd = 0.35,
        confirmationTime = "~1-2 min",
    ),
    POLYGON(
        id = "polygon",
        symbol = "USDT-POLYGON",
        displayName = "USDT · Polygon POS",
        networkName = "Polygon Network",
        explorerName = "PolygonScan",
        depositAddress = "0x71C8A68B4b035105260580979EceA3F0E37785F6",
        estimatedFeeUsd = 0.20,
        confirmationTime = "~2-4 min",
    ),
    BINANCE_PAY(
        id = "binance_pay",
        symbol = "Binance Pay",
        displayName = "Binance Pay (ID o Correo)",
        networkName = "Binance Pay",
        explorerName = "Binance",
        depositAddress = "284910284",
        estimatedFeeUsd = 0.00,
        confirmationTime = "Instantáneo (0% comisión)",
    );

    companion object {
        fun of(id: String?): CryptoNetwork = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TRC20
    }
}

/**
 * Valida de forma estricta una dirección de billetera según la red seleccionada.
 * Retorna un mensaje descriptivo en español si es inválida, o null si es válida.
 */
fun validateCryptoAddress(network: CryptoNetwork, address: String): String? {
    val clean = address.trim()
    if (clean.isEmpty()) return "Ingresa la dirección de destino o Binance Pay ID."

    return when (network) {
        CryptoNetwork.BINANCE_PAY -> {
            val isNumericId = clean.matches(Regex("""^\d{8,10}$"""))
            val isEmail = clean.matches(Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$"""))
            if (!isNumericId && !isEmail) {
                "Ingresa un Binance Pay ID numérico (8 a 10 dígitos) o correo registrado en Binance."
            } else null
        }
        CryptoNetwork.TRC20 -> {
            if (!clean.startsWith("T")) {
                "Las direcciones TRC-20 (Tron) deben comenzar con la letra 'T'."
            } else if (clean.length != 34) {
                "La dirección TRC-20 debe tener exactamente 34 caracteres (tiene ${clean.length})."
            } else if (!clean.matches(Regex("""^T[a-km-zA-HJ-NP-Z1-9]{33}$"""))) {
                "La dirección contiene caracteres Base58 inválidos (no se permiten 0, O, I, l)."
            } else null
        }
        CryptoNetwork.BEP20, CryptoNetwork.POLYGON -> {
            if (!clean.startsWith("0x")) {
                "Las direcciones EVM (${network.networkName}) deben comenzar con '0x'."
            } else if (clean.length != 42) {
                "La dirección debe tener exactamente 42 caracteres (tiene ${clean.length})."
            } else if (!clean.matches(Regex("""^0x[a-fA-F0-9]{40}$"""))) {
                "La dirección contiene caracteres hexadecimales inválidos."
            } else null
        }
    }
}

/**
 * Valida el formato de un hash de transacción (TxID / TxHash).
 */
fun validateTxHash(network: CryptoNetwork, txHash: String): String? {
    val clean = txHash.trim()
    if (clean.isEmpty()) return "Ingresa el hash (TxID) o ID de comprobante de la transacción."

    return when (network) {
        CryptoNetwork.BINANCE_PAY -> {
            if (clean.length < 6) "El ID de comprobante de Binance Pay debe tener al menos 6 caracteres." else null
        }
        CryptoNetwork.TRC20, CryptoNetwork.BEP20, CryptoNetwork.POLYGON -> {
            val hash = clean.removePrefix("0x")
            if (hash.length != 64 || !hash.matches(Regex("""^[a-fA-F0-9]{64}$"""))) {
                "El Hash de transacción (${network.networkName}) debe tener 64 caracteres hexadecimales."
            } else null
        }
    }
}

/**
 * Formatea una dirección para visualización segura (ej: TRC20 · TYDjhZ••••Trc20X9).
 */
fun maskCryptoAddress(network: CryptoNetwork, address: String): String {
    val clean = address.trim()
    if (clean.length <= 10) return clean
    return when (network) {
        CryptoNetwork.BINANCE_PAY -> "Binance Pay · ${clean.take(4)}•••${clean.takeLast(4)}"
        CryptoNetwork.TRC20 -> "TRC20 · ${clean.take(6)}••••${clean.takeLast(4)}"
        CryptoNetwork.BEP20 -> "BEP20 · ${clean.take(6)}••••${clean.takeLast(4)}"
        CryptoNetwork.POLYGON -> "POLYGON · ${clean.take(6)}••••${clean.takeLast(4)}"
    }
}

/**
 * Genera la URL del explorador de bloques público correspondiente.
 */
fun getExplorerTxUrl(network: CryptoNetwork, txHash: String): String? {
    val clean = txHash.trim()
    if (clean.isEmpty()) return null
    return when (network) {
        CryptoNetwork.TRC20 -> "https://tronscan.org/#/transaction/${clean.removePrefix("0x")}"
        CryptoNetwork.BEP20 -> "https://bscscan.com/tx/${if (clean.startsWith("0x")) clean else "0x$clean"}"
        CryptoNetwork.POLYGON -> "https://polygonscan.com/tx/${if (clean.startsWith("0x")) clean else "0x$clean"}"
        CryptoNetwork.BINANCE_PAY -> null
    }
}

