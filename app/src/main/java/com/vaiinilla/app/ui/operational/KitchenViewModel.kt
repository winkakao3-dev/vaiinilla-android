package com.vaiinilla.app.ui.operational

import androidx.lifecycle.viewModelScope
import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
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
import kotlinx.coroutines.launch
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

        fun markReady(
            orderId: String,
            expectedVersion: Int,
        ) {
            if (staleForTransition(orderId, OrderState.READY)) return
            val version = freshVersion(orderId, expectedVersion)
            performMutation(successNotice = ::kitchenActionNotice) {
                transitionOrder(
                    orderId = orderId,
                    targetState = OrderState.READY,
                    expectedVersion = version,
                    idempotencyKey =
                        MutationIdempotency.orderTransition(
                            orderId,
                            OrderState.READY.wireValue,
                            version,
                            pickupToken = null,
                        ),
                ).getOrThrow()
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
