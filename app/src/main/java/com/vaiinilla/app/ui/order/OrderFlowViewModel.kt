package com.vaiinilla.app.ui.order

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.config.AppEnvironment
import com.vaiinilla.app.core.config.DataSourceMode
import com.vaiinilla.app.core.config.EffectiveDataSourceResolver
import com.vaiinilla.app.data.operational.StaffPresenceCoordinator
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.OptionGroup
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.usecase.BuildCreateOrderRequestUseCase
import com.vaiinilla.app.domain.usecase.CreateOrderUseCase
import com.vaiinilla.app.domain.usecase.CreateStudentCheckoutUseCase
import com.vaiinilla.app.domain.usecase.GetCatalogUseCase
import com.vaiinilla.app.domain.usecase.GetOperationalStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class OrderFlowViewModel @Inject constructor(
    private val getCatalog: GetCatalogUseCase,
    private val getOperationalStatus: GetOperationalStatusUseCase,
    private val buildCreateOrderRequest: BuildCreateOrderRequestUseCase,
    private val createOrder: CreateOrderUseCase,
    private val createStudentCheckout: CreateStudentCheckoutUseCase,
    private val staffPresenceCoordinator: StaffPresenceCoordinator,
    private val dataSourceResolver: EffectiveDataSourceResolver,
    private val environment: AppEnvironment,
) : ViewModel() {
    private val _uiState = mutableStateOf(
        OrderFlowUiState(
            dataSourceMode = environment.dataSourceMode,
            testOnlyMode = dataSourceResolver.isTestOnlyMode,
        ),
    )
    val uiState: State<OrderFlowUiState> = _uiState

    private var pendingIdempotencyKey: String? = null

    init {
        refresh()
    }

    fun refresh() {
        val previous = _uiState.value
        _uiState.value = previous.copy(
            loading = true,
            errorMessage = null,
            testOnlyMode = dataSourceResolver.isTestOnlyMode,
            dataSourceMode = dataSourceResolver.effectiveMode(),
        )
        viewModelScope.launch {
            val catalogResult = withContext(Dispatchers.IO) { getCatalog() }
            val statusResult = withContext(Dispatchers.IO) { getOperationalStatus() }
            val failure = catalogResult.exceptionOrNull() ?: statusResult.exceptionOrNull()
            val errorMessage = failure?.message
                ?: failure?.javaClass?.simpleName?.let { "Error de red: $it" }

            _uiState.value = previous.copy(
                loading = false,
                catalog = catalogResult.getOrNull(),
                operationalStatus = statusResult.getOrNull(),
                errorMessage = errorMessage,
            )
        }
    }

    fun updateSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectCategory(categoryId: Int?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun openProduct(productId: Int) {
        val product = _uiState.value.catalog?.products?.firstOrNull { it.id == productId } ?: return
        val defaults = product.optionGroups.flatMapTo(linkedSetOf()) { group ->
            group.options.take(group.minimumSelections).map { it.id }
        }
        _uiState.value = _uiState.value.copy(
            selectedProductId = productId,
            selectedOptionIds = defaults,
            selectedQuantity = 1,
            createOrderError = null,
        )
    }

    fun closeProduct() {
        _uiState.value = _uiState.value.copy(
            selectedProductId = null,
            selectedOptionIds = emptySet(),
            selectedQuantity = 1,
        )
    }

    fun toggleOption(groupId: Int, optionId: Int) {
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
        _uiState.value = state.copy(
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
            _uiState.value = state.copy(createOrderError = validation.exceptionOrNull()?.message)
            return
        }

        val candidate = CartLine(
            product = product,
            quantity = state.selectedQuantity,
            selectedOptionIds = state.selectedOptionIds,
        )
        val existing = state.cartLines.firstOrNull { it.key == candidate.key }
        val updated = if (existing != null) {
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
        _uiState.value = state.copy(
            cartLines = updated,
            selectedProductId = null,
            selectedOptionIds = emptySet(),
            selectedQuantity = 1,
            createOrderError = null,
        )
    }

    fun changeCartLineQuantity(lineKey: String, delta: Int) {
        val state = _uiState.value
        val line = state.cartLines.firstOrNull { it.key == lineKey } ?: return
        val nextQuantity = line.quantity + delta
        val updated = when {
            nextQuantity <= 0 -> state.cartLines.filterNot { it.key == lineKey }
            nextQuantity > 20 -> state.cartLines
            else -> state.cartLines.map { current ->
                if (current.key == lineKey) current.copy(quantity = nextQuantity) else current
            }
        }
        pendingIdempotencyKey = null
        _uiState.value = state.copy(cartLines = updated, createOrderError = null)
    }

    fun updateKitchenNotes(notes: String) {
        pendingIdempotencyKey = null
        _uiState.value = _uiState.value.copy(kitchenNotes = notes, createOrderError = null)
    }

    fun updateCheckoutDestination(destination: OrderDestination) {
        pendingIdempotencyKey = null
        _uiState.value = _uiState.value.copy(
            checkoutDestination = destination,
            createOrderError = null,
        )
    }

    fun updateCheckoutPayment(payment: PaymentMethod) {
        pendingIdempotencyKey = null
        _uiState.value = _uiState.value.copy(
            checkoutPayment = payment,
            createOrderError = null,
        )
    }

    fun applyTestOnlyMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            testOnlyMode = enabled,
            dataSourceMode = dataSourceResolver.effectiveMode(),
            createOrderError = null,
            errorMessage = null,
        )
        refresh()
    }

    fun submitOrder(walletBalance: Int = 0, onWalletDebit: (Int) -> Unit = {}) {
        val state = _uiState.value
        if (state.cartLines.isEmpty()) {
            _uiState.value = state.copy(
                createOrderError = "Agrega al menos un producto antes de confirmar.",
            )
            return
        }
        if (state.creatingOrder) return

        if (state.checkoutPayment == PaymentMethod.BALANCE && !state.hasSufficientBalance(walletBalance)) {
            _uiState.value = state.copy(
                createOrderError = "Saldo insuficiente. Añade dinero en Cartera o elige otro método.",
            )
            return
        }

        _uiState.value = state.copy(creatingOrder = true, createOrderError = null)
        viewModelScope.launch {
            var current = _uiState.value
            if (dataSourceResolver.usesNetwork() && current.usesStudentCheckout) {
                _uiState.value = current.copy(
                    creatingOrder = false,
                    createOrderError = "Saldo, tarjeta y mesa están disponibles en modo demo local.",
                )
                return@launch
            }

            if (dataSourceResolver.usesNetwork()) {
                withContext(Dispatchers.IO) { staffPresenceCoordinator.primeStaffPresence() }
                val status = withContext(Dispatchers.IO) { getOperationalStatus() }.getOrNull()
                if (status != null) {
                    current = current.copy(operationalStatus = status)
                    _uiState.value = current.copy(creatingOrder = true, createOrderError = null)
                }
            }

            if (current.requiresOperationalReady && !current.isOperationallyReady) {
                val blocker = current.operationalBlockerMessage
                    ?: "El establecimiento no está recibiendo pedidos en este momento."
                _uiState.value = current.copy(
                    creatingOrder = false,
                    createOrderError = blocker,
                )
                return@launch
            }

            val request = buildCreateOrderRequest(
                lines = current.cartLines,
                kitchenNotes = current.kitchenNotes,
                paymentMethod = current.checkoutPayment,
                destination = current.checkoutDestination,
                spaceId = current.checkoutSpaceId,
            )
            val idempotencyKey = pendingIdempotencyKey ?: UUID.randomUUID().toString().also {
                pendingIdempotencyKey = it
            }
            val result = withContext(Dispatchers.IO) {
                if (current.usesStudentCheckout) {
                    createStudentCheckout(request, idempotencyKey)
                } else {
                    createOrder(request, idempotencyKey)
                }
            }
            result.fold(
                onSuccess = { order ->
                    pendingIdempotencyKey = null
                    if (current.checkoutPayment == PaymentMethod.BALANCE) {
                        val total = Money.parse(order.summary.total).toInt()
                        onWalletDebit(total)
                    }
                    _uiState.value = _uiState.value.copy(
                        creatingOrder = false,
                        cartLines = emptyList(),
                        kitchenNotes = "",
                        checkoutDestination = OrderDestination.TAKE_AWAY,
                        checkoutPayment = PaymentMethod.CASH,
                        createdOrder = order,
                        createOrderError = null,
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        creatingOrder = false,
                        createOrderError = error.message ?: "No se pudo crear el pedido.",
                    )
                },
            )
        }
    }

    fun clearCreatedOrder() {
        _uiState.value = _uiState.value.copy(createdOrder = null)
    }
}
