package com.soyvinko.app.biometric

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import coil3.compose.LocalPlatformContext

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

class AndroidBiometricManagerImpl(
    private val context: Context,
) : BiometricManager {

    override fun checkStatus(): BiometricStatus {
        val manager = AndroidBiometricManager.from(context)
        val authenticators = AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
            AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (manager.canAuthenticate(authenticators)) {
            AndroidBiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            AndroidBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled
            else -> BiometricStatus.NotAvailable
        }
    }

    override fun authenticate(
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onResult: (BiometricResult) -> Unit,
    ) {
        val activity = context.findFragmentActivity()
        if (activity == null) {
            onResult(BiometricResult.Error("No se pudo iniciar la autenticación biométrica en este momento."))
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onResult(BiometricResult.Canceled)
                } else {
                    onResult(BiometricResult.Error(errString.toString()))
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // El diálogo de Android gestiona visualmente el reintento de huella
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                if (subtitle.isNotBlank()) setSubtitle(subtitle)
            }
            .setNegativeButtonText(negativeButtonText.ifBlank { "Cancelar" })
            .setAllowedAuthenticators(
                AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
                    AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK,
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}

@Composable
actual fun rememberBiometricManager(): BiometricManager {
    val context = LocalPlatformContext.current
    return remember(context) { AndroidBiometricManagerImpl(context) }
}
