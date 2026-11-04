package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.AgendaDay
import com.vaiinilla.app.domain.model.CashCollectionResult
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.model.HomeDeliveryQuote
import com.vaiinilla.app.domain.model.HomeOrderAction
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.StripePaymentSession
import java.time.Instant

interface OrderRepository {
    fun createOrder(
        request: CreateOrderRequest,
        idempotencyKey: String,
    ): Result<CreatedOrder>

    /**
     * Cuerpo de `POST /pedidos` para un pedido tomado sin internet: el mismo que [createOrder] mandaría, más
     * `sin_conexion` con el id que le puso el teléfono, cuándo se tomó y su folio local (migración 161).
     */
    fun offlineOrderBody(
        request: CreateOrderRequest,
        orderId: String,
        capturedAt: Instant,
        localFolio: String,
    ): String = throw UnsupportedOperationException("Los pedidos sin conexión no están disponibles.")

    /** Manda un pedido guardado sin conexión, tal cual se guardó, con la misma llave en cada intento. */
    fun sendOfflineOrder(
        body: String,
        idempotencyKey: String,
    ): Result<CreatedOrder> =
        Result.failure(UnsupportedOperationException("Los pedidos sin conexión no están disponibles."))

    fun getOrder(orderId: String): Result<OrderDetail>

    fun listOrders(
        role: OperationalRole,
        updatedSince: String? = null,
    ): Result<List<OrderDetail>>

    /** Last successfully fetched client order list, when one was cached. */
    fun cachedClientOrders(): List<OrderDetail>? = null

    /**
     * El backend da el QR de recogida solo al crear el pedido. Si este dispositivo no lo tiene
     * (se perdió o el pedido se hizo en otro), el cliente lo vuelve a pedir y queda guardado.
     */
    fun recoverPickupToken(order: OrderDetail): Result<OrderDetail> = Result.success(order)

    fun retryStripePayment(
        orderId: String,
        idempotencyKey: String,
    ): Result<StripePaymentSession>

    fun collectCash(
        orderId: String,
        amountReceived: String,
        expectedVersion: Int,
        idempotencyKey: String,
        tip: String? = null,
    ): Result<CashCollectionResult>

    fun transition(
        orderId: String,
        targetState: OrderState,
        expectedVersion: Int,
        idempotencyKey: String,
        pickupToken: String? = null,
    ): Result<OrderDetail>

    /** El cliente avisa que ya llegó por su pedido (drive-thru). Avisar otra vez no cambia nada. */
    fun announceArrival(orderId: String): Result<OrderDetail> =
        Result.failure(UnsupportedOperationException("Avisar la llegada no está disponible."))

    /** Cancela o rechaza un pedido con un motivo que verá el cliente. */
    fun cancelOrder(
        orderId: String,
        expectedVersion: Int,
        reason: String,
        idempotencyKey: String,
    ): Result<OrderDetail> = Result.failure(UnsupportedOperationException("Cancelar pedidos no está disponible."))

    /** Cocina o Caja quita un artículo con motivo; el resto del pedido sigue y el total baja. */
    fun rejectItem(
        orderId: String,
        itemId: Int,
        expectedVersion: Int,
        reason: String,
        idempotencyKey: String,
    ): Result<OrderDetail> = Result.failure(UnsupportedOperationException("Quitar artículos no está disponible."))

    /** ¿El negocio llega a esta ubicación, y con qué costo? (CASA-05) */
    fun quoteHomeDelivery(
        latitude: Double,
        longitude: Double,
    ): Result<HomeDeliveryQuote> = Result.failure(UnsupportedOperationException("Cotizar el envío no está disponible."))

    /** Aceptar, rechazar, mandar, entregar o cobrar un pedido a domicilio o un encargo (CASA-09). */
    fun homeOrderAction(
        orderId: String,
        action: HomeOrderAction,
    ): Result<OrderDetail> = Result.failure(UnsupportedOperationException("Esta acción no está disponible."))

    /** Pedidos a domicilio y encargos por día, de `from` a `to` (`AAAA-MM-DD`). */
    fun agenda(
        from: String,
        to: String,
    ): Result<List<AgendaDay>> = Result.failure(UnsupportedOperationException("La agenda no está disponible."))
}

class OrderRepositoryException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
