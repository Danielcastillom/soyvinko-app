package com.soyvinko.app.data

/**
 * Guarda el refresh token (30 días, rotativo) y el último usuario. Cada plataforma lo cifra:
 * Android Keystore (EncryptedSharedPreferences) e iOS Keychain. El access token vive solo en memoria.
 */
interface TokenStorage {
    var refreshToken: String?
    var cachedUserJson: String?
    var avatarFileId: String?
        get() = null
        set(_) {}
    var appMode: String?
        get() = null
        set(_) {}
    var biometricEnabled: Boolean
        get() = false
        set(_) {}
    var biometricEmail: String?
        get() = null
        set(_) {}
    var biometricRefreshToken: String?
        get() = null
        set(_) {}
    var biometricLockOnLeave: Boolean
        get() = false
        set(_) {}

    fun clear()
    fun clear(clearBiometrics: Boolean) {
        clear()
    }
}

/** Respaldo sin persistencia (pruebas, o si el almacén cifrado no está disponible). */
class MemoryTokenStorage : TokenStorage {
    override var refreshToken: String? = null
    override var cachedUserJson: String? = null
    override var avatarFileId: String? = null
    override var appMode: String? = null
    override var biometricEnabled: Boolean = false
    override var biometricEmail: String? = null
    override var biometricRefreshToken: String? = null
    override var biometricLockOnLeave: Boolean = false

    override fun clear() {
        clear(clearBiometrics = false)
    }

    override fun clear(clearBiometrics: Boolean) {
        refreshToken = null
        cachedUserJson = null
        avatarFileId = null
        appMode = null
        if (clearBiometrics) {
            biometricEnabled = false
            biometricEmail = null
            biometricRefreshToken = null
            biometricLockOnLeave = false
        }
    }
}

/** Almacén cifrado de la plataforma; si falla (Keystore/Keychain dañado), usa memoria. */
expect fun platformTokenStorage(): TokenStorage
