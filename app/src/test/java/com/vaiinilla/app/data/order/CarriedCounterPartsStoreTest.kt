package com.vaiinilla.app.data.order

import android.content.Context
import org.junit.After
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
class CarriedCounterPartsStoreTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    private var clock = 1_000_000_000L

    @Before
    fun setUp() = clear()

    @After
    fun tearDown() = clear()

    @Test
    fun `ya lo lleve sigue marcado al volver a abrir la cuenta`() {
        CarriedCounterPartsStore(context) { clock }.setCarried("order-7", true)

        val reopened = CarriedCounterPartsStore(context) { clock }

        assertTrue(reopened.isCarried("order-7"))
        assertFalse(reopened.isCarried("order-8"))
    }

    @Test
    fun `desmarcar quita la marca`() {
        val store = CarriedCounterPartsStore(context) { clock }
        store.setCarried("order-7", true)
        store.setCarried("order-7", false)

        assertFalse(CarriedCounterPartsStore(context) { clock }.isCarried("order-7"))
    }

    @Test
    fun `la marca de hace mas de un dia se tira`() {
        CarriedCounterPartsStore(context) { clock }.setCarried("order-7", true)
        clock += CarriedCounterPartsStore.MAX_AGE_MILLIS + 1

        assertFalse(CarriedCounterPartsStore(context) { clock }.isCarried("order-7"))
    }

    @Test
    fun `id vacio no se guarda`() {
        val store = CarriedCounterPartsStore(context) { clock }
        store.setCarried("", true)

        assertFalse(store.isCarried(""))
    }

    private fun clear() {
        context
            .getSharedPreferences(CarriedCounterPartsStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
