package com.vaiinilla.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vaiinilla.app.ui.catalog.CatalogViewModel
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.StartScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    val catalogViewModel: CatalogViewModel = viewModel()
    val state by catalogViewModel.uiState

    NavHost(navController = navController, startDestination = Routes.START) {
        composable(Routes.START) {
            StartScreen(
                dataSourceMode = state.dataSourceMode,
                onOpenCatalog = { navController.navigate(Routes.CATALOG) },
            )
        }
        composable(Routes.CATALOG) {
            CatalogScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onRetry = catalogViewModel::refresh,
            )
        }
    }
}
