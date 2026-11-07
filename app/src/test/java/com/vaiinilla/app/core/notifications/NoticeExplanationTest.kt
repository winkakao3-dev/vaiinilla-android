package com.vaiinilla.app.core.notifications

import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.VenueContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Las mismas 9 pruebas que `NoticeExplanationTests.swift` de iOS, más la limpieza del payload. */
class NoticeExplanationTest {
    private val usagi = "est-usagi"
    private val michi = "est-michi"

    private fun membership(
        role: OperationalRole,
        establishment: String,
        name: String,
    ) = NoticeMembership("m-$role-$establishment", role, establishment, name)

    private fun mode(
        role: OperationalRole,
        establishment: String,
        name: String,
    ) = AuthorizedMode(role, establishment, name, membershipId = "m-$role-$establishment")

    @Test
    fun cashierAlertGoesToCashierAndExplainsTheOtherBusiness() {
        val explanation =
            NoticeExplanation.explain(
                VenueContext.STANDARD,
                mapOf("type" to "staff_alert", "alert" to "por_cobrar", "orderId" to "p-1", "folio" to "10"),
                isRemote = true,
            )
        assertEquals(listOf(OperationalRole.CASHIER), explanation.roles)
        assertEquals("p-1", explanation.orderId)
        val reason =
            explanation.reason(
                VenueContext.STANDARD,
                listOf(
                    membership(OperationalRole.WAITER, usagi, "USAGI"),
                    membership(OperationalRole.CASHIER, michi, "Michi Market"),
                ),
                openMode = "Mesero en USAGI",
            )
        assertTrue(reason.contains("Caja en Michi Market"))
        assertFalse(reason.contains("Mesero en USAGI,"))
        assertTrue(reason.contains("«Mesero en USAGI»"))
    }

    @Test
    fun barCommandGoesToCashierAndKitchenCommandToKitchen() {
        val bar =
            NoticeExplanation.explain(
                VenueContext.STANDARD,
                mapOf("type" to "staff_alert", "alert" to "nueva_comanda", "station" to "caja"),
                isRemote = true,
            )
        val kitchen =
            NoticeExplanation.explain(
                VenueContext.STANDARD,
                mapOf("type" to "staff_alert", "alert" to "nueva_comanda"),
                isRemote = true,
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
                isRemote = true,
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
                isRemote = true,
            )
        assertTrue(explanation.roles.isEmpty())
        assertEquals(usagi, explanation.establishmentId)
        assertTrue(explanation.what.contains("«Listo»"))
    }

    @Test
    fun localOrderNoticeSaysTheAppMadeIt() {
        val explanation =
            NoticeExplanation.explain(
                VenueContext.STANDARD,
                mapOf("type" to "order_state", "orderId" to "p-3", "state" to "cobrado"),
                isRemote = false,
            )
        assertTrue(explanation.trigger.contains("No vino del servidor"))
    }

    @Test
    fun unknownNoticeIsStillExplained() {
        assertEquals(
            "Otro aviso",
            NoticeExplanation.explain(VenueContext.STANDARD, mapOf("type" to "nuevo"), isRemote = true).kind,
        )
    }

    @Test
    fun reasonWhenTheAccountNoLongerHasTheRole() {
        val explanation =
            NoticeExplanation.explain(
                VenueContext.STANDARD,
                mapOf("type" to "staff_alert", "alert" to "listo_mesero"),
                isRemote = true,
            )
        val reason =
            explanation.reason(
                VenueContext.STANDARD,
                listOf(membership(OperationalRole.CASHIER, usagi, "USAGI")),
                openMode = null,
            )
        assertTrue(reason.contains("no tiene Mesero"))
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
