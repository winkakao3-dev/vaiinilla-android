package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail

data class OperationalUiState(
    val role: OperationalRole? = null,
    val orders: List<OrderDetail> = emptyList(),
    val selectedOrderId: String? = null,
    val loading: Boolean = false,
    val acting: Boolean = false,
    val cashSessionOpen: Boolean? = null,
    val errorMessage: String? = null,
    val lastSyncedAt: String? = null,
) {
    val selectedOrder: OrderDetail?
        get() = orders.firstOrNull { it.summary.id == selectedOrderId }
}
