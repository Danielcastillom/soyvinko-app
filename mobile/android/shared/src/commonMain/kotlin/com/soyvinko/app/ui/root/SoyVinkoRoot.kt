package com.soyvinko.app.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.LocalPlatformContext
import com.soyvinko.app.AppGraph
import com.soyvinko.app.tracking.LocationTracking
import com.soyvinko.app.ui.AppViewModels
import com.soyvinko.app.ui.SessionState
import com.soyvinko.app.ui.SessionViewModel
import com.soyvinko.app.ui.common.SessionActions
import com.soyvinko.app.ui.components.BrandLogo
import com.soyvinko.app.ui.feature.profile.LegalScreen
import com.soyvinko.app.ui.screens.ForgotPasswordScreen
import com.soyvinko.app.ui.screens.LoginScreen
import com.soyvinko.app.ui.screens.RegisterScreen
import com.soyvinko.app.ui.screens.WelcomeScreen
import com.soyvinko.app.ui.shell.MainShell
import com.soyvinko.app.ui.theme.Vinko

/**
 * Raíz de la app, igual en Android e iOS: recupera la sesión y muestra la bienvenida/inicio de
 * sesión o la app completa. `loading` se dibuja mientras se recupera la sesión (Android mantiene ahí
 * su pantalla de arranque del sistema).
 */
@Composable
fun SoyVinkoRoot(
    session: SessionViewModel = viewModel(factory = AppViewModels),
    loading: @Composable () -> Unit = { BrandSplash() },
) {
    val state by session.state.collectAsStateWithLifecycle()
    val context = LocalPlatformContext.current
    when (val s = state) {
        SessionState.Loading -> loading()
        SessionState.SignedOut -> SignedOutNav(session)
        is SessionState.SignedIn -> MainShell(
            app = AppGraph.services,
            user = s.user,
            session = SessionActions(reloadUser = session::reloadUser, logout = {
                LocationTracking.stop(context)
                session.logout()
            }),
            onUserUpdated = session::updateUser,
        )
    }
}

/** Marca centrada mientras arranca (iOS no tiene pantalla de arranque animada del sistema). */
@Composable
fun BrandSplash() {
    Box(Modifier.fillMaxSize().background(Vinko.Cream), contentAlignment = Alignment.Center) {
        BrandLogo()
    }
}

@Composable
private fun SignedOutNav(session: SessionViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onLogin = { nav.navigate("login") },
                onRegister = { nav.navigate("register") },
                expired = session.expiredNotice,
            )
        }
        composable("login") {
            LoginScreen(
                vm = viewModel(factory = AppViewModels),
                onLoggedIn = session::signedIn,
                onRegister = { nav.navigate("register") { popUpTo("welcome") } },
                onForgot = { nav.navigate("forgot") },
                onBack = { nav.popBackStack() },
            )
        }
        composable("register") {
            RegisterScreen(
                vm = viewModel(factory = AppViewModels),
                onRegistered = session::signedIn,
                onLogin = { nav.navigate("login") { popUpTo("welcome") } },
                onBack = { nav.popBackStack() },
                onLegal = { doc -> nav.navigate("legal/$doc") },
            )
        }
        // Términos y privacidad nativos también antes de iniciar sesión.
        composable("legal/{doc}") { entry ->
            LegalScreen(initial = entry.arguments?.getString("doc"), onBack = { nav.popBackStack() })
        }
        composable("forgot") {
            ForgotPasswordScreen(vm = viewModel(factory = AppViewModels), onBack = { nav.popBackStack() })
        }
    }
}
