package com.soyvinko.app.ui.feature.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.ApiException
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Bancos venezolanos (frontend/src/venezuelanBanks.ts). */
data class Bank(val code: String, val name: String, val pagoMovil: Boolean = true)

val VENEZUELAN_BANKS = listOf(
    Bank("0102", "Banco de Venezuela (BDV)"),
    Bank("0104", "Venezolano de Crédito"),
    Bank("0105", "Mercantil Banco"),
    Bank("0108", "BBVA Provincial"),
    Bank("0114", "Bancaribe"),
    Bank("0115", "Banco Exterior"),
    Bank("0128", "Banco Caroní"),
    Bank("0134", "Banesco"),
    Bank("0137", "Banco Sofitasa"),
    Bank("0138", "Banco Plaza"),
    Bank("0151", "Banco Fondo Común (BFC)"),
    Bank("0156", "100% Banco"),
    Bank("0157", "DelSur"),
    Bank("0163", "Banco del Tesoro"),
    Bank("0166", "Banco Agrícola"),
    Bank("0168", "Bancrecer"),
    Bank("0169", "Mi Banco"),
    Bank("0171", "Banco Activo"),
    Bank("0172", "Bancamiga"),
    Bank("0174", "Banplus"),
    Bank("0175", "Banco Bicentenario"),
    Bank("0177", "BANFANB"),
    Bank("0191", "BNC"),
)

fun bankByCode(code: String?): Bank? = VENEZUELAN_BANKS.firstOrNull { it.code == code }

val PHONE_PREFIXES = listOf("0414", "0424", "0412", "0416", "0426")

val DOCUMENT_TYPES = listOf(
    "V" to "V · Venezolano",
    "E" to "E · Extranjero",
    "J" to "J · Jurídico / Empresa",
    "G" to "G · Gubernamental",
)

/** Validación local igual a la web; el servidor vuelve a validar y responde en español. */
fun validatePayout(
    bankTransfer: Boolean,
    account: String,
    bankCode: String,
    phone: String,
    phonePrefix: String,
    docNumber: String,
    holder: String,
): String? {
    if (docNumber.length < 5) return "Ingresa un número de cédula o RIF válido (mínimo 5 dígitos)."
    if (holder.trim().length < 3) return "Ingresa el nombre y apellido o la razón social del titular."
    if (bankTransfer) {
        if (account.length != 20) return "El número de cuenta debe tener 20 dígitos (ahora tiene ${account.length})."
        if (!account.startsWith(bankCode)) return "Los primeros 4 dígitos de la cuenta (${account.take(4)}) no coinciden con el banco seleccionado ($bankCode)."
    } else if (phone.length != 7) {
        return "El número de celular debe tener 7 dígitos después del prefijo $phonePrefix."
    }
    return null
}

/** Formulario nativo de AddPayoutMethodModal.tsx: transferencia bancaria o Pago Móvil. */
@Composable
fun AddPayoutMethodScreen(onBack: () -> Unit, onSaved: (message: String) -> Unit) {
    val app = LocalApp.current
    val user = LocalUser.current
    val scope = rememberCoroutineScope()

    var bankTransfer by rememberSaveable { mutableStateOf(true) }
    var account by rememberSaveable { mutableStateOf("") }
    var bankCode by rememberSaveable { mutableStateOf(VENEZUELAN_BANKS.first().code) }
    var accountType by rememberSaveable { mutableStateOf("corriente") }
    var currency by rememberSaveable { mutableStateOf(Cur.VES) }
    var pmBank by rememberSaveable { mutableStateOf(VENEZUELAN_BANKS.first().code) }
    var prefix by rememberSaveable { mutableStateOf(PHONE_PREFIXES.first()) }
    var phone by rememberSaveable { mutableStateOf("") }
    var docType by rememberSaveable { mutableStateOf(if (user.isCompany) "J" else "V") }
    var docNumber by rememberSaveable { mutableStateOf("") }
    var holder by rememberSaveable { mutableStateOf(user.businessName?.takeIf { user.isCompany } ?: user.name) }
    var alias by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val detected = if (account.length >= 4) bankByCode(account.take(4)) else null
    val selectedBank = bankByCode(if (bankTransfer) bankCode else pmBank)

    fun submit() {
        error = validatePayout(bankTransfer, account, bankCode, phone, prefix, docNumber, holder)
        if (error != null) return
        val body = buildMap<String, String> {
            put("method_type", if (bankTransfer) "bank_transfer" else "pago_movil")
            put("currency", if (bankTransfer) currency.code else "VES")
            put("document_type", docType)
            put("document_number", docNumber)
            put("holder_name", holder.trim())
            alias.trim().takeIf { it.isNotEmpty() }?.let { put("label", it) }
            if (bankTransfer) {
                put("bank_code", bankCode)
                put("account_number", account)
                put("account_type", accountType)
            } else {
                put("bank_code", pmBank)
                put("phone_prefix", prefix)
                put("phone_number", phone)
            }
        }
        busy = true
        scope.launch {
            try {
                app.api.raw("POST", "/api/v1/integrations/payout-methods", JsonObject(body.mapValues { JsonPrimitive(it.value) }))
                onSaved("Cuenta registrada y verificada. Ya puedes usarla para retirar.")
            } catch (e: ApiException) {
                // Mensaje del servidor (p. ej. titular distinto al KYC aprobado o correo sin confirmar).
                error = listOfNotNull(e.message, e.fieldErrors.values.firstOrNull()?.takeIf { it != e.message }).joinToString(" ")
            } finally {
                busy = false
            }
        }
    }

    ScreenScaffold("Añadir cuenta de retiro", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Segmented(
                options = listOf(true to "Transferencia", false to "Pago Móvil"),
                selected = bankTransfer,
                onSelect = {
                    bankTransfer = it
                    error = null
                },
                icons = { if (it) Icons.Outlined.AccountBalance else Icons.Outlined.PhoneAndroid },
            )
            Text(
                "Por seguridad, la cuenta debe estar a tu nombre: el documento del titular tiene que coincidir con tu verificación de identidad aprobada y tu correo debe estar confirmado.",
                style = MaterialTheme.typography.bodySmall,
                color = Vinko.Muted,
            )
            if (!user.emailVerified) Banner("Confirma tu correo electrónico antes de registrar una cuenta para recibir retiros.", BannerTone.Info)
            error?.let { Banner(it) }

            VCard(padding = 18.dp) {
                if (bankTransfer) {
                    SectionLabel("Cuenta bancaria")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = account,
                        onValueChange = { raw ->
                            val digits = raw.filter(Char::isDigit).take(20)
                            account = digits
                            if (digits.length >= 4) bankByCode(digits.take(4))?.let { bankCode = it.code }
                            error = null
                        },
                        label = { Text("Número de cuenta (20 dígitos)") },
                        placeholder = { Text("01340000000000000000", color = Vinko.Subtle) },
                        singleLine = true,
                        supportingText = { Text("${account.length}/20 dígitos") },
                        trailingIcon = detected?.let {
                            {
                                Row(
                                    Modifier.padding(end = 10.dp).background(Vinko.MintBg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(it.name.substringBefore(" ("), style = MaterialTheme.typography.labelMedium, color = Vinko.Mint, maxLines = 1)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Dropdown(
                        label = "Banco receptor",
                        options = VENEZUELAN_BANKS,
                        selected = bankByCode(bankCode) ?: VENEZUELAN_BANKS.first(),
                        text = { "${it.code} · ${it.name}" },
                        onSelect = { bankCode = it.code },
                    )
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Tipo de cuenta")
                    Segmented(
                        options = listOf("corriente" to "Corriente", "ahorro" to "Ahorro"),
                        selected = accountType,
                        onSelect = { accountType = it },
                    )
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Moneda de la cuenta")
                    Segmented(
                        options = listOf(Cur.VES to "Bs · Bolívares", Cur.USD to "US$ · Divisas"),
                        selected = currency,
                        onSelect = { currency = it },
                    )
                } else {
                    SectionLabel("Pago Móvil")
                    Spacer(Modifier.height(12.dp))
                    Dropdown(
                        label = "Banco de Pago Móvil",
                        options = VENEZUELAN_BANKS.filter { it.pagoMovil },
                        selected = bankByCode(pmBank) ?: VENEZUELAN_BANKS.first(),
                        text = { "${it.code} · ${it.name}" },
                        onSelect = { pmBank = it.code },
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Dropdown(
                            label = "Prefijo",
                            options = PHONE_PREFIXES,
                            selected = prefix,
                            text = { it },
                            onSelect = { prefix = it },
                            modifier = Modifier.weight(0.42f),
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.filter(Char::isDigit).take(7); error = null },
                            label = { Text("Celular") },
                            placeholder = { Text("1234567", color = Vinko.Subtle) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors(),
                            modifier = Modifier.weight(0.58f),
                        )
                    }
                    Text(
                        "Prefijo móvil (0414, 0424, 0412, 0416, 0426) y 7 dígitos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Vinko.Muted,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    )
                }
            }

            VCard(padding = 18.dp) {
                SectionLabel("Titular")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dropdown(
                        label = "Tipo",
                        options = DOCUMENT_TYPES,
                        selected = DOCUMENT_TYPES.first { it.first == docType },
                        text = { it.first },
                        menuText = { it.second },
                        onSelect = { docType = it.first },
                        modifier = Modifier.weight(0.32f),
                    )
                    OutlinedTextField(
                        value = docNumber,
                        onValueChange = { docNumber = it.filter(Char::isDigit).take(10); error = null },
                        label = { Text("Cédula o RIF") },
                        leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null, tint = Vinko.Lavender) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                        modifier = Modifier.weight(0.68f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = holder,
                    onValueChange = { holder = it.take(120); error = null },
                    label = { Text("Nombre completo o razón social") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = Vinko.Lavender) },
                    singleLine = true,
                    supportingText = { Text("Debe coincidir con el titular registrado en el banco.") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it.take(80) },
                    label = { Text("Alias (opcional)") },
                    placeholder = {
                        Text(
                            if (bankTransfer) "${selectedBank?.name?.substringBefore(" (") ?: "Banco"} personal" else "Pago Móvil ${selectedBank?.name?.substringBefore(" (").orEmpty()}",
                            color = Vinko.Subtle,
                        )
                    },
                    leadingIcon = { Icon(Icons.Outlined.Sell, contentDescription = null, tint = Vinko.Lavender) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                Modifier.fillMaxWidth().background(Vinko.MintBg, RoundedCornerShape(14.dp)).padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Verificamos la cuenta al instante. Tus datos bancarios se guardan cifrados y solo se usan para tus retiros.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Mint,
                )
            }

            PrimaryButton("Guardar cuenta de retiro", onClick = { submit() }, loading = busy)
        }
    }
}
