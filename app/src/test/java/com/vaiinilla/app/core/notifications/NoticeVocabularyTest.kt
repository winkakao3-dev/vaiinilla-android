package com.vaiinilla.app.core.notifications

import com.vaiinilla.app.core.venue.VenueContextStore
import com.vaiinilla.app.domain.model.Customer
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.VenueKind
import org.junit.Assert.assertEquals
import org.junit.Test

/** El vocabulario de los avisos sigue al tipo del negocio: alumno en escuela, huésped en hotel, cliente en el resto. */
class NoticeVocabularyTest {
    private val store = VenueContextStore()
    private val venue get() = store.current

    private fun explain(alert: String) =
        NoticeExplanation.explain(
            venue,
            mapOf("type" to "staff_alert", "alert" to alert, "espacio_nombre" to "Habitación 304"),
        )

    @Test
    fun `quien pide se llama segun el negocio`() {
        assertEquals(Customer.STUDENT, Customer.of(VenueKind.fromWire("cafeteria"), school = true))
        assertEquals(Customer.GUEST, Customer.of(VenueKind.fromWire("hotel"), school = false))
        listOf(null, "cafeteria", "restaurante", "bar", "boliche", "padel", "drive_thru", "reposteria").forEach {
            assertEquals(Customer.CUSTOMER, Customer.of(VenueKind.fromWire(it), school = false))
        }
    }

    @Test
    fun `el modo cliente toma el nombre del negocio y los demas modos no cambian`() {
        store.update("hotel", false)
        assertEquals("Huésped", venue.roleLabel(OperationalRole.CLIENT))
        assertEquals("Caja", venue.roleLabel(OperationalRole.CASHIER))
        assertEquals("Alumno", venue.roleLabel(OperationalRole.CLIENT, school = true))
        store.update("boliche", false)
        assertEquals("Cliente", venue.roleLabel(OperationalRole.CLIENT))
        store.update("cafeteria", true)
        assertEquals("Alumno", venue.roleLabel(OperationalRole.CLIENT))
    }

    @Test
    fun `el aviso de listo para el mesero nombra a quien corresponde`() {
        store.update("cafeteria", true)
        assertEquals("Llévaselo al alumno a su lugar.", OrderAdvanceNotifier.readyForWaiterText(venue))
        store.update("restaurante", false)
        assertEquals("Llévaselo al cliente a su lugar.", OrderAdvanceNotifier.readyForWaiterText(venue))
        store.update("boliche", false)
        assertEquals("Llévaselo al cliente a su lugar.", OrderAdvanceNotifier.readyForWaiterText(venue))
        store.update("hotel", false)
        assertEquals("Llévaselo al huésped a su habitación.", OrderAdvanceNotifier.readyForWaiterText(venue))
    }

    @Test
    fun `la llamada sin nombre de espacio dice habitacion en un hotel y mesa en los demas`() {
        assertEquals("Una mesa te llama", OrderAdvanceNotifier.tableCallTitle(venue, ""))
        store.update("hotel", false)
        assertEquals("Una habitación te llama", OrderAdvanceNotifier.tableCallTitle(venue, ""))
        assertEquals("Hab. 12 te llama", OrderAdvanceNotifier.tableCallTitle(venue, "Hab. 12"))
    }

    @Test
    fun `el aviso de que el mesero va en camino lleva a donde esta quien llamo`() {
        assertEquals("tu mesa", SpaceCopy.yourPlace(venue, null))
        store.update("hotel", false)
        assertEquals("tu habitación", SpaceCopy.yourPlace(venue, null))
        val explanation = NoticeExplanation.explain(venue, mapOf("type" to "mesero_en_camino"))
        assertEquals("Recepción va a tu habitación.", explanation.what)
    }

    @Test
    fun `la explicacion de un aviso de hotel habla de habitacion y de huesped`() {
        store.update("hotel", false)
        assertEquals(
            "Un pedido está listo para llevarlo a su habitación.",
            explain("listo_mesero").what,
        )
    }

    @Test
    fun `en los demas negocios la explicacion queda como siempre`() {
        store.update("restaurante", false)
        assertEquals("Un pedido está listo para llevarlo a su mesa o espacio.", explain("listo_mesero").what)
    }

    @Test
    fun `el ultimo negocio visto se guarda y se recupera para un aviso con la app cerrada`() {
        val saved = mutableListOf<Pair<String?, Boolean>>()
        store.onChange = { type, school -> saved += type to school }
        store.update("hotel", null)
        store.update("hotel", null)
        store.update("cafeteria", true)
        // Solo se guarda el tipo que cambia el vocabulario (hotel, boliche); lo demás vuelve a ser el negocio estándar.
        assertEquals(listOf<Pair<String?, Boolean>>("hotel" to false, null to true), saved)

        store.onChange = null
        store.restore("hotel", false)
        assertEquals("huésped", venue.customer.lower)
        assertEquals(2, saved.size)
    }

    @Test
    fun `un estado que no trae modo escuela no borra el que ya se sabia`() {
        store.update("cafeteria", true)
        store.update("cafeteria", null)
        assertEquals("alumno", venue.customer.lower)
    }
}
