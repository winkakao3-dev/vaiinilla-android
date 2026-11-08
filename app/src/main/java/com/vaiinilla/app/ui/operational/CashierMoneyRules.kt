package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.data.wallet.PendingWalletReload
import com.vaiinilla.app.domain.model.CashCollectionResult
import com.vaiinilla.app.domain.model.ContractRules
import com.vaiinilla.app.domain.model.Money
import com.vaiinilla.app.domain.model.MoneyInput
import com.vaiinilla.app.domain.repository.CashDrawerRules
import com.vaiinilla.app.domain.repository.WalletRepositoryException
import com.vaiinilla.app.ui.components.moneyLabel
import com.vaiinilla.app.ui.components.rentalChargeLabel
import java.math.BigDecimal

/**
 * Las reglas de dinero de Caja sin estado ni red: qué se acepta antes de mandar un cobro, una recarga, un
 * movimiento del cajón o una apertura, y con qué texto se rechaza. El ViewModel decide cuándo preguntar; aquí se
 * decide la respuesta.
 */
internal object CashierMoneyRules {
    /** El cobro en efectivo ya normalizado a la forma del contrato, o por qué no se puede mandar. */
    sealed interface CollectionCheck {
        data class Valid(
            val wireAmount: String,
        ) : CollectionCheck

        data class Invalid(
            val message: String,
        ) : CollectionCheck
    }

    /**
     * [cashDue] es lo que el pedido pide en efectivo, si se conoce; sin él decide el servidor. [tip] es la
     * propina tal como la escribió el cajero.
     */
    fun checkCollection(
        amountReceived: String,
        cashDue: String?,
        tip: String?,
    ): CollectionCheck {
        // "200" o "200,5" son montos válidos: el contrato los quiere con dos decimales ("200.00").
        val wire = MoneyInput.toWire(amountReceived) ?: amountReceived.trim()
        val received =
            wire
                .takeIf { ContractRules.isValidMoney(it) }
                ?.let { runCatching { BigDecimal(it) }.getOrNull() }
                ?: return CollectionCheck.Invalid("Escribe el efectivo recibido (ej. 100 o 100.50).")
        val total = cashDue?.let { runCatching { Money.parse(it) }.getOrNull() }
        val tipAmount = tip?.let { runCatching { BigDecimal(it) }.getOrNull() } ?: BigDecimal.ZERO
        if (total != null && received < total + tipAmount) {
            return CollectionCheck.Invalid("El efectivo recibido no cubre el total del pedido más la propina.")
        }
        return CollectionCheck.Valid(wire)
    }

    sealed interface ReloadCheck {
        data class Valid(
            val amount: String,
        ) : ReloadCheck

        data class Invalid(
            val message: String,
        ) : ReloadCheck
    }

    fun checkReload(amount: String): ReloadCheck {
        val normalized = amount.trim()
        val valid =
            ContractRules.isValidMoney(normalized) &&
                runCatching { BigDecimal(normalized) > BigDecimal.ZERO }.getOrDefault(false)
        return if (valid) {
            ReloadCheck.Valid(normalized)
        } else {
            ReloadCheck.Invalid("El monto debe ser positivo y tener dos decimales (ej. 100.00).")
        }
    }

    /** El mensaje con que se rechaza un movimiento del cajón, o null si el monto y el motivo sirven. */
    fun drawerMovementError(
        amount: String,
        reason: String,
    ): String? {
        val positive = amount.toBigDecimalOrNull()?.signum() == 1
        if (positive && CashDrawerRules.validReason(reason)) return null
        return "Escribe un monto mayor a cero y un motivo de ${CashDrawerRules.REASON_MIN} a " +
            "${CashDrawerRules.REASON_MAX} letras."
    }

    /** El fondo con que se abre la caja en la forma del contrato; null si no se entiende (0 sí vale). */
    fun openingWireAmount(initialAmount: String): String? = MoneyInput.toWire(initialAmount)

    const val OPENING_AMOUNT_ERROR = "Escribe el efectivo con el que abres la caja (puede ser 0)."

    /** Texto del aviso de una recarga sin confirmar que estorba a otra distinta. */
    fun pendingReloadBlockMessage(pending: PendingWalletReload): String =
        "Hay una recarga anterior sin confirmar (${pendingReloadLabel(pending)}). " +
            "Confírmala o descártala antes de hacer otra."
}

/** «Pedido #12 cobrado · Cambio: $30.00» o «… · Sin cambio»; una renta dice «cobrada» con su cargo. */
internal fun cashCollectionNotice(receipt: CashCollectionResult): String {
    val change = runCatching { Money.parse(receipt.change) }.getOrNull()
    val what =
        if (receipt.order.reservation != null) {
            "${rentalChargeLabel(receipt.order)} cobrada"
        } else {
            "Pedido #${receipt.order.summary.folio} cobrado"
        }
    return if (change != null && change > BigDecimal.ZERO) {
        "$what · Cambio: ${moneyLabel(receipt.change)}"
    } else {
        "$what · Sin cambio"
    }
}

/**
 * Una recarga rechazada por el servidor (4xx) nunca abonó saldo: el registro
 * pendiente se puede descartar. Errores de red/timeout/5xx son ambiguos — el
 * POST pudo haber llegado — así que el registro se conserva para el replay.
 */
internal fun isDefinitiveWalletReloadFailure(error: Throwable): Boolean =
    (error as? WalletRepositoryException)?.httpStatus.let { status ->
        status != null && status in 400..499 && status != HTTP_REQUEST_TIMEOUT && status != HTTP_TOO_MANY_REQUESTS
    }

private const val HTTP_REQUEST_TIMEOUT = 408
private const val HTTP_TOO_MANY_REQUESTS = 429

/** Una recarga persistida de otro turno ya no se repite sola: hace falta que el cajero la reconozca. */
internal fun isStalePendingReload(
    pending: PendingWalletReload,
    nowMs: Long,
): Boolean = pending.storedAtEpochMs <= 0L || nowMs - pending.storedAtEpochMs > PENDING_RELOAD_AUTO_RETRY_MS

private const val PENDING_RELOAD_AUTO_RETRY_MS = 30 * 60_000L

internal fun pendingReloadLabel(pending: PendingWalletReload): String =
    moneyLabel(pending.amount) + (pending.userName?.let { " a $it" }.orEmpty())
