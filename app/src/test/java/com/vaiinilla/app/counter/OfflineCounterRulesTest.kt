package com.vaiinilla.app.counter

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.data.counter.StudentCounterJson
import com.vaiinilla.app.domain.counter.OfflineCounterRules
import com.vaiinilla.app.domain.counter.OfflineCounterSnapshot
import com.vaiinilla.app.domain.counter.OfflineStudentPurchase
import com.vaiinilla.app.domain.counter.StudentCard
import com.vaiinilla.app.domain.counter.StudentPurchaseItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/** Las mismas reglas que `OfflineCounterTests` de iOS: Caja sin conexión no puede dejar un saldo en negativo. */
class OfflineCounterRulesTest {
    private fun money(value: String) = BigDecimal(value)

    private fun card(
        balance: String,
        spent: String = "0.00",
        limit: String? = null,
        pin: Boolean = false,
        name: String = "Tilín Ramírez",
    ) = StudentCard(
        userId = "u-$name",
        name = name,
        studentId = "A100",
        group = "3B",
        hasOwnAccount = false,
        balance = money(balance),
        spentToday = money(spent),
        dailyLimit = limit?.let(::money),
        allergens = listOf("cacahuate"),
        todayOrders = emptyList(),
        requiresPin = pin,
    )

    private fun purchase(
        student: StudentCard,
        total: String,
        at: Instant = Instant.now(),
        problem: String? = null,
    ) = OfflineStudentPurchase(
        id = UUID.randomUUID().toString(),
        studentId = student.userId,
        studentName = student.name,
        items = listOf(StudentPurchaseItem(productId = 4, quantity = 1)),
        total = money(total),
        createdAt = at,
        problem = problem,
    )

    private fun assertMoney(
        expected: String,
        actual: BigDecimal,
    ) = assertEquals(0, money(expected).compareTo(actual))

    @Test
    fun `lo que se fía es lo menor entre saldo, límite y tope, menos lo ya guardado`() {
        assertMoney("200.00", OfflineCounterRules.allowance(card("330.00"), BigDecimal.ZERO))
        assertMoney("80.00", OfflineCounterRules.allowance(card("80.00"), BigDecimal.ZERO))
        assertMoney(
            "60.00",
            OfflineCounterRules.allowance(card("330.00", spent = "40.00", limit = "100.00"), BigDecimal.ZERO),
        )
        assertMoney("50.00", OfflineCounterRules.allowance(card("330.00"), money("150.00")))
        assertMoney("0", OfflineCounterRules.allowance(card("30.00"), money("50.00")))
    }

    @Test
    fun `sin conexión no se vende con PIN ni por encima de lo que se fía`() {
        assertEquals(
            OfflineCounterRules.Refusal.NeedsPin,
            OfflineCounterRules.refusal(money("20.00"), card("330.00", pin = true), BigDecimal.ZERO),
        )
        val over = OfflineCounterRules.refusal(money("60.00"), card("330.00"), money("150.00"))
        assertTrue(over is OfflineCounterRules.Refusal.OverAllowance)
        assertMoney("50.00", (over as OfflineCounterRules.Refusal.OverAllowance).allowance)
        assertNull(OfflineCounterRules.refusal(money("50.00"), card("330.00"), money("150.00")))
    }

    @Test
    fun `solo se guardan las fallas de conexión`() {
        assertTrue(OfflineCounterRules.isConnectionProblem(UnknownHostException("api")))
        assertTrue(OfflineCounterRules.isConnectionProblem(SocketTimeoutException("timeout")))
        assertTrue(OfflineCounterRules.isConnectionProblem(ApiClientException("HTTP_502", "", 502)))
        assertTrue(OfflineCounterRules.isConnectionProblem(ApiClientException("RATE_LIMITED", "", 429)))
        assertFalse(OfflineCounterRules.isConnectionProblem(CancellationException("cancelada")))
        assertFalse(OfflineCounterRules.isConnectionProblem(ApiClientException("INSUFFICIENT_BALANCE", "", 422)))
        // Un 500 con sobre es la respuesta del servidor, no una caída.
        assertFalse(OfflineCounterRules.isConnectionProblem(ApiClientException("INTERNAL_ERROR", "", 500)))
    }

    @Test
    fun `otro día olvida las fichas pero conserva lo que falta mandar`() {
        val tilin = card("330.00")
        val snapshot =
            OfflineCounterSnapshot(day = "2026-10-21")
                .remember(tilin)
                .copy(purchases = listOf(purchase(tilin, "20.00"), purchase(tilin, "30.00", problem = "Sin saldo")))
        assertMoney("20.00", snapshot.queued(tilin.userId))
        assertEquals(listOf("Tilín Ramírez"), snapshot.cardsMatching("tilin").map { it.name })
        assertEquals(1, snapshot.cardsMatching("a1").size)
        assertTrue(snapshot.cardsMatching("t").isEmpty())

        val tomorrow = snapshot.startingDay("2026-10-22")
        assertTrue(tomorrow.cards.isEmpty())
        assertEquals(2, tomorrow.purchases.size)
        assertEquals(snapshot, snapshot.startingDay("2026-10-21"))
    }

    @Test
    fun `lo guardado se lee igual y lo viejo ya no se reenvía`() {
        val tilin = card("330.00")
        val snapshot =
            OfflineCounterSnapshot(day = "2026-10-21")
                .remember(tilin)
                .copy(purchases = listOf(purchase(tilin, "20.00", at = Instant.ofEpochSecond(1_800_000_000))))
        val json = StudentCounterJson()
        val decoded = json.decodeSnapshot(json.encodeSnapshot(snapshot))
        assertEquals(snapshot, decoded)
        assertEquals(
            listOf("cacahuate"),
            decoded.cards
                .first()
                .card.allergens,
        )

        val sale = decoded.purchases[0]
        assertFalse(OfflineCounterRules.isTooOld(sale, sale.createdAt + Duration.ofHours(11)))
        assertTrue(OfflineCounterRules.isTooOld(sale, sale.createdAt + Duration.ofHours(13)))
    }
}
