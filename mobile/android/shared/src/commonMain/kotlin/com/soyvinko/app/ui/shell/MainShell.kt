package com.soyvinko.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soyvinko.app.data.AppServices
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.common.AppMode
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalAppMode
import com.soyvinko.app.ui.common.LocalOpenDrawer
import com.soyvinko.app.ui.common.LocalSession
import com.soyvinko.app.ui.common.LocalSetAppMode
import com.soyvinko.app.ui.common.LocalShellScope
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.SessionActions
import com.soyvinko.app.ui.components.VinkoDrawerSheet
import com.soyvinko.app.ui.feature.admin.adminGraph
import com.soyvinko.app.ui.feature.contracts.contractsGraph
import com.soyvinko.app.ui.feature.explore.exploreGraph
import com.soyvinko.app.ui.feature.pro.proGraph
import com.soyvinko.app.ui.feature.profile.profileGraph
import com.soyvinko.app.ui.feature.transport.transportGraph
import com.soyvinko.app.ui.feature.wallet.walletGraph
import com.soyvinko.app.ui.screens.HomeScreen
import com.soyvinko.app.ui.theme.Vinko
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soyvinko.app.ui.feature.pro.MarketCart
import com.soyvinko.app.ui.theme.Majesticons

private data class Tab(
    val route: String,
    val label: String,
    val lineIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val solidIcon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val CLIENT_TABS = listOf(
    Tab(Routes.HOME, "Inicio", Majesticons.Home, Majesticons.HomeSolid),
    Tab(Routes.REQUESTS, "Anunciar", Majesticons.Megaphone, Majesticons.MegaphoneSolid),
    Tab(Routes.CONTRACTS, "Servicios", Majesticons.Briefcase, Majesticons.BriefcaseSolid),
    Tab(Routes.PROFILE, "Perfil", Majesticons.User, Majesticons.UserSolid),
)

private val SELLER_TABS = listOf(
    Tab(Routes.MY_STORE, "Tienda", Icons.Outlined.Inventory2, Icons.Outlined.Inventory2),
    Tab(Routes.PRO, "Servicios", Icons.Outlined.WorkOutline, Icons.Outlined.WorkOutline),
    Tab(Routes.TRANSPORT, "Rider", Majesticons.Truck, Majesticons.TruckSolid),
    Tab(Routes.ORDERS, "Órdenes", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    Tab(Routes.MARKETPLACE, "Market", Icons.Outlined.Storefront, Icons.Filled.Storefront),
)

/** App con sesión iniciada: integra navegación nativa, Bottom Bar y Drawer lateral estilo FlutterFlow. */
@Composable
fun MainShell(app: AppServices, user: User, session: SessionActions, onUserUpdated: (User) -> Unit) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val shellScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    val initialMode = remember {
        if (app.storage.appMode == AppMode.SELLER.name) AppMode.SELLER else AppMode.CLIENT
    }
    var appMode by rememberSaveable { mutableStateOf(initialMode) }

    fun setMode(newMode: AppMode) {
        appMode = newMode
        app.storage.appMode = newMode.name
    }

    // Sincroniza automáticamente el modo si la ruta pertenece a una sección específica
    LaunchedEffect(route) {
        if (route != null) {
            if (route in Routes.SELLER_TABS && appMode != AppMode.SELLER) {
                setMode(AppMode.SELLER)
            } else if (route in setOf(Routes.HOME, Routes.REQUESTS, Routes.CONTRACTS) && appMode != AppMode.CLIENT) {
                setMode(AppMode.CLIENT)
            }
        }
    }

    CompositionLocalProvider(
        LocalApp provides app,
        LocalUser provides user,
        LocalSnackbar provides snackbar,
        LocalSession provides session,
        LocalShellScope provides shellScope,
        LocalAppMode provides appMode,
        LocalSetAppMode provides { setMode(it) },
        LocalOpenDrawer provides { shellScope.launch { drawerState.open() } },
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = route in Routes.TABS,
            drawerContent = {
                VinkoDrawerSheet(
                    user = user,
                    mode = appMode,
                    onModeChange = { newMode -> setMode(newMode) },
                    onNavigate = { target -> nav.navigate(target) },
                    onCloseDrawer = { shellScope.launch { drawerState.close() } },
                    onLogout = session.logout,
                )
            },
        ) {
            Scaffold(
                containerColor = Vinko.Cream,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (route in Routes.TABS) {
                        val currentTabs = if (appMode == AppMode.CLIENT) CLIENT_TABS else SELLER_TABS
                        Surface(
                            color = Color.White,
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    currentTabs.forEach { tab ->
                                        val selected = route == tab.route
                                        val badgeCount = when (tab.route) {
                                            Routes.MARKETPLACE -> MarketCart.count
                                            else -> 0
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable {
                                                    if (tab.route == route) {
                                                        // Ya está en la pestaña activa
                                                        return@clickable
                                                    }
                                                    if (tab.route == Routes.HOME) {
                                                        val popped = nav.popBackStack(Routes.HOME, inclusive = false)
                                                        if (!popped) {
                                                            nav.navigate(Routes.HOME) {
                                                                popUpTo(nav.graph.findStartDestination().id) { inclusive = false }
                                                                launchSingleTop = true
                                                            }
                                                        }
                                                    } else {
                                                        nav.navigate(tab.route) {
                                                            popUpTo(nav.graph.findStartDestination().id) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (selected) Color(0xFFEDE9FE) else Color.Transparent)
                                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (selected) tab.solidIcon else tab.lineIcon,
                                                        contentDescription = tab.label,
                                                        tint = if (selected) Vinko.Primary else Color(0xFF6B7280),
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                    if (badgeCount > 0) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .background(Vinko.Primary, CircleShape)
                                                                .padding(horizontal = 3.dp, vertical = 1.dp),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Text(
                                                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                                                color = Color.White,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                lineHeight = 9.sp,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                    NavHost(navController = nav, startDestination = Routes.HOME) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                nav = nav,
                                user = user,
                                onUserUpdated = onUserUpdated,
                                onNavigate = { nav.navigate(it) },
                                onOpenDrawer = { shellScope.launch { drawerState.open() } },
                            )
                        }
                        exploreGraph(nav)
                        contractsGraph(nav)
                        walletGraph(nav)
                        profileGraph(nav)
                        proGraph(nav)
                        transportGraph(nav)
                        adminGraph(nav)
                    }
                }
            }
        }
    }
}

/** Marcador temporal mientras una sección se implementa. */
@Composable
fun ComingSoon(name: String, onBack: () -> Unit) {
    ScreenScaffold(name, onBack = onBack) { p ->
        Box(Modifier.padding(p)) { EmptyBox("En construcción", "Esta sección estará disponible pronto.") }
    }
}
