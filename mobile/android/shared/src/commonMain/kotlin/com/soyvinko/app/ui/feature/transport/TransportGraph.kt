package com.soyvinko.app.ui.feature.transport

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.soyvinko.app.ui.shell.Routes

import com.soyvinko.app.ui.common.AppMode
import com.soyvinko.app.ui.common.LocalAppMode

fun NavGraphBuilder.transportGraph(nav: NavHostController) {
    composable(Routes.TRANSPORT) {
        val mode = LocalAppMode.current
        if (mode == AppMode.SELLER) {
            RiderScreen(nav)
        } else {
            NewShipmentScreen(nav)
        }
    }
    composable(Routes.TRANSPORT_NEW) {
        NewShipmentScreen(nav)
    }
    composable(Routes.SHIPMENT_TRACKING) { entry ->
        val id = entry.arguments?.getString("id").orEmpty()
        ShipmentTrackingScreen(id, nav)
    }
}
