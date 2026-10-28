package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.repository.PendingRefund
import com.vaiinilla.app.domain.repository.PendingRefundRepository
import com.vaiinilla.app.domain.repository.RefundMethod
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Caja confirma las devoluciones pendientes (G08, H03). */
class PendingRefundsViewModelTest {
    private fun refund(
        id: String,
        method: String = "efectivo",
    ) = PendingRefund(id, "cancelacion", null, "p-$id", method, "50.00", "pendiente", "2026-10-23T06:15:19.359Z")

    private class FakeRepository(
        var list: List<PendingRefund>,
        var failWith: Throwable? = null,
    ) : PendingRefundRepository {
        val confirmations = mutableListOf<Triple<String, RefundMethod, String>>()
        val notes = mutableListOf<String?>()

        override fun pending(spaceId: Int?): Result<List<PendingRefund>> = Result.success(list)

        override fun confirm(
            refundId: String,
            method: RefundMethod,
            note: String?,
            idempotencyKey: String,
        ): Result<PendingRefund> {
            confirmations += Triple(refundId, method, idempotencyKey)
            notes += note
            failWith?.let { return Result.failure(it) }
            val done = list.first { it.id == refundId }.copy(state = "devuelta")
            list = list.filterNot { it.id == refundId }
            return Result.success(done)
        }
    }

    @Test
    fun `confirmar la saca de la lista y avisa`() =
        runTest {
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val repo = FakeRepository(listOf(refund("a"), refund("b", "terminal")))
            val vm = PendingRefundsViewModel(repo, dispatcher, TestScope(dispatcher))
            vm.refresh()
            assertEquals(2, vm.uiState.value.refunds.size)

            vm.confirm(
                vm.uiState.value.refunds
                    .first(),
                RefundMethod.CASH,
            )

            assertEquals(
                listOf("b"),
                vm.uiState.value.refunds
                    .map { it.id },
            )
            assertEquals("Devolución de $50 confirmada.", vm.uiState.value.notice)
        }

    @Test
    fun `otro manda el motivo al servidor`() =
        runTest {
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val repo = FakeRepository(listOf(refund("a")))
            val vm = PendingRefundsViewModel(repo, dispatcher, TestScope(dispatcher))
            vm.refresh()

            vm.confirm(refund("a"), RefundMethod.OTHER, "Transferencia")

            assertEquals(listOf<String?>("Transferencia"), repo.notes)
        }

    @Test
    fun `reintentar tras un error usa la misma llave`() =
        runTest {
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val repo = FakeRepository(listOf(refund("a")), failWith = IllegalStateException("No hay una caja abierta."))
            val vm = PendingRefundsViewModel(repo, dispatcher, TestScope(dispatcher))
            vm.refresh()

            vm.confirm(
                vm.uiState.value.refunds
                    .first(),
                RefundMethod.CASH,
            )
            assertTrue(vm.uiState.value.noticeIsError)
            assertEquals("No hay una caja abierta.", vm.uiState.value.notice)
            repo.failWith = null
            vm.confirm(
                vm.uiState.value.refunds
                    .first(),
                RefundMethod.CASH,
            )

            assertEquals(repo.confirmations[0].third, repo.confirmations[1].third)
            assertTrue(
                vm.uiState.value.refunds
                    .isEmpty(),
            )
        }

    @Test
    fun `si ya se habia confirmado sale de la lista`() =
        runTest {
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            val repo =
                FakeRepository(
                    listOf(refund("a")),
                    failWith = ApiClientException("REFUND_ALREADY_DONE", "Esta devolución ya se confirmó.", 409),
                )
            val vm = PendingRefundsViewModel(repo, dispatcher, TestScope(dispatcher))
            vm.refresh()
            repo.list = emptyList()

            vm.confirm(refund("a"), RefundMethod.CASH)

            assertTrue(
                vm.uiState.value.refunds
                    .isEmpty(),
            )
        }
}
