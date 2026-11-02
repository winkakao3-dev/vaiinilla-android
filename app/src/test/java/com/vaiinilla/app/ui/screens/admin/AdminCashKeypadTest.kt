package com.vaiinilla.app.ui.screens.admin

import com.vaiinilla.app.data.order.OrderContractJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdminCashKeypadTest {
    @Test
    fun `el teclado deja un punto y dos decimales y borra de uno en uno`() {
        val typed = listOf("5", "0", "0", ".", ".", "5", "0", "9").fold("") { amount, key -> pressKey(amount, key) }
        assertEquals("500.50", typed)
        assertEquals("500.5", pressKey(typed, "⌫"))
        assertEquals("", pressKey("", "⌫"))
    }

    @Test
    fun `el arqueo lee efectivo_esperado de la sesion o de meta`() {
        val json = OrderContractJson()
        assertEquals(
            "1250.00",
            json.parseCashAmount(
                """{"data":{"id":"s","efectivo_esperado":"1250"},"error":null}""",
                "efectivo_esperado",
            ),
        )
        assertEquals(
            "80.50",
            json.parseCashAmount("""{"data":null,"meta":{"efectivo_esperado":80.5}}""", "efectivo_esperado"),
        )
        assertNull(json.parseCashAmount("""{"data":{"id":"s"},"error":null}""", "efectivo_esperado"))
    }
}
