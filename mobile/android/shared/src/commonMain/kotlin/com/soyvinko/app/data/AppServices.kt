package com.soyvinko.app.data

/** Datos de la app que cada plataforma entrega al arrancar. */
data class AppInfo(
    val versionName: String,
    val versionCode: Int,
    val apiBaseUrl: String,
    val webBaseUrl: String,
    /** Se guarda en el servidor como nombre del dispositivo de la sesión. */
    val userAgent: String,
    val platform: String,
)

/** Servicios compartidos por todas las pantallas: `LocalApp.current.ops`, `.api`, `.auth`. */
class AppServices(val info: AppInfo, val storage: TokenStorage = platformTokenStorage()) {
    val api = ApiClient(info.apiBaseUrl, storage, info.userAgent)
    val auth = AuthRepository(api, storage)
    val ops = Ops(api)

    var avatarFileId: String?
        get() = storage.avatarFileId ?: auth.cachedUser()?.avatarFileId
        set(value) { storage.avatarFileId = value }
}
