package com.vaiinilla.app.data.cache

import com.vaiinilla.app.core.security.SecureSessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RemoteResponseCacheTest {
    private val sessionStore = FakeSessionStore()
    private val cache =
        RemoteResponseCache(
            context = RuntimeEnvironment.getApplication(),
            sessionStore = sessionStore,
        )

    @Test
    fun `write then read returns the cached body`() {
        sessionStore.token = fakeJwt(uid = "u1")
        cache.write("wallets/me", emptyMap(), """{"saldo":"10.00"}""")

        assertEquals("""{"saldo":"10.00"}""", cache.read("wallets/me"))
    }

    @Test
    fun `query params participate in the key regardless of order`() {
        sessionStore.token = fakeJwt(uid = "u1")
        cache.write("pedidos", mapOf("b" to "2", "a" to "1"), "body")

        assertEquals("body", cache.read("pedidos", mapOf("a" to "1", "b" to "2")))
        assertNull(cache.read("pedidos", mapOf("a" to "1")))
    }

    @Test
    fun `entries are isolated between account contexts`() {
        sessionStore.token = fakeJwt(uid = "u1")
        cache.write("pedidos", emptyMap(), "pedidos-u1")

        sessionStore.token = fakeJwt(uid = "u2")

        assertNull(cache.read("pedidos"))
    }

    @Test
    fun `entries are isolated between roles of the same account`() {
        sessionStore.token = fakeJwt(uid = "u1", role = "cliente", mem = "m-cli")
        cache.write("pedidos", emptyMap(), "pedidos-cliente")

        sessionStore.token = fakeJwt(uid = "u1", role = "cajero", mem = "m-caja")

        assertNull(cache.read("pedidos"))
    }

    @Test
    fun `same claims keep the cache across token refreshes`() {
        sessionStore.token = fakeJwt(uid = "u1", seed = "a")
        cache.write("pedidos", emptyMap(), "pedidos-u1")

        sessionStore.token = fakeJwt(uid = "u1", seed = "b")

        assertEquals("pedidos-u1", cache.read("pedidos"))
    }

    @Test
    fun `without session nothing is cached or read`() {
        sessionStore.token = null

        cache.write("pedidos", emptyMap(), "body")
        assertNull(cache.read("pedidos"))
    }

    private fun fakeJwt(
        uid: String,
        role: String = "cliente",
        est: String = "e1",
        mem: String = "m1",
        seed: String = "x",
    ): String {
        val payload =
            """{"uid":"$uid","rol":"$role","establecimiento_id":"$est","membresia_id":"$mem","nonce":"$seed"}"""
        val b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        return "h.$b64.s"
    }

    private class FakeSessionStore : SecureSessionStore {
        var token: String? = null

        override fun saveAccessToken(token: String) {
            this.token = token
        }

        override fun readAccessToken(): String? = token

        override fun clear() {
            token = null
        }
    }
}
