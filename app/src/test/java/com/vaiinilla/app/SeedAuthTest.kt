package com.vaiinilla.app

import com.vaiinilla.app.data.auth.SesionesContextoDataDto
import com.vaiinilla.app.data.auth.SesionesContextoEnvelopeDto
import com.vaiinilla.app.data.fixture.MetaDto
import com.vaiinilla.app.domain.auth.SeedAccounts
import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SeedAuthTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `seed accounts map all operational roles`() {
        OperationalRole.entries.forEach { role ->
            val account = SeedAccounts.forRole(role)
            assertNotNull("Missing seed account for $role", account)
            assertEquals(role, account?.role)
        }
    }

    @Test
    fun `seed accounts use vaiinilla test emails`() {
        SeedAccounts.all().forEach { account ->
            assert(account.email.endsWith("@vaiinilla.test"))
            assert(account.membresiaId.isNotBlank())
            assert(account.password.isNotBlank())
        }
    }

    @Test
    fun `parses sesiones contexto response envelope`() {
        val raw = """
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
