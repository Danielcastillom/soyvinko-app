package com.soyvinko.app.data

/**
 * Guarda el refresh token (30 días, rotativo) y el último usuario. Cada plataforma lo cifra:
 * Android Keystore (EncryptedSharedPreferences) e iOS Keychain. El access token vive solo en memoria.
 */
interface TokenStorage {
    var refreshToken: String?
    var cachedUserJson: String?
    fun clear()
}

/** Respaldo sin persistencia (pruebas, o si el almacén cifrado no está disponible). */
class MemoryTokenStorage : TokenStorage {
    override var refreshToken: String? = null
    override var cachedUserJson: String? = null
    override fun clear() {
        refreshToken = null
        cachedUserJson = null
    }
}

/** Almacén cifrado de la plataforma; si falla (Keystore/Keychain dañado), usa memoria. */
expect fun platformTokenStorage(): TokenStorage
