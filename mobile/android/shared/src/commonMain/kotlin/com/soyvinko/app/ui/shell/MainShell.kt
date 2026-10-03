package com.soyvinko.app.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soyvinko.app.data.AppServices
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.common.LocalApp
import com.soyvinko.app.ui.common.LocalSession
import com.soyvinko.app.ui.common.LocalShellScope
import com.soyvinko.app.ui.common.LocalSnackbar
import com.soyvinko.app.ui.common.LocalUser
import com.soyvinko.app.ui.common.EmptyBox
import com.soyvinko.app.ui.common.ScreenScaffold
import com.soyvinko.app.ui.common.SessionActions
import com.soyvinko.app.ui.feature.admin.adminGraph
import com.soyvinko.app.ui.feature.contracts.contractsGraph
import com.soyvinko.app.ui.feature.explore.exploreGraph
import com.soyvinko.app.ui.feature.pro.proGraph
import com.soyvinko.app.ui.feature.profile.profileGraph
import com.soyvinko.app.ui.feature.wallet.walletGraph
import com.soyvinko.app.ui.screens.HomeScreen
import com.soyvinko.app.ui.theme.Vinko

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "Inicio", Icons.Outlined.Home),
    Tab(Routes.EXPLORE, "Explorar", Icons.Outlined.Explore),
    Tab(Routes.CONTRACTS, "Servicios", Icons.Outlined.WorkOutline),
    Tab(Routes.WALLET, "Wallet", Icons.Outlined.AccountBalanceWallet),
    Tab(Routes.PROFILE, "Perfil", Icons.Outlined.Person),
)

/** App con sesión iniciada: todas las secciones son nativas. */
@Composable
fun MainShell(app: AppServices, user: User, session: SessionActions, onUserUpdated: (User) -> Unit) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val shellScope = rememberCoroutineScope()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    CompositionLocalProvider(LocalApp provides app, LocalUser provides user, LocalSnackbar provides snackbar, LocalSession provides session, LocalShellScope provides shellScope) {
        Scaffold(
            containerColor = Vinko.Cream,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (route in Routes.TABS) {
                    NavigationBar(containerColor = Color.White) {
                        TABS.forEach { tab ->
                            NavigationBarItem(
                                selected = route == tab.route,
                                onClick = {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().route.orEmpty()) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Vinko.Primary,
                                    selectedTextColor = Vinko.Primary,
                                    indicatorColor = Vinko.Tint,
                                    unselectedIconColor = Vinko.Muted,
                                    unselectedTextColor = Vinko.Muted,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                NavHost(navController = nav, startDestination = Routes.HOME) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            user = user,
                            onUserUpdated = onUserUpdated,
                            onNavigate = { nav.navigate(it) },
                        )
                    }
                    exploreGraph(nav)
                    contractsGraph(nav)
                    walletGraph(nav)
                    profileGraph(nav)
                    proGraph(nav)
                    adminGraph(nav)
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
