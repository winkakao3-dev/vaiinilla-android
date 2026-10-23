package com.vaiinilla.app.data.guardianship

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.domain.guardianship.GuardianshipException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El código de la escuela que habilita a un papá (migración 104). */
class ParentUnlockContractTest {
    private fun status(extra: String) =
        """{"data":{"recibiendo_pedidos":true,"sesion_caja_abierta":true,"caja_en_linea":true,
        "cocina_en_linea":true,"tiempo_estimado_min":5,"consultado_en":"2026-10-22T16:53:34.919Z"$extra},
        "meta":{"page":null},"error":null}"""

    @Test
    fun operationalStatusReadsTutorHabilitadoAndAnOlderServerDoesNotAskForTheCode() {
        val parser = ContractResponseParser()
        assertTrue(parser.parseOperationalStatus(status("")).isParentUnlocked)
        val locked = parser.parseOperationalStatus(status(""","modo_escuela":true,"tutor_habilitado":false"""))
        assertTrue(locked.isSchool)
        assertFalse(locked.isParentUnlocked)
    }

    @Test
    fun unlockPostsTheSchoolCodeWithItsKeyAndMapsTheServerCode() {
        val calls = mutableListOf<Triple<String, String, Map<String, String>>>()
        var reply: Result<String> = Result.success("""{"data":{"tutor_habilitado":true},"error":null}""")
        val client =
            object : VaiinillaApiClient {
                override val baseUrl = "https://example.test/api/v1/"

                override fun get(
                    path: String,
                    query: Map<String, String>,
                ): Result<String> = error("no se usa")

                override fun post(
                    path: String,
                    body: String,
                    headers: Map<String, String>,
                ): Result<String> {
                    calls += Triple(path, body, headers)
                    return reply
                }
            }
        val repository = RemoteGuardianshipRepository(client)

        assertTrue(repository.unlockParent("SANDBX", "llave-1").isSuccess)
        val (path, body, headers) = calls.single()
        assertEquals("tutelas/habilitar", path)
        assertEquals(
            "SANDBX",
            Json
                .parseToJsonElement(body)
                .jsonObject["codigo_union"]
                ?.jsonPrimitive
                ?.content,
        )
        assertEquals("llave-1", headers["Idempotency-Key"])

        reply = Result.failure(ApiClientException("JOIN_CODE_INVALID", "Ese no es el código de la escuela.", 422))
        val error = repository.unlockParent("ZZZZZZ", "llave-2").exceptionOrNull() as GuardianshipException
        assertEquals(GuardianshipException.JOIN_CODE_INVALID, error.code)
        assertEquals(422, error.httpStatus)
    }
}
