package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.model.Venue
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaneRentalArtTest {
    @After
    fun resetVenue() {
        Venue.update(null)
    }

    @Test
    fun `in a bowling alley the rental draws the lane, not the padel court`() {
        Venue.update(Venue.BOWLING)
        assertTrue(isLaneRental("Pista 1"))
        assertTrue(isLaneRental(null))
    }

    @Test
    fun `a padel court keeps the court art`() {
        Venue.update("club")
        assertFalse(isLaneRental("Cancha 2"))
        assertFalse(isLaneRental(null))
    }

    @Test
    fun `a lane named like one draws the lane even before the store type loads`() {
        Venue.update(null)
        assertTrue(isLaneRental("Boliche 3"))
        assertTrue(isLaneRental("Carril de bolos"))
    }
}
