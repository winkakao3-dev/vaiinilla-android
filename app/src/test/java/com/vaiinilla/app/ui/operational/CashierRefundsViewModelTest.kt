package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.core.network.MutationIdempotency
import com.vaiinilla.app.domain.model.PendingRefund
import com.vaiinilla.app.domain.model.RefundMethod
import com.vaiinilla.app.domain.model.RefundOrigin
import com.vaiinilla.app.domain.repository.RefundRepository
import com.vaiinilla.app.ui.screens.refundPaidWithLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CashierRefundsViewModelTest {
    private val refund =
        PendingRefund(
            id = "6f1c2b9e-1111-4a2b-9c3d-000000000001",
            origin = RefundOrigin.OVERPAID_ACCOUNT,
            spaceId = 7,
            originalMethod = "terminal",
            amount = "45.00",
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `suggests the method the money came in with and offers cash only when it was not cash`() {
        assertEquals(RefundMethod.CASH, suggestedRefundMethod("efectivo"))
        assertEquals(RefundMethod.CARD, suggestedRefundMethod("terminal"))
        assertEquals(RefundMethod.CARD, suggestedRefundMethod("efectivo+terminal"))
        assertFalse(offersCashRefund("efectivo"))
        assertTrue(offersCashRefund("terminal"))
        assertTrue(offersCashRefund("saldo"))
    }

    @Test
    fun `mixed payments read with 'y' like the web panel`() {
        assertEquals("efectivo y terminal", refundPaidWithLabel("efectivo+terminal"))
        assertEquals("saldo", refundPaidWithLabel("saldo"))
    }

    @Test
    fun `success shows the web notice and the refund stays confirmed once`() {
        val repository = RecordingRefundRepository()
        val viewModel = CashierRefundsViewModel(repository)

        viewModel.confirm(refund, RefundMethod.CARD)
        awaitUntil { viewModel.uiState.value.notice != null && viewModel.uiState.value.confirming == null }

        assertEquals("Devolución de \$45 confirmada.", viewModel.uiState.value.notice)
        assertNull(viewModel.uiState.value.confirming)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(MutationIdempotency.refundConfirmation(refund.id, "terminal")), repository.keys)
    }

    @Test
    fun `a failed confirmation keeps the same key for the retry`() {
        val repository = RecordingRefundRepository(failFirst = true)
        val viewModel = CashierRefundsViewModel(repository)

        viewModel.confirm(refund, RefundMethod.CASH)
        awaitUntil { viewModel.uiState.value.errorMessage != null && viewModel.uiState.value.confirming == null }
        viewModel.confirm(refund, RefundMethod.CASH)
        awaitUntil { viewModel.uiState.value.notice != null && viewModel.uiState.value.confirming == null }

        assertEquals(2, repository.keys.size)
        assertEquals(repository.keys[0], repository.keys[1])
    }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 3_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "La condición no se cumplió a tiempo." }
            Thread.sleep(5)
        }
    }

    private class RecordingRefundRepository(
        private val failFirst: Boolean = false,
    ) : RefundRepository {
        val keys = mutableListOf<String>()
        private var calls = 0

        override fun pending(): Result<List<PendingRefund>> = Result.success(emptyList())

        override fun confirm(
            refundId: String,
            method: RefundMethod,
            note: String?,
            idempotencyKey: String,
        ): Result<PendingRefund> {
            keys += idempotencyKey
            calls += 1
            if (failFirst && calls == 1) return Result.failure(java.io.IOException("sin red"))
            return Result.success(
                PendingRefund(refundId, RefundOrigin.OVERPAID_ACCOUNT, 7, "terminal", "45.00"),
            )
        }
    }
}
