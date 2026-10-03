package com.soyvinko.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.LoginViewModel
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.CodeInput
import com.soyvinko.app.ui.components.FieldSpacer
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.theme.Vinko

@Composable
fun LoginScreen(
    vm: LoginViewModel,
    onLoggedIn: (User) -> Unit,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
    onBack: () -> Unit,
) {
    if (vm.step == LoginViewModel.Step.Code) {
        CodeStep(vm, onLoggedIn)
        return
    }
    AuthScaffold(
        title = "Hola de nuevo",
        subtitle = "Inicia sesión para seguir tus servicios, pagos y mensajes.",
        onBack = onBack,
    ) {
        vm.error?.let { Banner(it); FieldSpacer() }
        VinkoTextField(
            value = vm.email,
            onValueChange = { vm.email = it },
            label = "Correo electrónico",
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            error = vm.fieldErrors["email"],
        )
        VinkoTextField(
            value = vm.password,
            onValueChange = { vm.password = it },
            label = "Contraseña",
            leadingIcon = Icons.Outlined.Lock,
            password = true,
            imeAction = ImeAction.Done,
            onImeAction = { vm.submitCredentials(onLoggedIn) },
            error = vm.fieldErrors["password"],
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgot) { Text("¿Olvidaste tu contraseña?", color = Vinko.Primary) }
        }
        PrimaryButton("Continuar", onClick = { vm.submitCredentials(onLoggedIn) }, loading = vm.loading)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("¿No tienes cuenta?", style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
            TextButton(onClick = onRegister) { Text("Regístrate", color = Vinko.Primary, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun CodeStep(vm: LoginViewModel, onLoggedIn: (User) -> Unit) {
    AuthScaffold(
        title = "Revisa tu correo",
        subtitle = "Escribe el código de 6 dígitos que enviamos a ${vm.email}.",
        onBack = vm::back,
    ) {
        vm.error?.let { Banner(it); FieldSpacer() } ?: vm.info?.let { Banner(it, BannerTone.Info); FieldSpacer() }
        CodeInput(
            value = vm.code,
            onValueChange = { vm.code = it },
            onComplete = { vm.submitCode(onLoggedIn) },
            error = vm.error != null,
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Verificar e ingresar", onClick = { vm.submitCode(onLoggedIn) }, loading = vm.loading, enabled = vm.code.length == 6)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = vm::resend, enabled = vm.resendIn == 0 && !vm.loading) {
                Text(if (vm.resendIn > 0) "Reenviar código en ${vm.resendIn} s" else "Reenviar código")
            }
        }
        Text(
            "El código vence en pocos minutos. Si no lo ves, revisa la carpeta de spam.",
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Muted,
        )
    }
}
