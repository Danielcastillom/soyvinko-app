package com.soyvinko.app.biometric

import androidx.compose.runtime.Composable

/**
 * Estado de soporte de autenticación biométrica en el dispositivo.
 */
enum class BiometricStatus {
    /** Hardware biométrico disponible y huella/rostro configurados. */
    Available,
    /** El dispositivo tiene sensor pero el usuario no ha registrado ninguna huella o rostro en los ajustes del sistema. */
    NotEnrolled,
    /** El dispositivo no cuenta con sensor biométrico o no es compatible. */
    NotAvailable;

    val isAvailable: Boolean get() = this == Available
}

/**
 * Resultado de una solicitud de autenticación biométrica.
 */
sealed interface BiometricResult {
    /** Autenticación exitosa (huella o rostro reconocido). */
    data object Success : BiometricResult
    /** El usuario canceló el diálogo biométrico. */
    data object Canceled : BiometricResult
    /** Fallo o error al verificar la identidad biométrica. */
    data class Error(val message: String) : BiometricResult
}

/**
 * Controlador de autenticación biométrica multiplataforma (Android BiometricPrompt / iOS Touch ID / Face ID).
 */
interface BiometricManager {
    /** Verifica si el dispositivo soporta biometría y si hay credenciales registradas. */
    fun checkStatus(): BiometricStatus

    /**
     * Muestra el diálogo nativo de biometría del sistema operativo.
     */
    fun authenticate(
        title: String = "Acceso biométrico",
        subtitle: String = "Confirma tu identidad para continuar",
        negativeButtonText: String = "Cancelar",
        onResult: (BiometricResult) -> Unit,
    )
}

/**
 * Hook composable para obtener el [BiometricManager] contextual en Android e iOS.
 */
@Composable
expect fun rememberBiometricManager(): BiometricManager
