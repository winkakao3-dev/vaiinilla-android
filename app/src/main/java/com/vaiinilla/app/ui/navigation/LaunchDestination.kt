package com.vaiinilla.app.ui.navigation

import com.vaiinilla.app.core.notifications.OrderNotificationTarget
import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.model.OperationalRole

sealed class LaunchDestination {
    data object Login : LaunchDestination()

    data object Discovery : LaunchDestination()

    data object Catalog : LaunchDestination()

    data object StaffModes : LaunchDestination()
}

fun resolveLaunchDestination(
    pendingEstablishmentSlug: String?,
    session: StudentAuthSession?,
    hasStaffModes: Boolean,
    hasSavedVenue: Boolean,
): LaunchDestination {
    if (!pendingEstablishmentSlug.isNullOrBlank()) return LaunchDestination.Discovery
    if (session == null && !hasSavedVenue) return LaunchDestination.Login
    if (hasStaffModes) return LaunchDestination.StaffModes
    if (!hasSavedVenue) return LaunchDestination.Discovery
    return LaunchDestination.Catalog
}

fun LaunchDestination.toRoute(): String =
    when (this) {
        LaunchDestination.Login -> Routes.authLandingRoute(Routes.DISCOVERY)
        LaunchDestination.Discovery -> Routes.DISCOVERY
        LaunchDestination.Catalog -> Routes.CATALOG
        LaunchDestination.StaffModes -> Routes.STAFF_MODES
    }

fun hasStaffLaunchModes(roles: Collection<OperationalRole>): Boolean =
    roles.any { role ->
        when (role) {
            OperationalRole.CASHIER,
            OperationalRole.KITCHEN,
            OperationalRole.WAITER,
            -> true
            OperationalRole.CLIENT -> false
        }
    }

internal enum class OrderNotificationAction {
    OPEN_CLIENT_ORDER,
    SWITCH_TO_CLIENT_ORDER,
    SELECT_STAFF_ORDER,
    OPEN_STAFF_MODES,
    WAIT,
    DISCARD,
}

internal fun resolveOrderNotificationAction(
    target: OrderNotificationTarget,
    hasStudentSession: Boolean,
    studentAuthLoading: Boolean,
    operationalRole: OperationalRole?,
    accessLoading: Boolean,
    hasStaffModes: Boolean,
): OrderNotificationAction =
    when (target) {
        OrderNotificationTarget.CLIENT ->
            when {
                !hasStudentSession ->
                    if (studentAuthLoading) {
                        OrderNotificationAction.WAIT
                    } else {
                        OrderNotificationAction.DISCARD
                    }
                operationalRole != null && operationalRole != OperationalRole.CLIENT ->
                    if (accessLoading) {
                        OrderNotificationAction.WAIT
                    } else {
                        OrderNotificationAction.SWITCH_TO_CLIENT_ORDER
                    }
                else -> OrderNotificationAction.OPEN_CLIENT_ORDER
            }
        OrderNotificationTarget.STAFF ->
            when {
                operationalRole != null && operationalRole != OperationalRole.CLIENT ->
                    OrderNotificationAction.SELECT_STAFF_ORDER
                hasStudentSession && accessLoading -> OrderNotificationAction.WAIT
                hasStudentSession && hasStaffModes -> OrderNotificationAction.OPEN_STAFF_MODES
                hasStudentSession -> OrderNotificationAction.DISCARD
                else -> OrderNotificationAction.WAIT
            }
    }
