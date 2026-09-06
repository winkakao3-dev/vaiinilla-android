package com.vaiinilla.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.ui.account.AccountDeletionViewModel
import com.vaiinilla.app.ui.auth.student.StudentAuthViewModel
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.prefetchProductImages
import com.vaiinilla.app.ui.discovery.GuestDiscoveryViewModel
import com.vaiinilla.app.ui.discovery.QrScannerDialog
import com.vaiinilla.app.ui.mode.AuthorizedAccessViewModel
import com.vaiinilla.app.ui.mode.UnifiedTestModeManager
import com.vaiinilla.app.ui.operational.OperationalPresenceLifecycle
import com.vaiinilla.app.ui.operational.OperationalViewModel
import com.vaiinilla.app.ui.order.OrderFlowViewModel
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.order.isEstablishmentSwitch
import com.vaiinilla.app.ui.order.toRuntimeConfiguration
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
import com.vaiinilla.app.ui.wallet.WalletViewModel
import com.vaiinilla.app.ui.wallet.rememberWalletUiState
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
    val accountDeletionViewModel: AccountDeletionViewModel = viewModel()
    val orderState by orderFlowViewModel.uiState
    val operationalState by operationalViewModel.uiState
    val studentAuthState by studentAuthViewModel.state
    val authorizedAccessState by authorizedAccessViewModel.state
    val discoveryState by discoveryViewModel.state
    val walletRemoteState by walletViewModel.state.collectAsStateWithLifecycle()
    val accountDeletionState by accountDeletionViewModel.state
    val walletState = rememberWalletUiState()

    // Keep every in-memory copy of the selected venue aligned when persisted metadata
    // changes (for example, an establishment rename that also changes its slug).
    LaunchedEffect(orderState.guestVenue) {
        discoveryViewModel.refreshSelectedVenue()
        studentAuthViewModel.refreshGuestVenue()
    }

    // Warm product imagery as soon as catalog data exists, before cards enter composition.
    LaunchedEffect(orderState.catalog) {
        val urls =
            orderState.catalog
                ?.products
                ?.map { it.imageUrl }
                .orEmpty()
        if (urls.isNotEmpty()) prefetchProductImages(urls)
    }

    // Keep wallet data warm while the authenticated client shell is active so opening
    // Cartera does not spend its first visible frames waiting on the network.
    LaunchedEffect(
        studentAuthState.session?.uid,
        studentAuthState.session?.emailVerified,
        orderState.guestVenue?.establishment?.id,
    ) {
        if (
            studentAuthState.session?.emailVerified == true &&
            orderState.guestVenue != null &&
            studentAuthViewModel.isReadyForCheckout() &&
            walletRemoteState.data == null
        ) {
            walletViewModel.refresh()
        }
    }

    // Keep client orders warm while browsing the student shell. This makes Pedidos a
    // composition swap instead of starting its first network request after navigation.
    LaunchedEffect(
        studentAuthState.session?.uid,
        studentAuthState.session?.emailVerified,
        orderState.guestVenue?.establishment?.id,
        authorizedAccessState.activeContext?.role,
        operationalState.role,
    ) {
        if (
            studentAuthState.session?.emailVerified == true &&
            orderState.guestVenue != null &&
            authorizedAccessState.activeContext == null &&
            studentAuthViewModel.isReadyForCheckout() &&
            operationalState.role == null
        ) {
            operationalViewModel.setRole(OperationalRole.CLIENT)
        }
    }

    fun enterVenueAndOpenCatalog(venue: GuestVenueContext) {
        val switchingEstablishment = isEstablishmentSwitch(orderState.guestVenue, venue)
        if (switchingEstablishment) {
            operationalViewModel.clearRole()
        }
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
        navController.navigate(Routes.staffInvitationRoute(token)) {
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
        val targetRoute =
            when (modeRole) {
                OperationalRole.CASHIER -> Routes.CASHIER
                OperationalRole.KITCHEN -> Routes.KITCHEN
                OperationalRole.WAITER -> Routes.WAITER
                OperationalRole.CLIENT -> Routes.CATALOG
            }
        if (modeRole == OperationalRole.CLIENT) {
            orderFlowViewModel.refresh()
        }
        navController.navigate(targetRoute) {
            popUpTo(Routes.DISCOVERY) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    fun returnToClientFromAuthorizedMode() {
        authorizedAccessViewModel.returnToClient {
            operationalViewModel.setRole(OperationalRole.CLIENT)
            orderFlowViewModel.clearGuestVenue()
            orderFlowViewModel.refresh()
            navController.navigate(Routes.CATALOG) {
                popUpTo(Routes.DISCOVERY) {
                    inclusive = false
                }
                launchSingleTop = true
            }
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

    val activeOrder = orderState.createdOrder ?: operationalState.menuOrder

    fun dismissClientOrder(orderId: String) {
        operationalViewModel.dismissClientOrder(orderId)
        orderFlowViewModel.dismissCreatedOrder(orderId)
    }

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
                        hasStaffLaunchModes(
                            authorizedAccessViewModel.state.value.modes
                                .map { it.role },
                        ),
                )
            val route =
                when (destination) {
                    LaunchDestination.Login,
                    LaunchDestination.Discovery,
                    -> Routes.DISCOVERY
                    LaunchDestination.StaffModes -> Routes.STAFF_MODES
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
            when {
                session == null -> Routes.authLandingRoute(returnRoute)
                !session.emailVerified -> Routes.authVerifyRoute(returnRoute)
                else -> Routes.authLoginRoute(returnRoute)
            }
        navController.navigate(targetRoute) {
            launchSingleTop = true
        }
    }

    fun finishAccountSession(noticeMessage: String? = null) {
        orderFlowViewModel.clearForSessionTermination()
        walletViewModel.clearForSessionTermination()
        discoveryViewModel.clearForSessionTermination()
        operationalViewModel.clearRole()
        authorizedAccessViewModel.resetAfterSignOut()
        studentAuthViewModel.markSessionCleared(noticeMessage)
        navController.navigate(Routes.authLoginRoute(Routes.DISCOVERY)) {
            popUpTo(Routes.DISCOVERY) { inclusive = false }
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
        onPrepareStudent = { tab ->
            when (tab) {
                StudentTab.MENU -> {
                    if (orderState.catalog == null && !orderState.loading) {
                        orderFlowViewModel.refresh()
                    }
                }

                StudentTab.ORDERS -> {
                    if (!orderFlowViewModel.requiresStudentAuth()) {
                        if (operationalState.role != OperationalRole.CLIENT) {
                            operationalViewModel.setRole(OperationalRole.CLIENT)
                        } else if (operationalState.orders.isEmpty() && !operationalState.loading) {
                            operationalViewModel.refresh()
                        }
                    }
                }

                StudentTab.WALLET -> {
                    if (
                        studentAuthState.session?.emailVerified == true &&
                        studentAuthViewModel.isReadyForCheckout() &&
                        walletRemoteState.data == null &&
                        !walletRemoteState.loading
                    ) {
                        walletViewModel.refresh()
                    }
                }

                StudentTab.CART, StudentTab.ASSISTANT -> Unit
            }
        },
        catalogDetailOpen = orderState.selectedProductId != null,
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            enterTransition = { studentTabSlideEnter(this) },
            exitTransition = { studentTabSlideExit(this) },
            popEnterTransition = { studentTabSlideEnter(this) },
            popExitTransition = { studentTabSlideExit(this) },
        ) {
            fun switchToTestRole(targetRole: OperationalRole) {
                authorizedAccessViewModel.enterTestMode(targetRole) {
                    operationalViewModel.setRole(targetRole)
                    val targetRoute =
                        when (targetRole) {
                            OperationalRole.CLIENT -> {
                                orderFlowViewModel.enterGuestVenue(UnifiedTestModeManager.testGuestVenue)
                                Routes.CATALOG
                            }
                            OperationalRole.CASHIER -> {
                                orderFlowViewModel.clearGuestVenue()
                                Routes.CASHIER
                            }
                            OperationalRole.KITCHEN -> {
                                orderFlowViewModel.clearGuestVenue()
                                Routes.KITCHEN
                            }
                            OperationalRole.WAITER -> {
                                orderFlowViewModel.clearGuestVenue()
                                Routes.WAITER
                            }
                        }
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.id) {
                            inclusive = false
                            saveState = false
                        }
                        launchSingleTop = true
                    }
                }
            }

            composable(Routes.SPLASH) {
                var preloadedDestination by remember { mutableStateOf<LaunchDestination?>(null) }

                LaunchedEffect(Unit) {
                    authorizedAccessViewModel.refreshCurrentSession()
                    studentAuthViewModel.refreshGuestVenue()
                    discoveryViewModel.search("")
                    if (orderState.guestVenue != null) {
                        orderFlowViewModel.refresh()
                    }
                    authorizedAccessViewModel.refreshModes(force = true) {
                        preloadedDestination =
                            resolveLaunchDestination(
                                pendingEstablishmentSlug = pendingEstablishmentSlug,
                                session = authorizedAccessViewModel.state.value.session,
                                hasStaffModes =
                                    hasStaffLaunchModes(
                                        authorizedAccessViewModel.state.value.modes
                                            .map { it.role },
                                    ),
                            )
                    }
                }

                SplashScreen(
                    onFinished = {
                        val destination =
                            preloadedDestination ?: resolveLaunchDestination(
                                pendingEstablishmentSlug = pendingEstablishmentSlug,
                                session = authorizedAccessViewModel.state.value.session,
                                hasStaffModes =
                                    hasStaffLaunchModes(
                                        authorizedAccessViewModel.state.value.modes
                                            .map { it.role },
                                    ),
                            )
                        navigateLaunchDestination(destination)
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
                    onEnterTestMode = ::switchToTestRole,
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
                route = Routes.STAFF_INVITATION,
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
                            navController.navigate(Routes.STAFF_MODES) {
                                popUpTo(Routes.STAFF_INVITATION) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onAuthenticate = {
                        navigateStudentAuth(Routes.staffInvitationRoute(token))
                    },
                )
            }

            composable(Routes.STAFF_MODES) {
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
                    onEnterTestMode = ::switchToTestRole,
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
                    onDeleteOrder = ::dismissClientOrder,
                    onOpenWallet = { navController.navigateStudent(Routes.WALLET) },
                    onChangeVenue = {
                        navController.navigate(Routes.DISCOVERY) {
                            launchSingleTop = true
                        }
                    },
                    onOpenModes =
                        if (
                            authorizedAccessState.activeContext == null &&
                            authorizedAccessState.modes.any { mode ->
                                mode.role == OperationalRole.CASHIER || mode.role == OperationalRole.KITCHEN
                            }
                        ) {
                            { navController.navigate(Routes.STAFF_MODES) { launchSingleTop = true } }
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
                LaunchedEffect(
                    studentAuthState.session?.uid,
                    orderState.guestVenue?.establishment?.id,
                ) {
                    studentAuthViewModel.ensureVenueContext(
                        onReady = walletViewModel::refresh,
                        onNeedsAuth = { navigateStudentAuth(Routes.WALLET) },
                    )
                }
                WalletScreen(
                    remoteState = walletRemoteState,
                    userId =
                        walletRemoteState.data?.wallet?.userId
                            ?: studentAuthState.session?.uid,
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
                        navController.navigate(Routes.STAFF_MODES) {
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
                    accountDeletionState = accountDeletionState,
                    onRequestAccountDeletion = accountDeletionViewModel::requestConfirmation,
                    onConfirmAccountDeletion = accountDeletionViewModel::confirm,
                    onCancelAccountDeletion = accountDeletionViewModel::cancel,
                    onSubmitAccountDeletionPassword = { password ->
                        accountDeletionViewModel.submitPassword(
                            password = password,
                            onDeleted = {
                                finishAccountSession("Tu cuenta fue eliminada correctamente")
                            },
                            onSessionInvalidated = { finishAccountSession() },
                        )
                    },
                    onRetryAccountDeletion = {
                        accountDeletionViewModel.retry(
                            onDeleted = {
                                finishAccountSession("Tu cuenta fue eliminada correctamente")
                            },
                            onSessionInvalidated = { finishAccountSession() },
                        )
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
                            studentAuthViewModel.ensureVenueContext(
                                onReady = orderFlowViewModel::submitOrder,
                                onNeedsAuth = { navigateStudentAuth(Routes.CART) },
                            )
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
                LaunchedEffect(returnRoute) { studentAuthViewModel.refreshGuestVenue() }
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
                LaunchedEffect(returnRoute) { studentAuthViewModel.refreshGuestVenue() }
                val existingVerifiedSession = studentAuthState.session?.emailVerified == true
                StudentLoginScreen(
                    state = studentAuthState,
                    onBack = { navController.popBackStack() },
                    showBack = !isLaunchLogin,
                    onEmailChange = studentAuthViewModel::updateEmail,
                    onPasswordChange = studentAuthViewModel::updatePassword,
                    onContextualIdChange = studentAuthViewModel::updateContextualId,
                    onLogin = {
                        when {
                            isLaunchLogin && existingVerifiedSession -> finishLaunchAuth()
                            isLaunchLogin ->
                                studentAuthViewModel.loginIdentity { authenticated ->
                                    if (authenticated) {
                                        finishLaunchAuth()
                                    } else {
                                        navController.navigate(Routes.authVerifyRoute(returnRoute)) {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            existingVerifiedSession ->
                                studentAuthViewModel.completeEnrollment(
                                    onSuccess = { finishStudentAuth(returnRoute) },
                                    onNeedsVerify = {
                                        navController.navigate(
                                            Routes.authVerifyRoute(returnRoute),
                                        ) {
                                            launchSingleTop = true
                                        }
                                    },
                                )
                            else ->
                                studentAuthViewModel.login { enrolled ->
                                    if (enrolled) {
                                        finishStudentAuth(returnRoute)
                                    } else {
                                        navController.navigate(Routes.authVerifyRoute(returnRoute)) {
                                            launchSingleTop = true
                                        }
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
                    existingVerifiedSession = existingVerifiedSession,
                    onEnterTestMode = ::switchToTestRole,
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
                val isFreshConfirmation = orderState.createdOrder != null
                val selectedOrder = operationalState.selectedOrder
                val confirmationOrder =
                    orderState.createdOrder
                        ?: orderState.stripeObservedOrder?.takeIf {
                            selectedOrder == null || selectedOrder.summary.id == it.summary.id
                        }
                        ?: selectedOrder
                        ?: orderState.stripeObservedOrder
                val context = LocalContext.current
                LaunchedEffect(confirmationOrder?.summary?.id) {
                    val order = confirmationOrder ?: return@LaunchedEffect
                    if (order.summary.paymentMethod == PaymentMethod.STRIPE) {
                        orderFlowViewModel.resumeStripePaymentConfirmation(order)
                    }
                }
                val paymentSheet =
                    PaymentSheet
                        .Builder { result ->
                            when (result) {
                                is PaymentSheetResult.Completed -> orderFlowViewModel.onStripePaymentSheetCompleted()
                                is PaymentSheetResult.Canceled -> orderFlowViewModel.onStripePaymentSheetCanceled()
                                is PaymentSheetResult.Failed ->
                                    orderFlowViewModel.onStripePaymentSheetFailed(result.error.message)
                            }
                        }.build()
                LaunchedEffect(orderState.stripePresentationKey) {
                    val session = orderState.stripePaymentSession ?: return@LaunchedEffect
                    if (orderState.stripePresentationKey == null) return@LaunchedEffect
                    val stripeConfig = session.toRuntimeConfiguration()
                    PaymentConfiguration.init(
                        context = context,
                        publishableKey = stripeConfig.publishableKey,
                        stripeAccountId = stripeConfig.stripeAccountId,
                    )
                    orderFlowViewModel.markStripePaymentSheetPresented()
                    paymentSheet.presentWithPaymentIntent(
                        paymentIntentClientSecret = stripeConfig.clientSecret,
                        configuration = PaymentSheet.Configuration(merchantDisplayName = "Vaiinilla"),
                    )
                }
                OrderConfirmationScreen(
                    stripePaymentPhase = orderState.stripePaymentPhase,
                    stripePaymentMessage = orderState.stripePaymentMessage,
                    retryingStripePayment = orderState.retryingStripePayment,
                    onRetryStripePayment = orderFlowViewModel::retryStripePayment,
                    onRefreshStripePayment = orderFlowViewModel::refreshStripePaymentStatus,
                    purchaseCelebration = orderState.purchaseCelebration,
                    onPurchaseCelebrationFinished = orderFlowViewModel::completePurchaseCelebration,
                    order = confirmationOrder,
                    onReturnToMenu = {
                        if (isFreshConfirmation) {
                            orderFlowViewModel.clearCreatedOrder()
                        }
                        navController.navigate(Routes.CATALOG) {
                            popUpTo(Routes.CATALOG) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onViewTracking = {
                        operationalViewModel.setRole(OperationalRole.CLIENT)
                        confirmationOrder
                            ?.summary
                            ?.id
                            ?.let(operationalViewModel::selectOrder)
                        val returnedToExistingTracking =
                            navController.popBackStack(Routes.STUDENT_TRACKING, inclusive = false)
                        if (!returnedToExistingTracking) {
                            navController.navigateStudent(Routes.STUDENT_TRACKING)
                        }
                    },
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
                val venueAuthRequired = orderFlowViewModel.requiresStudentAuth()
                LaunchedEffect(venueAuthRequired, studentAuthState.session?.uid) {
                    if (venueAuthRequired) {
                        studentAuthViewModel.ensureVenueContext(
                            onReady = {
                                if (operationalState.role != OperationalRole.CLIENT) {
                                    operationalViewModel.setRole(OperationalRole.CLIENT)
                                }
                            },
                            onNeedsAuth = { navigateStudentAuth(Routes.STUDENT_TRACKING) },
                        )
                    } else if (operationalState.role != OperationalRole.CLIENT) {
                        operationalViewModel.setRole(OperationalRole.CLIENT)
                    }
                }
                if (!venueAuthRequired) {
                    StudentTrackingScreen(
                        state = operationalState,
                        orderState = orderState,
                        onMenu = { navController.navigateStudent(Routes.CATALOG) },
                        onAssistant = {},
                        onWallet = { navController.navigateStudent(Routes.WALLET) },
                        onCart = { navController.navigateStudent(Routes.CART) },
                        onOpenCatalog = { navController.navigateStudent(Routes.CATALOG) },
                        onSelectOrder = operationalViewModel::selectOrder,
                        onBackFromSelectedOrder = { operationalViewModel.selectOrder(null) },
                        onDeleteOrder = ::dismissClientOrder,
                        onViewReceipt = {
                            orderFlowViewModel.clearCreatedOrder()
                            navController.navigate(Routes.CONFIRMATION) { launchSingleTop = true }
                        },
                        onRefresh = { operationalViewModel.refresh() },
                    )
                }
            }

            composable(Routes.CASHIER) {
                val authorizedCashier = authorizedAccessState.activeContext?.role == OperationalRole.CASHIER
                LaunchedEffect(authorizedCashier) {
                    if (!authorizedCashier) {
                        navController.navigate(Routes.STAFF_MODES) {
                            launchSingleTop = true
                        }
                    }
                }
                if (authorizedCashier) {
                    OperationalPresenceLifecycle(OperationalRole.CASHIER, operationalViewModel)
                    CashierOperationalScreen(
                        state = operationalState,
                        onBack = returnToModes(navController, operationalViewModel),
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
                            if (authorizedAccessState.hasMultipleModes) {
                                returnToModes(navController, operationalViewModel)
                            } else {
                                null
                            },
                        restrictedMode = authorizedAccessState.activeContext?.restrictedMode,
                        onSwitchToRole = ::switchToTestRole,
                        onToggleProductAvailable = { id, avail ->
                            if (com.vaiinilla.app.ui.mode.UnifiedTestModeManager.isTestModeActive.value) {
                                com.vaiinilla.app.ui.mode.UnifiedTestModeManager
                                    .toggleProductAvailable(id, avail)
                                operationalViewModel.refresh()
                            } else {
                                operationalViewModel.setProductAvailable(id, avail)
                            }
                        },
                        onCreateCashierProduct = operationalViewModel::createCashierProduct,
                        onUploadCashierProductImage = operationalViewModel::uploadCashierProductImage,
                    )
                }
            }

            composable(Routes.KITCHEN) {
                val authorizedKitchen = authorizedAccessState.activeContext?.role == OperationalRole.KITCHEN
                LaunchedEffect(authorizedKitchen) {
                    if (!authorizedKitchen) {
                        returnToModes(navController, operationalViewModel)()
                    }
                }
                if (authorizedKitchen) {
                    OperationalPresenceLifecycle(OperationalRole.KITCHEN, operationalViewModel)
                    KitchenOperationalScreen(
                        state = operationalState,
                        onBack = returnToModes(navController, operationalViewModel),
                        onStart = operationalViewModel::startKitchen,
                        onReady = operationalViewModel::markReady,
                        onChangeMode =
                            if (authorizedAccessState.hasMultipleModes) {
                                returnToModes(navController, operationalViewModel)
                            } else {
                                null
                            },
                        onSwitchToRole = ::switchToTestRole,
                        restrictedMode = authorizedAccessState.activeContext?.restrictedMode,
                    )
                }
            }

            composable(Routes.WAITER) {
                val authorizedWaiter = authorizedAccessState.activeContext?.role == OperationalRole.WAITER
                LaunchedEffect(authorizedWaiter) {
                    if (!authorizedWaiter) {
                        returnToModes(navController, operationalViewModel)()
                    }
                }
                if (authorizedWaiter) {
                    OperationalPresenceLifecycle(OperationalRole.WAITER, operationalViewModel)
                    WaiterOperationalScreen(
                        state = operationalState,
                        onBack = returnToModes(navController, operationalViewModel),
                        onDeliver = { orderId, version -> operationalViewModel.deliver(orderId, version) },
                        onScanDeliver = { orderId, version ->
                            pendingPickupDelivery = PendingPickupDelivery(orderId, version)
                        },
                        onChangeMode =
                            if (authorizedAccessState.hasMultipleModes) {
                                returnToModes(navController, operationalViewModel)
                            } else {
                                null
                            },
                        onSwitchToRole = ::switchToTestRole,
                        restrictedMode = authorizedAccessState.activeContext?.restrictedMode,
                    )
                }
            }
        }
    }

    BackHandler(enabled = orderState.selectedProductId != null) {
        orderFlowViewModel.closeProduct()
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
        navController.navigate(Routes.STAFF_MODES) {
            popUpTo(Routes.DISCOVERY) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

private val StudentTabMotionEase = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private const val STUDENT_TAB_SLIDE_MS = 220

private fun studentTabSlideEnter(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition {
    val initialTab = studentTabForRoute(scope.initialState.destination.route)
    val targetTab = studentTabForRoute(scope.targetState.destination.route)
    val fromOrder = studentTabOrder(initialTab)
    val toOrder = studentTabOrder(targetTab)
    if (fromOrder == -1 || toOrder == -1 || fromOrder == toOrder) {
        return EnterTransition.None
    }
    val forward = toOrder > fromOrder
    return slideInHorizontally(
        animationSpec = tween(durationMillis = STUDENT_TAB_SLIDE_MS, easing = StudentTabMotionEase),
        initialOffsetX = { fullWidth -> if (forward) fullWidth else -fullWidth },
    )
}

private fun studentTabSlideExit(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition {
    val initialTab = studentTabForRoute(scope.initialState.destination.route)
    val targetTab = studentTabForRoute(scope.targetState.destination.route)
    val fromOrder = studentTabOrder(initialTab)
    val toOrder = studentTabOrder(targetTab)
    if (fromOrder == -1 || toOrder == -1 || fromOrder == toOrder) {
        return ExitTransition.None
    }
    val forward = toOrder > fromOrder
    return slideOutHorizontally(
        animationSpec = tween(durationMillis = STUDENT_TAB_SLIDE_MS, easing = StudentTabMotionEase),
        targetOffsetX = { fullWidth -> if (forward) -fullWidth else fullWidth },
    )
}
