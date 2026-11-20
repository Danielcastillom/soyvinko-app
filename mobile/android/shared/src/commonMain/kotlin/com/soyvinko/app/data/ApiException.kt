package com.soyvinko.app.data

/**
 * Error presentable al usuario. `fieldErrors` usa el nombre del campo sin el prefijo "body."
 * (p. ej. "email", "password") para mostrarlo bajo el campo correspondiente.
 */
class ApiException(
    override val message: String,
    val code: String = "error",
    val status: Int = 0,
    val fieldErrors: Map<String, String> = emptyMap(),
) : Exception(message) {
    val isNetwork: Boolean get() = status == 0
    val isUnauthorized: Boolean get() = status == 401
}
