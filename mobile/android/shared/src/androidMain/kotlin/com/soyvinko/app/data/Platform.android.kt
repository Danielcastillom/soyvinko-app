package com.soyvinko.app.data

import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.soyvinko.app.platform.AndroidPlatform
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun platformHttpEngine(): HttpClientEngine = OkHttp.create()

actual fun platformTokenStorage(): TokenStorage = try {
    SessionStore()
} catch (e: Exception) {
    // Keystore dañado o no disponible: la sesión funciona pero no se guarda entre aperturas.
    Log.w("SoyVinko", "Almacenamiento cifrado no disponible", e)
    MemoryTokenStorage()
}

/** Refresh token cifrado con una clave del Android Keystore. */
class SessionStore : TokenStorage {
    private val prefs = EncryptedSharedPreferences.create(
        AndroidPlatform.context,
        "soyvinko_session",
        MasterKey.Builder(AndroidPlatform.context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_REFRESH) else putString(KEY_REFRESH, value) }.apply()

    override var cachedUserJson: String?
        get() = prefs.getString(KEY_USER, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_USER) else putString(KEY_USER, value) }.apply()

    override var avatarFileId: String?
        get() = prefs.getString(KEY_AVATAR, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_AVATAR) else putString(KEY_AVATAR, value) }.apply()

    override var appMode: String?
        get() = prefs.getString(KEY_APP_MODE, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_APP_MODE) else putString(KEY_APP_MODE, value) }.apply()

    override var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    override var biometricEmail: String?
        get() = prefs.getString(KEY_BIOMETRIC_EMAIL, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_BIOMETRIC_EMAIL) else putString(KEY_BIOMETRIC_EMAIL, value) }.apply()

    override var biometricRefreshToken: String?
        get() = prefs.getString(KEY_BIOMETRIC_REFRESH, null)
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_BIOMETRIC_REFRESH) else putString(KEY_BIOMETRIC_REFRESH, value) }.apply()

    override var biometricLockOnLeave: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_LOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, value).apply()

    override fun clear() {
        clear(clearBiometrics = false)
    }

    override fun clear(clearBiometrics: Boolean) {
        if (clearBiometrics) {
            prefs.edit().clear().apply()
        } else {
            val bioEnabled = biometricEnabled
            val bioEmail = biometricEmail
            val bioRefresh = biometricRefreshToken
            val bioLock = biometricLockOnLeave
            prefs.edit().clear().apply()
            if (bioEnabled) {
                biometricEnabled = true
                biometricEmail = bioEmail
                biometricRefreshToken = bioRefresh
                biometricLockOnLeave = bioLock
            }
        }
    }

    private companion object {
        const val KEY_REFRESH = "refresh_token"
        const val KEY_USER = "user"
        const val KEY_AVATAR = "avatar_file_id"
        const val KEY_APP_MODE = "app_mode"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_BIOMETRIC_EMAIL = "biometric_email"
        const val KEY_BIOMETRIC_REFRESH = "biometric_refresh"
        const val KEY_BIOMETRIC_LOCK = "biometric_lock"
    }
}
