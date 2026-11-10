package com.vaiinilla.app.core.notifications

import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.VenueContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A qué modos va cada aviso, qué pedido trae y en qué orden se busca ese pedido. */
class NoticeExplanationTest {
    private val usagi = "est-usagi"
    private val michi = "est-michi"

    private fun mode(
        role: OperationalRole,
        establishment: String,
        name: String,
    ) = AuthorizedMode(role, establishment, name, membershipId = "m-$role-$establishment")

    @Test
    fun cashierAlertGoesToCashier() {
        val explanation =
            NoticeExplanation.explain(
                VenueContext.STANDARD,
                mapOf("type" to "staff_alert", "alert" to "por_cobrar", "orderId" to "p-1", "folio" to "10"),
            )
        assertEquals(listOf(OperationalRole.CASHIER), explanation.roles)
        assertEquals("p-1", explanation.orderId)
    }

    @Test
    fun barCommandGoesToCashierAndKitchenCommandToKitchen() {
        val bar =
            NoticeExplanation.explain(
                VenueContext.STANDARD,
                mapOf("type" to "staff_alert", "alert" to "nueva_comanda", "station" to "caja"),
            )
        val kitchen =
            NoticeExplanation.explain(
                VenueContext.STANDARD,
                mapOf("type" to "staff_alert", "alert" to "nueva_comanda"),
            )
        assertEquals(listOf(OperationalRole.CASHIER), bar.roles)
        assertEquals(listOf(OperationalRole.KITCHEN), kitchen.roles)
    }

    @Test
    fun shiftEndingGoesToWaiterAndCashier() {
        val explanation =
            NoticeExplanation.explain(
                VenueContext.STANDARD,
                mapOf("type" to "staff_alert", "alert" to "turno_por_terminar", "espacio_nombre" to "Cancha 2"),
            )
        assertEquals(listOf(OperationalRole.WAITER, OperationalRole.CASHIER), explanation.roles)
        assertTrue(explanation.what.contains("Cancha 2"))
        assertNull(explanation.orderId)
    }

    @Test
    fun childOrderIsPersonalAndKeepsItsBusiness() {
        val explanation =
            NoticeExplanation.explain(
                VenueContext.STANDARD,
                mapOf(
                    "type" to "order_state",
                    "orderId" to "p-2",
                    "state" to "listo",
                    "dependienteUsuarioId" to "u-9",
                    "establecimientoId" to usagi,
                ),
            )
        assertTrue(explanation.roles.isEmpty())
        assertEquals(usagi, explanation.establishmentId)
        assertTrue(explanation.what.contains("«Listo»"))
    }

    @Test
    fun unknownNoticeIsStillExplained() {
        assertEquals(
            "Otro aviso",
            NoticeExplanation.explain(VenueContext.STANDARD, mapOf("type" to "nuevo")).kind,
        )
    }

    @Test
    fun lookupTriesTheRoleFirstThenAdministration() {
        val waiter = mode(OperationalRole.WAITER, usagi, "USAGI")
        val cashier = mode(OperationalRole.CASHIER, michi, "Michi Market")
        val admin = mode(OperationalRole.ADMIN, usagi, "USAGI")
        val customer = mode(OperationalRole.CLIENT, usagi, "USAGI")
        val candidates =
            NoticeOrderLookup.candidates(
                roles = listOf(OperationalRole.CASHIER),
                establishmentId = null,
                modes = listOf(waiter, admin, customer, cashier),
            )
        assertEquals(listOf(cashier, admin), candidates)
    }

    @Test
    fun personalNoticeIsLookedUpAsCustomerOfItsBusiness() {
        val customerUsagi = mode(OperationalRole.CLIENT, usagi, "USAGI")
        val customerMichi = mode(OperationalRole.CLIENT, michi, "Michi Market")
        val candidates =
            NoticeOrderLookup.candidates(
                roles = emptyList(),
                establishmentId = michi,
                modes = listOf(customerUsagi, customerMichi),
            )
        assertEquals(listOf(customerMichi), candidates)
    }

    @Test
    fun firebaseInternalKeysAreDroppedFromThePayload() {
        val cleaned =
            ReceivedNoticeLog.cleanPayload(
                mapOf("type" to "staff_alert", "google.message_id" to "x", "gcm.n.e" to "1", "folio" to "4"),
            )
        assertEquals(mapOf("type" to "staff_alert", "folio" to "4"), cleaned)
    }
}
