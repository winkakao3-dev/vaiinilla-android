package com.vaiinilla.app.data.mode

import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastStaffModeTest {
    private fun mode(
        role: OperationalRole,
        membership: String = "m-$role",
        active: Boolean = true,
    ) = AuthorizedMode(
        role = role,
        establishmentId = "e1",
        establishmentName = "USAGI",
        membershipId = membership,
        establishmentActive = active,
    )

    private val modes =
        listOf(mode(OperationalRole.CLIENT), mode(OperationalRole.CASHIER), mode(OperationalRole.WAITER))

    @Test
    fun `vuelve al modo de personal que estaba abierto`() {
        assertEquals(
            mode(OperationalRole.CASHIER),
            rememberedAmong(modes, "CASHIER", "e1", "m-CASHIER"),
        )
    }

    @Test
    fun `si le quitaron ese acceso no vuelve a nada`() {
        assertNull(rememberedAmong(modes, "KITCHEN", "e1", "m-KITCHEN"))
        assertNull(rememberedAmong(modes, "CASHIER", "e1", "otra-membresia"))
    }

    @Test
    fun `nunca reabre el modo cliente ni un negocio suspendido`() {
        assertNull(rememberedAmong(modes, "CLIENT", "e1", "m-CLIENT"))
        val suspended = listOf(mode(OperationalRole.CASHIER, active = false))
        assertNull(rememberedAmong(suspended, "CASHIER", "e1", "m-CASHIER"))
    }
}
