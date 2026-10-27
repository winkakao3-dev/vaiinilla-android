package com.vaiinilla.app.ui.sharedtable

import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.SharedTableAmounts
import com.vaiinilla.app.domain.repository.SharedTableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/** C08: la mesa de un negocio no pasa a otro; con ella el menú de allá llamaba al mesero de acá (404). */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedTableVenueTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `entering another business forgets the table`() {
        val vm = SharedTableViewModel(FakeRepository())
        vm.bindVenue("usagi")
        runBlocking { vm.joinForWaiterCall("qr-mesa-5", "Kao") }
        assertEquals(5, spaceId(vm))

        vm.bindVenue("usagi")
        assertEquals(5, spaceId(vm))

        vm.bindVenue("sandbox")
        assertNull(vm.uiState.value.table)
    }

    @Test
    fun `si esto lo pago yo falla el error va junto al pedido y la mesa se vuelve a leer`() {
        val repository = FakeRepository(claimFails = true)
        val vm = SharedTableViewModel(repository)

        vm.claim(folio = 7, payIt = true)

        assertEquals(7, vm.uiState.value.errorFolio)
        assertEquals("Ese pedido ya está pagado", vm.uiState.value.error)
        assertEquals(1, repository.currentCalls)
    }

    private fun spaceId(vm: SharedTableViewModel): Int? {
        val table = vm.uiState.value.table ?: return null
        return table.spaceId
    }

    private class FakeRepository(
        private val claimFails: Boolean = false,
    ) : SharedTableRepository {
        var currentCalls = 0
        private val zero = SharedTableAmounts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        private val table =
            SharedTable(
                spaceId = 5,
                spaceName = "Mesa 5",
                spaceType = "mesa",
                myAlias = "Kao",
                accountOpen = true,
                participants = emptyList(),
                groups = emptyList(),
                totals = zero,
                myShare = zero,
            )

        override fun current(): Result<SharedTable?> {
            currentCalls++
            return Result.success(table)
        }

        override fun join(
            qrToken: String,
            alias: String,
        ): Result<SharedTable> = Result.success(table)

        override fun leave(): Result<Unit> = Result.success(Unit)

        override fun claim(
            folio: Int,
            payIt: Boolean,
        ): Result<SharedTable> =
            if (claimFails) {
                Result.failure(
                    IllegalStateException("Ese pedido ya está pagado"),
                )
            } else {
                Result.success(table)
            }
    }
}
