package com.vaiinilla.app.data.order

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DismissedClientOrdersStoreTest {
    private lateinit var store: DismissedClientOrdersStore

    @Before
    fun setUp() {
        store = DismissedClientOrdersStore(RuntimeEnvironment.getApplication())
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun `dismissed order survives store recreation`() {
        store.dismiss("order-123")

        val recreated = DismissedClientOrdersStore(RuntimeEnvironment.getApplication())

        assertTrue(recreated.isDismissed("order-123"))
        assertEquals(setOf("order-123"), recreated.read())
    }

    @Test
    fun `blank order id is ignored`() {
        store.dismiss("   ")

        assertFalse(store.isDismissed("   "))
        assertTrue(store.read().isEmpty())
    }
}
