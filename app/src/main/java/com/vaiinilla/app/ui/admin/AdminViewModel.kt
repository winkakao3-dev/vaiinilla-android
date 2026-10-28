package com.vaiinilla.app.ui.admin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminLiveCounts
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.AdminProductRules
import com.vaiinilla.app.domain.admin.AdminSummary
import com.vaiinilla.app.domain.admin.AdminUnpaidAccount
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.AdminRepository
import com.vaiinilla.app.domain.repository.AdminSpacesRepository
import com.vaiinilla.app.domain.repository.AdminStoreRepository
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import com.vaiinilla.app.domain.repository.OrderRepositoryException
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
    /** Periodo elegido a mano ("Personalizado"); manda sobre [preset]. */
    val customPeriod: AdminPeriod? = null,
    val live: AdminLiveCounts? = null,
    /** Sesión de caja abierta; null si está cerrada. [cashLoaded] dice si ya se consultó. */
    val cashSessionId: String? = null,
    val cashOpeningAmount: String? = null,
    val cashOpenedAt: String? = null,
    val cashLoaded: Boolean = false,
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
        private val storeRepository: AdminStoreRepository,
        private val spacesRepository: AdminSpacesRepository,
        private val cashSessionRepository: CashSessionRepository,
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
            if (preset == _uiState.value.preset &&
                _uiState.value.customPeriod == null &&
                _uiState.value.summary != null
            ) {
                return
            }
            _uiState.value = _uiState.value.copy(preset = preset, customPeriod = null)
            refreshSummary()
        }

        /** "Personalizado": del día [from] al [to], sin pasar de hoy. */
        fun selectCustomPeriod(period: AdminPeriod) {
            val today = java.time.LocalDate.now()
            val to = minOf(period.to, today)
            val from = minOf(period.from, to)
            _uiState.value = _uiState.value.copy(customPeriod = AdminPeriod(from, to))
            refreshSummary()
        }

        fun refreshHome() {
            refreshSummary()
            refreshCash()
            viewModelScope.launch {
                withContext(Dispatchers.IO) { storeRepository.liveCounts() }.onSuccess {
                    _uiState.value = _uiState.value.copy(live = it)
                }
            }
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
            val custom = _uiState.value.customPeriod
            _uiState.value = _uiState.value.copy(summaryLoading = true, summaryError = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        adminRepository.summary(
                            custom ?: AdminPeriod.lastDays(preset.days),
                        )
                    }
                if (_uiState.value.preset != preset || _uiState.value.customPeriod != custom) return@launch
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

        /** "Cerrar sin cobrar": el cliente se fue sin pagar y la cuenta se cierra sin cobro. */
        fun closeWithoutCharging(
            account: AdminUnpaidAccount,
            note: String,
            onDone: () -> Unit,
        ) {
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        spacesRepository.closeWithoutCharging(account.spaceId, note, UUID.randomUUID().toString())
                    }
                _uiState.value = _uiState.value.copy(saving = false)
                result.fold(
                    onSuccess = {
                        showNotice("Cuenta de ${account.spaceName} cerrada sin cobrar", isError = false)
                        onDone()
                        refreshHome()
                    },
                    onFailure = { showNotice(it.toUserFacingMessage("No se pudo cerrar la cuenta."), isError = true) },
                )
            }
        }

        // endregion

        // region Caja

        fun refreshCash() {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { cashSessionRepository.activeSession() }.onSuccess {
                    _uiState.value =
                        _uiState.value.copy(
                            cashSessionId = it?.id,
                            cashOpeningAmount = it?.initialAmount,
                            cashOpenedAt = it?.openedAt,
                            cashLoaded = true,
                        )
                }
            }
        }

        fun openCash(
            initialAmount: String,
            onDone: () -> Unit,
        ) {
            val amount =
                AdminProductRules.money(initialAmount)
                    ?: return showNotice("Escribe el fondo inicial, por ejemplo 500.", isError = true)
            cashCall("Caja abierta", onDone) { cashSessionRepository.openSession(amount, UUID.randomUUID().toString()) }
        }

        fun closeCash(
            finalAmount: String,
            onDone: () -> Unit,
        ) {
            val sessionId = _uiState.value.cashSessionId ?: return
            val amount =
                AdminProductRules.money(finalAmount)
                    ?: return showNotice("Escribe el efectivo contado, por ejemplo 1250.", isError = true)
            cashCall(
                "Caja cerrada",
                onDone,
            ) { cashSessionRepository.closeSession(sessionId, amount, UUID.randomUUID().toString()) }
        }

        private fun cashCall(
            success: String,
            onDone: () -> Unit,
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
                    onFailure = {
                        val code = (it as? OrderRepositoryException)?.code ?: (it as? CatalogRepositoryException)?.code
                        showNotice(
                            if (code == "CASH_SESSION_HAS_OPEN_ACCOUNTS") {
                                "Hay cuentas de mesas por cobrar: cóbralas o ciérralas antes de cerrar la caja."
                            } else {
                                it.toUserFacingMessage("No se pudo guardar la caja.")
                            },
                            isError = true,
                        )
                    },
                )
                refreshCash()
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
