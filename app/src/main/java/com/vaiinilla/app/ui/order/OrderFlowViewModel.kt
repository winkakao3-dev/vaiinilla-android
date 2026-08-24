package com.vaiinilla.app.ui.order

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.data.auth.ContextoExchanger
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.data.operational.StaffPresenceCoordinator
import com.vaiinilla.app.domain.auth.student.StudentAuthRepository
import com.vaiinilla.app.domain.discovery.DiscoveryFailures
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentStatus
import com.vaiinilla.app.domain.model.isStripePaymentConfirmedByBackend
import com.vaiinilla.app.domain.repository.DiscoveryRepository
import com.vaiinilla.app.domain.usecase.BuildCreateOrderRequestUseCase
import com.vaiinilla.app.domain.usecase.CreateRemoteOrderUseCase
import com.vaiinilla.app.domain.usecase.GetCatalogUseCase
import com.vaiinilla.app.domain.usecase.GetOperationalStatusUseCase
import com.vaiinilla.app.domain.usecase.GetOrderUseCase
import com.vaiinilla.app.domain.usecase.RetryStripePaymentUseCase
import com.vaiinilla.app.ui.assistant.AssistantChatMessage
import com.vaiinilla.app.ui.assistant.AssistantLocalReplies
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OrderFlowViewModel
    @Inject
    constructor(
        private val getCatalog: GetCatalogUseCase,
        private val getOperationalStatus: GetOperationalStatusUseCase,
        private val buildCreateOrderRequest: BuildCreateOrderRequestUseCase,
        private val createRemoteOrder: CreateRemoteOrderUseCase,
        private val getOrder: GetOrderUseCase,
        private val retryStripePayment: RetryStripePaymentUseCase,
        private val discoveryRepository: DiscoveryRepository,
        private val guestSessionStore: GuestSessionStore,
        private val studentAuthRepository: StudentAuthRepository,
        private val contextoExchange: ContextoExchanger,
        private val sessionStore: SecureSessionStore,
        private val staffPresenceCoordinator: StaffPresenceCoordinator,
    ) : ViewModel() {
        private val _uiState =
            mutableStateOf(
                OrderFlowUiState(
                    guestVenue = guestSessionStore.readVenue(),
                ),
            )
        val uiState: State<OrderFlowUiState> = _uiState

        private var pendingIdempotencyKey: String? = null
        private var pendingStripeRetryIdempotencyKey: String? = null
        private var activeCartStorageKey: String? = null
        private var guestVenueLoadJob: Job? = null
        private val activeJobs = mutableSetOf<Job>()

        init {
            val venue = guestSessionStore.readVenue()
            if (venue != null) {
                enterGuestVenue(venue)
            } else {
                refresh()
            }
        }

        fun enterGuestVenue(venue: GuestVenueContext) {
            val storageKey =
                guestSessionStore.cartStorageKey(
                    venue.establishment.id,
                    venue.space?.id,
                )
            // A cart reload can be triggered twice while returning from auth: once by
            // finishStudentAuth and once by the Cart destination. Do not persist the
            // already-reset in-memory state over the durable snapshot when both loads
            // target the same venue. Only save when changing tenant/space.
            if (activeCartStorageKey != null && activeCartStorageKey != storageKey) {
                persistCurrentCartIfNeeded()
            }
            val sameCart = activeCartStorageKey == storageKey
            val previous = _uiState.value
            val switchingEstablishment = isEstablishmentSwitch(previous.guestVenue, venue)
            if (switchingEstablishment) {
                activeJobs.toList().forEach(Job::cancel)
                activeJobs.clear()
                guestVenueLoadJob = null
                pendingIdempotencyKey = null
                pendingStripeRetryIdempotencyKey = null
                guestSessionStore.clearPendingCreateIdempotency()
                previous.createdOrder
                    ?.summary
                    ?.id
                    ?.let(guestSessionStore::clearPendingStripeRetryIdempotency)
            } else {
                guestVenueLoadJob?.cancel()
            }
            activeCartStorageKey = storageKey
            _uiState.value =
                previous.copy(
                    loading = true,
                    errorMessage = null,
                    guestVenue = venue,
                    cartLines = if (sameCart) previous.cartLines else emptyList(),
                    kitchenNotes = if (sameCart) previous.kitchenNotes else "",
                    selectedProductId = null,
                    selectedOptionIds = emptySet(),
                    selectedQuantity = 1,
                    createOrderError = null,
                    createdOrder = if (switchingEstablishment) null else previous.createdOrder,
                    stripePaymentSession = if (switchingEstablishment) null else previous.stripePaymentSession,
                    stripePresentationKey = if (switchingEstablishment) null else previous.stripePresentationKey,
                    stripePaymentPhase =
                        if (switchingEstablishment) StripePaymentPhase.IDLE else previous.stripePaymentPhase,
                    stripePaymentMessage = if (switchingEstablishment) null else previous.stripePaymentMessage,
                    retryingStripePayment = if (switchingEstablishment) false else previous.retryingStripePayment,
                    checkoutDestination =
                        if (venue.space != null) {
                            OrderDestination.IN_SPACE
                        } else {
                            OrderDestination.TAKE_AWAY
                        },
                    selectedSpaceId = venue.space?.id ?: 0,
                )
            guestSessionStore.saveVenue(venue)
            guestVenueLoadJob =
                launchTracked {
                    val catalogResult =
                        withContext(Dispatchers.IO) {
                            discoveryRepository.getGuestCatalog(venue.establishment.slug)
                        }
                    if (activeCartStorageKey != storageKey) return@launchTracked
                    val catalog = catalogResult.getOrNull()
                    val restored =
                        if (catalog != null) {
                            guestSessionStore.restoreCartLines(
                                guestSessionStore.readCartSnapshot(storageKey),
                                catalog.products,
                            )
                        } else {
                            emptyList()
                        }
                    val nextCart =
                        restored.ifEmpty {
                            if (sameCart) _uiState.value.cartLines else emptyList()
                        }
                    // Public guest discovery is valid without identity. Operational status is
                    // authenticated, so its failure must not hide a catalog that loaded correctly.
                    val failure = catalogResult.exceptionOrNull()
                    val suspended = DiscoveryFailures.isEstablishmentSuspended(failure)
                    _uiState.value =
                        _uiState.value.copy(
                            loading = false,
                            catalog = catalog,
                            operationalStatus = null,
                            cartLines = nextCart,
                            errorMessage = failure?.message,
                            guestVenueSuspended = suspended,
                            guestVenue = venue,
                        )
                }
        }

        fun refresh() {
            val venue = _uiState.value.guestVenue
            if (venue != null) {
                enterGuestVenue(venue)
                return
            }
            val previous = _uiState.value
            _uiState.value =
                previous.copy(
                    loading = true,
                    errorMessage = null,
                )
            launchTracked {
                val catalogResult = withContext(Dispatchers.IO) { getCatalog() }
                val statusResult = withContext(Dispatchers.IO) { getOperationalStatus() }
                val failure = catalogResult.exceptionOrNull() ?: statusResult.exceptionOrNull()
                val errorMessage =
                    failure?.message
                        ?: failure?.javaClass?.simpleName?.let { "Error de red: $it" }

                _uiState.value =
                    previous.copy(
                        loading = false,
                        catalog = catalogResult.getOrNull(),
                        operationalStatus = statusResult.getOrNull(),
                        errorMessage = errorMessage,
                    )
            }
        }

        /** Persists guest cart before leaving for auth. Venue/cart keys stay in GuestSessionStore. */
        fun prepareForGuestAuth() {
            persistCurrentCartIfNeeded()
        }

        /** Guest discovery checkout requires student auth until email is verified and enrolled. */
        fun requiresStudentAuth(): Boolean {
            val state = _uiState.value
            val venue = state.guestVenue ?: return false
            return !studentAuthRepository.isReadyForCheckout(venue.establishment.id)
        }

        /** Reloads guest venue + cart after returning from auth without clearing tenant state. */
        fun restoreGuestSessionAfterAuth() {
            val venue = guestSessionStore.readVenue() ?: return
            enterGuestVenue(venue)
        }

        /** Leaves the guest venue before entering an authenticated operational mode. */
        fun clearGuestVenue(persistCurrentCart: Boolean = true) {
            if (persistCurrentCart) persistCurrentCartIfNeeded()
            guestVenueLoadJob?.cancel()
            guestVenueLoadJob = null
            activeCartStorageKey = null
            guestSessionStore.clearVenue()
            _uiState.value =
                _uiState.value.copy(
                    guestVenue = null,
                    cartLines = emptyList(),
                )
        }

        fun clearForSessionTermination() {
            activeJobs.toList().forEach(Job::cancel)
            activeJobs.clear()
            pendingIdempotencyKey = null
            pendingStripeRetryIdempotencyKey = null
            guestVenueLoadJob?.cancel()
            guestVenueLoadJob = null
            activeCartStorageKey = null
            guestSessionStore.clearAll()
            _uiState.value = OrderFlowUiState()
        }

        fun updateSearch(query: String) {
            _uiState.value = _uiState.value.copy(searchQuery = query)
        }

        fun selectCategory(categoryId: Int?) {
            _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
        }

        fun openProduct(productId: Int) {
            val product =
                _uiState.value.catalog
                    ?.products
                    ?.firstOrNull { it.id == productId } ?: return
            val defaults =
                product.optionGroups.flatMapTo(linkedSetOf()) { group ->
                    group.options.take(group.minimumSelections).map { it.id }
                }
            _uiState.value =
                _uiState.value.copy(
                    selectedProductId = productId,
                    selectedOptionIds = defaults,
                    selectedQuantity = 1,
                    createOrderError = null,
                )
        }

        fun closeProduct() {
            _uiState.value =
                _uiState.value.copy(
                    selectedProductId = null,
                    selectedOptionIds = emptySet(),
                    selectedQuantity = 1,
                )
        }

        fun toggleOption(
            groupId: Int,
            optionId: Int,
        ) {
            val state = _uiState.value
            val product = state.selectedProduct ?: return
            val group = product.optionGroups.firstOrNull { it.id == groupId } ?: return
            if (group.options.none { it.id == optionId }) return

            val groupOptionIds = group.options.mapTo(mutableSetOf()) { it.id }
            val selected = state.selectedOptionIds.toMutableSet()
            val isSelected = optionId in selected

            when {
                isSelected && group.minimumSelections == 0 -> selected.remove(optionId)
                isSelected -> Unit
                group.maximumSelections == 1 -> {
                    selected.removeAll(groupOptionIds)
                    selected.add(optionId)
                }
                selected.count { it in groupOptionIds } < group.maximumSelections -> selected.add(optionId)
            }

            _uiState.value = state.copy(selectedOptionIds = selected)
        }

        fun clearOptionalGroup(groupId: Int) {
            val state = _uiState.value
            val product = state.selectedProduct ?: return
            val group = product.optionGroups.firstOrNull { it.id == groupId } ?: return
            if (group.minimumSelections != 0) return
            val groupOptionIds = group.options.mapTo(mutableSetOf()) { it.id }
            _uiState.value =
                state.copy(
                    selectedOptionIds = state.selectedOptionIds - groupOptionIds,
                )
        }

        fun changeSelectedQuantity(delta: Int) {
            val next = (_uiState.value.selectedQuantity + delta).coerceIn(1, 20)
            _uiState.value = _uiState.value.copy(selectedQuantity = next)
        }

        fun addSelectedProductToCart() {
            val state = _uiState.value
            val product = state.selectedProduct ?: return
            val validation = runCatching { ContractRules.validateSelections(product, state.selectedOptionIds) }
            if (validation.isFailure) {
                _uiState.value =
                    state.copy(
                        createOrderError =
                            validation.exceptionOrNull().toUserFacingMessage(
                                "Revisa las opciones del producto.",
                            ),
                    )
                return
            }

            val candidate =
                CartLine(
                    product = product,
                    quantity = state.selectedQuantity,
                    selectedOptionIds = state.selectedOptionIds,
                )
            val existing = state.cartLines.firstOrNull { it.key == candidate.key }
            val updated =
                if (existing != null) {
                    val nextQuantity = existing.quantity + candidate.quantity
                    if (nextQuantity > 20) {
                        _uiState.value = state.copy(createOrderError = "La cantidad máxima por línea es 20.")
                        return
                    }
                    state.cartLines.map { line ->
                        if (line.key == candidate.key) line.copy(quantity = nextQuantity) else line
                    }
                } else {
                    if (state.cartLines.size >= 50) {
                        _uiState.value = state.copy(createOrderError = "El carrito admite hasta 50 líneas.")
                        return
                    }
                    state.cartLines + candidate
                }

            pendingIdempotencyKey = null
            _uiState.value =
                state.copy(
                    cartLines = updated,
                    selectedProductId = null,
                    selectedOptionIds = emptySet(),
                    selectedQuantity = 1,
                    createOrderError = null,
                )
            persistCurrentCartIfNeeded()
        }

        fun changeCartLineQuantity(
            lineKey: String,
            delta: Int,
        ) {
            val state = _uiState.value
            val line = state.cartLines.firstOrNull { it.key == lineKey } ?: return
            val nextQuantity = line.quantity + delta
            val updated =
                when {
                    nextQuantity <= 0 -> state.cartLines.filterNot { it.key == lineKey }
                    nextQuantity > 20 -> state.cartLines
                    else ->
                        state.cartLines.map { current ->
                            if (current.key == lineKey) current.copy(quantity = nextQuantity) else current
                        }
                }
            pendingIdempotencyKey = null
            _uiState.value = state.copy(cartLines = updated, createOrderError = null)
            persistCurrentCartIfNeeded()
        }

        private fun persistCurrentCartIfNeeded() {
            val key = activeCartStorageKey ?: return
            guestSessionStore.saveCartSnapshot(key, _uiState.value.cartLines)
        }

        fun updateKitchenNotes(notes: String) {
            pendingIdempotencyKey = null
            _uiState.value = _uiState.value.copy(kitchenNotes = notes, createOrderError = null)
        }

        fun updateCheckoutDestination(destination: OrderDestination) {
            if (
                destination == OrderDestination.IN_SPACE &&
                _uiState.value.guestVenue?.space == null
            ) {
                _uiState.value =
                    _uiState.value.copy(
                        createOrderError = "Escanea el QR de un espacio para pedir en mesa.",
                    )
                return
            }
            pendingIdempotencyKey = null
            _uiState.value =
                _uiState.value.copy(
                    checkoutDestination = destination,
                    createOrderError = null,
                )
        }

        fun updateCheckoutSpace(spaceId: Int) {
            pendingIdempotencyKey = null
            _uiState.value =
                _uiState.value.copy(
                    selectedSpaceId = spaceId,
                    createOrderError = null,
                )
        }

        fun updateCheckoutPayment(payment: PaymentMethod) {
            pendingIdempotencyKey = null
            _uiState.value =
                _uiState.value.copy(
                    checkoutPayment = payment,
                    createOrderError = null,
                )
        }

        fun submitOrder() {
            val state = _uiState.value
            if (state.cartLines.isEmpty()) {
                _uiState.value =
                    state.copy(
                        createOrderError = "Agrega al menos un producto antes de confirmar.",
                    )
                return
            }
            if (state.creatingOrder) return

            if (requiresStudentAuth()) {
                _uiState.value =
                    state.copy(
                        createOrderError = "Inicia sesión y verifica tu correo antes de confirmar el pedido.",
                    )
                return
            }

            _uiState.value = state.copy(creatingOrder = true, createOrderError = null)
            launchTracked {
                var current = _uiState.value
                current.guestVenue?.let { venue ->
                    if (sessionStore.readAccessToken().isNullOrBlank()) {
                        withContext(Dispatchers.IO) { refreshClientContext(venue) }
                    }
                }
                val staffPresenceResult =
                    withContext(Dispatchers.IO) {
                        staffPresenceCoordinator.primeStaffPresence()
                    }
                if (staffPresenceResult.isFailure) {
                    val reason =
                        staffPresenceResult.exceptionOrNull().toUserFacingMessage(
                            "No se pudo avisar a Caja y Cocina.",
                        )
                    _uiState.value =
                        current.copy(
                            creatingOrder = false,
                            createOrderError = "No pudimos validar la disponibilidad operativa. $reason",
                        )
                    return@launchTracked
                }
                var statusResult = withContext(Dispatchers.IO) { getOperationalStatus() }
                if (
                    statusResult.isFailure &&
                    current.guestVenue?.establishment?.clientIdRequired == false
                ) {
                    refreshClientContext(requireNotNull(current.guestVenue))
                    statusResult = withContext(Dispatchers.IO) { getOperationalStatus() }
                }
                val status =
                    statusResult.getOrElse { error ->
                        _uiState.value =
                            current.copy(
                                creatingOrder = false,
                                createOrderError =
                                    "No pudimos verificar si el establecimiento está recibiendo pedidos. " +
                                        error.toUserFacingMessage("Vuelve a iniciar sesión."),
                            )
                        return@launchTracked
                    }
                current = current.copy(operationalStatus = status)
                _uiState.value = current.copy(creatingOrder = true, createOrderError = null)

                if (current.requiresOperationalReady && !current.isOperationallyReady) {
                    val blocker =
                        current.operationalBlockerMessage
                            ?: "El establecimiento no está recibiendo pedidos en este momento."
                    _uiState.value =
                        current.copy(
                            creatingOrder = false,
                            createOrderError = blocker,
                        )
                    return@launchTracked
                }

                val request =
                    buildCreateOrderRequest(
                        lines = current.cartLines,
                        kitchenNotes = current.kitchenNotes,
                        paymentMethod = current.checkoutPayment,
                        destination = current.checkoutDestination,
                        spaceId = current.checkoutSpaceId,
                    )
                val requestFingerprint = createOrderFingerprint(request)
                val idempotencyKey =
                    pendingIdempotencyKey
                        ?: guestSessionStore.readPendingCreateIdempotency(requestFingerprint)
                        ?: UUID.randomUUID().toString().also { generated ->
                            pendingIdempotencyKey = generated
                            guestSessionStore.savePendingCreateIdempotency(requestFingerprint, generated)
                        }
                pendingIdempotencyKey = idempotencyKey
                val result =
                    withContext(Dispatchers.IO) {
                        createRemoteOrder(request, idempotencyKey)
                    }
                result.fold(
                    onSuccess = { created ->
                        pendingIdempotencyKey = null
                        guestSessionStore.clearPendingCreateIdempotency()
                        pendingStripeRetryIdempotencyKey = null
                        val stripeSession = created.stripeSession
                        _uiState.value =
                            _uiState.value.copy(
                                creatingOrder = false,
                                cartLines = emptyList(),
                                kitchenNotes = "",
                                checkoutDestination = OrderDestination.TAKE_AWAY,
                                checkoutPayment = PaymentMethod.CASH,
                                createdOrder = created.order,
                                stripePaymentSession = stripeSession,
                                stripePresentationKey = stripeSession?.let { UUID.randomUUID().toString() },
                                stripePaymentPhase =
                                    if (stripeSession != null) StripePaymentPhase.READY else StripePaymentPhase.IDLE,
                                stripePaymentMessage =
                                    if (stripeSession != null) "Elige cómo pagar de forma segura." else null,
                                createOrderError = null,
                            )
                        activeCartStorageKey?.let { guestSessionStore.clearCart(it) }
                    },
                    onFailure = { error ->
                        _uiState.value =
                            _uiState.value.copy(
                                creatingOrder = false,
                                createOrderError = error.toUserFacingMessage("No se pudo crear el pedido."),
                            )
                    },
                )
            }
        }

        private fun createOrderFingerprint(request: CreateOrderRequest): String {
            val canonical =
                buildString {
                    append(request.paymentMethod.wireValue)
                    append('|')
                    append(request.destination.wireValue)
                    append('|')
                    append(request.spaceId ?: "null")
                    append('|')
                    append(request.kitchenNotes)
                    request.items.forEach { item ->
                        append('|')
                        append(item.productId)
                        append(':')
                        append(item.quantity)
                        append(':')
                        append(item.optionIds.sorted().joinToString(","))
                    }
                }
            return MessageDigest
                .getInstance("SHA-256")
                .digest(canonical.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }

        private suspend fun refreshClientContext(venue: GuestVenueContext) {
            runCatching {
                val firebaseToken = studentAuthRepository.getIdToken(forceRefresh = true).getOrThrow()
                val context =
                    contextoExchange.exchange(
                        firebaseIdToken = firebaseToken,
                        establecimientoSlug = venue.establishment.slug,
                        establecimientoId = venue.establishment.id,
                        identificadorCliente = null,
                    )
                sessionStore.saveAccessToken(context.accessToken)
            }
        }

        fun markStripePaymentSheetPresented() {
            if (_uiState.value.stripePaymentSession == null) return
            _uiState.value =
                _uiState.value.copy(
                    stripePaymentPhase = StripePaymentPhase.PRESENTING,
                    stripePaymentMessage = null,
                )
        }

        fun onStripePaymentSheetCompleted() {
            val orderId =
                _uiState.value.createdOrder
                    ?.summary
                    ?.id ?: return
            dropStripeSecret(
                phase = StripePaymentPhase.PROCESSING_CONFIRMATION,
                message = "Procesando confirmación del pago…",
            )
            reconcileStripeOrder(orderId = orderId, maxAttempts = 6)
        }

        fun onStripePaymentSheetCanceled() {
            val orderId =
                _uiState.value.createdOrder
                    ?.summary
                    ?.id ?: return
            dropStripeSecret(
                phase = StripePaymentPhase.PROCESSING_CONFIRMATION,
                message = "Verificando el estado del pago…",
            )
            reconcileStripeOrder(orderId = orderId, maxAttempts = 2, localCanceled = true)
        }

        fun onStripePaymentSheetFailed(errorMessage: String?) {
            val orderId =
                _uiState.value.createdOrder
                    ?.summary
                    ?.id ?: return
            dropStripeSecret(
                phase = StripePaymentPhase.PROCESSING_CONFIRMATION,
                message = "Verificando el intento de pago…",
            )
            reconcileStripeOrder(
                orderId = orderId,
                maxAttempts = 2,
                localFailureMessage = errorMessage?.takeIf { it.isNotBlank() },
            )
        }

        fun retryStripePayment() {
            val order = _uiState.value.createdOrder ?: return
            if (order.summary.paymentMethod != PaymentMethod.STRIPE || _uiState.value.retryingStripePayment) return
            val paymentStatus = order.payment?.status
            if (paymentStatus != null && !paymentStatus.canRetry) return

            val key =
                pendingStripeRetryIdempotencyKey
                    ?: guestSessionStore.readPendingStripeRetryIdempotency(order.summary.id)
                    ?: UUID.randomUUID().toString().also { generated ->
                        pendingStripeRetryIdempotencyKey = generated
                        guestSessionStore.savePendingStripeRetryIdempotency(order.summary.id, generated)
                    }
            pendingStripeRetryIdempotencyKey = key
            _uiState.value =
                _uiState.value.copy(
                    retryingStripePayment = true,
                    stripePaymentMessage = "Preparando un nuevo intento…",
                )
            launchTracked {
                val result =
                    withContext(Dispatchers.IO) {
                        retryStripePayment(order.summary.id, key)
                    }
                result.fold(
                    onSuccess = { session ->
                        pendingStripeRetryIdempotencyKey = null
                        guestSessionStore.clearPendingStripeRetryIdempotency(order.summary.id)
                        _uiState.value =
                            _uiState.value.copy(
                                retryingStripePayment = false,
                                stripePaymentSession = session,
                                stripePresentationKey = UUID.randomUUID().toString(),
                                stripePaymentPhase = StripePaymentPhase.READY,
                                stripePaymentMessage = "Listo para reintentar el pago.",
                            )
                    },
                    onFailure = { error ->
                        _uiState.value =
                            _uiState.value.copy(
                                retryingStripePayment = false,
                                stripePaymentPhase = StripePaymentPhase.PENDING,
                                stripePaymentMessage =
                                    error.toUserFacingMessage(
                                        "No se pudo reanudar el pago todavía.",
                                    ),
                            )
                    },
                )
            }
        }

        private fun dropStripeSecret(
            phase: StripePaymentPhase,
            message: String?,
        ) {
            _uiState.value =
                _uiState.value.copy(
                    stripePaymentSession = null,
                    stripePresentationKey = null,
                    stripePaymentPhase = phase,
                    stripePaymentMessage = message,
                )
        }

        private fun reconcileStripeOrder(
            orderId: String,
            maxAttempts: Int,
            localCanceled: Boolean = false,
            localFailureMessage: String? = null,
        ) {
            launchTracked {
                var latest = _uiState.value.createdOrder
                var lastError: Throwable? = null
                for (attempt in 0 until maxAttempts) {
                    val result = withContext(Dispatchers.IO) { getOrder(orderId) }
                    result.onSuccess { latest = it }.onFailure { lastError = it }
                    val status = latest?.payment?.status
                    val confirmed = latest?.isStripePaymentConfirmedByBackend() == true
                    val terminalPaymentStatus =
                        status in
                            setOf(
                                StripePaymentStatus.FAILED,
                                StripePaymentStatus.CANCELED,
                                StripePaymentStatus.REFUND_PENDING,
                                StripePaymentStatus.REFUNDING,
                                StripePaymentStatus.REFUNDED,
                            )
                    if (confirmed || terminalPaymentStatus) break
                    if (attempt < maxAttempts - 1) {
                        delay(STRIPE_CONFIRMATION_POLL_MS * (attempt + 1))
                    }
                }

                val refreshed = latest
                val status = refreshed?.payment?.status
                val phase = stripePhaseFromBackend(refreshed)
                val message =
                    when (phase) {
                        StripePaymentPhase.CONFIRMED -> "Pago confirmado por Vaiinilla."
                        StripePaymentPhase.PENDING ->
                            when {
                                localCanceled -> "Pago pendiente. Puedes retomarlo cuando quieras."
                                localFailureMessage != null ->
                                    "Vaiinilla aún no confirma un fallo. El pedido sigue pendiente de pago."
                                lastError != null ->
                                    lastError.toUserFacingMessage(
                                        "No pudimos confirmar el estado del pago todavía.",
                                    )
                                else ->
                                    "El pago sigue procesándose. Actualizaremos el estado desde Vaiinilla."
                            }
                        StripePaymentPhase.FAILED ->
                            "El backend confirmó que el pago falló. Puedes reintentarlo."
                        StripePaymentPhase.CANCELED ->
                            "El backend confirmó que el pago fue cancelado. Puedes reintentarlo."
                        StripePaymentPhase.REFUNDING -> "El reembolso está en proceso."
                        StripePaymentPhase.REFUNDED -> "El pago fue reembolsado."
                        else -> "Verificando el estado del pago…"
                    }
                _uiState.value =
                    _uiState.value.copy(
                        createdOrder = refreshed ?: _uiState.value.createdOrder,
                        stripePaymentPhase = phase,
                        stripePaymentMessage = message,
                    )
            }
        }

        fun dismissCreatedOrder(orderId: String) {
            if (_uiState.value.createdOrder
                    ?.summary
                    ?.id == orderId
            ) {
                clearCreatedOrder()
            }
        }

        fun clearCreatedOrder() {
            _uiState.value.createdOrder
                ?.summary
                ?.id
                ?.let(guestSessionStore::clearPendingStripeRetryIdempotency)
            pendingStripeRetryIdempotencyKey = null
            _uiState.value =
                _uiState.value.copy(
                    createdOrder = null,
                    stripePaymentSession = null,
                    stripePresentationKey = null,
                    stripePaymentPhase = StripePaymentPhase.IDLE,
                    stripePaymentMessage = null,
                    retryingStripePayment = false,
                )
        }

        fun sendAssistantMessage(text: String) {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return
            val products =
                _uiState.value.catalog
                    ?.products
                    .orEmpty()
            val reply = AssistantLocalReplies.reply(trimmed, products)
            val current = _uiState.value.assistantChatMessages
            _uiState.value =
                _uiState.value.copy(
                    assistantChatMessages =
                        current +
                            AssistantChatMessage(trimmed, fromUser = true) +
                            AssistantChatMessage(reply, fromUser = false),
                )
        }

        fun clearAssistantChat() {
            _uiState.value = _uiState.value.copy(assistantChatMessages = emptyList())
        }

        private fun launchTracked(block: suspend () -> Unit): Job {
            lateinit var job: Job
            job =
                viewModelScope.launch {
                    try {
                        block()
                    } finally {
                        activeJobs.remove(job)
                    }
                }
            activeJobs += job
            return job
        }

        private companion object {
            const val STRIPE_CONFIRMATION_POLL_MS = 500L
        }
    }

internal fun firstGuestVenueFailure(
    catalogResult: Result<*>,
    _statusResult: Result<*>,
): Throwable? = catalogResult.exceptionOrNull()
