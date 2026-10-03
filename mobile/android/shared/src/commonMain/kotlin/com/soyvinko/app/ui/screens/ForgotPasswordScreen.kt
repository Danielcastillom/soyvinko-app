package com.soyvinko.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.soyvinko.app.ui.ForgotViewModel
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.FieldSpacer
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.components.VinkoTextField

@Composable
fun ForgotPasswordScreen(vm: ForgotViewModel, onBack: () -> Unit) {
    AuthScaffold(
        title = "Recupera tu acceso",
        subtitle = "Te enviaremos un enlace para crear una contraseña nueva.",
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
            vm.email, { vm.email = it }, "Correo de tu cuenta",
            leadingIcon = Icons.Outlined.Email, keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done, onImeAction = vm::submit,
        )
        PrimaryButton("Enviar enlace", onClick = vm::submit, loading = vm.loading)
    }
}
