package com.vaiinilla.app.data.order

import com.vaiinilla.app.domain.model.CashCollectionResult
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.CreatedOrder
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.OrderSummary
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.StripePaymentSession
import com.vaiinilla.app.domain.repository.OrderRepository
import com.vaiinilla.app.domain.usecase.RecoverPickupQrUseCase
import com.vaiinilla.app.domain.usecase.needsPickupQrRecovery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PickupQrRecoveryTest {
    private val json = OrderContractJson()

    private fun order(
        state: OrderState,
        pickupToken: String? = null,
    ) = OrderDetail(
        summary =
            OrderSummary(
                id = "order-1",
                folio = 7,
                operationalDate = "2026-10-11",
                state = state,
                paymentMethod = PaymentMethod.CASH,
                destination = OrderDestination.IN_SPACE,
                space = null,
                subtotal = "0.00",
                combinedSavings = "0.00",
                cashbackAwarded = "0.00",
                total = "0.00",
                version = 1,
                createdAt = "2026-10-11T08:00:00Z",
                updatedAt = "2026-10-11T08:00:00Z",
            ),
        user = null,
        kitchenNotes = "",
        items = emptyList(),
        pickupToken = pickupToken,
    )

    @Test
    fun `parsea el token de recogida y quita los espacios`() {
        assertEquals("qr-123", json.parsePickupQr("""{"data":{"qr_token":"  qr-123 "}}"""))
    }

    @Test
    fun `sin token utilizable no hay QR`() {
        assertNull(json.parsePickupQr("""{"data":{"qr_token":"   "}}"""))
        assertNull(json.parsePickupQr("""{"data":{}}"""))
        assertNull(json.parsePickupQr("""{"data":null}"""))
    }

    @Test
    fun `un envelope con error no se trata como QR`() {
        assertThrows(IllegalArgumentException::class.java) {
            json.parsePickupQr("""{"data":null,"error":{"code":"QR_NOT_READY"}}""")
        }
    }

    @Test
    fun `solo recupera el QR de un pedido pagado sin token`() {
        assertTrue(order(OrderState.PAID).needsPickupQrRecovery())
        assertTrue(order(OrderState.READY).needsPickupQrRecovery())
        assertTrue(order(OrderState.DELIVERED).needsPickupQrRecovery())
        assertFalse(order(OrderState.PENDING_PAYMENT).needsPickupQrRecovery())
        assertFalse(order(OrderState.CANCELED).needsPickupQrRecovery())
        assertFalse(order(OrderState.READY, pickupToken = "ya-lo-tengo").needsPickupQrRecovery())
    }

    @Test
    fun `el caso de uso pega el token recuperado al pedido`() {
        val repo = FakePickupQrRepository(result = Result.success("qr-9"))
        val recovered = RecoverPickupQrUseCase(repo)(order(OrderState.READY))
        assertEquals("qr-9", recovered.pickupToken)
        assertEquals(listOf("order-1"), repo.requestedIds)
    }

    @Test
    fun `si el servidor no da el QR el pedido sigue igual, y uno con token no llama al servidor`() {
        val repo = FakePickupQrRepository(result = Result.failure(IllegalStateException("409")))
        val original = order(OrderState.READY)
        assertEquals(original, RecoverPickupQrUseCase(repo)(original))
        val paidWithToken = order(OrderState.READY, pickupToken = "ya")
        assertEquals(paidWithToken, RecoverPickupQrUseCase(repo)(paidWithToken))
        assertEquals(listOf("order-1"), repo.requestedIds)
    }
}

private class FakePickupQrRepository(
    private val result: Result<String>,
) : OrderRepository {
    val requestedIds = mutableListOf<String>()

    override fun pickupQr(orderId: String): Result<String> {
        requestedIds += orderId
        return result
    }

    override fun createOrder(
        request: CreateOrderRequest,
        idempotencyKey: String,
    ): Result<CreatedOrder> = unsupported()

    override fun getOrder(orderId: String): Result<OrderDetail> = unsupported()

    override fun listOrders(
        role: OperationalRole,
        updatedSince: String?,
    ): Result<List<OrderDetail>> = unsupported()

    override fun retryStripePayment(
        orderId: String,
        idempotencyKey: String,
    ): Result<StripePaymentSession> = unsupported()

    override fun collectCash(
        orderId: String,
        amountReceived: String,
        expectedVersion: Int,
        idempotencyKey: String,
        tip: String?,
    ): Result<CashCollectionResult> = unsupported()

    override fun transition(
        orderId: String,
        targetState: OrderState,
        expectedVersion: Int,
        idempotencyKey: String,
        pickupToken: String?,
    ): Result<OrderDetail> = unsupported()

    private fun <T> unsupported(): Result<T> = Result.failure(UnsupportedOperationException("no usado en este test"))
}
