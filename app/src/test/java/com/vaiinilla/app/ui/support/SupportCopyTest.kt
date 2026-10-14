package com.vaiinilla.app.ui.support

import org.junit.Assert.assertEquals
import org.junit.Test

class SupportCopyTest {
    @Test
    fun `el contacto abre un mailto al equipo`() {
        assertEquals("equipo@vaiinilla.app", SupportCopy.EMAIL)
        assertEquals("mailto:equipo@vaiinilla.app", SupportCopy.MAILTO)
    }

    @Test
    fun `la nota legal introduce los enlaces a Términos y Privacidad`() {
        assertEquals("Los documentos legales vigentes están en:", SupportCopy.LEGAL_NOTE)
    }

    @Test
    fun `las tres tarjetas de ayuda usan el orden y el texto de la PWA`() {
        assertEquals(listOf("Pedidos y saldo", "Pagos", "Tu cuenta"), SupportCopy.cards.map { it.title })
        assertEquals(
            "Si tu pedido no aparece, tardó más de lo indicado o tu saldo no se refleja después de una recarga, " +
                "escríbenos con la hora aproximada y el establecimiento donde lo hiciste.",
            SupportCopy.cards[0].body,
        )
        assertEquals(
            "Los pagos con tarjeta los procesa Stripe de forma segura. Si un cargo se ve duplicado o no se completó, " +
                "mándanos el correo de tu cuenta y la fecha del intento.",
            SupportCopy.cards[1].body,
        )
        assertEquals(
            "Para eliminar tu cuenta puedes hacerlo desde la app en Configuración, o seguir la guía en " +
                "app.vaiinilla.app/eliminar-cuenta.",
            SupportCopy.cards[2].body,
        )
    }

    @Test
    fun `search narrows topics by title or answer and an empty search shows them all`() {
        assertEquals(SupportCopy.cards, supportTopicsMatching("  "))
        assertEquals(listOf("Pagos"), supportTopicsMatching("stripe").map { it.title })
        assertEquals(emptyList<SupportCard>(), supportTopicsMatching("zzz"))
    }
}
