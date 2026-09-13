package com.vaiinilla.app.data.wallet

import com.vaiinilla.app.core.security.SecureSessionStore
import com.vaiinilla.app.domain.repository.WalletRepositoryException
import com.vaiinilla.app.ui.operational.isDefinitiveWalletReloadFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PendingWalletReloadStoreTest {
    private val sessionStore = FakeSessionStore()
    private val store =
        PendingWalletReloadStore(
            context = RuntimeEnvironment.getApplication(),
            sessionStore = sessionStore,
        )

    @Test
    fun `write and read roundtrip under the same context`() {
        sessionStore.token = fakeJwt(uid = "cajero-1")
        store.write(PendingWalletReload("u1", "100.00", "key-1", storedAtEpochMs = 7L))

        val pending = store.read()

        assertEquals("u1", pending?.userId)
        assertEquals("100.00", pending?.amount)
        assertEquals("key-1", pending?.idempotencyKey)
        assertEquals(7L, pending?.storedAtEpochMs)
    }

    @Test
    fun `pending reload is invisible to another operator context`() {
        sessionStore.token = fakeJwt(uid = "cajero-1")
        store.write(PendingWalletReload("u1", "100.00", "key-1"))

        sessionStore.token = fakeJwt(uid = "cajero-2")

        assertNull(store.read())
    }

    @Test
    fun `pending reload survives token rotation with same claims`() {
        sessionStore.token = fakeJwt(uid = "cajero-1", seed = "a")
        store.write(PendingWalletReload("u1", "100.00", "key-1"))

        sessionStore.token = fakeJwt(uid = "cajero-1", seed = "b")

        assertEquals("key-1", store.read()?.idempotencyKey)
    }

    @Test
    fun `clear removes only the current context record`() {
        sessionStore.token = fakeJwt(uid = "cajero-1")
        store.write(PendingWalletReload("u1", "100.00", "key-1"))
        store.clear()

        assertNull(store.read())
    }

    @Test
    fun `without session there is no pending reload and writes are ignored`() {
        sessionStore.token = null

        assertNull(store.read())
        store.write(PendingWalletReload("u1", "100.00", "key-1"))
        assertNull(store.read())
    }

    @Test
    fun `server rejection clears the pending record but network errors keep it`() {
        assertTrue(
            isDefinitiveWalletReloadFailure(
                WalletRepositoryException("VALIDACION", "rechazada", httpStatus = 422),
            ),
        )
        assertFalse(
            isDefinitiveWalletReloadFailure(
                WalletRepositoryException("INTERNO", "fallo interno", httpStatus = 500),
            ),
        )
        assertFalse(isDefinitiveWalletReloadFailure(IOException("timeout")))
        assertFalse(isDefinitiveWalletReloadFailure(IllegalStateException("desconocido")))
    }

    private fun fakeJwt(
        uid: String,
        role: String = "cajero",
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
