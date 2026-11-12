package com.vaiinilla.app.data.admin

import com.vaiinilla.app.data.tape.ServerTape
import com.vaiinilla.app.data.tape.TapeFloor
import com.vaiinilla.app.domain.admin.InvitationState
import com.vaiinilla.app.domain.admin.StaffRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** El repositorio real del personal contra lo que el servidor de pruebas contestó de verdad. */
class RemoteAdminStaffRepositoryTapeTest {
    @get:Rule
    val tape = ServerTape()

    private val floor by lazy { TapeFloor(tape) }

    // Invitar, reenviar y revocar no se graban: el servidor de pruebas no tiene remitente de correo
    // (ACCESS_EMAIL_FROM) y contesta 500 al invitar.
    @Test
    fun `a alguien del equipo se le cambia el puesto y se le da de baja`() {
        val staff = RemoteAdminStaffRepository(floor.client("admin"))
        assertTrue(
            staff.invitations(InvitationState.PENDING, null).getOrThrow().invitations.all {
                it.stateWire ==
                    "pendiente"
            },
        )
        val extra = staff.memberships().getOrThrow().single { it.name == "Vigía extra" }
        assertEquals("mesero", extra.roleWire)
        assertEquals("cocina", staff.changeRole(extra.id, StaffRole.KITCHEN, floor.key()).getOrThrow().roleWire)
        staff.deactivate(extra.id, floor.key()).getOrThrow()
        assertTrue(staff.memberships().getOrThrow().none { it.id == extra.id })
    }
}
