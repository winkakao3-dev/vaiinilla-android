package com.vaiinilla.app.localhub

import com.vaiinilla.app.data.localhub.LocalHubClient
import com.vaiinilla.app.data.localhub.LocalHubHandler
import com.vaiinilla.app.data.localhub.LocalHubJson
import com.vaiinilla.app.data.localhub.LocalHubServer
import com.vaiinilla.app.domain.localhub.LocalHubOperations
import com.vaiinilla.app.domain.localhub.LocalHubSigning
import com.vaiinilla.app.domain.localhub.LocalTicket
import com.vaiinilla.app.domain.localhub.LocalTicketBoardSnapshot
import com.vaiinilla.app.domain.localhub.LocalTicketItem
import com.vaiinilla.app.domain.localhub.LocalTicketState
import com.vaiinilla.app.ui.screens.kitchenLocalTickets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.Base64

class LocalHubTest {
    private val now = Instant.parse("2026-11-09T20:00:00Z")
    private val key = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { it.toByte() })
    private val otherKey = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 7 })

    private fun ticket(
        id: String = "8f0a7c1e-0000-4000-8000-000000000001",
        capturedAt: Instant = now,
        station: String = "cocina",
    ) = LocalTicket(
        id = id,
        localFolio = "M7-014",
        placeLabel = "Pista 3",
        items = listOf(LocalTicketItem("Alitas", 2, "BBQ", station)),
        capturedAt = capturedAt,
        takenBy = "Mesero",
    )

    private fun signed(
        op: String,
        payload: String,
        signingKey: String = key,
        at: Instant = now,
    ): String =
        LocalHubJson.encodeRequest(
            LocalHubJson.Request(
                op = op,
                ts = at.toEpochMilli(),
                firma = LocalHubSigning.sign(signingKey, at.toEpochMilli(), op, payload),
                payload = payload,
            ),
        )

    private fun handler(): Pair<LocalHubHandler, () -> LocalTicketBoardSnapshot> {
        var board = LocalTicketBoardSnapshot()
        val handler =
            LocalHubHandler(
                keys = { listOf(key) },
                loadBoard = { board },
                saveBoard = { board = it },
                now = { now },
            )
        return handler to { board }
    }

    @Test
    fun `una firma valida se acepta y una ajena, vieja o alterada no`() {
        val ts = now.toEpochMilli()
        val firma = LocalHubSigning.sign(key, ts, "op", "{}")
        assertTrue(LocalHubSigning.verify(listOf(otherKey, key), ts, "op", "{}", firma, now))
        assertFalse(LocalHubSigning.verify(listOf(otherKey), ts, "op", "{}", firma, now))
        assertFalse(LocalHubSigning.verify(listOf(key), ts, "op", "{\"x\":1}", firma, now))
        assertFalse(LocalHubSigning.verify(listOf(key), ts, "otra", "{}", firma, now))
        assertFalse(LocalHubSigning.verify(listOf(key), ts, "op", "{}", firma, now.plus(Duration.ofMinutes(11))))
    }

    @Test
    fun `la misma comanda dos veces no se duplica y las de otro turno se van`() {
        val board = LocalTicketBoardSnapshot().add(ticket()).add(ticket())
        assertEquals(1, board.tickets.size)
        val old = board.add(ticket(id = "viejo", capturedAt = now.minus(Duration.ofHours(13))))
        assertEquals(listOf(ticket().id), old.withoutOlderThan(now).tickets.map { it.id })
    }

    @Test
    fun `el concentrador agrega, lista y marca comandas firmadas`() {
        val (handler, board) = handler()
        val add = signed(LocalHubOperations.ADD_TICKET, LocalHubJson.encodeTicket(ticket()))
        assertTrue(LocalHubJson.decodeResponse(handler.handle(add)).ok)
        val mark = LocalHubJson.encodeMark(LocalHubJson.MarkTicket(ticket().id, LocalTicketState.READY.wireValue))
        assertTrue(LocalHubJson.decodeResponse(handler.handle(signed(LocalHubOperations.MARK_TICKET, mark))).ok)
        val listed = LocalHubJson.decodeResponse(handler.handle(signed(LocalHubOperations.LIST_TICKETS, "")))
        val tickets = LocalHubJson.decodeTickets(listed.payload)
        assertEquals(LocalTicketState.READY, tickets.single().state)
        assertEquals(ticket().copy(state = LocalTicketState.READY), board().tickets.single())
    }

    @Test
    fun `sin la llave del negocio el concentrador no acepta nada`() {
        val (handler, board) = handler()
        val raw = handler.handle(signed(LocalHubOperations.ADD_TICKET, LocalHubJson.encodeTicket(ticket()), otherKey))
        assertEquals(LocalHubHandler.ERROR_SIGNATURE, LocalHubJson.decodeResponse(raw).error)
        assertTrue(board().tickets.isEmpty())
        assertEquals(LocalHubHandler.ERROR_BAD_REQUEST, LocalHubJson.decodeResponse(handler.handle("no es json")).error)
    }

    @Test
    fun `mesero y cocina hablan con la caja por un socket`() {
        var board = LocalTicketBoardSnapshot()
        val handler = LocalHubHandler(keys = { listOf(key) }, loadBoard = { board }, saveBoard = { board = it })
        val server = LocalHubServer(handler, port = 0)
        server.start()
        try {
            val client = LocalHubClient()
            val port = server.boundPort
            val fresh = ticket(capturedAt = Instant.now())
            val payload = LocalHubJson.encodeTicket(fresh)
            assertTrue(client.call(LOCALHOST, port, key, LocalHubOperations.ADD_TICKET, payload).getOrThrow().ok)
            val listed = client.call(LOCALHOST, port, key, LocalHubOperations.LIST_TICKETS, "").getOrThrow()
            assertEquals(listOf(fresh.id), LocalHubJson.decodeTickets(listed.payload).map { it.id })
            val rejected = client.call(LOCALHOST, port, otherKey, LocalHubOperations.LIST_TICKETS, "").getOrThrow()
            assertEquals(LocalHubHandler.ERROR_SIGNATURE, rejected.error)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `cocina deja de ver la comanda cuando llega el pedido real y no ve lo de barra`() {
        val kitchen = ticket()
        val bar = ticket(id = "barra", station = "caja")
        assertEquals(listOf(kitchen.id), kitchenLocalTickets(listOf(kitchen, bar), emptySet()).map { it.id })
        assertTrue(kitchenLocalTickets(listOf(kitchen), setOf(kitchen.id)).isEmpty())
    }

    @Test
    fun `las llaves se leen del sobre de llave-local`() {
        val raw =
            """{"success":true,"data":{"llaves":[{"dia":"2026-11-09","llave":"a"},""" +
                """{"dia":"2026-11-10","llave":"b"}]}}"""
        assertEquals(listOf("a", "b"), LocalHubJson.decodeKeys(raw))
    }

    private companion object {
        const val LOCALHOST = "127.0.0.1"
    }
}
