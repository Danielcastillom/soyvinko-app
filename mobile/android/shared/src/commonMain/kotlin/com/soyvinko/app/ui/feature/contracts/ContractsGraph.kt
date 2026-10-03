package com.soyvinko.app.ui.feature.contracts

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.soyvinko.app.ui.shell.Routes

/**
 * Servicios contratados: lista (pestaña inferior), detalle del contrato, chat y ubicación en vivo.
 * Espejo nativo de frontend/src/pages/Contracts.tsx sobre las operaciones api.* de contratos.
 */
fun NavGraphBuilder.contractsGraph(nav: NavHostController) {
    composable(Routes.CONTRACTS) { ContractsListScreen(nav) }
    composable(Routes.CONTRACT) { entry -> ContractDetailScreen(entry.arguments?.getString("id").orEmpty(), nav) }
    composable(Routes.CHAT) { entry -> ChatScreen(entry.arguments?.getString("id").orEmpty(), nav) }
    composable(Routes.TRACKING) { entry -> TrackingScreen(entry.arguments?.getString("id").orEmpty(), nav) }
}
