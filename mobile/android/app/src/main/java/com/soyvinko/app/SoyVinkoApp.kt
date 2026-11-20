package com.soyvinko.app

import android.app.Application
import android.os.Build
import com.soyvinko.app.data.ApiClient
import com.soyvinko.app.data.AppInfo
import com.soyvinko.app.data.AppServices
import com.soyvinko.app.data.AuthRepository
import com.soyvinko.app.data.Ops
import com.soyvinko.app.platform.AndroidPlatform

/** Arranque de Android: prepara la capa de plataforma y los servicios compartidos con iOS (módulo shared). */
class SoyVinkoApp : Application() {
    val services: AppServices get() = AppGraph.services
    val api: ApiClient get() = services.api
    val auth: AuthRepository get() = services.auth
    val ops: Ops get() = services.ops

    override fun onCreate() {
        super.onCreate()
        configure(BuildConfig.API_BASE_URL)
    }

    /** También lo usan las pruebas para apuntar a un servidor simulado. */
    fun configure(baseUrl: String) {
        AndroidPlatform.init(this, BuildConfig.VERSION_NAME)
        // El servidor guarda este texto como nombre del dispositivo de la sesión.
        val agent = "SoyVinko-Android/${BuildConfig.VERSION_NAME} (${Build.MANUFACTURER} ${Build.MODEL}; Android ${Build.VERSION.RELEASE})"
        val info = AppInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            apiBaseUrl = baseUrl,
            webBaseUrl = BuildConfig.WEB_BASE_URL,
            userAgent = agent,
            platform = "Android",
        )
        AppGraph.configure(AppServices(info))
    }
}
