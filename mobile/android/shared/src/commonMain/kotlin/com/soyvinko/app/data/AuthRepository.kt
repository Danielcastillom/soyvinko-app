package com.soyvinko.app.data

import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer

sealed interface LoginOutcome {
    data class CodeRequired(val email: String, val message: String) : LoginOutcome
    data class LoggedIn(val user: User) : LoginOutcome
}

class AuthRepository(private val api: ApiClient, private val storage: TokenStorage) {

    /** Usuario guardado de la última sesión, para mostrar la app sin esperar a la red. */
    fun cachedUser(): User? = storage.cachedUserJson?.let {
        runCatching { ApiJson.decodeFromString(User.serializer(), it) }.getOrNull()
    }

    fun hasSession(): Boolean = storage.refreshToken != null

    /** Indica si el usuario dejó configurado el acceso biométrico en este dispositivo. */
    fun isBiometricEnabled(): Boolean = storage.biometricEnabled && storage.biometricRefreshToken != null

    fun biometricEmail(): String? = storage.biometricEmail

    fun isBiometricLockOnLeave(): Boolean = storage.biometricLockOnLeave

    fun setBiometricLockOnLeave(enabled: Boolean) {
        storage.biometricLockOnLeave = enabled
    }

    /**
     * Activa y deja configurado el acceso biométrico para la cuenta actual.
     */
    fun enableBiometric(email: String, refreshToken: String? = storage.refreshToken, lockOnLeave: Boolean = true) {
        storage.biometricEnabled = true
        storage.biometricEmail = email
        if (refreshToken != null) {
            storage.biometricRefreshToken = refreshToken
        }
        storage.biometricLockOnLeave = lockOnLeave
    }

    /**
     * Desactiva el acceso biométrico y remueve las credenciales biométricas guardadas.
     */
    fun disableBiometric() {
        storage.biometricEnabled = false
        storage.biometricEmail = null
        storage.biometricRefreshToken = null
    }

    /**
     * Inicia sesión utilizando las credenciales biométricas guardadas sin pedir contraseña.
     */
    suspend fun loginWithBiometrics(): User {
        val bioRefresh = storage.biometricRefreshToken
            ?: throw ApiException("No hay acceso biométrico configurado. Inicia sesión con tu contraseña.", "biometric_not_configured", 401)
        storage.refreshToken = bioRefresh
        return try {
            val payload = api.refreshSession(staleToken = null)
            val user = payload.user ?: me()
            payload.refreshToken?.let {
                storage.biometricRefreshToken = it
                storage.refreshToken = it
            }
            storage.biometricEmail = user.email
            user
        } catch (e: ApiException) {
            if (e.isUnauthorized || e.status == 403) {
                storage.biometricRefreshToken = null
                storage.biometricEnabled = false
                throw ApiException("Tu sesión biométrica expiró. Ingresa tu contraseña para reactivarla.", "session_expired", 401)
            }
            if (e.isNetwork) {
                cachedUser() ?: throw e
            } else {
                throw e
            }
        }
    }

    /** Recupera la sesión guardada; devuelve null si ya no es válida. */
    suspend fun restore(): User? {
        if (!hasSession()) return null
        return try {
            api.refreshSession(staleToken = null).user ?: me()
        } catch (e: ApiException) {
            if (e.isNetwork) cachedUser() else null
        }
    }

    /**
     * Paso 1 (sin código): el servidor valida la contraseña y envía un código de 6 dígitos al correo.
     * Paso 2 (con código): devuelve la sesión.
     */
    suspend fun login(email: String, password: String, code: String? = null): LoginOutcome {
        val payload = api.post(
            "/api/v1/auth/login",
            LoginRequest(email.trim(), password, code?.trim()),
            LoginRequest.serializer(),
            AuthPayload.serializer(),
            auth = false,
        )
        if (payload.status == "code_required" || payload.accessToken == null) {
            return LoginOutcome.CodeRequired(
                email.trim(),
                payload.message ?: "Te enviamos un código de 6 dígitos a tu correo.",
            )
        }
        return LoginOutcome.LoggedIn(saveSession(payload))
    }

    suspend fun resendCode(email: String, password: String): String =
        api.post(
            "/api/v1/auth/resend-login-code",
            LoginRequest(email.trim(), password),
            LoginRequest.serializer(),
            AuthPayload.serializer(),
            auth = false,
        ).message ?: "Te enviamos un nuevo código."

    suspend fun register(request: RegisterRequest): User =
        saveSession(
            api.post("/api/v1/auth/register", request, RegisterRequest.serializer(), AuthPayload.serializer(), auth = false),
        )

    suspend fun forgotPassword(email: String): String =
        api.post(
            "/api/v1/auth/forgot-password",
            EmailRequest(email.trim()),
            EmailRequest.serializer(),
            MessagePayload.serializer(),
            auth = false,
        ).message ?: "Si la cuenta existe, recibirás instrucciones en tu correo."

    suspend fun me(): User = api.get("/api/v1/auth/me", User.serializer()).also {
        storage.cachedUserJson = ApiJson.encodeToString(User.serializer(), it)
    }

    suspend fun dashboard(): Dashboard = api.get("/api/v1/account/dashboard", Dashboard.serializer())

    suspend fun sendVerificationEmail(): String =
        api.post<Unit, MessagePayload>(
            "/api/v1/auth/verification-email", null, null, MessagePayload.serializer(),
        ).message ?: "Te enviamos un enlace de verificación."

    /** Revoca la sesión en el servidor (si hay red) y borra siempre los datos locales. */
    suspend fun logout() {
        val refresh = storage.refreshToken
        try {
            if (refresh != null) {
                api.post(
                    "/api/v1/auth/logout",
                    RefreshRequest(refresh),
                    RefreshRequest.serializer(),
                    String.serializer().nullable,
                    auth = false,
                )
            }
        } catch (_: ApiException) {
            // Sin red o sesión ya revocada: igual se cierra localmente.
        } finally {
            api.accessToken = null
            storage.clear()
        }
    }

    private fun saveSession(payload: AuthPayload): User {
        val user = payload.user ?: throw ApiException("No recibimos los datos de tu cuenta.", "bad_response", 200)
        api.accessToken = payload.accessToken
        payload.refreshToken?.let {
            storage.refreshToken = it
            if (storage.biometricEnabled) {
                storage.biometricRefreshToken = it
                storage.biometricEmail = user.email
            }
        }
        storage.cachedUserJson = ApiJson.encodeToString(User.serializer(), user)
        return user
    }
}
