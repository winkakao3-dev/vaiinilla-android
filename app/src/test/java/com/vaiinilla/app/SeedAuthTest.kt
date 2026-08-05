package com.vaiinilla.app

import com.vaiinilla.app.data.auth.SesionesContextoDataDto
import com.vaiinilla.app.data.auth.SesionesContextoEnvelopeDto
import com.vaiinilla.app.data.contract.MetaDto
import com.vaiinilla.app.domain.auth.SeedAccounts
import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedAuthTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `seed accounts require local passwords when enabled`() {
        // Without passwords in BuildConfig (typical CI), forRole returns null.
        // With local.properties passwords on a developer machine, all roles resolve.
        val configured = SeedAccounts.isConfigured()
        if (configured) {
            OperationalRole.entries.forEach { role ->
                val account = SeedAccounts.forRole(role)
                assertEquals(role, account?.role)
                assertTrue(account!!.email.endsWith("@vaiinilla.test"))
                assertTrue(account.membresiaId.isNotBlank())
                assertTrue(account.password.isNotBlank())
            }
        } else {
            OperationalRole.entries.forEach { role ->
                assertEquals(null, SeedAccounts.forRole(role))
            }
            assertTrue(SeedAccounts.all().isEmpty())
            assertFalse(SeedAccounts.isConfigured())
        }
    }

    @Test
    fun `parses sesiones contexto response envelope`() {
        val raw =
            """
            {
              "data": {
                "access_token": "jwt-test-token",
                "token_type": "Bearer",
                "expires_in": 900,
                "contexto": {
                  "usuario_id": "032819a8-8dbd-4aef-a728-2e1be9ef09ab",
                  "membresia_id": "9c3334d4-1ed6-4cc2-a4a8-785a6afd03f3",
                  "establecimiento_id": "8246ff44-aad0-4e49-9268-b71c997893fe",
                  "rol": "cliente"
                }
              },
              "meta": {
                "page": null,
                "total_pages": null,
                "total_items": null,
                "cursor": null
              },
              "error": null
            }
            """.trimIndent()

        val envelope = json.decodeFromString<SesionesContextoEnvelopeDto>(raw)
        val data: SesionesContextoDataDto = envelope.data

        assertEquals("jwt-test-token", data.accessToken)
        assertEquals("Bearer", data.tokenType)
        assertEquals(900, data.expiresIn)
        assertEquals("cliente", data.contexto?.rol)
        assertEquals(MetaDto(null, null, null, null), envelope.meta)
    }
}
