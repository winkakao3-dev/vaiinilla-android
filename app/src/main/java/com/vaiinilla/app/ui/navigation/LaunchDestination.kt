package com.vaiinilla.app.ui.navigation

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
