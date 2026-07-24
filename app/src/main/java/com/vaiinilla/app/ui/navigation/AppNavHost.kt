package com.vaiinilla.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.auth.RoleAuthViewModel
import com.vaiinilla.app.ui.operational.OperationalViewModel
import com.vaiinilla.app.ui.order.OrderFlowViewModel
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CashierOperationalScreen
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import com.vaiinilla.app.ui.screens.RoleSelectorScreen
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import com.vaiinilla.app.ui.screens.WaiterOperationalScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    val orderFlowViewModel: OrderFlowViewModel = viewModel()
    val operationalViewModel: OperationalViewModel = viewModel()
    val roleAuthViewModel: RoleAuthViewModel = viewModel()
    val orderState by orderFlowViewModel.uiState
    val operationalState by operationalViewModel.uiState
    val roleAuthState by roleAuthViewModel.state

    LaunchedEffect(orderState.createdOrder?.summary?.id) {
        val created = orderState.createdOrder ?: return@LaunchedEffect
        operationalViewModel.setRole(OperationalRole.CLIENT)
        operationalViewModel.refreshOrder(created.summary.id)
        operationalViewModel.selectOrder(created.summary.id)
        if (navController.currentDestination?.route != Routes.CONFIRMATION) {
            navController.navigate(Routes.CONFIRMATION) {
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.ROLE_SELECTOR) {
        composable(Routes.ROLE_SELECTOR) {
            RoleSelectorScreen(
                loadingRole = roleAuthState.authenticatingRole.takeIf { roleAuthState.loading },
                errorMessage = roleAuthState.errorMessage,
                onDismissError = roleAuthViewModel::clearError,
                onRoleSelected = { role ->
                    roleAuthViewModel.authenticate(role) {
                        operationalViewModel.setRole(role)
                        when (role) {
                            OperationalRole.CLIENT -> {
                                orderFlowViewModel.refresh()
                                navController.navigate(Routes.CATALOG) {
                                    launchSingleTop = true
                                }
                            }
                            OperationalRole.CASHIER -> navController.navigate(Routes.CASHIER) {
                                launchSingleTop = true
                            }
                            OperationalRole.KITCHEN -> navController.navigate(Routes.KITCHEN) {
                                launchSingleTop = true
                            }
                            OperationalRole.WAITER -> navController.navigate(Routes.WAITER) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
            )
        }

        composable(Routes.CATALOG) {
            CatalogScreen(
                state = orderState,
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
                onOpenTracking = {
                    operationalViewModel.setRole(OperationalRole.CLIENT)
                    navController.navigate(Routes.STUDENT_TRACKING) { launchSingleTop = true }
                },
            )
        }

        composable(Routes.CART) {
            LaunchedEffect(Unit) {
                orderFlowViewModel.refresh()
            }
            CartScreen(
                state = orderState,
                onMenu = { navController.popBackStack(Routes.CATALOG, inclusive = false) },
                onQuantityChange = orderFlowViewModel::changeCartLineQuantity,
                onNotesChange = orderFlowViewModel::updateKitchenNotes,
                onConfirm = orderFlowViewModel::submitOrder,
            )
        }

        composable(Routes.CONFIRMATION) {
            OrderConfirmationScreen(
                order = orderState.createdOrder,
                onReturnToMenu = {
                    orderFlowViewModel.clearCreatedOrder()
                    navController.navigate(Routes.CATALOG) {
                        popUpTo(Routes.CATALOG) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onViewTracking = {
                    operationalViewModel.setRole(OperationalRole.CLIENT)
                    orderState.createdOrder?.summary?.id?.let(operationalViewModel::selectOrder)
                    navController.navigate(Routes.STUDENT_TRACKING) { launchSingleTop = true }
                },
            )
        }

        composable(Routes.STUDENT_TRACKING) {
            StudentTrackingScreen(
                state = operationalState,
                trackingHint = operationalViewModel::trackingHint,
                onBack = {
                    operationalViewModel.clearRole()
                    navController.navigate(Routes.ROLE_SELECTOR) {
                        popUpTo(Routes.ROLE_SELECTOR) { inclusive = true }
                    }
                },
                onOpenCatalog = {
                    navController.navigate(Routes.CATALOG) { launchSingleTop = true }
                },
                onSelectOrder = operationalViewModel::selectOrder,
                onClearSelection = { operationalViewModel.selectOrder(null) },
            )
        }

        composable(Routes.CASHIER) {
            CashierOperationalScreen(
                state = operationalState,
                onBack = returnToRoles(navController, operationalViewModel),
                onOpenCashSession = operationalViewModel::openCashRegister,
                onCollect = operationalViewModel::collectCash,
                onDeliver = operationalViewModel::deliver,
            )
        }

        composable(Routes.KITCHEN) {
            KitchenOperationalScreen(
                state = operationalState,
                onBack = returnToRoles(navController, operationalViewModel),
                onStart = operationalViewModel::startKitchen,
                onReady = operationalViewModel::markReady,
            )
        }

        composable(Routes.WAITER) {
            WaiterOperationalScreen(
                state = operationalState,
                onBack = returnToRoles(navController, operationalViewModel),
                onDeliver = operationalViewModel::deliver,
            )
        }
    }
}

private fun returnToRoles(
    navController: NavHostController,
    operationalViewModel: OperationalViewModel,
): () -> Unit = {
    operationalViewModel.clearRole()
    navController.navigate(Routes.ROLE_SELECTOR) {
        popUpTo(Routes.ROLE_SELECTOR) { inclusive = true }
    }
}
