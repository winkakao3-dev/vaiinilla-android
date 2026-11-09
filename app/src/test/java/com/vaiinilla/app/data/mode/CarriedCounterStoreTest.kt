package com.vaiinilla.app.data.mode

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CarriedCounterStoreTest {
    private fun store() = CarriedCounterStore(ApplicationProvider.getApplicationContext())

    @Test
    fun `la marca sobrevive a abrir otra instancia y se puede desmarcar`() {
        store().setCarried("pedido-1", true)
        assertTrue(store().isCarried("pedido-1"))
        assertFalse(store().isCarried("pedido-2"))
        store().setCarried("pedido-1", false)
        assertFalse(store().isCarried("pedido-1"))
    }

    @Test
    fun `solo guarda los ultimos pedidos marcados`() {
        val s = store()
        repeat(CarriedCounterStore.MAX_ORDERS + 5) { s.setCarried("p$it", true) }
        assertFalse(s.isCarried("p0"))
        assertTrue(s.isCarried("p${CarriedCounterStore.MAX_ORDERS + 4}"))
    }
}
