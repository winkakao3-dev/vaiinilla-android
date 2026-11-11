package com.vaiinilla.app.data.platform

import com.vaiinilla.app.data.tape.ServerTape
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

/** El repositorio real de Plataforma contra lo que el servidor de pruebas contestó de verdad. */
class PlatformRepositoryTapeTest {
    @get:Rule
    val tape = ServerTape()

    private fun key() = UUID.randomUUID().toString()

    @Test
    fun `Plataforma crea una tienda, la encuentra, la configura, le pone foto y ve las metricas`() {
        runBlocking {
            val platform = PlatformRepository(tape.client(SESSION))
            val token = tape.bearer(SESSION)
            val suffix = tape.value("sufijo") { UUID.randomUUID().toString().take(6) }
            val name = "Cinta de plataforma $suffix"
            val fields =
                mapOf(
                    "nombre" to name,
                    "slug" to "cinta-plataforma-$suffix",
                    "tipo" to "cafeteria",
                    "zona_horaria" to "America/Chihuahua",
                    "hora_cierre_forzado" to "03:00:00",
                )
            val created = platform.create(token, fields, key()).getOrThrow()
            assertEquals(name, created.nombre)
            assertEquals("cinta-plataforma-$suffix", created.slug)
            assertEquals("cafeteria", created.tipo)

            val found = platform.stores(token, null, name, null).getOrThrow().stores
            assertEquals(listOf(created.id), found.map { it.id })

            val change = mapOf("descripcion" to "Tienda de grabación")
            assertEquals(
                "Tienda de grabación",
                platform.configure(token, created.id, change, key()).getOrThrow().descripcion,
            )
            assertNotNull(platform.uploadImage(token, created.id, PIXEL, "image/png", key()).getOrThrow())

            val link = "https://www.google.com/maps/@28.6353,-106.0889,17z"
            val (latitude, longitude) = platform.resolveMaps(token, link).getOrThrow()
            assertEquals(28.6353, latitude, 0.001)
            assertEquals(-106.0889, longitude, 0.001)

            val today = LocalDate.parse(tape.value("hoy") { LocalDate.now().toString() })
            val metrics = platform.metrics(token, today.minusDays(7).toString(), today.toString()).getOrThrow()
            assertTrue(metrics.resumen.toString().isNotBlank())

            val closed = platform.setSuspended(token, created.id, suspend = true, "Grabación terminada", key())
            assertEquals("suspendido", closed.getOrThrow().estado)
        }
    }

    private companion object {
        const val SESSION = "plataforma.superadmin"
        val PIXEL: ByteArray =
            Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
            )
    }
}
