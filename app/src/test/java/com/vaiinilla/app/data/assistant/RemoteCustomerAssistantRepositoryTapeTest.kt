package com.vaiinilla.app.data.assistant

import com.vaiinilla.app.data.tape.ServerTape
import com.vaiinilla.app.data.tape.TapeFloor
import com.vaiinilla.app.domain.assistant.CustomerAssistantTurn
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** El asistente del cliente contra lo que el servidor de pruebas contestó de verdad. */
class RemoteCustomerAssistantRepositoryTapeTest {
    @get:Rule
    val tape = ServerTape()

    private val floor by lazy { TapeFloor(tape) }

    @Test
    fun `el asistente le contesta al cliente con lo que hay en el menu`() {
        floor.open()
        floor.product()
        val assistant = RemoteCustomerAssistantRepository(floor.client("cliente"))
        val turns = listOf(CustomerAssistantTurn(fromCustomer = true, text = "¿Qué me recomiendas de comer?"))
        val reply = assistant.reply(turns, tape.store("restaurante").id, null).getOrThrow()
        assertTrue(reply.message.isNotBlank())
        assertTrue(reply.remainingMessages >= 0)
    }
}
