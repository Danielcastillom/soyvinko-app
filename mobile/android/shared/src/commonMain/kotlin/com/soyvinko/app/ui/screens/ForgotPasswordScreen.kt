package com.soyvinko.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.soyvinko.app.ui.ForgotViewModel
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.FieldSpacer
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.components.VinkoTextField

/**
 * Pantalla de recuperación de contraseña con diseño estilizado y claro.
 */
@Composable
fun ForgotPasswordScreen(vm: ForgotViewModel, onBack: () -> Unit) {
    AuthScaffold(
        title = "¿Olvidaste tu contraseña?",
        subtitle = "Te enviaremos un enlace a tu correo para restablecer tu acceso.",
        onBack = onBack,
    ) {
        val sent = vm.sent
        if (sent != null) {
            Banner(sent, BannerTone.Success)
            FieldSpacer()
            SecondaryButton("Volver a iniciar sesión", onClick = onBack)
            return@AuthScaffold
        }
        vm.error?.let { Banner(it); FieldSpacer() }
        VinkoTextField(
            value = vm.email,
            onValueChange = { vm.email = it },
            label = "Correo Electrónico",
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onImeAction = vm::submit,
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Enviar enlace de recuperación", onClick = vm::submit, loading = vm.loading)
        Spacer(Modifier.height(8.dp))
        SecondaryButton("Volver atrás", onClick = onBack)
    }
}
