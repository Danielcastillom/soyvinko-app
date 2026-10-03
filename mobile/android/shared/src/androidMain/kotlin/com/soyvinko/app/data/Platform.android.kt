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

    override fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_REFRESH = "refresh_token"
        const val KEY_USER = "user"
    }
}
