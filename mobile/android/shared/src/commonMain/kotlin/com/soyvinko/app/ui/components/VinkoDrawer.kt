package com.soyvinko.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.soyvinko.app.ui.common.AppMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.screens.HeroGradient
import com.soyvinko.app.ui.shell.Routes
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.feature.profile.Avatar
import com.soyvinko.app.ui.theme.Majesticons
import com.soyvinko.app.ui.theme.Manrope
import com.soyvinko.app.ui.theme.Vinko

/**
 * Menú lateral (Navigation Drawer) profesional para SoyVinko:
 * Incluye la cabecera con el perfil, el conmutador de Modo Cliente / Proveedor,
 * accesos directos al historial y solicitudes, verificación, soporte y cierre de sesión.
 */
@Composable
fun VinkoDrawerSheet(
    user: User,
    mode: AppMode = AppMode.CLIENT,
    onModeChange: (AppMode) -> Unit = {},
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(
        modifier = modifier.width(320.dp).fillMaxHeight(),
        drawerContainerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            // Cabecera del usuario con selector de Modo (Cliente vs Vendedor)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeroGradient)
                    .statusBarsPadding()
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.clickable { onCloseDrawer(); onNavigate(Routes.PROFILE) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(
                        name = user.name,
                        photoUrl = user.avatarFileId ?: LocalApp.current.avatarFileId,
                        size = 50.dp,
                        light = true,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = Vinko.Lilac,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Selector / Switch de Modo en la parte superior del menú lateral
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val clientActive = mode == AppMode.CLIENT
                        val sellerActive = mode == AppMode.SELLER

                        // Opción Modo Cliente
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (clientActive) Color.White else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onModeChange(AppMode.CLIENT)
                                    onCloseDrawer()
                                    onNavigate(Routes.HOME)
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = if (clientActive) Vinko.Primary else Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Cliente",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (clientActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (clientActive) Vinko.Primary else Color.White,
                                )
                            }
                        }

                        // Opción Modo Vendedor
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (sellerActive) Color.White else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onModeChange(AppMode.SELLER)
                                    onCloseDrawer()
                                    onNavigate(Routes.MARKETPLACE)
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    tint = if (sellerActive) Vinko.Primary else Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Vendedor",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (sellerActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (sellerActive) Vinko.Primary else Color.White,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (mode == AppMode.CLIENT) {
                // Secciones del Modo Cliente: Servicios y Contrataciones
                DrawerItem(
                    icon = Majesticons.Home,
                    title = "Explorar Servicios",
                    onClick = { onCloseDrawer(); onNavigate(Routes.HOME) },
                )
                DrawerItem(
                    icon = Majesticons.Megaphone,
                    title = "Mis Solicitudes",
                    onClick = { onCloseDrawer(); onNavigate(Routes.REQUESTS) },
                )
                DrawerItem(
                    icon = Majesticons.Briefcase,
                    title = "Servicios Contratados",
                    onClick = { onCloseDrawer(); onNavigate(Routes.CONTRACTS) },
                )
                DrawerItem(
                    icon = Majesticons.Truck,
                    title = "Pedir Transporte / Envíos (Rider)",
                    onClick = { onCloseDrawer(); onNavigate(Routes.TRANSPORT_NEW) },
                )

                DrawerItem(
                    icon = Majesticons.Pin,
                    title = "Mis Direcciones",
                    onClick = { onCloseDrawer(); onNavigate(Routes.ADDRESSES) },
                )
                DrawerItem(
                    icon = Majesticons.Bell,
                    title = "Notificaciones",
                    onClick = { onCloseDrawer(); onNavigate(Routes.NOTIFICATIONS) },
                )
                DrawerItem(
                    icon = Icons.Outlined.VerifiedUser,
                    title = "Verificación de Identidad (KYC)",
                    onClick = { onCloseDrawer(); onNavigate(Routes.KYC) },
                )
            } else {
                // Secciones del Modo Vendedor: Tienda, Freelancers, Riders, Órdenes
                DrawerItem(
                    icon = Icons.Outlined.Inventory2,
                    title = "Mi Tienda e Inventario (Vendedor)",
                    onClick = { onCloseDrawer(); onNavigate(Routes.MY_STORE) },
                )
                DrawerItem(
                    icon = Icons.Outlined.WorkOutline,
                    title = "Servicios y Tarifas (Freelancers)",
                    onClick = { onCloseDrawer(); onNavigate(Routes.PRO) },
                )
                DrawerItem(
                    icon = Majesticons.Truck,
                    title = "Panel Rider y Envíos (Conductores)",
                    onClick = { onCloseDrawer(); onNavigate(Routes.TRANSPORT) },
                )
                DrawerItem(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Consulta de Órdenes / Ventas",
                    onClick = { onCloseDrawer(); onNavigate(Routes.ORDERS) },
                )
                DrawerItem(
                    icon = Icons.Outlined.Storefront,
                    title = "Marketplace / Artículos",
                    onClick = { onCloseDrawer(); onNavigate(Routes.MARKETPLACE) },
                )
                DrawerItem(
                    icon = Majesticons.CreditCard,
                    title = "Billetera y Cobros",
                    onClick = { onCloseDrawer(); onNavigate(Routes.WALLET) },
                )
                DrawerItem(
                    icon = Majesticons.Bell,
                    title = "Notificaciones de Ventas",
                    onClick = { onCloseDrawer(); onNavigate(Routes.NOTIFICATIONS) },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Vinko.Line)

            DrawerItem(
                icon = Majesticons.Lock,
                title = "Seguridad y Contraseña",
                onClick = { onCloseDrawer(); onNavigate(Routes.SECURITY) },
            )
            DrawerItem(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                title = "Reportar un Problema / Ayuda",
                onClick = { onCloseDrawer(); onNavigate(Routes.SUPPORT) },
            )
            DrawerItem(
                icon = Icons.Outlined.Gavel,
                title = "Legales",
                onClick = { onCloseDrawer(); onNavigate(Routes.legal("terminos")) },
            )

            if (user.isStaff) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Vinko.Line)
                DrawerItem(
                    icon = Icons.Outlined.AdminPanelSettings,
                    title = "Panel de Administración",
                    tint = Vinko.Coral,
                    onClick = { onCloseDrawer(); onNavigate(Routes.ADMIN) },
                )
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Vinko.Line)
            Spacer(Modifier.height(8.dp))

            // Cerrar sesión
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCloseDrawer(); onLogout() }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "Cerrar sesión",
                    tint = Vinko.Coral,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Cerrar Sesión",
                    style = MaterialTheme.typography.labelLarge,
                    color = Vinko.Coral,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = "SoyVinko Móvil v1.2.0 · Venezuela",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Vinko.Subtle,
                modifier = Modifier.padding(start = 20.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    title: String,
    tint: Color = Vinko.Primary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (tint == Vinko.Coral) Vinko.Coral else Vinko.Ink,
        )
    }
}
