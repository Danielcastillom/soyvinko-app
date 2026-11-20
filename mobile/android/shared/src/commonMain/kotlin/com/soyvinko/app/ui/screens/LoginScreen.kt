package com.soyvinko.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.LoginViewModel
import com.soyvinko.app.ui.components.AppleLogo
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.CodeInput
import com.soyvinko.app.ui.components.FieldSpacer
import com.soyvinko.app.ui.components.GoogleLogo
import com.soyvinko.app.ui.components.OrDivider
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.biometric.BiometricResult
import com.soyvinko.app.biometric.rememberBiometricManager
import com.soyvinko.app.ui.components.SocialAuthButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.theme.Vinko

/**
 * Pantalla de inicio de sesión idéntica en estructura y elegancia visual al prototipo
 * FlutterFlow oficial de Vinko: cabecera limpia, inputs redondeados, acceso biométrico,
 * botones sociales, recuperación de clave y consentimiento legal transparente.
 */
@Composable
fun LoginScreen(
    vm: LoginViewModel,
    onLoggedIn: (User) -> Unit,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
    onBack: () -> Unit,
    onLegal: (String) -> Unit = {},
) {
    if (vm.step == LoginViewModel.Step.Code) {
        CodeStep(vm, onLoggedIn)
        return
    }

    val biometric = rememberBiometricManager()
    val biometricStatus = remember { biometric.checkStatus() }
    var autoPrompted by remember { mutableStateOf(false) }

    val triggerBiometricAuth: () -> Unit = {
        biometric.authenticate(
            title = "Iniciar sesión en SoyVinko",
            subtitle = "Confirma tu identidad para ingresar",
            onResult = { result ->
                if (result is BiometricResult.Success) {
                    vm.loginWithBiometrics(onLoggedIn)
                }
            },
        )
    }

    LaunchedEffect(vm.isBiometricAvailable, biometricStatus) {
        if (vm.isBiometricAvailable && biometricStatus.isAvailable && !autoPrompted) {
            autoPrompted = true
            triggerBiometricAuth()
        }
    }

    AuthScaffold(
        title = "Bienvenid@",
        subtitle = "Ingresa tus datos para continuar",
        onBack = onBack,
    ) {
        vm.error?.let { Banner(it); FieldSpacer() }

        if (vm.isBiometricAvailable && biometricStatus.isAvailable) {
            BiometricLoginBanner(
                email = vm.biometricEmail,
                loading = vm.loading,
                onClick = triggerBiometricAuth,
            )
            FieldSpacer()
            OrDivider()
            FieldSpacer()
        }

        // Campo 1: Correo Electrónico
        VinkoTextField(
            value = vm.email,
            onValueChange = { vm.email = it },
            label = "Correo Electrónico",
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            error = vm.fieldErrors["email"],
        )

        // Campo 2: Contraseña
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

        // Olvidó su contraseña
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onForgot) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Vinko.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Botón principal de acceso
        PrimaryButton(
            text = "Iniciar Sesión",
            onClick = { vm.submitCredentials(onLoggedIn) },
            loading = vm.loading,
        )

        // Separador social "Ó"
        OrDivider()

        // Botones sociales
        SocialAuthButton(
            text = "Continúa con tu cuenta Google",
            icon = { GoogleLogo() },
            onClick = {
                // Punto de anclaje para Google OAuth nativo
                vm.submitCredentials(onLoggedIn)
            },
        )

        Spacer(Modifier.height(8.dp))

        SocialAuthButton(
            text = "Continúa con tu cuenta Apple",
            icon = { AppleLogo() },
            onClick = {
                // Punto de anclaje para Apple Sign-In
            },
        )

        Spacer(Modifier.height(16.dp))

        // Enlace para crear cuenta
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "¿Aún no tienes cuenta?",
                style = MaterialTheme.typography.bodyMedium,
                color = Vinko.Muted,
            )
            TextButton(onClick = onRegister) {
                Text(
                    text = "Regístrate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Vinko.Primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Disclaimer legal idéntico al prototipo FlutterFlow
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Al iniciar sesión estás aceptando nuestros ")
                    withStyle(SpanStyle(color = Vinko.Primary, fontWeight = FontWeight.SemiBold)) {
                        append("Términos y Condiciones")
                    }
                    append(" y nuestras ")
                    withStyle(SpanStyle(color = Vinko.Primary, fontWeight = FontWeight.SemiBold)) {
                        append("Políticas de Privacidad")
                    }
                    append(".")
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                color = Vinko.Subtle,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { onLegal("terminos") },
            )
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
        PrimaryButton(
            text = "Verificar e ingresar",
            onClick = { vm.submitCode(onLoggedIn) },
            loading = vm.loading,
            enabled = vm.code.length == 6,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = vm::resend, enabled = vm.resendIn == 0 && !vm.loading) {
                Text(if (vm.resendIn > 0) "Reenviar código en ${vm.resendIn} s" else "Reenviar código")
            }
        }
        Text(
            text = "El código vence en pocos minutos. Si no lo ves, revisa la carpeta de spam.",
            style = MaterialTheme.typography.bodySmall,
            color = Vinko.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BiometricLoginBanner(
    email: String?,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = !loading,
        shape = RoundedCornerShape(16.dp),
        color = Vinko.Tint,
        border = BorderStroke(1.dp, Vinko.Primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Vinko.Primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = "Acceso biométrico",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Acceso biométrico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Vinko.Ink,
                )
                Text(
                    text = email?.let { "Continuar como $it" } ?: "Ingresa con tu huella o Face ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Muted,
                )
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Vinko.Primary,
                )
            } else {
                Text(
                    text = "Entrar",
                    style = MaterialTheme.typography.labelLarge,
                    color = Vinko.Primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

