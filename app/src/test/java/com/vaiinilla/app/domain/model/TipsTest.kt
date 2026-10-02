package com.vaiinilla.app.domain.model

import com.vaiinilla.app.data.operational.WaiterContractJson
import com.vaiinilla.app.data.order.OrderContractJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TipsTest {
    @Test
    fun `porcentaje redondeado al centavo, medio centavo hacia arriba`() {
        assertEquals(BigDecimal("22.58"), TipChoice.Percent(15).amountFor(BigDecimal("150.50")))
        assertEquals(BigDecimal("6.67"), TipChoice.Percent(20).amountFor(BigDecimal("33.33")))
        assertEquals(BigDecimal("0.00"), TipChoice.None.amountFor(BigDecimal("100.00")))
    }

    @Test
    fun `monto libre valido con dos decimales y lo demas es cero`() {
        assertEquals(BigDecimal("12.50"), TipChoice.Custom("12.5").amountFor(BigDecimal("100")))
        assertEquals(BigDecimal("0.00"), TipChoice.Custom("abc").amountFor(BigDecimal("100")))
        assertEquals(BigDecimal("0.00"), TipChoice.Custom("1.234").amountFor(BigDecimal("100")))
    }

    @Test
    fun `sin propina no se manda el campo`() {
        assertNull(BigDecimal.ZERO.asTipWire())
        assertEquals("15.00", BigDecimal("15").asTipWire())
        assertFalse(OrderContractJson().encodeCashCollection("100.00", 2).contains("propina"))
        assertTrue(
            OrderContractJson().encodeCashCollection("120.00", 2, tip = "15.00").contains("\"propina\":\"15.00\""),
        )
        assertFalse(WaiterContractJson().encodeCollectAccount("100.00", "100.00").contains("propina"))
        assertTrue(
            WaiterContractJson()
                .encodeCollectAccount(
                    "130.00",
                    "100.00",
                    tip = "20.00",
                ).contains("\"propina\":\"20.00\""),
        )
    }
}
