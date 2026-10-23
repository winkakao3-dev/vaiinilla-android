package com.vaiinilla.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/** F19: el contexto que quedó de otro negocio no sirve para preguntar si esta escuela es escuela. */
class JwtContextEstablishmentTest {
    private fun jwt(payload: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return "h." + encoder.encodeToString(payload.toByteArray()) + ".s"
    }

    @Test
    fun elContextoDeOtroNegocioNoEsDeEstaEscuela() {
        val cafe = jwt("""{"uid":"u","rol":"cliente","establecimiento_id":"cafe","membresia_id":"m"}""")
        assertFalse(JwtContextScope.isContextFor(cafe, "escuela"))
        assertTrue(JwtContextScope.isContextFor(cafe, "cafe"))
    }

    @Test
    fun sinTokenNoHayContextoYSinClaimSeDaPorBueno() {
        assertFalse(JwtContextScope.isContextFor(null, "escuela"))
        assertFalse(JwtContextScope.isContextFor(" ", "escuela"))
        assertTrue(JwtContextScope.isContextFor("opaco", "escuela"))
    }
}
