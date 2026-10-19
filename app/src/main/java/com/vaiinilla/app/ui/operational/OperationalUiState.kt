package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.model.WalletReloadReceipt
import com.vaiinilla.app.domain.repository.StoreImageKind
import com.vaiinilla.app.domain.repository.StoreImages

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
    /** Si la última carga de pedidos salió bien; null mientras no termina ninguna para este rol. */
    val lastRefreshOk: Boolean? = null,
    val lastSyncedAt: String? = null,
    val walletClients: List<WalletClient> = emptyList(),
    val walletSearchLoading: Boolean = false,
    val walletReloadReceipt: WalletReloadReceipt? = null,
    val catalog: Catalog? = null,
    /** Logo y foto del negocio para la tarjeta de Caja; null si el servidor aún no los ofrece a Caja. */
    val storeImages: StoreImages? = null,
    /** Cuál de las dos imágenes se está subiendo o quitando. */
    val storeImageBusy: StoreImageKind? = null,
    /** Lo último que dijo la tarjeta: subida, quitada o por qué no se pudo. */
    val storeImageNotice: String? = null,
    val cashChangeNotice: String? = null,
    /** Confirmación de una acción que el servidor ya aceptó (por ejemplo, "Comanda #12 lista"). */
    val actionNotice: String? = null,
    /** Hoja de cierre de caja; null mientras está cerrada. */
    val closeCash: CloseCashUi? = null,
) {
    val selectedOrder: OrderDetail?
        get() = orders.firstOrNull { it.summary.id == selectedOrderId }
}

/** Qué ocupa el lugar de la fila de pedidos de Cocina o Caja. */
internal enum class QueuePresentation { Content, Loading, Error, Empty }

/** "Al día" o "Nada por cobrar" solo se dicen si la última carga salió bien. */
internal fun queuePresentation(
    hasOrders: Boolean,
    lastRefreshOk: Boolean?,
): QueuePresentation =
    when {
        hasOrders -> QueuePresentation.Content
        lastRefreshOk == null -> QueuePresentation.Loading
        lastRefreshOk == false -> QueuePresentation.Error
        else -> QueuePresentation.Empty
    }

/** Estado de la hoja "Cerrar caja". */
data class CloseCashUi(
    val closing: Boolean = false,
    /** El servidor rechazó el cierre porque hay cuentas a pagar al final sin cobrar. */
    val blockedByAccounts: Boolean = false,
    val errorMessage: String? = null,
)
