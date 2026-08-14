package com.vaiinilla.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.auth.student.StudentAuthViewModel
import com.vaiinilla.app.ui.discovery.GuestDiscoveryViewModel
import com.vaiinilla.app.ui.discovery.QrScannerDialog
import com.vaiinilla.app.ui.mode.AuthorizedAccessViewModel
import com.vaiinilla.app.ui.operational.OperationalViewModel
import com.vaiinilla.app.ui.order.OrderFlowViewModel
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.profile.displayInitials
import com.vaiinilla.app.ui.screens.AssistantChatScreen
import com.vaiinilla.app.ui.screens.AuthorizedModeScreen
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CashierOperationalScreen
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.DiscoveryScreen
import com.vaiinilla.app.ui.screens.InvitationAcceptanceScreen
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import com.vaiinilla.app.ui.screens.ReceiptStickerScreen
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
import com.vaiinilla.app.ui.wallet.WalletUiState
import com.vaiinilla.app.ui.wallet.WalletViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private data class PendingPickupDelivery(
    val orderId: String,
    val expectedVersion: Int,
)

@Composable
fun AppNavHost(
    navController: NavHostController,
    pendingEstablishmentSlug: String? = null,
    pendingInvitationToken: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    onInvitationConsumed: () -> Unit = {},
) {
    val orderFlowViewModel: OrderFlowViewModel = viewModel()
    val operationalViewModel: OperationalViewModel = viewModel()
    val studentAuthViewModel: StudentAuthViewModel = viewModel()
    val authorizedAccessViewModel: AuthorizedAccessViewModel = viewModel()
    val discoveryViewModel: GuestDiscoveryViewModel = viewModel()
    val walletViewModel: WalletViewModel = viewModel()
    val orderState by orderFlowViewModel.uiState
    val operationalState by operationalViewModel.uiState
    val studentAuthState by studentAuthViewModel.state
    val authorizedAccessState by authorizedAccessViewModel.state
    val discoveryState by discoveryViewModel.state
    val walletRemoteState by walletViewModel.state.collectAsStateWithLifecycle()
    val walletState = WalletUiState()

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
    var qrScannerOpen by remember { mutableStateOf(false) }
    var walletUserQrOpen by remember { mutableStateOf(false) }
    var pendingPickupDelivery by remember { mutableStateOf<PendingPickupDelivery?>(null) }

    fun openInvitation(token: String) {
        navController.navigate(Routes.vai27InvitationRoute(token)) {
            launchSingleTop = true
        }
    }

    LaunchedEffect(pendingInvitationToken) {
        val token = pendingInvitationToken?.trim().orEmpty()
        if (token.isEmpty()) return@LaunchedEffect
        openInvitation(token)
        onInvitationConsumed()
    }

    fun navigateAuthorizedMode(modeRole: OperationalRole) {
        // Keep a guest cart in its tenant-scoped snapshot, but do not carry the
        // guest venue/catalog state into an authenticated operational context.
        orderFlowViewModel.clearGuestVenue()
        when (modeRole) {
            OperationalRole.CASHIER -> navController.navigate(Routes.CASHIER) { launchSingleTop = true }
            OperationalRole.KITCHEN -> navController.navigate(Routes.KITCHEN) { launchSingleTop = true }
            OperationalRole.WAITER -> navController.navigate(Routes.WAITER) { launchSingleTop = true }
            OperationalRole.CLIENT -> {
                orderFlowViewModel.refresh()
                navController.navigateStudent(Routes.CATALOG)
            }
        }
    }

    fun returnToClientFromAuthorizedMode() {
        authorizedAccessViewModel.returnToClient {
            operationalViewModel.setRole(OperationalRole.CLIENT)
            orderFlowViewModel.clearGuestVenue()
            orderFlowViewModel.refresh()
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

    LaunchedEffect(
        authorizedAccessState.activeContext?.membershipId,
    ) {
        if (authorizedAccessState.activeContext == null) return@LaunchedEffect
        while (isActive) {
            delay(AUTHORIZED_ACCESS_SYNC_INTERVAL_MS)
            if (!isActive) break
            authorizedAccessViewModel.syncAuthorizedAccessOrReturnToClient(
                onForcedToClient = ::returnToClientFromAuthorizedMode,
            )
        }
    }

    LaunchedEffect(
        studentAuthState.session?.uid,
        studentAuthState.session?.emailVerified,
    ) {
        if (studentAuthState.session?.emailVerified == true) {
            authorizedAccessViewModel.refreshModes(force = true)
        }
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

    fun navigateLaunchDestination(destination: LaunchDestination) {
        val currentRoute = navController.currentDestination?.route
        if (currentRoute == Routes.CATALOG) {
            navController.popBackStack(Routes.SPLASH, inclusive = true)
            return
        }
        navController.navigate(destination.toRoute()) {
            popUpTo(Routes.SPLASH) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun finishLaunchAuth() {
        orderFlowViewModel.restoreGuestSessionAfterAuth()
        studentAuthViewModel.refreshGuestVenue()
        authorizedAccessViewModel.refreshCurrentSession()
        authorizedAccessViewModel.refreshModes(force = true) {
            val destination =
                resolveLaunchDestination(
                    pendingEstablishmentSlug = pendingEstablishmentSlug,
                    session = authorizedAccessViewModel.state.value.session,
                    hasStaffModes =
                        hasStaffLaunchModes(authorizedAccessViewModel.state.value.modes.map { it.role }),
                )
            val route =
                when (destination) {
                    LaunchDestination.Login,
                    LaunchDestination.Discovery,
                    -> Routes.DISCOVERY
                    LaunchDestination.StaffModes -> Routes.VAI27_MODES
                }
            navController.navigate(route) {
                popUpTo(Routes.AUTH_LOGIN) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun navigateStudentAuth(returnRoute: String = Routes.CART) {
        orderFlowViewModel.prepareForGuestAuth()
        studentAuthViewModel.refreshGuestVenue()
        val session = studentAuthViewModel.state.value.session
        val targetRoute =
            if (session != null && !session.emailVerified) {
                Routes.authVerifyRoute(returnRoute)
            } else {
                Routes.authLandingRoute(returnRoute)
            }
        navController.navigate(targetRoute) {
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
        catalogDetailOpen = orderState.selectedProductId != null,
    ) {
        NavHost(navController = navController, startDestination = Routes.SPLASH) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        authorizedAccessViewModel.refreshModes(force = true) {
                            navigateLaunchDestination(
                                resolveLaunchDestination(
                                    pendingEstablishmentSlug = pendingEstablishmentSlug,
                                    session = authorizedAccessViewModel.state.value.session,
                                    hasStaffModes =
                                        hasStaffLaunchModes(
                                            authorizedAccessViewModel.state.value.modes.map { it.role },
                                        ),
                                ),
                            )
                        }
                    },
                )
            }

            composable(Routes.DISCOVERY) {
                DiscoveryScreen(
                    state = discoveryState,
                    onQueryChange = discoveryViewModel::updateQuery,
                    onSpaceTokenChange = discoveryViewModel::updateSpaceToken,
                    onOpenQrScanner = { qrScannerOpen = true },
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
                    profileInitials = displayInitials(studentAuthState.session?.displayName.orEmpty()),
                    onOpenAccount = { navController.navigate(Routes.WALLET_ACCOUNT) },
                )

                if (qrScannerOpen) {
                    QrScannerDialog(
                        onClose = { qrScannerOpen = false },
                        onPayload = { rawValue ->
                            qrScannerOpen = false
                            discoveryViewModel.resolveQrPayload(
                                rawValue = rawValue,
                                onEntered = ::enterVenueAndOpenCatalog,
                            )
                        },
                    )
                }
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
                    onSignOut = {
                        studentAuthViewModel.signOut {
                            authorizedAccessViewModel.resetAfterSignOut()
                            navController.navigate(Routes.authLoginRoute(Routes.DISCOVERY)) {
                                popUpTo(Routes.SPLASH) { inclusive = false }
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
                    onOpenWallet = { navController.navigateStudent(Routes.WALLET) },
                    onChangeVenue = {
                        navController.navigate(Routes.DISCOVERY) {
                            launchSingleTop = true
                        }
                    },
                    onOpenModes =
                        if (
                            authorizedAccessState.activeContext == null &&
                            authorizedAccessState.hasMultipleModes
                        ) {
                            { navController.navigate(Routes.VAI27_MODES) { launchSingleTop = true } }
                        } else {
                            null
                        },
                    profileInitials = displayInitials(studentAuthState.session?.displayName.orEmpty()),
                    onOpenAccount = { navController.navigateStudent(Routes.WALLET_ACCOUNT) },
                )
            }

            composable(Routes.ASSISTANT) {
                AssistantChatScreen(
                    state = orderState,
                    embeddedInBottomNav = false,
                    onSendMessage = orderFlowViewModel::sendAssistantMessage,
                    onClearChat = orderFlowViewModel::clearAssistantChat,
                    onClose = { navController.navigateStudent(Routes.CATALOG) },
                    onOrders = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                    onWallet = { navController.navigateStudent(Routes.WALLET) },
                    onCart = { navController.navigateStudent(Routes.CART) },
                )
            }

            composable(Routes.ASSISTANT_CHAT) {
                AssistantChatScreen(
                    state = orderState,
                    embeddedInBottomNav = false,
                    onSendMessage = orderFlowViewModel::sendAssistantMessage,
                    onClearChat = orderFlowViewModel::clearAssistantChat,
                    onClose = { navController.popBackStack() },
                    onOrders = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                    onWallet = { navController.navigateStudent(Routes.WALLET) },
                    onCart = { navController.navigateStudent(Routes.CART) },
                )
            }

            composable(Routes.WALLET) {
                LaunchedEffect(Unit) { walletViewModel.refresh() }
                WalletScreen(
                    remoteState = walletRemoteState,
                    onRetry = walletViewModel::refresh,
                    onMenu = { navController.navigateStudent(Routes.CATALOG) },
                    onAssistant = {},
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
                WalletAddMoneyScreen(
                    walletState = walletState,
                    initialMethod = entry.arguments?.getString("method") ?: "card",
                    onBack = { navController.popBackStack() },
                    onCreditBalance = {},
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
                LaunchedEffect(Unit) { walletViewModel.refresh() }
                val hasStaffModes = authorizedAccessState.modes.any { it.role != OperationalRole.CLIENT }
                WalletAccountScreen(
                    onBack = { navController.popBackStack() },
                    displayName = studentAuthState.session?.displayName.orEmpty(),
                    email = studentAuthState.session?.email.orEmpty(),
                    // ponytail: QR uses Firebase uid until wallets/me returns usuario_id; recarga still needs Railway id.
                    userId =
                        walletRemoteState.data?.wallet?.userId
                            ?: studentAuthState.session?.uid,
                    signedIn = studentAuthState.session != null,
                    hasStaffModes = hasStaffModes,
                    onOpenStaffModes = {
                        navController.navigate(Routes.VAI27_MODES) {
                            launchSingleTop = true
                        }
                    },
                    onChangeVenue = {
                        navController.navigate(Routes.DISCOVERY) {
                            launchSingleTop = true
                            popUpTo(Routes.DISCOVERY) { inclusive = false }
                        }
                    },
                    onSignOut = {
                        orderFlowViewModel.clearGuestVenue()
                        studentAuthViewModel.signOut {
                            authorizedAccessViewModel.resetAfterSignOut()
                            navController.navigate(Routes.authLoginRoute(Routes.DISCOVERY)) {
                                popUpTo(Routes.SPLASH) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    },
                    onSignIn = {
                        navController.navigate(Routes.authLoginRoute(Routes.DISCOVERY)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.CART) {
                val guestAuthRequired = orderFlowViewModel.requiresStudentAuth()
                CartScreen(
                    state = orderState,
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
                            orderFlowViewModel.submitOrder()
                        }
                    },
                    onOpenTracking = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                    onOpenWallet = { navController.navigateStudent(Routes.WALLET) },
                    guestAuthRequired = guestAuthRequired,
                    profileInitials = displayInitials(studentAuthState.session?.displayName.orEmpty()),
                    onOpenAccount = { navController.navigateStudent(Routes.WALLET_ACCOUNT) },
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
                    onPasswordConfirmChange = studentAuthViewModel::updatePasswordConfirm,
                    onContextualIdChange = studentAuthViewModel::updateContextualId,
                    onTermsChange = studentAuthViewModel::updateTermsAccepted,
                    onPrivacyChange = studentAuthViewModel::updatePrivacyAccepted,
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
                val isLaunchLogin = returnRoute == Routes.DISCOVERY
                StudentLoginScreen(
                    state = studentAuthState,
                    onBack = { navController.popBackStack() },
                    showBack = !isLaunchLogin,
                    onEmailChange = studentAuthViewModel::updateEmail,
                    onPasswordChange = studentAuthViewModel::updatePassword,
                    onContextualIdChange = studentAuthViewModel::updateContextualId,
                    onLogin = {
                        studentAuthViewModel.login { enrolled ->
                            if (enrolled) {
                                if (isLaunchLogin) {
                                    finishLaunchAuth()
                                } else {
                                    finishStudentAuth(returnRoute)
                                }
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
                            if (returnRoute == Routes.DISCOVERY) {
                                finishLaunchAuth()
                            } else {
                                finishStudentAuth(returnRoute)
                            }
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
                    onAssistant = {},
                    onWallet = { navController.navigateStudent(Routes.WALLET) },
                    onCart = { navController.navigateStudent(Routes.CART) },
                    onOpenCatalog = { navController.navigateStudent(Routes.CATALOG) },
                    onSelectOrder = operationalViewModel::selectOrder,
                    onViewSticker = { navController.navigate(Routes.receiptStickerRoute()) },
                )
            }

            composable(Routes.CASHIER) {
                val authorizedCashier = authorizedAccessState.activeContext?.role == OperationalRole.CASHIER
                LaunchedEffect(authorizedCashier) {
                    if (!authorizedCashier) {
                        navController.navigate(Routes.VAI27_MODES) {
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
                            returnToModes(navController, operationalViewModel)
                        },
                    onOpenCashSession = operationalViewModel::openCashRegister,
                    onCollect = operationalViewModel::collectCash,
                    onSearchWalletClients = operationalViewModel::searchWalletClients,
                    onOpenWalletUserQr = { walletUserQrOpen = true },
                    onReloadWallet = operationalViewModel::reloadWallet,
                    onDeliver = { orderId, version -> operationalViewModel.deliver(orderId, version) },
                    onScanDeliver = { orderId, version ->
                        pendingPickupDelivery = PendingPickupDelivery(orderId, version)
                    },
                    onChangeMode =
                        if (authorizedCashier && authorizedAccessState.hasMultipleModes) {
                            {
                                operationalViewModel.clearRole()
                                navController.navigate(Routes.VAI27_MODES) { launchSingleTop = true }
                            }
                        } else {
                            null
                        },
                    restrictedMode =
                        authorizedAccessState.activeContext
                            ?.takeIf { it.role == OperationalRole.CASHIER }
                            ?.restrictedMode,
                    onToggleProductAvailable = operationalViewModel::setProductAvailable,
                    onCreateCashierProduct = operationalViewModel::createCashierProduct,
                    onUploadCashierProductImage = operationalViewModel::uploadCashierProductImage,
                )
            }

            composable(Routes.KITCHEN) {
                val authorizedKitchen = authorizedAccessState.activeContext?.role == OperationalRole.KITCHEN
                LaunchedEffect(authorizedKitchen) {
                    if (!authorizedKitchen) {
                        navController.navigate(Routes.VAI27_MODES) {
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
                            returnToModes(navController, operationalViewModel)
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
                    restrictedMode =
                        authorizedAccessState.activeContext
                            ?.takeIf { it.role == OperationalRole.KITCHEN }
                            ?.restrictedMode,
                )
            }

            composable(Routes.WAITER) {
                val authorizedWaiter = authorizedAccessState.activeContext?.role == OperationalRole.WAITER
                LaunchedEffect(authorizedWaiter) {
                    if (!authorizedWaiter) {
                        navController.navigate(Routes.VAI27_MODES) {
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
                            returnToModes(navController, operationalViewModel)
                        },
                    onDeliver = { orderId, version -> operationalViewModel.deliver(orderId, version) },
                    onScanDeliver = { orderId, version ->
                        pendingPickupDelivery = PendingPickupDelivery(orderId, version)
                    },
                    onChangeMode =
                        if (authorizedWaiter && authorizedAccessState.hasMultipleModes) {
                            {
                                operationalViewModel.clearRole()
                                navController.navigate(Routes.VAI27_MODES) { launchSingleTop = true }
                            }
                        } else {
                            null
                        },
                    restrictedMode =
                        authorizedAccessState.activeContext
                            ?.takeIf { it.role == OperationalRole.WAITER }
                            ?.restrictedMode,
                )
            }
        }
    }

    if (walletUserQrOpen) {
        QrScannerDialog(
            onClose = { walletUserQrOpen = false },
            helperText = "Apunta al QR de cuenta que muestra el alumno",
            onPayload = { rawValue ->
                walletUserQrOpen = false
                operationalViewModel.resolveWalletUserQr(rawValue)
            },
        )
    }

    if (pendingPickupDelivery != null) {
        QrScannerDialog(
            onClose = { pendingPickupDelivery = null },
            helperText = "Apunta al QR de recogida que muestra el alumno",
            onPayload = { rawValue ->
                val target = pendingPickupDelivery
                pendingPickupDelivery = null
                if (target != null) {
                    operationalViewModel.deliver(
                        orderId = target.orderId,
                        expectedVersion = target.expectedVersion,
                        scannedPickupToken = rawValue,
                    )
                }
            },
        )
    }
}

private const val AUTHORIZED_ACCESS_SYNC_INTERVAL_MS = 30_000L

private fun NavHostController.navigateStudent(route: String) {
    navigate(route) {
        popUpTo(Routes.CATALOG) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun returnToModes(
    navController: NavHostController,
    operationalViewModel: OperationalViewModel,
): () -> Unit =
    {
        operationalViewModel.clearRole()
        navController.navigate(Routes.VAI27_MODES) {
            launchSingleTop = true
        }
    }
