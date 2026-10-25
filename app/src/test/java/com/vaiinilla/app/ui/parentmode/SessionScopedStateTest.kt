package com.vaiinilla.app.ui.parentmode

import com.vaiinilla.app.core.notifications.ChildDeepLink
import com.vaiinilla.app.core.notifications.ChildDeepLinkStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionScopedStateTest {
    @Test
    fun `resetting the children state forgets the previous guardian's kids`() =
        runBlocking {
            val state = ChildrenState(ParentFixtures.services(FakeGuardianship(listOf(ParentFixtures.ana))))
            state.load()
            assertTrue(state.hasLoaded)
            assertEquals(1, state.children.size)

            state.reset()

            assertEquals(emptyList<Any>(), state.children)
            assertFalse(state.hasLoaded)
            assertNull(state.error)
        }

    @Test
    fun `resetting the deep link store drops pending and requested child links`() {
        val link = ChildDeepLink("u-ana", "est-1")
        ChildDeepLinkStore.request(link)
        ChildDeepLinkStore.handToWallet(link)
        ChildDeepLinkStore.request(link)

        ChildDeepLinkStore.reset()

        assertNull(ChildDeepLinkStore.pending.value)
        assertNull(ChildDeepLinkStore.requestedChild.value)
    }
}
