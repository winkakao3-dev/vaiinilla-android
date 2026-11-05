package com.vaiinilla.app.ui.admin

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.network.RetryKeys
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminLiveCounts
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.AdminProductRules
import com.vaiinilla.app.domain.admin.AdminSummary
import com.vaiinilla.app.domain.admin.AdminUnpaidAccount
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.domain.admin.FORCED_CLOSE_REASON_MIN
import com.vaiinilla.app.domain.admin.ForcedCloseResult
import com.vaiinilla.app.domain.model.AgendaDay
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.HomeOrderAction
import com.vaiinilla.app.domain.model.HomeOrderBoard
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.AdminRepository
import com.vaiinilla.app.domain.repository.AdminSpacesRepository
import com.vaiinilla.app.domain.repository.AdminStoreRepository
import com.vaiinilla.app.domain.repository.CashLastClose
import com.vaiinilla.app.domain.repository.CashSessionRepository
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import com.vaiinilla.app.domain.time.BusinessTime
import com.vaiinilla.app.ui.components.moneyLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
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
private val NOT_CHARGED =
    setOf(OrderState.PENDING_PAYMENT, OrderState.CANCELED, OrderState.EXPIRED, OrderState.NOT_PICKED_UP)

enum class AdminOrderFilter(
    val label: String,
    val states: Set<OrderState>,
) {
    ACTIVE("Activos", setOf(OrderState.PENDING_PAYMENT, OrderState.PAID, OrderState.PREPARING, OrderState.READY)),
    DELIVERED("Entregados", setOf(OrderState.DELIVERED)),
    INCIDENTS("Incidencias", setOf(OrderState.CANCELED, OrderState.NOT_PICKED_UP, OrderState.EXPIRED)),
    ALL("Todos", emptySet()),
    ;

    /** Con domicilio o encargos, «Activos» suma lo que espera aceptarse y lo que va en camino (CASA-09). */
    fun states(homeOrders: Boolean): Set<OrderState> =
        if (this == ACTIVE && homeOrders) states + OrderState.AWAITING_ACCEPTANCE + OrderState.ON_THE_WAY else states
}

/** «Por aceptar»: lo que llegó en su consulta propia más lo que trajo la lista, sin repetir y del más viejo al nuevo. */
internal fun awaitingAcceptance(
    fetched: List<OrderDetail>,
    listed: List<OrderDetail>,
): List<OrderDetail> =
    (fetched + listed)
        .filter { it.summary.state == OrderState.AWAITING_ACCEPTANCE }
        .distinctBy { it.summary.id }

data class AdminUiState(
    val preset: AdminPeriodPreset = AdminPeriodPreset.TODAY,
    /** Periodo elegido a mano ("Personalizado"); manda sobre [preset]. */
    val customPeriod: AdminPeriod? = null,
    val live: AdminLiveCounts? = null,
    /** Sesión de caja abierta; null si está cerrada. [cashLoaded] dice si ya se consultó. */
    val cashSessionId: String? = null,
    val cashOpeningAmount: String? = null,
    /** El fondo que propone el servidor para abrir caja; null si no llegó. */
    val suggestedCashFloat: String? = null,
    val cashOpenedAt: String? = null,
    val cashLoaded: Boolean = false,
    val cashCheckFailed: Boolean = false,
    /** Arqueo: lo que debería haber al cerrar; null si el servidor no lo manda. */
    val cashExpected: String? = null,
    /** Lo entregado hoy, que en el dibujo va debajo de los activos. */
    val deliveredToday: List<OrderDetail> = emptyList(),
    val cashLastClose: CashLastClose? = null,
    /** Los cobros en efectivo de la sesión abierta, para la lista de Caja. */
    val cashOrders: List<OrderDetail> = emptyList(),
    val summary: AdminSummary? = null,
    val summaryLoading: Boolean = false,
    val summaryError: String? = null,
    val operationalStatus: OperationalStatus? = null,
    val unpaid: AdminUnpaidAccounts? = null,
    val catalog: Catalog? = null,
    /** Lo archivado: fuera del menú, a la vista para desarchivarlo. */
    val archivedProducts: List<Product> = emptyList(),
    val catalogError: String? = null,
    val orderFilter: AdminOrderFilter = AdminOrderFilter.ACTIVE,
    val orders: List<OrderDetail> = emptyList(),
    val ordersCursor: String? = null,
    val ordersLoading: Boolean = false,
    val ordersError: String? = null,
    /**
     * Los encargos que esperan respuesta, pedidos aparte (T02): «Activos» llega del más viejo al más nuevo de 30 en
     * 30, y un encargo recién hecho quedaba fuera de la primera página detrás de los pendientes viejos.
     */
    val awaitingOrders: List<OrderDetail> = emptyList(),
    /** Repostería (CASA-09): de hoy a 15 días, y lo entregado que falta cobrar de los últimos 30. */
    val agenda: List<AgendaDay> = emptyList(),
    val owed: List<OrderDetail> = emptyList(),
    val agendaLoading: Boolean = false,
    val agendaError: String? = null,
    /** Algo del menú se está guardando: los botones de guardar se deshabilitan. */
    val saving: Boolean = false,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
) {
    /** El negocio entrega a domicilio o toma encargos: Pedidos suma la agenda, «Me deben» y los estados nuevos. */
    val takesHomeOrders: Boolean
        get() = operationalStatus?.homeDelivery != null || operationalStatus?.preorders != null
}

@HiltViewModel
class AdminViewModel
    @Inject
    constructor(
        private val adminRepository: AdminRepository,
        private val catalogRepository: CatalogRepository,
        private val storeRepository: AdminStoreRepository,
        private val spacesRepository: AdminSpacesRepository,
        private val cashSessionRepository: CashSessionRepository,
        private val orderRepository: OrderRepository,
    ) : ViewModel() {
        private val _uiState = mutableStateOf(AdminUiState())
        val uiState: State<AdminUiState> = _uiState
        private var noticeJob: Job? = null
        private var ordersJob: Job? = null
        private var visibleScope: String? = null

        /** Un reintento tras perder la respuesta reusa la llave: el servidor no lo hace dos veces. */
        private val retryKeys = RetryKeys()

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
                val tookHomeOrders = _uiState.value.takesHomeOrders
                _uiState.value =
                    _uiState.value.copy(
                        operationalStatus = status.getOrNull() ?: _uiState.value.operationalStatus,
                        unpaid = unpaid.getOrNull() ?: _uiState.value.unpaid,
                    )
                // Al entrar, «Activos» se pidió antes de saber si el negocio toma encargos y salió sin los que esperan
                // respuesta ni los que van en camino (T02): se vuelve a pedir con ellos.
                if (!tookHomeOrders &&
                    _uiState.value.takesHomeOrders &&
                    _uiState.value.orderFilter == AdminOrderFilter.ACTIVE
                ) {
                    refreshOrders()
                }
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
            reason: String,
            onDone: (Boolean) -> Unit,
        ) {
            if (_uiState.value.saving) return onDone(false)
            val motivo = reason.trim()
            // Contrato D: el servidor pide de 3 a 240 letras; el diálogo no deja confirmar con menos.
            if (motivo.length < FORCED_CLOSE_REASON_MIN) return onDone(false)
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        retryKeys.withKey("close-unpaid", account.spaceId, motivo) { key ->
                            spacesRepository.closeWithoutCharging(account.spaceId, motivo, key)
                        }
                    }
                _uiState.value = _uiState.value.copy(saving = false)
                result.fold(
                    onSuccess = { closed ->
                        showNotice(forcedCloseNotice(account.spaceName, closed), isError = false)
                        onDone(true)
                        refreshHome()
                    },
                    onFailure = {
                        showNotice(it.toUserFacingMessage("No se pudo cerrar la cuenta."), isError = true)
                        onDone(false)
                    },
                )
            }
        }

        // endregion

        // region Caja

        fun refreshCash() {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { cashSessionRepository.activeSession() }
                    .onSuccess {
                        _uiState.value =
                            _uiState.value.copy(
                                cashSessionId = it?.id,
                                cashOpeningAmount = it?.initialAmount,
                                suggestedCashFloat = cashSessionRepository.suggestedFloat,
                                cashExpected = cashSessionRepository.expectedCash,
                                cashOpenedAt = it?.openedAt,
                                cashLastClose =
                                    mergeLastClose(cashSessionRepository.lastClose, _uiState.value.cashLastClose),
                                cashLoaded = true,
                                cashCheckFailed = false,
                            )
                        if (it !=
                            null
                        ) {
                            loadCashOrders(it.openedAt)
                        } else {
                            _uiState.value =
                                _uiState.value.copy(cashOrders = emptyList())
                        }
                    }.onFailure {
                        if (!_uiState.value.cashLoaded) _uiState.value = _uiState.value.copy(cashCheckFailed = true)
                    }
            }
        }

        private suspend fun loadCashOrders(openedAt: String?) {
            val page = withContext(Dispatchers.IO) { adminRepository.orders(emptySet(), null) }.getOrNull() ?: return
            val since = openedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
            _uiState.value =
                _uiState.value.copy(
                    cashOrders =
                        page.orders.filter { order ->
                            val summary = order.summary
                            val paidInCash =
                                summary.chargedMethod?.lowercase() == PaymentMethod.CASH.wireValue ||
                                    (summary.chargedMethod == null && summary.paymentMethod == PaymentMethod.CASH)
                            val charged = summary.state !in NOT_CHARGED
                            val created = runCatching { Instant.parse(summary.createdAt) }.getOrNull()
                            paidInCash &&
                                charged &&
                                !summary.paymentPending &&
                                (since == null || created == null || !created.isBefore(since))
                        },
                )
        }

        fun openCash(
            initialAmount: String,
            onDone: () -> Unit,
        ) {
            val amount =
                AdminProductRules.money(initialAmount)
                    ?: return showNotice("Escribe el fondo inicial, por ejemplo 500.", isError = true)
            cashCall("Caja abierta", onDone) {
                retryKeys.withKey("open-cash", amount) { key -> cashSessionRepository.openSession(amount, key) }
            }
        }

        fun closeCash(
            finalAmount: String,
            onDone: () -> Unit,
        ) {
            val sessionId = _uiState.value.cashSessionId ?: return
            val amount =
                AdminProductRules.money(finalAmount)
                    ?: return showNotice("Escribe el efectivo contado, por ejemplo 1250.", isError = true)
            val current = _uiState.value
            cashCall(
                "Caja cerrada",
                {
                    _uiState.value =
                        _uiState.value.copy(
                            cashLastClose =
                                CashLastClose(
                                    counted = amount,
                                    closedAt = Instant.now().toString(),
                                    opening = current.cashOpeningAmount,
                                    expected = current.cashExpected,
                                    closedByMe = true,
                                ),
                        )
                    onDone()
                },
            ) {
                retryKeys.withKey("close-cash", sessionId, amount) { key ->
                    cashSessionRepository.closeSession(sessionId, amount, key)
                }
            }
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
                val remembered = withContext(Dispatchers.IO) { adminRepository.archivedProducts() }
                _uiState.value =
                    result.fold(
                        onSuccess = { loaded ->
                            val (archived, active) = loaded.products.partition(Product::archived)
                            val activeIds = active.map(Product::id).toSet()
                            // Si volvió al menú desde otro lado, ya no es archivado.
                            remembered.filter { it.id in activeIds }.forEach { adminRepository.forgetArchived(it.id) }
                            // `GET catalogo` no trae los archivados: tras reiniciar salen de lo que se recordó (T03).
                            val known =
                                (remembered + _uiState.value.archivedProducts + archived)
                                    .filter { it.id !in activeIds }
                                    .associateBy(Product::id)
                                    .values
                                    .toList()
                            _uiState.value.copy(
                                catalog = loaded.copy(products = active),
                                archivedProducts = known,
                                catalogError = null,
                            )
                        },
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
                    retryKeys
                        .withKey("create-category", input) { key -> adminRepository.createCategory(input, key) }
                        .map { }
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
                            retryKeys.withKey("create-product", ready) { key ->
                                adminRepository.createProduct(ready, key)
                            }
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

        fun setArchived(
            product: Product,
            archived: Boolean,
        ) {
            mutate(
                success = if (archived) "${product.name} archivado" else "${product.name} volvió al menú",
                onDone = {
                    val others = _uiState.value.archivedProducts.filter { it.id != product.id }
                    _uiState.value =
                        _uiState.value.copy(
                            archivedProducts = if (archived) others + product.copy(archived = true) else others,
                        )
                },
            ) {
                adminRepository.setProductArchived(product.id, archived, UUID.randomUUID().toString())
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
                    val states = filter.states(_uiState.value.takesHomeOrders)
                    val result = withContext(Dispatchers.IO) { adminRepository.orders(states, cursor) }
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
                    if (filter == AdminOrderFilter.ACTIVE && !append) {
                        loadAwaiting()
                        loadDeliveredToday()
                    }
                }
        }

        private suspend fun loadAwaiting() {
            val page =
                withContext(Dispatchers.IO) {
                    adminRepository.orders(setOf(OrderState.AWAITING_ACCEPTANCE), null)
                }.getOrNull() ?: return
            _uiState.value = _uiState.value.copy(awaitingOrders = page.orders)
        }

        private suspend fun loadDeliveredToday() {
            val page =
                withContext(Dispatchers.IO) {
                    adminRepository.orders(AdminOrderFilter.DELIVERED.states, null)
                }.getOrNull() ?: return
            val today =
                java.time.LocalDate
                    .now(BusinessTime.zone)
                    .toString()
            _uiState.value =
                _uiState.value.copy(deliveredToday = page.orders.filter { it.summary.operationalDate == today })
        }

        /** Lo que viene de hoy a 15 días y, para «Me deben», lo entregado en los últimos 30. */
        fun refreshAgenda() {
            if (_uiState.value.agendaLoading) return
            val today = java.time.LocalDate.now(BusinessTime.zone)
            _uiState.value = _uiState.value.copy(agendaLoading = true, agendaError = null)
            viewModelScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        orderRepository.agenda(today.minusDays(30).toString(), today.plusDays(15).toString())
                    }
                _uiState.value =
                    result.fold(
                        onSuccess = { days ->
                            _uiState.value.copy(
                                agenda = days.filter { it.date >= today.toString() },
                                owed = HomeOrderBoard.owed(days.flatMap { it.orders }),
                                agendaLoading = false,
                            )
                        },
                        onFailure = {
                            _uiState.value.copy(
                                agendaLoading = false,
                                agendaError = it.toUserFacingMessage("No se pudo cargar la agenda."),
                            )
                        },
                    )
            }
        }

        /** Aceptar, rechazar, mandar, entregar o cobrar; [onDone] recibe el pedido como quedó. */
        fun homeAction(
            order: OrderDetail,
            action: HomeOrderAction,
            onDone: (OrderDetail) -> Unit,
        ) {
            if (_uiState.value.saving) return
            _uiState.value = _uiState.value.copy(saving = true)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { orderRepository.homeOrderAction(order.summary.id, action) }
                _uiState.value = _uiState.value.copy(saving = false)
                result.fold(
                    onSuccess = { updated ->
                        _uiState.value =
                            _uiState.value.copy(
                                orders =
                                    _uiState.value.orders.map {
                                        if (it.summary.id ==
                                            updated.summary.id
                                        ) {
                                            updated
                                        } else {
                                            it
                                        }
                                    },
                                awaitingOrders =
                                    _uiState.value.awaitingOrders.filter { it.summary.id != updated.summary.id },
                            )
                        showNotice(doneMessage(action), isError = false)
                        onDone(updated)
                        refreshAgenda()
                    },
                    onFailure = { showNotice(it.toUserFacingMessage("No se pudo guardar."), isError = true) },
                )
            }
        }

        private fun doneMessage(action: HomeOrderAction): String =
            when (action) {
                HomeOrderAction.Accept -> "Encargo aceptado"
                is HomeOrderAction.Reject -> "Encargo rechazado; se le devuelve lo que pagó"
                HomeOrderAction.SendOut -> "En camino; al cliente le llegó su código"
                is HomeOrderAction.Deliver -> "Entregado"
                is HomeOrderAction.Collect -> "Cobrado"
                is HomeOrderAction.Cancel -> "Pedido cancelado; al cliente le llega el motivo"
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

/**
 * El cierre del servidor gana, pero si es el mismo que este dispositivo acaba de hacer
 * (mismo monto contado) conserva el «Cerró: Tú».
 */
internal fun mergeLastClose(
    server: CashLastClose?,
    local: CashLastClose?,
): CashLastClose? {
    if (server == null) return local
    if (local == null || !local.closedByMe) return server
    val sameCount =
        server.counted.toBigDecimalOrNull()?.let { counted ->
            local.counted.toBigDecimalOrNull()?.compareTo(counted) == 0
        } == true
    return if (sameCount) server.copy(closedByMe = true) else server
}

/** Una foto ya preparada para subir. */
class AdminImage(
    val bytes: ByteArray,
    val filename: String,
    val mimeType: String,
)

/** R3: lo que dejó «Cerrar sin cobrar», en una línea: cuánto se perdió, cuánto abonado sí entró y qué salió de Cocina. */
internal fun forcedCloseNotice(
    spaceName: String,
    closed: ForcedCloseResult,
): String {
    val lost = closed.lost.toBigDecimalOrNull()?.signum() == 1
    val paid = closed.paid.toBigDecimalOrNull()?.signum() == 1
    return buildList {
        add("$spaceName cerrada sin cobrar")
        if (lost) add("se perdieron ${moneyLabel(closed.lost)}")
        if (paid) add("lo abonado (${moneyLabel(closed.paid)}) sí entró a la caja")
        when (closed.cancelled) {
            0 -> Unit
            1 -> add("1 pedido cancelado")
            else -> add("${closed.cancelled} pedidos cancelados")
        }
    }.joinToString(" · ")
}
