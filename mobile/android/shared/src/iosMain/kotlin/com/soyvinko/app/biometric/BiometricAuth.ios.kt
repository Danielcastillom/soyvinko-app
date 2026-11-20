package com.soyvinko.app.biometric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosBiometricManagerImpl : BiometricManager {

    override fun checkStatus(): BiometricStatus {
        val context = LAContext()
        val canEvaluate = context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error = null,
        )
        return if (canEvaluate) {
            BiometricStatus.Available
        } else {
            BiometricStatus.NotAvailable
        }
    }

    override fun authenticate(
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onResult: (BiometricResult) -> Unit,
    ) {
        val context = LAContext()
        if (negativeButtonText.isNotBlank()) {
            context.localizedCancelTitle = negativeButtonText
        }
        val reason = subtitle.ifBlank { title }

        context.evaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = reason,
        ) { success, error ->
            dispatch_async(dispatch_get_main_queue()) {
                if (success) {
                    onResult(BiometricResult.Success)
                } else if (error != null) {
                    val desc = error.localizedDescription
                    if (desc.contains("cancel", ignoreCase = true)) {
                        onResult(BiometricResult.Canceled)
                    } else {
                        onResult(BiometricResult.Error(desc))
                    }
                } else {
                    onResult(BiometricResult.Canceled)
                }
            }
        }
    }
}

@Composable
actual fun rememberBiometricManager(): BiometricManager = remember { IosBiometricManagerImpl() }
