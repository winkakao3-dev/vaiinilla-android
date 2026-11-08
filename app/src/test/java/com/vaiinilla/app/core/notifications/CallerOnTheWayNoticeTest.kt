package com.vaiinilla.app.core.notifications

import androidx.test.core.app.ApplicationProvider
import com.vaiinilla.app.domain.model.VenueContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** En un hotel quien atiende la llamada es recepción: el aviso al huésped no habla de un mesero. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CallerOnTheWayNoticeTest {
    @Test
    fun `in a hotel reception is on the way and elsewhere the waiter`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val hotel = OrderAdvanceNotifier.notifyCallerOnTheWay(context, VenueContext.of("hotel"), "c1")
        assertEquals("Ya vienen a tu habitación", hotel.title)
        assertEquals("Recepción tomó tu llamada.", hotel.body)
        val bar = OrderAdvanceNotifier.notifyCallerOnTheWay(context, VenueContext.STANDARD, "c2")
        assertEquals("Tu mesero va en camino", bar.title)
    }
}