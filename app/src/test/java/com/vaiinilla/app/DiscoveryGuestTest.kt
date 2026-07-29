package com.vaiinilla.app

import android.net.Uri
import com.vaiinilla.app.data.discovery.FixtureDiscoveryRepository
import com.vaiinilla.app.data.fixture.ContractFixtureParser
import com.vaiinilla.app.data.guest.GuestCartLineSnapshot
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.model.CartLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

class FixtureDiscoveryRepositoryTest {
    private val repository =
        FixtureDiscoveryRepository(TestFixtureSource(), ContractFixtureParser())

    @Test
    fun `search returns sorted establishments and filters by name`() {
        val all = repository.searchEstablishments(query = "").getOrThrow().first
        assertEquals(2, all.size)
        assertEquals("Cafetería Centro", all.first().name)

        val filtered = repository.searchEstablishments(query = "norte").getOrThrow().first
        assertEquals(1, filtered.size)
        assertEquals("cafeteria-norte", filtered.single().slug)
    }

    @Test
    fun `resolve mesa4 returns establishment and space`() {
        val resolved = repository.resolveSpaceToken("mesa4").getOrThrow()
        assertEquals("cafeteria-centro", resolved.establishment.slug)
        assertEquals(12, resolved.space?.id)
        assertEquals("Mesa 4", resolved.space?.name)
    }

    @Test
    fun `guest catalog reuses entrega 01 fixture for known slug`() {
        val catalog = repository.getGuestCatalog("cafeteria-centro").getOrThrow()
        assertEquals(3, catalog.products.size)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuestSessionAndDeepLinkTest {
    @Test
    fun `parses general QR slug from app link`() {
        val uri = Uri.parse("https://vaiinilla.app/e/cafeteria-centro")
        assertEquals("cafeteria-centro", MainActivity.establishmentSlugFrom(uri))
        assertEquals("cafeteria-centro", MainActivity.establishmentSlugFrom(Uri.parse("https://www.vaiinilla.app/e/cafeteria-centro")))
        assertNull(MainActivity.establishmentSlugFrom(Uri.parse("https://vaiinilla.app/other")))
    }

    @Test
    fun `cart keys isolate tenants and restore drops unknown products`() {
        val store = GuestSessionStore(RuntimeEnvironment.getApplication())
        assertEquals("est-a:12", store.cartStorageKey("est-a", 12))
        assertEquals("est-a:none", store.cartStorageKey("est-a", null))

        val catalog =
            FixtureDiscoveryRepository(TestFixtureSource(), ContractFixtureParser())
                .getGuestCatalog("cafeteria-centro")
                .getOrThrow()
        val product = catalog.products.first()
        val keyA = store.cartStorageKey("est-a", 12)
        val keyB = store.cartStorageKey("est-b", null)
        store.saveCartSnapshot(
            keyA,
            listOf(
                CartLine(product = product, quantity = 2, selectedOptionIds = emptySet()),
            ),
        )
        store.saveCartSnapshot(keyB, emptyList())

        assertEquals(1, store.readCartSnapshot(keyA).size)
        assertTrue(store.readCartSnapshot(keyB).isEmpty())

        val restored =
            store.restoreCartLines(
                listOf(
                    GuestCartLineSnapshot(productId = product.id, quantity = 2, selectedOptionIds = emptyList()),
                    GuestCartLineSnapshot(productId = 999_999, quantity = 1, selectedOptionIds = emptyList()),
                ),
                catalog.products,
            )
        assertEquals(1, restored.size)
        assertEquals(product.id, restored.single().product.id)
        assertEquals(2, restored.single().quantity)
    }
}
