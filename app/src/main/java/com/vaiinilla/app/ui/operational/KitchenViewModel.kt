package com.vaiinilla.app.ui.operational

import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
import com.vaiinilla.app.core.runCatchingCancellable
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import com.vaiinilla.app.domain.repository.DeviceIdentity
import com.vaiinilla.app.domain.usecase.CancelOrderUseCase
import com.vaiinilla.app.domain.usecase.GetOrderUseCase
import com.vaiinilla.app.domain.usecase.ListOrdersUseCase
import com.vaiinilla.app.domain.usecase.TransitionOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Cocina: la fila de comandas por preparar y el menú para apagar lo que se acabó. */
@HiltViewModel
class KitchenViewModel
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
    ) : StaffOrdersViewModel(
            role = OperationalRole.KITCHEN,
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
        private var kitchenPolicyLoaded = false
        private val readyQueue = PendingReadyQueue()

        // Las marcas salen una por una: la acción de la fila ignora una segunda mientras la primera sigue en vuelo.
        private val readySending = Mutex()

        override fun onRoleReset(establishmentChanged: Boolean) {
            kitchenPolicyLoaded = false
        }

        // Cocina relee si el negocio prepara lo que falta cobrar (R14): al entrar y con cada sincronización
        // completa, no en cada sondeo.
        override fun onRefreshStarted(
            fullSync: Boolean,
            generation: Long,
        ) {
            if (fullSync || !kitchenPolicyLoaded) refreshKitchenPolicy(generation)
        }

        override fun refreshCatalog() = refreshKitchenCatalog()

        fun startKitchen(
            orderId: String,
            expectedVersion: Int,
        ) {
            if (staleForTransition(orderId, OrderState.PREPARING)) return
            val version = freshVersion(orderId, expectedVersion)
            performMutation(successNotice = ::kitchenActionNotice) {
                transitionOrder(
                    orderId = orderId,
                    targetState = OrderState.PREPARING,
                    expectedVersion = version,
                    idempotencyKey =
                        MutationIdempotency.orderTransition(
                            orderId,
                            OrderState.PREPARING.wireValue,
                            version,
                            pickupToken = null,
                        ),
                ).getOrThrow()
            }
        }

        /**
         * «Marcar lista» con ventana de deshacer: la marca espera [PendingReadyQueue.UNDO_WINDOW_MS] aquí, no en la
         * pantalla, así que girar el teléfono, cambiar de pestaña o salir no la pierde (y varias a la vez se mandan
         * todas).
         */
        fun requestReady(
            orderId: String,
            expectedVersion: Int,
        ) {
            if (state.value.role == null) return
            if (staleForTransition(orderId, OrderState.READY)) return
            readyQueue.add(orderId, freshVersion(orderId, expectedVersion), System.currentTimeMillis())
            publishPendingReady()
            val generation = roleGeneration
            viewModelScope.launch {
                delay(PendingReadyQueue.UNDO_WINDOW_MS)
                if (generation == roleGeneration) sendReady(readyQueue.takeDue(System.currentTimeMillis()))
            }
        }

        fun undoReady(orderId: String) {
            if (readyQueue.undo(orderId)) publishPendingReady()
        }

        /** Manda ya todas las marcas en espera: al salir de la pantalla, con Atrás o al soltar el rol (B04). */
        fun flushPendingReady() {
            val all = readyQueue.drain()
            if (all.isEmpty()) return
            publishPendingReady()
            sendReady(all)
        }

        override fun onBeforeRelease() = flushPendingReady()

        private fun publishPendingReady() {
            if (state.value.role == null) return
            state.value = state.value.copy(pendingReady = readyQueue.snapshot())
        }

        private fun sendReady(items: List<PendingReady>) {
            if (items.isEmpty()) return
            publishPendingReady()
            val generation = roleGeneration
            viewModelScope.launch {
                readySending.withLock { items.forEach { sendOneReady(it, generation) } }
            }
        }

        private suspend fun sendOneReady(
            item: PendingReady,
            generation: Long,
        ) {
            val version = freshVersion(item.orderId, item.version)
            val result =
                runCatchingCancellable {
                    withContext(Dispatchers.IO) {
                        transitionOrder(
                            orderId = item.orderId,
                            targetState = OrderState.READY,
                            expectedVersion = version,
                            idempotencyKey =
                                MutationIdempotency.orderTransition(
                                    item.orderId,
                                    OrderState.READY.wireValue,
                                    version,
                                    pickupToken = null,
                                ),
                        ).getOrThrow()
                    }
                }
            // La petición ya salió aunque se haya soltado el rol; solo la pantalla de otro rol no debe recibirla.
            if (generation != roleGeneration || state.value.role != OperationalRole.KITCHEN) return
            result
                .onSuccess { order ->
                    applyServerOrder(order)
                    state.value = state.value.copy(actionNotice = kitchenActionNotice(order))
                }.onFailure { error ->
                    showMutationError(staffMutationMessage(error))
                    if (isStaleOrderError(error)) requestFullSync()
                }
        }

        private fun refreshKitchenPolicy(generation: Long) {
            kitchenPolicyLoaded = true
            viewModelScope.launch {
                val status = withContext(Dispatchers.IO) { catalogRepository.getOperationalStatus() }.getOrNull()
                if (generation != roleGeneration || state.value.role != OperationalRole.KITCHEN) return@launch
                if (status == null) {
                    // Sin respuesta se vuelve a intentar en el siguiente sondeo; se queda lo último que se supo.
                    kitchenPolicyLoaded = false
                    return@launch
                }
                state.value = state.value.copy(kitchenPreparesUnpaid = status.kitchenPreparesUnpaid)
            }
        }

        /** N03: Cocina lee el menú para apagar lo que se acabó; un fallo se queda en la hoja y no toca la fila de comandas. */
        private fun refreshKitchenCatalog() {
            val generation = roleGeneration
            state.value = state.value.copy(catalogFailed = false)
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { catalogRepository.getCatalog() }
                if (generation != roleGeneration || state.value.role != OperationalRole.KITCHEN) return@launch
                state.value =
                    state.value.copy(
                        catalog = result.getOrNull() ?: state.value.catalog,
                        catalogFailed = result.isFailure,
                    )
            }
        }
    }
