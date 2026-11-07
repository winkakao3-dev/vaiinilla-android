package com.vaiinilla.app.ui.operational

import android.content.Context
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.cashcut.AccountingReport
import com.vaiinilla.app.data.cashcut.CashCutRepository
import com.vaiinilla.app.data.cashcut.DayCut
import com.vaiinilla.app.data.cashcut.ShiftCut
import com.vaiinilla.app.data.wallet.PendingWalletReloadStore
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.domain.model.CashCollectionResult
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import com.vaiinilla.app.domain.repository.DeviceIdentity
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.domain.repository.WalletRepository
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.model.WalletData
import com.vaiinilla.app.domain.model.WalletReloadReceipt
import com.vaiinilla.app.domain.usecase.CancelOrderUseCase
import com.vaiinilla.app.domain.usecase.CloseCashSessionUseCase
import com.vaiinilla.app.domain.usecase.CollectCashUseCase
import com.vaiinilla.app.domain.usecase.GetOrderUseCase
import com.vaiinilla.app.domain.usecase.ListOrdersUseCase
import com.vaiinilla.app.domain.usecase.OpenCashSessionUseCase
import com.vaiinilla.app.domain.usecase.TransitionOrderUseCase
import java.time.LocalDate
import java.util.Base64

/** Arnés de ViewModels de personal en la JVM: repositorios en memoria, sin red. */
internal object StaffHarness {
    private val mainThread: java.util.concurrent.ExecutorService by lazy { java.util.concurrent.Executors.newSingleThreadExecutor() }
    private var mainInstalled = false

    /**
     * Un solo hilo para el estado, como el hilo principal de verdad (con varios, dos escrituras se pisan), puesto una
     * vez por proceso: ponerlo y quitarlo por prueba chocaba con corutinas de ViewModels de pruebas anteriores.
     */
    @Synchronized
    fun installMain() {
        if (mainInstalled) return
        mainInstalled = true
        kotlinx.coroutines.Dispatchers.setMain(mainThread.asCoroutineDispatcher())
    }

    fun jwt(establishment: String = "est-1"): String {
        val payload = """{"uid":"u1","rol":"cajero","establecimiento_id":"$establishment","membresia_id":"m1"}"""
        return "h." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray()) + ".s"
    }

    /** Espera, en tiempo real, a que una condición se cumpla; el trabajo de IO corre en otros hilos. */
    fun awaitUntil(
        what: String,
        timeoutMs: Long = 20_000,
        cond: () -> Boolean,
    ) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!cond()) {
            if (System.currentTimeMillis() > end) throw AssertionError("Se esperaba: $what")
            Thread.sleep(10)
        }
    }

    /** El registrador de token nunca llega a la red en la JVM: no hay Firebase y falla antes de usar el cliente. */
    fun registrar(
        context: Context,
        store: SecureSessionStore,
    ): DeviceTokenRegistrar {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val field = unsafeClass.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null)
        val api =
            unsafeClass.getMethod("allocateInstance", Class::class.java).invoke(unsafe, HttpVaiinillaApiClient::class.java)
                as HttpVaiinillaApiClient
        return DeviceTokenRegistrar(context, api, store)
    }

    fun cashierViewModel(
        context: Context,
        orders: FakeOrders = FakeOrders(),
        cash: FakeCashSession = FakeCashSession(),
        wallet: FakeWallet = FakeWallet(),
        session: FakeSessionStore = FakeSessionStore(jwt()),
        catalog: FakeCatalog = FakeCatalog(),
    ): CashierViewModel =
        CashierViewModel(
            listOrders = ListOrdersUseCase(orders),
            getOrder = GetOrderUseCase(orders),
            transitionOrder = TransitionOrderUseCase(orders),
            cancelOrder = CancelOrderUseCase(orders),
            heartbeatRepository = FakeHeartbeat(),
            deviceIdentity = FakeDevice(),
            deviceTokenRegistrar = registrar(context, session),
            sessionStore = session,
            catalogRepository = catalog,
            collectCash = CollectCashUseCase(orders),
            openCashSession = OpenCashSessionUseCase(cash),
            closeCashSession = CloseCashSessionUseCase(cash),
            cashSessionRepository = cash,
            walletRepository = wallet,
            pendingWalletReloadStore = PendingWalletReloadStore(context, session),
            cashCutRepository = FakeCashCut(),
        )

    fun kitchenViewModel(
        context: Context,
        orders: FakeOrders = FakeOrders(),
        catalog: FakeCatalog = FakeCatalog(),
        session: FakeSessionStore = FakeSessionStore(jwt()),
    ): KitchenViewModel =
        KitchenViewModel(
            listOrders = ListOrdersUseCase(orders),
            getOrder = GetOrderUseCase(orders),
            transitionOrder = TransitionOrderUseCase(orders),
            cancelOrder = CancelOrderUseCase(orders),
            heartbeatRepository = FakeHeartbeat(),
            deviceIdentity = FakeDevice(),
            deviceTokenRegistrar = registrar(context, session),
            sessionStore = session,
            catalogRepository = catalog,
        )
}

internal fun sampleProduct(
    id: Int,
    name: String,
    station: PreparationStation = PreparationStation.KITCHEN,
) = Product(
    id = id,
    categoryId = 1,
    preparationStation = station,
    name = name,
    description = "",
    ingredients = "",
    allergens = "",
    estimatedTimeMinutes = 5,
    counterPrice = "50.00",
    digitalPrice = "45.00",
    available = true,
    imageUrl = "",
    optionGroups = emptyList(),
)

internal fun sampleOrder(
    id: String,
    state: OrderState,
    folio: Int = 7,
    version: Int = 1,
) = OrderDetail(
    summary =
        OrderSummary(
            id = id,
            folio = folio,
            operationalDate = "2026-11-07",
            state = state,
            paymentMethod = PaymentMethod.BALANCE,
            destination = OrderDestination.TAKE_AWAY,
            space = null,
            subtotal = "50.00",
            combinedSavings = "0.00",
            cashbackAwarded = "0.00",
            total = "50.00",
            version = version,
            createdAt = "2026-11-07T10:00:00Z",
            updatedAt = "2026-11-07T10:00:00Z",
        ),
    user = null,
    kitchenNotes = "",
    items = emptyList(),
    pickupToken = null,
)

internal class FakeSessionStore(
    var token: String? = null,
) : SecureSessionStore {
    override fun saveAccessToken(token: String) {
        this.token = token
    }

    override fun readAccessToken(): String? = token

    override fun clear() {
        token = null
    }
}

internal class FakeHeartbeat : DeviceHeartbeatRepository {
    override fun sendHeartbeat(
        deviceId: String,
        role: OperationalRole,
    ): Result<Unit> = Result.success(Unit)
}

internal class FakeDevice : DeviceIdentity {
    override fun id(): String = "device-1"
}

internal class FakeCatalog : CatalogRepository {
    @Volatile var products: List<Product> = emptyList()

    @Volatile var availabilityFails: Throwable? = null
    val availabilityCalls = java.util.concurrent.CopyOnWriteArrayList<Pair<Int, Boolean>>()

    override fun getCatalog(): Result<Catalog> = Result.success(Catalog(emptyList(), products, null))

    override fun getOperationalStatus(): Result<OperationalStatus> = Result.failure(IllegalStateException("sin estado"))

    override fun createProduct(
        draft: CatalogProductDraft,
        idempotencyKey: String,
    ): Result<Product> = Result.failure(IllegalStateException("no"))

    override fun setProductAvailability(
        productId: Int,
        available: Boolean,
        idempotencyKey: String,
    ): Result<Product> {
        availabilityCalls += productId to available
        availabilityFails?.let { return Result.failure(it) }
        val updated = products.first { it.id == productId }.copy(available = available)
        products = products.map { if (it.id == productId) updated else it }
        return Result.success(updated)
    }

    override fun uploadProductImage(
        productId: Int,
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        idempotencyKey: String,
    ): Result<Product> = Result.failure(IllegalStateException("no"))
}

internal class FakeWallet : WalletRepository {
    @Volatile var clients: List<WalletClient> = emptyList()
    val reloadCalls = java.util.concurrent.CopyOnWriteArrayList<Triple<String, String, String>>()

    override fun getMyWallet(): Result<WalletData> = Result.failure(IllegalStateException("no"))

    override fun searchClients(query: String): Result<List<WalletClient>> =
        Result.success(clients.filter { it.name.contains(query, ignoreCase = true) })

    override fun lookupClient(userId: String): Result<WalletClient> = Result.failure(IllegalStateException("no"))

    override fun reloadCash(
        userId: String,
        amount: String,
        idempotencyKey: String,
    ): Result<WalletReloadReceipt> {
        reloadCalls += Triple(userId, amount, idempotencyKey)
        return Result.success(WalletReloadReceipt(userId, "50.00", amount, "150.00", "mov-1", "sesion-1"))
    }
}

internal class FakeCashCut : CashCutRepository {
    override fun shiftCut(sessionId: String): Result<ShiftCut> = Result.failure(IllegalStateException("no"))

    override fun dayCut(date: LocalDate?): Result<DayCut> = Result.failure(IllegalStateException("no"))

    override fun accounting(
        from: LocalDate,
        to: LocalDate,
    ): Result<AccountingReport> = Result.failure(IllegalStateException("no"))

    override fun accountingCsv(
        from: LocalDate,
        to: LocalDate,
    ): Result<String> = Result.failure(IllegalStateException("no"))

    override fun dayStart(): Result<String?> = Result.success(null)

    override fun setDayStart(hhmm: String): Result<String> = Result.failure(IllegalStateException("no"))
}

internal class FakeOrders : OrderRepository {
    @Volatile var orders: List<OrderDetail> = emptyList()
    val transitionCalls = java.util.concurrent.CopyOnWriteArrayList<Triple<String, OrderState, Int>>()

    val createdRequests = java.util.concurrent.CopyOnWriteArrayList<Pair<CreateOrderRequest, String>>()

    @Volatile var createFails: Throwable? = null

    override fun createOrder(
        request: CreateOrderRequest,
        idempotencyKey: String,
    ): Result<CreatedOrder> {
        createdRequests += request to idempotencyKey
        createFails?.let { return Result.failure(it) }
        return Result.success(CreatedOrder(sampleOrder("nuevo-${createdRequests.size}", OrderState.PAID, folio = 7)))
    }

    override fun offlineOrderBody(
        request: CreateOrderRequest,
        orderId: String,
        capturedAt: java.time.Instant,
        localFolio: String,
        collectNow: Boolean,
    ): String = """{"id":"$orderId","folio_local":"$localFolio"}"""

    override fun getOrder(orderId: String): Result<OrderDetail> =
        orders.firstOrNull { it.summary.id == orderId }?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("no existe"))

    override fun listOrders(
        role: OperationalRole,
        updatedSince: String?,
    ): Result<List<OrderDetail>> = Result.success(orders)

    override fun retryStripePayment(
        orderId: String,
        idempotencyKey: String,
    ): Result<StripePaymentSession> = Result.failure(IllegalStateException("no"))

    override fun collectCash(
        orderId: String,
        amountReceived: String,
        expectedVersion: Int,
        idempotencyKey: String,
        tip: String?,
    ): Result<CashCollectionResult> = Result.failure(IllegalStateException("no"))

    override fun transition(
        orderId: String,
        targetState: OrderState,
        expectedVersion: Int,
        idempotencyKey: String,
        pickupToken: String?,
    ): Result<OrderDetail> {
        transitionCalls += Triple(orderId, targetState, expectedVersion)
        val current = orders.firstOrNull { it.summary.id == orderId } ?: return Result.failure(IllegalStateException("no existe"))
        val moved = current.copy(summary = current.summary.copy(state = targetState, version = expectedVersion + 1))
        orders = orders.map { if (it.summary.id == orderId) moved else it }
        return Result.success(moved)
    }
}

internal class FakeCashSession : CashSessionRepository {
    @Volatile var sessionOpen = false

    @Volatile var openFails: Throwable? = null

    @Volatile var closeFails: Throwable? = null
    val openCalls = java.util.concurrent.CopyOnWriteArrayList<Pair<String, String>>()
    val closeCalls = java.util.concurrent.CopyOnWriteArrayList<Triple<String, String, String>>()

    /** Tras un fallo de red del POST, el servidor sí abrió la caja. */
    @Volatile var openLandsDespiteError = false

    override fun openSession(
        initialAmount: String,
        idempotencyKey: String,
    ): Result<Unit> {
        openCalls += initialAmount to idempotencyKey
        openFails?.let {
            if (openLandsDespiteError) sessionOpen = true
            return Result.failure(it)
        }
        sessionOpen = true
        return Result.success(Unit)
    }

    override fun hasActiveSession(): Result<Boolean> = Result.success(sessionOpen)

    override fun activeSessionId(): Result<String?> = Result.success(if (sessionOpen) "sesion-1" else null)

    override fun closeSession(
        sessionId: String,
        finalAmount: String,
        idempotencyKey: String,
    ): Result<Unit> {
        closeCalls += Triple(sessionId, finalAmount, idempotencyKey)
        closeFails?.let { return Result.failure(it) }
        sessionOpen = false
        return Result.success(Unit)
    }
}
