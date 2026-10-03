package com.soyvinko.app.ui.feature.profile

import com.soyvinko.app.ui.common.todayCaracas
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import com.soyvinko.app.ui.common.PlatformBackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraFront
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soyvinko.app.data.UploadPart
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSession
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.b
import com.soyvinko.app.ui.common.list
import com.soyvinko.app.ui.common.obj
import com.soyvinko.app.ui.common.rememberPhotoPicker
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.common.s
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.components.SecondaryButton
import com.soyvinko.app.ui.components.VinkoTextField
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private val DOC_TYPES = listOf("V" to "Cédula V", "E" to "Cédula E", "P" to "Pasaporte")
private val PHONE_PREFIXES = listOf("0414", "0424", "0412", "0416", "0426")
private val STEP_TITLES = listOf("Cédula", "Selfie", "Confirmar")

/** Una foto lista para subir y su miniatura. */
private class Shot(val part: UploadPart, val preview: ImageBitmap?)

/**
 * Verificación de identidad en 3 pasos, como la web (PersonalKycWizard): foto de la cédula con
 * lectura OCR, selfie con el documento y confirmación. Envía todo a /api/v1/support/kyc-personal.
 */
@Composable
fun KycSubmitScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val app = LocalApp.current
    val session = LocalSession.current
    val scope = rememberCoroutineScope()
    val submit = rememberSubmitter()
    val account = rememberResource { app.ops.call("my_account").obj() }

    var step by rememberSaveable { mutableIntStateOf(0) }
    var front by remember { mutableStateOf<Shot?>(null) }
    var selfie by remember { mutableStateOf<Shot?>(null) }
    var proof by remember { mutableStateOf<Shot?>(null) }
    var processing by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var ocr by remember { mutableStateOf<JsonObject?>(null) }
    var ocrFailed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    var docType by rememberSaveable { mutableStateOf("V") }
    var docNumber by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var birthdate by rememberSaveable { mutableStateOf("") }
    var phonePrefix by rememberSaveable { mutableStateOf("0414") }
    var phoneRest by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var prefilled by rememberSaveable { mutableStateOf(false) }

    // Datos de la cuenta como punto de partida (nombre y teléfono guardados).
    LaunchedEffect(account.data) {
        val data = account.data ?: return@LaunchedEffect
        if (prefilled) return@LaunchedEffect
        prefilled = true
        if (fullName.isBlank()) fullName = data.s("name").orEmpty()
        val phone = data.list("contacts").firstOrNull { it.s("kind") == "phone" || it.s("kind") == "whatsapp" }?.s("value")
        val digits = phone?.filter(Char::isDigit).orEmpty().let { if (it.startsWith("58") && it.length == 12) "0" + it.drop(2) else it }
        if (digits.length == 11 && digits.take(4) in PHONE_PREFIXES) phonePrefix = digits.take(4)
        if (phoneRest.isBlank() && digits.length >= 7) phoneRest = digits.takeLast(7)
    }

    fun scan(shot: UploadPart) {
        scope.launch {
            scanning = true
            ocrFailed = false
            try {
                val result = app.api.upload("/api/v1/support/kyc-ocr-scan", listOf(shot.copy(field = "file"))).obj()
                ocr = result
                // Si el documento está vencido, OcrResult lo explica y no se puede continuar.
                if (result != null && !result.b("is_expired")) {
                    result.s("doc_type")?.takeIf { it in listOf("V", "E", "P") }?.let { docType = it }
                    result.s("doc_number")?.let { docNumber = it.filter(Char::isDigit).take(10) }
                    result.s("full_name")?.let { fullName = it }
                    result.s("birthdate")?.let { birthdate = it }
                    ocrFailed = !result.b("success") && result.s("doc_number") == null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // La lectura automática es opcional: el usuario puede escribir sus datos.
                ocrFailed = true
            } finally {
                scanning = false
            }
        }
    }

    fun accept(part: UploadPart, field: String, name: String, assign: (Shot) -> Unit, afterwards: (UploadPart) -> Unit = {}) {
        scope.launch {
            processing = field
            try {
                val ready = prepareImage(part, field, name)
                assign(Shot(ready, previewOf(ready.bytes)))
                afterwards(ready)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "No pudimos procesar la foto. Inténtalo de nuevo."
            } finally {
                processing = null
            }
        }
    }

    val onFront: (UploadPart) -> Unit = { part ->
        error = null
        ocr = null
        accept(part, "identity_front", "cedula_anverso.jpg", { front = it }) { scan(it) }
    }
    val frontCamera = rememberKycCamera(front = false, onError = { error = it }, onCaptured = onFront)
    val frontGallery = rememberPhotoPicker(1) { parts -> parts.firstOrNull()?.let(onFront) }
    val selfieCamera = rememberKycCamera(front = true, onError = { error = it }) { part ->
        error = null
        accept(part, "selfie", "selfie_con_cedula.jpg", { selfie = it })
    }
    val proofCamera = rememberKycCamera(front = false, onError = { error = it }) { part ->
        accept(part, "proof_of_address", "comprobante_domicilio.jpg", { proof = it })
    }
    val proofGallery = rememberPhotoPicker(1) { parts ->
        parts.firstOrNull()?.let { accept(it, "proof_of_address", "comprobante_domicilio.jpg", { s -> proof = s }) }
    }

    val expired = ocr.b("is_expired")
    val phoneValid = phoneRest.length == 7
    val step1Problem = when {
        front == null -> "Toma la foto del anverso de tu cédula para continuar."
        expired -> "Documento vencido detectado. Debes presentar una cédula vigente."
        docNumber.length < 5 -> "Escribe un número de documento válido."
        fullName.trim().length < 3 -> "Escribe tus nombres y apellidos completos."
        birthdate.isBlank() -> "Indica tu fecha de nacimiento."
        !phoneValid -> "Escribe tu número celular (7 dígitos después del código)."
        else -> null
    }

    PlatformBackHandler(enabled = step > 0 && !done) { step-- ; error = null }

    ScreenScaffold("Verificar identidad", onBack = { if (step > 0 && !done) { step--; error = null } else onBack() }) { padding ->
        if (done) {
            SuccessView(padding, fullName, "$docType-$docNumber", onDone)
            return@ScreenScaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            Stepper(step)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                error?.let { Banner(it) }
                when (step) {
                    0 -> {
                        VCard {
                            CardHeader(Icons.Outlined.Badge, "Anverso de tu cédula", "Con buena luz, sin reflejos y con los 4 bordes visibles.")
                            Spacer(Modifier.height(14.dp))
                            PhotoSlot(
                                shot = front,
                                busy = processing == "identity_front",
                                overlay = if (scanning) "Leyendo los datos de tu cédula…" else null,
                                emptyIcon = Icons.Outlined.Badge,
                                emptyText = "Fotografía el frente de tu cédula de identidad. No hace falta el reverso.",
                                onRetake = frontCamera,
                            ) {
                                PrimaryButton("Tomar foto", onClick = frontCamera)
                                Spacer(Modifier.height(8.dp))
                                SecondaryButton("Elegir de la galería", onClick = frontGallery)
                            }
                            ocr?.let { result ->
                                if (!scanning) {
                                    Spacer(Modifier.height(12.dp))
                                    OcrResult(result)
                                }
                            }
                            if (ocrFailed && !scanning && !expired) {
                                Spacer(Modifier.height(12.dp))
                                Banner("No pudimos leer todos los datos automáticamente. Revisa y completa el formulario.", BannerTone.Info)
                            }
                        }
                        VCard {
                            Text("Confirma tus datos", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            Text("Tipo de documento", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
                            ChoiceChips(DOC_TYPES, docType, { docType = it })
                            VinkoTextField(
                                docNumber, { docNumber = it.filter(Char::isDigit).take(10) }, "Número de documento",
                                keyboardType = KeyboardType.Number,
                            )
                            Spacer(Modifier.height(4.dp))
                            VinkoTextField(
                                fullName, { fullName = it.take(120) }, "Nombres y apellidos",
                                capitalization = KeyboardCapitalization.Words,
                                supporting = "Tal como figuran en tu cédula",
                            )
                            Spacer(Modifier.height(4.dp))
                            BirthdateField(birthdate) { birthdate = it }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                SelectField(
                                    "Código", PHONE_PREFIXES.map { it to it }, phonePrefix, { phonePrefix = it },
                                    modifier = Modifier.width(118.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                VinkoTextField(
                                    phoneRest, { phoneRest = it.filter(Char::isDigit).take(7) }, "Celular",
                                    keyboardType = KeyboardType.Phone,
                                    modifier = Modifier.weight(1f),
                                    supporting = "7 dígitos",
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            VinkoTextField(
                                address, { address = it.take(300) }, "Dirección residencial (opcional)",
                                leadingIcon = Icons.Outlined.Home,
                                capitalization = KeyboardCapitalization.Sentences,
                                supporting = "Ciudad, estado, municipio o urbanización",
                            )
                            val saved = account.data.list("addresses")
                            if (saved.isNotEmpty() && address.isBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Usar una dirección guardada", style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
                                ChoiceChips(
                                    saved.take(3).map { it.s("id").orEmpty() to (it.s("label") ?: "Dirección") },
                                    null,
                                    { id ->
                                        saved.firstOrNull { it.s("id") == id }?.let { a ->
                                            address = listOfNotNull(a.s("address_line"), addressArea(a)).joinToString(", ")
                                        }
                                    },
                                )
                            }
                        }
                    }
                    1 -> {
                        VCard {
                            CardHeader(Icons.Outlined.CameraFront, "Selfie con tu cédula", "Usa la cámara frontal y sostén tu documento junto a tu rostro.")
                            Spacer(Modifier.height(10.dp))
                            Guide("Tu rostro debe verse nítido e iluminado, sin lentes oscuros ni gorra.")
                            Guide("Sostén la cédula firme sin tapar los datos con los dedos.")
                            Spacer(Modifier.height(10.dp))
                            PhotoSlot(
                                shot = selfie,
                                busy = processing == "selfie",
                                overlay = null,
                                emptyIcon = Icons.Outlined.CameraFront,
                                emptyText = "Por seguridad, la selfie debe tomarse en este momento con la cámara.",
                                onRetake = selfieCamera,
                                tall = true,
                            ) {
                                PrimaryButton("Tomar selfie", onClick = selfieCamera)
                            }
                        }
                        VCard {
                            CardHeader(Icons.Outlined.AttachFile, "Comprobante de domicilio (opcional)", "Recibo de servicio, constancia de residencia o RIF.")
                            Spacer(Modifier.height(10.dp))
                            val p = proof
                            if (p != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    p.preview?.let {
                                        Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)))
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    Text("Comprobante adjunto", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { proof = null }) { Icon(Icons.Outlined.Close, contentDescription = "Quitar comprobante", tint = Vinko.Muted) }
                                }
                            } else if (processing == "proof_of_address") {
                                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Vinko.Primary, trackColor = Vinko.Tint)
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SmallAction(Icons.Outlined.PhotoCamera, "Cámara", Modifier.weight(1f), proofCamera)
                                    SmallAction(Icons.Outlined.PhotoLibrary, "Galería", Modifier.weight(1f), proofGallery)
                                }
                            }
                        }
                    }
                    else -> {
                        VCard {
                            CardHeader(Icons.Outlined.VerifiedUser, "Confirma tu expediente", "Estos datos quedarán vinculados a tu cuenta para contratos y retiros.", Vinko.Mint, Vinko.MintBg)
                            Spacer(Modifier.height(12.dp))
                            InfoRow("Nombre", fullName.trim())
                            InfoRow("Documento", "$docType-$docNumber")
                            InfoRow("Nacimiento", isoToDisplay(birthdate))
                            InfoRow("Teléfono", "$phonePrefix-$phoneRest")
                            if (address.isNotBlank()) InfoRow("Dirección", address.trim())
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Thumb("Cédula", front, Modifier.weight(1f))
                                Thumb("Selfie", selfie, Modifier.weight(1f))
                                if (proof != null) Thumb("Comprobante", proof, Modifier.weight(1f))
                            }
                        }
                        Text(
                            "Declaro bajo fe de juramento que la información suministrada es verídica y que las fotos corresponden " +
                                "a mi persona y a mi documento de identidad vigente en la República Bolivariana de Venezuela.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Vinko.Muted,
                            modifier = Modifier.background(Vinko.Tint, RoundedCornerShape(14.dp)).padding(14.dp),
                        )
                        submit.error?.let { Banner(it) }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Column(Modifier.background(Vinko.Cream).padding(horizontal = 20.dp, vertical = 12.dp)) {
                when (step) {
                    0 -> PrimaryButton(
                        "Continuar a la selfie",
                        enabled = !scanning && processing == null && front != null && !expired,
                        onClick = {
                            error = step1Problem
                            if (step1Problem == null) step = 1
                        },
                    )
                    1 -> PrimaryButton(
                        "Revisar y confirmar",
                        enabled = selfie != null && processing == null,
                        onClick = { error = null; step = 2 },
                    )
                    else -> PrimaryButton(
                        "Confirmar y enviar verificación",
                        loading = submit.busy,
                        onClick = {
                            val f = front
                            val s = selfie
                            if (f == null || s == null) {
                                error = "Faltan fotografías obligatorias. Completa los pasos anteriores."
                                return@PrimaryButton
                            }
                            submit.run {
                                val files = listOfNotNull(f.part, s.part, proof?.part)
                                val fields = buildMap {
                                    put("phone", phonePrefix + phoneRest)
                                    put("document_type", docType)
                                    put("document_number", docNumber)
                                    put("full_name", fullName.trim())
                                    put("birthdate", birthdate)
                                    address.trim().takeIf { it.isNotEmpty() }?.let { put("address", it) }
                                }
                                app.api.upload("/api/v1/support/kyc-personal", files, fields)
                                done = true
                                session.reloadUser()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Stepper(step: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        STEP_TITLES.forEachIndexed { i, title ->
            val active = i == step
            val complete = i < step
            Box(
                Modifier
                    .size(26.dp)
                    .background(if (active || complete) Vinko.Primary else Color.White, RoundedCornerShape(50))
                    .border(1.dp, if (active || complete) Vinko.Primary else Vinko.Line, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                if (complete) Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                else Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = if (active) Color.White else Vinko.Muted)
            }
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = if (active) Vinko.Ink else Vinko.Muted)
            if (i < STEP_TITLES.lastIndex) {
                Box(Modifier.weight(1f).padding(horizontal = 8.dp).height(2.dp).background(if (complete) Vinko.Primary else Vinko.Line))
            }
        }
    }
}

@Composable
private fun PhotoSlot(
    shot: Shot?,
    busy: Boolean,
    overlay: String?,
    emptyIcon: ImageVector,
    emptyText: String,
    onRetake: () -> Unit,
    tall: Boolean = false,
    actions: @Composable () -> Unit,
) {
    val height = if (tall) 280.dp else 200.dp
    when {
        busy -> Box(Modifier.fillMaxWidth().height(height).background(Vinko.Tint, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Vinko.Primary)
        }
        shot != null -> {
            Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(16.dp)).background(Vinko.Dark)) {
                shot.preview?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
                if (overlay != null) {
                    Column(
                        Modifier.matchParentSize().background(Vinko.Dark.copy(alpha = 0.6f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(overlay, color = Color.White, style = MaterialTheme.typography.titleSmall)
                    }
                }
                TextButton(
                    onClick = onRetake,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    colors = ButtonDefaults.textButtonColors(containerColor = Color.White.copy(alpha = 0.92f), contentColor = Vinko.Ink),
                ) {
                    Icon(Icons.Outlined.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Repetir foto")
                }
            }
        }
        else -> Column(
            Modifier
                .fillMaxWidth()
                .background(Vinko.Cream, RoundedCornerShape(16.dp))
                .border(1.dp, Vinko.Lilac, RoundedCornerShape(16.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            com.soyvinko.app.ui.common.IconTile(emptyIcon, size = 56.dp)
            Spacer(Modifier.height(10.dp))
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = Vinko.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            actions()
        }
    }
}

@Composable
private fun OcrResult(result: JsonObject) {
    if (result.b("is_expired")) {
        Row(Modifier.fillMaxWidth().background(Vinko.CoralBg, RoundedCornerShape(14.dp)).padding(14.dp)) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Vinko.Coral)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Documento vencido", style = MaterialTheme.typography.titleSmall, color = Vinko.Coral)
                Text(
                    result.s("expiration_message") ?: "Tu cédula venció el ${result.s("expiration_date").orEmpty()}. No se admiten documentos vencidos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Coral,
                )
            }
        }
        return
    }
    if (result.s("doc_number") == null && result.s("full_name") == null) return
    Column(Modifier.fillMaxWidth().background(Vinko.MintBg, RoundedCornerShape(14.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Datos leídos de tu cédula", style = MaterialTheme.typography.titleSmall, color = Vinko.Mint)
        }
        Spacer(Modifier.height(6.dp))
        result.s("doc_number")?.let { InfoRow("Cédula", "${result.s("doc_type") ?: "V"}-$it") }
        result.s("full_name")?.let { InfoRow("Titular", it) }
        result.s("birthdate")?.let { InfoRow("Nacimiento", isoToDisplay(it)) }
        result.s("expiration_date")?.let { InfoRow("Vigencia", "$it (vigente)") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdateField(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = if (value.isBlank()) "" else isoToDisplay(value),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Fecha de nacimiento") },
            placeholder = { Text("DD/MM/AAAA", color = Vinko.Subtle) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Vinko.Lavender) },
            shape = RoundedCornerShape(14.dp),
            colors = vinkoFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val currentYear = todayCaracas().year
        val initial = runCatching { LocalDate.parse(value).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }.getOrNull()
            ?: LocalDate(currentYear - 30, 1, 1).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial, yearRange = (currentYear - 100)..(currentYear - 18))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString()) }
                    open = false
                }) { Text("Aceptar", color = Vinko.Primary) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar", color = Vinko.Muted) } },
        ) {
            DatePicker(state = state, showModeToggle = true)
        }
    }
}

@Composable
private fun Guide(text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Vinko.Mint, modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SmallAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Vinko.Ink),
    ) {
        Icon(icon, contentDescription = null, tint = Vinko.Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Thumb(label: String, shot: Shot?, modifier: Modifier) {
    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(12.dp)).background(Vinko.Tint)) {
            shot?.preview?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Vinko.Muted)
    }
}

@Composable
private fun SuccessView(padding: androidx.compose.foundation.layout.PaddingValues, name: String, document: String, onDone: () -> Unit) {
    ProfilePage(padding) {
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            com.soyvinko.app.ui.common.IconTile(Icons.Outlined.CheckCircle, Vinko.Mint, Vinko.MintBg, size = 72.dp)
            Spacer(Modifier.height(16.dp))
            Text("¡Verificación enviada!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tu expediente quedó registrado. El equipo de cumplimiento lo revisará en las próximas 24 a 48 horas hábiles y te avisaremos por correo.",
                style = MaterialTheme.typography.bodyMedium,
                color = Vinko.Muted,
                textAlign = TextAlign.Center,
            )
        }
        VCard {
            InfoRow("Titular", name.trim())
            InfoRow("Documento", document)
        }
        PrimaryButton("Ver estado de mi verificación", onClick = onDone)
    }
}
