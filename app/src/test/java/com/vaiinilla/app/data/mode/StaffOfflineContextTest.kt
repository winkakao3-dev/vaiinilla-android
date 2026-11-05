package com.vaiinilla.app.data.mode

import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.RestrictedMode
import com.vaiinilla.app.domain.model.OperationalRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffOfflineContextTest {
    private val caja =
        StaffOfflineContext(
            role = OperationalRole.CASHIER,
            establishmentId = "e-1",
            establishmentName = "Boliche",
            membershipId = "m-1",
            restrictedMode = RestrictedMode.READ_ONLY,
        )

    @Test
    fun `solo coincide con el mismo rol negocio y membresia`() {
        val mode = AuthorizedMode(OperationalRole.CASHIER, "e-1", "Boliche", "m-1")
        assertTrue(caja.matches(mode))
        assertFalse(caja.matches(mode.copy(role = OperationalRole.WAITER)))
        assertFalse(caja.matches(mode.copy(membershipId = "m-2")))
    }

    @Test
    fun `el contexto reabierto sin internet usa el token guardado y vence ya`() {
        val context = caja.toModeContext("token-guardado")
        assertEquals("token-guardado", context.accessToken)
        assertEquals(0, context.expiresIn)
        assertEquals(RestrictedMode.READ_ONLY, context.restrictedMode)
        assertEquals("m-1", context.membershipId)
    }

    @Test
    fun `solo mesero y caja se reabren sin internet`() {
        assertTrue(canWorkOffline(OperationalRole.WAITER))
        assertTrue(canWorkOffline(OperationalRole.CASHIER))
        assertFalse(canWorkOffline(OperationalRole.KITCHEN))
        assertFalse(canWorkOffline(OperationalRole.ADMIN))
        assertFalse(canWorkOffline(OperationalRole.CLIENT))
    }
}
