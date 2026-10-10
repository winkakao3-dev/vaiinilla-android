package com.vaiinilla.app

import androidx.test.core.app.ApplicationProvider
import com.vaiinilla.app.GuestReservationTest.FakeApi
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.security.PickupTokenStore
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.guest.GuestIdentityRequiredException
import com.vaiinilla.app.data.guest.GuestOrderGateway
import com.vaiinilla.app.data.guest.GuestReservationSession
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.PaymentMethod
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GuestOrderGatewayTest {
    private lateinit var store: GuestSessionStore

    @Before
    fun setUp() {
        store = GuestSessionStore(ApplicationProvider.getApplicationContext(), InMemoryGuestKeyVault())
        store.clearGuest()
    }

    @Test
    fun listSendsKeyInBodyWithoutAnyBearer() {
        store.saveGuest(name = "Ana", key = "llave-1")
        val api = FakeApi { _, _, _, _ -> Result.failure(ApiClientException("UNAVAILABLE", "reintenta", 503)) }

        gateway(api).list("demo")

        val call = api.callsTo("publico/invitados/pedidos").single()
        assertNull(call.bearer)
        assertTrue(call.body.orEmpty().contains("\"llave\":\"llave-1\""))
        assertTrue(call.body.orEmpty().contains("\"establecimiento_slug\":\"demo\""))
    }

    @Test
    fun listWithoutIdentityFailsWithoutCallingTheApi() {
        val api = FakeApi { _, _, _, _ -> error("no debe llamarse") }

        val result = gateway(api).list("demo")

        assertTrue(result.exceptionOrNull() is GuestIdentityRequiredException)
        assertTrue(api.callsTo("publico/invitados/pedidos").isEmpty())
    }

    @Test
    fun createRetriesOnceWithRenewedGuestSessionAndSameIdempotencyKey() {
        store.saveGuest(name = "Ana", key = "llave-1")
        var renewals = 0
        val api =
            FakeApi { path, bearer, _, _ ->
                when {
                    path == "publico/invitados/sesiones" -> {
                        renewals++
                        Result.success(sessionJson("jwt-$renewals"))
                    }
                    path == "pedidos" && bearer == "jwt-1" ->
                        Result.failure(ApiClientException("UNAUTHENTICATED", "vencida", 401))
                    path == "pedidos" ->
                        Result.failure(ApiClientException("INVALID", "rechazado", 422))
                    else -> Result.failure(IllegalStateException("ruta inesperada: $path"))
                }
            }

        val result = gateway(api).create("demo", request(), idempotencyKey = "key-1")

        val calls = api.callsTo("pedidos")
        assertEquals(listOf("jwt-1", "jwt-2"), calls.map { it.bearer })
        assertEquals(listOf("key-1", "key-1"), calls.map { it.headers["Idempotency-Key"] })
        assertEquals("INVALID", (result.exceptionOrNull() as OrderRepositoryException).code)
    }

    @Test
    fun claimSendsFirebaseTokenAndForgetsKeyOnlyWhenServerAccepts() {
        store.saveGuest(name = "Ana", key = "llave-1")
        val api =
            FakeApi { path, _, _, _ ->
                if (path == "invitados/reclamar") Result.success("{}") else Result.failure(IllegalStateException(path))
            }

        val result = gateway(api).claim("firebase-id")

        assertTrue(result.isSuccess)
        val call = api.callsTo("invitados/reclamar").single()
        assertEquals("firebase-id", call.bearer)
        assertTrue(call.body.orEmpty().contains("\"llave\":\"llave-1\""))
        assertNull(store.readGuestKey())
    }

    @Test
    fun claimKeepsKeyWhenServerFailsSoItCanRetry() {
        store.saveGuest(name = "Ana", key = "llave-1")
        val api = FakeApi { _, _, _, _ -> Result.failure(ApiClientException("UNAVAILABLE", "reintenta", 503)) }

        val result = gateway(api).claim("firebase-id")

        assertTrue(result.isFailure)
        assertEquals("llave-1", store.readGuestKey())
    }

    @Test
    fun claimWithoutGuestKeyIsANoop() {
        val api = FakeApi { _, _, _, _ -> error("no debe llamarse") }

        assertTrue(gateway(api).claim("firebase-id").isSuccess)
        assertTrue(api.callsTo("invitados/reclamar").isEmpty())
    }

    private fun gateway(api: FakeApi) =
        GuestOrderGateway(
            apiClient = api,
            guestSession = GuestReservationSession(api, store),
            store = store,
            orderContractJson = OrderContractJson(),
            pickupTokenStore = NoPickupTokens,
            parser = ContractResponseParser(),
        )

    private fun request() =
        CreateOrderRequest(
            paymentMethod = PaymentMethod.CASH,
            destination = OrderDestination.TAKE_AWAY,
            spaceId = null,
            kitchenNotes = "",
            items = listOf(CreateOrderItem(productId = 1, quantity = 1, optionIds = emptyList())),
        )

    private fun sessionJson(token: String) =
        """{"data":{"access_token":"$token","token_type":"bearer","expires_in":900}}"""

    private object NoPickupTokens : PickupTokenStore {
        override fun save(
            orderId: String,
            pickupToken: String,
        ) = Unit

        override fun read(orderId: String): String? = null
    }
}
