package com.vaiinilla.app.ui.hotel.admin

import com.vaiinilla.app.domain.hotel.HotelConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HotelSettingsAdminLogicTest {
    @Test
    fun `una hora de una cifra se manda con dos y lo demas queda igual`() {
        val padded = HotelConfig(kitchenFrom = "7:00", kitchenTo = "23:00", checkoutTime = "9:30", lateCheckoutTime = null).withPaddedClocks()
        assertEquals("07:00", padded.kitchenFrom)
        assertEquals("23:00", padded.kitchenTo)
        assertEquals("09:30", padded.checkoutTime)
        assertNull(padded.lateCheckoutTime)
    }
}
