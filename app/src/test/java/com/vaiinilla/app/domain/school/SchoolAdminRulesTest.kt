package com.vaiinilla.app.domain.school

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SchoolAdminRulesTest {
    private val settings =
        SchoolSettings(
            joinCode = "SANDBX",
            joinRequiresCode = false,
            guardiansNeedApproval = true,
            reloadMinimum = "50",
            reloadMaximum = "2000",
            daysAhead = 5,
            orderCutoff = "09:00",
            cancelCutoff = "",
        )

    @Test
    fun `reload range compares as numbers, not as text`() {
        // Como texto, "50" > "2000"; como números no.
        assertNull(SchoolSettingsRules.problem(settings))
        assertNull(SchoolSettingsRules.problem(settings.copy(reloadMinimum = "50.00", reloadMaximum = "100")))
        assertEquals(
            "El mínimo de recarga no puede pasar del máximo.",
            SchoolSettingsRules.problem(settings.copy(reloadMinimum = "300", reloadMaximum = "200")),
        )
    }

    @Test
    fun `reload range is required and hours must look like 08_30`() {
        assertEquals(
            "Escribe el mínimo de recarga como 50 o 50.00.",
            SchoolSettingsRules.problem(settings.copy(reloadMinimum = "")),
        )
        assertEquals(
            "Escribe el máximo de recarga como 2000.",
            SchoolSettingsRules.problem(settings.copy(reloadMaximum = "abc")),
        )
        assertEquals("Escribe las horas como 08:30.", SchoolSettingsRules.problem(settings.copy(orderCutoff = "9")))
        assertEquals(
            "Escribe las horas como 08:30.",
            SchoolSettingsRules.problem(settings.copy(cancelCutoff = "25:00")),
        )
        assertEquals("Se aparta de 0 a 14 días antes.", SchoolSettingsRules.problem(settings.copy(daysAhead = 15)))
        assertEquals("50.00", SchoolSettingsRules.money("50"))
        assertEquals("1500.50", SchoolSettingsRules.money("1,500.5"))
        assertNull(SchoolSettingsRules.money("0"))
    }

    @Test
    fun `roster parser reads tabs, semicolons and commas and skips the header`() {
        val result =
            SchoolRosterParser.parse(
                """
                Matrícula	Nombre	Grupo
                a100	Tilin Ramírez	3B
                A101;Ana López;1°A
                A102, Leo,
                """.trimIndent(),
            )
        assertTrue(result.problems.isEmpty())
        assertEquals(
            listOf(
                RosterStudent("A100", "Tilin Ramírez", "3B"),
                RosterStudent("A101", "Ana López", "1°A"),
                RosterStudent("A102", "Leo", null),
            ),
            result.students,
        )
        assertTrue(result.canSave)
    }

    @Test
    fun `roster parser marks repeated or missing ids and blocks saving`() {
        val result = SchoolRosterParser.parse("A100, Tilin\n\n, Sin matrícula\na100, Otra vez")
        assertEquals(listOf(RosterStudent("A100", "Tilin", null)), result.students)
        assertEquals(
            listOf(SchoolRosterParser.Problem(3, "sin matrícula"), SchoolRosterParser.Problem(4, "matrícula repetida")),
            result.problems,
        )
        assertEquals("Línea 3: sin matrícula", result.problems.first().text)
        assertFalse(result.canSave)
        assertFalse(SchoolRosterParser.parse("   ").canSave)
    }

    @Test
    fun `roster groups line names up to four groups`() {
        val roster = listOf("3B", "1A", "2C", "1A", "4D", "5E").mapIndexed { i, g -> RosterStudent("A$i", null, g) }
        assertEquals("En 5 grupos: 1A, 2C, 3B, 4D…", SchoolRosterParser.groupsLine(roster))
        assertEquals("Grupo 3B", SchoolRosterParser.groupsLine(listOf(RosterStudent("A", null, "3B"))))
        assertNull(SchoolRosterParser.groupsLine(listOf(RosterStudent("A", null, null))))
    }

    @Test
    fun `student request resolution depends on today's balance`() {
        val refund =
            SchoolStudentRequest(
                id = "s1",
                kind = StudentRequestKind.REFUND,
                refundMethod = RefundMethod.TRANSFER,
                note = null,
                balanceAtRequest = "150.00",
                currentBalance = "0.00",
                guardianName = "Rosa",
                guardianEmail = null,
                studentName = "Tilin",
                studentId = null,
                group = null,
            )
        assertFalse(refund.canResolve)
        assertTrue(refund.copy(currentBalance = "150.00").canResolve)
        assertFalse(refund.copy(currentBalance = "150.0").balanceChanged)
        val removal = refund.copy(kind = StudentRequestKind.DATA_REMOVAL, refundMethod = null)
        assertTrue(removal.canResolve)
        assertFalse(removal.copy(currentBalance = "10.00").canResolve)
        assertNull(removal.studentLine)
    }

    @Test
    fun `day closure summary uses compact money`() {
        val money: (String) -> String = { "$" + it.removeSuffix(".00") }
        assertEquals(
            "Día cerrado. Se cancelaron 3 apartados y regresaron $100 a su saldo.",
            DayClosure(3, "100.00", 0).summary(money),
        )
        assertEquals(
            "Día cerrado. Se canceló 1 apartado y regresaron $25.50 a su saldo. 1 no se pudo cancelar: ciérralo otra vez.",
            DayClosure(1, "25.50", 1).summary(money),
        )
        assertEquals("Día cerrado. No había nada apartado.", DayClosure(0, "0.00", 0).summary(money))
    }

    @Test
    fun `school dates come from the server today`() {
        val today = LocalDate.of(2026, 10, 21)
        val dates = SchoolDates.dates(today, 14)
        assertEquals(15, dates.size)
        assertEquals(LocalDate.of(2026, 11, 4), dates.last())
        assertEquals("Mié", SchoolDates.weekdayShort(today))
        assertEquals("Hoy", SchoolDates.longLabel(today, today))
        assertEquals("Mañana", SchoolDates.longLabel(today.plusDays(1), today))
        assertEquals("Viernes 23", SchoolDates.longLabel(today.plusDays(2), today))
        assertTrue(SchoolDates.isWeekend(LocalDate.of(2026, 10, 24)))
        assertEquals(today, SchoolDates.parse("2026-10-21T00:00:00.000Z"))
        assertNull(SchoolDates.parse("ayer"))
    }
}
