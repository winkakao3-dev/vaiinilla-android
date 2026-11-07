package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.core.notifications.DeviceTokenRegistrar
import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import com.vaiinilla.app.domain.repository.DeviceIdentity
import com.vaiinilla.app.domain.usecase.CancelOrderUseCase
import com.vaiinilla.app.domain.usecase.GetOrderUseCase
import com.vaiinilla.app.domain.usecase.ListOrdersUseCase
import com.vaiinilla.app.domain.usecase.TransitionOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Mesero: su pantalla vive en [WaiterViewModel] (mesas, llamadas, rentas). Este lleva solo lo que el rol necesita
 * del lado operativo: el token de avisos, la presencia del dispositivo y los pedidos que el servidor le ofrece.
 */
@HiltViewModel
class WaiterOrdersViewModel
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
            role = OperationalRole.WAITER,
            listOrders = listOrders,
            getOrder = getOrder,
            transitionOrder = transitionOrder,
            cancelOrder = cancelOrder,
            heartbeatRepository = heartbeatRepository,
            deviceIdentity = deviceIdentity,
            deviceTokenRegistrar = deviceTokenRegistrar,
            sessionStore = sessionStore,
            catalogRepository = catalogRepository,
        )
