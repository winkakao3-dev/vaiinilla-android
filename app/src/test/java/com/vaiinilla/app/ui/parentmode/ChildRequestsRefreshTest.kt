package com.vaiinilla.app.ui.parentmode

import com.vaiinilla.app.domain.guardianship.StudentRequest
import com.vaiinilla.app.domain.guardianship.StudentRequestKind
import com.vaiinilla.app.domain.guardianship.StudentRequestStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/** CARTERA-30: la ficha ve resolverse la devolución sin salir y entrar, y entonces refresca el saldo. */
class ChildRequestsRefreshTest {
    private fun request(status: StudentRequestStatus) =
        StudentRequest(
            id = "s-1",
            kind = StudentRequestKind.entries.first(),
            status = status,
            refundMethod = null,
            note = null,
            balanceAtRequest = BigDecimal("40"),
            refunded = null,
            rejectionReason = null,
            createdAt = null,
            resolvedAt = null,
        )

    private class SteppedRequests(
        var current: List<StudentRequest>,
    ) : FakeGuardianship(listOf(ParentFixtures.ana)) {
        override fun studentRequests(dependentId: String): Result<List<StudentRequest>> = Result.success(current)
    }

    @Test
    fun `al resolverse la solicitud pendiente se refresca la cartera del hijo`() =
        runBlocking {
            val guardianship = SteppedRequests(listOf(request(StudentRequestStatus.PENDING)))
            val state =
                ChildAccountState(ParentFixtures.ana, ParentFixtures.services(guardianship), Dispatchers.Unconfined)

            state.refreshRequests()
            assertTrue(state.hasPendingRequest)
            assertNull(state.wallet)

            guardianship.current = listOf(request(StudentRequestStatus.RESOLVED))
            state.refreshRequests()
            assertFalse(state.hasPendingRequest)
            assertEquals(StudentRequestStatus.RESOLVED, state.requests.single().status)
            assertNotNull(state.wallet)
        }
}
