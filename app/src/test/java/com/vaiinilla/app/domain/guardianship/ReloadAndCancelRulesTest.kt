package com.vaiinilla.app.domain.guardianship

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.ui.parentmode.ChildReloadModel
import com.vaiinilla.app.ui.parentmode.FakeAccount
import com.vaiinilla.app.ui.parentmode.ParentFixtures
import com.vaiinilla.app.ui.parentmode.keyAfterFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDateTime

class ReloadAndCancelRulesTest {
    private val range = CardReloadRange(BigDecimal("50.00"), BigDecimal("2000.00"))

    private fun account(
        charged: Long,
        statuses: List<CardReloadStatus>,
        started: () -> Unit = {},
    ): DependentAccountRepository {
        val queue = ArrayDeque(statuses)
        return object : DependentAccountRepository by FakeAccount(ParentFixtures.wallet) {
            override fun startCardReload(
                amount: BigDecimal,
                idempotencyKey: String,
            ): Result<CardReloadCheckout> {
                started()
                return Result.success(
                    CardReloadCheckout(
                        CardReload("r1", amount, charged, CardReloadStatus.AWAITING_PAYMENT),
                        "cs",
                        "pk",
                        "a",
                    ),
                )
            }

            override fun cardReload(id: String): Result<CardReload> =
                Result.success(
                    CardReload(id, BigDecimal("100.00"), charged, queue.removeFirstOrNull() ?: statuses.last()),
                )
        }
    }

    private fun model(
        account: DependentAccountRepository,
        resume: String? = null,
    ) = ChildReloadModel(
        account,
        range,
        Dispatchers.Unconfined,
        pollIntervalMs = 0,
        maxPolls = 2,
        resumeReloadId = resume,
    )

    @Test
    fun `a reload the bank never confirmed blocks paying again`() =
        runBlocking {
            var starts = 0
            val model = model(account(10000, listOf(CardReloadStatus.AWAITING_PAYMENT), started = { starts++ }))
            model.choose(100)
            model.start()
            model.onPaymentSheetCompleted()

            assertEquals(ChildReloadModel.Phase.InReview, model.phase)
            assertEquals("r1", model.unsettledReloadId)
            assertFalse(model.canPay)
            model.start()
            assertEquals(1, starts)
        }

    @Test
    fun `reopening the sheet resumes asking the bank about the unconfirmed reload`() =
        runBlocking {
            val model = model(account(10000, listOf(CardReloadStatus.CREDITED)), resume = "r1")
            assertEquals(ChildReloadModel.Phase.Confirming, model.phase)

            model.confirm()

            assertEquals(ChildReloadModel.Phase.Credited(BigDecimal("100.00")), model.phase)
            assertNull(model.unsettledReloadId)
        }

    @Test
    fun `a reload without a fee goes straight to payment and one with a fee shows the total first`() =
        runBlocking {
            val free = model(account(10000, listOf(CardReloadStatus.CREDITED)))
            free.choose(100)
            free.start()
            assertEquals(ChildReloadModel.Phase.Paying, free.phase)

            val withFee = model(account(10350, listOf(CardReloadStatus.CREDITED)))
            withFee.choose(100)
            withFee.start()
            assertEquals(ChildReloadModel.Phase.Review(10350), withFee.phase)
            withFee.edit("200")
            assertEquals(ChildReloadModel.Phase.Editing, withFee.phase)
            assertNull(withFee.checkout)
        }

    @Test
    fun `an amount with a dangling separator is still read`() {
        val model = model(account(10000, listOf(CardReloadStatus.CREDITED)))
        model.edit("100.")
        assertEquals(0, BigDecimal("100").compareTo(model.amount))
        assertTrue(model.canPay)
    }

    @Test
    fun `a scheduled order stops being cancellable at the cutoff of its delivery day`() {
        val before = LocalDateTime.of(2026, 10, 26, 7, 59)
        val after = LocalDateTime.of(2026, 10, 26, 8, 0)

        assertTrue(ScheduledOrderRules.canCancel("listo", "2026-10-26", "08:00", before))
        assertFalse(ScheduledOrderRules.canCancel("listo", "2026-10-26", "08:00", after))
        assertTrue(ScheduledOrderRules.canCancel("cobrado", "2026-10-27", "08:00", after))
        assertFalse(ScheduledOrderRules.canCancel("cobrado", "2026-10-26", null, after))
        assertFalse(ScheduledOrderRules.canCancel("preparando", "2026-10-30", "08:00", before))
        assertTrue(ScheduledOrderRules.canCancel("listo", "2026-10-26", "08:00", null))
    }

    @Test
    fun `balance exactly at the alert threshold is not low`() {
        val controls = DependentControls(lowBalanceAlert = BigDecimal("50.00"))
        val atThreshold = ParentFixtures.ana.copy(balance = BigDecimal("50.00"), controls = controls)
        val below = ParentFixtures.ana.copy(balance = BigDecimal("49.99"), controls = controls)

        assertFalse(atThreshold.isLowBalance)
        assertTrue(below.isLowBalance)
    }

    @Test
    fun `an idempotency key survives a network failure but not a definitive rejection`() {
        val key = "k1"

        assertEquals(key, keyAfterFailure(IOException("sin red"), key))
        assertEquals(key, keyAfterFailure(ApiClientException("X", "x", 503), key))
        assertEquals(key, keyAfterFailure(ApiClientException("X", "x", 408), key))
        assertNotEquals(key, keyAfterFailure(ApiClientException("X", "x", 409), key))
        assertNotEquals(key, keyAfterFailure(ApiClientException("X", "x", 422), key))
    }
}
