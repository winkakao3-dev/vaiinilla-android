package com.vaiinilla.app.data.discovery

import com.vaiinilla.app.domain.model.PublicVideo
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicEstablishmentDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun decodeFirst(venueJson: String) =
        json
            .decodeFromString<EstablishmentListEnvelope>("""{"data":[$venueJson]}""")
            .data
            .single()
            .toDomain()

    private fun venueWith(extra: String = "") =
        """{"id":"est-1","nombre":"Cafetería Centro","slug":"cafeteria-centro",""" +
            """"identificador_cliente_etiqueta":"Matrícula","identificador_cliente_obligatorio":false$extra}"""

    @Test
    fun `missing videos_local maps to empty list`() {
        assertTrue(decodeFirst(venueWith()).localVideos.isEmpty())
    }

    @Test
    fun `null videos_local maps to empty list`() {
        assertTrue(decodeFirst(venueWith(""","videos_local":null""")).localVideos.isEmpty())
    }

    @Test
    fun `keeps only web links with a title and trims them`() {
        val venue =
            decodeFirst(
                venueWith(
                    """,
                    "videos_local":[
                      {"url":"https://youtu.be/abc","titulo":"  Tour del local  "},
                      {"url":"http://example.com/v","titulo":"Cocina"},
                      {"url":"intent://evil#Intent;end","titulo":"Intent"},
                      {"url":"javascript:alert(1)","titulo":"JS"},
                      {"url":"https://example.com/sin-titulo","titulo":"   "},
                      {"titulo":"Sin URL"},
                      {"url":null,"titulo":null}
                    ]""",
                ),
            )

        assertEquals(
            listOf(
                PublicVideo(url = "https://youtu.be/abc", title = "Tour del local"),
                PublicVideo(url = "http://example.com/v", title = "Cocina"),
            ),
            venue.localVideos,
        )
    }
}
