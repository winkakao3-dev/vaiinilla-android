package com.vaiinilla.app.domain.guardianship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/** F11: la Cartera del hijo dice su límite por día y lo que le queda hoy, con el texto de iOS. */
class StudentWalletControlsTest {
    private fun controls(
        limit: String? = null,
        available: String? = null,
        blocked: Set<Int> = emptySet(),
        allergen: Set<Int> = emptySet(),
    ) = MyGuardianControls(
        tutorNames = listOf("Marta"),
        dailyLimit = limit?.let(::BigDecimal),
        availableToday = available?.let(::BigDecimal),
        blockedProductIds = blocked,
        allergenProductIds = allergen,
        allergens = emptyList(),
    )

    @Test
    fun conLimiteDiceCuantoLeQuedaHoy() {
        assertEquals(
            "Límite de \$100 al día; hoy te quedan \$40.50.",
            controls(limit = "100.00", available = "40.50").walletDetail,
        )
    }

    @Test
    fun juntaElLimiteConLoQueNoPuedePedir() {
        assertEquals(
            "Límite de \$80 al día; hoy te quedan \$80. 2 productos no están a tu alcance.",
            controls(limit = "80.00", available = "80.00", blocked = setOf(1), allergen = setOf(1, 2)).walletDetail,
        )
        assertEquals("1 producto no está a tu alcance.", controls(blocked = setOf(3)).walletDetail)
    }

    @Test
    fun sinLimiteNiBloqueosNoHayDetalle() {
        assertNull(controls().walletDetail)
    }
}
