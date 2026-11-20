package com.soyvinko.app.ui.feature.pro

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.soyvinko.app.ui.shell.Routes

// Rutas internas de la sección (prefijos "pro/" y "marketplace/").
const val OPPORTUNITY = "pro/opportunities/{id}?mode={mode}"
const val BRANCH_NEW = "pro/branch/new"
const val CART = "marketplace/cart"

fun opportunityRoute(id: String, mode: String) = "pro/opportunities/$id?mode=$mode"

/** Punto de entrada de la sección: panel profesional y marketplace. */
fun NavGraphBuilder.proGraph(nav: NavHostController) {
    // Panel profesional
    composable(Routes.PRO) { ProScreen(nav) }
    composable(Routes.PRO_SETUP) { ProSetupScreen(nav) }
    composable(BRANCH_NEW) { BranchWizardScreen(nav) }
    composable(Routes.OPPORTUNITIES) { OpportunitiesScreen(nav) }
    composable(
        OPPORTUNITY,
        arguments = listOf(
            navArgument("id") {},
            navArgument("mode") { defaultValue = "onsite" },
        ),
    ) { entry ->
        OpportunityDetailScreen(
            id = entry.arguments?.getString("id").orEmpty(),
            mode = entry.arguments?.getString("mode") ?: "onsite",
            nav = nav,
        )
    }
    composable(Routes.MY_PROPOSALS) { ProposalsScreen(nav) }

    // Marketplace
    composable(Routes.MARKETPLACE) { MarketplaceScreen(nav) }
    composable(Routes.STORE) { entry -> StoreScreen(entry.arguments?.getString("id").orEmpty(), nav) }
    composable(CART) { CartScreen(nav) }
    composable(Routes.ORDERS) { OrdersScreen(nav) }
    composable(Routes.MY_STORE) { MyStoreScreen(nav) }
}
