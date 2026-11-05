package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.model.WalletReloadReceipt
import com.vaiinilla.app.domain.repository.CashDrawerReport
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
    /** Alta, foto o pausa de un producto en curso; aparte de `acting` para no apagar los botones de los pedidos. */
    val catalogBusy: Boolean = false,
    val cashSessionOpen: Boolean? = null,
    /** El fondo que propone el servidor para abrir caja; null si no llegó. */
    val suggestedCashFloat: String? = null,
    /** Lo que debería haber en el cajón (`efectivo_esperado`), para comparar al cerrar. */
    val expectedCash: String? = null,
    /** Con qué fondo se abrió la caja (`monto_inicial`) y a qué hora (`abierta_en`, ISO en UTC); null si no se sabe. */
    val cashOpeningAmount: String? = null,
    val cashOpenedAt: String? = null,
    /** La caja nunca se pudo leer (sin red al entrar): la pantalla ofrece reintentar en vez de callar. */
    val cashSessionCheckFailed: Boolean = false,
    val heartbeatOnline: Boolean? = null,
    val errorMessage: String? = null,
    /** Si la última carga de pedidos salió bien; null mientras no termina ninguna para este rol. */
    val lastRefreshOk: Boolean? = null,
    val lastSyncedAt: String? = null,
    val walletClients: List<WalletClient> = emptyList(),
    val walletSearchLoading: Boolean = false,
    val walletReloadReceipt: WalletReloadReceipt? = null,
    /** Recarga en efectivo que quedó sin confirmar; Caja la muestra para confirmarla o descartarla. */
    val pendingWalletReload: PendingReloadUi? = null,
    val catalog: Catalog? = null,
    /** N03: Cocina no pudo leer el catálogo (sin red o el servidor no se lo deja); la hoja «¿Qué se acabó?» lo dice. */
    val catalogFailed: Boolean = false,
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
    /**
     * Cocina: `cocina_prepara_sin_cobrar` del estado operativo. Con false, un pedido por cobrar no se puede
     * empezar hasta que Caja lo cobre. Sin leerlo todavía vale true, como siempre (R14).
     */
    val kitchenPreparesUnpaid: Boolean = true,
    /**
     * Pedidos que Caja cobró en efectivo sin internet y que el servidor aún no registra: ese dinero ya está en el
     * cajón pero no en el esperado. Mientras haya alguno no se cierra la caja (R5).
     */
    val unsyncedOfflineCash: Int = 0,
    /** Hoja «Movimientos del cajón» y desglose del cierre: el resumen y los renglones de la sesión abierta. */
    val cashDrawer: CashDrawerUi = CashDrawerUi(),
) {
    val selectedOrder: OrderDetail?
        get() = orders.firstOrNull { it.summary.id == selectedOrderId }

    /** La caja en un solo valor; las banderas sueltas de arriba se leen siempre a través de esto. */
    val cashSession: CashSession
        get() =
            when (cashSessionOpen) {
                null -> if (cashSessionCheckFailed) CashSession.CheckFailed else CashSession.Unknown
                false -> CashSession.Closed
                true ->
                    when {
                        closeCash == null -> CashSession.Open
                        closeCash.blockedByAccounts -> CashSession.CloseBlockedByAccounts
                        closeCash.closing -> CashSession.Closing
                        else -> CashSession.ConfirmingClose
                    }
            }
}

/**
 * Los estados de la caja de un turno. Se derivan de `cashSessionOpen`, `cashSessionCheckFailed` y `closeCash` para
 * que una combinación imposible (hoja de cierre con la caja cerrada, error de lectura con la caja abierta) no
 * pueda leerse como un estado.
 */
enum class CashSession {
    /** Todavía no se lee. */
    Unknown,

    /** Nunca se pudo leer (sin red al entrar): la pantalla ofrece reintentar. */
    CheckFailed,
    Closed,
    Open,

    /** La hoja de cierre está abierta, esperando el conteo. */
    ConfirmingClose,
    Closing,

    /** El servidor rechazó el cierre: hay cuentas a pagar al final sin cobrar. */
    CloseBlockedByAccounts,
    ;

    val isOpen: Boolean
        get() = this == Open || this == ConfirmingClose || this == Closing || this == CloseBlockedByAccounts
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

/** Lo que se sabe del cajón de la sesión abierta y si se está anotando una entrada o salida. */
data class CashDrawerUi(
    val loading: Boolean = false,
    val report: CashDrawerReport? = null,
    /** No se pudo leer el cajón; lo que haya en [report] es la última lectura buena. */
    val errorMessage: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    /** Sube con cada movimiento anotado: la hoja limpia el formulario. */
    val savedCount: Int = 0,
)

/** Estado de la hoja "Cerrar caja". */
data class CloseCashUi(
    val closing: Boolean = false,
    /** El servidor rechazó el cierre porque hay cuentas a pagar al final sin cobrar. */
    val blockedByAccounts: Boolean = false,
    val errorMessage: String? = null,
)

/** La recarga sin confirmar tal como la ve el cajero: a quién y cuánto. */
data class PendingReloadUi(
    val userName: String?,
    val amount: String,
)

/** El enlace con el servidor tal como lo ve Cocina: sin primera carga se conecta; un fallo es "sin conexión". */
internal fun operationalLink(lastRefreshOk: Boolean?): WaiterBoardLink =
    when (lastRefreshOk) {
        null -> WaiterBoardLink.Connecting
        true -> WaiterBoardLink.Live
        false -> WaiterBoardLink.Offline
    }

/** Por qué no se puede cerrar la caja todavía: hay efectivo cobrado sin internet sin registrar. */
internal fun unsyncedOfflineCashNotice(count: Int): String =
    if (count == 1) {
        "Hay 1 pedido cobrado sin internet que aún no se registra. Espera a que se mande (o resuélvelo en " +
            "«Pedidos guardados») antes de cerrar: ese efectivo todavía no cuenta en la caja."
    } else {
        "Hay $count pedidos cobrados sin internet que aún no se registran. Espera a que se manden (o resuélvelos " +
            "en «Pedidos guardados») antes de cerrar: ese efectivo todavía no cuenta en la caja."
    }

/** Lo último que se supo de un rol de personal en un negocio, al soltarlo. */
internal data class StaffStateSnapshot(
    val role: OperationalRole,
    val establishmentId: String?,
    val takenAtMs: Long,
    val state: OperationalUiState,
) {
    /** Sirve para volver al mismo rol en el mismo negocio poco después; más tarde la foto ya no se parece. */
    fun matches(
        role: OperationalRole,
        establishmentId: String?,
        nowMs: Long,
    ): Boolean =
        role == this.role &&
            establishmentId == this.establishmentId &&
            nowMs - takenAtMs in 0..STAFF_SNAPSHOT_TTL_MS

    companion object {
        const val STAFF_SNAPSHOT_TTL_MS = 5 * 60 * 1_000L
    }
}

/**
 * Pasa a este estado (recién puesto el rol) lo último conocido de [known]: la caja abierta o cerrada, su fondo, el
 * catálogo y, con [restoreOrders], la fila. Así Caja no vuelve a «Revisando la caja» ni a «Cargando pedidos» y el
 * menú conserva «Cerrar caja» mientras se vuelve a pedir; el servidor corrige lo que haya cambiado.
 */
internal fun OperationalUiState.keepKnownStaffState(
    known: OperationalUiState,
    restoreOrders: Boolean,
): OperationalUiState =
    copy(
        cashSessionOpen = cashSessionOpen ?: known.cashSessionOpen,
        suggestedCashFloat = suggestedCashFloat ?: known.suggestedCashFloat,
        expectedCash = expectedCash ?: known.expectedCash,
        cashOpeningAmount = cashOpeningAmount ?: known.cashOpeningAmount,
        cashOpenedAt = cashOpenedAt ?: known.cashOpenedAt,
        catalog = catalog ?: known.catalog,
        // Solo la última lectura del cajón: lo que estaba en curso (cargando, guardando) ya no lo está.
        cashDrawer =
            if (cashDrawer == CashDrawerUi()) {
                known.cashDrawer.copy(loading = false, saving = false, saveError = null)
            } else {
                cashDrawer
            },
        orders = if (restoreOrders && orders.isEmpty()) known.orders else orders,
        lastRefreshOk = if (restoreOrders && lastRefreshOk == null) known.lastRefreshOk else lastRefreshOk,
        lastSyncedAt = lastSyncedAt ?: known.lastSyncedAt,
    )
