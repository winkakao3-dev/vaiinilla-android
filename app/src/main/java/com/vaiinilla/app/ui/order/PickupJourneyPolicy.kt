package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.PaymentMethod
import kotlin.math.sqrt

/** Los tres pasos de la barra de progreso de la recogida. */
internal enum class PickupStep(
    val label: String,
) {
    ORDERED("Pedido"),
    MAKING("Preparando"),
    READY("Listo"),
}

internal fun pickupStep(state: OrderState): PickupStep? =
    when (state) {
        OrderState.PENDING_PAYMENT, OrderState.PAID -> PickupStep.ORDERED
        OrderState.PREPARING -> PickupStep.MAKING
        OrderState.READY, OrderState.DELIVERED -> PickupStep.READY
        OrderState.CANCELED, OrderState.NOT_PICKED_UP, OrderState.EXPIRED -> null
    }

private fun OrderDetail.cashPending(): Boolean =
    summary.state == OrderState.PENDING_PAYMENT && summary.paymentMethod == PaymentMethod.CASH

private fun OrderDetail.spaceName(): String = summary.space?.name?.takeIf { it.isNotBlank() } ?: "tu espacio"

internal fun pickupTitle(order: OrderDetail): String =
    when (order.summary.state) {
        OrderState.PENDING_PAYMENT -> if (order.cashPending()) "Por pagar" else "Pedido recibido"
        OrderState.PAID -> "Pedido recibido"
        OrderState.PREPARING -> "Preparando"
        OrderState.READY -> "¡Listo!"
        else -> order.summary.state.label
    }

/** Mismo copy que la PWA (`order-labels.ts`). */
internal fun pickupHint(order: OrderDetail): String {
    val state = order.summary.state
    val toSpace = order.summary.destination == OrderDestination.IN_SPACE
    return when {
        order.cashPending() -> "Muestra tu código en Caja para pagar."
        toSpace && state == OrderState.READY -> "¡Listo! Te lo llevan a ${order.spaceName()}."
        toSpace && (state == OrderState.PAID || state == OrderState.PREPARING) -> "Te lo llevan a ${order.spaceName()}."
        state == OrderState.PENDING_PAYMENT -> "Estamos confirmando tu pago."
        state == OrderState.PAID -> "Cocina recibió la comanda."
        state == OrderState.PREPARING -> "Tu comida se está preparando."
        state == OrderState.READY -> "Recógelo en la barra."
        state == OrderState.DELIVERED -> "Pedido completado."
        state == OrderState.CANCELED -> "Este pedido se canceló."
        state == OrderState.NOT_PICKED_UP -> "No se recogió el pedido."
        else -> "Este pedido expiró."
    }
}

internal fun pickupCodeHint(order: OrderDetail): String =
    when {
        order.summary.destination == OrderDestination.IN_SPACE ->
            "Tu mesero te lo lleva. Si te pide el código, muéstrale este."
        order.cashPending() -> "Muéstralo en Caja para pagar."
        else -> "Muéstralo en la barra para recoger tu pedido."
    }

/**
 * "Mostrar código" solo sirve mientras el pedido sigue abierto. En mesa o cancha, además, el negocio
 * debe pedir el QR al entregar, salvo que haya que pagar en Caja.
 */
internal fun pickupCodeUseful(
    order: OrderDetail,
    deliveryRequiresQr: Boolean,
): Boolean =
    order.summary.state in setOf(OrderState.PENDING_PAYMENT, OrderState.PAID, OrderState.PREPARING, OrderState.READY) &&
        (order.summary.destination != OrderDestination.IN_SPACE || deliveryRequiresQr || order.cashPending())

/**
 * El código se abre solo cuando hay que enseñarlo: listo para recoger en barra, o pago en efectivo
 * pendiente. En mesa o cancha lo lleva el mesero, así que no se abre solo.
 */
internal fun shouldAutoRevealPickupCode(
    order: OrderDetail,
    hasCode: Boolean,
): Boolean =
    hasCode &&
        order.summary.destination != OrderDestination.IN_SPACE &&
        (order.summary.state == OrderState.READY || order.cashPending())

/**
 * En qué punto (0–1) del recorrido empieza a girar cada bloque del mosaico hasta su color del QR:
 * del centro hacia afuera, con un poco de desorden estable para que no se vea mecánico.
 * Todos terminan antes de 0.88, así el QR queda completo y escaneable al final.
 */
internal fun mosaicCellStart(
    col: Int,
    row: Int,
    size: Int,
): Float {
    val center = (size - 1) / 2f
    val dx = col - center
    val dy = row - center
    val distance = sqrt(dx * dx + dy * dy) / (center * sqrt(2f)).coerceAtLeast(1f)
    val jitter = (((col * 73_856_093) xor (row * 19_349_663)).toUInt() % 1000u).toFloat() / 1000f
    return MOSAIC_FLIP_FROM + (distance.coerceIn(0f, 1f) * 0.82f + jitter * 0.18f) * MOSAIC_FLIP_SPREAD
}

internal const val MOSAIC_FLIP_FROM = 0.42f
internal const val MOSAIC_FLIP_SPREAD = 0.3f
internal const val MOSAIC_FLIP_LENGTH = 0.14f

/** El lugar del mundo donde se sigue un pedido, como `PickupPlace` en iOS. */
internal enum class PickupPlace {
    /** La fachada de la tienda; el QR sale de ella. */
    STOREFRONT,

    /** Asomados por la vitrina mientras se prepara un pedido para llevar. */
    KITCHEN,

    /** El lugar visto desde arriba, con la mesa (o el espacio) a donde llega el pedido. */
    TABLE,

    /** La cancha rentada vista desde arriba. */
    COURT,
}

/** Los espacios que se ven en el plano con mesas; una cancha no tiene mesas. */
internal fun spaceHasTables(type: String): Boolean = type != "cancha"

internal fun pickupPlace(order: OrderDetail): PickupPlace =
    when {
        order.reservation != null -> PickupPlace.COURT
        order.summary.destination == OrderDestination.IN_SPACE &&
            spaceHasTables(order.summary.space?.type ?: "mesa") -> PickupPlace.TABLE
        order.summary.state == OrderState.PREPARING -> PickupPlace.KITCHEN
        else -> PickupPlace.STOREFRONT
    }

/** Mesas en el plano del Vainiverso (vista desde arriba del lugar). */
internal const val FLOOR_TABLE_COUNT = 6

/**
 * Cuál mesa del plano es la tuya: la del número en el nombre ("Mesa 4" → la cuarta) o, si no tiene, una fija según
 * el id, para que el mismo espacio caiga siempre en el mismo lugar.
 */
internal fun floorTableIndex(
    name: String,
    id: Int,
): Int {
    val number = Regex("\\d+").findAll(name).lastOrNull()?.value?.toIntOrNull()
    if (number != null && number > 0) return (number - 1) % FLOOR_TABLE_COUNT
    return kotlin.math.abs(id) % FLOOR_TABLE_COUNT
}
