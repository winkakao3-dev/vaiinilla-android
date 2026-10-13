package com.vaiinilla.app.ui.order

import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
import com.vaiinilla.app.ui.screens.paymentActionLabel
import com.vaiinilla.app.ui.screens.shouldShowCheckoutDock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VenueCheckoutRegressionTest {
    @Test
    fun `different establishments form a hard order-state boundary`() {
        val current = venue(establishmentId = "cafeteria-a", spaceId = 12)
        val nextStore = venue(establishmentId = "cafeteria-b", spaceId = null)
        val sameStoreNewSpace = venue(establishmentId = "cafeteria-a", spaceId = 20)

        assertTrue(isEstablishmentSwitch(current, nextStore))
        assertFalse(isEstablishmentSwitch(current, sameStoreNewSpace))
    }

    @Test
    fun `checkout dock disappears while keyboard is visible`() {
        assertTrue(shouldShowCheckoutDock(hasCartItems = true, imeVisible = false))
        assertFalse(shouldShowCheckoutDock(hasCartItems = true, imeVisible = true))
        assertFalse(shouldShowCheckoutDock(hasCartItems = false, imeVisible = false))
    }

    @Test
    fun `pay button says what happens with each method`() {
        assertEquals(
            "Pedir · pagas \$60 en caja",
            paymentActionLabel(PaymentMethod.CASH, payAtEnd = false, total = "\$60"),
        )
        assertEquals(
            "Pagar \$60 con saldo",
            paymentActionLabel(PaymentMethod.BALANCE, payAtEnd = false, total = "\$60"),
        )
        assertEquals("Pagar \$60", paymentActionLabel(PaymentMethod.STRIPE, payAtEnd = false, total = "\$60"))
        assertEquals("Pedir · pagas al final", paymentActionLabel(PaymentMethod.CASH, payAtEnd = true, total = "\$60"))
        assertEquals("Elige cómo pagar", paymentActionLabel(null, payAtEnd = false, total = "\$60"))
    }

    private fun venue(
        establishmentId: String,
        spaceId: Int?,
    ): GuestVenueContext =
        GuestVenueContext(
            establishment =
                PublicEstablishment(
                    id = establishmentId,
                    name = establishmentId,
                    slug = establishmentId,
                    clientIdLabel = "Matrícula",
                    clientIdRequired = false,
                ),
            space = spaceId?.let { PublicSpace(id = it, name = "Mesa $it", type = "mesa") },
        )
}
