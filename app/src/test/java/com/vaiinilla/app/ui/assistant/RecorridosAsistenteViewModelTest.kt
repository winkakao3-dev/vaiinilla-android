package com.vaiinilla.app.ui.assistant

import com.vaiinilla.app.domain.assistant.AssistantOrderLine
import com.vaiinilla.app.domain.assistant.AssistantOrderProposal
import com.vaiinilla.app.domain.assistant.AssistantProductCard
import com.vaiinilla.app.domain.assistant.CustomerAssistantReply
import com.vaiinilla.app.domain.assistant.CustomerAssistantRepository
import com.vaiinilla.app.domain.assistant.CustomerAssistantTurn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/**
 * El asistente del cliente dentro del ViewModel (Y06 y Y07): la propuesta de pedido y su paso al carrito, y una
 * pregunta fuera de tema. Lo que el modelo de lenguaje contesta lo decide el servidor y no se prueba aquí.
 * Corre en la JVM; no cuenta como recorrido en teléfono.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecorridosAsistenteViewModelTest {
    private lateinit var mainThread: java.util.concurrent.ExecutorService
    private val replies = CopyOnWriteArrayList<CustomerAssistantReply>()
    private val seenTurns = CopyOnWriteArrayList<List<CustomerAssistantTurn>>()

    private val repository =
        object : CustomerAssistantRepository {
            override fun reply(
                turns: List<CustomerAssistantTurn>,
                establishmentId: String?,
            ): Result<CustomerAssistantReply> {
                seenTurns += turns
                return Result.success(replies[seenTurns.size - 1])
            }

            override fun needsGuestName() = false

            override fun enterAsGuest(name: String) = Result.success(Unit)
        }

    @Before
    fun setUp() {
        mainThread = java.util.concurrent.Executors.newSingleThreadExecutor()
        Dispatchers.setMain(mainThread.asCoroutineDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainThread.shutdownNow()
    }

    private fun card(id: Int, name: String) = AssistantProductCard(id, name, "60.00", null, true, false, 8)

    private fun order(vararg lines: Pair<AssistantProductCard, Int>) =
        AssistantOrderProposal(lines.map { AssistantOrderLine(it.first, it.second, null) }, "120.00", emptyList())

    private fun reply(
        message: String,
        order: AssistantOrderProposal? = null,
        remaining: Int = 30,
    ) = CustomerAssistantReply(message, emptyList(), order, null, emptyList(), remaining)

    private fun wait(
        what: String,
        cond: () -> Boolean,
    ) {
        val end = System.currentTimeMillis() + 20_000
        while (!cond()) {
            if (System.currentTimeMillis() > end) throw AssertionError("Se esperaba: $what")
            Thread.sleep(10)
        }
    }

    private fun viewModel() = CustomerAssistantViewModel(repository).also { it.onVisible("est-1:4", "est-1") }

    @Test
    fun y06_laPropuestaDePedidoLlegaConSusLineasYSoloLaMasRecienteSePuedePonerEnElCarrito() {
        val tacos = card(1, "Tacos")
        val agua = card(2, "Agua")
        replies += reply("Dos tacos y un agua", order(tacos to 2, agua to 1))
        replies += reply("Mejor solo los tacos", order(tacos to 2))
        val vm = viewModel()
        vm.send("quiero algo ligero")
        wait("la primera propuesta") { vm.state.value.turns.size == 2 && !vm.state.value.thinking }
        assertEquals(1, vm.state.value.latestOrderTurn)
        vm.send("sin el agua")
        wait("la segunda propuesta") { vm.state.value.turns.size == 4 && !vm.state.value.thinking }
        assertEquals(3, vm.state.value.latestOrderTurn)
        assertEquals(1, vm.state.value.turns[3].order!!.lines.size)
    }

    @Test
    fun y06_alPonerElPedidoEnElCarritoElTurnoQuedaHechoYElSiguienteLoReemplaza() {
        val tacos = card(1, "Tacos")
        replies += reply("Dos tacos", order(tacos to 2))
        val vm = viewModel()
        vm.send("tacos")
        wait("la propuesta") { vm.state.value.turns.size == 2 && !vm.state.value.thinking }
        val added = vm.state.value.turns[1].order!!.lines
        vm.markOrderInCart(1, added)
        assertTrue(1 in vm.state.value.done)
        assertEquals(added, vm.state.value.cartOrder)
        vm.forgetCartOrder()
        assertTrue(vm.state.value.cartOrder.isEmpty())
    }

    @Test
    fun y07_unaPreguntaFueraDeTemaSeMuestraComoRespuestaSinProductosNiPedido() {
        replies += reply("De eso no te puedo ayudar, pero dime qué se te antoja.", remaining = 29)
        val vm = viewModel()
        vm.send("¿quién ganó el partido de ayer?")
        wait("la respuesta") { vm.state.value.turns.size == 2 && !vm.state.value.thinking }
        val answer = vm.state.value.turns[1]
        assertFalse(answer.fromCustomer)
        assertTrue(answer.products.isEmpty())
        assertEquals(null, answer.order)
        assertEquals(-1, vm.state.value.latestOrderTurn)
        assertEquals(29, vm.state.value.remaining)
        assertTrue(vm.state.value.canSend)
    }

    @Test
    fun y07_conLaPlaticaAgotadaYaNoSePuedeEnviar() {
        replies += reply("Ya casi terminamos.", remaining = 1)
        val vm = viewModel()
        vm.send("hola")
        wait("la respuesta") { vm.state.value.turns.size == 2 && !vm.state.value.thinking }
        assertFalse(vm.state.value.canSend)
        vm.send("otra más")
        Thread.sleep(100)
        assertEquals(2, vm.state.value.turns.size)
    }

    @Test
    fun laPlaticaMandaAlServidorTodosLosTurnosYElUltimoEsDelCliente() {
        replies += reply("Hola")
        replies += reply("Con gusto")
        val vm = viewModel()
        vm.send("hola")
        wait("la primera respuesta") { vm.state.value.turns.size == 2 && !vm.state.value.thinking }
        vm.send("¿qué hay de postre?")
        wait("la segunda respuesta") { vm.state.value.turns.size == 4 && !vm.state.value.thinking }
        assertEquals(3, seenTurns[1].size)
        assertTrue(seenTurns[1].last().fromCustomer)
    }
}
