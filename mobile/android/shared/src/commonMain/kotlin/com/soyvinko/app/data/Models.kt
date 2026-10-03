package com.soyvinko.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Usuario tal como lo devuelve la API (`auth_principal`). */
@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String,
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("account_type") val accountType: String = "natural",
    @SerialName("business_name") val businessName: String? = null,
    val rif: String? = null,
    @SerialName("contact_name") val contactName: String? = null,
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
) {
    val firstName: String get() = name.trim().substringBefore(" ")
    val isProfessional: Boolean get() = "professional" in roles
    val isCompany: Boolean get() = accountType == "juridico"
    /** Personal de SoyVinko: misma regla que la web (Protected admin). */
    val isStaff: Boolean get() = roles.any { it in setOf("admin", "superadmin", "support") }
    fun can(permission: String): Boolean = permission in permissions || "superadmin" in roles
}

/** Respuesta de login, registro y renovación. Con `status = code_required` aún no hay sesión. */
@Serializable
data class AuthPayload(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null,
    val user: User? = null,
    val status: String? = null,
    val message: String? = null,
    @SerialName("verification_email_sent") val verificationEmailSent: Boolean? = null,
)

@Serializable
data class Dashboard(
    val requests: Int = 0,
    @SerialName("active_contracts") val activeContracts: Int = 0,
    @SerialName("completed_contracts") val completedContracts: Int = 0,
    @SerialName("credit_balance") val creditBalance: Int? = null,
    @SerialName("unread_notifications") val unreadNotifications: Int = 0,
)

@Serializable
data class MessagePayload(val message: String? = null)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val code: String? = null,
    val client: String = "native",
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerialName("confirm_password") val confirmPassword: String,
    val name: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("business_name") val businessName: String? = null,
    val rif: String? = null,
    @SerialName("contact_name") val contactName: String? = null,
    val client: String = "native",
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
    val client: String = "native",
)

@Serializable
data class EmailRequest(val email: String)

/** Formato de error común de la API: {"error": {"code", "message", "details": [{field, message}]}}. */
@Serializable
data class ErrorEnvelope(val error: ApiErrorBody? = null, val detail: String? = null)

@Serializable
data class ApiErrorBody(
    val code: String = "error",
    val message: String = "No se pudo completar la operación.",
    @SerialName("request_id") val requestId: String? = null,
    val details: List<FieldErrorBody> = emptyList(),
)

@Serializable
data class FieldErrorBody(val field: String, val message: String)
