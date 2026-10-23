package com.vaiinilla.app.domain.guardianship

import com.vaiinilla.app.domain.model.PreparationStation
import com.vaiinilla.app.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/** B10: lo que el negocio apagó sale en el menú como Agotado y no se agrega, con o sin tutor. */
class SoldOutRestrictionTest {
    private val controls =
        MyGuardianControls(
            tutorNames = listOf("Kao"),
            dailyLimit = BigDecimal("100"),
            availableToday = BigDecimal("80"),
            blockedProductIds = setOf(2),
            allergenProductIds = emptySet(),
            allergens = emptyList(),
        )

    @Test
    fun `a product turned off is sold out for everyone`() {
        val off = product(id = 3, available = false)
        assertEquals(ChildRestriction.SoldOut, null.restrictionFor(off))
        assertEquals(ChildRestriction.SoldOut, controls.restrictionFor(off))
        assertEquals("Agotado", ChildRestriction.SoldOut.label)
    }

    @Test
    fun `sold out wins over the tutor block`() {
        assertEquals(ChildRestriction.SoldOut, controls.restrictionFor(product(id = 2, available = false)))
        assertEquals(ChildRestriction.Blocked, controls.restrictionFor(product(id = 2, available = true)))
    }

    @Test
    fun `an available product without controls is free to add`() {
        assertNull(null.restrictionFor(product(id = 3, available = true)))
    }

    private fun product(
        id: Int,
        available: Boolean,
    ) = Product(
        id = id,
        categoryId = 1,
        preparationStation = PreparationStation.entries.first(),
        name = "Chocolate frío",
        description = "",
        ingredients = "",
        allergens = "",
        estimatedTimeMinutes = 5,
        counterPrice = "20.00",
        digitalPrice = "20.00",
        available = available,
        imageUrl = "",
        optionGroups = emptyList(),
    )
}
