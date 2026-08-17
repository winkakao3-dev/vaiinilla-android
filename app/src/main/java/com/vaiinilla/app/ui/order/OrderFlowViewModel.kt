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
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.repository.DiscoveryRepository
import com.vaiinilla.app.domain.usecase.BuildCreateOrderRequestUseCase
import com.vaiinilla.app.domain.usecase.CreateRemoteOrderUseCase
import com.vaiinilla.app.domain.usecase.GetCatalogUseCase
import com.vaiinilla.app.domain.usecase.GetOperationalStatusUseCase
import com.vaiinilla.app.ui.assistant.AssistantChatMessage
import com.vaiinilla.app.ui.assistant.AssistantLocalReplies
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        private var activeCartStorageKey: String? = null

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
            activeCartStorageKey = storageKey
            val previous = _uiState.value
            _uiState.value =
                previous.copy(
                    loading = true,
                    errorMessage = null,
                    guestVenue = venue,
                    cartLines = if (sameCart) previous.cartLines else emptyList(),
                    selectedProductId = null,
                    selectedOptionIds = emptySet(),
                    selectedQuantity = 1,
                    createOrderError = null,
                    checkoutDestination =
                        if (venue.space != null) {
                            OrderDestination.IN_SPACE
                        } else {
                            OrderDestination.TAKE_AWAY
                        },
                    selectedSpaceId = venue.space?.id ?: 0,
                )
            guestSessionStore.saveVenue(venue)
            viewModelScope.launch {
                val catalogResult =
                    withContext(Dispatchers.IO) {
                        discoveryRepository.getGuestCatalog(venue.establishment.slug)
                    }
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
            viewModelScope.launch {
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
        fun clearGuestVenue() {
            persistCurrentCartIfNeeded()
            activeCartStorageKey = null
            guestSessionStore.clearVenue()
            _uiState.value =
                _uiState.value.copy(
                    guestVenue = null,
                    cartLines = emptyList(),
                )
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
            if (payment == PaymentMethod.CARD) {
                _uiState.value =
                    _uiState.value.copy(
                        createOrderError = "La tarjeta aún no está disponible. Usa efectivo o saldo.",
                    )
                return
            }
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
            viewModelScope.launch {
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
                    return@launch
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
                        return@launch
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
                    return@launch
                }

                val request =
                    buildCreateOrderRequest(
                        lines = current.cartLines,
                        kitchenNotes = current.kitchenNotes,
                        paymentMethod = current.checkoutPayment,
                        destination = current.checkoutDestination,
                        spaceId = current.checkoutSpaceId,
                    )
                val idempotencyKey =
                    pendingIdempotencyKey ?: UUID.randomUUID().toString().also {
                        pendingIdempotencyKey = it
                    }
                val result =
                    withContext(Dispatchers.IO) {
                        createRemoteOrder(request, idempotencyKey)
                    }
                result.fold(
                    onSuccess = { order ->
                        pendingIdempotencyKey = null
                        _uiState.value =
                            _uiState.value.copy(
                                creatingOrder = false,
                                cartLines = emptyList(),
                                kitchenNotes = "",
                                checkoutDestination = OrderDestination.TAKE_AWAY,
                                checkoutPayment = PaymentMethod.CASH,
                                createdOrder = order,
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

        fun clearCreatedOrder() {
            _uiState.value = _uiState.value.copy(createdOrder = null)
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
    }

internal fun firstGuestVenueFailure(
    catalogResult: Result<*>,
    _statusResult: Result<*>,
): Throwable? = catalogResult.exceptionOrNull()
