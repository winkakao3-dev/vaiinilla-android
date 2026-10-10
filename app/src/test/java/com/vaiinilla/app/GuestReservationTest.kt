package com.vaiinilla.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.guest.GuestIdentityRequiredException
import com.vaiinilla.app.data.guest.GuestReservationSession
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.data.reservations.RemoteReservationRepository
import com.vaiinilla.app.data.reservations.ReservationContractJson
import com.vaiinilla.app.domain.model.ReservationAccess
import com.vaiinilla.app.domain.repository.ReservationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GuestReservationTest {
    private lateinit var store: GuestSessionStore

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        store = GuestSessionStore(context, InMemoryGuestKeyVault())
        store.clearGuest()
    }

    @Test
    fun legacyPlaintextKeyMovesToVaultOnce() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("vaiinilla_guest_session", Context.MODE_PRIVATE)
        prefs.edit().putString("guest_key", "llave-vieja").commit()

        assertEquals("llave-vieja", store.readGuestKey())
        assertNull(prefs.getString("guest_key", null))
        assertEquals("llave-vieja", store.readGuestKey())
    }

    @Test
    fun enterStoresIdentityAndUsesCachedSessionWithoutRenewing() {
        val api = FakeApi { path, _, _, _ -> handle(path) }
        val session = GuestReservationSession(api, store)

        session.enter("demo", "Ana").getOrThrow()

        assertTrue(session.hasIdentity())
        assertEquals("Ana", session.storedName())
        assertEquals("jwt-1", session.bearer("demo").getOrThrow())
        assertEquals(0, api.callsTo("publico/invitados/sesiones").size)
        val create = api.callsTo("publico/invitados").single()
        assertTrue(create.body.orEmpty().contains("\"terminos_version\":\"2026-10\""))
        assertTrue(create.headers.containsKey("Idempotency-Key"))
    }

    @Test
    fun bearerRenewsWithStoredKeyWhenNoSessionIsCached() {
        store.saveGuest(name = "Ana", key = "llave-1")
        val api = FakeApi { path, _, _, _ -> handle(path) }
        val session = GuestReservationSession(api, store)

        assertEquals("jwt-renewed", session.bearer("demo").getOrThrow())
        assertTrue(api.callsTo("publico/invitados/sesiones").single().body.orEmpty().contains("\"llave\":\"llave-1\""))
    }

    @Test
    fun rejectedKeyForgetsIdentityAndAsksToEnterAgain() {
        store.saveGuest(name = "Ana", key = "llave-vencida")
        val api =
            FakeApi { path, _, _, _ ->
                if (path == "publico/invitados/sesiones") {
                    Result.failure(ApiClientException("UNAUTHENTICATED", "llave inválida", 401))
                } else {
                    handle(path)
                }
            }
        val session = GuestReservationSession(api, store)

        val result = session.bearer("demo")

        assertTrue(result.exceptionOrNull() is GuestIdentityRequiredException)
        assertFalse(session.hasIdentity())
        assertNull(store.readGuestKey())
    }

    @Test
    fun transientRenewalFailureKeepsIdentity() {
        store.saveGuest(name = "Ana", key = "llave-1")
        val api =
            FakeApi { path, _, _, _ ->
                if (path == "publico/invitados/sesiones") {
                    Result.failure(ApiClientException("UNAVAILABLE", "reintenta", 503))
                } else {
                    handle(path)
                }
            }
        val session = GuestReservationSession(api, store)

        val result = session.bearer("demo")

        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull() is GuestIdentityRequiredException)
        assertTrue(session.hasIdentity())
        assertEquals("llave-1", store.readGuestKey())
    }

    @Test
    fun guestUnauthorizedRetriesOnceWithNewSessionAndSameIdempotencyKey() {
        store.saveGuest(name = "Ana", key = "llave-1")
        var renewals = 0
        val api =
            FakeApi { path, bearer, _, _ ->
                when {
                    path == "publico/invitados/sesiones" -> {
                        renewals++
                        Result.success(sessionJson("jwt-$renewals"))
                    }
                    path == "reservas" && bearer == "jwt-1" ->
                        Result.failure(ApiClientException("UNAUTHENTICATED", "vencida", 401))
                    path == "reservas" ->
                        Result.failure(ApiClientException("SLOT_TAKEN", "ocupada", 409))
                    else -> handle(path)
                }
            }
        val repository = repository(api)

        val result =
            repository.create(
                access = ReservationAccess.Guest("demo"),
                courtId = 7,
                start = Instant.parse("2026-10-10T18:00:00Z"),
                durationMinutes = 60,
                customerName = null,
                idempotencyKey = "key-1",
            )

        val reservaCalls = api.callsTo("reservas")
        assertEquals(listOf("jwt-1", "jwt-2"), reservaCalls.map { it.bearer })
        assertEquals(listOf("key-1", "key-1"), reservaCalls.map { it.headers["Idempotency-Key"] })
        assertEquals("SLOT_TAKEN", (result.exceptionOrNull() as ReservationException).code)
    }

    private fun repository(api: FakeApi) =
        RemoteReservationRepository(
            apiClient = api,
            contractJson = ReservationContractJson(OrderContractJson()),
            guestSession = GuestReservationSession(api, store),
        )

    private fun handle(path: String): Result<String> =
        when (path) {
            "publico/legal/vigente" -> Result.success(LEGAL_JSON)
            "publico/invitados" -> Result.success(GUEST_JSON)
            "publico/invitados/sesiones" -> Result.success(sessionJson("jwt-renewed"))
            else -> Result.failure(IllegalStateException("ruta inesperada: $path"))
        }

    private fun sessionJson(token: String) =
        """{"data":{"access_token":"$token","token_type":"bearer","expires_in":900}}"""

    internal class FakeApi(
        private val handler:
            (path: String, bearer: String?, body: String?, headers: Map<String, String>) -> Result<String>,
    ) : VaiinillaApiClient {
        override val baseUrl = "https://api.test"
        private val calls = mutableListOf<Call>()

        fun callsTo(path: String): List<Call> = calls.filter { it.path == path }

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> = record(path, null, null, emptyMap())

        override fun getPublic(
            path: String,
            query: Map<String, String>,
        ): Result<String> = record(path, null, null, emptyMap())

        override fun getWithBearer(
            bearer: String,
            path: String,
            query: Map<String, String>,
        ): Result<String> = record(path, bearer, null, emptyMap())

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> = record(path, null, body, headers)

        override fun postPublic(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> = record(path, null, body, headers)

        override fun postWithBearer(
            bearer: String,
            path: String,
            body: String?,
            headers: Map<String, String>,
        ): Result<String> = record(path, bearer, body, headers)

        override fun postWithoutBody(
            path: String,
            headers: Map<String, String>,
        ): Result<String> = record(path, null, null, headers)

        private fun record(
            path: String,
            bearer: String?,
            body: String?,
            headers: Map<String, String>,
        ): Result<String> {
            calls += Call(path, bearer, body, headers)
            return handler(path, bearer, body, headers)
        }

        data class Call(
            val path: String,
            val bearer: String?,
            val body: String?,
            val headers: Map<String, String>,
        )
    }

    private companion object {
        const val LEGAL_JSON = """{"data":{"terminos_version":"2026-10","privacidad_version":"2026-10"}}"""
        const val GUEST_JSON =
            """{"data":{"access_token":"jwt-1","token_type":"bearer","expires_in":900,""" +
                """"invitado":{"nombre":"Ana","llave":"llave-1"}}}"""
    }
}
