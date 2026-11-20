package com.soyvinko.app.ui.feature.profile

import com.soyvinko.app.ui.common.decodeUrlComponent
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.soyvinko.app.ui.shell.Routes

// Rutas internas de la sección (siempre con el prefijo "profile/").
const val ADD_ADDRESS = "profile/addresses/new"
const val KYC_SUBMIT = "profile/kyc/submit"
const val NEW_TICKET = "profile/support/new"

/** Punto de entrada de la sección: perfil, cuenta, direcciones, verificación, seguridad, novedades, soporte y legales. */
fun NavGraphBuilder.profileGraph(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }

    composable(Routes.PROFILE) { ProfileHubScreen(nav) }
    composable(Routes.ACCOUNT) { AccountScreen(onBack = back) }

    composable(Routes.ADDRESSES) { AddressesScreen(onBack = back, onAdd = { nav.navigate(ADD_ADDRESS) }) }
    composable(ADD_ADDRESS) { AddAddressScreen(onBack = back, onSaved = back) }

    composable(Routes.KYC) { KycScreen(onBack = back, onStart = { nav.navigate(KYC_SUBMIT) }) }
    composable(KYC_SUBMIT) { KycSubmitScreen(onBack = back, onDone = back) }

    composable(Routes.SECURITY) { SecurityScreen(onBack = back) }

    composable(Routes.NOTIFICATIONS) { NotificationsScreen(onBack = back, onOpen = { nav.navigate(it) }) }

    composable(Routes.SUPPORT) {
        SupportScreen(onBack = back, onNew = { nav.navigate(NEW_TICKET) }, onOpen = { nav.navigate(Routes.ticket(it)) })
    }
    composable(NEW_TICKET) {
        NewTicketScreen(
            onBack = back,
            onCreated = { id ->
                if (id == null) {
                    nav.popBackStack()
                } else {
                    nav.navigate(Routes.ticket(id)) { popUpTo(NEW_TICKET) { inclusive = true } }
                }
            },
        )
    }
    composable(Routes.TICKET, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
        TicketScreen(id = entry.arguments?.getString("id").orEmpty(), onBack = back)
    }

    composable(Routes.LEGAL, arguments = listOf(navArgument("doc") { type = NavType.StringType })) { entry ->
        LegalScreen(initial = entry.arguments?.getString("doc")?.let(::decodeUrlComponent), onBack = back)
    }
}
