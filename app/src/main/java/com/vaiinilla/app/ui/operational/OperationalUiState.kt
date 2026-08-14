package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.model.WalletReloadReceipt

data class OperationalUiState(
    val role: OperationalRole? = null,
    val orders: List<OrderDetail> = emptyList(),
    val selectedOrderId: String? = null,
    val loading: Boolean = false,
    val acting: Boolean = false,
    val cashSessionOpen: Boolean? = null,
    val errorMessage: String? = null,
    val lastSyncedAt: String? = null,
    val walletClients: List<WalletClient> = emptyList(),
    val walletSearchLoading: Boolean = false,
    val walletReloadReceipt: WalletReloadReceipt? = null,
    val catalog: Catalog? = null,
) {
    val selectedOrder: OrderDetail?
        get() = orders.firstOrNull { it.summary.id == selectedOrderId }
}
