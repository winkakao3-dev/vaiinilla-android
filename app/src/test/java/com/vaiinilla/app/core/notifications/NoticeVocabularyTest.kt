package com.vaiinilla.app.core.notifications

import com.vaiinilla.app.domain.model.Customer
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.Venue
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El vocabulario de los avisos sigue al tipo del negocio: alumno en escuela, huésped en hotel, cliente en el resto. */
class NoticeVocabularyTest {
    @After
    fun resetVenue() {
        Venue.onChange = null
        Venue.restore(null, false)
    }

    private fun explain(alert: String) =
        NoticeExplanation.explain(
            mapOf("type" to "staff_alert", "alert" to alert, "espacio_nombre" to "Habitación 304"),
            isRemote = true,
        )

    @Test
    fun `quien pide se llama segun el negocio`() {
        assertEquals(Customer.STUDENT, Customer.of("cafeteria", school = true))
        assertEquals(Customer.GUEST, Customer.of("hotel", school = false))
        listOf(null, "cafeteria", "restaurante", "bar", "boliche", "padel", "drive_thru", "reposteria").forEach {
            assertEquals(Customer.CUSTOMER, Customer.of(it, school = false))
        }
    }

    @Test
    fun `el modo cliente toma el nombre del negocio y los demas modos no cambian`() {
        Venue.update("hotel", false)
        assertEquals("Huésped", Venue.roleLabel(OperationalRole.CLIENT))
        assertEquals("Caja", Venue.roleLabel(OperationalRole.CASHIER))
        assertEquals("Alumno", Venue.roleLabel(OperationalRole.CLIENT, school = true))
        Venue.update("boliche", false)
        assertEquals("Cliente", Venue.roleLabel(OperationalRole.CLIENT))
        Venue.update("cafeteria", true)
        assertEquals("Alumno", Venue.roleLabel(OperationalRole.CLIENT))
    }

    @Test
    fun `el aviso de listo para el mesero nombra a quien corresponde`() {
        Venue.update("cafeteria", true)
        assertEquals("Entregar al espacio del alumno.", OrderAdvanceNotifier.readyForWaiterText())
        Venue.update("restaurante", false)
        assertEquals("Entregar al espacio del cliente.", OrderAdvanceNotifier.readyForWaiterText())
        Venue.update("boliche", false)
        assertEquals("Entregar al espacio del cliente.", OrderAdvanceNotifier.readyForWaiterText())
        Venue.update("hotel", false)
        assertEquals("Entregar a la habitación del huésped.", OrderAdvanceNotifier.readyForWaiterText())
    }

    @Test
    fun `la llamada sin nombre de espacio dice habitacion en un hotel y mesa en los demas`() {
        assertEquals("Una mesa te llama", OrderAdvanceNotifier.tableCallTitle(""))
        Venue.update("hotel", false)
        assertEquals("Una habitación te llama", OrderAdvanceNotifier.tableCallTitle(""))
        assertEquals("Hab. 12 te llama", OrderAdvanceNotifier.tableCallTitle("Hab. 12"))
    }

    @Test
    fun `el aviso de que el mesero va en camino lleva a donde esta quien llamo`() {
        assertEquals("tu mesa", SpaceCopy.yourPlace(null))
        Venue.update("hotel", false)
        assertEquals("tu habitación", SpaceCopy.yourPlace(null))
        val explanation = NoticeExplanation.explain(mapOf("type" to "mesero_en_camino"), isRemote = true)
        assertEquals("Tu mesero va a tu habitación.", explanation.what)
    }

    @Test
    fun `la explicacion de un aviso de hotel habla de habitacion y de huesped`() {
        Venue.update("hotel", false)
        assertEquals(
            "Un pedido está listo para llevarlo a su habitación.",
            explain("listo_mesero").what,
        )
        assertEquals("La persona hospedada en Habitación 304.", explain("llamada_mesa").trigger)
        val reason =
            explain("por_cobrar").reason(
                listOf(NoticeMembership("m1", OperationalRole.CASHIER, "e1", "Hotel Sol")),
                openMode = null,
            )
        assertTrue(reason.contains("Caja en Hotel Sol"))
    }

    @Test
    fun `en los demas negocios la explicacion queda como siempre`() {
        Venue.update("restaurante", false)
        assertEquals("Un pedido está listo para llevarlo a su mesa o espacio.", explain("listo_mesero").what)
        assertEquals("La persona sentada en Habitación 304.", explain("llamada_mesa").trigger)
        assertFalse(explain("llamada_mesa").trigger.contains("hospedada"))
    }

    @Test
    fun `cada membresia dice su propio nombre del modo cliente`() {
        Venue.update("hotel", false)
        val reason =
            NoticeExplanation
                .explain(mapOf("type" to "order_state", "orderId" to "p-1"), isRemote = true)
                .copy(roles = listOf(OperationalRole.CLIENT))
                .reason(
                    listOf(
                        NoticeMembership("m1", OperationalRole.CLIENT, "e1", "Escuela Sol", isSchool = true),
                        NoticeMembership("m2", OperationalRole.CLIENT, "e2", "Hotel Luna"),
                    ),
                    openMode = null,
                )
        assertTrue(reason.contains("Alumno en Escuela Sol"))
        assertTrue(reason.contains("Huésped en Hotel Luna"))
    }

    @Test
    fun `el ultimo negocio visto se guarda y se recupera para un aviso con la app cerrada`() {
        val saved = mutableListOf<Pair<String?, Boolean>>()
        Venue.onChange = { type, school -> saved += type to school }
        Venue.update("hotel", null)
        Venue.update("hotel", null)
        Venue.update("cafeteria", true)
        assertEquals(listOf<Pair<String?, Boolean>>("hotel" to false, "cafeteria" to true), saved)

        Venue.onChange = null
        Venue.restore("hotel", false)
        assertEquals("huésped", Venue.customer)
        assertEquals(2, saved.size)
    }

    @Test
    fun `un estado que no trae modo escuela no borra el que ya se sabia`() {
        Venue.update("cafeteria", true)
        Venue.update("cafeteria", null)
        assertEquals("alumno", Venue.customer)
    }
}
