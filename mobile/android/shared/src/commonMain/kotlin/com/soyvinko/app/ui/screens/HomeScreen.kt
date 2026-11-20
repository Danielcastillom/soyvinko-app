package com.soyvinko.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.soyvinko.app.data.User
import com.soyvinko.app.ui.feature.explore.ExploreScreen

/**
 * Pantalla principal (Inicio): experiencia minimalista de exploración y marketplace,
 * enfocada en la búsqueda y contratación directa de servicios y profesionales sin saturación.
 */
@Composable
fun HomeScreen(
    nav: NavHostController,
    user: User? = null,
    onUserUpdated: ((User) -> Unit)? = null,
    onNavigate: ((String) -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
) {
    ExploreScreen(
        nav = nav,
        onOpenDrawer = onOpenDrawer,
    )
}

@Composable
fun HomeScreen(
    user: User? = null,
    onUserUpdated: ((User) -> Unit)? = null,
    onNavigate: ((String) -> Unit)? = null,
    onOpenDrawer: () -> Unit = {},
) {
    val nav = androidx.navigation.compose.rememberNavController()
    HomeScreen(
        nav = nav,
        user = user,
        onUserUpdated = onUserUpdated,
        onNavigate = onNavigate,
        onOpenDrawer = onOpenDrawer,
    )
}
