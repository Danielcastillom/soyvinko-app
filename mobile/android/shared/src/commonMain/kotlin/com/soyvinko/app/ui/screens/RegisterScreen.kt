package com.soyvinko.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.RegisterViewModel
import com.soyvinko.app.ui.components.AppleLogo
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.FieldSpacer
import com.soyvinko.app.ui.components.GoogleLogo
import com.soyvinko.app.ui.components.OrDivider
import com.soyvinko.app.ui.components.PasswordChecklist
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.components.SocialAuthButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.theme.Vinko

/**
 * Pantalla de registro moderna alineada con el prototipo FlutterFlow de Vinko:
 * selector de tipo de cuenta, formulario limpio con validación en vivo,
 * opciones sociales y aceptación de términos.
 */
@Composable
fun RegisterScreen(
    vm: RegisterViewModel,
    onRegistered: (User) -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
    onLegal: (String) -> Unit = {},
) {
    AuthScaffold(
        title = "Crea tu cuenta",
        subtitle = "Únete a la app que vincula a Venezuela",
        onBack = onBack,
    ) {
        SectionLabel("Tipo de cuenta")
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(12.dp)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("natural" to "Persona", "juridico" to "Empresa").forEach { (value, label) ->
                val selected = vm.accountType == value
                Text(
                    text = label,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { vm.accountType = value }
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Vinko.Primary else Vinko.Muted,
                )
            }
        }

        FieldSpacer()
        vm.error?.let { Banner(it); FieldSpacer() }

        if (vm.isCompany) {
            VinkoTextField(
                value = vm.businessName,
                onValueChange = { vm.businessName = it },
                label = "Razón social",
                leadingIcon = Icons.Outlined.Business,
                capitalization = KeyboardCapitalization.Words,
                error = vm.fieldErrors["business_name"],
            )
            VinkoTextField(
                value = vm.rif,
                onValueChange = { vm.rif = it.uppercase() },
                label = "RIF",
                leadingIcon = Icons.Outlined.Badge,
                capitalization = KeyboardCapitalization.Characters,
                supporting = "Ejemplo: J-12345678-9",
                error = vm.fieldErrors["rif"],
            )
            VinkoTextField(
                value = vm.contactName,
                onValueChange = { vm.contactName = it },
                label = "Persona de contacto",
                leadingIcon = Icons.Outlined.Person,
                capitalization = KeyboardCapitalization.Words,
                error = vm.fieldErrors["contact_name"] ?: vm.fieldErrors["name"],
            )
        } else {
            VinkoTextField(
                value = vm.name,
                onValueChange = { vm.name = it },
                label = "Nombre y apellido",
                leadingIcon = Icons.Outlined.Person,
                capitalization = KeyboardCapitalization.Words,
                error = vm.fieldErrors["name"],
            )
        }

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
            error = vm.fieldErrors["password"],
        )

        if (vm.password.isNotEmpty()) {
            PasswordChecklist(vm.password)
            FieldSpacer()
        }

        VinkoTextField(
            value = vm.confirm,
            onValueChange = { vm.confirm = it },
            label = "Confirma la contraseña",
            leadingIcon = Icons.Outlined.Lock,
            password = true,
            imeAction = ImeAction.Done,
            onImeAction = { vm.submit(onRegistered) },
            error = vm.fieldErrors["confirm_password"],
        )

        // Checkbox de términos y condiciones
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { vm.acceptedTerms = !vm.acceptedTerms }.padding(vertical = 4.dp),
        ) {
            Checkbox(
                checked = vm.acceptedTerms,
                onCheckedChange = { vm.acceptedTerms = it },
                colors = CheckboxDefaults.colors(checkedColor = Vinko.Primary),
            )
            Text(
                text = "Acepto los términos de servicio y la política de privacidad.",
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Muted,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = { onLegal("terminos") }) {
                Text("Ver términos", color = Vinko.Primary, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { onLegal("privacidad") }) {
                Text("Ver privacidad", color = Vinko.Primary, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(6.dp))

        PrimaryButton(
            text = "Crear Cuenta",
            onClick = { vm.submit(onRegistered) },
            loading = vm.loading,
        )

        // Separador social "Ó"
        OrDivider()

        SocialAuthButton(
            text = "Registrarse con Google",
            icon = { GoogleLogo() },
            onClick = { /* Anclaje Google Sign-Up */ },
        )

        Spacer(Modifier.height(8.dp))

        SocialAuthButton(
            text = "Registrarse con Apple",
            icon = { AppleLogo() },
            onClick = { /* Anclaje Apple Sign-Up */ },
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "¿Ya tienes cuenta?",
                style = MaterialTheme.typography.bodyMedium,
                color = Vinko.Muted,
            )
            TextButton(onClick = onLogin) {
                Text(
                    text = "Inicia sesión",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Vinko.Primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
