package com.vaiinilla.app

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.wallet.RemoteWalletRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteWalletRepositoryTest {
    @Test
    fun `wallet propia mapea saldo visible y movimientos del contrato remoto`() {
        val client = FakeWalletClient()
        val result = RemoteWalletRepository(client).getMyWallet()

        assertTrue(result.isSuccess)
        val wallet = result.getOrThrow()
        assertEquals("125.00", wallet.wallet.visibleBalance)
        assertEquals("Compra con saldo", wallet.movements.single().description)
        assertEquals("90.00", wallet.movements.single().balanceAfter)
        assertEquals("wallets/me", client.lastPath)
    }

    @Test
    fun `busqueda de clientes usa q y respeta el identificador contextual`() {
        val client = FakeWalletClient()
        val result = RemoteWalletRepository(client).searchClients(" Ana ")

        assertTrue(result.isSuccess)
        assertEquals("wallets/clientes", client.lastPath)
        assertEquals(mapOf("q" to "Ana"), client.lastQuery)
        assertEquals("A01234", result.getOrThrow().first().contextualId)
    }

    @Test
    fun `busqueda de clientes trae correo enmascarado, ultimo pedido y saldo, y tolera que falten`() {
        val (ana, homonima) = RemoteWalletRepository(FakeWalletClient()).searchClients("Ana").getOrThrow()

        assertEquals("a***@ejemplo.com", ana.maskedEmail)
        assertEquals("2026-10-28T19:04:11.000Z", ana.lastOrderAt)
        assertEquals("321.95", ana.balance)
        assertEquals(null, homonima.maskedEmail)
        assertEquals(null, homonima.lastOrderAt)
        assertEquals(null, homonima.balance)
    }

    @Test
    fun `cliente escaneado toma el saldo de su wallet`() {
        val client = FakeWalletClient()
        val ana = RemoteWalletRepository(client).lookupClient("u1").getOrThrow()

        assertEquals("wallets/u1", client.lastPath)
        assertEquals("125.00", ana.balance)
    }

    @Test
    fun `recarga mapea comprobante y envia idempotencia`() {
        val client = FakeWalletClient()
        val result = RemoteWalletRepository(client).reloadCash("u1", "100.00", "key-1")

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals("25.00", receipt.previousBalance)
        assertEquals("125.00", receipt.newBalance)
        assertEquals("wallets/u1/recargas-efectivo", client.lastPath)
        assertEquals("key-1", client.lastHeaders["Idempotency-Key"])
    }

    private class FakeWalletClient : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"
        var lastPath: String? = null
        var lastQuery: Map<String, String> = emptyMap()
        var lastHeaders: Map<String, String> = emptyMap()

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> {
            lastPath = path
            lastQuery = query
            return when (path) {
                "wallets/me", "wallets/u1" ->
                    Result.success(
                        """
                        {"data":{"cliente":{"usuario_id":"u1","nombre":"Ana Pérez","identificador_cliente":"A01234"},"wallet":{"id":"w1","usuario_id":"u1","establecimiento_id":"e1","saldo":"125.00","actualizado_en":"2026-08-12T12:00:00Z"},"movimientos":[{"id":"m1","tipo":"compra","descripcion":"Compra con saldo","monto":"-35.00","saldo_posterior":"90.00","pedido_id":"p1","creado_en":"2026-08-12T12:00:00Z","pedido_folio":12,"lugar":"Mesa 2","resumen_productos":"2× Molletes y 1 más"}]},"meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}
                        """.trimIndent(),
                    )
                "wallets/clientes" ->
                    Result.success(
                        """
                        {"data":[{"usuario_id":"u1","nombre":"Ana Pérez","identificador_cliente":"A01234","correo_enmascarado":"a***@ejemplo.com","ultimo_pedido_en":"2026-10-28T19:04:11.000Z","saldo":"321.95"},{"usuario_id":"u2","nombre":"Ana Pérez"}],"meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}
                        """.trimIndent(),
                    )
                else -> error("ruta no usada: $path")
            }
        }

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> {
            lastPath = path
            lastHeaders = headers
            return Result.success(
                """
                {"data":{"usuario_id":"u1","saldo_anterior":"25.00","recarga":"100.00","saldo_nuevo":"125.00","movimiento_id":"m1","sesion_caja_id":"c1"},"meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}
                """.trimIndent(),
            )
        }
    }
}
