package com.vaiinilla.app.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.vaiinilla.app.R
import com.vaiinilla.app.core.notifications.ChildDeepLinkStore
import com.vaiinilla.app.core.notifications.OrderAdvanceNotifier
import com.vaiinilla.app.core.notifications.OrderNotificationTarget
import com.vaiinilla.app.data.auth.student.GoogleSignInHelper
import com.vaiinilla.app.data.auth.student.GoogleSignInUnavailableException
import com.vaiinilla.app.domain.counter.CounterScope
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.isStripePaymentConfirmedByBackend
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.canCallWaiter
import com.vaiinilla.app.domain.repository.hasPendingAccount
import com.vaiinilla.app.ui.account.AccountDeletionViewModel
import com.vaiinilla.app.ui.admin.AdminSpacesViewModel
import com.vaiinilla.app.ui.admin.AdminStaffViewModel
import com.vaiinilla.app.ui.admin.AdminStoreViewModel
import com.vaiinilla.app.ui.admin.AdminViewModel
import com.vaiinilla.app.ui.auth.student.StudentAuthViewModel
import com.vaiinilla.app.ui.components.MenuWaiterLayer
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.WhileStarted
import com.vaiinilla.app.ui.components.isRental
import com.vaiinilla.app.ui.components.prefetchProductImages
import com.vaiinilla.app.ui.counter.CashierStudentsEntry
import com.vaiinilla.app.ui.counter.KitchenReservedEntry
import com.vaiinilla.app.ui.counter.ReservedOrdersSheet
import com.vaiinilla.app.ui.counter.ReservedOrdersViewModel
import com.vaiinilla.app.ui.counter.StudentCounterSheet
import com.vaiinilla.app.ui.counter.StudentCounterViewModel
import com.vaiinilla.app.ui.counter.actions
import com.vaiinilla.app.ui.discovery.GuestDiscoveryViewModel
import com.vaiinilla.app.ui.discovery.QrScannerAlternative
import com.vaiinilla.app.ui.discovery.QrScannerDialog
import com.vaiinilla.app.ui.mode.AuthorizedAccessViewModel
import com.vaiinilla.app.ui.operational.CashierContext
import com.vaiinilla.app.ui.operational.CourtsViewModel
import com.vaiinilla.app.ui.operational.OperationalPresenceLifecycle
import com.vaiinilla.app.ui.operational.OperationalViewModel
import com.vaiinilla.app.ui.operational.PendingRefundsViewModel
import com.vaiinilla.app.ui.operational.WaiterViewModel
import com.vaiinilla.app.ui.order.OrderFlowViewModel
import com.vaiinilla.app.ui.order.canPayWithBalance
import com.vaiinilla.app.ui.order.canPayWithCard
import com.vaiinilla.app.ui.order.cartItemCount
import com.vaiinilla.app.ui.order.isEstablishmentSwitch
import com.vaiinilla.app.ui.order.menuWaiterSpaceId
import com.vaiinilla.app.ui.order.toRuntimeConfiguration
import com.vaiinilla.app.ui.order.waiterCallAlias
import com.vaiinilla.app.ui.parentmode.ChildrenViewModel
import com.vaiinilla.app.ui.parentmode.GuardianControlsViewModel
import com.vaiinilla.app.ui.parentmode.ParentModeWalletHost
import com.vaiinilla.app.ui.profile.displayInitials
import com.vaiinilla.app.ui.reservations.ReservationsScreen
import com.vaiinilla.app.ui.reservations.ReservationsViewModel
import com.vaiinilla.app.ui.screens.AssistantChatScreen
import com.vaiinilla.app.ui.screens.AuthorizedModeScreen
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CashierOperationalScreen
import com.vaiinilla.app.ui.screens.CashierRefundsSection
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.CounterOrderSheet
import com.vaiinilla.app.ui.screens.DiscoveryScreen
import com.vaiinilla.app.ui.screens.FloorTable
import com.vaiinilla.app.ui.screens.InvitationAcceptanceScreen
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
import com.vaiinilla.app.ui.screens.LocalScene
import com.vaiinilla.app.ui.screens.LocalVenueArt
import com.vaiinilla.app.ui.screens.MenuFeedScreen
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import com.vaiinilla.app.ui.screens.OrdersAccessGate
import com.vaiinilla.app.ui.screens.ReceiptStickerScreen
import com.vaiinilla.app.ui.screens.SharedTableCard
import com.vaiinilla.app.ui.screens.StoreEntrance
import com.vaiinilla.app.ui.screens.StoreEntranceOutcome
import com.vaiinilla.app.ui.screens.StoreEntranceOverlay
import com.vaiinilla.app.ui.screens.StudentAuthLandingScreen
import com.vaiinilla.app.ui.screens.StudentForgotPasswordScreen
import com.vaiinilla.app.ui.screens.StudentLoginScreen
import com.vaiinilla.app.ui.screens.StudentRegisterScreen
import com.vaiinilla.app.ui.screens.StudentTrackingScreen
import com.vaiinilla.app.ui.screens.StudentVerifyEmailScreen
import com.vaiinilla.app.ui.screens.VenueSwitchDialog
import com.vaiinilla.app.ui.screens.WaiterOperationalScreen
import com.vaiinilla.app.ui.screens.WalletAccountScreen
import com.vaiinilla.app.ui.screens.WalletAddCardScreen
import com.vaiinilla.app.ui.screens.WalletAddMoneyScreen
import com.vaiinilla.app.ui.screens.WalletPaymentMethodsScreen
import com.vaiinilla.app.ui.screens.WalletScreen
import com.vaiinilla.app.ui.screens.WelcomeBootStage
import com.vaiinilla.app.ui.screens.admin.AdminActions
import com.vaiinilla.app.ui.screens.admin.AdminCashActions
import com.vaiinilla.app.ui.screens.admin.AdminScreen
import com.vaiinilla.app.ui.screens.admin.AdminSectionsHost
import com.vaiinilla.app.ui.screens.admin.AdminTakeOrderTab
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.screens.rememberReduceMotion
import com.vaiinilla.app.ui.screens.rememberVenueArt
import com.vaiinilla.app.ui.screens.scenePaletteForTheme
import com.vaiinilla.app.ui.sharedtable.SharedTableViewModel
import com.vaiinilla.app.ui.support.SupportLastOrder
import com.vaiinilla.app.ui.support.SupportScreen
import com.vaiinilla.app.ui.wallet.WalletViewModel
import com.vaiinilla.app.ui.wallet.rememberWalletUiState
import com.vaiinilla.app.ui.wallet.visibleFor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private data class PendingPickupDelivery(
    val orderId: String,
    val expectedVersion: Int,
)

private const val POP_ANIMATION_MILLIS = 300
private const val POP_ENTER_INITIAL_SCALE = 0.92f

@Composable
fun AppNavHost(
    navController: NavHostController,
    pendingEstablishmentSlug: String? = null,
    pendingSpaceToken: String? = null,
    pendingInvitationToken: String? = null,
    pendingOrderId: String? = null,
    pendingOrderTarget: OrderNotificationTarget? = null,
    onDeepLinkConsumed: () -> Unit = {},
    onSpaceLinkConsumed: () -> Unit = {},
    onInvitationConsumed: () -> Unit = {},
    onOrderConsumed: () -> Unit = {},
) {
    // Vive lo que vive esta composición: si la actividad se recrea (p. ej. al cambiar el tamaño de letra), lo que
    // espera aquí se cancela y no navega con un NavController que ya no está en pantalla (L01).
    val launchNavScope = rememberCoroutineScope()
    val orderFlowViewModel: OrderFlowViewModel = viewModel()
    val operationalViewModel: OperationalViewModel = viewModel()
    val adminViewModel: AdminViewModel = viewModel()
    val adminStaffViewModel: AdminStaffViewModel = viewModel()
    val adminStoreViewModel: AdminStoreViewModel = viewModel()
    val adminSpacesViewModel: AdminSpacesViewModel = viewModel()
    val adminSchoolViewModel: com.vaiinilla.app.ui.admin.school.AdminSchoolViewModel = viewModel()
    val waiterViewModel: WaiterViewModel = viewModel()
    val courtsViewModel: CourtsViewModel = viewModel()
    val reservationsViewModel: ReservationsViewModel = viewModel()
    val sharedTableViewModel: SharedTableViewModel = viewModel()
    val studentAuthViewModel: StudentAuthViewModel = viewModel()
    val authorizedAccessViewModel: AuthorizedAccessViewModel = viewModel()
    val discoveryViewModel: GuestDiscoveryViewModel = viewModel()
    val walletViewModel: WalletViewModel = viewModel()
    val childrenViewModel: ChildrenViewModel = viewModel()
    val guardianControlsViewModel: GuardianControlsViewModel = viewModel()
    val accountDeletionViewModel: AccountDeletionViewModel = viewModel()
    // Caja de una escuela (alumnos sin teléfono) y "Para mañana" de Cocina: padres e hijos.
    val studentCounterViewModel: StudentCounterViewModel = viewModel()
    val reservedOrdersViewModel: ReservedOrdersViewModel = viewModel()
    val pendingRefundsViewModel: PendingRefundsViewModel = viewModel()
    val orderState by orderFlowViewModel.uiState
    val operationalState by operationalViewModel.uiState
    val waiterState by waiterViewModel.uiState
    val studentAuthState by studentAuthViewModel.state
    val authorizedAccessState by authorizedAccessViewModel.state
    val discoveryState by discoveryViewModel.state
    val walletRemoteState by walletViewModel.state.collectAsStateWithLifecycle()
    val accountDeletionState by accountDeletionViewModel.state
    val walletState = rememberWalletUiState()
    val currentVenueId = orderState.guestVenue?.establishment?.id
    val walletForVenue = walletRemoteState.visibleFor(currentVenueId)
    // La cola de Caja sin conexión vive todo el turno de Caja, no solo con la hoja de Alumnos abierta.
    val cashierShift =
        authorizedAccessState.activeContext
            ?.takeIf { it.role == OperationalRole.CASHIER }
            ?.let { CounterScope(membershipId = it.membershipId, establishmentId = it.establishmentId) }
    LaunchedEffect(cashierShift) { studentCounterViewModel.setShift(cashierShift) }
    LaunchedEffect(authorizedAccessState.activeContext?.membershipId) {
        waiterViewModel.bindContext(authorizedAccessState.activeContext?.membershipId)
    }
    LaunchedEffect(operationalState.cashSessionOpen) {
        studentCounterViewModel.setCashOpen(operationalState.cashSessionOpen != false)
    }

    // Keep every in-memory copy of the selected venue aligned when persisted metadata
    // changes (for example, an establishment rename that also changes its slug).
    LaunchedEffect(orderState.guestVenue) {
        discoveryViewModel.refreshSelectedVenue()
        studentAuthViewModel.refreshGuestVenue()
    }
    // La mesa compartida no pasa de un negocio a otro (C08).
    LaunchedEffect(orderState.guestVenue?.establishment?.id) {
        sharedTableViewModel.bindVenue(orderState.guestVenue?.establishment?.id)
    }

    // Warm product imagery as soon as catalog data exists, before cards enter composition.
    LaunchedEffect(orderState.catalog) {
        val urls =
            orderState.catalog
                ?.products
                ?.map { it.imageUrl }
                .orEmpty()
        if (urls.isNotEmpty()) prefetchProductImages(urls, card = true)
    }

    // Volver del background refresca el saldo: la recarga la hace Caja desde
    // otro dispositivo y no llega ningún evento push de wallet.
    val lifecycleOwner = LocalLifecycleOwner.current
    // Solo el cliente tiene cartera: el personal recibía 403 en cada regreso a la app.
    val isClientRole by rememberUpdatedState(operationalState.role == OperationalRole.CLIENT)
    val latestVenueId by rememberUpdatedState(currentVenueId)
    // "Hoy te quedan $N" depende del día y de lo que ya se gastó: se pide de nuevo al volver a la app.
    var resumeTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    resumeTick++
                    if (isClientRole) walletViewModel.refreshIfStale(latestVenueId)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            studentAuthViewModel.isReadyForCheckout()
        ) {
            walletViewModel.refreshIfStale(currentVenueId)
        }
    }

    // El alumno con tutor ve sus controles en menú, ficha y carrito (GET /tutelas/mis-controles).
    LaunchedEffect(
        orderState.operationalStatus?.isSchool,
        studentAuthState.session?.uid,
        orderState.guestVenue?.establishment?.id,
        authorizedAccessState.activeContext?.role,
        orderState.createOrderError,
        orderState.createdOrder?.summary?.id,
        resumeTick,
    ) {
        val isClientInSchool =
            orderState.operationalStatus?.isSchool == true &&
                authorizedAccessState.activeContext == null &&
                studentAuthState.session?.emailVerified == true &&
                studentAuthViewModel.isReadyForCheckout()
        guardianControlsViewModel.refresh(isClientInSchool)
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

    val appContext = LocalContext.current
    LaunchedEffect(Unit) {
        operationalViewModel.orderAdvanceEvents.collect { event ->
            OrderAdvanceNotifier.notify(
                context = appContext,
                orderId = event.orderId,
                folio = event.folio,
                newState = event.newState,
                inSpace = event.inSpace,
                spaceType = event.spaceType,
            )
            // Pagar, cancelar o devolver mueve el saldo: no se espera a que el dato envejezca.
            walletViewModel.invalidate()
            walletViewModel.refreshIfStale(latestVenueId)
        }
    }
    // El pago de una renta crea un pedido: se sigue en la confirmación (hoja de tarjeta, folio para pagar en caja o
    // compra ya pagada con saldo). Si la persona ya salió de Reservas, el pedido se adopta y no se le arrastra (FLOW-16).
    LaunchedEffect(Unit) {
        reservationsViewModel.paidOrderEvents.collect { order ->
            orderFlowViewModel.adoptReservationOrder(order)
            orderFlowViewModel.refreshRentedCourt()
            if (navController.currentDestination?.route == Routes.RESERVATIONS) {
                navController.navigate(Routes.CONFIRMATION) { launchSingleTop = true }
            }
        }
    }
    // Se pregunta una sola vez por proceso: con "no volver a preguntar" Android ya no abre el diálogo, así que
    // pedirlo en cada cambio de rol no avisaba a nadie. Si dice que no, se le explica y se le lleva a Ajustes.
    var notificationPermissionAsked by rememberSaveable { mutableStateOf(false) }
    var notificationPromptOpen by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) notificationPromptOpen = true
        }
    LaunchedEffect(operationalState.role) {
        if (
            operationalState.role != null &&
            !notificationPermissionAsked &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !OrderAdvanceNotifier.canPost(appContext)
        ) {
            notificationPermissionAsked = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    if (notificationPromptOpen) NotificationPermissionPrompt(onDismiss = { notificationPromptOpen = false })

    // Tap en notificación push/local: objetivo cliente -> STUDENT_TRACKING con
    // la orden seleccionada; staff con rol activo -> solo la selecciona en su
    // cola; staff autenticado sin rol activo -> espera la selección de modo.
    // Intents antiguos sin objetivo se tratan como cliente.
    var clientSwitchAttemptedFor by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(
        pendingOrderId,
        pendingOrderTarget,
        studentAuthState.session?.uid,
        studentAuthState.loading,
        operationalState.role,
        authorizedAccessState.loading,
        authorizedAccessState.modes,
    ) {
        val target = pendingOrderTarget ?: OrderNotificationTarget.CLIENT
        val orderId = pendingOrderId
        if (orderId == null) {
            clientSwitchAttemptedFor = null
            // Aviso de personal sin pedido (llamada de mesa): abrir la consola de modos.
            if (target == OrderNotificationTarget.STAFF) {
                if (navController.currentDestination?.route != Routes.STAFF_MODES) {
                    navController.navigate(Routes.STAFF_MODES) { launchSingleTop = true }
                }
                onOrderConsumed()
            }
            return@LaunchedEffect
        }

        fun openClientOrder() {
            if (operationalState.role != OperationalRole.CLIENT) {
                operationalViewModel.setRole(OperationalRole.CLIENT)
            }
            operationalViewModel.refreshOrder(orderId)
            operationalViewModel.selectOrder(orderId)
            navController.navigateStudent(Routes.STUDENT_TRACKING)
            // Un pedido que no es renta se abre en su pantalla de recogida, como al tocarlo en la lista; sin esto
            // el aviso de "listo" dejaba al cliente en la lista (PEDIDO-12). Si aún no se cargó, queda la lista.
            val known = operationalState.orders.firstOrNull { it.summary.id == orderId }
            if (known != null && !known.isRental) {
                orderFlowViewModel.clearCreatedOrder()
                navController.navigate(Routes.CONFIRMATION) { launchSingleTop = true }
            }
            onOrderConsumed()
        }

        when (
            resolveOrderNotificationAction(
                target = target,
                hasStudentSession = studentAuthState.session != null,
                studentAuthLoading = studentAuthState.loading,
                operationalRole = operationalState.role,
                accessLoading = authorizedAccessState.loading,
                hasStaffModes = authorizedAccessState.modes.any { it.role != OperationalRole.CLIENT },
            )
        ) {
            OrderNotificationAction.OPEN_CLIENT_ORDER -> openClientOrder()
            OrderNotificationAction.SWITCH_TO_CLIENT_ORDER -> {
                if (clientSwitchAttemptedFor == orderId) {
                    onOrderConsumed()
                } else {
                    clientSwitchAttemptedFor = orderId
                    authorizedAccessViewModel.returnToClient { openClientOrder() }
                }
            }
            OrderNotificationAction.SELECT_STAFF_ORDER -> {
                operationalViewModel.refreshOrder(orderId)
                operationalViewModel.selectOrder(orderId)
                onOrderConsumed()
            }
            OrderNotificationAction.OPEN_STAFF_MODES -> {
                if (navController.currentDestination?.route != Routes.STAFF_MODES) {
                    navController.navigate(Routes.STAFF_MODES) { launchSingleTop = true }
                }
            }
            OrderNotificationAction.WAIT -> Unit
            OrderNotificationAction.DISCARD -> onOrderConsumed()
        }
    }

    // El QR personal de la cartera abierto sobre la casita: la barra de abajo se va mientras tanto.
    var walletCodeOpen by remember { mutableStateOf(false) }

    // Entrar a un lugar cruza la puerta de la tienda (`StoreEntranceOverlay`); el menú aparece al terminar.
    var entrance by remember { mutableStateOf<StoreEntrance?>(null) }
    var entranceEntered by remember { mutableStateOf(false) }
    var entranceTimedOut by remember { mutableStateOf(false) }
    LaunchedEffect(entrance?.id) {
        if (entrance == null) return@LaunchedEffect
        // Si el lugar nunca responde, la entrada no se queda tapando la pantalla.
        delay(ENTRANCE_TIMEOUT_MS)
        if (!entranceEntered) entranceTimedOut = true
    }
    // Con el lugar ya abierto el menú puede tardar en cargar: pasado el mismo plazo la puerta se abre y el menú
    // muestra su propia carga o su error, en vez de tapar la pantalla sin salida (COMPRA-21).
    LaunchedEffect(entrance?.id, entranceEntered) {
        if (entrance == null || !entranceEntered) return@LaunchedEffect
        delay(ENTRANCE_TIMEOUT_MS)
        entranceTimedOut = true
    }
    val entranceOutcome =
        when {
            entranceEntered && orderState.loading && !entranceTimedOut -> StoreEntranceOutcome.WAITING
            entranceEntered -> StoreEntranceOutcome.READY
            entranceTimedOut -> StoreEntranceOutcome.FAILED
            discoveryState.resolving -> StoreEntranceOutcome.WAITING
            discoveryState.pendingSwitch != null ||
                discoveryState.errorMessage != null ||
                discoveryState.suspendedMessage != null -> StoreEntranceOutcome.FAILED
            else -> StoreEntranceOutcome.WAITING
        }

    fun startEntrance(code: String?) {
        entranceEntered = false
        entranceTimedOut = false
        entrance = StoreEntrance(code = code?.trim()?.takeIf { it.isNotEmpty() })
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
        // Con el QR de una mesa, la entrada se vuelve el plano del lugar con tu mesa marcada.
        val current = entrance
        val space = venue.space
        if (current?.code != null && current.table == null && space != null) {
            FloorTable.of(space.id, space.name, space.type)?.let { entrance = current.copy(table = it) }
        }
        entranceEntered = true
    }

    // Aviso de un hijo (Padres e hijos): su escuela y su ficha en Cartera.
    ChildNoticeEffect(
        action =
            resolveOrderNotificationAction(
                target = OrderNotificationTarget.CLIENT,
                hasStudentSession = studentAuthState.session != null,
                studentAuthLoading = studentAuthState.loading,
                operationalRole = operationalState.role,
                accessLoading = authorizedAccessState.loading,
                hasStaffModes = authorizedAccessState.modes.any { it.role != OperationalRole.CLIENT },
            ),
        currentEstablishmentId = orderState.guestVenue?.establishment?.id,
        returnToClient = { onReturned -> authorizedAccessViewModel.returnToClient(onReturned) },
        findSchool = discoveryViewModel::schoolFor,
        openSchool = { school, onEntered ->
            discoveryViewModel.openSchool(school) { venue ->
                enterVenueAndOpenCatalog(venue)
                onEntered()
            }
        },
        openWallet = { navController.navigateStudent(Routes.WALLET) },
    )

    LaunchedEffect(pendingEstablishmentSlug) {
        val slug = pendingEstablishmentSlug?.trim().orEmpty()
        if (slug.isEmpty()) return@LaunchedEffect
        startEntrance(slug)
        discoveryViewModel.openSlug(
            slug = slug,
            onEntered = { venue ->
                enterVenueAndOpenCatalog(venue)
            },
            onFinished = onDeepLinkConsumed,
        )
    }
    LaunchedEffect(pendingSpaceToken) {
        val token = pendingSpaceToken?.trim().orEmpty()
        if (token.isEmpty()) return@LaunchedEffect
        startEntrance(token)
        discoveryViewModel.updateSpaceToken(token)
        discoveryViewModel.resolveSpaceToken(
            onEntered = ::enterVenueAndOpenCatalog,
            onFinished = onSpaceLinkConsumed,
        )
    }
    var qrScannerOpen by remember { mutableStateOf(false) }
    // Cada aumento abre la hoja del código de mesa en Descubrir (alternativa al QR sin cámara).
    var tableCodeRequest by remember { mutableIntStateOf(0) }
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
                OperationalRole.ADMIN -> Routes.ADMIN
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
        // Solo con la app a la vista; al volver revisa de inmediato por si el acceso se revocó mientras tanto (FLOW-15).
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                authorizedAccessViewModel.syncAuthorizedAccessOrReturnToClient(
                    onForcedToClient = ::returnToClientFromAuthorizedMode,
                )
                delay(AUTHORIZED_ACCESS_SYNC_INTERVAL_MS)
            }
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
        walletViewModel.invalidate()
        operationalViewModel.setRole(OperationalRole.CLIENT)
        operationalViewModel.refreshOrder(created.summary.id)
        operationalViewModel.selectOrder(created.summary.id)
        if (navController.currentDestination?.route != Routes.CONFIRMATION) {
            navController.navigate(Routes.CONFIRMATION) {
                launchSingleTop = true
            }
        }
    }

    // El pedido recién creado conserva el estado con que nació; la lista del sondeo trae el vigente. Un pedido
    // cerrado ya no es "activo" (PEDIDO-08).
    val activeOrder =
        (
            orderState.createdOrder?.let { created ->
                operationalState.orders.firstOrNull { it.summary.id == created.summary.id } ?: created
            } ?: operationalState.menuOrder
        )?.takeUnless { it.summary.state.isClosed }

    fun dismissClientOrder(orderId: String) {
        operationalViewModel.dismissClientOrder(orderId)
        orderFlowViewModel.dismissCreatedOrder(orderId)
    }

    fun finishStudentAuth(returnRoute: String) {
        orderFlowViewModel.restoreGuestSessionAfterAuth()
        orderFlowViewModel.claimGuestOrders()
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
        orderFlowViewModel.claimGuestOrders()
        studentAuthViewModel.refreshGuestVenue()
        authorizedAccessViewModel.refreshCurrentSession()
        val modesRefreshed = CompletableDeferred<Boolean>()
        authorizedAccessViewModel.refreshModes(force = true) { modesRefreshed.complete(it) }
        launchNavScope.launch {
            val modesLoaded = modesRefreshed.await()
            val destination =
                resolveLaunchDestination(
                    pendingEstablishmentSlug = pendingEstablishmentSlug,
                    session = authorizedAccessViewModel.state.value.session,
                    hasStaffModes =
                        hasStaffLaunchModes(
                            authorizedAccessViewModel.state.value.modes
                                .map { it.role },
                        ),
                    hasSavedVenue = studentAuthViewModel.state.value.guestVenue != null,
                    modesLoaded = modesLoaded,
                )
            val route =
                when (destination) {
                    LaunchDestination.Login,
                    LaunchDestination.Discovery,
                    -> Routes.DISCOVERY
                    LaunchDestination.Catalog -> Routes.CATALOG
                    LaunchDestination.StaffModes -> Routes.STAFF_MODES
                }
            navController.navigate(route) {
                popUpTo(Routes.AUTH_LOGIN) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Cartera y Pedidos sin comedor guardado: el acceso no lo resuelve, elegir comedor sí.
    fun navigateChooseVenue() {
        navController.navigate(Routes.DISCOVERY) {
            launchSingleTop = true
            popUpTo(Routes.DISCOVERY) { inclusive = false }
        }
    }

    fun navigateStudentAuth(
        returnRoute: String = Routes.CART,
        // Cartera y Pedidos mandan al acceso solas al abrirse: si quedaran debajo, atrás las volvería a abrir y
        // ellas al acceso otra vez, sin salida. El acceso las reemplaza y atrás regresa a lo de antes.
        replaceReturnScreen: Boolean = false,
    ) {
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
            if (replaceReturnScreen) popUpTo(returnRoute) { inclusive = true }
        }
    }

    // Todo cierre de sesión (salir, sesión vencida, cuenta eliminada) pasa por aquí: lo que viven los
    // ViewModels de la actividad y los objetos globales no sobrevive al usuario que sale.
    fun clearSessionScopedState() {
        orderFlowViewModel.clearForSessionTermination()
        walletViewModel.clearForSessionTermination()
        discoveryViewModel.clearForSessionTermination()
        childrenViewModel.clearForSessionTermination()
        guardianControlsViewModel.clear()
        ChildDeepLinkStore.reset()
        operationalViewModel.clearRole()
        authorizedAccessViewModel.resetAfterSignOut()
        accountDeletionViewModel.cancel()
    }

    fun navigateToSignedOutLanding() {
        navController.navigate(Routes.authLandingRoute(Routes.DISCOVERY)) {
            popUpTo(Routes.DISCOVERY) { inclusive = false }
            launchSingleTop = true
        }
    }

    // La cuenta ya se cerró en el servidor y en el teléfono (borrado de cuenta).
    fun finishAccountSession(noticeMessage: String? = null) {
        clearSessionScopedState()
        studentAuthViewModel.markSessionCleared(noticeMessage)
        navigateToSignedOutLanding()
    }

    fun signOutAndFinish(noticeMessage: String? = null) {
        studentAuthViewModel.signOut {
            clearSessionScopedState()
            if (noticeMessage != null) studentAuthViewModel.markSessionCleared(noticeMessage)
            navigateToSignedOutLanding()
        }
    }

    // Credenciales rechazadas varias veces seguidas: la sesión ya no sirve y reintentar no la arregla.
    val latestSignedIn by rememberUpdatedState(studentAuthState.session != null)
    LaunchedEffect(Unit) {
        authorizedAccessViewModel.sessionExpired.collect {
            if (latestSignedIn) signOutAndFinish("Tu sesión venció. Inicia sesión de nuevo.")
        }
    }

    val authReturnArg =
        navArgument("returnRoute") {
            type = NavType.StringType
            defaultValue = Routes.CART
        }

    // Logo y foto del negocio para las escenas del cliente (carrito, seguimiento, Mis pedidos y la entrada).
    val venueArt = rememberVenueArt(orderState.guestVenue?.establishment)
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
                    // El catálogo también lo necesitan las cards de pedidos
                    // para resolver producto_id → imagen_url en los thumbnails.
                    if (orderState.catalog == null && !orderState.loading) {
                        orderFlowViewModel.refresh()
                    }
                    if (!orderFlowViewModel.requiresAccountAuth()) {
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
                        studentAuthViewModel.isReadyForCheckout()
                    ) {
                        walletViewModel.refreshIfStale(currentVenueId)
                    }
                }

                StudentTab.CART -> {
                    // El saldo decide si "Saldo Vaiinilla" alcanza para pagar:
                    // no debe quedar congelado desde la primera carga.
                    if (studentAuthState.session?.emailVerified == true) {
                        walletViewModel.refreshIfStale(currentVenueId)
                    }
                    // El vacío muestra "Pedidos anteriores": si el usuario entra
                    // a Carrito antes que a Pedidos, las órdenes aún no cargan.
                    if (!orderFlowViewModel.requiresAccountAuth()) {
                        if (operationalState.role != OperationalRole.CLIENT) {
                            operationalViewModel.setRole(OperationalRole.CLIENT)
                        } else if (operationalState.orders.isEmpty() && !operationalState.loading) {
                            operationalViewModel.refresh()
                        }
                    }
                }

                StudentTab.ASSISTANT -> Unit
            }
        },
        catalogDetailOpen = orderState.selectedProductId != null || entrance != null,
        walletCodeOpen = walletCodeOpen,
        overlay = {
            entrance?.let { current ->
                CompositionLocalProvider(
                    LocalScene provides scenePaletteForTheme(),
                    LocalVenueArt provides venueArt,
                ) {
                    key(current.id) {
                        StoreEntranceOverlay(
                            entrance = current,
                            outcome = entranceOutcome,
                            onFinished = { if (entrance?.id == current.id) entrance = null },
                        )
                    }
                }
            }
            discoveryState.pendingSwitch?.let { next ->
                VenueSwitchDialog(
                    onConfirm = {
                        startEntrance(null)
                        discoveryViewModel.confirmPendingSwitch(::enterVenueAndOpenCatalog)
                    },
                    onDismiss = discoveryViewModel::dismissPendingSwitch,
                    sameVenue = orderState.guestVenue?.establishment?.id == next.establishment.id,
                    nextSpaceName = next.space?.name,
                )
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            // Las transiciones de ida quedan instantaneas a proposito; solo el pop
            // necesita movimiento para que el back predictivo tenga algo que
            // previsualizar mientras se arrastra desde el borde.
            popEnterTransition = {
                fadeIn(animationSpec = tween(POP_ANIMATION_MILLIS)) +
                    scaleIn(
                        initialScale = POP_ENTER_INITIAL_SCALE,
                        animationSpec = tween(POP_ANIMATION_MILLIS),
                    )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(POP_ANIMATION_MILLIS),
                ) + fadeOut(animationSpec = tween(POP_ANIMATION_MILLIS))
            },
        ) {
            composable(Routes.SPLASH) {
                LaunchedEffect(Unit) {
                    authorizedAccessViewModel.refreshCurrentSession()
                    studentAuthViewModel.refreshGuestVenue()
                    discoveryViewModel.search("")
                    val savedVenue = studentAuthViewModel.state.value.guestVenue
                    if (savedVenue != null && orderState.guestVenue == null) {
                        orderFlowViewModel.enterGuestVenue(savedVenue)
                    } else if (orderState.guestVenue != null) {
                        orderFlowViewModel.refresh()
                    }
                    val session = authorizedAccessViewModel.state.value.session
                    if (session == null) {
                        navigateLaunchDestination(
                            resolveLaunchDestination(
                                pendingEstablishmentSlug = pendingEstablishmentSlug,
                                session = null,
                                hasStaffModes = false,
                                hasSavedVenue = savedVenue != null,
                            ),
                        )
                    } else {
                        // El callback del ViewModel sobrevive a la actividad; se espera aquí para que, si la
                        // actividad se recrea, este efecto se cancele y navegue el del NavController nuevo (L01).
                        val modesRefreshed = CompletableDeferred<Boolean>()
                        authorizedAccessViewModel.refreshModes(force = true) { modesRefreshed.complete(it) }
                        val modesLoaded = modesRefreshed.await()
                        navigateLaunchDestination(
                            resolveLaunchDestination(
                                pendingEstablishmentSlug = pendingEstablishmentSlug,
                                session = authorizedAccessViewModel.state.value.session,
                                hasStaffModes =
                                    hasStaffLaunchModes(
                                        authorizedAccessViewModel.state.value.modes
                                            .map { it.role },
                                    ),
                                hasSavedVenue = studentAuthViewModel.state.value.guestVenue != null,
                                modesLoaded = modesLoaded,
                            ),
                        )
                    }
                }

                WelcomeBootStage(reduceMotion = rememberReduceMotion())
            }

            composable(Routes.DISCOVERY) {
                // Tus escuelas: las de sus accesos, para volver sin código.
                // Tras cambiar de cuenta o de modo el estado se limpió y no se pasa por SPLASH: se vuelve a buscar.
                LaunchedEffect(studentAuthState.session?.uid, discoveryState.sessionEpoch) {
                    discoveryViewModel.loadMySchools()
                    if (discoveryState.sessionEpoch > 0 && discoveryState.establishments.isEmpty()) {
                        discoveryViewModel.search("")
                    }
                }
                DiscoveryScreen(
                    tableCodeRequest = tableCodeRequest,
                    state = discoveryState,
                    onQueryChange = discoveryViewModel::updateQuery,
                    onSpaceTokenChange = discoveryViewModel::updateSpaceToken,
                    onOpenQrScanner = { qrScannerOpen = true },
                    onSelectEstablishment = { establishment ->
                        startEntrance(null)
                        discoveryViewModel.selectEstablishment(
                            establishment = establishment,
                            onEntered = ::enterVenueAndOpenCatalog,
                        )
                        // Con carrito en otro lugar primero se confirma el cambio; la puerta espera a ese sí.
                        if (discoveryViewModel.state.value.pendingSwitch != null) entrance = null
                    },
                    onResolveSpace = {
                        startEntrance(discoveryState.spaceTokenInput)
                        discoveryViewModel.resolveSpaceToken(onEntered = ::enterVenueAndOpenCatalog)
                    },
                    onContinueSelected = {
                        val selected = discoveryState.selected ?: return@DiscoveryScreen
                        startEntrance(null)
                        enterVenueAndOpenCatalog(selected)
                    },
                    profileInitials = displayInitials(studentAuthState.session?.displayName.orEmpty()),
                    onOpenAccount = { navController.navigate(Routes.WALLET_ACCOUNT) },
                    onBack =
                        if (navController.previousBackStackEntry != null) {
                            { navController.popBackStack() }
                        } else {
                            null
                        },
                    onEnableNearby = discoveryViewModel::enableNearby,
                    onNearbyPermissionDenied = discoveryViewModel::onNearbyPermissionDenied,
                    onDisableNearby = discoveryViewModel::disableNearby,
                    onLoadMore = discoveryViewModel::loadMore,
                    onOpenSchool = { school ->
                        startEntrance(null)
                        discoveryViewModel.openSchool(school, onEntered = ::enterVenueAndOpenCatalog)
                    },
                )

                if (qrScannerOpen) {
                    QrScannerDialog(
                        onClose = { qrScannerOpen = false },
                        alternative =
                            QrScannerAlternative("Escribir código de mesa") {
                                qrScannerOpen = false
                                tableCodeRequest++
                            },
                        onPayload = { rawValue ->
                            qrScannerOpen = false
                            startEntrance(rawValue)
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
                val goBack: () -> Unit = { navController.popBackStack() }
                AuthorizedModeScreen(
                    state = authorizedAccessState,
                    // En las apps de personal Modos es la primera pantalla: no hay a dónde volver.
                    onBack = goBack.takeIf { navController.previousBackStackEntry != null },
                    onSelectMode = { mode ->
                        authorizedAccessViewModel.activateMode(mode) {
                            operationalViewModel.setRole(mode.role)
                            navigateAuthorizedMode(mode.role)
                        }
                    },
                    onSignOut = { signOutAndFinish() },
                    onRetry = { authorizedAccessViewModel.refreshModes(force = true) },
                )
            }

            composable(Routes.CATALOG) {
                // Con sesión el mapa sale del negocio; sin sesión, del mapa público del slug que se está mirando.
                val hasStudentSession = !orderFlowViewModel.requiresStudentAuth()
                val venueSlug = orderState.guestVenue?.establishment?.slug
                // Los sondeos de pantalla se pausan con la app en segundo plano (FLOW-15).
                WhileStarted(
                    hasStudentSession,
                    venueSlug,
                    onStart = { courtsViewModel.onVisible(hasStudentSession, venueSlug) },
                    onStop = courtsViewModel::onHidden,
                )
                CatalogScreen(
                    courts = courtsViewModel.uiState.value.courts,
                    state = orderState,
                    activeOrder = activeOrder,
                    onRetry = orderFlowViewModel::refresh,
                    onSearchChange = orderFlowViewModel::updateSearch,
                    onCategorySelected = orderFlowViewModel::selectCategory,
                    onProductSelected = orderFlowViewModel::openProduct,
                    onDismissProduct = orderFlowViewModel::closeProduct,
                    onToggleOption = orderFlowViewModel::toggleOption,
                    onClearOptionalGroup = orderFlowViewModel::clearOptionalGroup,
                    onSelectedNotesChange = orderFlowViewModel::updateSelectedNotes,
                    onQuantityChange = orderFlowViewModel::changeSelectedQuantity,
                    onAddProduct = orderFlowViewModel::addSelectedProductToCart,
                    onOpenTracking = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                    onOpenWallet = { navController.navigateStudent(Routes.WALLET) },
                    onChangeVenue = {
                        navController.navigate(Routes.DISCOVERY) {
                            launchSingleTop = true
                        }
                    },
                    profileInitials = displayInitials(studentAuthState.session?.displayName.orEmpty()),
                    onOpenAccount = { navController.navigateStudent(Routes.WALLET_ACCOUNT) },
                    onOpenCart = { navController.navigateStudent(Routes.CART) },
                    onOpenFeed = { navController.navigate(Routes.MENU_FEED) { launchSingleTop = true } },
                    waiterLayer =
                        menuWaiterSpaceId(
                            guestSpaceId = orderState.guestVenue?.space?.id,
                            sharedTableSpaceId =
                                sharedTableViewModel.uiState.value.table
                                    ?.spaceId,
                            signedIn = studentAuthState.session != null,
                        )?.let { spaceId ->
                            val qrToken = orderState.guestVenue?.space?.qrToken
                            val alias = waiterCallAlias(studentAuthState.session?.displayName.orEmpty())
                            val layer: @Composable (Boolean, Boolean) -> Unit = { compact, visible ->
                                MenuWaiterLayer(
                                    spaceId = spaceId,
                                    visible = visible,
                                    compact = compact,
                                    onCurrent = { id ->
                                        orderFlowViewModel.ensureClientContext()
                                        operationalViewModel.currentWaiterCall(id)
                                    },
                                    onCall = { id, reason, orderId ->
                                        orderFlowViewModel.ensureClientContext()
                                        operationalViewModel.callWaiter(id, reason, orderId)
                                    },
                                    onCancel = operationalViewModel::cancelWaiterCall,
                                    onJoinTable =
                                        qrToken?.let { token ->
                                            { sharedTableViewModel.joinForWaiterCall(token, alias) }
                                        },
                                )
                            }
                            layer
                        },
                    // Sin cuenta también se renta, salvo en los lugares que piden cuenta: ahí se entra primero.
                    onOpenCourts =
                        if (courtsViewModel.uiState.value.courts
                                .any { it.rentable }
                        ) {
                            {
                                val needsAccount =
                                    !hasStudentSession && orderState.guestVenue?.establishment?.clientIdRequired == true
                                if (needsAccount) {
                                    navController.navigate(Routes.authLandingRoute(Routes.RESERVATIONS))
                                } else {
                                    navController.navigate(Routes.RESERVATIONS) { launchSingleTop = true }
                                }
                            }
                        } else {
                            null
                        },
                )
            }

            composable(Routes.MENU_FEED) {
                MenuFeedScreen(
                    state = orderState,
                    onBack = { navController.popBackStack() },
                    onQuickAdd = orderFlowViewModel::quickAddProduct,
                    onQuickRemove = orderFlowViewModel::quickRemoveProduct,
                    onOpenProduct = orderFlowViewModel::openProduct,
                    onDismissProduct = orderFlowViewModel::closeProduct,
                    onToggleOption = orderFlowViewModel::toggleOption,
                    onClearOptionalGroup = orderFlowViewModel::clearOptionalGroup,
                    onSelectedNotesChange = orderFlowViewModel::updateSelectedNotes,
                    onQuantityChange = orderFlowViewModel::changeSelectedQuantity,
                    onAddSelected = orderFlowViewModel::addSelectedProductToCart,
                    // Atrás desde el carrito regresa al platillo donde ibas.
                    onOpenCart = { navController.navigate(Routes.CART) { launchSingleTop = true } },
                    onConsumeCreateOrderError = orderFlowViewModel::clearCreateOrderError,
                )
            }

            composable(Routes.RESERVATIONS) {
                val reservationsVenueSlug = orderState.guestVenue?.establishment?.slug
                // Con sesión se renta con la cuenta: si en este lugar aún no hay contexto de cliente se prepara
                // primero, en vez de caer en "Renta sin cuenta". Si falla, el error se ve arriba.
                val reservationsSignedIn = studentAuthState.session != null
                var reservationsVenueReady by remember(reservationsSignedIn, reservationsVenueSlug) {
                    mutableStateOf(!orderFlowViewModel.requiresStudentAuth())
                }
                LaunchedEffect(reservationsSignedIn, reservationsVenueSlug) {
                    if (reservationsSignedIn && !reservationsVenueReady) {
                        studentAuthViewModel.ensureVenueContext(
                            onReady = { reservationsVenueReady = true },
                            onNeedsAuth = { navController.navigate(Routes.authLandingRoute(Routes.RESERVATIONS)) },
                            onFailed = reservationsViewModel::showError,
                        )
                    }
                }
                val reservationsHasAccount = reservationsVenueReady
                val preparingAccount = reservationsSignedIn && !reservationsVenueReady
                val reservationsState =
                    reservationsViewModel.uiState.value.let { state ->
                        if (preparingAccount) state.copy(guestEntry = false, loading = state.error == null) else state
                    }
                WhileStarted(
                    reservationsHasAccount,
                    reservationsVenueSlug,
                    preparingAccount,
                    onStart = {
                        if (!preparingAccount) {
                            reservationsViewModel.onVisible(
                                guestSlug = reservationsVenueSlug,
                                hasAccount = reservationsHasAccount,
                            )
                        }
                    },
                    onStop = reservationsViewModel::onHidden,
                )
                ReservationsScreen(
                    state = reservationsState,
                    venueName = orderState.guestVenue?.establishment?.name,
                    onBack = { navController.popBackStack() },
                    onSelectDate = reservationsViewModel::selectDate,
                    onSelectCourt = reservationsViewModel::selectCourt,
                    onRentNow = reservationsViewModel::selectRentNow,
                    onSelectStart = reservationsViewModel::selectStart,
                    onSelectDuration = reservationsViewModel::selectDuration,
                    onReserve = reservationsViewModel::reserve,
                    onPay = reservationsViewModel::pay,
                    onDismissPayment = reservationsViewModel::dismissPayment,
                    onResumePayment = reservationsViewModel::resumePayment,
                    onCancel = reservationsViewModel::cancel,
                    onDismissMessage = reservationsViewModel::dismissMessages,
                    cardAvailable = orderState.canPayWithCard,
                    onGuestNameChange = reservationsViewModel::onGuestNameChange,
                    onEnterAsGuest = reservationsViewModel::enterAsGuest,
                    onSignIn =
                        if (reservationsHasAccount) {
                            null
                        } else {
                            { navController.navigate(Routes.authLandingRoute(Routes.RESERVATIONS)) }
                        },
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
                var venueContextReady by remember { mutableStateOf(false) }
                LaunchedEffect(
                    studentAuthState.session?.uid,
                    orderState.guestVenue?.establishment?.id,
                ) {
                    venueContextReady = false
                    studentAuthViewModel.ensureVenueContext(
                        onReady = {
                            venueContextReady = true
                            // Con el contexto ya de esta escuela: si es escuela, Mis hijos (F19).
                            orderFlowViewModel.refreshOperationalStatusForWallet()
                        },
                        onNeedsAuth = { navigateStudentAuth(Routes.WALLET, replaceReturnScreen = true) },
                        onNeedsVenue = ::navigateChooseVenue,
                        // Sin contexto no hay saldo: se consulta igual para que el error salga con "Reintentar".
                        onFailed = { walletViewModel.refresh() },
                    )
                }
                // Con Cartera visible el saldo se mueve por recargas en Caja: sondeo ligero, solo con el
                // contexto de esta escuela listo y mientras la app está a la vista.
                LaunchedEffect(venueContextReady) {
                    if (!venueContextReady) return@LaunchedEffect
                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        while (true) {
                            walletViewModel.refresh()
                            delay(WALLET_VISIBLE_REFRESH_MS)
                        }
                    }
                }
                // Padres e hijos: solo una escuela muestra Mis hijos y lo de tutores y alumnos, y para saberlo hace
                // falta el estado operativo con el contexto de cliente.
                LaunchedEffect(orderState.guestVenue?.establishment?.id) {
                    orderFlowViewModel.refreshOperationalStatusForWallet()
                }
                ParentModeWalletHost(
                    enabled = orderState.operationalStatus?.isSchool == true,
                    childrenViewModel = childrenViewModel,
                    hasTutors = guardianControlsViewModel.controls?.tutorNames?.isNotEmpty() == true,
                    controlsLoaded = guardianControlsViewModel.hasLoaded,
                    onFullScreenChange = { walletCodeOpen = it },
                    onAccountClaimed = {
                        val renewed = CompletableDeferred<Unit>()
                        studentAuthViewModel.completeEnrollment(
                            onSuccess = { renewed.complete(Unit) },
                            onNeedsVerify = { renewed.complete(Unit) },
                            onNeedsContextualId = { renewed.complete(Unit) },
                            onFailed = { renewed.complete(Unit) },
                        )
                        renewed.await()
                        walletViewModel.refresh()
                    },
                ) { familySection ->
                    WalletScreen(
                        remoteState = walletForVenue,
                        userId = walletForVenue.data?.wallet?.userId,
                        venueName = orderState.guestVenue?.establishment?.name,
                        canRecharge = orderState.canPayWithBalance,
                        onRetry = walletViewModel::refresh,
                        onMenu = { navController.navigateStudent(Routes.CATALOG) },
                        onCodeOpenChange = { walletCodeOpen = it },
                        caretakerLine = guardianControlsViewModel.controls?.caretakerLine,
                        caretakerDetail = guardianControlsViewModel.controls?.walletDetail,
                        familySection = familySection,
                    )
                }
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
                    // El uid de Firebase no es el id de la cartera: Caja no lo encuentra. Sin cartera no hay código.
                    userId = walletForVenue.data?.wallet?.userId,
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
                    onSignOut = { signOutAndFinish() },
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
                    onAccountDeletionMfaCodeChange = accountDeletionViewModel::updateMfaCode,
                    onAccountDeletionMfaFactorSelected = accountDeletionViewModel::selectMfaFactor,
                    onSubmitAccountDeletionMfa = {
                        accountDeletionViewModel.submitMfaCode(
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
                        navController.navigate(Routes.authLandingRoute(Routes.DISCOVERY)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenSupport = {
                        navController.navigate(Routes.SUPPORT) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.SUPPORT) {
                val lastOrder =
                    operationalState.orders.firstOrNull()?.takeIf {
                        operationalState.role ==
                            OperationalRole.CLIENT
                    }
                SupportScreen(
                    onBack = { navController.popBackStack() },
                    termsUrl = studentAuthState.termsUrl,
                    privacyUrl = studentAuthState.privacyUrl,
                    lastOrder =
                        lastOrder?.let {
                            SupportLastOrder(
                                title = "Pedido #${it.summary.folio}",
                                detail = "${it.summary.state.label} · ${it.summary.destination.label}",
                            )
                        },
                    onOpenLastOrder = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                )
            }

            composable(Routes.CART) {
                val guestAuthRequired = orderFlowViewModel.requiresAccountAuth()
                LaunchedEffect(Unit) { orderFlowViewModel.refreshOperationalStatusForCheckout() }
                CompositionLocalProvider(LocalVenueArt provides venueArt) {
                    CartScreen(
                        state = orderState,
                        guestCheckout = orderFlowViewModel.isGuestCheckout(),
                        onGuestNameChange = orderFlowViewModel::updateGuestName,
                        onMenu = { navController.navigateStudent(Routes.CATALOG) },
                        onQuantityChange = orderFlowViewModel::changeCartLineQuantity,
                        onNotesChange = orderFlowViewModel::updateKitchenNotes,
                        onDestinationChange = orderFlowViewModel::updateCheckoutDestination,
                        onSpaceChange = orderFlowViewModel::updateCheckoutSpace,
                        onPaymentChange = orderFlowViewModel::updateCheckoutPayment,
                        onPayAtEnd = orderFlowViewModel::selectPayAtEnd,
                        onConfirm = {
                            if (guestAuthRequired) {
                                studentAuthViewModel.ensureVenueContext(
                                    onReady = orderFlowViewModel::submitOrder,
                                    onNeedsAuth = { navigateStudentAuth(Routes.CART) },
                                    onFailed = orderFlowViewModel::showCreateOrderError,
                                )
                            } else {
                                orderFlowViewModel.submitOrder()
                            }
                        },
                        onResolvePendingStripePayment = {
                            orderFlowViewModel.resolvePendingStripePayment {
                                navController.navigate(Routes.CONFIRMATION) { launchSingleTop = true }
                            }
                        },
                        walletBalance = walletForVenue.data?.wallet?.visibleBalance,
                        guestAuthRequired = guestAuthRequired,
                        profileInitials = displayInitials(studentAuthState.session?.displayName.orEmpty()),
                        onOpenAccount = { navController.navigateStudent(Routes.WALLET_ACCOUNT) },
                        isRefreshing = orderState.refreshing,
                        onRefresh = orderFlowViewModel::refreshOperationalStatus,
                        recentOrders =
                            operationalState.orders
                                .filter {
                                    it.summary.state == OrderState.DELIVERED ||
                                        it.summary.state.isTerminalWithoutDelivery
                                }.take(3),
                        onSelectOrder = { orderId ->
                            operationalViewModel.selectOrder(orderId)
                            navController.navigateStudent(Routes.STUDENT_TRACKING)
                        },
                        preparingCheckout = guestAuthRequired && studentAuthState.loading,
                        onCheckoutOpened = orderFlowViewModel::refreshOperationalStatusForCheckout,
                    )
                }
            }

            composable(
                route = Routes.AUTH_LANDING,
                arguments = listOf(authReturnArg),
            ) { entry ->
                val returnRoute = entry.arguments?.getString("returnRoute") ?: Routes.CART
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val webClientId = stringResource(R.string.default_web_client_id)
                StudentAuthLandingScreen(
                    state = studentAuthState,
                    onBack =
                        if (navController.previousBackStackEntry != null) {
                            { navController.popBackStack() }
                        } else {
                            null
                        },
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
                    onGoogleSignIn = {
                        coroutineScope.launch {
                            val idToken =
                                try {
                                    GoogleSignInHelper.requestIdToken(
                                        context = context,
                                        webClientId = webClientId,
                                    )
                                } catch (error: GetCredentialException) {
                                    null
                                } catch (error: GoogleSignInUnavailableException) {
                                    null
                                }
                            if (idToken == null) {
                                studentAuthViewModel.reportError(
                                    "No se pudo iniciar sesión con Google. Inténtalo de nuevo.",
                                )
                                return@launch
                            }
                            studentAuthViewModel.loginWithGoogle(idToken) { authenticated ->
                                if (authenticated) {
                                    finishStudentAuth(returnRoute)
                                } else {
                                    navController.navigate(Routes.authVerifyRoute(returnRoute)) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    },
                    onExplore = {
                        navController.navigate(Routes.DISCOVERY) {
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
                    onMfaCodeChange = studentAuthViewModel::updateMfaCode,
                    onMfaFactorSelected = studentAuthViewModel::selectMfaFactor,
                    onSubmitMfa = studentAuthViewModel::submitMfaCode,
                    onCancelMfa = studentAuthViewModel::cancelMfa,
                    existingVerifiedSession = existingVerifiedSession,
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
                DisposableEffect(Unit) { onDispose { studentAuthViewModel.resetPasswordRecovery() } }
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
                // El pedido recién creado es una foto; el seleccionado se refresca con el polling y trae el estado
                // actual (preparando, listo…). Si es el mismo pedido, gana el fresco sin perder el código local.
                val refreshedCreated =
                    orderState.createdOrder?.let { created ->
                        selectedOrder
                            ?.takeIf { it.summary.id == created.summary.id }
                            ?.let { fresh -> fresh.copy(pickupToken = fresh.pickupToken ?: created.pickupToken) }
                            ?: created
                    }
                val confirmationOrder =
                    refreshedCreated
                        ?: orderState.stripeObservedOrder?.takeIf {
                            selectedOrder == null || selectedOrder.summary.id == it.summary.id
                        }
                        ?: selectedOrder
                        ?: orderState.stripeObservedOrder
                val context = LocalContext.current
                LaunchedEffect(confirmationOrder?.summary?.id) {
                    val order = confirmationOrder ?: return@LaunchedEffect
                    // Un pedido viejo abierto desde Pedidos ya cobrado no debe reabrir el flujo de Stripe.
                    val stripeStillOpen = isFreshConfirmation || !order.isStripePaymentConfirmedByBackend()
                    if (order.summary.paymentMethod == PaymentMethod.STRIPE && stripeStillOpen) {
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
                CompositionLocalProvider(LocalVenueArt provides venueArt) {
                    OrderConfirmationScreen(
                        stripePaymentPhase = orderState.stripePaymentPhase,
                        stripePaymentMessage = orderState.stripePaymentMessage,
                        retryingStripePayment = orderState.retryingStripePayment,
                        onRetryStripePayment = orderFlowViewModel::retryStripePayment,
                        onRefreshStripePayment = orderFlowViewModel::refreshStripePaymentStatus,
                        onReturnStripeToCart = { order ->
                            orderFlowViewModel.returnFailedStripeOrderToCart(order) {
                                val returnedToExistingCart =
                                    navController.popBackStack(Routes.CART, inclusive = false)
                                if (!returnedToExistingCart) {
                                    navController.navigateStudent(Routes.CART)
                                }
                            }
                        },
                        abandoningStripePayment = orderState.resolvingPendingStripePayment,
                        onAbandonStripePayment = { order ->
                            orderFlowViewModel.abandonStripePaymentToCart(order) {
                                val returnedToExistingCart =
                                    navController.popBackStack(Routes.CART, inclusive = false)
                                if (!returnedToExistingCart) {
                                    navController.navigateStudent(Routes.CART)
                                }
                            }
                        },
                        purchaseCelebration = orderState.purchaseCelebration,
                        onPurchaseCelebrationFinished = orderFlowViewModel::completePurchaseCelebration,
                        order = confirmationOrder,
                        deliveryRequiresQr = orderState.operationalStatus?.deliveryRequiresQr != false,
                        showTrackingButton = isFreshConfirmation,
                        // Como en el menú: la llamada va con el contexto de este negocio, no el del anterior (K02).
                        onCurrentWaiterCall = { id ->
                            orderFlowViewModel.ensureClientContext()
                            operationalViewModel.currentWaiterCall(id)
                        },
                        onCallWaiter = { id, reason, orderId ->
                            orderFlowViewModel.ensureClientContext()
                            operationalViewModel.callWaiter(id, reason, orderId)
                        },
                        onCancelWaiter = operationalViewModel::cancelWaiterCall,
                        onViewReceipt = { navController.navigate(Routes.receiptStickerRoute()) },
                        guest = studentAuthState.session == null,
                        onReturnToMenu = {
                            // Abierto desde Pedidos: atrás regresa a la lista. La selección se limpia allá,
                            // cuando esta pantalla ya salió, para no quedarse sin pedido a media animación.
                            if (!isFreshConfirmation &&
                                navController.popBackStack(Routes.STUDENT_TRACKING, inclusive = false)
                            ) {
                                return@OrderConfirmationScreen
                            }
                            if (isFreshConfirmation) {
                                orderFlowViewModel.clearCreatedOrder()
                            }
                            operationalViewModel.selectOrder(null)
                            navController.navigate(Routes.CATALOG) {
                                popUpTo(Routes.CATALOG) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onViewTracking = {
                            operationalViewModel.setRole(OperationalRole.CLIENT)
                            operationalViewModel.selectOrder(null)
                            val returnedToExistingTracking =
                                navController.popBackStack(Routes.STUDENT_TRACKING, inclusive = false)
                            if (!returnedToExistingTracking) {
                                navController.navigateStudent(Routes.STUDENT_TRACKING)
                            }
                        },
                    )
                }
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
                val receiptOrder =
                    orderState.createdOrder ?: operationalState.selectedOrder ?: orderState.stripeObservedOrder
                if (receiptOrder == null) {
                    // El pedido se descartó con el recibo abierto: sin pedido no hay recibo que dibujar.
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    ReceiptStickerScreen(
                        order = receiptOrder,
                        onBack = { navController.popBackStack() },
                        initialStyleIndex = styleIndex,
                    )
                }
            }

            composable(Routes.STUDENT_TRACKING) {
                val venueAuthRequired = orderFlowViewModel.requiresAccountAuth()
                LaunchedEffect(venueAuthRequired, studentAuthState.session?.uid) {
                    if (venueAuthRequired) {
                        studentAuthViewModel.ensureVenueContext(
                            onReady = {
                                if (operationalState.role != OperationalRole.CLIENT) {
                                    operationalViewModel.setRole(OperationalRole.CLIENT)
                                }
                            },
                            onNeedsAuth = { navigateStudentAuth(Routes.STUDENT_TRACKING, replaceReturnScreen = true) },
                            onNeedsVenue = ::navigateChooseVenue,
                        )
                    } else if (operationalState.role != OperationalRole.CLIENT) {
                        operationalViewModel.setRole(OperationalRole.CLIENT)
                    }
                }
                // Los pedidos se abren en la pantalla de recogida (Vainiverso); solo las rentas
                // conservan el detalle dentro de la lista. Al volver se suelta la selección.
                LaunchedEffect(Unit) {
                    if (operationalState.selectedOrder?.isRental == false) {
                        delay(POP_ANIMATION_MILLIS.toLong() + 50)
                        operationalViewModel.selectOrder(null)
                    }
                }
                if (!venueAuthRequired) {
                    CompositionLocalProvider(LocalVenueArt provides venueArt) {
                        StudentTrackingScreen(
                            state =
                                operationalState.takeIf { it.selectedOrder?.isRental != false }
                                    ?: operationalState.copy(selectedOrderId = null),
                            orderState = orderState,
                            onMenu = { navController.navigateStudent(Routes.CATALOG) },
                            onAssistant = {},
                            onWallet = { navController.navigateStudent(Routes.WALLET) },
                            onCart = { navController.navigateStudent(Routes.CART) },
                            onOpenCatalog = { navController.navigateStudent(Routes.CATALOG) },
                            onSelectOrder = { id ->
                                operationalViewModel.selectOrder(id)
                                val rental =
                                    operationalState.orders.firstOrNull { it.summary.id == id }?.isRental == true
                                if (!rental) {
                                    orderFlowViewModel.clearCreatedOrder()
                                    navController.navigate(Routes.CONFIRMATION) { launchSingleTop = true }
                                }
                            },
                            onBackFromSelectedOrder = { operationalViewModel.selectOrder(null) },
                            onDeleteOrder = ::dismissClientOrder,
                            onViewReceipt = {
                                orderFlowViewModel.clearCreatedOrder()
                                navController.navigate(Routes.CONFIRMATION) { launchSingleTop = true }
                            },
                            onRefresh = { operationalViewModel.refresh() },
                            canCallWaiter = ::canCallWaiter,
                            onCurrentWaiterCall = { id ->
                                orderFlowViewModel.ensureClientContext()
                                operationalViewModel.currentWaiterCall(id)
                            },
                            onAnnounceArrival = operationalViewModel::announceArrival,
                            onCallWaiter = { id, reason, orderId ->
                                orderFlowViewModel.ensureClientContext()
                                operationalViewModel.callWaiter(id, reason, orderId)
                            },
                            onCancelWaiter = operationalViewModel::cancelWaiterCall,
                            sharedTable = {
                                // Solo consulta la mesa mientras Mis pedidos está a la vista.
                                WhileStarted(
                                    onStart = sharedTableViewModel::onVisible,
                                    onStop = sharedTableViewModel::onHidden,
                                )
                                SharedTableCard(
                                    state = sharedTableViewModel.uiState.value,
                                    qrToken = orderState.guestVenue?.space?.qrToken,
                                    onAliasChange = sharedTableViewModel::updateAlias,
                                    onJoin = {
                                        orderState.guestVenue
                                            ?.space
                                            ?.qrToken
                                            ?.let(sharedTableViewModel::join)
                                    },
                                    onClaim = sharedTableViewModel::claim,
                                    onLeave = sharedTableViewModel::leave,
                                    spaceType = orderState.guestVenue?.space?.type,
                                )
                            },
                        )
                    }
                } else {
                    OrdersAccessGate(
                        loading = studentAuthState.loading,
                        errorMessage = studentAuthState.errorMessage,
                        onRetry = {
                            studentAuthViewModel.ensureVenueContext(
                                onReady = { operationalViewModel.setRole(OperationalRole.CLIENT) },
                                onNeedsAuth = { navigateStudentAuth(Routes.STUDENT_TRACKING) },
                            )
                        },
                        onSignIn = { navigateStudentAuth(Routes.STUDENT_TRACKING) },
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
                    val studentCounterState by studentCounterViewModel.uiState
                    val studentCounterOffline by studentCounterViewModel.offlineState.collectAsStateWithLifecycle()
                    var studentsSheetOpen by rememberSaveable { mutableStateOf(false) }
                    // Una sola lectura del tablero para el contador de mesas; el sondeo es del tablero.
                    LaunchedEffect(Unit) { waiterViewModel.refresh() }
                    WhileStarted(
                        onStart = pendingRefundsViewModel::onVisible,
                        onStop = pendingRefundsViewModel::onHidden,
                    )
                    CashierOperationalScreen(
                        state = operationalState,
                        onBack = returnToModes(navController, operationalViewModel),
                        onOpenCashSession = operationalViewModel::openCashRegister,
                        onRequestCloseCash = operationalViewModel::requestCloseCash,
                        onDismissCloseCash = operationalViewModel::dismissCloseCash,
                        onCloseCash = operationalViewModel::closeCashRegister,
                        onCollect = operationalViewModel::collectCash,
                        onSearchWalletClients = operationalViewModel::searchWalletClients,
                        onOpenWalletUserQr = { walletUserQrOpen = true },
                        onReloadWallet = operationalViewModel::reloadWallet,
                        onConfirmPendingReload = operationalViewModel::confirmPendingWalletReload,
                        onDiscardPendingReload = operationalViewModel::discardPendingWalletReload,
                        deliveryRequiresQr = waiterState.deliveryRequiresQr,
                        onCashChangeNoticed = operationalViewModel::consumeCashChangeNotice,
                        onScanDeliver = { orderId, version ->
                            pendingPickupDelivery = PendingPickupDelivery(orderId, version)
                        },
                        onDeliver = { orderId, version -> operationalViewModel.deliver(orderId, version) },
                        onRejectOrder = operationalViewModel::rejectOrder,
                        onRejectItem = operationalViewModel::rejectItem,
                        onActionNoticed = operationalViewModel::consumeActionNotice,
                        onChangeMode =
                            if (authorizedAccessState.hasMultipleModes) {
                                returnToModes(navController, operationalViewModel)
                            } else {
                                null
                            },
                        onOpenAccounts = { navController.navigate(Routes.CASHIER_ACCOUNTS) { launchSingleTop = true } },
                        restrictedMode = authorizedAccessState.activeContext?.restrictedMode,
                        onToggleProductAvailable = operationalViewModel::setProductAvailable,
                        onCreateCashierProduct = { draft, bytes, filename, mime, onSuccess ->
                            operationalViewModel.createCashierProduct(
                                draft = draft,
                                imageBytes = bytes,
                                imageFilename = filename,
                                imageMime = mime,
                                onSuccess = onSuccess,
                            )
                        },
                        onUploadCashierProductImage = operationalViewModel::uploadCashierProductImage,
                        onUploadStoreImage = operationalViewModel::uploadStoreImage,
                        onRemoveStoreImage = operationalViewModel::removeStoreImage,
                        assistantUserKey =
                            authorizedAccessState.activeContext?.membershipId ?: "cashier",
                        onNewCounterOrder = waiterViewModel::openCounterOrder,
                        orderTables = waiterState.tables.map { it.space }.filter { it.type != "cancha" },
                        onNewTableOrder = waiterViewModel::openTableOrder,
                        orderTablesLoading = waiterState.loading,
                        orderTablesError = waiterState.errorMessage,
                        onRefreshOrderTables = waiterViewModel::refresh,
                        // Con el tablero sin actualizar no se afirma "Nada por cobrar".
                        pendingSpaces =
                            waiterState.tables
                                .takeIf { it.isNotEmpty() && waiterState.consecutiveBoardFailures == 0 }
                                ?.count { it.hasPendingAccount() },
                        studentsEntry =
                            studentCounterState.policy?.takeIf { it.isSchool }?.let {
                                {
                                    CashierStudentsEntry(
                                        offline = studentCounterOffline,
                                        colors = rememberOperationalColors(),
                                        onClick = { studentsSheetOpen = true },
                                    )
                                }
                            },
                        placeName = authorizedAccessState.activeContext?.establishmentName.orEmpty(),
                        refundsEntry = {
                            CashierRefundsSection(
                                state = pendingRefundsViewModel.uiState.value,
                                canConfirm =
                                    authorizedAccessState.activeContext?.restrictedMode != RestrictedMode.READ_ONLY,
                                cashOpen = operationalState.cashSessionOpen,
                                colors = rememberOperationalColors(),
                                onConfirm = pendingRefundsViewModel::confirm,
                            )
                        },
                        notice = waiterState.toastMessage,
                        onDismissError = operationalViewModel::dismissMutationError,
                        onRefresh = operationalViewModel::refresh,
                    )
                    if (studentsSheetOpen) {
                        StudentCounterSheet(
                            state = studentCounterState,
                            offline = studentCounterOffline,
                            catalog = operationalState.catalog,
                            canCharge =
                                operationalState.cashSessionOpen == true &&
                                    authorizedAccessState.activeContext?.restrictedMode != RestrictedMode.READ_ONLY,
                            actions = remember(studentCounterViewModel) { studentCounterViewModel.actions() },
                            onDismiss = {
                                studentsSheetOpen = false
                                studentCounterViewModel.closeStudent()
                            },
                        )
                    }
                    if (waiterState.counterOrderOpen) {
                        CounterOrderSheet(
                            state = waiterState,
                            colors = rememberOperationalColors(),
                            onDismiss = waiterViewModel::closeCounterOrder,
                            onSubmit = { lines, name ->
                                waiterViewModel.submitCounterOrder(
                                    lines,
                                    name,
                                    CashierContext(cashOpen = operationalState.cashSessionOpen),
                                )
                            },
                            submitAction =
                                if (operationalState.cashSessionOpen == true) "Dejar por cobrar" else "Crear pedido",
                            onSubmitAndCollect =
                                if (operationalState.cashSessionOpen == true) {
                                    { lines, name ->
                                        waiterViewModel.submitCounterOrder(
                                            lines,
                                            name,
                                            CashierContext(cashOpen = true),
                                            onCreated = operationalViewModel::refreshOrder,
                                        )
                                    }
                                } else {
                                    null
                                },
                        )
                    }
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
                    val reservedState by reservedOrdersViewModel.uiState
                    var reservedSheetOpen by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(authorizedAccessState.activeContext?.establishmentId) {
                        reservedOrdersViewModel.refreshPolicy()
                    }
                    KitchenOperationalScreen(
                        state = operationalState,
                        onBack = returnToModes(navController, operationalViewModel),
                        onStart = operationalViewModel::startKitchen,
                        onReady = operationalViewModel::markReady,
                        onReject = operationalViewModel::rejectOrder,
                        onRejectItem = operationalViewModel::rejectItem,
                        onChangeMode =
                            if (authorizedAccessState.hasMultipleModes) {
                                returnToModes(navController, operationalViewModel)
                            } else {
                                null
                            },
                        restrictedMode = authorizedAccessState.activeContext?.restrictedMode,
                        assistantUserKey =
                            authorizedAccessState.activeContext?.membershipId ?: "kitchen",
                        reservedEntry =
                            if (reservedState.isSchool) {
                                {
                                    KitchenReservedEntry(colors = rememberOperationalColors(), onClick = {
                                        reservedOrdersViewModel.open()
                                        reservedSheetOpen = true
                                    })
                                }
                            } else {
                                null
                            },
                        onActionNoticed = operationalViewModel::consumeActionNotice,
                        onDismissError = operationalViewModel::dismissMutationError,
                        onRefresh = operationalViewModel::refresh,
                    )
                    if (reservedSheetOpen) {
                        ReservedOrdersSheet(
                            state = reservedState,
                            colors = rememberOperationalColors(),
                            onSelect = reservedOrdersViewModel::select,
                            onRetry = reservedOrdersViewModel::retry,
                            onDismiss = { reservedSheetOpen = false },
                        )
                    }
                }
            }

            composable(Routes.ADMIN) {
                val active = authorizedAccessState.activeContext
                val authorizedAdmin = active?.role == OperationalRole.ADMIN
                val scope = active?.membershipId.orEmpty()
                LaunchedEffect(authorizedAdmin, scope) {
                    if (!authorizedAdmin) {
                        returnToModes(navController, operationalViewModel)()
                    } else {
                        adminViewModel.onVisible(scope)
                        // Para el menú: si el negocio renta canchas y si la tarjeta suma comisión.
                        adminStoreViewModel.onVisible(scope)
                        adminSpacesViewModel.onVisible(scope)
                    }
                }
                if (authorizedAdmin) {
                    // Sin OperationalPresenceLifecycle: la administración no manda latidos ni aparece como en turno.
                    val adminState by adminViewModel.uiState
                    val storeState by adminStoreViewModel.uiState
                    val spacesState by adminSpacesViewModel.uiState
                    val stripe = storeState.stripe
                    AdminScreen(
                        state = adminState,
                        placeName = active?.establishmentName.orEmpty(),
                        restrictedMode = active?.restrictedMode,
                        actions =
                            AdminActions(
                                onRefreshHome = adminViewModel::refreshHome,
                                onRefreshCatalog = adminViewModel::refreshCatalog,
                                onSelectPeriod = adminViewModel::selectPeriod,
                                onSelectCustomPeriod = adminViewModel::selectCustomPeriod,
                                onCloseWithoutCharging = adminViewModel::closeWithoutCharging,
                                onSaveCategory = adminViewModel::saveCategory,
                                onSaveProduct = adminViewModel::saveProduct,
                                onSetAvailable = adminViewModel::setAvailable,
                                onRemoveImage = adminViewModel::removeImage,
                                onSelectOrderFilter = adminViewModel::selectOrderFilter,
                                onRefreshOrders = adminViewModel::refreshOrders,
                                onLoadMoreOrders = adminViewModel::loadMoreOrders,
                            ),
                        sections =
                            AdminSectionsHost(
                                staff = adminStaffViewModel,
                                store = adminStoreViewModel,
                                spaces = adminSpacesViewModel,
                                cash =
                                    AdminCashActions(
                                        onOpenCash = adminViewModel::openCash,
                                        onCloseCash = adminViewModel::closeCash,
                                    ),
                                // Pedidos → Mesas: el tablero del mesero para pedir en la mesa donde se esté. Sondea
                                // solo mientras se ve.
                                tablesBoard = {
                                    DisposableEffect(Unit) {
                                        waiterViewModel.onVisible()
                                        onDispose { waiterViewModel.onHidden() }
                                    }
                                    WaiterOperationalScreen(
                                        state = waiterState,
                                        onBack = {},
                                        onRefresh = waiterViewModel::refresh,
                                        onGoing = waiterViewModel::markGoing,
                                        onAttended = waiterViewModel::markAttended,
                                        onOpenSpace = waiterViewModel::openSpace,
                                        onCloseSpace = waiterViewModel::closeSpace,
                                        onOpenTurn = waiterViewModel::openTurn,
                                        onStartRental = waiterViewModel::startRental,
                                        onConfirmRental = waiterViewModel::confirmRental,
                                        onCancelRental = waiterViewModel::cancelPendingRental,
                                        onExtendTurn = waiterViewModel::extendTurn,
                                        onReleaseSpace = waiterViewModel::releaseSpace,
                                        onDismissCollection = waiterViewModel::dismissCollection,
                                        onFilterAttending = waiterViewModel::setFilterAttending,
                                        onOpenCounterOrder = waiterViewModel::openCounterOrder,
                                        onOpenTableOrder = waiterViewModel::openTableOrder,
                                        onCloseCounterOrder = waiterViewModel::closeCounterOrder,
                                        onSubmitCounterOrder = waiterViewModel::submitCounterOrder,
                                        restrictedMode = active?.restrictedMode,
                                        placeName = active?.establishmentName.orEmpty(),
                                        embedded = true,
                                        canChargeAndDeliver = false,
                                    )
                                },
                                school = adminSchoolViewModel,
                                // Pedir: el pedido de mostrador o de mesa del mesero, con el estilo del menú. Sondea
                                // las mesas solo mientras se ve.
                                orderTaker = { header ->
                                    DisposableEffect(Unit) {
                                        waiterViewModel.onVisible()
                                        onDispose {
                                            waiterViewModel.closeCounterOrder()
                                            waiterViewModel.onHidden()
                                        }
                                    }
                                    AdminTakeOrderTab(
                                        state = waiterState,
                                        colors = rememberOperationalColors(),
                                        header = header,
                                        onPickCounter = waiterViewModel::openCounterOrder,
                                        onPickTable = waiterViewModel::openTableOrder,
                                        onSubmit = { lines, name -> waiterViewModel.submitCounterOrder(lines, name) },
                                    )
                                },
                            ),
                        scope = scope,
                        rentsSpaces = spacesState.spaces.orEmpty().any { it.hourlyPrice != null },
                        cardFeeToCustomer =
                            stripe != null &&
                                stripe.enabled &&
                                stripe.chargesEnabled &&
                                stripe.payoutsEnabled &&
                                stripe.passFeeToCustomer,
                        isSchool = storeState.config?.isSchool == true,
                        photoUrl = storeState.images.photoUrl,
                        logoUrl = storeState.images.logoUrl,
                        onChangeMode = returnToModes(navController, operationalViewModel),
                    )
                }
            }

            composable(Routes.CASHIER_ACCOUNTS) {
                val authorizedCashier = authorizedAccessState.activeContext?.role == OperationalRole.CASHIER
                LaunchedEffect(authorizedCashier) {
                    if (!authorizedCashier) {
                        returnToModes(navController, operationalViewModel)()
                    }
                }
                if (authorizedCashier) {
                    OperationalPresenceLifecycle(OperationalRole.CASHIER, operationalViewModel)
                    DisposableEffect(Unit) {
                        waiterViewModel.onVisible()
                        onDispose { waiterViewModel.onHidden() }
                    }
                    val backToCashier: () -> Unit = { navController.popBackStack() }
                    WaiterOperationalScreen(
                        state = waiterState,
                        onBack = backToCashier,
                        onRefresh = waiterViewModel::refresh,
                        onGoing = waiterViewModel::markGoing,
                        onAttended = waiterViewModel::markAttended,
                        onDeliver = { order, qrToken -> waiterViewModel.deliver(order, qrToken) },
                        onOpenSpace = waiterViewModel::openSpace,
                        onCloseSpace = waiterViewModel::closeSpace,
                        onOpenTurn = waiterViewModel::openTurn,
                        onStartRental = waiterViewModel::startRental,
                        onConfirmRental = waiterViewModel::confirmRental,
                        onCancelRental = waiterViewModel::cancelPendingRental,
                        onExtendTurn = waiterViewModel::extendTurn,
                        onReleaseSpace = waiterViewModel::releaseSpace,
                        onCollectAccount = waiterViewModel::collectAccount,
                        onAbono = waiterViewModel::abonar,
                        onDismissCollection = waiterViewModel::dismissCollection,
                        onFilterAttending = waiterViewModel::setFilterAttending,
                        onOpenCounterOrder = waiterViewModel::openCounterOrder,
                        onOpenTableOrder = waiterViewModel::openTableOrder,
                        onCloseCounterOrder = waiterViewModel::closeCounterOrder,
                        onSubmitCounterOrder = { lines, name ->
                            // Desde Caja el pedido no se "envía a Caja": se dice si se puede cobrar ya (CAJA-21).
                            waiterViewModel.submitCounterOrder(
                                lines,
                                name,
                                CashierContext(cashOpen = operationalState.cashSessionOpen),
                            )
                        },
                        onChangeMode = backToCashier,
                        restrictedMode = authorizedAccessState.activeContext?.restrictedMode,
                        placeName = authorizedAccessState.activeContext?.establishmentName.orEmpty(),
                        assistantUserKey =
                            authorizedAccessState.activeContext?.membershipId ?: "cashier-accounts",
                        kicker = "CAJA",
                        roleSubtitle = "Cuenta de caja",
                        roleChip = "CJ",
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
                    DisposableEffect(Unit) {
                        waiterViewModel.onVisible()
                        onDispose { waiterViewModel.onHidden() }
                    }
                    var alertCall by remember { mutableStateOf<TableCall?>(null) }
                    LaunchedEffect(Unit) {
                        waiterViewModel.newCallEvents.collect { call -> alertCall = call }
                    }
                    WaiterOperationalScreen(
                        state = waiterState,
                        onBack = returnToModes(navController, operationalViewModel),
                        onRefresh = waiterViewModel::refresh,
                        onGoing = waiterViewModel::markGoing,
                        onAttended = waiterViewModel::markAttended,
                        onDeliver = { order, qrToken -> waiterViewModel.deliver(order, qrToken) },
                        onOpenSpace = waiterViewModel::openSpace,
                        onCloseSpace = waiterViewModel::closeSpace,
                        onOpenTurn = waiterViewModel::openTurn,
                        onStartRental = waiterViewModel::startRental,
                        onConfirmRental = waiterViewModel::confirmRental,
                        onCancelRental = waiterViewModel::cancelPendingRental,
                        onExtendTurn = waiterViewModel::extendTurn,
                        onReleaseSpace = waiterViewModel::releaseSpace,
                        onCollectAccount = waiterViewModel::collectAccount,
                        onAbono = waiterViewModel::abonar,
                        onDismissCollection = waiterViewModel::dismissCollection,
                        onFilterAttending = waiterViewModel::setFilterAttending,
                        onOpenCounterOrder = waiterViewModel::openCounterOrder,
                        onOpenTableOrder = waiterViewModel::openTableOrder,
                        onCloseCounterOrder = waiterViewModel::closeCounterOrder,
                        onSubmitCounterOrder = waiterViewModel::submitCounterOrder,
                        newCallTable = alertCall,
                        onAlertConsumed = { alertCall = null },
                        onChangeMode =
                            if (authorizedAccessState.hasMultipleModes) {
                                returnToModes(navController, operationalViewModel)
                            } else {
                                null
                            },
                        restrictedMode = authorizedAccessState.activeContext?.restrictedMode,
                        placeName = authorizedAccessState.activeContext?.establishmentName.orEmpty(),
                        assistantUserKey =
                            authorizedAccessState.activeContext?.membershipId ?: "waiter",
                    )
                }
            }
        }
    }

    // La ficha de producto maneja su propio back predictivo dentro de
    // ProductDetailSheet; no registrar otro BackHandler aqui porque le
    // ganaria por orden de composicion y mataria el preview.

    if (walletUserQrOpen) {
        QrScannerDialog(
            onClose = { walletUserQrOpen = false },
            helperText = "Apunta al QR de cuenta que muestra el alumno",
            // El buscador por nombre vive en la hoja de recarga que abrió el escáner: cerrar regresa a él.
            alternative = QrScannerAlternative("Buscar cliente por nombre") { walletUserQrOpen = false },
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
private const val WALLET_VISIBLE_REFRESH_MS = 8_000L
private const val ENTRANCE_TIMEOUT_MS = 15_000L

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
