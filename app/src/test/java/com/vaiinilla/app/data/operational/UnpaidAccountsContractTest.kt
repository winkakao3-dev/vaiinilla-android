package com.vaiinilla.app.data.operational

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnpaidAccountsContractTest {
    @Test
    fun `parses accounts with the backend fields and flags who left without paying`() {
        val raw =
            """
            {"data":{
              "cuentas":[
                {"espacio":{"id":3,"nombre":"Mesa 3","tipo":"mesa"},"pedidos":2,"total":"240.00",
                 "desde":"2026-10-11T14:00:00.000Z","horas":5,"clientes":["Ana","Luis"],
                 "abandonada":false,"abonado":"0.00"},
                {"espacio":{"id":9,"nombre":"Cancha 2","tipo":"cancha"},"pedidos":1,"total":"90.00",
                 "desde":"2026-10-11T18:00:00.000Z","horas":0,"clientes":[],
                 "abandonada":true,"abonado":"0.00"}
              ],
              "total":"330.00","abandonadas":1
            },"error":null,"meta":null}
            """.trimIndent()

        val result = WaiterContractJson().parseUnpaidAccounts(raw)

        assertEquals("330.00", result.total)
        assertEquals(1, result.abandonedCount)
        assertEquals(2, result.accounts.size)
        val mesa = result.accounts[0]
        assertEquals(3, mesa.spaceId)
        assertEquals("Mesa 3", mesa.spaceName)
        assertEquals(2, mesa.orders)
        assertEquals(5, mesa.hours)
        assertEquals(listOf("Ana", "Luis"), mesa.clients)
        assertFalse(mesa.abandoned)
        assertTrue(result.accounts[1].abandoned)
        assertEquals(emptyList<String>(), result.accounts[1].clients)
    }

    @Test
    fun `an empty list parses to no accounts`() {
        val result =
            WaiterContractJson().parseUnpaidAccounts(
                """{"data":{"cuentas":[],"total":"0.00","abandonadas":0},"error":null}""",
            )

        assertTrue(result.accounts.isEmpty())
        assertEquals(0, result.abandonedCount)
    }
}
