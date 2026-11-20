package com.soyvinko.app.ui.feature.explore

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.soyvinko.app.ui.shell.Routes

/** Punto de entrada de la sección: Explorar, perfiles, favoritos y solicitudes del cliente. */
fun NavGraphBuilder.exploreGraph(nav: NavHostController) {
    composable(Routes.EXPLORE) { ExploreScreen(nav) }
    composable(Routes.CATEGORIES) { CategoriesScreen(nav) }
    composable(Routes.PROFESSIONAL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
        ProfessionalScreen(nav, entry.arguments?.getString("id").orEmpty())
    }
    composable(Routes.FAVORITES) { FavoritesScreen(nav) }
    composable(Routes.REQUESTS) { NewRequestScreen(nav, "", "") }
    composable(Routes.REQUEST, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
        RequestDetailScreen(nav, entry.arguments?.getString("id").orEmpty())
    }
    composable(
        Routes.NEW_REQUEST,
        arguments = listOf(
            navArgument("professional") {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument("offering") {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) { entry ->
        NewRequestScreen(
            nav,
            professional = entry.arguments?.getString("professional").orEmpty(),
            offering = entry.arguments?.getString("offering").orEmpty(),
        )
    }
}
