package com.vaiinilla.app.data.order

import android.content.Context
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
    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    private lateinit var store: DismissedClientOrdersStore

    @Before
    fun setUp() {
        clearAllStores()
        store = DismissedClientOrdersStore(context)
    }

    @After
    fun tearDown() {
        clearAllStores()
    }

    @Test
    fun `dismissed order survives store recreation`() {
        store.dismiss("order-123")

        val recreated = DismissedClientOrdersStore(context)

        assertTrue(recreated.isDismissed("order-123"))
        assertEquals(setOf("order-123"), recreated.read())
    }

    @Test
    fun `blank order id is ignored`() {
        store.dismiss("   ")

        assertFalse(store.isDismissed("   "))
        assertTrue(store.read().isEmpty())
    }

    @Test
    fun `legacy immediate swipe dismissals are not carried into confirmation store`() {
        context
            .getSharedPreferences(DismissedClientOrdersStore.LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(DismissedClientOrdersStore.KEY_DISMISSED_ORDER_IDS, setOf("old-order-1", "old-order-2"))
            .commit()

        val migrated = DismissedClientOrdersStore(context)

        assertTrue(migrated.read().isEmpty())
        assertFalse(
            context
                .getSharedPreferences(DismissedClientOrdersStore.LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .contains(DismissedClientOrdersStore.KEY_DISMISSED_ORDER_IDS),
        )
    }

    @Test
    fun `confirmed v2 dismissal remains after legacy migration runs again`() {
        store.dismiss("confirmed-order")
        context
            .getSharedPreferences(DismissedClientOrdersStore.LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(DismissedClientOrdersStore.KEY_DISMISSED_ORDER_IDS, setOf("accidental-old-order"))
            .commit()

        val recreated = DismissedClientOrdersStore(context)

        assertEquals(setOf("confirmed-order"), recreated.read())
    }

    private fun clearAllStores() {
        context
            .getSharedPreferences(DismissedClientOrdersStore.LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context
            .getSharedPreferences(DismissedClientOrdersStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
