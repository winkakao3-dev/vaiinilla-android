package com.vaiinilla.app

import android.net.Uri
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.discovery.FixtureDiscoveryRepository
import com.vaiinilla.app.data.guest.GuestCartLineSnapshot
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.discovery.DiscoveryFailures
import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.GuestVenueContext
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.domain.model.PublicSpace
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
        FixtureDiscoveryRepository(TestFixtureSource(), ContractResponseParser())

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

    @Test
    fun `suspended slug returns establishment suspended error`() {
        val failure = repository.getEstablishment("cafeteria-suspendida").exceptionOrNull()
        assertTrue(DiscoveryFailures.isEstablishmentSuspended(failure))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuestSessionAndDeepLinkTest {
    private val centro =
        PublicEstablishment(
            id = "8246ff44-aad0-4e49-9268-b71c997893fe",
            name = "Cafetería Centro",
            slug = "cafeteria-centro",
            clientIdLabel = "Matrícula",
            clientIdRequired = true,
        )

    @Test
    fun `parses general QR slug from app link`() {
        val uri = Uri.parse("https://vaiinilla.app/e/cafeteria-centro")
        assertEquals("cafeteria-centro", MainActivity.establishmentSlugFrom(uri))
        assertEquals(
            "cafeteria-centro",
            MainActivity.establishmentSlugFrom(Uri.parse("https://www.vaiinilla.app/e/cafeteria-centro")),
        )
        assertNull(MainActivity.establishmentSlugFrom(Uri.parse("https://vaiinilla.app/other")))
        assertNull(MainActivity.establishmentSlugFrom(Uri.parse("http://vaiinilla.app/e/cafeteria-centro")))
        assertNull(MainActivity.establishmentSlugFrom(Uri.parse("https://vaiinilla.app/e/cafeteria-centro/extra")))
        assertNull(MainActivity.establishmentSlugFrom(Uri.parse("https://user@vaiinilla.app/e/cafeteria-centro")))
    }

    @Test
    fun `invitation links require canonical https host path and bounded token`() {
        assertEquals(
            "invite-123",
            MainActivity.invitationTokenFrom(
                Uri.parse("https://vaiinilla.app/invitaciones/aceptar?token=invite-123"),
            ),
        )
        assertNull(
            MainActivity.invitationTokenFrom(
                Uri.parse("http://vaiinilla.app/invitaciones/aceptar?token=invite-123"),
            ),
        )
        assertNull(
            MainActivity.invitationTokenFrom(
                Uri.parse("https://vaiinilla.app/invitaciones/aceptar/extra?token=invite-123"),
            ),
        )
        val oversized = "a".repeat(4_097)
        assertNull(
            MainActivity.invitationTokenFrom(
                Uri.parse("https://vaiinilla.app/invitaciones/aceptar?token=$oversized"),
            ),
        )
    }

    @Test
    fun `cart keys isolate tenants and restore drops unknown products`() {
        val store = GuestSessionStore(RuntimeEnvironment.getApplication())
        assertEquals("est-a:12", store.cartStorageKey("est-a", 12))
        assertEquals("est-a:none", store.cartStorageKey("est-a", null))

        val catalog =
            FixtureDiscoveryRepository(TestFixtureSource(), ContractResponseParser())
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

    @Test
    fun `venue metadata refresh follows stable id and preserves space`() {
        val store = GuestSessionStore(RuntimeEnvironment.getApplication())
        store.clearAll()
        val oldVenue =
            GuestVenueContext(
                establishment =
                    PublicEstablishment(
                        id = "stable-id",
                        name = "Stripe",
                        slug = "stripe-tienda",
                        clientIdLabel = "Matrícula",
                        clientIdRequired = false,
                    ),
                space = PublicSpace(id = 12, name = "Mesa 4", type = "mesa"),
            )
        store.saveVenue(oldVenue)

        val refreshed =
            store.refreshSelectedVenueMetadata(
                PublicEstablishment(
                    id = "stable-id",
                    name = "VENECIA",
                    slug = "venecia-tienda",
                    clientIdLabel = "Matrícula",
                    clientIdRequired = false,
                ),
            )

        assertEquals("VENECIA", refreshed?.establishment?.name)
        assertEquals("venecia-tienda", refreshed?.establishment?.slug)
        assertEquals(12, refreshed?.space?.id)
        assertEquals(refreshed, store.readVenue())
    }

    @Test
    fun `guest venue and cart survive auth handoff without clearing session`() {
        val store = GuestSessionStore(RuntimeEnvironment.getApplication())
        val venue =
            GuestVenueContext(
                establishment = centro,
                space = PublicSpace(id = 12, name = "Mesa 4", type = "mesa"),
            )
        val catalog =
            FixtureDiscoveryRepository(TestFixtureSource(), ContractResponseParser())
                .getGuestCatalog("cafeteria-centro")
                .getOrThrow()
        val product = catalog.products.first()
        store.saveVenue(venue)
        val key = store.cartStorageKey(venue.establishment.id, venue.space?.id)
        store.saveCartSnapshot(
            key,
            listOf(CartLine(product = product, quantity = 1, selectedOptionIds = emptySet())),
        )

        // Auth must not clear the selected venue; the store should preserve it.
        assertEquals(venue, store.readVenue())
        assertEquals(1, store.readCartSnapshot(key).size)
    }
}
