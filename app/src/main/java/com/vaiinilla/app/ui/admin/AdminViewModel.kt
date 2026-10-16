package com.vaiinilla.app.ui.admin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.AdminProductRules
import com.vaiinilla.app.domain.admin.AdminSummary
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.AdminRepository
import com.vaiinilla.app.domain.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/** Los atajos de periodo del resumen, como en la web. */
enum class AdminPeriodPreset(
    val label: String,
    val days: Int,
) {
    TODAY("Hoy", 1),
    WEEK("7 días", 7),
    MONTH("30 días", 30),
}

/** Los filtros del historial, los mismos que "Pedidos e historial" en la web. */
enum class AdminOrderFilter(
    val label: String,
    val states: Set<OrderState>,
) {
    ACTIVE("Activos", setOf(OrderState.PENDING_PAYMENT, OrderState.PAID, OrderState.PREPARING, OrderState.READY)),
    DELIVERED("Entregados", setOf(OrderState.DELIVERED)),
    INCIDENTS("Incidencias", setOf(OrderState.CANCELED, OrderState.NOT_PICKED_UP, OrderState.EXPIRED)),
    ALL("Todos", emptySet()),
}

data class AdminUiState(
    val preset: AdminPeriodPreset = AdminPeriodPreset.TODAY,
    val summary: AdminSummary? = null,
    val summaryLoading: Boolean = false,
    val summaryError: String? = null,
    val operationalStatus: OperationalStatus? = null,
    val unpaid: AdminUnpaidAccounts? = null,
    val catalog: Catalog? = null,
    val catalogError: String? = null,
    val orderFilter: AdminOrderFilter = AdminOrderFilter.ACTIVE,
    val orders: List<OrderDetail> = emptyList(),
    val ordersCursor: String? = null,
    val ordersLoading: Boolean = false,
    val ordersError: String? = null,
    /** Algo del menú se está guardando: los botones de guardar se deshabilitan. */
    val saving: Boolean = false,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
)

@HiltViewModel
class AdminViewModel
    @Inject
    constructor(
        private val adminRepository: AdminRepository,
        private val catalogRepository: CatalogRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(AdminUiState())
        val uiState: State<AdminUiState> = _uiState
        private var noticeJob: Job? = null
        private var ordersJob: Job? = null
        private var visibleScope: String? = null

        /** [scope] es la membresía activa: si cambió de negocio, no se muestran ni un momento los datos del anterior. */
        fun onVisible(scope: String) {
            if (scope != visibleScope) {
                visibleScope = scope
                noticeJob?.cancel()
                ordersJob?.cancel()
                _uiState.value = AdminUiState()
            }
            refreshHome()
            refreshCatalog()
            refreshOrders()
        }

        // region Inicio

        fun selectPeriod(preset: AdminPeriodPreset) {
            if (preset == _uiState.value.preset && _uiState.value.summary != null) return
            _uiState.value = _uiState.value.copy(preset = preset)
            refreshSummary()
        }

        fun refreshHome() {
            refreshSummary()
            viewModelScope.launch {
                val status = withContext(Dispatchers.IO) { catalogRepository.getOperationalStatus() }
                val unpaid = withContext(Dispatchers.IO) { adminRepository.unpaidAccounts() }
                _uiState.value =
                    _uiState.value.copy(
                        operationalStatus = status.getOrNull() ?: _uiState.value.operationalStatus,
                        unpaid = unpaid.getOrNull() ?: _uiState.value.unpaid,
                    )
            }
        }

        private fun refreshSummary() {
            val preset = _uiState.value.preset
            _uiState.value = _uiState.value.copy(summaryLoading = true, summaryError = null)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { adminRepository.summary(AdminPeriod.lastDays(preset.days)) }
                if (_uiState.value.preset != preset) return@launch
                _uiState.value =
                    result.fold(
                        onSuccess = { _uiState.value.copy(summary = it, summaryLoading = false) },
                        onFailure = {
                            _uiState.value.copy(
                                summaryLoading = false,
                                summaryError = it.toUserFacingMessage("No se pudo cargar el resumen."),
                            )
                        },
                    )
            }
        }

        // endregion

        // region Menú

        fun refreshCatalog() {
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { catalogRepository.getCatalog() }
                _uiState.value =
                    result.fold(
                        onSuccess = { _uiState.value.copy(catalog = it, catalogError = null) },
                        onFailure = {
                            _uiState.value.copy(catalogError = it.toUserFacingMessage("No se pudo cargar el menú."))
                        },
                    )
            }
        }

        /** Crea o renombra una categoría; [onDone] cierra la hoja cuando el servidor la guardó. */
        fun saveCategory(
            id: Int?,
            input: AdminCategoryInput,
            onDone: () -> Unit,
        ) {
            if (input.name.isBlank()) return showNotice("Escribe el nombre de la categoría.", isError = true)
            mutate(success = if (id == null) "Categoría creada" else "Categoría guardada", onDone = onDone) {
                if (id == null) {
                    adminRepository.createCategory(input, UUID.randomUUID().toString()).map { }
                } else {
                    adminRepository.updateCategory(id, input, UUID.randomUUID().toString()).map { }
                }
            }
        }

        /**
         * Crea o guarda un producto completo y, si hay foto nueva, la sube después. Si solo falla la foto, el
         * producto queda guardado y se avisa.
         */
        fun saveProduct(
            id: Int?,
            input: AdminProductInput,
            image: AdminImage?,
            onDone: () -> Unit,
        ) {
            AdminProductRules.problem(input)?.let { return showNotice(it, isError = true) }
            if (_uiState.value.saving) return
            val ready = AdminProductRules.normalized(input)
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val saved =
                    withContext(Dispatchers.IO) {
                        if (id == null) {
                            adminRepository.createProduct(ready, UUID.randomUUID().toString())
                        } else {
                            adminRepository.updateProduct(id, ready, UUID.randomUUID().toString())
                        }
                    }
                val product =
                    saved.getOrElse { error ->
                        _uiState.value = _uiState.value.copy(saving = false)
                        showNotice(error.toUserFacingMessage("No se pudo guardar el producto."), isError = true)
                        return@launch
                    }
                var warning: String? = null
                if (image != null) {
                    val uploaded =
                        withContext(Dispatchers.IO) {
                            catalogRepository.uploadProductImage(
                                productId = product.id,
                                bytes = image.bytes,
                                filename = image.filename,
                                mimeType = image.mimeType,
                                idempotencyKey = MutationIdempotency.productImage(product.id, image.bytes),
                            )
                        }
                    if (uploaded.isFailure) warning = "${product.name} se guardó, pero la foto no se subió."
                }
                _uiState.value = _uiState.value.copy(saving = false)
                showNotice(warning ?: "${product.name} guardado", isError = warning != null)
                onDone()
                refreshCatalog()
            }
        }

        fun setAvailable(
            product: Product,
            available: Boolean,
        ) {
            mutate(success = if (available) "${product.name} disponible" else "${product.name} agotado") {
                catalogRepository.setProductAvailability(product.id, available, UUID.randomUUID().toString()).map { }
            }
        }

        fun removeImage(product: Product) {
            mutate(success = "Foto de ${product.name} quitada") {
                adminRepository.deleteProductImage(product.id, UUID.randomUUID().toString()).map { }
            }
        }

        // endregion

        // region Pedidos

        fun selectOrderFilter(filter: AdminOrderFilter) {
            if (filter == _uiState.value.orderFilter) return
            _uiState.value = _uiState.value.copy(orderFilter = filter, orders = emptyList(), ordersCursor = null)
            refreshOrders()
        }

        fun refreshOrders() = loadOrders(append = false)

        fun loadMoreOrders() {
            if (_uiState.value.ordersCursor != null) loadOrders(append = true)
        }

        private fun loadOrders(append: Boolean) {
            val filter = _uiState.value.orderFilter
            val cursor = if (append) _uiState.value.ordersCursor else null
            ordersJob?.cancel()
            _uiState.value = _uiState.value.copy(ordersLoading = true, ordersError = null)
            ordersJob =
                viewModelScope.launch {
                    val result = withContext(Dispatchers.IO) { adminRepository.orders(filter.states, cursor) }
                    if (_uiState.value.orderFilter != filter) return@launch
                    _uiState.value =
                        result.fold(
                            onSuccess = { page ->
                                _uiState.value.copy(
                                    orders = if (append) _uiState.value.orders + page.orders else page.orders,
                                    ordersCursor = page.cursor,
                                    ordersLoading = false,
                                )
                            },
                            onFailure = {
                                _uiState.value.copy(
                                    ordersLoading = false,
                                    ordersError = it.toUserFacingMessage("No se pudieron cargar los pedidos."),
                                )
                            },
                        )
                }
        }

        // endregion

        fun dismissNotice() {
            noticeJob?.cancel()
            _uiState.value = _uiState.value.copy(notice = null)
        }

        private fun mutate(
            success: String,
            onDone: () -> Unit = {},
            call: () -> Result<Unit>,
        ) {
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { call() }
                _uiState.value = _uiState.value.copy(saving = false)
                result.fold(
                    onSuccess = {
                        showNotice(success, isError = false)
                        onDone()
                    },
                    onFailure = { showNotice(it.toUserFacingMessage("No se pudo guardar."), isError = true) },
                )
                refreshCatalog()
            }
        }

        private fun showNotice(
            text: String,
            isError: Boolean,
        ) {
            noticeJob?.cancel()
            _uiState.value = _uiState.value.copy(notice = text, noticeIsError = isError)
            noticeJob =
                viewModelScope.launch {
                    delay(NOTICE_MS)
                    _uiState.value = _uiState.value.copy(notice = null)
                }
        }

        private companion object {
            const val NOTICE_MS = 3_500L
        }
    }

/** Una foto ya preparada para subir. */
class AdminImage(
    val bytes: ByteArray,
    val filename: String,
    val mimeType: String,
)
