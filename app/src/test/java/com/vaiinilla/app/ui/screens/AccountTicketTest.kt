package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.repository.AccountOrder
import com.vaiinilla.app.domain.repository.SpaceAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountTicketTest {
    private val account =
        SpaceAccount(
            orders =
                listOf(
                    AccountOrder("a", 41, "entregado", "120.00", "Ana", "Hamburguesa <doble>", true, pending = true),
                    AccountOrder("b", 42, "entregado", "60.00", null, "Papas", true, pending = false),
                ),
            total = "180.00",
            pending = "120.00",
            paid = "60.00",
            settled = false,
        )

    @Test
    fun `el ticket lista pedidos, total, pagado y por cobrar`() {
        val html = buildAccountTicketHtml("Cancha 1", account, "29/09/2026 20:15")
        assertTrue(html.contains("<h1>Cancha 1</h1>"))
        assertTrue(html.contains("29/09/2026 20:15"))
        assertTrue(html.contains("#41 · Ana"))
        assertTrue(html.contains("#42 (pagado)"))
        assertTrue(html.contains("$180.00"))
        assertTrue(html.contains("$60.00"))
        assertTrue(html.contains("$120.00"))
        assertTrue(html.contains("No es un comprobante fiscal"))
    }

    @Test
    fun `el texto de usuario se escapa para no romper el documento`() {
        val html = buildAccountTicketHtml("Cancha <1>", account, "hoy")
        assertTrue(html.contains("Hamburguesa &lt;doble&gt;"))
        assertTrue(html.contains("Cancha &lt;1&gt;"))
        assertFalse(html.contains("<doble>"))
    }

    @Test
    fun `escapa los caracteres especiales de html`() {
        assertEquals("&lt;a href=&quot;x&quot;&gt;&amp;&#39;", escapeHtml("<a href=\"x\">&'"))
    }
}
