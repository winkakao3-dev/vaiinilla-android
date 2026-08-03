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
import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.config.DemoFeatures
import com.vaiinilla.app.di.DataSourceResolverEntryPoint
import com.vaiinilla.app.domain.model.DemoCheckoutFixtures
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.auth.RoleAuthViewModel
import com.vaiinilla.app.ui.auth.student.StudentAuthViewModel
import com.vaiinilla.app.ui.discovery.GuestDiscoveryViewModel
import com.vaiinilla.app.ui.mode.AuthorizedAccessViewModel
import com.vaiinilla.app.ui.operational.OperationalViewModel
import com.vaiinilla.app.ui.order.OrderFlowViewModel
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.screens.AssistantChatScreen
import com.vaiinilla.app.ui.screens.AssistantScreen
import com.vaiinilla.app.ui.screens.AuthorizedModeScreen
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CashierOperationalScreen
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.DemoGalleryScreen
import com.vaiinilla.app.ui.screens.DiscoveryScreen
import com.vaiinilla.app.ui.screens.InvitationAcceptanceScreen
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import com.vaiinilla.app.ui.screens.ReceiptStickerScreen
import com.vaiinilla.app.ui.screens.RoleSelectorScreen
import com.vaiinilla.app.ui.screens.SplashScreen
import com.vaiinilla.app.ui.screens.StudentAuthLandingScreen
import com.vaiinilla.app.ui.screens.StudentForgotPasswordScreen
import com.vaiinilla.app.ui.screens.StudentLoginScreen
import com.vaiinilla.app.ui.screens.StudentRegisterScreen
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import com.vaiinilla.app.ui.screens.StudentVerifyEmailScreen
import com.vaiinilla.app.ui.screens.WaiterOperationalScreen
import com.vaiinilla.app.ui.screens.WalletAccountScreen
import com.vaiinilla.app.ui.screens.WalletAddCardScreen
import com.vaiinilla.app.ui.screens.WalletAddMoneyScreen
import com.vaiinilla.app.ui.screens.WalletPaymentMethodsScreen
import com.vaiinilla.app.ui.screens.WalletScreen
import com.vaiinilla.app.ui.wallet.rememberWalletUiState
import dagger.hilt.android.EntryPointAccessors

@Composable
fun AppNavHost(
    navController: NavHostController,
    pendingEstablishmentSlug: String? = null,
    pendingInvitationToken: String? = null,
    pendingMockInvitationToken: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    onInvitationConsumed: () -> Unit = {},
    onMockInvitationConsumed: () -> Unit = {},
) {
    val orderFlowViewModel: OrderFlowViewModel = viewModel()
    val operationalViewModel: OperationalViewModel = viewModel()
    val roleAuthViewModel: RoleAuthViewModel = viewModel()
    val studentAuthViewModel: StudentAuthViewModel = viewModel()
    val authorizedAccessViewModel: AuthorizedAccessViewModel = viewModel()
    val discoveryViewModel: GuestDiscoveryViewModel = viewModel()
    val orderState by orderFlowViewModel.uiState
    val operationalState by operationalViewModel.uiState
    val roleAuthState by roleAuthViewModel.state
    val studentAuthState by studentAuthViewModel.state
    val authorizedAccessState by authorizedAccessViewModel.state
    val discoveryState by discoveryViewModel.state
    val walletState = rememberWalletUiState()
    val context = LocalContext.current

    fun enterVenueAndOpenCatalog(venue: GuestVenueContext) {
        orderFlowViewModel.enterGuestVenue(venue)
        navController.navigate(Routes.CATALOG) {
            launchSingleTop = true
        }
    }

    LaunchedEffect(pendingEstablishmentSlug) {
        val slug = pendingEstablishmentSlug?.trim().orEmpty()
        if (slug.isEmpty()) return@LaunchedEffect
        discoveryViewModel.openSlug(
            slug = slug,
            onEntered = { venue ->
                enterVenueAndOpenCatalog(venue)
            },
            onFinished = onDeepLinkConsumed,
        )
    }
    val dataSourceResolver =
        remember {
            EntryPointAccessors
                .fromApplication(
                    context.applicationContext,
                    DataSourceResolverEntryPoint::class.java,
                ).effectiveDataSourceResolver()
        }
    val demoGallerySeeder =
        remember {
            EntryPointAccessors
                .fromApplication(
                    context.applicationContext,
                    DataSourceResolverEntryPoint::class.java,
                ).demoGallerySeeder()
        }
    var testOnlyMode by remember { mutableStateOf(dataSourceResolver.isTestOnlyMode) }
    val demoUnlocked = DemoFeatures.isUnlocked(testOnlyMode)

    fun openMockInvitation(token: String) {
        if (!BuildConfig.DEBUG || dataSourceResolver.effectiveMode() != DataSourceMode.MOCK) return
        // La carga vive en el LaunchedEffect de la ruta para evitar doble fetch.
        navController.navigate(Routes.vai27InvitationRoute(token)) {
            launchSingleTop = true
        }
    }

    fun openInvitation(token: String) {
        if (dataSourceResolver.effectiveMode() != DataSourceMode.REMOTE) return
        navController.navigate(Routes.vai27InvitationRoute(token)) {
            launchSingleTop = true
        }
    }

    LaunchedEffect(pendingInvitationToken, dataSourceResolver.effectiveMode()) {
        val token = pendingInvitationToken?.trim().orEmpty()
        if (token.isEmpty()) return@LaunchedEffect
        if (dataSourceResolver.effectiveMode() == DataSourceMode.REMOTE) {
            openInvitation(token)
        }
        onInvitationConsumed()
    }

    LaunchedEffect(pendingMockInvitationToken, dataSourceResolver.effectiveMode()) {
        val token = pendingMockInvitationToken?.trim().orEmpty()
        if (token.isEmpty()) return@LaunchedEffect
        if (BuildConfig.DEBUG && dataSourceResolver.effectiveMode() == DataSourceMode.MOCK) {
            openMockInvitation(token)
        }
        onMockInvitationConsumed()
    }

    fun navigateAuthorizedMode(modeRole: OperationalRole) {
        when (modeRole) {
            OperationalRole.CASHIER -> navController.navigate(Routes.CASHIER) { launchSingleTop = true }
            OperationalRole.KITCHEN -> navController.navigate(Routes.KITCHEN) { launchSingleTop = true }
            OperationalRole.WAITER -> navController.navigate(Routes.WAITER) { launchSingleTop = true }
            OperationalRole.CLIENT -> navController.navigateStudent(Routes.CATALOG)
        }
    }

    fun returnToClientFromAuthorizedMode() {
        authorizedAccessViewModel.returnToClient {
            operationalViewModel.setRole(OperationalRole.CLIENT)
            navController.navigateStudent(Routes.CATALOG)
        }
    }

    LaunchedEffect(
        authorizedAccessState.activeContext,
        authorizedAccessState.modes,
        authorizedAccessState.loading,
    ) {
        val active = authorizedAccessState.activeContext ?: return@LaunchedEffect
        if (authorizedAccessState.loading) return@LaunchedEffect
        val stillAuthorized =
            authorizedAccessState.modes.any {
                it.role == active.role &&
                    it.establishmentId == active.establishmentId &&
                    it.membershipId == active.membershipId
            }
        if (!stillAuthorized) {
            returnToClientFromAuthorizedMode()
        }
    }

    fun navigateDemo(route: String) {
        if (!demoUnlocked) return
        navController.navigateStudent(route)
    }

    fun navigateGalleryItem(itemId: String) {
        if (!demoUnlocked) return
        operationalViewModel.setRole(OperationalRole.CLIENT)
        orderFlowViewModel.clearCreatedOrder()
        when (itemId) {
            "splash" -> navController.navigate(Routes.SPLASH) { launchSingleTop = true }
            "01" -> navController.navigate(Routes.ROLE_SELECTOR) { launchSingleTop = true }
            "vai27-invitation" -> {
                authorizedAccessViewModel.resetFixtures()
                authorizedAccessViewModel.prepareMockGallerySession()
                openMockInvitation("vai27-valid-cashier")
            }
            "vai27-modes" -> {
                authorizedAccessViewModel.resetFixtures()
                authorizedAccessViewModel.prepareMockGallerySession()
                authorizedAccessViewModel.prepareMockGalleryModes()
                navController.navigate(Routes.VAI27_MODES) { launchSingleTop = true }
            }
            "vai27-external-revoke" -> {
                authorizedAccessViewModel.resetFixtures()
                authorizedAccessViewModel.prepareMockGallerySession()
                authorizedAccessViewModel.prepareMockExternalRevocationScenario {
                    operationalViewModel.setRole(OperationalRole.CLIENT)
                    navController.navigateStudent(Routes.CATALOG)
                }
            }
            "02" -> {
                demoGallerySeeder.seedCatalogCleared(orderFlowViewModel)
                operationalViewModel.applyGalleryClientOrders(emptyList(), selectedOrderId = null)
                navController.navigate(Routes.CATALOG) { launchSingleTop = true }
            }
            "05" -> {
                demoGallerySeeder.seedCatalogActiveOrder(orderFlowViewModel, operationalViewModel)
                navController.navigate(Routes.CATALOG) { launchSingleTop = true }
            }
            "06" -> {
                demoGallerySeeder.seedCatalogEmptySearch(orderFlowViewModel)
                navController.navigate(Routes.CATALOG) { launchSingleTop = true }
            }
            "07" -> {
                demoGallerySeeder.seedCatalogProductSheet(orderFlowViewModel)
                navController.navigate(Routes.CATALOG) { launchSingleTop = true }
            }
            "09" -> navController.navigate(Routes.ASSISTANT_HUB) { launchSingleTop = true }
            "57" -> navController.navigate(Routes.ASSISTANT_CHAT) { launchSingleTop = true }
            "12" -> {
                demoGallerySeeder.seedCartEmpty(orderFlowViewModel)
                navController.navigate(Routes.CART) { launchSingleTop = true }
            }
            "13" -> {
                demoGallerySeeder.seedCheckout(
                    orderFlowViewModel,
                    OrderDestination.TAKE_AWAY,
                    PaymentMethod.CASH,
                )
                navController.navigate(Routes.CART) { launchSingleTop = true }
            }
            "14" -> {
                demoGallerySeeder.seedCheckout(
                    orderFlowViewModel,
                    OrderDestination.IN_SPACE,
                    PaymentMethod.BALANCE,
                    DemoCheckoutFixtures.DEFAULT_SPACE.id,
                )
                navController.navigate(Routes.CART) { launchSingleTop = true }
            }
            "15" -> {
                demoGallerySeeder.seedCheckout(
                    orderFlowViewModel,
                    OrderDestination.TAKE_AWAY,
                    PaymentMethod.CARD,
                )
                navController.navigate(Routes.CART) { launchSingleTop = true }
            }
            "16" -> demoGallerySeeder.seedConfirmation(orderFlowViewModel, PaymentMethod.CASH)
            "17" -> demoGallerySeeder.seedConfirmation(orderFlowViewModel, PaymentMethod.BALANCE)
            "18" -> demoGallerySeeder.seedConfirmation(orderFlowViewModel, PaymentMethod.CARD)
            "19" -> {
                demoGallerySeeder.seedTrackingEmpty(operationalViewModel)
                navController.navigate(Routes.STUDENT_TRACKING) { launchSingleTop = true }
            }
            "20" -> {
                demoGallerySeeder.seedTrackingOrder(
                    operationalViewModel,
                    OrderState.PENDING_PAYMENT,
                    PaymentMethod.CASH,
                )
                navController.navigate(Routes.STUDENT_TRACKING) { launchSingleTop = true }
            }
            "21" -> {
                demoGallerySeeder.seedTrackingOrder(
                    operationalViewModel,
                    OrderState.PAID,
                    PaymentMethod.BALANCE,
                )
                navController.navigate(Routes.STUDENT_TRACKING) { launchSingleTop = true }
            }
            "22" -> {
                demoGallerySeeder.seedTrackingOrder(
                    operationalViewModel,
                    OrderState.PREPARING,
                    PaymentMethod.CASH,
                )
                navController.navigate(Routes.STUDENT_TRACKING) { launchSingleTop = true }
            }
            "23" -> {
                demoGallerySeeder.seedTrackingOrder(
                    operationalViewModel,
                    OrderState.READY,
                    PaymentMethod.CASH,
                )
                navController.navigate(Routes.STUDENT_TRACKING) { launchSingleTop = true }
            }
            "24" -> {
                demoGallerySeeder.seedTrackingOrder(
                    operationalViewModel,
                    OrderState.DELIVERED,
                    PaymentMethod.CASH,
                )
                navController.navigate(Routes.STUDENT_TRACKING) { launchSingleTop = true }
            }
            "25" -> navController.navigate(Routes.WALLET) { launchSingleTop = true }
            "26" -> navController.navigate("wallet/add-money?method=card") { launchSingleTop = true }
            "27" -> navController.navigate("wallet/add-money?method=spei") { launchSingleTop = true }
            "28" -> navController.navigate(Routes.WALLET_METHODS) { launchSingleTop = true }
            "29" -> navController.navigate(Routes.WALLET_ADD_CARD) { launchSingleTop = true }
            "30" -> navController.navigate(Routes.WALLET_ACCOUNT) { launchSingleTop = true }
            "51", "52", "53", "54", "55", "56" -> {
                val styleIndex = itemId.toInt() - 51
                operationalViewModel.applyGalleryClientOrders(emptyList(), selectedOrderId = null)
                navController.navigate(Routes.receiptStickerRoute(styleIndex)) { launchSingleTop = true }
            }
            "caja" -> {
                operationalViewModel.setRole(OperationalRole.CASHIER)
                navController.navigate(Routes.CASHIER) { launchSingleTop = true }
            }
            "cocina" -> {
                operationalViewModel.setRole(OperationalRole.KITCHEN)
                navController.navigate(Routes.KITCHEN) { launchSingleTop = true }
            }
            "mesero" -> {
                operationalViewModel.setRole(OperationalRole.WAITER)
                navController.navigate(Routes.WAITER) { launchSingleTop = true }
            }
        }
    }

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

    val activeOrder =
        orderState.createdOrder
            ?: operationalState.selectedOrder
            ?: operationalState.orders.firstOrNull()

    fun finishStudentAuth(returnRoute: String) {
        orderFlowViewModel.restoreGuestSessionAfterAuth()
        studentAuthViewModel.refreshGuestVenue()
        authorizedAccessViewModel.refreshCurrentSession()
        if (!navController.popBackStack(returnRoute, inclusive = false)) {
            navController.navigate(returnRoute) { launchSingleTop = true }
        }
    }

    fun navigateStudentAuth(returnRoute: String = Routes.CART) {
        orderFlowViewModel.prepareForGuestAuth()
        studentAuthViewModel.refreshGuestVenue()
        navController.navigate(Routes.authLandingRoute(returnRoute)) {
            launchSingleTop = true
        }
    }

    val authReturnArg =
        navArgument("returnRoute") {
            type = NavType.StringType
            defaultValue = Routes.CART
        }

    StudentShellHost(
        navController = navController,
        cartCount = orderState.cartItemCount,
        onNavigateStudent = { route -> navController.navigateStudent(route) },
        onNavigateDemo = { route -> navigateDemo(route) },
        catalogDetailOpen = orderState.selectedProductId != null,
    ) {
        NavHost(navController = navController, startDestination = Routes.SPLASH) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        navController.navigate(Routes.DISCOVERY) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.DISCOVERY) {
                DiscoveryScreen(
                    state = discoveryState,
                    onQueryChange = discoveryViewModel::updateQuery,
                    onSpaceTokenChange = discoveryViewModel::updateSpaceToken,
                    onSelectEstablishment = { establishment ->
                        discoveryViewModel.selectEstablishment(
                            establishment = establishment,
                            onEntered = ::enterVenueAndOpenCatalog,
                        )
                    },
                    onResolveSpace = {
                        discoveryViewModel.resolveSpaceToken(onEntered = ::enterVenueAndOpenCatalog)
                    },
                    onConfirmSwitch = {
                        discoveryViewModel.confirmPendingSwitch(::enterVenueAndOpenCatalog)
                    },
                    onDismissSwitch = discoveryViewModel::dismissPendingSwitch,
                    onContinueSelected = {
                        val selected = discoveryState.selected ?: return@DiscoveryScreen
                        enterVenueAndOpenCatalog(selected)
                    },
                    onOpenDemoRoles = {
                        if (DemoFeatures.toolsAvailable) {
                            testOnlyMode = true
                            orderFlowViewModel.clearGuestVenueForDemo()
                            navController.navigate(Routes.ROLE_SELECTOR) {
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }

            composable(Routes.ROLE_SELECTOR) {
                RoleSelectorScreen(
                    testOnlyMode = testOnlyMode,
                    onTestOnlyModeChange = { enabled -> testOnlyMode = enabled },
                    loadingRole = roleAuthState.authenticatingRole.takeIf { roleAuthState.loading },
                    errorMessage = roleAuthState.errorMessage,
                    onDismissError = roleAuthViewModel::clearError,
                    onOpenDemoGallery = {
                        if (DemoFeatures.toolsAvailable) {
                            testOnlyMode = true
                            navController.navigate(Routes.DEMO_GALLERY) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onRoleSelected = { role ->
                        val staffRole = role != OperationalRole.CLIENT
                        if (!staffRole || demoUnlocked) {
                            roleAuthViewModel.authenticate(role) {
                                operationalViewModel.setRole(role)
                                when (role) {
                                    OperationalRole.CLIENT -> {
                                        orderFlowViewModel.clearGuestVenueForDemo()
                                        orderFlowViewModel.refresh()
                                        navController.navigate(Routes.CATALOG) {
                                            launchSingleTop = true
                                        }
                                    }
                                    OperationalRole.CASHIER ->
                                        navController.navigate(Routes.CASHIER) {
                                            launchSingleTop = true
                                        }
                                    OperationalRole.KITCHEN ->
                                        navController.navigate(Routes.KITCHEN) {
                                            launchSingleTop = true
                                        }
                                    OperationalRole.WAITER ->
                                        navController.navigate(Routes.WAITER) {
                                            launchSingleTop = true
                                        }
                                }
                            }
                        }
                    },
                )
            }

            composable(
                route = Routes.VAI27_INVITATION,
                arguments = listOf(navArgument("token") { type = NavType.StringType }),
            ) { entry ->
                val token = entry.arguments?.getString("token").orEmpty()
                LaunchedEffect(token, studentAuthState.session?.uid) {
                    if (token.isNotBlank()) {
                        authorizedAccessViewModel.openInvitation(token)
                        authorizedAccessViewModel.refreshCurrentSession()
                    }
                }
                InvitationAcceptanceScreen(
                    state = authorizedAccessState,
                    onBack = { navController.popBackStack() },
                    onAccept = {
                        authorizedAccessViewModel.acceptInvitation {
                            navController.navigate(Routes.VAI27_MODES) {
                                popUpTo(Routes.VAI27_INVITATION) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onAuthenticate = {
                        navigateStudentAuth(Routes.vai27InvitationRoute(token))
                    },
                )
            }

            composable(Routes.VAI27_MODES) {
                LaunchedEffect(authorizedAccessState.session?.uid) {
                    authorizedAccessViewModel.refreshModes(force = true)
                }
                AuthorizedModeScreen(
                    state = authorizedAccessState,
                    onBack = { navController.popBackStack() },
                    onSelectMode = { mode ->
                        authorizedAccessViewModel.activateMode(mode) {
                            operationalViewModel.setRole(mode.role)
                            navigateAuthorizedMode(mode.role)
                        }
                    },
                    onReturnToClient = ::returnToClientFromAuthorizedMode,
                )
            }

            composable(Routes.DEMO_GALLERY) {
                LaunchedEffect(Unit) {
                    if (!DemoFeatures.toolsAvailable) {
                        navController.popBackStack()
                        return@LaunchedEffect
                    }
                    testOnlyMode = true
                    orderFlowViewModel.refresh()
                }
                DemoGalleryScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onItemSelected = { itemId -> navigateGalleryItem(itemId) },
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
                    onOpenAssistant = { navigateDemo(Routes.ASSISTANT) },
                    onOpenWallet = { navigateDemo(Routes.WALLET) },
                    onChangeVenue = {
                        navController.navigate(Routes.DISCOVERY) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.ASSISTANT) {
                LaunchedEffect(demoUnlocked) {
                    if (!demoUnlocked) navController.popBackStack()
                }
                AssistantChatScreen(
                    state = orderState,
                    embeddedInBottomNav = true,
                    onSendMessage = orderFlowViewModel::sendAssistantMessage,
                    onClearChat = orderFlowViewModel::clearAssistantChat,
                    onClose = { navController.navigateStudent(Routes.CATALOG) },
                    onMenu = { navController.navigateStudent(Routes.CATALOG) },
                    onOrders = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                    onWallet = { navigateDemo(Routes.WALLET) },
                    onCart = { navController.navigateStudent(Routes.CART) },
                )
            }

            composable(Routes.ASSISTANT_HUB) {
                LaunchedEffect(demoUnlocked) {
                    if (!demoUnlocked) navController.popBackStack()
                }
                AssistantScreen(
                    state = orderState,
                    onOpenChat = { navController.navigate(Routes.ASSISTANT) { launchSingleTop = true } },
                    onOpenProduct = { productId ->
                        orderFlowViewModel.openProduct(productId)
                        navController.navigateStudent(Routes.CATALOG)
                    },
                    onMenu = { navController.navigateStudent(Routes.CATALOG) },
                    onOrders = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                    onWallet = { navigateDemo(Routes.WALLET) },
                    onCart = { navController.navigateStudent(Routes.CART) },
                )
            }

            composable(Routes.ASSISTANT_CHAT) {
                LaunchedEffect(demoUnlocked) {
                    if (!demoUnlocked) navController.popBackStack()
                }
                AssistantChatScreen(
                    state = orderState,
                    embeddedInBottomNav = false,
                    onSendMessage = orderFlowViewModel::sendAssistantMessage,
                    onClearChat = orderFlowViewModel::clearAssistantChat,
                    onClose = { navController.popBackStack() },
                    onMenu = { navController.navigateStudent(Routes.CATALOG) },
                    onOrders = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                    onWallet = { navigateDemo(Routes.WALLET) },
                    onCart = { navController.navigateStudent(Routes.CART) },
                )
            }

            composable(Routes.WALLET) {
                LaunchedEffect(demoUnlocked) {
                    if (!demoUnlocked) navController.popBackStack()
                }
                WalletScreen(
                    state = orderState,
                    balance = walletState.balance,
                    onAddMoney = { navController.navigate("wallet/add-money?method=card") },
                    onPaymentMethods = { navController.navigate(Routes.WALLET_METHODS) },
                    onAccount = { navController.navigate(Routes.WALLET_ACCOUNT) },
                    onMenu = { navController.navigateStudent(Routes.CATALOG) },
                    onAssistant = { navigateDemo(Routes.ASSISTANT) },
                    onOrders = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                    onCart = { navController.navigateStudent(Routes.CART) },
                )
            }

            composable(
                route = Routes.WALLET_ADD_MONEY,
                arguments =
                    listOf(
                        navArgument("method") {
                            type = NavType.StringType
                            defaultValue = "card"
                        },
                    ),
            ) { entry ->
                LaunchedEffect(demoUnlocked) {
                    if (!demoUnlocked) navController.popBackStack()
                }
                WalletAddMoneyScreen(
                    walletState = walletState,
                    initialMethod = entry.arguments?.getString("method") ?: "card",
                    onBack = { navController.popBackStack() },
                    onCreditBalance = { amount -> walletState.balance += amount },
                )
            }

            composable(Routes.WALLET_METHODS) {
                LaunchedEffect(demoUnlocked) {
                    if (!demoUnlocked) navController.popBackStack()
                }
                WalletPaymentMethodsScreen(
                    walletState = walletState,
                    onBack = { navController.popBackStack() },
                    onAddCard = { navController.navigate(Routes.WALLET_ADD_CARD) },
                )
            }

            composable(Routes.WALLET_ADD_CARD) {
                LaunchedEffect(demoUnlocked) {
                    if (!demoUnlocked) navController.popBackStack()
                }
                WalletAddCardScreen(
                    walletState = walletState,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(Routes.WALLET_ACCOUNT) {
                LaunchedEffect(demoUnlocked) {
                    if (!demoUnlocked) navController.popBackStack()
                }
                WalletAccountScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.CART) {
                LaunchedEffect(Unit) {
                    orderFlowViewModel.refresh()
                }
                val guestAuthRequired = orderFlowViewModel.requiresStudentAuth()
                CartScreen(
                    state = orderState,
                    walletBalance = walletState.balance,
                    onMenu = { navController.navigateStudent(Routes.CATALOG) },
                    onQuantityChange = orderFlowViewModel::changeCartLineQuantity,
                    onNotesChange = orderFlowViewModel::updateKitchenNotes,
                    onDestinationChange = orderFlowViewModel::updateCheckoutDestination,
                    onSpaceChange = orderFlowViewModel::updateCheckoutSpace,
                    onPaymentChange = orderFlowViewModel::updateCheckoutPayment,
                    onConfirm = {
                        if (guestAuthRequired) {
                            navigateStudentAuth(Routes.CART)
                        } else {
                            orderFlowViewModel.submitOrder(
                                walletBalance = walletState.balance,
                                onWalletDebit = { amount -> walletState.balance -= amount },
                            )
                        }
                    },
                    onOpenTracking = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                    onOpenAssistant = { navigateDemo(Routes.ASSISTANT) },
                    onOpenWallet = { navigateDemo(Routes.WALLET) },
                    showDemoTabs = demoUnlocked,
                    guestAuthRequired = guestAuthRequired,
                )
            }

            composable(
                route = Routes.AUTH_LANDING,
                arguments = listOf(authReturnArg),
            ) { entry ->
                val returnRoute = entry.arguments?.getString("returnRoute") ?: Routes.CART
                StudentAuthLandingScreen(
                    state = studentAuthState,
                    onBack = { navController.popBackStack() },
                    onRegister = {
                        navController.navigate(Routes.authRegisterRoute(returnRoute)) {
                            launchSingleTop = true
                        }
                    },
                    onLogin = {
                        navController.navigate(Routes.authLoginRoute(returnRoute)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(
                route = Routes.AUTH_REGISTER,
                arguments = listOf(authReturnArg),
            ) { entry ->
                val returnRoute = entry.arguments?.getString("returnRoute") ?: Routes.CART
                StudentRegisterScreen(
                    state = studentAuthState,
                    onBack = { navController.popBackStack() },
                    onNameChange = studentAuthViewModel::updateName,
                    onEmailChange = studentAuthViewModel::updateEmail,
                    onPasswordChange = studentAuthViewModel::updatePassword,
                    onContextualIdChange = studentAuthViewModel::updateContextualId,
                    onTermsChange = studentAuthViewModel::updateTermsAccepted,
                    onRegister = {
                        studentAuthViewModel.register {
                            navController.navigate(Routes.authVerifyRoute(returnRoute)) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onLogin = {
                        navController.navigate(Routes.authLoginRoute(returnRoute)) {
                            launchSingleTop = true
                        }
                    },
                    onForgotPassword = {
                        navController.navigate(Routes.authForgotRoute(returnRoute)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(
                route = Routes.AUTH_LOGIN,
                arguments = listOf(authReturnArg),
            ) { entry ->
                val returnRoute = entry.arguments?.getString("returnRoute") ?: Routes.CART
                StudentLoginScreen(
                    state = studentAuthState,
                    onBack = { navController.popBackStack() },
                    onEmailChange = studentAuthViewModel::updateEmail,
                    onPasswordChange = studentAuthViewModel::updatePassword,
                    onContextualIdChange = studentAuthViewModel::updateContextualId,
                    onLogin = {
                        studentAuthViewModel.login { enrolled ->
                            if (enrolled) {
                                finishStudentAuth(returnRoute)
                            } else {
                                navController.navigate(Routes.authVerifyRoute(returnRoute)) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    },
                    onForgotPassword = {
                        navController.navigate(Routes.authForgotRoute(returnRoute)) {
                            launchSingleTop = true
                        }
                    },
                    onRegister = {
                        navController.navigate(Routes.authRegisterRoute(returnRoute)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(
                route = Routes.AUTH_VERIFY,
                arguments = listOf(authReturnArg),
            ) { entry ->
                val returnRoute = entry.arguments?.getString("returnRoute") ?: Routes.CART
                StudentVerifyEmailScreen(
                    state = studentAuthState,
                    onBack = { navController.popBackStack() },
                    onResend = studentAuthViewModel::resendVerification,
                    onCheckVerified = {
                        studentAuthViewModel.checkVerification {
                            finishStudentAuth(returnRoute)
                        }
                    },
                )
            }

            composable(
                route = Routes.AUTH_FORGOT,
                arguments = listOf(authReturnArg),
            ) {
                StudentForgotPasswordScreen(
                    state = studentAuthState,
                    onBack = { navController.popBackStack() },
                    onEmailChange = studentAuthViewModel::updateEmail,
                    onSendReset = studentAuthViewModel::sendPasswordReset,
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
                        orderState.createdOrder
                            ?.summary
                            ?.id
                            ?.let(operationalViewModel::selectOrder)
                        navController.navigateStudent(Routes.STUDENT_TRACKING)
                    },
                    onViewSticker = { navController.navigate(Routes.receiptStickerRoute()) },
                )
            }

            composable(
                route = Routes.RECEIPT_STICKER,
                arguments =
                    listOf(
                        navArgument("style") {
                            type = NavType.IntType
                            defaultValue = 0
                        },
                    ),
            ) { entry ->
                val styleIndex = entry.arguments?.getInt("style") ?: 0
                ReceiptStickerScreen(
                    order = orderState.createdOrder ?: operationalState.selectedOrder,
                    onBack = { navController.popBackStack() },
                    initialStyleIndex = styleIndex,
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
                    onAssistant = { navigateDemo(Routes.ASSISTANT) },
                    onWallet = { navigateDemo(Routes.WALLET) },
                    onCart = { navController.navigateStudent(Routes.CART) },
                    onOpenCatalog = { navController.navigateStudent(Routes.CATALOG) },
                    onSelectOrder = operationalViewModel::selectOrder,
                    onViewSticker = { navController.navigate(Routes.receiptStickerRoute()) },
                )
            }

            composable(Routes.CASHIER) {
                val authorizedCashier = authorizedAccessState.activeContext?.role == OperationalRole.CASHIER
                LaunchedEffect(demoUnlocked, authorizedCashier) {
                    if (!demoUnlocked && !authorizedCashier) {
                        navController.navigate(Routes.ROLE_SELECTOR) {
                            launchSingleTop = true
                        }
                    }
                }
                CashierOperationalScreen(
                    state = operationalState,
                    onBack =
                        if (authorizedCashier) {
                            {
                                operationalViewModel.clearRole()
                                navController.navigate(Routes.VAI27_MODES) { launchSingleTop = true }
                            }
                        } else {
                            returnToRoles(navController, operationalViewModel)
                        },
                    onOpenCashSession = operationalViewModel::openCashRegister,
                    onCollect = operationalViewModel::collectCash,
                    onDeliver = operationalViewModel::deliver,
                    onChangeMode =
                        if (authorizedCashier && authorizedAccessState.hasMultipleModes) {
                            {
                                operationalViewModel.clearRole()
                                navController.navigate(Routes.VAI27_MODES) { launchSingleTop = true }
                            }
                        } else {
                            null
                        },
                )
            }

            composable(Routes.KITCHEN) {
                val authorizedKitchen = authorizedAccessState.activeContext?.role == OperationalRole.KITCHEN
                LaunchedEffect(demoUnlocked, authorizedKitchen) {
                    if (!demoUnlocked && !authorizedKitchen) {
                        navController.navigate(Routes.ROLE_SELECTOR) {
                            launchSingleTop = true
                        }
                    }
                }
                KitchenOperationalScreen(
                    state = operationalState,
                    onBack =
                        if (authorizedKitchen) {
                            {
                                operationalViewModel.clearRole()
                                navController.navigate(Routes.VAI27_MODES) { launchSingleTop = true }
                            }
                        } else {
                            returnToRoles(navController, operationalViewModel)
                        },
                    onStart = operationalViewModel::startKitchen,
                    onReady = operationalViewModel::markReady,
                    onChangeMode =
                        if (authorizedKitchen && authorizedAccessState.hasMultipleModes) {
                            {
                                operationalViewModel.clearRole()
                                navController.navigate(Routes.VAI27_MODES) { launchSingleTop = true }
                            }
                        } else {
                            null
                        },
                )
            }

            composable(Routes.WAITER) {
                val authorizedWaiter = authorizedAccessState.activeContext?.role == OperationalRole.WAITER
                LaunchedEffect(demoUnlocked, authorizedWaiter) {
                    if (!demoUnlocked && !authorizedWaiter) {
                        navController.navigate(Routes.ROLE_SELECTOR) {
                            launchSingleTop = true
                        }
                    }
                }
                WaiterOperationalScreen(
                    state = operationalState,
                    onBack =
                        if (authorizedWaiter) {
                            {
                                operationalViewModel.clearRole()
                                navController.navigate(Routes.VAI27_MODES) { launchSingleTop = true }
                            }
                        } else {
                            returnToRoles(navController, operationalViewModel)
                        },
                    onDeliver = operationalViewModel::deliver,
                    onChangeMode =
                        if (authorizedWaiter && authorizedAccessState.hasMultipleModes) {
                            {
                                operationalViewModel.clearRole()
                                navController.navigate(Routes.VAI27_MODES) { launchSingleTop = true }
                            }
                        } else {
                            null
                        },
                )
            }
        }
    }
}

private fun NavHostController.navigateStudent(route: String) {
    navigate(route) {
        popUpTo(Routes.CATALOG) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun returnToRoles(
    navController: NavHostController,
    operationalViewModel: OperationalViewModel,
): () -> Unit =
    {
        operationalViewModel.clearRole()
        navController.navigate(Routes.ROLE_SELECTOR) {
            popUpTo(Routes.ROLE_SELECTOR) { inclusive = true }
        }
    }
