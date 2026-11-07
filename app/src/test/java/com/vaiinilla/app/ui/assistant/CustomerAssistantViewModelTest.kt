package com.vaiinilla.app.ui.assistant

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.assistant.AssistantOrderLine
import com.vaiinilla.app.domain.assistant.AssistantProductCard
import com.vaiinilla.app.domain.assistant.CustomerAssistantReply
import com.vaiinilla.app.domain.assistant.CustomerAssistantRepository
import com.vaiinilla.app.domain.assistant.CustomerAssistantTurn
import com.vaiinilla.app.ui.operational.StaffHarness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerAssistantViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        StaffHarness.restoreMain()
    }

    @Test
    fun `el invitado sin alta da su nombre y la platica sigue`() {
        val repository = FakeRepository(guestNeedsName = true)
        val vm = CustomerAssistantViewModel(repository)
        vm.onVisible("est-1:4", "est-1")

        vm.send("¿Qué me recomiendas?")
        waitUntil { vm.state.value.needsName }
        assertEquals(0, repository.replies)
        assertFalse(vm.state.value.canSend)

        vm.submitName("  Sofía   L ")
        waitUntil { vm.state.value.turns.size == 2 }
        assertEquals("Sofía L", repository.enteredName)
        assertFalse(vm.state.value.needsName)
        assertEquals("est-1", repository.lastEstablishment)
    }

    @Test
    fun `una respuesta en camino no entra a la platica nueva`() {
        val gate = CountDownLatch(1)
        val repository = FakeRepository(gate = gate)
        val vm = CustomerAssistantViewModel(repository)
        vm.onVisible("est-1:4", "est-1")

        vm.send("Hola")
        waitUntil { repository.started == 1 }
        vm.restart()
        gate.countDown()
        waitUntil { repository.replies == 1 }
        Thread.sleep(50)

        assertTrue(
            vm.state.value.turns
                .isEmpty(),
        )
        assertFalse(vm.state.value.thinking)
    }

    @Test
    fun `el asistente apagado dice lo que explica el servidor`() {
        val repository =
            FakeRepository(
                failure =
                    ApiClientException(
                        "ASSISTANT_UNAVAILABLE",
                        "El asistente no está disponible. Pide desde el menú o llama al mesero.",
                        503,
                    ),
            )
        val vm = CustomerAssistantViewModel(repository)
        vm.onVisible("est-1:4", "est-1")
        vm.send("Hola")
        waitUntil { vm.state.value.error != null }
        assertEquals("El asistente no está disponible. Pide desde el menú o llama al mesero.", vm.state.value.error)
    }

    @Test
    fun `si la tienda lo apaga se cierra la platica sin reintento`() {
        val message = "Este lugar no tiene asistente. Pide desde el menú o llama al mesero."
        val repository = FakeRepository(failure = ApiClientException("ASSISTANT_DISABLED", message, 403))
        val vm = CustomerAssistantViewModel(repository)
        vm.onVisible("est-1:4", "est-1")
        vm.send("Hola")
        waitUntil { vm.state.value.error != null }
        assertEquals(message, vm.state.value.error)
        assertTrue(vm.state.value.closed)
        assertFalse(vm.state.value.canSend)
        vm.retry()
        Thread.sleep(50)
        assertEquals(1, repository.started)
    }

    @Test
    fun `con el carrito vacio olvida el pedido que puso el asistente`() {
        val vm = CustomerAssistantViewModel(FakeRepository())
        vm.onVisible("est-1:4", "est-1")
        val tacos =
            AssistantOrderLine(
                product = AssistantProductCard(1, "Tacos", "60.00", null, true, false, null),
                quantity = 2,
                note = null,
            )
        vm.markOrderInCart(0, listOf(tacos))
        assertEquals(listOf(tacos), vm.state.value.cartOrder)

        vm.forgetCartOrder()

        assertTrue(
            vm.state.value.cartOrder
                .isEmpty(),
        )
        assertTrue(0 in vm.state.value.done)
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "El asistente no contestó a tiempo." }
            Thread.sleep(10)
        }
    }

    private class FakeRepository(
        var guestNeedsName: Boolean = false,
        private val gate: CountDownLatch? = null,
        private val failure: Throwable? = null,
    ) : CustomerAssistantRepository {
        @Volatile var started = 0

        @Volatile var replies = 0

        @Volatile var enteredName: String? = null

        @Volatile var lastEstablishment: String? = null

        override fun reply(
            turns: List<CustomerAssistantTurn>,
            establishmentId: String?,
        ): Result<CustomerAssistantReply> {
            started++
            lastEstablishment = establishmentId
            gate?.await()
            replies++
            failure?.let { return Result.failure(it) }
            val reply = CustomerAssistantReply("Te recomiendo los nachos", emptyList(), null, null, emptyList(), 38)
            return Result.success(reply)
        }

        override fun needsGuestName(): Boolean = guestNeedsName

        override fun enterAsGuest(name: String): Result<Unit> {
            enteredName = name
            guestNeedsName = false
            return Result.success(Unit)
        }
    }
}
