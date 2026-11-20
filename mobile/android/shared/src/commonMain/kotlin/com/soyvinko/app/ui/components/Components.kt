package com.soyvinko.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.painterResource
import com.soyvinko.shared.resources.Res
import com.soyvinko.shared.resources.ic_logo_mark
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko

@Composable
fun BrandLogo(modifier: Modifier = Modifier, light: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(Res.drawable.ic_logo_mark), contentDescription = null, modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = if (light) Color.White else Vinko.Ink)) { append("Soy") }
                withStyle(SpanStyle(color = if (light) Vinko.Lilac else Vinko.Primary)) { append("Vinko") }
            },
            fontFamily = Manrope,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
        )
    }
}

@Composable
fun VinkoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    error: String? = null,
    supporting: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    password: Boolean = false,
    enabled: Boolean = true,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        isError = error != null,
        shape = RoundedCornerShape(14.dp),
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = Vinko.Lavender) } },
        trailingIcon = if (password) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = Vinko.Muted,
                    )
                }
            }
        } else null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else keyboardType,
            capitalization = capitalization,
            autoCorrectEnabled = !password && keyboardType == KeyboardType.Text,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Vinko.Primary,
            unfocusedBorderColor = Vinko.Line,
            focusedContainerColor = Vinko.FieldBg,
            unfocusedContainerColor = Vinko.FieldBg,
            disabledContainerColor = Vinko.FieldBg,
            focusedLabelColor = Vinko.Primary,
            unfocusedLabelColor = Vinko.Primary,
        ),
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Vinko.Primary, disabledContainerColor = Vinko.Lilac, disabledContentColor = Color.White),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Vinko.Ink),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

enum class BannerTone { Error, Info, Success }

@Composable
fun Banner(text: String, tone: BannerTone = BannerTone.Error, modifier: Modifier = Modifier) {
    val (fg, bg, icon) = when (tone) {
        BannerTone.Error -> Triple(Vinko.Coral, Vinko.CoralBg, Icons.Outlined.ErrorOutline)
        BannerTone.Info -> Triple(Vinko.Primary, Vinko.Tint, Icons.Outlined.Info)
        BannerTone.Success -> Triple(Vinko.Mint, Vinko.MintBg, Icons.Outlined.CheckCircle)
    }
    Row(
        modifier.fillMaxWidth().background(bg, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = fg, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Reglas de contraseña del servidor (mínimo 12 caracteres) más recomendaciones. */
@Composable
fun PasswordChecklist(password: String) {
    val rules = listOf(
        "Al menos 12 caracteres" to (password.length >= 12),
        "Una mayúscula y una minúscula" to (password.any(Char::isUpperCase) && password.any(Char::isLowerCase)),
        "Un número o símbolo" to password.any { !it.isLetter() },
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 4.dp)) {
        rules.forEach { (text, ok) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (ok) Vinko.Mint else Vinko.Subtle,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(text, style = MaterialTheme.typography.bodySmall, color = if (ok) Vinko.Mint else Vinko.Muted)
            }
        }
    }
}

/** Seis casillas para el código enviado por correo; un único campo oculto recibe el teclado. */
@Composable
fun CodeInput(value: String, onValueChange: (String) -> Unit, onComplete: () -> Unit, error: Boolean = false) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = value,
        onValueChange = { raw ->
            val digits = raw.filter(Char::isDigit).take(6)
            onValueChange(digits)
            if (digits.length == 6) onComplete()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (value.length == 6) onComplete() }),
        modifier = Modifier.focusRequester(focus).semantics { contentDescription = "Código de 6 dígitos" },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(6) { i ->
                    val char = value.getOrNull(i)
                    val active = i == value.length
                    Box(
                        Modifier
                            .weight(1f)
                            .height(58.dp)
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = when {
                                    error -> Vinko.Coral
                                    active -> Vinko.Primary
                                    char != null -> Vinko.Lavender
                                    else -> Vinko.Line
                                },
                                shape = RoundedCornerShape(14.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            char?.toString() ?: "",
                            fontFamily = Manrope,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = Vinko.Ink,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = Vinko.Primary)
}

@Composable
fun FieldSpacer() = Spacer(Modifier.height(12.dp))

@Composable
fun AuthBrandHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(Vinko.Tint, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_logo_mark),
                contentDescription = "Vinko Logo",
                modifier = Modifier.size(46.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Vinko.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Vinko.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Vinko.Line)
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .background(Vinko.Tint, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Ó",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Vinko.Subtle,
            )
        }
        HorizontalDivider(modifier = Modifier.weight(1f), color = Vinko.Line)
    }
}

@Composable
fun SocialAuthButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Line),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Vinko.Ink,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = Vinko.Ink,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .background(Vinko.Tint, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "G",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = Color(0xFF4285F4),
        )
    }
}

@Composable
fun AppleLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .background(Vinko.Tint, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Vinko.Ink,
        )
    }
}

