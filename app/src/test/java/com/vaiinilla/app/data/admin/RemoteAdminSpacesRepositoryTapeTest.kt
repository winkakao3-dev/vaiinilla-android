package com.vaiinilla.app.data.admin

import com.vaiinilla.app.data.tape.ServerTape
import com.vaiinilla.app.data.tape.TapeFloor
import com.vaiinilla.app.domain.admin.SpacePatch
import com.vaiinilla.app.domain.admin.SpaceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Base64

/** Los repositorios reales del dueño (lugares, personal, menú) contra lo que el servidor de pruebas contestó. */
class RemoteAdminSpacesRepositoryTapeTest {
    @get:Rule
    val tape = ServerTape()

    private val floor by lazy { TapeFloor(tape) }

    @Test
    fun `un lugar se crea, se edita, recibe y pierde su foto, se crean varios de un golpe y se borran`() {
        val spaces = floor.spaces()
        val table = floor.newSpace()
        assertEquals(SpaceType.TABLE.wire, table.typeWire)
        assertTrue(table.active)
        assertTrue("cada lugar nace con su QR", table.qrUrl.isNotBlank())

        val patch =
            SpacePatch(
                name = "${table.name} B",
                description = "Junto a la ventana",
                features = listOf("Sombra"),
                setsProfile = true,
                capacity = 6,
                setsCapacity = true,
            )
        val edited = spaces.update(table.id, patch, floor.key()).getOrThrow()
        assertEquals("${table.name} B", edited.name)
        assertEquals("Junto a la ventana", edited.description)
        assertEquals(listOf("Sombra"), edited.features)
        assertEquals(6, edited.capacity)

        val photo = spaces.uploadImage(table.id, PIXEL, "mesa.png", "image/png", floor.key()).getOrThrow()
        assertNotNull(photo.imageUrl)
        assertNull(spaces.removeImage(table.id, floor.key()).getOrThrow().imageUrl)

        val prefix = "${table.name} L"
        val batch = spaces.createBatch(SpaceType.TABLE, prefix, 1, 3, floor.key(), 4, null, null).getOrThrow()
        assertEquals(3, batch.requested)
        assertEquals(3, batch.created)
        val created = spaces.spaces().getOrThrow().filter { it.name.startsWith(prefix) }
        assertEquals(listOf("$prefix 1", "$prefix 2", "$prefix 3"), created.map { it.name }.sorted())
        assertTrue(created.all { it.capacity == 4 })

        (created + edited).forEach { spaces.delete(it.id, floor.key()).getOrThrow() }
        assertFalse(spaces.spaces().getOrThrow().any { it.name.startsWith(table.name) && it.active })
    }

    @Test
    fun `el horario de reservas se guarda completo y se lee igual`() {
        val spaces = floor.spaces()
        val before = spaces.reservationSchedule().getOrThrow()
        val wanted = before.copy(opening = "09:00", closing = "21:00", daysAhead = 10, weekDays = listOf(1, 2, 3, 4, 5))
        val saved = spaces.saveReservationSchedule(wanted, floor.key()).getOrThrow()
        assertEquals("09:00", saved.opening)
        assertEquals("21:00", saved.closing)
        assertEquals(10, saved.daysAhead)
        assertEquals(listOf(1, 2, 3, 4, 5), saved.weekDays)
        assertEquals(saved, spaces.reservationSchedule().getOrThrow())
        spaces.saveReservationSchedule(before, floor.key()).getOrThrow()
    }

    private companion object {
        val PIXEL: ByteArray =
            Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
            )
    }
}
