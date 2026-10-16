package com.vaiinilla.app

import com.vaiinilla.app.data.discovery.PublicEstablishmentDto
import com.vaiinilla.app.data.discovery.toDomain
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PublicEstablishmentImagesTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(images: String) =
        json
            .decodeFromString<PublicEstablishmentDto>(
                """{"id":"e1","nombre":"Fonda","slug":"fonda","identificador_cliente_etiqueta":"Nombre",
                "identificador_cliente_obligatorio":false$images}""",
            ).toDomain()

    @Test
    fun `trae la foto y el logo del negocio`() {
        val place = parse(""","imagen_url":"https://cdn.test/foto.jpg","logo_url":"https://cdn.test/logo.png"""")
        assertEquals("https://cdn.test/foto.jpg", place.photoUrl)
        assertEquals("https://cdn.test/logo.png", place.logoUrl)
    }

    @Test
    fun `sin imágenes, o sin https, quedan vacías y la app usa su escena`() {
        val none = parse("")
        assertNull(none.photoUrl)
        assertNull(none.logoUrl)
        val insecure = parse(""","imagen_url":"http://cdn.test/foto.jpg","logo_url":null""")
        assertNull(insecure.photoUrl)
        assertNull(insecure.logoUrl)
    }
}
