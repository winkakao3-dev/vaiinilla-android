package com.vaiinilla.app.domain.model

/**
 * Las transiciones que el personal pide con `POST /pedidos/{id}/transiciones`, tal como las acepta el servidor
 * (`veredicto` en `transiciones.service.js` del backend). El servidor sigue siendo quien decide; esta tabla deja
 * escrito en un solo lugar lo que antes cada pantalla deducía por su cuenta, para no mandar una petición que ya
 * se sabe que va a responder 409.
 *
 * Lo demás no pasa por aquí: cobrar (Caja), cancelar o rechazar (cancelaciones), aceptar, mandar y entregar a
 * domicilio (`HomeOrderAction`) y lo que el servidor hace solo (expirar, pasar a listo un pedido sin cocina).
 */
object OrderTransitions {
    private val requestable: Map<OrderState, Set<OrderState>> =
        mapOf(
            // Cocina puede arrancar un pedido en efectivo antes de que se cobre.
            OrderState.PREPARING to setOf(OrderState.PENDING_PAYMENT, OrderState.PAID),
            OrderState.READY to setOf(OrderState.PREPARING),
            OrderState.DELIVERED to setOf(OrderState.READY),
        )

    /** Estados desde los que el personal puede pedir [target]; vacío si [target] no se pide por transición. */
    fun sourcesOf(target: OrderState): Set<OrderState> = requestable[target].orEmpty()

    /** Si tiene sentido pedir pasar de [from] a [target]. */
    fun canRequest(
        from: OrderState,
        target: OrderState,
    ): Boolean = from in sourcesOf(target)
}
