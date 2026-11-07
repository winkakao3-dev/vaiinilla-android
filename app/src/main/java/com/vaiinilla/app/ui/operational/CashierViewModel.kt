package com.vaiinilla.app.ui.operational

import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.cashcut.CashCutRepository
import com.vaiinilla.app.data.wallet.PendingWalletReload
import com.vaiinilla.app.data.wallet.PendingWalletReloadStore
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.cashAmountDue
import com.vaiinilla.app.domain.repository.CashDrawerMovementType
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import com.vaiinilla.app.domain.repository.DeviceIdentity
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import com.vaiinilla.app.domain.repository.StoreImageKind
import com.vaiinilla.app.domain.repository.WalletRepository
import com.vaiinilla.app.domain.repository.WalletRepositoryException
import com.vaiinilla.app.domain.usecase.CancelOrderUseCase
import com.vaiinilla.app.domain.usecase.CloseCashSessionUseCase
import com.vaiinilla.app.domain.usecase.CollectCashUseCase
import com.vaiinilla.app.domain.usecase.GetOrderUseCase
import com.vaiinilla.app.domain.usecase.ListOrdersUseCase
import com.vaiinilla.app.domain.usecase.OpenCashSessionUseCase
import com.vaiinilla.app.domain.usecase.TransitionOrderUseCase
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.discovery.QrPayload
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/** Caja: el cobro en efectivo, la apertura y el cierre, el cajón, las recargas de saldo y el menú propio. */
@HiltViewModel
class CashierViewModel
    @Inject
    constructor(
        listOrders: ListOrdersUseCase,
        getOrder: GetOrderUseCase,
        transitionOrder: TransitionOrderUseCase,
        cancelOrder: CancelOrderUseCase,
        heartbeatRepository: DeviceHeartbeatRepository,
        deviceIdentity: DeviceIdentity,
        deviceTokenRegistrar: DeviceTokenRegistrar,
        sessionStore: SecureSessionStore,
        catalogRepository: CatalogRepository,
        private val collectCash: CollectCashUseCase,
        private val openCashSession: OpenCashSessionUseCase,
        private val closeCashSession: CloseCashSessionUseCase,
        private val cashSessionRepository: CashSessionRepository,
        private val walletRepository: WalletRepository,
        private val pendingWalletReloadStore: PendingWalletReloadStore,
        private val cashCutRepository: CashCutRepository,
    ) : StaffOrdersViewModel(
            role = OperationalRole.CASHIER,
            listOrders = listOrders,
            getOrder = getOrder,
            transitionOrder = transitionOrder,
            cancelOrder = cancelOrder,
            heartbeatRepository = heartbeatRepository,
            deviceIdentity = deviceIdentity,
            deviceTokenRegistrar = deviceTokenRegistrar,
            sessionStore = sessionStore,
            catalogRepository = catalogRepository,
        ) {
        private var pendingWalletReload: PendingWalletReload? = null
        private var walletSearchJob: Job? = null
        private var cashDrawerJob: Job? = null

        /** La llave del movimiento del cajón que falló por red: el reintento del mismo no lo anota dos veces. */
        private var drawerMovementKey: Pair<String, String>? = null

        // Un cobro que terminó cuando el cajero ya había salido de la pantalla se avisa al volver.
        private var deferredCashierNotice: String? = null

        // El negocio del aviso pendiente: el cobro de un negocio no se avisa en la Caja de otro (en vivo, #10).
        private var deferredNoticeEstablishmentId: String? = null

        override fun onRoleReset(establishmentChanged: Boolean) {
            walletSearchJob?.cancel()
            walletSearchJob = null
            pendingWalletReload = null
        }

        override fun onRoleStarted() {
            pendingWalletReload = null
            surfaceDeferredCashierNotice()
        }

        override fun onPolled() = refreshCashSession()

        override fun onAfterFirstRefresh() {
            refreshCatalog()
            reconcilePendingWalletReload()
        }

        override fun onRoleCleared() {
            cashSessionRepository.forgetLastClose()
        }

        override fun refreshCatalog() {
            loadStoreImages()
            val generation = roleGeneration
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { catalogRepository.getCatalog() }
                if (generation != roleGeneration || state.value.role != OperationalRole.CASHIER) return@launch
                state.value =
                    state.value.copy(
                        catalog = result.getOrNull(),
                        errorMessage =
                            when {
                                mutationErrorVisible -> state.value.errorMessage
                                result.isFailure ->
                                    result.exceptionOrNull().toUserFacingMessage(
                                        state.value.errorMessage ?: "No se pudo cargar el catálogo.",
                                    )
                                // Un error de la carga de pedidos sigue ahí hasta que esa carga salga bien.
                                else -> state.value.errorMessage.takeIf { state.value.lastRefreshOk == false }
                            },
                    )
            }
        }

        /** Abre la caja con el fondo que el cajero contó; el servidor lo guarda como monto inicial del arqueo. */
        fun openCashRegister(initialAmount: String) {
            if (state.value.role != OperationalRole.CASHIER || state.value.acting) return
            val wireAmount = CashierMoneyRules.openingWireAmount(initialAmount)
            if (wireAmount == null) {
                showMutationError(CashierMoneyRules.OPENING_AMOUNT_ERROR)
                return
            }
            val generation = roleGeneration
            mutationErrorVisible = false
            state.value = state.value.copy(acting = true, errorMessage = null)
            mutationJob =
                viewModelScope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            openCashSession(wireAmount, retryKeys.keyFor("open-cash", wireAmount))
                        }
                    if (generation != roleGeneration || state.value.role != OperationalRole.CASHIER) return@launch
                    result
                        .onSuccess {
                            retryKeys.forget("open-cash", wireAmount)
                            state.value = state.value.copy(acting = false, cashSessionOpen = true)
                        }.onFailure { error ->
                            // Si el POST llegó pero la respuesta se perdió, la sesión ya
                            // existe: releer el estado real evita que un reintento manual
                            // abra una segunda sesión o muestre un error espurio.
                            val alreadyOpen =
                                withContext(Dispatchers.IO) {
                                    cashSessionRepository.hasActiveSession().getOrDefault(false)
                                }
                            state.value =
                                state.value.copy(
                                    acting = false,
                                    cashSessionOpen = if (alreadyOpen) true else state.value.cashSessionOpen,
                                )
                            if (!alreadyOpen) {
                                showMutationError(error.toUserFacingMessage())
                            }
                        }
                }
        }

        fun requestCloseCash() {
            if (state.value.role != OperationalRole.CASHIER || state.value.cashSessionOpen != true) return
            state.value = state.value.copy(closeCash = CloseCashUi())
            // El cierre enseña el mismo desglose que el corte (R37).
            loadCashDrawer()
        }

        /** Lee el resumen y los renglones del cajón de la caja abierta (N11). */
        fun loadCashDrawer() {
            if (state.value.role != OperationalRole.CASHIER || state.value.cashSessionOpen != true) return
            if (cashDrawerJob?.isActive == true) return
            val generation = roleGeneration
            state.value =
                state.value.copy(cashDrawer = state.value.cashDrawer.copy(loading = true, errorMessage = null))
            cashDrawerJob =
                viewModelScope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            cashSessionRepository.activeSessionId().mapCatching { id ->
                                val sessionId = id ?: error("No hay una caja abierta.")
                                cashSessionRepository.drawerReport(sessionId).getOrThrow()
                            }
                        }
                    // «Cobrado hoy» con todos los métodos; si falla se queda lo último (o lo del cajón en efectivo).
                    val day = withContext(Dispatchers.IO) { cashCutRepository.dayCut(null) }.getOrNull()
                    if (generation != roleGeneration || state.value.role != OperationalRole.CASHIER) return@launch
                    day?.let { state.value = state.value.copy(collectedToday = CollectedToday.of(it)) }
                    val drawer = state.value.cashDrawer
                    state.value =
                        result.fold(
                            onSuccess = { report ->
                                state.value.copy(
                                    cashDrawer = drawer.copy(loading = false, report = report, errorMessage = null),
                                    expectedCash = report.summary.expected,
                                )
                            },
                            onFailure = { error ->
                                state.value.copy(
                                    cashDrawer =
                                        drawer.copy(
                                            loading = false,
                                            errorMessage = error.toUserFacingMessage("No se pudo leer el cajón."),
                                        ),
                                    // Lo que haya respondido la consulta de la caja sigue valiendo.
                                    expectedCash = cashSessionRepository.expectedCash ?: state.value.expectedCash,
                                )
                            },
                        )
                }
        }

        /** Caja mete o saca efectivo del cajón con su motivo; el esperado y el cierre ya lo cuentan. */
        fun registerDrawerMovement(
            type: CashDrawerMovementType,
            amount: String,
            reason: String,
        ) {
            if (state.value.role != OperationalRole.CASHIER || state.value.cashSessionOpen != true) return
            val drawer = state.value.cashDrawer
            if (drawer.saving) return
            val movementError = CashierMoneyRules.drawerMovementError(amount, reason)
            if (movementError != null) {
                state.value = state.value.copy(cashDrawer = drawer.copy(saveError = movementError))
                return
            }
            val fingerprint = listOf(type.wireValue, amount, reason.trim()).joinToString("|")
            val key =
                drawerMovementKey?.takeIf { it.first == fingerprint }?.second ?: UUID.randomUUID().toString()
            drawerMovementKey = fingerprint to key
            val generation = roleGeneration
            state.value = state.value.copy(cashDrawer = drawer.copy(saving = true, saveError = null))
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        cashSessionRepository.activeSessionId().mapCatching { id ->
                            val sessionId = id ?: error("No hay una caja abierta.")
                            cashSessionRepository
                                .registerDrawerMovement(sessionId, type, amount, reason, key)
                                .getOrThrow()
                        }
                    }
                if (generation != roleGeneration || state.value.role != OperationalRole.CASHIER) return@launch
                val current = state.value.cashDrawer
                result
                    .onSuccess { expected ->
                        drawerMovementKey = null
                        state.value =
                            state.value.copy(
                                cashDrawer =
                                    current.copy(saving = false, saveError = null, savedCount = current.savedCount + 1),
                                expectedCash = expected ?: state.value.expectedCash,
                                actionNotice =
                                    if (type == CashDrawerMovementType.IN) {
                                        "Entrada de ${moneyLabel(amount)} anotada"
                                    } else {
                                        "Salida de ${moneyLabel(amount)} anotada"
                                    },
                            )
                        loadCashDrawer()
                    }.onFailure { error ->
                        state.value =
                            state.value.copy(
                                cashDrawer =
                                    current.copy(
                                        saving = false,
                                        saveError = error.toUserFacingMessage("No se pudo anotar el movimiento."),
                                    ),
                            )
                    }
            }
        }

        /** Lo dice la cola de pedidos sin internet de Caja; el cierre se bloquea mientras haya alguno. */
        fun setUnsyncedOfflineCash(count: Int) {
            if (state.value.unsyncedOfflineCash == count) return
            state.value = state.value.copy(unsyncedOfflineCash = count)
        }

        fun dismissCloseCash() {
            if (state.value.closeCash?.closing == true) return
            state.value = state.value.copy(closeCash = null)
        }

        /** Cierra la caja con el efectivo contado. Con cuentas por cobrar el servidor lo rechaza y se avisa. */
        fun closeCashRegister(finalAmount: String) {
            val current = state.value.closeCash ?: return
            if (state.value.role != OperationalRole.CASHIER || current.closing) return
            // Efectivo cobrado sin internet que aún no se registra: cerrar ahora lo deja fuera del arqueo y, al
            // sincronizar, caería en otra caja o quedaría por cobrar (y se cobraría dos veces).
            val unsynced = state.value.unsyncedOfflineCash
            if (unsynced > 0) {
                state.value =
                    state.value.copy(closeCash = current.copy(errorMessage = unsyncedOfflineCashNotice(unsynced)))
                return
            }
            val generation = roleGeneration
            state.value = state.value.copy(closeCash = CloseCashUi(closing = true))
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        cashSessionRepository.activeSessionId().mapCatching { id ->
                            val sessionId = id ?: error("No hay una caja abierta.")
                            val key = retryKeys.keyFor("close-cash", sessionId, finalAmount)
                            closeCashSession(sessionId, finalAmount, key)
                                .getOrThrow()
                                .also { retryKeys.forget("close-cash", sessionId, finalAmount) }
                        }
                    }
                if (generation != roleGeneration || state.value.role != OperationalRole.CASHIER) return@launch
                result
                    .onSuccess {
                        state.value =
                            state.value.copy(closeCash = null, cashSessionOpen = false, cashDrawer = CashDrawerUi())
                        // Al cerrar, el servidor expira los pedidos por cobrar: que no queden como cobrables.
                        requestFullSync()
                    }.onFailure { error ->
                        val blocked =
                            error is OrderRepositoryException && error.code == CASH_HAS_OPEN_ACCOUNTS_CODE
                        // Si la respuesta del cierre se perdió, la caja ya pudo cerrarse: se relee el estado real
                        // en vez de dejar "abierta" en pantalla y un error que el reintento no puede arreglar.
                        val stillOpen =
                            if (blocked) {
                                true
                            } else {
                                withContext(Dispatchers.IO) { cashSessionRepository.hasActiveSession() }
                                    .getOrDefault(true)
                            }
                        if (generation != roleGeneration || state.value.role != OperationalRole.CASHIER) {
                            return@launch
                        }
                        state.value =
                            if (stillOpen) {
                                state.value.copy(
                                    closeCash =
                                        CloseCashUi(
                                            blockedByAccounts = blocked,
                                            errorMessage = if (blocked) null else error.toUserFacingMessage(),
                                        ),
                                )
                            } else {
                                state.value.copy(closeCash = null, cashSessionOpen = false)
                            }
                        if (!stillOpen) requestFullSync()
                    }
            }
        }

        fun resolveWalletUserQr(rawValue: String) {
            if (state.value.role != OperationalRole.CASHIER) return
            val payload =
                QrPayloadParser
                    .parse(rawValue)
                    .getOrElse { error ->
                        showMutationError(error.toUserFacingMessage("No se pudo leer el QR."))
                        return
                    }
            val userId = (payload as? QrPayload.User)?.userId
            if (userId == null) {
                showMutationError("Ese QR no es de un alumno. Pide el código de su cuenta.")
                return
            }
            walletSearchJob?.cancel()
            val generation = roleGeneration
            mutationErrorVisible = false
            state.value = state.value.copy(walletSearchLoading = true, errorMessage = null)
            walletSearchJob =
                viewModelScope.launch {
                    // El QR trae solo un id: se resuelve a nombre antes de pedir el monto, para que el cajero
                    // vea a quién abona y un QR ajeno o inexistente se rechace aquí.
                    val result = withContext(Dispatchers.IO) { walletRepository.lookupClient(userId) }
                    if (generation != roleGeneration || state.value.role != OperationalRole.CASHIER) return@launch
                    result
                        .onSuccess { client ->
                            state.value =
                                state.value.copy(walletClients = listOf(client), walletSearchLoading = false)
                        }.onFailure { error ->
                            state.value =
                                state.value.copy(walletClients = emptyList(), walletSearchLoading = false)
                            val notClient = (error as? WalletRepositoryException)?.httpStatus == 404
                            showMutationError(
                                if (notClient) {
                                    "Ese QR no es de un cliente de este negocio."
                                } else {
                                    error.toUserFacingMessage("No se pudo identificar al cliente. Inténtalo otra vez.")
                                },
                            )
                        }
                }
        }

        fun searchWalletClients(query: String) {
            if (state.value.role != OperationalRole.CASHIER) return
            val normalizedQuery = query.trim()
            if (normalizedQuery.length < 2) {
                walletSearchJob?.cancel()
                walletSearchJob = null
                state.value =
                    state.value.copy(
                        walletClients = emptyList(),
                        walletSearchLoading = false,
                    )
                showMutationError("Escribe al menos 2 caracteres para buscar un cliente.")
                return
            }
            walletSearchJob?.cancel()
            val generation = roleGeneration
            mutationErrorVisible = false
            state.value = state.value.copy(walletSearchLoading = true, errorMessage = null)
            walletSearchJob =
                viewModelScope.launch {
                    val result = withContext(Dispatchers.IO) { walletRepository.searchClients(normalizedQuery) }
                    if (generation != roleGeneration || state.value.role != OperationalRole.CASHIER) return@launch
                    result
                        .onSuccess { clients ->
                            state.value =
                                state.value.copy(
                                    walletClients = clients,
                                    walletSearchLoading = false,
                                )
                        }.onFailure { error ->
                            state.value =
                                state.value.copy(
                                    walletClients = emptyList(),
                                    walletSearchLoading = false,
                                )
                            showMutationError(error.toUserFacingMessage("No se pudieron buscar clientes."))
                        }
                }
        }

        fun reloadWallet(
            userId: String,
            amount: String,
        ) {
            if (
                state.value.role != OperationalRole.CASHIER ||
                state.value.cashSessionOpen != true ||
                state.value.acting
            ) {
                return
            }
            val normalizedAmount =
                when (val check = CashierMoneyRules.checkReload(amount)) {
                    is CashierMoneyRules.ReloadCheck.Valid -> check.amount
                    is CashierMoneyRules.ReloadCheck.Invalid -> {
                        showMutationError(check.message)
                        return
                    }
                }
            val stored = pendingWalletReloadStore.read()
            if (stored != null && (stored.userId != userId || stored.amount != normalizedAmount)) {
                publishPendingWalletReload(stored)
                showMutationError(CashierMoneyRules.pendingReloadBlockMessage(stored))
                return
            }
            val userName =
                state.value.walletClients
                    .firstOrNull { it.userId == userId }
                    ?.name
            val attempt =
                stored
                    ?: PendingWalletReload(
                        userId = userId,
                        amount = normalizedAmount,
                        idempotencyKey = UUID.randomUUID().toString(),
                        storedAtEpochMs = System.currentTimeMillis(),
                        userName = userName,
                    ).also {
                        // Persistir ANTES del POST: si el proceso muere tras el abono,
                        // la misma Idempotency-Key sobrevive y el retry confirma.
                        pendingWalletReloadStore.write(it)
                        pendingWalletReload = it
                    }
            val generation = roleGeneration
            mutationErrorVisible = false
            state.value = state.value.copy(acting = true, errorMessage = null)
            mutationJob =
                viewModelScope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            walletRepository.reloadCash(attempt.userId, attempt.amount, attempt.idempotencyKey)
                        }
                    if (generation != roleGeneration || state.value.role != OperationalRole.CASHIER) return@launch
                    result
                        .onSuccess { receipt ->
                            pendingWalletReload = null
                            pendingWalletReloadStore.clear()
                            state.value =
                                state.value.copy(
                                    acting = false,
                                    walletReloadReceipt = receipt,
                                    pendingWalletReload = null,
                                    errorMessage = null,
                                )
                        }.onFailure { error ->
                            if (isDefinitiveWalletReloadFailure(error)) {
                                // El servidor procesó y rechazó la recarga: nada quedó abonado.
                                pendingWalletReload = null
                                pendingWalletReloadStore.clear()
                            }
                            state.value = state.value.copy(acting = false)
                            showMutationError(error.toUserFacingMessage("No se pudo registrar la recarga."))
                        }
                }
        }

        /**
         * Al abrir Caja con una recarga persistida (el proceso murió a mitad del POST): si es reciente se
         * confirma sola con la MISMA Idempotency-Key, que es segura; si es vieja se deja a la vista para que el
         * cajero decida, porque abonar saldo días después de un cobro que nadie recuerda no es confirmar.
         */
        private fun reconcilePendingWalletReload() {
            val pending = pendingWalletReloadStore.read() ?: return
            pendingWalletReload = pending
            if (isStalePendingReload(pending, System.currentTimeMillis())) {
                publishPendingWalletReload(pending)
                return
            }
            replayPendingWalletReload(pending, userInitiated = false)
        }

        /** El cajero confirma la recarga pendiente que ve en la hoja. */
        fun confirmPendingWalletReload() {
            val pending = pendingWalletReloadStore.read() ?: return
            if (state.value.role != OperationalRole.CASHIER || state.value.acting) return
            replayPendingWalletReload(pending, userInitiated = true)
        }

        /** El cajero descarta la recarga pendiente: no se abonó nada o ya se resolvió fuera de la app. */
        fun discardPendingWalletReload() {
            pendingWalletReload = null
            pendingWalletReloadStore.clear()
            state.value = state.value.copy(pendingWalletReload = null)
        }

        private fun publishPendingWalletReload(pending: PendingWalletReload) {
            state.value =
                state.value.copy(
                    pendingWalletReload = PendingReloadUi(userName = pending.userName, amount = pending.amount),
                )
        }

        private fun replayPendingWalletReload(
            pending: PendingWalletReload,
            userInitiated: Boolean,
        ) {
            val generation = roleGeneration
            if (userInitiated) state.value = state.value.copy(acting = true, errorMessage = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        walletRepository.reloadCash(pending.userId, pending.amount, pending.idempotencyKey)
                    }
                if (generation != roleGeneration) return@launch
                val isCashier = state.value.role == OperationalRole.CASHIER
                result
                    .onSuccess { receipt ->
                        pendingWalletReload = null
                        pendingWalletReloadStore.clear()
                        if (isCashier) {
                            state.value =
                                state.value.copy(
                                    acting = false,
                                    walletReloadReceipt = receipt,
                                    pendingWalletReload = null,
                                    // La hoja de recarga no está abierta al confirmar en segundo plano.
                                    actionNotice = "Se confirmó la recarga pendiente de ${moneyLabel(pending.amount)}",
                                    errorMessage = null,
                                )
                        }
                    }.onFailure { error ->
                        if (isDefinitiveWalletReloadFailure(error)) {
                            pendingWalletReload = null
                            pendingWalletReloadStore.clear()
                        }
                        if (isCashier) {
                            val stillPending = pendingWalletReloadStore.read()
                            state.value =
                                state.value.copy(
                                    acting = false,
                                    pendingWalletReload =
                                        stillPending?.let {
                                            PendingReloadUi(userName = it.userName, amount = it.amount)
                                        },
                                )
                            if (userInitiated || stillPending != null) {
                                showMutationError(
                                    "No se pudo confirmar la recarga pendiente (${pendingReloadLabel(pending)}). " +
                                        "Inténtalo otra vez o descártala.",
                                )
                            }
                        }
                    }
            }
        }

        fun collectCash(
            orderId: String,
            amountReceived: String,
            expectedVersion: Int,
            tip: String? = null,
        ) {
            val role = state.value.role ?: return
            // Dos toques en el mismo cuadro lanzarían dos cobros y perderían el rastro del primero.
            if (state.value.acting) return
            // El ticket pudo abrirse hace varios ciclos de polling: cobrar con la
            // versión que el servidor tiene ahora, no con la del render viejo.
            val freshest = state.value.orders.firstOrNull { it.summary.id == orderId }
            val version = freshest?.summary?.version ?: expectedVersion
            val cashDue = freshest?.summary?.cashAmountDue
            val normalizedAmount =
                when (val check = CashierMoneyRules.checkCollection(amountReceived, cashDue, tip)) {
                    is CashierMoneyRules.CollectionCheck.Valid -> check.wireAmount
                    is CashierMoneyRules.CollectionCheck.Invalid -> {
                        showMutationError(check.message)
                        return
                    }
                }
            val generation = roleGeneration
            val establishmentAtStart = roleEstablishmentId
            mutationErrorVisible = false
            state.value =
                state.value.copy(acting = true, errorMessage = null, cashChangeNotice = null)
            mutationJob =
                viewModelScope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            collectCash(
                                orderId = orderId,
                                amountReceived = normalizedAmount,
                                expectedVersion = version,
                                // La propina es parte del cobro: otra propina es otro cobro.
                                idempotencyKey =
                                    MutationIdempotency.cashCollection(
                                        orderId,
                                        normalizedAmount + "|" + tip.orEmpty(),
                                        version,
                                    ),
                                tip = tip,
                            )
                        }
                    if (generation != roleGeneration || state.value.role != role) {
                        // El cajero salió de la pantalla con el cobro en vuelo: el dinero ya se movió, así que
                        // se le dice al volver en vez de dejarlo sin saber si cobró.
                        result.onSuccess { receipt ->
                            deferredCashierNotice = cashCollectionNotice(receipt)
                            deferredNoticeEstablishmentId = establishmentAtStart
                        }
                        return@launch
                    }
                    result
                        .map { receipt ->
                            // La respuesta del cobro trae la reserva; si no, se conserva la que ya se veía.
                            val reservation = receipt.order.reservation ?: freshest?.reservation
                            receipt.copy(order = receipt.order.copy(reservation = reservation))
                        }.onSuccess { receipt ->
                            val updated =
                                state.value.orders
                                    .filterNot { it.summary.id == receipt.order.summary.id } + receipt.order
                            state.value =
                                state.value.copy(
                                    acting = false,
                                    orders = updated.sortedByDescending { it.summary.updatedAt },
                                    selectedOrderId = receipt.order.summary.id,
                                    cashChangeNotice = cashCollectionNotice(receipt),
                                )
                            // Cobrado el pedido, el servidor deja de ofrecerlo a Caja: se concilia la lista.
                            requestFullSync()
                        }.onFailure { error ->
                            state.value = state.value.copy(acting = false)
                            showMutationError(error.toUserFacingMessage())
                            if (isStaleOrderError(error)) requestFullSync()
                        }
                }
        }

        private fun surfaceDeferredCashierNotice() {
            val notice = deferredCashierNotice ?: return
            val noticeEstablishment = deferredNoticeEstablishmentId
            deferredCashierNotice = null
            deferredNoticeEstablishmentId = null
            if (isOtherEstablishment(noticeEstablishment, roleEstablishmentId)) return
            state.value = state.value.copy(cashChangeNotice = notice)
        }

        fun consumeCashChangeNotice() {
            if (state.value.cashChangeNotice != null) {
                state.value = state.value.copy(cashChangeNotice = null)
            }
        }

        /** Relee si la caja está abierta; otro dispositivo o el administrador pudo abrirla o cerrarla. */
        fun refreshCashSession() {
            if (state.value.role != OperationalRole.CASHIER) return
            val generation = roleGeneration
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { cashSessionRepository.hasActiveSession() }
                if (generation != roleGeneration || state.value.role != OperationalRole.CASHIER) return@launch
                result
                    .onSuccess { open ->
                        val session = cashSessionRepository.openedSession.takeIf { open }
                        state.value =
                            state.value.copy(
                                cashSessionOpen = open,
                                cashSessionCheckFailed = false,
                                // Otra caja la cerró: la hoja de cierre ya no tiene qué cerrar.
                                closeCash = state.value.closeCash.takeIf { open },
                                suggestedCashFloat = cashSessionRepository.suggestedFloat,
                                expectedCash = cashSessionRepository.expectedCash,
                                cashOpeningAmount = session?.initialAmount,
                                cashOpenedAt = session?.openedAt,
                            )
                    }.onFailure {
                        // Con la caja ya conocida se conserva lo último; solo si nunca se supo, la pantalla pide reintentar.
                        state.value =
                            state.value.copy(cashSessionCheckFailed = state.value.cashSessionOpen == null)
                    }
            }
        }

        /** Un servidor que aún no da las imágenes a Caja responde 403: la tarjeta simplemente no aparece. */
        private fun loadStoreImages() {
            val generation = roleGeneration
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { catalogRepository.getStoreImages() }
                if (generation != roleGeneration) return@launch
                result.onSuccess { images -> state.value = state.value.copy(storeImages = images) }
            }
        }

        fun uploadStoreImage(
            kind: StoreImageKind,
            bytes: ByteArray,
            filename: String,
            mimeType: String,
        ) {
            if (state.value.storeImageBusy != null) return
            if (bytes.size > MAX_PRODUCT_IMAGE_BYTES) {
                state.value = state.value.copy(storeImageNotice = "La imagen no puede pesar más de 5 MB.")
                return
            }
            state.value = state.value.copy(storeImageBusy = kind, storeImageNotice = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        catalogRepository.uploadStoreImage(
                            kind = kind,
                            bytes = bytes,
                            filename = filename,
                            mimeType = mimeType,
                            idempotencyKey = MutationIdempotency.storeImage(kind.wireValue, bytes),
                        )
                    }
                state.value =
                    result.fold(
                        onSuccess = { images ->
                            state.value.copy(
                                storeImages = images,
                                storeImageBusy = null,
                                storeImageNotice = storeImageDone(kind, removed = false),
                            )
                        },
                        onFailure = { error ->
                            state.value.copy(
                                storeImageBusy = null,
                                storeImageNotice = error.toUserFacingMessage("No se pudo subir la imagen."),
                            )
                        },
                    )
            }
        }

        fun removeStoreImage(kind: StoreImageKind) {
            if (state.value.storeImageBusy != null) return
            state.value = state.value.copy(storeImageBusy = kind, storeImageNotice = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        catalogRepository.removeStoreImage(kind, UUID.randomUUID().toString())
                    }
                state.value =
                    result.fold(
                        onSuccess = { images ->
                            state.value.copy(
                                storeImages = images,
                                storeImageBusy = null,
                                storeImageNotice = storeImageDone(kind, removed = true),
                            )
                        },
                        onFailure = { error ->
                            state.value.copy(
                                storeImageBusy = null,
                                storeImageNotice = error.toUserFacingMessage("No se pudo quitar la imagen."),
                            )
                        },
                    )
            }
        }

        private fun storeImageDone(
            kind: StoreImageKind,
            removed: Boolean,
        ): String =
            when (kind) {
                StoreImageKind.LOGO -> if (removed) "Logo quitado." else "Logo actualizado. Tus clientes ya lo ven."
                StoreImageKind.PHOTO ->
                    if (removed) "Foto del lugar quitada." else "Foto del lugar actualizada. Tus clientes ya la ven."
            }

        fun createCashierProduct(
            draft: CatalogProductDraft,
            imageBytes: ByteArray? = null,
            imageFilename: String? = null,
            imageMime: String? = null,
            onSuccess: (String?) -> Unit = {},
        ) {
            if (imageBytes != null && imageBytes.size > MAX_PRODUCT_IMAGE_BYTES) {
                showMutationError("La foto no puede pesar más de 5 MB.")
                return
            }
            if (state.value.catalogBusy) return
            mutationErrorVisible = false
            state.value = state.value.copy(catalogBusy = true, errorMessage = null)
            viewModelScope.launch {
                val created =
                    withContext(Dispatchers.IO) {
                        catalogRepository.createProduct(draft, retryKeys.keyFor("create-product", draft))
                    }
                created.onSuccess { retryKeys.forget("create-product", draft) }
                val createdProduct =
                    created
                        .getOrElse { error ->
                            state.value = state.value.copy(catalogBusy = false)
                            showMutationError(error.toUserFacingMessage("No se pudo crear el producto."))
                            // Si el POST llegó pero la respuesta se perdió, el producto ya
                            // existe: el refresh lo muestra en lugar de dejar un duplicado.
                            refreshCatalog()
                            return@launch
                        }
                var finalProduct = createdProduct
                if (imageBytes != null && imageFilename != null && imageMime != null) {
                    val uploaded =
                        withContext(Dispatchers.IO) {
                            catalogRepository.uploadProductImage(
                                productId = createdProduct.id,
                                bytes = imageBytes,
                                filename = imageFilename,
                                mimeType = imageMime,
                                idempotencyKey = MutationIdempotency.productImage(createdProduct.id, imageBytes),
                            )
                        }
                    if (uploaded.isFailure) {
                        val catalog = state.value.catalog
                        val warning =
                            uploaded.exceptionOrNull().toUserFacingMessage(
                                "El producto se creó, pero la foto no se subió.",
                            )
                        state.value =
                            state.value.copy(
                                catalogBusy = false,
                                catalog =
                                    catalog?.copy(
                                        products = catalog.products + createdProduct,
                                    ),
                                errorMessage = null,
                            )
                        onSuccess(warning)
                        refreshCatalog()
                        return@launch
                    }
                    finalProduct = uploaded.getOrThrow()
                }
                val catalog = state.value.catalog
                state.value =
                    state.value.copy(
                        catalogBusy = false,
                        catalog =
                            catalog?.copy(
                                products =
                                    catalog.products
                                        .filterNot { product -> product.id == finalProduct.id } +
                                        finalProduct,
                            ),
                    )
                onSuccess(null)
                refreshCatalog()
            }
        }

        fun uploadCashierProductImage(
            productId: Int,
            bytes: ByteArray,
            filename: String,
            mimeType: String,
        ) {
            if (bytes.size > MAX_PRODUCT_IMAGE_BYTES) {
                showMutationError("La foto no puede pesar más de 5 MB.")
                return
            }
            if (state.value.catalogBusy) return
            mutationErrorVisible = false
            state.value = state.value.copy(catalogBusy = true, errorMessage = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        catalogRepository.uploadProductImage(
                            productId = productId,
                            bytes = bytes,
                            filename = filename,
                            mimeType = mimeType,
                            idempotencyKey = MutationIdempotency.productImage(productId, bytes),
                        )
                    }
                result.fold(
                    onSuccess = { updated ->
                        val catalog = state.value.catalog
                        state.value =
                            state.value.copy(
                                catalogBusy = false,
                                catalog =
                                    catalog?.copy(
                                        products =
                                            catalog.products.map { product ->
                                                if (product.id == updated.id) updated else product
                                            },
                                    ),
                            )
                    },
                    onFailure = { error ->
                        state.value = state.value.copy(catalogBusy = false)
                        showMutationError(
                            error.toUserFacingMessage("No se pudo subir la foto (producto $productId)."),
                        )
                    },
                )
            }
        }

        private companion object {
            const val CASH_HAS_OPEN_ACCOUNTS_CODE = "CASH_SESSION_HAS_OPEN_ACCOUNTS"
            const val MAX_PRODUCT_IMAGE_BYTES = 5 * 1024 * 1024
        }
    }
