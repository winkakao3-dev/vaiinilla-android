package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.VenueContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaneRentalArtTest {
    @Test
    fun `in a bowling alley the rental draws the lane, not the padel court`() {
        val venue = VenueContext.of("boliche")
        assertTrue(isLaneRental(venue, "Pista 1"))
        assertTrue(isLaneRental(venue, null))
    }

    @Test
    fun `a padel court keeps the court art`() {
        val venue = VenueContext.of("club")
        assertFalse(isLaneRental(venue, "Cancha 2"))
        assertFalse(isLaneRental(venue, null))
    }

    @Test
    fun `a lane named like one draws the lane even before the store type loads`() {
        val venue = VenueContext.STANDARD
        assertTrue(isLaneRental(venue, "Boliche 3"))
        assertTrue(isLaneRental(venue, "Carril de bolos"))
    }
}
