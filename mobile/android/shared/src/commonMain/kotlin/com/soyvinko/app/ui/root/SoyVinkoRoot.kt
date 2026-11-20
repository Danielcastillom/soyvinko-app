package com.soyvinko.app.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.LocalPlatformContext
import com.soyvinko.app.AppGraph
import com.soyvinko.app.biometric.BiometricResult
import com.soyvinko.app.biometric.rememberBiometricManager
import com.soyvinko.app.data.User
import com.soyvinko.app.tracking.LocationTracking
import com.soyvinko.app.ui.AppViewModels
import com.soyvinko.app.ui.SessionState
import com.soyvinko.app.ui.SessionViewModel
import com.soyvinko.app.ui.common.SessionActions
import com.soyvinko.app.ui.components.Banner
import com.soyvinko.app.ui.components.BannerTone
import com.soyvinko.app.ui.components.BrandLogo
import com.soyvinko.app.ui.components.PrimaryButton
import com.soyvinko.app.ui.feature.profile.LegalScreen
import com.soyvinko.app.ui.screens.ForgotPasswordScreen
import com.soyvinko.app.ui.screens.LoginScreen
import com.soyvinko.app.ui.screens.RegisterScreen
import com.soyvinko.app.ui.screens.WelcomeScreen
import com.soyvinko.app.ui.shell.MainShell
import com.soyvinko.app.ui.theme.Vinko

/**
 * Raíz de la app, igual en Android e iOS: recupera la sesión y muestra la bienvenida/inicio de
 * sesión o la app completa. Incluye bloqueo biométrico automático cuando se deja la app y se vuelve a ella.
 */
@Composable
fun SoyVinkoRoot(
    session: SessionViewModel = viewModel(factory = AppViewModels),
    loading: @Composable () -> Unit = { BrandSplash() },
) {
    val state by session.state.collectAsStateWithLifecycle()
    val context = LocalPlatformContext.current
    var isBiometricLocked by remember { mutableStateOf(false) }
    var hasLeftApp by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (state is SessionState.SignedIn && AppGraph.services.auth.isBiometricEnabled() && AppGraph.services.auth.isBiometricLockOnLeave()) {
            hasLeftApp = true
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasLeftApp && state is SessionState.SignedIn && AppGraph.services.auth.isBiometricEnabled() && AppGraph.services.auth.isBiometricLockOnLeave()) {
            isBiometricLocked = true
        }
    }

    when (val s = state) {
        SessionState.Loading -> loading()
        SessionState.SignedOut -> {
            isBiometricLocked = false
            hasLeftApp = false
            SignedOutNav(session)
        }
        is SessionState.SignedIn -> {
            if (isBiometricLocked) {
                BiometricLockOverlay(
                    user = s.user,
                    onUnlocked = {
                        isBiometricLocked = false
                        hasLeftApp = false
                    },
                    onLogout = {
                        isBiometricLocked = false
                        hasLeftApp = false
                        LocationTracking.stop(context)
                        session.logout()
                    },
                )
            } else {
                MainShell(
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
    }
}

@Composable
private fun BiometricLockOverlay(
    user: User,
    onUnlocked: () -> Unit,
    onLogout: () -> Unit,
) {
    val biometric = rememberBiometricManager()
    val biometricStatus = remember { biometric.checkStatus() }
    var promptFailed by remember { mutableStateOf<String?>(null) }

    val triggerUnlock = {
        promptFailed = null
        biometric.authenticate(
            title = "Desbloquear SoyVinko",
            subtitle = "Confirma tu identidad para continuar",
            onResult = { result ->
                when (result) {
                    is BiometricResult.Success -> onUnlocked()
                    is BiometricResult.Error -> promptFailed = result.message
                    is BiometricResult.Canceled -> {}
                }
            },
        )
    }

    LaunchedEffect(Unit) {
        if (biometricStatus.isAvailable) {
            triggerUnlock()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Vinko.Cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            BrandLogo()
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Vinko.MintBg, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = "Bloqueado",
                    tint = Vinko.Mint,
                    modifier = Modifier.size(44.dp),
                )
            }
            Text(
                text = "SoyVinko protegido",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Vinko.Ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Tu sesión de ${user.firstName.ifBlank { user.email }} está protegida. Usa tu huella dactilar o Face ID para continuar.",
                style = MaterialTheme.typography.bodyMedium,
                color = Vinko.Muted,
                textAlign = TextAlign.Center,
            )

            promptFailed?.let {
                Banner(it, BannerTone.Error)
            }

            Spacer(Modifier.height(8.dp))

            PrimaryButton(
                text = "Desbloquear con biometría",
                onClick = triggerUnlock,
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(onClick = onLogout) {
                Text(
                    text = "Cerrar sesión",
                    style = MaterialTheme.typography.labelLarge,
                    color = Vinko.Coral,
                )
            }
        }
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
                onLegal = { doc -> nav.navigate("legal/$doc") },
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
