package com.soyvinko.app.ui.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.soyvinko.app.data.Dashboard
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSession
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.screens.HeroGradient
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Vinko

/** Pestaña Perfil: resumen de la cuenta y acceso a cada ajuste. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileHubScreen(nav: NavHostController) {
    val app = LocalApp.current
    val user = LocalUser.current
    val session = LocalSession.current
    val dashboard = rememberResource { app.ops.get("dashboard", Dashboard.serializer()) }
    val kyc = rememberResource { app.ops.call("my_kyc").objects() }
    var confirmLogout by remember { mutableStateOf(false) }
    val kycState = kyc.data?.let { kycStateOf(it.firstOrNull()) }
    val unread = dashboard.data?.unreadNotifications ?: 0

    PullToRefreshBox(
        isRefreshing = dashboard.loading && dashboard.data != null,
        onRefresh = {
            dashboard.reload()
            kyc.reload()
            session.reloadUser()
        },
        modifier = Modifier.fillMaxSize().background(Vinko.Cream),
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(HeroGradient, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
            ) {
                Text("TU IDENTIDAD EN SOYVINKO", style = MaterialTheme.typography.labelSmall, color = Vinko.Lilac)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(user.name)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            user.businessName?.takeIf { user.isCompany && it.isNotBlank() } ?: user.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = Vinko.Lilac, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(accountTypeLabel(user.isCompany, user.isProfessional), Color.White, Color.White.copy(alpha = 0.14f))
                    when (kycState) {
                        KycState.Approved -> Pill("Identidad verificada", Vinko.Mint, Vinko.MintBg, Icons.Outlined.VerifiedUser)
                        KycState.Pending -> Pill("Verificación en revisión", Vinko.Amber, Vinko.AmberBg, Icons.Outlined.HourglassTop)
                        KycState.Expired, KycState.Rejected, KycState.None ->
                            Pill("Sin verificar", Color.White, Color.White.copy(alpha = 0.14f), Icons.Outlined.WarningAmber)
                        null -> Unit
                    }
                }
            }

            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!user.emailVerified) {
                    VCard {
                        CardHeader(
                            Icons.Outlined.MarkEmailUnread,
                            "Confirma tu correo",
                            "Protege tu cuenta y habilita los retiros. Puedes reenviar el enlace desde Seguridad.",
                            Vinko.Amber,
                            Vinko.AmberBg,
                        )
                    }
                }

                SectionLabel("Tu cuenta")
                VCard(padding = 6.dp) {
                    MenuRow(Icons.Outlined.Person, "Cuenta", if (user.isCompany) "Datos de la empresa, contactos y pagos" else "Datos personales, contactos y pagos") {
                        nav.navigate(Routes.ACCOUNT)
                    }
                    MenuRow(Icons.Outlined.LocationOn, "Direcciones", "Lugares donde recibes servicios") { nav.navigate(Routes.ADDRESSES) }
                    MenuRow(
                        icon = Icons.Outlined.VerifiedUser,
                        title = "Verificación de identidad",
                        subtitle = when (kycState) {
                            KycState.Approved -> "Identidad verificada"
                            KycState.Pending -> "Tu expediente está en revisión"
                            KycState.Expired -> "Tu verificación venció: renuévala"
                            KycState.Rejected -> "No fue aprobada: vuelve a enviarla"
                            KycState.None -> "Habilita retiros y la insignia de verificado"
                            null -> "Cédula y selfie con tu documento"
                        },
                        tint = if (kycState == KycState.Approved) Vinko.Mint else Vinko.Primary,
                        bg = if (kycState == KycState.Approved) Vinko.MintBg else Vinko.Tint,
                    ) { nav.navigate(Routes.KYC) }
                    MenuRow(Icons.Outlined.Lock, "Seguridad", "Contraseña y dispositivos conectados", last = true) {
                        nav.navigate(Routes.SECURITY)
                    }
                }

                SectionLabel("Actividad")
                VCard(padding = 6.dp) {
                    MenuRow(
                        Icons.Outlined.Notifications,
                        "Novedades",
                        if (unread > 0) "$unread sin leer" else "Estás al día",
                        badge = unread,
                    ) { nav.navigate(Routes.NOTIFICATIONS) }
                    MenuRow(Icons.Outlined.SupportAgent, "Soporte", "Casos, ayuda y disputas", last = true) { nav.navigate(Routes.SUPPORT) }
                }

                SectionLabel("SoyVinko")
                VCard(padding = 6.dp) {
                    if (user.isProfessional) {
                        MenuRow(Icons.Outlined.WorkOutline, "Panel profesional", "Oportunidades, propuestas y tu perfil público", tint = Vinko.Mint, bg = Vinko.MintBg) {
                            nav.navigate(Routes.PRO)
                        }
                    } else {
                        MenuRow(Icons.Outlined.WorkOutline, "Activa tu perfil profesional", "Ofrece tus servicios y recibe 3 Vinkos de bienvenida", tint = Vinko.Mint, bg = Vinko.MintBg) {
                            nav.navigate(Routes.PRO)
                        }
                    }
                    MenuRow(Icons.Outlined.Storefront, "Marketplace", "Productos de comercios de confianza", tint = Vinko.Cyan, bg = Vinko.CyanBg) {
                        nav.navigate(Routes.MARKETPLACE)
                    }
                    MenuRow(Icons.Outlined.Gavel, "Legales", "Términos, privacidad, cookies y aviso legal", last = true) {
                        nav.navigate(Routes.legal("terminos"))
                    }
                }

                if (user.isStaff) {
                    SectionLabel("Equipo SoyVinko")
                    VCard(padding = 6.dp) {
                        MenuRow(Icons.Outlined.AdminPanelSettings, "Administración", "Usuarios, identidad, retiros, soporte y moderación", tint = Vinko.Coral, bg = Vinko.CoralBg, last = true) {
                            nav.navigate(Routes.ADMIN)
                        }
                    }
                }

                OutlinedButton(
                    onClick = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp).padding(top = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.CoralBg),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Vinko.Coral),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    "SoyVinko para ${LocalApp.current.info.platform} · versión ${LocalApp.current.info.versionName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Vinko.Subtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "¿Cerrar sesión?",
            text = "Tendrás que ingresar tu correo y contraseña para volver a entrar en este teléfono.",
            confirm = "Cerrar sesión",
            destructive = true,
            onConfirm = {
                confirmLogout = false
                session.logout()
            },
            onDismiss = { confirmLogout = false },
        )
    }
}

fun accountTypeLabel(company: Boolean, professional: Boolean) = when {
    company && professional -> "Empresa y profesional"
    company -> "Cuenta empresa"
    professional -> "Cliente y profesional"
    else -> "Cuenta personal"
}
