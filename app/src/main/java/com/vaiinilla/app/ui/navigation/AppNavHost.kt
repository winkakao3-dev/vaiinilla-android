package com.vaiinilla.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vaiinilla.app.di.DataSourceResolverEntryPoint
import com.vaiinilla.app.domain.model.OperationalRole
import dagger.hilt.android.EntryPointAccessors
import com.vaiinilla.app.ui.operational.OperationalViewModel
import com.vaiinilla.app.ui.order.OrderFlowViewModel
import com.vaiinilla.app.ui.screens.AssistantChatScreen
import com.vaiinilla.app.ui.screens.AssistantScreen
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CashierOperationalScreen
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import com.vaiinilla.app.ui.screens.ReceiptStickerScreen
import com.vaiinilla.app.ui.screens.RoleSelectorScreen
import com.vaiinilla.app.ui.screens.SplashScreen
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import com.vaiinilla.app.ui.screens.WaiterOperationalScreen
import com.vaiinilla.app.ui.screens.WalletAccountScreen
import com.vaiinilla.app.ui.screens.WalletAddCardScreen
import com.vaiinilla.app.ui.screens.WalletAddMoneyScreen
import com.vaiinilla.app.ui.screens.WalletPaymentMethodsScreen
import com.vaiinilla.app.ui.screens.WalletScreen
import com.vaiinilla.app.ui.wallet.rememberWalletUiState

@Composable
fun AppNavHost(navController: NavHostController) {
    val orderFlowViewModel: OrderFlowViewModel = viewModel()
    val operationalViewModel: OperationalViewModel = viewModel()
    val orderState by orderFlowViewModel.uiState
    val operationalState by operationalViewModel.uiState
    val walletState = rememberWalletUiState()
    val context = LocalContext.current
    val dataSourceResolver = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DataSourceResolverEntryPoint::class.java,
        ).effectiveDataSourceResolver()
    }
    var testOnlyMode by remember { mutableStateOf(dataSourceResolver.isTestOnlyMode) }

    LaunchedEffect(testOnlyMode) {
        dataSourceResolver.isTestOnlyMode = testOnlyMode
        orderFlowViewModel.applyTestOnlyMode(testOnlyMode)
        operationalViewModel.onRuntimeModeChanged()
    }

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

    val activeOrder = orderState.createdOrder
        ?: operationalState.selectedOrder
        ?: operationalState.orders.firstOrNull()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.ROLE_SELECTOR) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.ROLE_SELECTOR) {
            RoleSelectorScreen(
                testOnlyMode = testOnlyMode,
                onTestOnlyModeChange = { enabled -> testOnlyMode = enabled },
                onRoleSelected = { role ->
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
                },
            )
        }

        composable(Routes.CATALOG) {
            CatalogScreen(
                state = orderState,
                activeOrder = activeOrder,
                onRetry = orderFlowViewModel::refresh,
                onSearchChange = orderFlowViewModel::updateSearch,
                onCategorySelected = orderFlowViewModel::selectCategory,
                onProductSelected = orderFlowViewModel::openProduct,
                onDismissProduct = orderFlowViewModel::closeProduct,
                onToggleOption = orderFlowViewModel::toggleOption,
                onClearOptionalGroup = orderFlowViewModel::clearOptionalGroup,
                onQuantityChange = orderFlowViewModel::changeSelectedQuantity,
                onAddProduct = orderFlowViewModel::addSelectedProductToCart,
                onOpenCart = { navController.navigateStudent(Routes.CART) },
                onOpenTracking = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                onOpenAssistant = { navController.navigateStudent(Routes.ASSISTANT) },
                onOpenWallet = { navController.navigateStudent(Routes.WALLET) },
            )
        }

        composable(Routes.ASSISTANT) {
            AssistantScreen(
                state = orderState,
                onOpenChat = { navController.navigate(Routes.ASSISTANT_CHAT) { launchSingleTop = true } },
                onOpenProduct = { productId ->
                    orderFlowViewModel.openProduct(productId)
                    navController.navigateStudent(Routes.CATALOG)
                },
                onMenu = { navController.navigateStudent(Routes.CATALOG) },
                onOrders = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                onWallet = { navController.navigateStudent(Routes.WALLET) },
                onCart = { navController.navigateStudent(Routes.CART) },
            )
        }

        composable(Routes.ASSISTANT_CHAT) {
            AssistantChatScreen(
                state = orderState,
                onClose = { navController.popBackStack() },
                onMenu = { navController.navigateStudent(Routes.CATALOG) },
                onOrders = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                onWallet = { navController.navigateStudent(Routes.WALLET) },
                onCart = { navController.navigateStudent(Routes.CART) },
            )
        }

        composable(Routes.WALLET) {
            WalletScreen(
                state = orderState,
                balance = walletState.balance,
                onAddMoney = { navController.navigate("wallet/add-money?method=card") },
                onPaymentMethods = { navController.navigate(Routes.WALLET_METHODS) },
                onAccount = { navController.navigate(Routes.WALLET_ACCOUNT) },
                onMenu = { navController.navigateStudent(Routes.CATALOG) },
                onAssistant = { navController.navigateStudent(Routes.ASSISTANT) },
                onOrders = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                onCart = { navController.navigateStudent(Routes.CART) },
            )
        }

        composable(
            route = Routes.WALLET_ADD_MONEY,
            arguments = listOf(navArgument("method") { type = NavType.StringType; defaultValue = "card" }),
        ) { entry ->
            WalletAddMoneyScreen(
                walletState = walletState,
                initialMethod = entry.arguments?.getString("method") ?: "card",
                onBack = { navController.popBackStack() },
                onCreditBalance = { amount -> walletState.balance += amount },
            )
        }

        composable(Routes.WALLET_METHODS) {
            WalletPaymentMethodsScreen(
                walletState = walletState,
                onBack = { navController.popBackStack() },
                onAddCard = { navController.navigate(Routes.WALLET_ADD_CARD) },
            )
        }

        composable(Routes.WALLET_ADD_CARD) {
            WalletAddCardScreen(
                walletState = walletState,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(Routes.WALLET_ACCOUNT) {
            WalletAccountScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CART) {
            LaunchedEffect(Unit) {
                orderFlowViewModel.refresh()
            }
            CartScreen(
                state = orderState,
                walletBalance = walletState.balance,
                onMenu = { navController.navigateStudent(Routes.CATALOG) },
                onQuantityChange = orderFlowViewModel::changeCartLineQuantity,
                onNotesChange = orderFlowViewModel::updateKitchenNotes,
                onDestinationChange = orderFlowViewModel::updateCheckoutDestination,
                onPaymentChange = orderFlowViewModel::updateCheckoutPayment,
                onConfirm = {
                    orderFlowViewModel.submitOrder(
                        walletBalance = walletState.balance,
                        onWalletDebit = { amount -> walletState.balance -= amount },
                    )
                },
                onOpenTracking = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                onOpenAssistant = { navController.navigateStudent(Routes.ASSISTANT) },
                onOpenWallet = { navController.navigateStudent(Routes.WALLET) },
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
                    navController.navigateStudent(Routes.STUDENT_TRACKING)
                },
                onViewSticker = { navController.navigate(Routes.RECEIPT_STICKER) },
            )
        }

        composable(Routes.RECEIPT_STICKER) {
            ReceiptStickerScreen(
                order = orderState.createdOrder ?: operationalState.selectedOrder,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.STUDENT_TRACKING) {
            LaunchedEffect(Unit) {
                operationalViewModel.setRole(OperationalRole.CLIENT)
            }
            StudentTrackingScreen(
                state = operationalState,
                orderState = orderState,
                onMenu = { navController.navigateStudent(Routes.CATALOG) },
                onAssistant = { navController.navigateStudent(Routes.ASSISTANT) },
                onWallet = { navController.navigateStudent(Routes.WALLET) },
                onCart = { navController.navigateStudent(Routes.CART) },
                onOpenCatalog = { navController.navigateStudent(Routes.CATALOG) },
                onSelectOrder = operationalViewModel::selectOrder,
                onViewSticker = { navController.navigate(Routes.RECEIPT_STICKER) },
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

private fun NavHostController.navigateStudent(route: String) {
    navigate(route) { launchSingleTop = true }
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
