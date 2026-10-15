package com.vaiinilla.app.ui.operational

import org.junit.Assert.assertEquals
import org.junit.Test

class QueuePresentationTest {
    @Test
    fun `empty queue says up to date only after a successful refresh`() {
        assertEquals(QueuePresentation.Empty, queuePresentation(hasOrders = false, lastRefreshOk = true))
    }

    @Test
    fun `empty queue while the first refresh runs is loading, not up to date`() {
        assertEquals(QueuePresentation.Loading, queuePresentation(hasOrders = false, lastRefreshOk = null))
    }

    @Test
    fun `empty queue after a failed refresh is an error with retry`() {
        assertEquals(QueuePresentation.Error, queuePresentation(hasOrders = false, lastRefreshOk = false))
    }

    @Test
    fun `orders already on screen stay visible when a refresh fails`() {
        assertEquals(QueuePresentation.Content, queuePresentation(hasOrders = true, lastRefreshOk = false))
        assertEquals(QueuePresentation.Content, queuePresentation(hasOrders = true, lastRefreshOk = null))
    }
}
