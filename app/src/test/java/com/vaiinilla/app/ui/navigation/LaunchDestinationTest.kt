package com.vaiinilla.app.ui.navigation

import com.vaiinilla.app.domain.auth.student.StudentAuthSession
import com.vaiinilla.app.domain.model.OperationalRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchDestinationTest {
    private val session =
        StudentAuthSession(
            uid = "u1",
            email = "dani@utch.mx",
            displayName = "Dani",
            emailVerified = true,
        )

    @Test
    fun `slug opens discovery even without a session`() {
        assertEquals(
            LaunchDestination.Discovery,
            resolveLaunchDestination(
                pendingEstablishmentSlug = "ca",
                session = null,
                hasStaffModes = false,
            ),
        )
    }

    @Test
    fun `no session goes to login`() {
        assertEquals(
            LaunchDestination.Login,
            resolveLaunchDestination(
                pendingEstablishmentSlug = null,
                session = null,
                hasStaffModes = true,
            ),
        )
    }

    @Test
    fun `staff session goes to modes`() {
        assertEquals(
            LaunchDestination.StaffModes,
            resolveLaunchDestination(
                pendingEstablishmentSlug = " ",
                session = session,
                hasStaffModes = true,
            ),
        )
    }

    @Test
    fun `student session goes to discovery`() {
        assertEquals(
            LaunchDestination.Discovery,
            resolveLaunchDestination(
                pendingEstablishmentSlug = null,
                session = session,
                hasStaffModes = false,
            ),
        )
    }

    @Test
    fun `only caja cocina mesero count as staff launch modes`() {
        assertFalse(hasStaffLaunchModes(listOf(OperationalRole.CLIENT)))
        assertTrue(hasStaffLaunchModes(listOf(OperationalRole.CLIENT, OperationalRole.CASHIER)))
        assertTrue(hasStaffLaunchModes(listOf(OperationalRole.KITCHEN)))
        assertTrue(hasStaffLaunchModes(listOf(OperationalRole.WAITER)))
    }
}
