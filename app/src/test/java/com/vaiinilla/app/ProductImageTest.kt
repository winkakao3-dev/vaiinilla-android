package com.vaiinilla.app

import com.vaiinilla.app.ui.components.productImageIsRemote
import com.vaiinilla.app.ui.components.productImageResource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductImageTest {
    @Test
    fun `supabase catalog urls are remote`() {
        assertTrue(
            productImageIsRemote(
                "https://lyhidclkdpfgxnabevia.supabase.co/storage/v1/object/public/catalogo-productos/x.jpg",
            ),
        )
        assertFalse(productImageIsRemote("waffle"))
        assertFalse(productImageIsRemote("fixture://jamaica"))
    }

    @Test
    fun `unknown catalog keys do not fall back to waffle`() {
        assertNull(productImageResource("https://cdn.example/producto.jpg"))
        assertNull(productImageResource("unknown-sku"))
    }
}
