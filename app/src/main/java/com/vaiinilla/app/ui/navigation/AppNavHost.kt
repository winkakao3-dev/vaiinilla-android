package com.vaiinilla.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vaiinilla.app.ui.order.OrderFlowViewModel
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    val orderFlowViewModel: OrderFlowViewModel = viewModel()
    val state by orderFlowViewModel.uiState

    LaunchedEffect(state.createdOrder?.summary?.id) {
        if (state.createdOrder != null && navController.currentDestination?.route != Routes.CONFIRMATION) {
            navController.navigate(Routes.CONFIRMATION) {
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.CATALOG) {
        composable(Routes.CATALOG) {
            CatalogScreen(
                state = state,
                onRetry = orderFlowViewModel::refresh,
                onSearchChange = orderFlowViewModel::updateSearch,
                onCategorySelected = orderFlowViewModel::selectCategory,
                onProductSelected = orderFlowViewModel::openProduct,
                onDismissProduct = orderFlowViewModel::closeProduct,
                onToggleOption = orderFlowViewModel::toggleOption,
                onClearOptionalGroup = orderFlowViewModel::clearOptionalGroup,
                onQuantityChange = orderFlowViewModel::changeSelectedQuantity,
                onAddProduct = orderFlowViewModel::addSelectedProductToCart,
                onOpenCart = {
                    orderFlowViewModel.closeProduct()
                    navController.navigate(Routes.CART) { launchSingleTop = true }
                },
            )
        }
        composable(Routes.CART) {
            CartScreen(
                state = state,
                onMenu = { navController.popBackStack(Routes.CATALOG, inclusive = false) },
                onQuantityChange = orderFlowViewModel::changeCartLineQuantity,
                onNotesChange = orderFlowViewModel::updateKitchenNotes,
                onConfirm = orderFlowViewModel::submitOrder,
            )
        }
        composable(Routes.CONFIRMATION) {
            OrderConfirmationScreen(
                order = state.createdOrder,
                onReturnToMenu = {
                    orderFlowViewModel.clearCreatedOrder()
                    navController.navigate(Routes.CATALOG) {
                        popUpTo(Routes.CATALOG) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
