package com.vaiinilla.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class JwtContextScopeTest {
    private fun jwt(payloadJson: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("{}".toByteArray(Charsets.UTF_8))
        val payload = encoder.encodeToString(payloadJson.toByteArray(Charsets.UTF_8))
        return "$header.$payload.signature"
    }

    private fun contextPayload(
        uid: String? = "uid-1",
        role: String = "caja",
        establishment: String = "est-1",
        membership: String = "mem-1",
        extra: String = "",
    ): String {
        val uidField = uid?.let { "\"uid\":\"$it\"," } ?: ""
        return """{$uidField"rol":"$role","establecimiento_id":"$establishment","membresia_id":"$membership"$extra}"""
    }

    @Test
    fun `stableKey survives token refresh with same context claims`() {
        val first = jwt(contextPayload(extra = ",\"exp\":1"))
        val refreshed = jwt(contextPayload(extra = ",\"exp\":999,\"iat\":5"))
        assertNotEquals(first, refreshed)
        assertEquals("uid-1:caja:est-1:mem-1", JwtContextScope.stableKey(first))
        assertEquals(JwtContextScope.stableKey(first), JwtContextScope.stableKey(refreshed))
    }

    @Test
    fun `stableKey falls back to sub when uid is missing`() {
        val token =
            jwt(
                """{"sub":"uid-9","rol":"caja","establecimiento_id":"est-1","membresia_id":"mem-1"}""",
            )
        assertEquals("uid-9:caja:est-1:mem-1", JwtContextScope.stableKey(token))
    }

    @Test
    fun `different context claims produce different keys`() {
        val base = JwtContextScope.stableKey(jwt(contextPayload()))
        assertNotEquals(base, JwtContextScope.stableKey(jwt(contextPayload(uid = "uid-2"))))
        assertNotEquals(base, JwtContextScope.stableKey(jwt(contextPayload(role = "cliente"))))
        assertNotEquals(base, JwtContextScope.stableKey(jwt(contextPayload(establishment = "est-2"))))
        assertNotEquals(base, JwtContextScope.stableKey(jwt(contextPayload(membership = "mem-2"))))
    }

    @Test
    fun `malformed tokens and missing claims return null`() {
        assertNull(JwtContextScope.stableKey(""))
        assertNull(JwtContextScope.stableKey("not-a-jwt"))
        assertNull(JwtContextScope.stableKey("a.b"))
        assertNull(
            JwtContextScope.stableKey(
                jwt("aW50LXZhbGlk"),
            ),
        )
        assertNull(JwtContextScope.stableKey(jwt(contextPayload(uid = null))))
        assertNull(
            JwtContextScope.stableKey(
                jwt(
                    """{"sub":" ","rol":"caja","establecimiento_id":"e","membresia_id":"m"}""",
                ),
            ),
        )
        assertNull(
            JwtContextScope.stableKey(
                jwt("""{"uid":"u","establecimiento_id":"e","membresia_id":"m"}"""),
            ),
        )
        assertNull(
            JwtContextScope.stableKey(
                jwt("""{"uid":"u","rol":"caja","membresia_id":"m"}"""),
            ),
        )
        assertNull(
            JwtContextScope.stableKey(
                jwt("""{"uid":"u","rol":"caja","establecimiento_id":"e"}"""),
            ),
        )
    }

    @Test
    fun `storageKey hashes unstable tokens deterministically`() {
        val bad = "opaque-token"
        val key = JwtContextScope.storageKey(bad)
        assertTrue(key.startsWith("token:"))
        assertEquals("token:${JwtContextScope.sha256(bad)}", key)
        assertEquals(key, JwtContextScope.storageKey(bad))
        assertEquals("uid-1:caja:est-1:mem-1", JwtContextScope.storageKey(jwt(contextPayload())))
    }

    @Test
    fun `sha256 is lowercase hex`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            JwtContextScope.sha256("abc"),
        )
    }
}
