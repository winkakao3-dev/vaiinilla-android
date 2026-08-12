package com.vaiinilla.app

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.wallet.RemoteWalletRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteWalletRepositoryTest {
    @Test
    fun `wallet propia usa contrato remoto y mapea buckets sin exponerlos a UI`() {
        val client = FakeWalletClient()
        val result =
            RemoteWalletRepository(client)
                .getMyWallet()

        assertTrue(result.isSuccess)
        val wallet = result.getOrThrow()
        assertEquals("125.00", wallet.wallet.visibleBalance)
        assertEquals(
            "100.00",
            wallet.wallet.pendingCommissionBalance,
        )
        assertEquals(
            "25.00",
            wallet.wallet.paidCommissionBalance,
        )
        assertEquals(
            "recarga_efectivo",
            wallet.movements.single().type,
        )
        assertEquals("wallets/me", client.lastPath)
    }

    private class FakeWalletClient : VaiinillaApiClient {
        override val baseUrl: String = "https://example.invalid/api/v1/"
        var lastPath: String? = null

        override fun get(
            path: String,
            query: Map<String, String>,
        ): Result<String> {
            lastPath = path
            return Result.success(
                """
                {"data":{"wallet":{"id":"w1","usuario_id":"u1","establecimiento_id":"e1","saldo_comision_pagada":"25.00","saldo_comision_pendiente":"100.00","saldo_visible":"125.00","actualizado_en":"2026-08-12T12:00:00Z"},"movimientos":[{"id":"m1","tipo":"recarga_efectivo","monto":"100.00","bucket":"pendiente","pedido_id":null,"registrado_por":"c1","idempotency_key":"k1","creado_en":"2026-08-12T12:00:00Z"}]},"meta":{"page":null,"total_pages":null,"total_items":null,"cursor":null},"error":null}
                """.trimIndent(),
            )
        }

        override fun post(
            path: String,
            body: String,
            headers: Map<String, String>,
        ): Result<String> = error("no usado")
    }
}
