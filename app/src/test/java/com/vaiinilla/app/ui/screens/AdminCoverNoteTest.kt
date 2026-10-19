package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdminCoverNoteTest {
    private fun mode(
        role: OperationalRole,
        place: String = "Café Norte",
    ) = AuthorizedMode(role = role, establishmentId = place, establishmentName = place, membershipId = "$place-$role")

    @Test
    fun `sin administración no hay nota`() {
        assertNull(adminCoverNote(listOf(mode(OperationalRole.CASHIER), mode(OperationalRole.WAITER))))
    }

    @Test
    fun `administración sola, sin los demás roles todavía, no explica nada`() {
        assertNull(adminCoverNote(listOf(mode(OperationalRole.ADMIN))))
    }

    @Test
    fun `administración con sus roles explica de dónde salen`() {
        val note =
            adminCoverNote(
                listOf(
                    mode(OperationalRole.ADMIN),
                    mode(OperationalRole.CASHIER),
                    mode(OperationalRole.KITCHEN),
                    mode(OperationalRole.WAITER),
                ),
            )
        assertEquals(
            "Como eres administración en Café Norte, también tienes Caja, Mesero y Cocina para cubrir a quien " +
                "falte. Entras con las mismas pantallas de ese rol y lo que hagas queda a tu nombre.",
            note,
        )
    }

    @Test
    fun `en varios negocios no nombra uno solo`() {
        val note =
            adminCoverNote(
                listOf(
                    mode(OperationalRole.ADMIN, "A"),
                    mode(OperationalRole.CASHIER, "A"),
                    mode(OperationalRole.ADMIN, "B"),
                    mode(OperationalRole.CASHIER, "B"),
                ),
            )
        assertEquals(true, note?.startsWith("Como eres administración, también"))
    }
}
