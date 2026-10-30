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
                hasSavedVenue = false,
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
                hasSavedVenue = false,
            ),
        )
    }

    @Test
    fun `table link takes a staff session to discovery instead of the modes`() {
        assertEquals(
            LaunchDestination.Discovery,
            resolveLaunchDestination(
                pendingEstablishmentSlug = null,
                session = session,
                hasStaffModes = true,
                hasSavedVenue = false,
                pendingSpaceToken = "qr-mesa-4",
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
                hasSavedVenue = false,
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
                hasSavedVenue = false,
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

    @Test
    fun `staff whose modes could not load go to the modes screen and not to the client menu`() {
        assertEquals(
            LaunchDestination.StaffModes,
            resolveLaunchDestination(
                pendingEstablishmentSlug = null,
                session = session,
                hasStaffModes = false,
                hasSavedVenue = false,
                modesLoaded = false,
            ),
        )
    }

    @Test
    fun `a saved venue still opens its catalog when modes could not load`() {
        assertEquals(
            LaunchDestination.Catalog,
            resolveLaunchDestination(
                pendingEstablishmentSlug = null,
                session = session,
                hasStaffModes = false,
                hasSavedVenue = true,
                modesLoaded = false,
            ),
        )
    }
}
