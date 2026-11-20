package com.soyvinko.app.ui.feature.wallet

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.soyvinko.app.ui.shell.Routes

/** Aviso que deja el formulario de cuenta de retiro para la pantalla de retiros. */
private const val NOTICE = "wallet_notice"

/** Punto de entrada de la sección: registra sus pantallas. */
fun NavGraphBuilder.walletGraph(nav: NavHostController) {
    composable(Routes.WALLET) {
        WalletScreen(
            onNavigate = { nav.navigate(it) },
            onBack = { nav.popBackStack() },
        )
    }
    composable(Routes.WALLET_HISTORY) {
        WalletHistoryScreen(onBack = { nav.popBackStack() })
    }
    composable(Routes.WITHDRAW) { entry ->
        val notice by entry.savedStateHandle.getStateFlow<String?>(NOTICE, null).collectAsStateWithLifecycle()
        WithdrawScreen(
            onBack = { nav.popBackStack() },
            onAddMethod = { nav.navigate(WalletRoutes.ADD_PAYOUT_METHOD) },
            notice = notice,
            onNoticeShown = { entry.savedStateHandle[NOTICE] = null },
        )
    }
    composable(WalletRoutes.PLANS) {
        VinkoPlansScreen(onBack = { nav.popBackStack() })
    }
    composable(WalletRoutes.ADD_PAYOUT_METHOD) {
        AddPayoutMethodScreen(
            onBack = { nav.popBackStack() },
            onSaved = { message ->
                nav.previousBackStackEntry?.savedStateHandle?.set(NOTICE, message)
                nav.popBackStack()
            },
        )
    }
}
