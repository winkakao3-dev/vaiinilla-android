package com.vaiinilla.app.ui.operational

import com.vaiinilla.app.core.network.ApiFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueuePresentationTest {
    private val offline = QueueSync.Failed(ApiFailure.NoConnection)
    private val serverDown = QueueSync.Failed(ApiFailure.ServerError(503, "UNAVAILABLE", "Servidor ocupado."))

    @Test
    fun `empty queue says up to date only after a successful refresh`() {
        assertEquals(QueuePresentation.Empty, queuePresentation(hasOrders = false, queue = QueueSync.Live))
    }

    @Test
    fun `empty queue while the first refresh runs is loading, not up to date`() {
        assertEquals(QueuePresentation.Loading, queuePresentation(hasOrders = false, queue = QueueSync.Pending))
    }

    @Test
    fun `empty queue after a failed refresh is an error with retry`() {
        assertEquals(QueuePresentation.Error, queuePresentation(hasOrders = false, queue = offline))
        assertEquals(QueuePresentation.Error, queuePresentation(hasOrders = false, queue = serverDown))
    }

    @Test
    fun `orders already on screen stay visible when a refresh fails`() {
        assertEquals(QueuePresentation.Content, queuePresentation(hasOrders = true, queue = offline))
        assertEquals(QueuePresentation.Content, queuePresentation(hasOrders = true, queue = QueueSync.Pending))
    }

    @Test
    fun `only a network cut is called no internet, a server failure is not`() {
        assertTrue(offline.lostConnection)
        assertTrue(QueueSync.Failed(ApiFailure.Timeout).lostConnection)
        assertTrue(QueueSync.Failed(ApiFailure.ServerUnreachable).lostConnection)
        assertFalse(serverDown.lostConnection)
        assertFalse(QueueSync.Failed(ApiFailure.BadResponse).lostConnection)
        assertFalse(QueueSync.Live.lostConnection)
        assertFalse(QueueSync.Pending.lostConnection)
    }

    @Test
    fun `failure text comes from the mapper and falls back only when nothing failed`() {
        assertEquals("Servidor ocupado.", serverDown.failureText("respaldo"))
        assertEquals(ApiFailure.NoConnection.text, offline.failureText("respaldo"))
        assertEquals("respaldo", QueueSync.Live.failureText("respaldo"))
    }
}
