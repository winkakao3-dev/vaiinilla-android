package com.vaiinilla.app.domain.model

import com.vaiinilla.app.domain.guardianship.compactMoney
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/** Lo que encontró break-ui: iniciales con emoji y montos sin separador de miles. */
class NameInitialsTest {
    @Test
    fun skipsWordsWithoutLetters() {
        assertEquals("SR", NameInitials.of("🦄 Sofi Ramírez"))
        assertEquals("D", NameInitials.of("🍩 Donas La Esperanza", max = 1))
        assertEquals("ÑZ", NameInitials.of("ñandú zúñiga"))
        assertEquals("J", NameInitials.of("Jo"))
        assertEquals("", NameInitials.of("🌮 "))
    }

    @Test
    fun moneyGroupsThousands() {
        assertEquals("\$12,345.67", compactMoney(BigDecimal("12345.67")))
        assertEquals("\$1,000", compactMoney(BigDecimal("1000.00")))
        assertEquals("\$999.50", compactMoney(BigDecimal("999.5")))
        assertEquals("\$0", compactMoney(BigDecimal.ZERO))
        assertEquals("-\$1,200", compactMoney(BigDecimal("-1200")))
    }
}
