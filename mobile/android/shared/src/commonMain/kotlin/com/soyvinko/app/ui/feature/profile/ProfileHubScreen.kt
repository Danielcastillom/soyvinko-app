package com.soyvinko.app.ui.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
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
import com.soyvinko.app.ui.common.AppMode
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalAppMode
import com.soyvinko.app.ui.common.LocalSetAppMode
import androidx.compose.material3.Surface
import com.soyvinko.app.ui.common.LocalSession
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.VCard
import com.soyvinko.app.ui.common.objects
import com.soyvinko.app.ui.common.rememberResource
import com.soyvinko.app.ui.components.SectionLabel
import com.soyvinko.app.ui.screens.HeroGradient
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.theme.Majesticons
import com.soyvinko.app.ui.theme.Vinko
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight

/** Pestaña Perfil: resumen de la cuenta y acceso a cada ajuste. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileHubScreen(nav: NavHostController) {
    val app = LocalApp.current
    val user = LocalUser.current
    val session = LocalSession.current
    val appMode = LocalAppMode.current
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
                    .background(Vinko.Primary, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text("TU IDENTIDAD EN SOYVINKO", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Vinko.Lilac)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(
                        name = user.name,
                        photoUrl = user.avatarFileId ?: app.avatarFileId,
                        size = 48.dp,
                        light = true,
                        onClick = { nav.navigate(Routes.ACCOUNT) },
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            user.businessName?.takeIf { user.isCompany && it.isNotBlank() } ?: user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(user.email, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Vinko.Lilac, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    // Perfil de cliente independiente sin conmutador en cabecera
                }
                Spacer(Modifier.height(8.dp))
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

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileShortcutButtons(nav)

                if (!user.emailVerified) {
                    VCard(
                        modifier = Modifier.clickable { nav.navigate(Routes.SECURITY) },
                        padding = 12.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Vinko.AmberBg, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.MarkEmailUnread,
                                    contentDescription = null,
                                    tint = Vinko.Amber,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Confirma tu correo",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Vinko.Ink,
                                )
                                Text(
                                    "Toca aquí para enviar el enlace de verificación.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Vinko.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Vinko.Primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                if (appMode == AppMode.SELLER) {
                    SectionLabel("Billetera")
                    VCard(padding = 12.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { nav.navigate(Routes.WALLET) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Vinko.Tint, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Majesticons.CreditCardSolid,
                                    contentDescription = null,
                                    tint = Vinko.Primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Saldo disponible",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = Vinko.Muted,
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${dashboard.data?.creditBalance ?: 0}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Vinko.Primary,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Vinkos",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 12.sp,
                                        color = Vinko.Lilac,
                                        modifier = Modifier.padding(bottom = 2.dp),
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Vinko.Primary, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    "Ver wallet",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.width(3.dp))
                                Icon(
                                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Divider()
                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            TextButton(
                                onClick = { nav.navigate(Routes.WALLET) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = Vinko.Primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Recargar",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = Vinko.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            TextButton(
                                onClick = { nav.navigate(Routes.WITHDRAW) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.AccountBalance,
                                    contentDescription = null,
                                    tint = Vinko.Primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Retirar",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = Vinko.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            TextButton(
                                onClick = { nav.navigate(Routes.WALLET_HISTORY) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    Majesticons.Clock,
                                    contentDescription = null,
                                    tint = Vinko.Muted,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Historial",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = Vinko.Muted,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }

                SectionLabel("Tu cuenta")
                VCard(padding = 4.dp) {
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
                    MenuRow(Majesticons.Lock, "Seguridad", "Contraseña y dispositivos conectados", last = true) {
                        nav.navigate(Routes.SECURITY)
                    }
                }

                SectionLabel("Actividad")
                VCard(padding = 4.dp) {
                    MenuRow(
                        Majesticons.Bell,
                        "Novedades",
                        if (unread > 0) "$unread sin leer" else "Estás al día",
                        badge = unread,
                    ) { nav.navigate(Routes.NOTIFICATIONS) }
                    MenuRow(Icons.Outlined.SupportAgent, "Soporte", "Casos, ayuda y disputas", last = true) { nav.navigate(Routes.SUPPORT) }
                }

                if (user.isProfessional) {
                    SectionLabel("SoyVinko")
                    VCard(padding = 4.dp) {
                        MenuRow(Majesticons.Briefcase, "Panel profesional", "Oportunidades, propuestas y tu perfil público", tint = Vinko.Mint, bg = Vinko.MintBg, last = true) {
                            nav.navigate(Routes.PRO)
                        }
                    }
                }

                SectionLabel("Proveedor")
                VCard(padding = 4.dp) {
                    MenuRow(
                        icon = Majesticons.Briefcase,
                        title = if (user.isProfessional) "Perfil de Proveedor / Panel Profesional" else "Registrarme como Proveedor",
                        subtitle = if (user.isProfessional) "Gestiona tus servicios, tarifas y oportunidades" else "Publica tus servicios profesionales",
                        tint = Vinko.Mint,
                        bg = Vinko.MintBg,
                        last = true,
                    ) {
                        nav.navigate(Routes.PRO)
                    }
                }

                if (user.isStaff) {
                    SectionLabel("Equipo SoyVinko")
                    VCard(padding = 4.dp) {
                        MenuRow(Icons.Outlined.AdminPanelSettings, "Administración", "Usuarios, identidad, retiros, soporte y moderación", tint = Vinko.Coral, bg = Vinko.CoralBg, last = true) {
                            nav.navigate(Routes.ADMIN)
                        }
                    }
                }

                OutlinedButton(
                    onClick = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp).padding(top = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.CoralBg),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Vinko.Coral),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cerrar sesión", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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

@Composable
private fun ProfileShortcutButtons(nav: NavHostController) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProfileShortcutButton(
                title = "Cuenta",
                subtitle = "Datos personales",
                icon = Icons.Outlined.Person,
                modifier = Modifier.weight(1f),
                onClick = { nav.navigate(Routes.ACCOUNT) },
            )
            ProfileShortcutButton(
                title = "Direcciones",
                subtitle = "Lugares de servicio",
                icon = Icons.Outlined.Place,
                modifier = Modifier.weight(1f),
                onClick = { nav.navigate(Routes.ADDRESSES) },
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProfileShortcutButton(
                title = "Marketplace",
                subtitle = "Tiendas y productos",
                icon = Icons.Outlined.Storefront,
                modifier = Modifier.weight(1f),
                onClick = { nav.navigate(Routes.MARKETPLACE) },
            )
            ProfileShortcutButton(
                title = "Buscar proveedor",
                subtitle = "Encuentra especialistas",
                icon = Icons.Outlined.Search,
                modifier = Modifier.weight(1f),
                onClick = { nav.navigate(Routes.HOME) },
            )
        }
    }
}

@Composable
private fun ProfileShortcutButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Vinko.Tint,
        border = androidx.compose.foundation.BorderStroke(1.dp, Vinko.Primary),
        modifier = modifier.height(72.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Vinko.Primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = Vinko.Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Vinko.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 12.sp,
                    )
                }
            }
        }
    }
}
