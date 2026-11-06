package com.vaiinilla.app.ui.operational

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** En vivo, #10: el aviso «Renta … cobrada» de un negocio seguía en la Caja del otro. */
class StoreSwitchNoticeTest {
    @Test
    fun `another store only when both are known and differ`() {
        assertTrue(isOtherEstablishment("boliche", "pasteleria"))
        assertFalse(isOtherEstablishment("boliche", "boliche"))
    }

    @Test
    fun `a token without the claim is not a switch`() {
        assertFalse(isOtherEstablishment(null, "pasteleria"))
        assertFalse(isOtherEstablishment("boliche", null))
        assertFalse(isOtherEstablishment(null, null))
    }
}
