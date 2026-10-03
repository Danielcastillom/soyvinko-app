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
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.FieldSpacer
import com.soyvinko.app.ui.components.PasswordChecklist
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.theme.Vinko

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
        subtitle = "Gratis. Contrata servicios hoy y, cuando quieras, ofrece los tuyos.",
        onBack = onBack,
    ) {
        SectionLabel("Tipo de cuenta")
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().background(Vinko.Tint, RoundedCornerShape(14.dp)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("natural" to "Persona", "juridico" to "Empresa").forEach { (value, label) ->
                val selected = vm.accountType == value
                Text(
                    label,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(11.dp))
                        .clickable { vm.accountType = value }
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Vinko.Primary else Vinko.Muted,
                )
            }
        }
        FieldSpacer()
        vm.error?.let { Banner(it); FieldSpacer() }

        if (vm.isCompany) {
            VinkoTextField(vm.businessName, { vm.businessName = it }, "Razón social", leadingIcon = Icons.Outlined.Business,
                capitalization = KeyboardCapitalization.Words, error = vm.fieldErrors["business_name"])
            VinkoTextField(vm.rif, { vm.rif = it.uppercase() }, "RIF", leadingIcon = Icons.Outlined.Badge,
                capitalization = KeyboardCapitalization.Characters, supporting = "Ejemplo: J-12345678-9", error = vm.fieldErrors["rif"])
            VinkoTextField(vm.contactName, { vm.contactName = it }, "Persona de contacto", leadingIcon = Icons.Outlined.Person,
                capitalization = KeyboardCapitalization.Words, error = vm.fieldErrors["contact_name"] ?: vm.fieldErrors["name"])
        } else {
            VinkoTextField(vm.name, { vm.name = it }, "Nombre y apellido", leadingIcon = Icons.Outlined.Person,
                capitalization = KeyboardCapitalization.Words, error = vm.fieldErrors["name"])
        }
        VinkoTextField(vm.email, { vm.email = it }, "Correo electrónico", leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email, error = vm.fieldErrors["email"])
        VinkoTextField(vm.password, { vm.password = it }, "Contraseña", leadingIcon = Icons.Outlined.Lock,
            password = true, error = vm.fieldErrors["password"])
        if (vm.password.isNotEmpty()) {
            PasswordChecklist(vm.password)
            FieldSpacer()
        }
        VinkoTextField(vm.confirm, { vm.confirm = it }, "Confirma la contraseña", leadingIcon = Icons.Outlined.Lock,
            password = true, imeAction = ImeAction.Done, onImeAction = { vm.submit(onRegistered) },
            error = vm.fieldErrors["confirm_password"])

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { vm.acceptedTerms = !vm.acceptedTerms }) {
            Checkbox(
                checked = vm.acceptedTerms,
                onCheckedChange = { vm.acceptedTerms = it },
                colors = CheckboxDefaults.colors(checkedColor = Vinko.Primary),
            )
            Text("Acepto los términos de servicio y la política de privacidad.", style = MaterialTheme.typography.bodySmall, color = Vinko.Muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { onLegal("terminos") }) {
                Text("Ver términos", color = Vinko.Primary)
            }
            TextButton(onClick = { onLegal("privacidad") }) {
                Text("Ver privacidad", color = Vinko.Primary)
            }
        }
        PrimaryButton("Crear mi cuenta", onClick = { vm.submit(onRegistered) }, loading = vm.loading)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("¿Ya tienes cuenta?", style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted)
            TextButton(onClick = onLogin) { Text("Inicia sesión", color = Vinko.Primary, fontWeight = FontWeight.Bold) }
        }
    }
}
