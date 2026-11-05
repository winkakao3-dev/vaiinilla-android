package com.vaiinilla.app.ui.navigation

import android.Manifest
import android.os.Build
import android.widget.Toast
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
import com.vaiinilla.app.core.notifications.NoticeMembership
import com.vaiinilla.app.core.notifications.NoticePhoneContext
import com.vaiinilla.app.core.notifications.OrderAdvanceNotifier
import com.vaiinilla.app.core.notifications.OrderNotificationTarget
import com.vaiinilla.app.core.notifications.ReceivedNotice
import com.vaiinilla.app.core.notifications.ReceivedNoticeLog
import com.vaiinilla.app.data.auth.student.GoogleSignInHelper
import com.vaiinilla.app.data.auth.student.GoogleSignInUnavailableException
import com.vaiinilla.app.data.counter.AndroidConnectivityWatcher
import com.vaiinilla.app.domain.admin.BusinessType
import com.vaiinilla.app.domain.counter.CounterScope
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.isStripePaymentConfirmedByBackend
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.canCallWaiter
import com.vaiinilla.app.domain.repository.hasPendingAccount
import com.vaiinilla.app.ui.account.AccountDeletionViewModel
import com.vaiinilla.app.ui.admin.AdminSpacesViewModel
import com.vaiinilla.app.ui.admin.AdminStaffViewModel
import com.vaiinilla.app.ui.admin.AdminStoreViewModel
import com.vaiinilla.app.ui.admin.AdminViewModel
import com.vaiinilla.app.ui.assistant.CustomerAssistantActions
import com.vaiinilla.app.ui.assistant.CustomerAssistantScreen
import com.vaiinilla.app.ui.assistant.CustomerAssistantViewModel
import com.vaiinilla.app.ui.auth.student.StudentAuthViewModel
import com.vaiinilla.app.ui.components.MenuCartBar
import com.vaiinilla.app.ui.components.MenuWaiterLayer
import com.vaiinilla.app.ui.components.StudentTab
import com.vaiinilla.app.ui.components.WhileStarted
import com.vaiinilla.app.ui.components.isRental
import com.vaiinilla.app.ui.components.moneyLabel
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
import com.vaiinilla.app.ui.discovery.QrScannerTableCode
import com.vaiinilla.app.ui.mode.AuthorizedAccessViewModel
import com.vaiinilla.app.ui.notices.ReceivedNoticesScreen
import com.vaiinilla.app.ui.notices.ReceivedNoticesViewModel
import com.vaiinilla.app.ui.operational.CashierContext
import com.vaiinilla.app.ui.operational.CashierUnpaidViewModel
import com.vaiinilla.app.ui.operational.CourtsViewModel
import com.vaiinilla.app.ui.operational.OperationalPresenceLifecycle
import com.vaiinilla.app.ui.operational.OperationalViewModel
import com.vaiinilla.app.ui.operational.PendingRefundsViewModel
import com.vaiinilla.app.ui.operational.WaiterViewModel
import com.vaiinilla.app.ui.order.JourneyVenue
import com.vaiinilla.app.ui.order.MenuWaiterCallState
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
import com.vaiinilla.app.ui.reservations.CourtRentSheet
import com.vaiinilla.app.ui.reservations.ReservationsScreen
import com.vaiinilla.app.ui.reservations.ReservationsViewModel
import com.vaiinilla.app.ui.screens.AccountDeletionWarnings
import com.vaiinilla.app.ui.screens.AssistantChatScreen
import com.vaiinilla.app.ui.screens.AuthorizedModeScreen
import com.vaiinilla.app.ui.screens.CartScreen
import com.vaiinilla.app.ui.screens.CashierOperationalScreen
import com.vaiinilla.app.ui.screens.CashierRefundsEntry
import com.vaiinilla.app.ui.screens.CashierUnpaidAccountsScreen
import com.vaiinilla.app.ui.screens.CatalogScreen
import com.vaiinilla.app.ui.screens.CounterOrderSheet
import com.vaiinilla.app.ui.screens.DiscoveryScreen
import com.vaiinilla.app.ui.screens.FloorTable
import com.vaiinilla.app.ui.screens.HomeCheckoutActions
import com.vaiinilla.app.ui.screens.InvitationAcceptanceScreen
import com.vaiinilla.app.ui.screens.KitchenOperationalScreen
import com.vaiinilla.app.ui.screens.LocalScene
import com.vaiinilla.app.ui.screens.LocalVenueArt
import com.vaiinilla.app.ui.screens.MenuFeedScreen
import com.vaiinilla.app.ui.screens.OfflineOrdersCard
import com.vaiinilla.app.ui.screens.OrderConfirmationScreen
import com.vaiinilla.app.ui.screens.OrdersAccessGate
import com.vaiinilla.app.ui.screens.PickupDeliveryFooter
import com.vaiinilla.app.ui.screens.SharedTableCard
import com.vaiinilla.app.ui.screens.SharedTableInviteSheet
import com.vaiinilla.app.ui.screens.SharedTablePage
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
import com.vaiinilla.app.ui.screens.accountDeletionBalance
import com.vaiinilla.app.ui.screens.admin.AdminActions
import com.vaiinilla.app.ui.screens.admin.AdminCashActions
import com.vaiinilla.app.ui.screens.admin.AdminOrdersTables
import com.vaiinilla.app.ui.screens.admin.AdminScreen
import com.vaiinilla.app.ui.screens.admin.AdminSectionsHost
import com.vaiinilla.app.ui.screens.admin.AdminTakeOrderTab
import com.vaiinilla.app.ui.screens.rememberOperationalColors
import com.vaiinilla.app.ui.screens.rememberReduceMotion
import com.vaiinilla.app.ui.screens.rememberVenueArt
import com.vaiinilla.app.ui.screens.scenePaletteForTheme
import com.vaiinilla.app.ui.sharedtable.SHARED_TABLE_WATCHER_ORDERS
import com.vaiinilla.app.ui.sharedtable.SHARED_TABLE_WATCHER_PAGE
import com.vaiinilla.app.ui.sharedtable.SharedTableViewModel
import com.vaiinilla.app.ui.support.SupportLastOrder
import com.vaiinilla.app.ui.support.SupportScreen
import com.vaiinilla.app.ui.wallet.WalletViewModel
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
    val adminInventoryViewModel: com.vaiinilla.app.ui.admin.AdminInventoryViewModel = viewModel()
    val adminCashCutViewModel: com.vaiinilla.app.ui.admin.AdminCashCutViewModel = viewModel()
    val adminStoreViewModel: AdminStoreViewModel = viewModel()
    val adminSpacesViewModel: AdminSpacesViewModel = viewModel()
    val adminSchoolViewModel: com.vaiinilla.app.ui.admin.school.AdminSchoolViewModel = viewModel()
    val waiterViewModel: WaiterViewModel = viewModel()
    val cashierUnpaidViewModel: CashierUnpaidViewModel = viewModel()
    val courtsViewModel: CourtsViewModel = viewModel()
    val reservationsViewModel: ReservationsViewModel = viewModel()
    // Hoja de «Rentar» sobre el asistente; el cobro hecho ahí sí lleva a la confirmación (ver paidOrderEvents).
    var courtRentSheetOpen by remember { mutableStateOf(false) }
    var courtRentSheetPaying by remember { mutableStateOf(false) }
    val sharedTableViewModel: SharedTableViewModel = viewModel()
    val assistantViewModel: CustomerAssistantViewModel = viewModel()
    val openOrderAssistant = { navController.navigate(Routes.ORDER_ASSISTANT) { launchSingleTop = true } }
    val studentAuthViewModel: StudentAuthViewModel = viewModel()
    val authorizedAccessViewModel: AuthorizedAccessViewModel = viewModel()
    val platformViewModel: com.vaiinilla.app.ui.platform.PlatformViewModel = viewModel()
    val storeAccessContext = androidx.compose.ui.platform.LocalContext.current
    val storeAccessMemory = remember { StoreAccessMemory.from(storeAccessContext) }
    val discoveryViewModel: GuestDiscoveryViewModel = viewModel()
    val walletViewModel: WalletViewModel = viewModel()
    val childrenViewModel: ChildrenViewModel = viewModel()
    val guardianControlsViewModel: GuardianControlsViewModel = viewModel()
    val accountDeletionViewModel: AccountDeletionViewModel = viewModel()
    // Caja de una escuela (alumnos sin teléfono) y "Para mañana" de Cocina: padres e hijos.
    val studentCounterViewModel: StudentCounterViewModel = viewModel()
    val reservedOrdersViewModel: ReservedOrdersViewModel = viewModel()
    val pendingRefundsViewModel: PendingRefundsViewModel = viewModel()
    // Aquí y no dentro de su ruta: la entrada de navegación no tiene la fábrica de Hilt y no lo puede construir.
    val receivedNoticesViewModel: ReceivedNoticesViewModel = viewModel()
    val orderState by orderFlowViewModel.uiState
    val operationalState by operationalViewModel.uiState
    val waiterState by waiterViewModel.uiState
    val studentAuthState by studentAuthViewModel.state
    val authorizedAccessState by authorizedAccessViewModel.state
    val discoveryState by discoveryViewModel.state
    val walletRemoteState by walletViewModel.state.collectAsStateWithLifecycle()
    val accountDeletionState by accountDeletionViewModel.state
    val currentVenueId = orderState.guestVenue?.establishment?.id
    val walletForVenue = walletRemoteState.visibleFor(currentVenueId)

    // Quién usa la app en este momento; se lee al momento de usarlo porque «lista para pedir» vive en el repositorio.
    fun currentIdentity(): AppIdentity =
        appIdentity(
            session = studentAuthState.session,
            readyForCheckout = studentAuthViewModel::isReadyForCheckout,
            activeContext = authorizedAccessState.activeContext,
        )
    // La cola de Caja sin conexión vive todo el turno de Caja, no solo con la hoja de Alumnos abierta.
    val cashierShift =
        authorizedAccessState.activeContext
            ?.takeIf { it.role == OperationalRole.CASHIER }
            ?.let { CounterScope(membershipId = it.membershipId, establishmentId = it.establishmentId) }
    LaunchedEffect(cashierShift) { studentCounterViewModel.setShift(cashierShift) }
    LaunchedEffect(authorizedAccessState.activeContext?.membershipId) {
        waiterViewModel.bindContext(authorizedAccessState.activeContext?.membershipId)
    }
    // Efectivo que Caja cobró sin internet y aún no se registra: el cierre de caja espera a que se mande (R5).
    val unsyncedOfflineCash = waiterState.offlineOrders.unregisteredCash.size
    LaunchedEffect(unsyncedOfflineCash, operationalState.role) {
        operationalViewModel.setUnsyncedOfflineCash(unsyncedOfflineCash)
    }
    // Pedidos guardados sin internet de un modo que ya no está abierto (app reiniciada, otro modo): salen solos.
    LaunchedEffect(authorizedAccessState.modes, authorizedAccessState.activeContext?.membershipId) {
        waiterViewModel.rescueOfflineOrders(authorizedAccessState.modes)
    }
    // «Avisos recibidos» explica cada aviso con la cuenta, el modo abierto y los modos que tenía el teléfono.
    val noticeContext = LocalContext.current
    LaunchedEffect(studentAuthState.session?.email, authorizedAccessState.modes, authorizedAccessState.activeContext) {
        val session = studentAuthState.session ?: return@LaunchedEffect
        ReceivedNoticeLog.updatePhone(
            noticeContext,
            NoticePhoneContext(
                account = session.email,
                openMode =
                    authorizedAccessState.activeContext
                        ?.let { "${it.role.label} en ${it.establishmentName}" }
                        ?: "Cliente",
                memberships =
                    authorizedAccessState.modes.map {
                        NoticeMembership(it.membershipId, it.role, it.establishmentId, it.establishmentName)
                    },
            ),
        )
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
        if (currentIdentity().canUseClientAccount && orderState.guestVenue != null) {
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
        val isClientInSchool = orderState.operationalStatus?.isSchool == true && currentIdentity().isClient
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
        if (orderState.guestVenue != null && operationalState.role == null && currentIdentity().isClient) {
            operationalViewModel.setRole(OperationalRole.CLIENT)
        }
    }

    val appContext = LocalContext.current
    LaunchedEffect(Unit) {
        operationalViewModel.orderAdvanceEvents.collect { event ->
            // El mismo avance puede llegar después por push: este id es de la app y no se junta con el del servidor.
            val noticeId = "app:${event.orderId}:${event.newState.wireValue}"
            val shown =
                OrderAdvanceNotifier.notify(
                    context = appContext,
                    orderId = event.orderId,
                    folio = event.folio,
                    newState = event.newState,
                    inSpace = event.inSpace,
                    spaceType = event.spaceType,
                    noticeId = noticeId,
                    acceptedAwaitingPayment = event.acceptedAwaitingPayment,
                    pickUpAtBar = event.pickUpAtBar,
                    homeDelivery = event.homeDelivery,
                    rental = event.rental,
                    rentalSpaceName = event.rentalSpaceName,
                )
            ReceivedNoticeLog.record(
                context = appContext,
                id = noticeId,
                title = shown.title,
                body = shown.body,
                payload =
                    mapOf(
                        "type" to "order_state",
                        "orderId" to event.orderId,
                        "folio" to event.folio.toString(),
                        "state" to event.newState.wireValue,
                    ),
                isRemote = false,
                arrival = if (shown.posted) ReceivedNotice.Arrival.SHOWN else ReceivedNotice.Arrival.NOT_SHOWN,
            )
            // Pagar, cancelar o devolver mueve el saldo: no se espera a que el dato envejezca.
            walletViewModel.invalidate()
            walletViewModel.refreshIfStale(latestVenueId)
        }
    }
    // Con el carrito vacío (pedido mandado o vaciado a mano) el asistente olvida lo que puso: su siguiente pedido ya
    // no resta líneas que el cliente agregue después (#26).
    val cartEmpty = orderState.cartLines.isEmpty()
    LaunchedEffect(cartEmpty) {
        if (cartEmpty) assistantViewModel.forgetCartOrder()
    }
    // El pago de una renta crea un pedido: se sigue en la confirmación (hoja de tarjeta, folio para pagar en caja o
    // compra ya pagada con saldo). Si la persona ya salió de Reservas, el pedido se adopta y no se le arrastra (FLOW-16).
    LaunchedEffect(Unit) {
        reservationsViewModel.paidOrderEvents.collect { order ->
            orderFlowViewModel.adoptReservationOrder(order)
            orderFlowViewModel.refreshRentedCourt()
            if (navController.currentDestination?.route == Routes.RESERVATIONS || courtRentSheetPaying) {
                navController.navigate(Routes.CONFIRMATION) { launchSingleTop = true }
            }
            courtRentSheetPaying = false
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

    // El pedido de un aviso que aún no estaba cargado (app cerrada o recién pasada a cliente): se abre al llegar (K01).
    var notifiedOrderToOpen by remember { mutableStateOf<String?>(null) }

    // «Mesa te llama»: al tablero del mesero, donde la llamada se ve con su motivo; no a Modos (K02).
    fun openWaiterCall() {
        fun showWaiterBoard() {
            orderFlowViewModel.clearGuestVenue()
            navController.navigate(Routes.WAITER) {
                popUpTo(Routes.DISCOVERY) { inclusive = false }
                launchSingleTop = true
            }
        }
        val waiterModes = authorizedAccessState.modes.filter { it.role == OperationalRole.WAITER }
        when (
            resolveWaiterCallAction(
                operationalRole = operationalState.role,
                accessLoading = authorizedAccessState.loading,
                waiterModes = waiterModes.size,
            )
        ) {
            WaiterCallAction.OPEN_WAITER -> {
                if (navController.currentDestination?.route != Routes.WAITER) showWaiterBoard()
                onOrderConsumed()
            }
            WaiterCallAction.ACTIVATE_WAITER -> {
                onOrderConsumed()
                authorizedAccessViewModel.activateMode(waiterModes.single()) {
                    operationalViewModel.setRole(OperationalRole.WAITER)
                    showWaiterBoard()
                }
            }
            WaiterCallAction.OPEN_STAFF_MODES -> {
                if (navController.currentDestination?.route != Routes.STAFF_MODES) {
                    navController.navigate(Routes.STAFF_MODES) { launchSingleTop = true }
                }
                onOrderConsumed()
            }
            WaiterCallAction.WAIT -> Unit
        }
    }

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
            if (target == OrderNotificationTarget.WAITER_CALL) {
                openWaiterCall()
                return@LaunchedEffect
            }
            // Aviso de personal sin pedido (turno por terminar): abrir la consola de modos.
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
            // Pasar a cliente vacía la lista: sin esto el aviso dejaba al cliente en Pedidos, no en su pedido (K01).
            notifiedOrderToOpen = orderId.takeIf { known == null }
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

    LaunchedEffect(notifiedOrderToOpen, operationalState.orders) {
        val id = notifiedOrderToOpen ?: return@LaunchedEffect
        val order = operationalState.orders.firstOrNull { it.summary.id == id } ?: return@LaunchedEffect
        notifiedOrderToOpen = null
        // Una renta se sigue en la lista; si la persona ya se fue a otra pantalla, no se le arrastra.
        if (order.isRental || navController.currentDestination?.route != Routes.STUDENT_TRACKING) return@LaunchedEffect
        operationalViewModel.selectOrder(id)
        orderFlowViewModel.clearCreatedOrder()
        navController.navigate(Routes.CONFIRMATION) { launchSingleTop = true }
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
        orderFlowViewModel.clearMenuFilters()
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

    // Cambiar a cliente desmontaría la pantalla de un cobro o cierre que el servidor ya está moviendo.
    fun linkBlockedByStaffAction(): Boolean {
        if (authorizedAccessState.activeContext == null || !operationalState.acting) return false
        Toast
            .makeText(appContext, "Termina lo que estás cobrando y vuelve a abrir el enlace.", Toast.LENGTH_LONG)
            .show()
        return true
    }

    // Escanear una mesa o una tienda es querer pedir ahí: con un modo de personal activo se sale a cliente.
    fun leaveStaffModeForLink(onLeft: () -> Unit) {
        authorizedAccessViewModel.leaveStaffModeForLink {
            operationalViewModel.clearRole()
            onLeft()
        }
    }

    LaunchedEffect(pendingEstablishmentSlug) {
        val slug = pendingEstablishmentSlug?.trim().orEmpty()
        if (slug.isEmpty()) return@LaunchedEffect
        if (linkBlockedByStaffAction()) return@LaunchedEffect onDeepLinkConsumed()
        startEntrance(slug)
        leaveStaffModeForLink {
            discoveryViewModel.openSlug(
                slug = slug,
                onEntered = { venue ->
                    enterVenueAndOpenCatalog(venue)
                },
                onFinished = onDeepLinkConsumed,
            )
        }
    }
    LaunchedEffect(pendingSpaceToken) {
        val token = pendingSpaceToken?.trim().orEmpty()
        if (token.isEmpty()) return@LaunchedEffect
        if (linkBlockedByStaffAction()) return@LaunchedEffect onSpaceLinkConsumed()
        startEntrance(token)
        leaveStaffModeForLink {
            discoveryViewModel.updateSpaceToken(token)
            discoveryViewModel.resolveSpaceToken(
                onEntered = ::enterVenueAndOpenCatalog,
                onFinished = onSpaceLinkConsumed,
            )
        }
    }
    var qrScannerOpen by remember { mutableStateOf(false) }
    // Cada aumento abre la hoja del código de mesa en Descubrir (alternativa al QR sin cámara).
    var tableCodeRequest by remember { mutableIntStateOf(0) }
    var walletUserQrOpen by remember { mutableStateOf(false) }
    var pendingPickupDelivery by remember { mutableStateOf<PendingPickupDelivery?>(null) }
    var cashierSearchRequests by remember { mutableIntStateOf(0) }

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
                // Pedir como cliente es buscar dónde pedir: el menú sin lugar elegido cargaba el negocio de la
                // membresía de cliente, con el encabezado vacío («M», «MENÚ»).
                OperationalRole.CLIENT -> Routes.DISCOVERY
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

    /** Con los modos ya cargados, vuelve al modo de personal que estaba abierto (O04). */
    fun reopenRememberedStaffMode() {
        if (authorizedAccessViewModel.state.value.activeContext != null) return
        val remembered = authorizedAccessViewModel.rememberedStaffMode() ?: return
        authorizedAccessViewModel.activateMode(remembered) {
            operationalViewModel.setRole(remembered.role)
            navigateAuthorizedMode(remembered.role)
        }
    }

    /**
     * O04: al volver la red, Modos que se quedó en error recarga solo y reabre el modo recordado; un modo reabierto
     * sin internet se activa de nuevo con el servidor (token nuevo; si el acceso se revocó, lo saca la revisión).
     */
    fun onNetworkBack() {
        val access = authorizedAccessViewModel.state.value
        val active = access.activeContext
        if (access.restoredOffline && active != null) {
            authorizedAccessViewModel.refreshModes(force = true) { loaded ->
                if (!loaded) return@refreshModes
                val current = authorizedAccessViewModel.state.value
                if (!current.restoredOffline) return@refreshModes
                current.modes
                    .firstOrNull {
                        it.role == active.role &&
                            it.establishmentId == active.establishmentId &&
                            it.membershipId == active.membershipId
                    }?.let { authorizedAccessViewModel.activateMode(it) }
            }
            return
        }
        val onModes = navController.currentDestination?.route == Routes.STAFF_MODES
        if (onModes && active == null && !access.loading && (access.modes.isEmpty() || access.errorMessage != null)) {
            authorizedAccessViewModel.refreshModes(force = true) { loaded ->
                if (loaded) reopenRememberedStaffMode()
            }
        }
    }

    val connectivityContext = LocalContext.current
    DisposableEffect(Unit) {
        val reconnect =
            AndroidConnectivityWatcher(connectivityContext).onReconnect {
                // Llega en un hilo de ConnectivityManager: se pasa al principal.
                launchNavScope.launch { onNetworkBack() }
            }
        onDispose { reconnect.close() }
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
            platformViewModel.checkAuthority()
        } else {
            platformViewModel.clearOnSignOut()
        }
    }

    val storeAccessPending = pendingStoreAccess(authorizedAccessState.modes, storeAccessMemory)?.establishmentId
    LaunchedEffect(storeAccessPending) {
        if (storeAccessPending != null && navController.currentDestination?.route != Routes.STORE_ACCESS) {
            navController.navigate(Routes.STORE_ACCESS) { launchSingleTop = true }
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
        orderFlowViewModel.claimGuestOrders(onClaimed = operationalViewModel::refresh)
        studentAuthViewModel.refreshGuestVenue()
        authorizedAccessViewModel.refreshCurrentSession()
        fun continueToReturnRoute() {
            if (!navController.popBackStack(returnRoute, inclusive = false)) {
                navController.navigate(returnRoute) { launchSingleTop = true }
            }
        }
        // Quien venía a pagar o a otra pantalla sigue ahí; solo quien entró sin rumbo elige con qué acceso.
        val choosesAccess = returnRoute == Routes.DISCOVERY || returnRoute == Routes.CATALOG
        if (!choosesAccess) {
            continueToReturnRoute()
            return
        }
        val modesRefreshed = CompletableDeferred<Boolean>()
        authorizedAccessViewModel.refreshModes(force = true) { modesRefreshed.complete(it) }
        launchNavScope.launch {
            modesRefreshed.await()
            val hasStaff = hasStaffLaunchModes(authorizedAccessViewModel.state.value.modes.map { it.role })
            if (hasStaff || platformViewModel.fetchIsAuthority()) {
                navController.navigate(Routes.STAFF_MODES) {
                    popUpTo(returnRoute) { inclusive = false }
                    launchSingleTop = true
                }
            } else {
                continueToReturnRoute()
            }
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
        orderFlowViewModel.claimGuestOrders(onClaimed = operationalViewModel::refresh)
        studentAuthViewModel.refreshGuestVenue()
        authorizedAccessViewModel.refreshCurrentSession()
        val modesRefreshed = CompletableDeferred<Boolean>()
        authorizedAccessViewModel.refreshModes(force = true) { modesRefreshed.complete(it) }
        launchNavScope.launch {
            val modesLoaded = modesRefreshed.await()
            val destination =
                resolveLaunchDestination(
                    pendingEstablishmentSlug = pendingEstablishmentSlug,
                    pendingSpaceToken = pendingSpaceToken,
                    session = authorizedAccessViewModel.state.value.session,
                    hasStaffModes =
                        hasStaffLaunchModes(
                            authorizedAccessViewModel.state.value.modes
                                .map { it.role },
                        ) || platformViewModel.fetchIsAuthority(),
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
        sharedTableViewModel.clearForSessionTermination()
        ChildDeepLinkStore.reset()
        ReceivedNoticeLog.clear(appContext)
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
                    if (currentIdentity().canUseClientAccount) {
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
                                pendingSpaceToken = pendingSpaceToken,
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
                        val destination =
                            resolveLaunchDestination(
                                pendingEstablishmentSlug = pendingEstablishmentSlug,
                                pendingSpaceToken = pendingSpaceToken,
                                session = authorizedAccessViewModel.state.value.session,
                                hasStaffModes =
                                    hasStaffLaunchModes(
                                        authorizedAccessViewModel.state.value.modes
                                            .map { it.role },
                                    ),
                                hasSavedVenue = studentAuthViewModel.state.value.guestVenue != null,
                                modesLoaded = modesLoaded,
                            )
                        navigateLaunchDestination(destination)
                        // Android cerró la app con Caja, Cocina o Mesero abiertos: se vuelve a ese modo sin pasar
                        // por la lista. Modos queda debajo, como cuando se elige a mano.
                        val remembered =
                            authorizedAccessViewModel
                                .rememberedStaffMode()
                                ?.takeIf { destination == LaunchDestination.StaffModes && modesLoaded }
                        if (remembered != null) {
                            authorizedAccessViewModel.activateMode(remembered) {
                                operationalViewModel.setRole(remembered.role)
                                navigateAuthorizedMode(remembered.role)
                            }
                        } else if (!modesLoaded && destination == LaunchDestination.StaffModes) {
                            // Sin internet al abrir (O04): Mesero o Caja se reabren con lo guardado y la cola sin
                            // conexión guarda los pedidos. Si no se puede, Modos muestra el error y reintenta solo.
                            authorizedAccessViewModel.reopenRememberedModeOffline()?.let { reopened ->
                                operationalViewModel.setRole(reopened.role)
                                navigateAuthorizedMode(reopened.role)
                            }
                        }
                    }
                }

                WelcomeBootStage(reduceMotion = rememberReduceMotion())
            }

            composable(Routes.DISCOVERY) {
                // Tus escuelas: las de sus accesos, para volver sin código.
                // Tras cambiar de cuenta o de modo el estado se limpió y no se pasa por SPLASH: se vuelve a buscar.
                LaunchedEffect(studentAuthState.session?.uid, discoveryState.sessionEpoch) {
                    discoveryViewModel.loadLastPlace()
                    discoveryViewModel.loadMySchools()
                    if (discoveryState.sessionEpoch > 0 && discoveryState.establishments.isEmpty()) {
                        discoveryViewModel.search("")
                    }
                }
                DiscoveryScreen(
                    tableCodeRequest = tableCodeRequest,
                    state = discoveryState,
                    onQueryChange = discoveryViewModel::updateQuery,
                    onRetrySearch = discoveryViewModel::retrySearch,
                    onEnableNearby = discoveryViewModel::enableNearby,
                    onDisableNearby = discoveryViewModel::disableNearby,
                    onNearbyDenied = discoveryViewModel::onNearbyPermissionDenied,
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
                    onLoadMore = discoveryViewModel::loadMore,
                    onOpenSchool = { school ->
                        startEntrance(null)
                        discoveryViewModel.openSchool(school, onEntered = ::enterVenueAndOpenCatalog)
                    },
                )

                if (qrScannerOpen) {
                    val venueWithoutTable =
                        discoveryState.selected?.establishment ?: discoveryState.lastPlace?.establishment
                    QrScannerDialog(
                        onClose = { qrScannerOpen = false },
                        title = "Apunta al QR de tu mesa",
                        subtitle = "También sirve acercar el teléfono a la etiqueta",
                        alternative =
                            QrScannerAlternative("Escribir código de mesa") {
                                qrScannerOpen = false
                                tableCodeRequest++
                            },
                        tableCode =
                            QrScannerTableCode(
                                venueName = venueWithoutTable?.name,
                                onSubmit = { code ->
                                    qrScannerOpen = false
                                    discoveryViewModel.updateSpaceToken(code)
                                    startEntrance(code)
                                    discoveryViewModel.resolveSpaceToken(onEntered = ::enterVenueAndOpenCatalog)
                                },
                                onOrderWithoutTable = {
                                    qrScannerOpen = false
                                    if (venueWithoutTable != null) {
                                        startEntrance(null)
                                        discoveryViewModel.selectEstablishment(
                                            establishment = venueWithoutTable,
                                            onEntered = ::enterVenueAndOpenCatalog,
                                        )
                                        if (discoveryViewModel.state.value.pendingSwitch != null) entrance = null
                                    }
                                },
                            ),
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
                // Quien pasa por Modos vuelve al menú sin la categoría ni la búsqueda de antes: juntas dejaban
                // la lista vacía («Antojitos» con «cafe», EQUIPO-31).
                LaunchedEffect(Unit) { orderFlowViewModel.clearMenuFilters() }
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
                    onRetry = {
                        authorizedAccessViewModel.refreshModes(force = true) { loaded ->
                            if (loaded) reopenRememberedStaffMode()
                        }
                    },
                    isPlatformAuthority = platformViewModel.state.value.authority?.isAuthority == true,
                    onOpenPlatform = { navController.navigate(Routes.PLATFORM) { launchSingleTop = true } },
                    onContinueAsClient = {
                        val target =
                            if (studentAuthViewModel.state.value.guestVenue != null) Routes.CATALOG else Routes.DISCOVERY
                        navController.navigate(target) {
                            popUpTo(Routes.STAFF_MODES) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.PLATFORM) {
                com.vaiinilla.app.ui.screens.platform.PlatformScreen(
                    viewModel = platformViewModel,
                    onExit = { navController.popBackStack() },
                )
            }

            composable(Routes.STORE_ACCESS) {
                val mode = pendingStoreAccess(authorizedAccessState.modes, storeAccessMemory)
                LaunchedEffect(mode == null) { if (mode == null) navController.popBackStack() }
                if (mode != null) {
                    com.vaiinilla.app.ui.screens.platform.StoreAccessScreen(
                        storeName = mode.establishmentName,
                        busy = authorizedAccessState.loading,
                        onConfigure = {
                            authorizedAccessViewModel.activateMode(mode) {
                                storeAccessMemory.markSeen(mode.establishmentId)
                                operationalViewModel.setRole(mode.role)
                                navigateAuthorizedMode(mode.role)
                            }
                        },
                        onLater = {
                            storeAccessMemory.markSeen(mode.establishmentId)
                            navController.popBackStack()
                        },
                    )
                }
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
                            // Sin cuenta también se llama al mesero, con la sesión del invitado.
                            signedIn = studentAuthState.session != null || orderFlowViewModel.isGuestCheckout(),
                            rentedCourtId = orderState.rentedCourt?.id,
                        )?.let { spaceId ->
                            val qrToken = orderState.guestVenue?.space?.qrToken
                            val alias =
                                waiterCallAlias(
                                    studentAuthState.session?.displayName ?: orderFlowViewModel.guestDisplayName(),
                                )
                            val guestNeedsName =
                                studentAuthState.session == null && orderFlowViewModel.guestDisplayName().isBlank()
                            val layer: @Composable (MenuCartBar?, Boolean) -> Unit = { cart, visible ->
                                MenuWaiterLayer(
                                    spaceId = spaceId,
                                    visible = visible,
                                    cart = cart,
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
                                    // Sin cuenta y sin nombre no se sienta como «Cliente»: la hoja le pide el suyo.
                                    onJoinAs =
                                        qrToken
                                            ?.takeIf { guestNeedsName }
                                            ?.let { token ->
                                                { name: String -> sharedTableViewModel.joinForWaiterCall(token, name) }
                                            },
                                    place =
                                        orderState.guestVenue?.space?.let { SpaceCopy.named(it.type, it.name) }
                                            ?: sharedTableViewModel.uiState.value.table
                                                ?.let { SpaceCopy.named(it.spaceType, it.spaceName) }
                                            ?: orderState.rentedCourt?.let { SpaceCopy.named(it.type, it.name) },
                                    // Solo si la tienda tiene el asistente para clientes.
                                    onOpenAssistant =
                                        if (orderState.guestVenue?.establishment?.assistantForCustomers == false) {
                                            null
                                        } else {
                                            openOrderAssistant
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
                SharedTableInviteHost(
                    viewModel = sharedTableViewModel,
                    qrToken =
                        orderState.guestVenue
                            ?.space
                            ?.qrToken
                            .takeIf { studentAuthState.session != null },
                    spaceName = orderState.guestVenue?.space?.name,
                    spaceType = orderState.guestVenue?.space?.type,
                    displayName = studentAuthState.session?.displayName.orEmpty(),
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

            composable(Routes.ORDER_ASSISTANT) {
                val venue = orderState.guestVenue
                val table = sharedTableViewModel.uiState.value.table
                val assistantSignedIn = studentAuthState.session != null || orderFlowViewModel.isGuestCheckout()
                val assistantPlaceId =
                    menuWaiterSpaceId(
                        guestSpaceId = venue?.space?.id,
                        sharedTableSpaceId = table?.spaceId,
                        signedIn = assistantSignedIn,
                    )
                // Quien rentó una pista en la app también llama al mesero desde el asistente (#18).
                val rentedCourt = orderState.rentedCourt
                val assistantSpaceId = assistantPlaceId ?: rentedCourt?.id?.takeIf { assistantSignedIn }
                // La plática se reinicia con el lugar del QR o la mesa, no cuando llega la pista rentada.
                LaunchedEffect(venue?.establishment?.id, assistantPlaceId) {
                    val establishmentId = venue?.establishment?.id
                    assistantViewModel.onVisible("$establishmentId:$assistantPlaceId", establishmentId)
                }
                val prepare: suspend () -> Unit = { orderFlowViewModel.ensureClientContext() }
                CustomerAssistantScreen(
                    state = assistantViewModel.state.value,
                    venueName = venue?.establishment?.name,
                    place =
                        venue?.space?.let { SpaceCopy.named(it.type, it.name) }
                            ?: table?.let { SpaceCopy.named(it.spaceType, it.spaceName) }
                            ?: rentedCourt
                                ?.takeIf { assistantSpaceId == it.id }
                                ?.let { SpaceCopy.named(it.type, it.name) },
                    customerName =
                        (studentAuthState.session?.displayName ?: orderFlowViewModel.guestDisplayName())
                            .takeIf { it.isNotBlank() }
                            ?.let(::waiterCallAlias),
                    actions =
                        CustomerAssistantActions(
                            addProduct = { card ->
                                if (card.needsOptions) {
                                    // La ficha con sus opciones vive en el menú.
                                    navController.popBackStack()
                                    orderFlowViewModel.openProduct(card.productId)
                                    false
                                } else {
                                    // Solo dice «Agregado» si de verdad entró (agotado o carrito lleno no cuentan).
                                    val before = orderFlowViewModel.uiState.value.cartLines
                                    orderFlowViewModel.quickAddProduct(card.productId)
                                    orderFlowViewModel.uiState.value.cartLines != before
                                }
                            },
                            addOrder = { order, replacing ->
                                orderFlowViewModel.addAssistantLines(order.lines, replacing)
                            },
                            openCart = { navController.navigateStudent(Routes.CART) },
                            // La ficha del producto (la misma del menú) vive debajo del asistente.
                            openProduct = { card ->
                                navController.popBackStack()
                                orderFlowViewModel.openProduct(card.productId)
                            },
                            // La tarjeta de una pista abre la hoja de renta con esa pista elegida. Si el lugar pide
                            // cuenta y no hay sesión, se entra primero; preparar la cuenta o dar el nombre del
                            // invitado se resuelve en Reservas, que ya sabe hacerlo.
                            openCourt = { court ->
                                reservationsViewModel.preselectCourt(court.courtId)
                                reservationsViewModel.dismissMessages()
                                val requiresAuth = orderFlowViewModel.requiresStudentAuth()
                                when {
                                    requiresAuth && venue?.establishment?.clientIdRequired == true ->
                                        navController.navigate(Routes.authLandingRoute(Routes.RESERVATIONS))
                                    requiresAuth && studentAuthState.session != null ->
                                        navController.navigate(Routes.RESERVATIONS) { launchSingleTop = true }
                                    else -> {
                                        reservationsViewModel.onVisible(
                                            guestSlug = venue?.establishment?.slug,
                                            hasAccount = !requiresAuth,
                                        )
                                        if (reservationsViewModel.uiState.value.guestEntry) {
                                            navController.navigate(Routes.RESERVATIONS) { launchSingleTop = true }
                                        } else {
                                            courtRentSheetPaying = false
                                            courtRentSheetOpen = true
                                        }
                                    }
                                }
                            },
                            callWaiter =
                                if (assistantSpaceId == null) {
                                    null
                                } else {
                                    { offer ->
                                        orderFlowViewModel.ensureClientContext()
                                        operationalViewModel.callWaiter(assistantSpaceId, offer.reason, null).map { }
                                    }
                                },
                        ),
                    onSend = { text -> assistantViewModel.send(text, prepare) },
                    onRetry = { assistantViewModel.retry(prepare) },
                    onRestart = assistantViewModel::restart,
                    onTurnDone = assistantViewModel::markDone,
                    onOrderInCart = assistantViewModel::markOrderInCart,
                    onSubmitName = { name -> assistantViewModel.submitName(name, prepare) },
                    onBack = { navController.popBackStack() },
                    guestName = orderFlowViewModel.guestDisplayName(),
                )
                if (courtRentSheetOpen) {
                    val hasAccount = !orderFlowViewModel.requiresStudentAuth()
                    WhileStarted(
                        venue?.establishment?.slug,
                        hasAccount,
                        onStart = {
                            reservationsViewModel.onVisible(
                                guestSlug = venue?.establishment?.slug,
                                hasAccount = hasAccount,
                            )
                        },
                        onStop = reservationsViewModel::onHidden,
                    )
                    CourtRentSheet(
                        state = reservationsViewModel.uiState.value,
                        onDismiss = {
                            reservationsViewModel.dismissPayment()
                            reservationsViewModel.dismissMessages()
                            courtRentSheetOpen = false
                        },
                        onPickNow = reservationsViewModel::pickRentNow,
                        onSelectStart = reservationsViewModel::selectStart,
                        onSelectDuration = reservationsViewModel::selectDuration,
                        onSelectPlayers = reservationsViewModel::selectPlayers,
                        onReserve = reservationsViewModel::reserve,
                        onPay = { method ->
                            courtRentSheetPaying = true
                            reservationsViewModel.pay(method)
                        },
                        onChangeDuration = reservationsViewModel::changePendingDuration,
                        onChangePlayers = reservationsViewModel::changePendingPlayers,
                        onOtherDays = {
                            courtRentSheetOpen = false
                            navController.navigate(Routes.RESERVATIONS) { launchSingleTop = true }
                        },
                        cardAvailable = orderState.canPayWithCard,
                        balanceAvailable = orderState.canPayWithBalance,
                    )
                }
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
                    onRentNow = reservationsViewModel::rentNow,
                    onSelectStart = reservationsViewModel::selectStart,
                    onSelectDuration = reservationsViewModel::selectDuration,
                    onReserve = reservationsViewModel::reserve,
                    onSelectPlayers = reservationsViewModel::selectPlayers,
                    onPay = reservationsViewModel::pay,
                    onDismissPayment = reservationsViewModel::dismissPayment,
                    onResumePayment = reservationsViewModel::resumePayment,
                    onCancel = reservationsViewModel::cancel,
                    onDismissMessage = reservationsViewModel::dismissMessages,
                    onChangeDuration = reservationsViewModel::changePendingDuration,
                    onChangePlayers = reservationsViewModel::changePendingPlayers,
                    cardAvailable = orderState.canPayWithCard,
                    balanceAvailable = orderState.canPayWithBalance,
                    onGuestNameChange = reservationsViewModel::onGuestNameChange,
                    onEnterAsGuest = reservationsViewModel::enterAsGuest,
                    onSignIn =
                        if (reservationsHasAccount) {
                            null
                        } else {
                            { navController.navigate(Routes.authLandingRoute(Routes.RESERVATIONS)) }
                        },
                    onCreateAccount =
                        if (reservationsHasAccount) {
                            null
                        } else {
                            { navController.navigate(Routes.authRegisterRoute(Routes.RESERVATIONS)) }
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
                    schoolName = orderState.guestVenue?.establishment?.name,
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
                        guardianControls = guardianControlsViewModel.controls,
                        familySection = familySection,
                        profileInitials = displayInitials(studentAuthState.session?.displayName.orEmpty()),
                        profileName = studentAuthState.session?.displayName,
                        onOpenAccount = { navController.navigateStudent(Routes.WALLET_ACCOUNT) },
                        onChangeVenue = {
                            navController.navigate(Routes.DISCOVERY) {
                                launchSingleTop = true
                            }
                        },
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
                    initialMethod = entry.arguments?.getString("method") ?: "card",
                    onBack = { navController.popBackStack() },
                    onCreditBalance = {},
                )
            }

            composable(Routes.WALLET_METHODS) {
                WalletPaymentMethodsScreen(
                    onBack = { navController.popBackStack() },
                    onAddCard = { navController.navigate(Routes.WALLET_ADD_CARD) },
                )
            }

            composable(Routes.WALLET_ADD_CARD) {
                WalletAddCardScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(Routes.RECEIVED_NOTICES) {
                ReceivedNoticesScreen(
                    viewModel = receivedNoticesViewModel,
                    modes = authorizedAccessState.modes,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.WALLET_ACCOUNT) {
                LaunchedEffect(Unit) { walletViewModel.refresh() }
                // Antes de borrar la cuenta hay que decir qué hijos dependen de ella; sin abrir Padres e hijos no se han traído.
                LaunchedEffect(studentAuthState.session?.uid) {
                    if (studentAuthState.session != null &&
                        !childrenViewModel.state.hasLoaded
                    ) {
                        childrenViewModel.state.load()
                    }
                }
                val hasStaffModes =
                    authorizedAccessState.modes.any { it.role != OperationalRole.CLIENT } ||
                        platformViewModel.state.value.authority?.isAuthority == true
                WalletAccountScreen(
                    onBack = { navController.popBackStack() },
                    displayName = studentAuthState.session?.displayName.orEmpty(),
                    email = studentAuthState.session?.email.orEmpty(),
                    // El uid de Firebase no es el id de la cartera: Caja no lo encuentra. Sin cartera no hay código.
                    userId = walletForVenue.data?.wallet?.userId,
                    signedIn = studentAuthState.session != null,
                    venueName = orderState.guestVenue?.establishment?.name,
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
                    onOpenNotices = {
                        navController.navigate(Routes.RECEIVED_NOTICES) { launchSingleTop = true }
                    },
                    accountDeletionState = accountDeletionState,
                    termsUrl = studentAuthState.termsUrl,
                    privacyUrl = studentAuthState.privacyUrl,
                    deletionWarnings =
                        AccountDeletionWarnings(
                            balance = accountDeletionBalance(walletForVenue.data?.wallet?.visibleBalance),
                            venueName = orderState.guestVenue?.establishment?.name,
                            principalChildren =
                                childrenViewModel.state.children
                                    .filter { it.isPrincipal }
                                    .map { it.firstName },
                            staffRoles =
                                authorizedAccessState.modes
                                    .map { it.role }
                                    .filter { it != OperationalRole.CLIENT }
                                    .distinct()
                                    .map { it.label },
                        ),
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
                                folio = it.summary.folio,
                                state = it.summary.state.label,
                                total = moneyLabel(it.summary.total),
                            )
                        },
                    onOpenLastOrder = { navController.navigateStudent(Routes.STUDENT_TRACKING) },
                )
            }

            composable(Routes.SHARED_TABLE) {
                WhileStarted(
                    onStart = { sharedTableViewModel.onVisible(SHARED_TABLE_WATCHER_PAGE) },
                    onStop = { sharedTableViewModel.onHidden(SHARED_TABLE_WATCHER_PAGE) },
                )
                val tableState = sharedTableViewModel.uiState.value
                val table = tableState.table
                // Al salir de la mesa (o si ya no está sentado) la página no tiene qué mostrar.
                LaunchedEffect(tableState.loaded, table == null) {
                    if (tableState.loaded && table == null) navController.popBackStack()
                }
                if (table != null) {
                    val waiter =
                        remember(table.spaceId) {
                            MenuWaiterCallState(
                                spaceId = table.spaceId,
                                onCurrent = { id ->
                                    orderFlowViewModel.ensureClientContext()
                                    operationalViewModel.currentWaiterCall(id)
                                },
                                onCall = { id, reason, orderId ->
                                    orderFlowViewModel.ensureClientContext()
                                    operationalViewModel.callWaiter(id, reason, orderId)
                                },
                                onCancel = operationalViewModel::cancelWaiterCall,
                                onJoinTable = null,
                            )
                        }
                    SharedTablePage(
                        table = table,
                        busy = tableState.busy,
                        error = tableState.error,
                        errorFolio = tableState.errorFolio,
                        onClaim = sharedTableViewModel::claim,
                        onLeave = sharedTableViewModel::leave,
                        waiter = waiter,
                    )
                }
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
                        onPickUpAtBarChange = orderFlowViewModel::updateCheckoutPickUpAtBar,
                        onConfirmTakeAwayWithSeat = orderFlowViewModel::confirmTakeAwayWithSeat,
                        onDismissTakeAwayWithSeat = orderFlowViewModel::dismissTakeAwayWithSeat,
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
                        venueName = orderState.guestVenue?.establishment?.name,
                        venuePhotoUrl = orderState.guestVenue?.establishment?.photoUrl,
                        onWallet = { navController.navigateStudent(Routes.WALLET) },
                        home =
                            HomeCheckoutActions(
                                onDestinationChange = orderFlowViewModel::updateCheckoutDestination,
                                onEditAddress = orderFlowViewModel::editDeliveryAddress,
                                onQuoteStart = orderFlowViewModel::startDeliveryQuote,
                                onQuoteFailed = orderFlowViewModel::failDeliveryQuote,
                                onQuote = orderFlowViewModel::quoteHomeDelivery,
                                onSelectDate = orderFlowViewModel::selectDeliveryDate,
                                onSelectPayment = orderFlowViewModel::selectHomePayment,
                            ),
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
                        studentAuthViewModel.checkVerification(
                            onVerified = {
                                if (returnRoute == Routes.DISCOVERY) {
                                    finishLaunchAuth()
                                } else {
                                    finishStudentAuth(returnRoute)
                                }
                            },
                            // La matrícula ya no se pide al crear la cuenta: con la sesión verificada, la pantalla
                            // de entrar pide solo lo que falta para este lugar.
                            onNeedsContextualId = {
                                navController.navigate(Routes.authLoginRoute(returnRoute)) {
                                    launchSingleTop = true
                                }
                            },
                        )
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
                // Aunque algo suelte la selección, la copia del sondeo sigue siendo la vigente: una renta que Caja
                // cobró ya no se queda en «Por pagar» con la foto de cuando se apartó (en vivo, D03).
                val refreshedCreated =
                    orderState.createdOrder?.let { created ->
                        val polled =
                            selectedOrder?.takeIf { it.summary.id == created.summary.id }
                                ?: operationalState.orders.firstOrNull { it.summary.id == created.summary.id }
                        polled
                            ?.takeIf { it.summary.version >= created.summary.version }
                            ?.let { fresh -> fresh.copy(pickupToken = fresh.pickupToken ?: created.pickupToken) }
                            ?: created
                    }
                // Con un pedido elegido que aún carga (abierto desde un aviso) no se muestra el pago de otro pedido:
                // se veía «Pago confirmado» de un pedido con tarjeta anterior (K01).
                val selectedId = operationalState.selectedOrderId
                val confirmationOrder =