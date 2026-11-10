package com.vaiinilla.app.data.operational

import com.vaiinilla.app.data.tape.ServerTape
import com.vaiinilla.app.data.tape.TapeFloor
import com.vaiinilla.app.domain.admin.SpacePatch
import com.vaiinilla.app.domain.admin.SpaceType
import com.vaiinilla.app.domain.repository.AbonoMode
import com.vaiinilla.app.domain.repository.AccountPaymentMethod
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.SpaceAvailabilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

/** El repositorio real del mesero contra lo que el servidor de pruebas contestó de verdad. */
class RemoteWaiterRepositoryTapeTest {
    @get:Rule
    val tape = ServerTape()

    private val floor by lazy { TapeFloor(tape) }

    private fun money(amount: String?) = BigDecimal(amount)

    @Test
    fun `la cuenta de una mesa suma sus pedidos, acepta un abono y se cobra el resto`() {
        floor.open()
        val product = floor.product()
        val table = floor.newSpace()
        assertEquals("Ana", floor.sit(table, "cliente", "Ana").myAlias)
        assertEquals(listOf("Ana"), floor.sharedTable("cliente2").othersAt(floor.qrToken(table)).getOrThrow())
        floor.sit(table, "cliente2", "Beto")
        val first = floor.order(product, 2, table.id, payAtEnd = true)
        val second = floor.order(product, 1, table.id, payAtEnd = true, account = "cliente2")
        assertTrue(first.summary.payAtEnd)
        val total = money(first.summary.total) + money(second.summary.total)
        assertEquals(money(TapeFloor.PRICE) * BigDecimal(3), total)

        val shared = floor.sharedTable("cliente").current().getOrThrow()!!
        assertEquals(table.id, shared.spaceId)
        assertEquals(setOf("Ana", "Beto"), shared.participants.map { it.alias }.toSet())
        assertEquals(total, shared.totals.total)
        assertEquals(money(first.summary.total), shared.myShare.total)
        assertTrue(shared.accountOpen)

        val waiter = floor.waiter("mesero")
        val boardTable =
            waiter
                .board()
                .getOrThrow()
                .tables
                .single { it.space.id == table.id }
        assertEquals(setOf(first.summary.id, second.summary.id), boardTable.orders.map { it.id }.toSet())
        assertTrue(boardTable.orders.all { it.payAtEnd && it.paymentPending })
        val occupied = waiter.availability().getOrThrow().single { it.space.id == table.id }
        assertEquals(SpaceAvailabilityState.OCUPADA, occupied.state)
        assertFalse(occupied.settled)

        val detail = waiter.spaceSession(table.id).getOrThrow()
        val account = detail.account!!
        assertEquals(total, money(account.total))
        assertEquals(total, money(account.pending))
        assertEquals(2, account.orders.size)
        val listed =
            floor
                .admin()
                .unpaidAccounts()
                .getOrThrow()
                .accounts
                .single { it.spaceId == table.id }
        assertEquals(total, money(listed.remaining))
        assertEquals(table.name, listed.spaceName)

        val cashier = floor.waiter("cajero")
        val abono =
            cashier
                .abonar(
                    table.id,
                    AccountPaymentMethod.CASH,
                    AbonoMode.AMOUNT,
                    amount = "50.00",
                    parts = null,
                    received = "100.00",
                    expectedRemaining = total.toPlainString(),
                    idempotencyKey = floor.key(),
                ).getOrThrow()
        assertEquals(money("50.00"), money(abono.amount))
        assertEquals(money("50.00"), money(abono.change))
        assertEquals(total - money("50.00"), money(abono.remaining))
        assertFalse(abono.settled)

        val rest = total - money("50.00")
        val collected =
            cashier
                .collectAccount(table.id, AccountPaymentMethod.CASH, "100.00", rest.toPlainString(), null, floor.key())
                .getOrThrow()
        assertEquals(2, collected.ordersCollected)
        assertEquals(money("100.00") - rest, money(collected.change))
        assertTrue(collected.settled)
        assertTrue(
            waiter
                .spaceSession(table.id)
                .getOrThrow()
                .account
                ?.settled ?: true,
        )
        assertTrue(
            floor
                .admin()
                .unpaidAccounts()
                .getOrThrow()
                .accounts
                .none { it.spaceId == table.id },
        )
        floor.sharedTable("cliente").leave().getOrThrow()
        floor.sharedTable("cliente2").leave().getOrThrow()
        assertNull(floor.sharedTable("cliente").current().getOrThrow())
    }

    @Test
    fun `el cliente llama al mesero, el mesero va y la atiende, y otra llamada se cancela`() {
        floor.open()
        val product = floor.product()
        val table = floor.newSpace()
        floor.sit(table, "cliente", "Ana")
        floor.sit(table, "cliente2", "Beto")
        val order = floor.order(product, 1, table.id, payAtEnd = true)
        val customer = floor.waiter("cliente")
        assertNull(customer.currentCall(table.id).getOrThrow())

        val call = customer.call(table.id, CallReason.CUENTA, order.summary.id, floor.key()).getOrThrow()
        assertEquals(CallStatus.PENDIENTE, call.status)
        assertEquals(CallReason.CUENTA, call.reason)
        assertEquals(table.id, call.space.id)
        assertEquals(call.id, customer.currentCall(table.id).getOrThrow()?.id)

        val waiter = floor.waiter("mesero")
        val seen =
            waiter
                .board()
                .getOrThrow()
                .tables
                .single { it.space.id == table.id }
                .call
        assertEquals(call.id, seen?.id)
        val onTheWay = waiter.transitionCall(seen!!, CallStatus.EN_CAMINO, floor.key()).getOrThrow()
        assertEquals(CallStatus.EN_CAMINO, onTheWay.status)
        assertNotNull(onTheWay.takenBy)
        assertEquals(
            CallStatus.ATENDIDA,
            waiter.transitionCall(onTheWay, CallStatus.ATENDIDA, floor.key()).getOrThrow().status,
        )
        assertNull(
            waiter
                .board()
                .getOrThrow()
                .tables
                .single { it.space.id == table.id }
                .call,
        )

        val other = floor.waiter("cliente2")
        val second = other.call(table.id, CallReason.UTENSILIOS, null, floor.key()).getOrThrow()
        assertEquals(CallStatus.CANCELADA, other.cancelCall(second, floor.key()).getOrThrow().status)
        assertNull(other.currentCall(table.id).getOrThrow())

        val rest =
            waiter
                .spaceSession(table.id)
                .getOrThrow()
                .account!!
                .pending
        floor
            .waiter(
                "cajero",
            ).collectAccount(table.id, AccountPaymentMethod.TERMINAL, null, rest, null, floor.key())
            .getOrThrow()
        floor.sharedTable("cliente").leave().getOrThrow()
        floor.sharedTable("cliente2").leave().getOrThrow()
    }

    @Test
    fun `una mesa se abre por tiempo, se alarga y se libera, y una cancha con precio se anuncia para rentar`() {
        floor.open()
        val table = floor.newSpace()
        val cashier = floor.waiter("cajero")
        assertEquals(
            SpaceAvailabilityState.LIBRE,
            cashier
                .spaceSession(table.id)
                .getOrThrow()
                .availability.state,
        )

        cashier.openSession(table.id, 60, floor.key()).getOrThrow()
        val open = cashier.spaceSession(table.id).getOrThrow()
        assertEquals(SpaceAvailabilityState.OCUPADA, open.availability.state)
        val session = open.session!!
        assertNotNull(session.endsAt)
        cashier.extendSession(table.id, 30, session.version, floor.key()).getOrThrow()
        val extended = cashier.spaceSession(table.id).getOrThrow().session!!
        assertTrue("alargar recorre el final", extended.endsAt!! > session.endsAt!!)
        cashier.releaseSpace(table.id, extended.version, floor.key()).getOrThrow()
        assertEquals(
            SpaceAvailabilityState.LIBRE,
            cashier
                .spaceSession(table.id)
                .getOrThrow()
                .availability.state,
        )

        val court =
            floor
                .spaces()
                .create(tape.value("cancha") { "Cancha ${table.name}" }, SpaceType.COURT, floor.key(), null, null, null)
                .getOrThrow()
        floor
            .spaces()
            .update(
                court.id,
                SpacePatch(hourlyPrice = "200.00", setsHourlyPrice = true),
                floor.key(),
            ).getOrThrow()
        val listed = cashier.availability().getOrThrow().single { it.space.id == court.id }
        assertEquals(money("200.00"), money(listed.pricePerHour))
        assertTrue(listed.rentable)
        val public = cashier.publicAvailability(tape.store("restaurante").slug).getOrThrow()
        assertEquals(SpaceAvailabilityState.LIBRE, public.single { it.space.id == court.id }.state)
        val rented = cashier.openSession(court.id, 60, floor.key())
        assertTrue("una cancha con precio no se abre por turno: se renta", rented.isFailure)
    }
}
