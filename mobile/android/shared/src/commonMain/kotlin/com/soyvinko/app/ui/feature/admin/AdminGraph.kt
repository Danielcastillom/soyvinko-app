package com.soyvinko.app.ui.feature.admin

import com.soyvinko.app.ui.common.encodeUrlComponent
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.soyvinko.app.ui.shell.Routes

// Rutas internas de la sección (siempre con el prefijo "admin/").
const val ADMIN_KYC = "admin/kyc/{id}?reason={reason}"
const val ADMIN_TICKET = "admin/ticket/{id}"
const val ADMIN_CATEGORIES = "admin/categories"

fun adminKyc(id: String, reason: String) = "admin/kyc/$id?reason=${encodeUrlComponent(reason)}"
fun adminTicket(id: String) = "admin/ticket/$id"

/** Punto de entrada de la sección: panel de administración del equipo SoyVinko (port nativo de pages/Admin.tsx). */
fun NavGraphBuilder.adminGraph(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }

    composable(Routes.ADMIN) {
        AdminHomeScreen(
            onBack = back,
            onOpen = { nav.navigate(Routes.adminResource(it)) },
            onCategories = { nav.navigate(ADMIN_CATEGORIES) },
        )
    }
    composable(Routes.ADMIN_RESOURCE, arguments = listOf(navArgument("resource") { type = NavType.StringType })) { entry ->
        AdminResourceScreen(
            resource = entry.arguments?.getString("resource").orEmpty(),
            onBack = back,
            onOpenKyc = { id, reason -> nav.navigate(adminKyc(id, reason)) },
            onOpenTicket = { nav.navigate(adminTicket(it)) },
        )
    }
    composable(
        ADMIN_KYC,
        arguments = listOf(
            navArgument("id") { type = NavType.StringType },
            navArgument("reason") { type = NavType.StringType; defaultValue = KYC_OPEN_REASON },
        ),
    ) { entry ->
        AdminKycCaseScreen(
            id = entry.arguments?.getString("id").orEmpty(),
            reason = entry.arguments?.getString("reason")?.takeIf { it.trim().length >= 10 } ?: KYC_OPEN_REASON,
            onBack = back,
        )
    }
    composable(ADMIN_TICKET, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
        AdminTicketScreen(id = entry.arguments?.getString("id").orEmpty(), onBack = back)
    }
    composable(ADMIN_CATEGORIES) { AdminCategoriesScreen(onBack = back) }
}
