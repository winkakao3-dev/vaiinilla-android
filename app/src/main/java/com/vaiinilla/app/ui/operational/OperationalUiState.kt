package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.model.WalletReloadReceipt

data class OperationalUiState(
    val role: OperationalRole? = null,
    val orders: List<OrderDetail> = emptyList(),
    val latestClientOrder: OrderDetail? = null,
    val menuOrder: OrderDetail? = null,
    val selectedOrderId: String? = null,
    val loading: Boolean = false,
    val acting: Boolean = false,
    val cashSessionOpen: Boolean? = null,
    val heartbeatOnline: Boolean? = null,
    val errorMessage: String? = null,
    val lastSyncedAt: String? = null,
    val walletClients: List<WalletClient> = emptyList(),
    val walletSearchLoading: Boolean = false,
    val walletReloadReceipt: WalletReloadReceipt? = null,
    val catalog: Catalog? = null,
    val cashChangeNotice: String? = null,
    /** Hoja de cierre de caja; null mientras está cerrada. */
    val closeCash: CloseCashUi? = null,
) {
    val selectedOrder: OrderDetail?
        get() = orders.firstOrNull { it.summary.id == selectedOrderId }
}

/** Estado de la hoja "Cerrar caja". */
data class CloseCashUi(
    val closing: Boolean = false,
    /** El servidor rechazó el cierre porque hay cuentas a pagar al final sin cobrar. */
    val blockedByAccounts: Boolean = false,
    val errorMessage: String? = null,
)
