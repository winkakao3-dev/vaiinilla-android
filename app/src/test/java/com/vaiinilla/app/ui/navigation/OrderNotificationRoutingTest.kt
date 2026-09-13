package com.vaiinilla.app.ui.navigation

import com.vaiinilla.app.core.notifications.OrderNotificationTarget
import com.vaiinilla.app.domain.model.OperationalRole
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderNotificationRoutingTest {
    private data class Case(
        val name: String,
        val target: OrderNotificationTarget,
        val hasStudentSession: Boolean = false,
        val studentAuthLoading: Boolean = false,
        val operationalRole: OperationalRole? = null,
        val accessLoading: Boolean = false,
        val hasStaffModes: Boolean = false,
        val expected: OrderNotificationAction,
    )

    private val cases =
        listOf(
            Case(
                name = "client notification with student session opens client order",
                target = OrderNotificationTarget.CLIENT,
                hasStudentSession = true,
                expected = OrderNotificationAction.OPEN_CLIENT_ORDER,
            ),
            Case(
                name = "client notification while student auth loads waits",
                target = OrderNotificationTarget.CLIENT,
                studentAuthLoading = true,
                expected = OrderNotificationAction.WAIT,
            ),
            Case(
                name = "client notification without session is discarded",
                target = OrderNotificationTarget.CLIENT,
                expected = OrderNotificationAction.DISCARD,
            ),
            Case(
                name = "authenticated cashier tapping client notification switches to client context",
                target = OrderNotificationTarget.CLIENT,
                hasStudentSession = true,
                operationalRole = OperationalRole.CASHIER,
                expected = OrderNotificationAction.SWITCH_TO_CLIENT_ORDER,
            ),
            Case(
                name = "client notification during in-flight staff context switch waits",
                target = OrderNotificationTarget.CLIENT,
                hasStudentSession = true,
                operationalRole = OperationalRole.CASHIER,
                accessLoading = true,
                expected = OrderNotificationAction.WAIT,
            ),
            Case(
                name = "authenticated cashier tapping staff notification selects staff order",
                target = OrderNotificationTarget.STAFF,
                hasStudentSession = true,
                operationalRole = OperationalRole.CASHIER,
                expected = OrderNotificationAction.SELECT_STAFF_ORDER,
            ),
            Case(
                name = "seed staff app without student session selects staff order",
                target = OrderNotificationTarget.STAFF,
                operationalRole = OperationalRole.KITCHEN,
                expected = OrderNotificationAction.SELECT_STAFF_ORDER,
            ),
            Case(
                name = "active staff role wins even while access is still loading",
                target = OrderNotificationTarget.STAFF,
                hasStudentSession = true,
                operationalRole = OperationalRole.WAITER,
                accessLoading = true,
                expected = OrderNotificationAction.SELECT_STAFF_ORDER,
            ),
            Case(
                name = "staff notification waits while access modes load",
                target = OrderNotificationTarget.STAFF,
                hasStudentSession = true,
                accessLoading = true,
                expected = OrderNotificationAction.WAIT,
            ),
            Case(
                name = "staff notification with staff modes opens mode picker",
                target = OrderNotificationTarget.STAFF,
                hasStudentSession = true,
                hasStaffModes = true,
                expected = OrderNotificationAction.OPEN_STAFF_MODES,
            ),
            Case(
                name = "staff notification with client role and staff modes opens mode picker",
                target = OrderNotificationTarget.STAFF,
                hasStudentSession = true,
                operationalRole = OperationalRole.CLIENT,
                hasStaffModes = true,
                expected = OrderNotificationAction.OPEN_STAFF_MODES,
            ),
            Case(
                name = "staff notification without staff modes is discarded",
                target = OrderNotificationTarget.STAFF,
                hasStudentSession = true,
                expected = OrderNotificationAction.DISCARD,
            ),
            Case(
                name = "staff notification without session or role waits",
                target = OrderNotificationTarget.STAFF,
                expected = OrderNotificationAction.WAIT,
            ),
        )

    @Test
    fun `resolveOrderNotificationAction follows the settled policy`() {
        cases.forEach { case ->
            assertEquals(
                case.name,
                case.expected,
                resolveOrderNotificationAction(
                    target = case.target,
                    hasStudentSession = case.hasStudentSession,
                    studentAuthLoading = case.studentAuthLoading,
                    operationalRole = case.operationalRole,
                    accessLoading = case.accessLoading,
                    hasStaffModes = case.hasStaffModes,
                ),
            )
        }
    }
}
