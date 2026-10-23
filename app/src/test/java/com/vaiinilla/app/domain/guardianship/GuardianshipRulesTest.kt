package com.vaiinilla.app.domain.guardianship

import com.vaiinilla.app.core.notifications.GuardianPushNotifier
import com.vaiinilla.app.domain.school.SchoolAllergen
import com.vaiinilla.app.ui.parentmode.ChildControlsRules
import com.vaiinilla.app.ui.parentmode.ChildReloadModel
import com.vaiinilla.app.ui.parentmode.dailyLimitLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class GuardianshipRulesTest {
    // 2026-10-23 es viernes.
    private val calendar =
        MenuCalendar(
            schedule =
                OrderSchedule(
                    today = "2026-10-22",
                    daysAhead = 5,
                    orderCutoff = "09:00",
                    cancelCutoff = "08:00",
                    slots = emptyList(),
                ),
            days = listOf(MenuDay("2026-10-24", listOf(7)), MenuDay("2026-10-26", listOf(3, 4))),
            closedDays = listOf(NoServiceDay("2026-10-23", "Junta de maestros")),
        )

    @Test
    fun plannerDaysSkipWeekendWithoutMenuAndTodayAfterCutoff() {
        assertEquals(
            listOf("2026-10-22", "2026-10-23", "2026-10-24", "2026-10-26", "2026-10-27"),
            WeekPlannerRules.dates(calendar, nowMinutes = 8 * 60),
        )
        // Pasadas las 9:00 ya no se aparta para hoy; el domingo sin menú no sale.
        assertEquals("2026-10-23", WeekPlannerRules.dates(calendar, nowMinutes = 9 * 60).first())
        // El primer día que se puede elegir se salta el cerrado.
        assertEquals("2026-10-24", WeekPlannerRules.firstSelectable(listOf("2026-10-23", "2026-10-24"), calendar))
    }

    @Test
    fun plannerProductsFollowTheDayMenuAndClosedDaysOfferNothing() {
        val available = listOf(1, 3)
        val all = listOf(1, 3, 4, 7)
        assertEquals(emptyList<Int>(), WeekPlannerRules.productIds(calendar, "2026-10-23", available, all))
        assertEquals(listOf(3, 4), WeekPlannerRules.productIds(calendar, "2026-10-26", available, all))
        assertEquals(available, WeekPlannerRules.productIds(calendar, "2026-10-27", available, all))
        assertEquals("Mañana", SchoolCalendar.longLabel("2026-10-23", "2026-10-22"))
        assertEquals("Lunes 26", SchoolCalendar.longLabel("2026-10-26", "2026-10-22"))
        assertTrue(ScheduledOrderRules.canCancel("listo"))
        assertFalse(ScheduledOrderRules.canCancel("preparando"))
    }

    @Test
    fun weekPlanKeepsOneChoicePerProductAndDayAndSumsExtras() {
        var plan = WeekPlan().set(1, 7, "2026-10-24", optionIds = listOf(12, 11))
        plan = plan.set(2, 7, "2026-10-24")
        plan = plan.set(1, 3, "2026-10-26").withSlot("2026-10-26", "13:00-14:00")
        assertEquals(listOf(11, 12), plan.optionIds(7, "2026-10-24"))
        val prices = mapOf(7 to BigDecimal("30.00"), 3 to BigDecimal("15.50"))
        val extras = mapOf(11 to BigDecimal("5.00"), 12 to BigDecimal("0.00"))
        // (30 + 5) × 2 + 15.50
        assertEquals(BigDecimal("85.50"), plan.total(prices, extras).setScale(2))
        val orders = plan.orders
        assertEquals(listOf("2026-10-24", "2026-10-26"), orders.map { it.date })
        assertEquals("13:00-14:00", orders[1].slot)
        assertEquals(PlannedItem(7, 2, listOf(11, 12)), orders[0].items.single())
        plan = plan.set(0, 7, "2026-10-24")
        assertEquals(listOf("2026-10-26"), plan.plannedDates)
        assertEquals(WeekPlan.MAX_PER_PRODUCT, WeekPlan().set(99, 1, "d").quantity(1, "d"))
    }

    @Test
    fun controlsSheetValidatesAmountsAndKeepsProductBlocks() {
        assertNull(ChildControlsRules.amountProblem(""))
        assertNull(ChildControlsRules.amountProblem("60.5"))
        assertEquals("Escribe un monto como 60 o 60.50.", ChildControlsRules.amountProblem("sesenta"))
        val edited =
            ChildControlsRules.edited(
                current = DependentControls(blockedProductIds = setOf(9), dailyLimit = BigDecimal("80.00")),
                dailyLimit = "",
                lowBalance = "30",
                notes = "Sin picante",
                allergens = setOf(SchoolAllergen.HUEVO),
                blockedCategories = setOf(2),
            )
        assertNull(edited.dailyLimit)
        assertEquals(BigDecimal("30.00"), edited.lowBalanceAlert)
        assertEquals(setOf(9), edited.blockedProductIds)
        assertEquals("60.5", TypedAmount.editable(BigDecimal("60.50")))
    }

    @Test
    fun codesAreNormalizedWithoutAmbiguousLetters() {
        assertEquals("ABC234", GuardianLinkCodeRules.normalized(" abc-234 "))
        assertNull(GuardianLinkCodeRules.normalized("ABC10O"))
        assertEquals("ABCD2345", GuardianLinkCodeRules.normalizedAccountCode("abcd 2345"))
        assertEquals("SANDBX", SchoolJoinCodeRules.sanitized("sand-bx!"))
        assertTrue(SchoolJoinCodeRules.isComplete("SANDBX"))
        assertFalse(SchoolJoinCodeRules.isComplete("1234"))
        assertTrue(Dependent.isValidPin("0420"))
        assertFalse(Dependent.isValidPin("42a0"))
    }

    @Test
    fun studentSeesWhatIsLeftToday() {
        val controls =
            MyGuardianControls(
                tutorNames = listOf("Ana"),
                dailyLimit = BigDecimal("60.00"),
                availableToday = BigDecimal("40.00"),
                blockedProductIds = emptySet(),
                allergenProductIds = emptySet(),
                allergens = emptyList(),
            )
        assertEquals("Te quedan \$40 de tu límite de hoy", dailyLimitLine(controls, BigDecimal("35")))
        assertEquals("Tu límite de hoy no alcanza: te quedan \$40", dailyLimitLine(controls, BigDecimal("45")))
        assertNull(dailyLimitLine(controls.copy(availableToday = null), BigDecimal("45")))
        assertEquals("Te cuida Ana", controls.caretakerLine)
    }

    @Test
    fun reloadOnlyCreditsWhenTheServerSaysSo() {
        val statuses = ArrayDeque(listOf(CardReloadStatus.AWAITING_PAYMENT, CardReloadStatus.CREDITED))
        val account =
            object : com.vaiinilla.app.domain.guardianship.DependentAccountRepository {
                override fun wallet() = error("no")

                override fun startCardReload(
                    amount: BigDecimal,
                    idempotencyKey: String,
                ) = Result.success(
                    CardReloadCheckout(
                        CardReload("r1", amount, 10350, CardReloadStatus.AWAITING_PAYMENT),
                        "cs",
                        "pk",
                        "acct",
                    ),
                )

                override fun cardReload(id: String) =
                    Result.success(CardReload(id, BigDecimal("100.00"), 10350, statuses.removeFirst()))

                override fun menuCalendar() = error("no")

                override fun controls() = error("no")

                override fun reserve(
                    days: List<ScheduledDayOrder>,
                    idempotencyKey: String,
                ) = error("no")

                override fun orders() = error("no")

                override fun cancel(
                    order: ScheduledOrder,
                    idempotencyKey: String,
                ) = error("no")
            }
        val model =
            ChildReloadModel(
                account,
                CardReloadRange(BigDecimal("50.00"), BigDecimal("2000.00")),
                io = Dispatchers.Unconfined,
                pollIntervalMs = 0,
                maxPolls = 3,
            )
        model.edit("20")
        assertEquals("El mínimo es \$50.", model.problem)
        assertFalse(model.canPay)
        model.choose(100)
        assertTrue(model.canPay)
        runBlocking {
            model.start()
            assertEquals(ChildReloadModel.Phase.Paying, model.phase)
            model.onPaymentSheetCompleted()
        }
        assertEquals(ChildReloadModel.Phase.Credited(BigDecimal("100.00")), model.phase)
        assertNull(model.checkout)
    }

    @Test
    fun guardianPushTextsNameTheChild() {
        assertEquals(
            GuardianPushNotifier.Text("Tilin · Pedido #12", "Tilin su pedido está listo para recoger."),
            GuardianPushNotifier.orderStateText("Tilin", "12", "listo"),
        )
        assertEquals("Tu hijo · Pedido #12", GuardianPushNotifier.orderStateText(null, "12", "cobrado")!!.title)
        assertNull(GuardianPushNotifier.orderStateText("Tilin", "12", "por_cobrar"))
        assertEquals(
            "Le quedan \$20. Recarga para que no se quede sin comer.",
            GuardianPushNotifier.lowBalanceText("Tilin", "20.00").body,
        )
        assertEquals("Saldo: \$150.50.", GuardianPushNotifier.reloadCreditedText(null, "150.50").body)
    }
}
