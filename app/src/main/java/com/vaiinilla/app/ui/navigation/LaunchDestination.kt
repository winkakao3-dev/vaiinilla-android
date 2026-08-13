package com.vaiinilla.app.ui.navigation

import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.model.OperationalRole

sealed class LaunchDestination {
    data object Login : LaunchDestination()

    data object Discovery : LaunchDestination()

    data object StaffModes : LaunchDestination()
}

fun resolveLaunchDestination(
    pendingEstablishmentSlug: String?,
    session: StudentAuthSession?,
    hasStaffModes: Boolean,
): LaunchDestination {
    if (!pendingEstablishmentSlug.isNullOrBlank()) return LaunchDestination.Discovery
    if (session == null) return LaunchDestination.Login
    if (hasStaffModes) return LaunchDestination.StaffModes
    return LaunchDestination.Discovery
}

fun LaunchDestination.toRoute(): String =
    when (this) {
        LaunchDestination.Login -> Routes.authLoginRoute(Routes.DISCOVERY)
        LaunchDestination.Discovery -> Routes.DISCOVERY
        LaunchDestination.StaffModes -> Routes.VAI27_MODES
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
