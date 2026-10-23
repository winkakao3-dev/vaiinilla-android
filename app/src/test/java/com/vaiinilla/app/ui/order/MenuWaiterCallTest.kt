package com.vaiinilla.app.ui.order

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.CallsUnavailableException
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.TableCallTaker
import com.vaiinilla.app.domain.repository.TableSpace
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Llamar al mesero" del menú: cuándo sale la píldora, qué dice y cómo reacciona a lo que responde el servidor. */
class MenuWaiterCallTest {
    @Test
    fun `sale con la mesa del QR o la mesa compartida y nunca sin cuenta`() {
        assertEquals(1, menuWaiterSpaceId(guestSpaceId = 1, sharedTableSpaceId = null, signedIn = true))
        assertEquals(1, menuWaiterSpaceId(guestSpaceId = 1, sharedTableSpaceId = 7, signedIn = true))
        assertEquals(7, menuWaiterSpaceId(guestSpaceId = null, sharedTableSpaceId = 7, signedIn = true))
        // Para llevar o escuela: sin mesa no hay a quién llamar.
        assertNull(menuWaiterSpaceId(guestSpaceId = null, sharedTableSpaceId = null, signedIn = true))
        assertNull(menuWaiterSpaceId(guestSpaceId = 1, sharedTableSpaceId = null, signedIn = false))
    }

    @Test
    fun `la pildora dice el estado y se encoge junto a Pedir ya`() {
        assertEquals(
            MenuWaiterPillLabel(MenuWaiterIcon.BELL, "Llamar al mesero", "Mesero"),
            menuWaiterPillLabel(call = null, cooldown = 0),
        )
        assertEquals("Otra vez en 42 s", menuWaiterPillLabel(call = null, cooldown = 42).title)
        assertEquals(
            MenuWaiterPillLabel(MenuWaiterIcon.RINGING, "Llamando al mesero…", "Llamando…"),
            menuWaiterPillLabel(call(CallStatus.PENDIENTE), cooldown = 0),
        )
        assertEquals(
            MenuWaiterPillLabel(MenuWaiterIcon.WALKING, "Luis va en camino", "Luis va"),
            menuWaiterPillLabel(call(CallStatus.EN_CAMINO, taker = "Luis Pérez"), cooldown = 0),
        )
        assertEquals("Tu mesero va en camino", menuWaiterPillLabel(call(CallStatus.EN_CAMINO, taker = " "), 0).title)
    }

    @Test
    fun `solo quien llamo cancela, tambien con el mesero en camino`() {
        assertTrue(canCancelMenuCall(call(CallStatus.PENDIENTE)))
        // K02: el servidor deja cancelar mientras la llamada siga abierta.
        assertTrue(canCancelMenuCall(call(CallStatus.EN_CAMINO)))
        assertFalse(canCancelMenuCall(call(CallStatus.ATENDIDA)))
        // Otro de la mesa llamó: el servidor no manda su nombre y cancelar le daría CALL_NOT_FOUND.
        assertFalse(canCancelMenuCall(call(CallStatus.PENDIENTE, client = null)))
    }

    @Test
    fun `el alias para sentarse es el primer nombre o Cliente`() {
        assertEquals("Ana", waiterCallAlias("  Ana  López "))
        assertEquals("Cliente", waiterCallAlias(""))
        assertEquals(30, waiterCallAlias("x".repeat(40)).length)
    }

    @Test
    fun `llama sin pedido y queda abierta`() =
        runTest {
            val orderIds = mutableListOf<String?>()
            val state =
                state(onCall = { _, _, orderId ->
                    orderIds += orderId
                    Result.success(call(CallStatus.PENDIENTE))
                })
            state.tap()
            assertEquals(MenuWaiterPhase.CHOOSING, state.phase)
            assertTrue(state.expanded)

            assertTrue(state.send(CallReason.UTENSILIOS))
            assertEquals(listOf<String?>(null), orderIds)
            assertEquals(MenuWaiterPhase.OPEN, state.phase)
            assertFalse(state.expanded)

            // Tocar la píldora con la llamada abierta muestra su estado.
            state.tap()
            assertTrue(state.showsCall)
        }

    @Test
    fun `si no esta sentado lo une a la mesa y reintenta`() =
        runTest {
            var calls = 0
            var joins = 0
            val state =
                state(
                    onCall = { _, _, _ ->
                        calls += 1
                        if (calls == 1) Result.failure(notAtTable()) else Result.success(call(CallStatus.PENDIENTE))
                    },
                    onJoin = {
                        joins += 1
                        Result.success(Unit)
                    },
                )
            state.tap()
            assertTrue(state.send(CallReason.ATENCION))
            assertEquals(2, calls)
            assertEquals(1, joins)
            assertEquals(MenuWaiterPhase.OPEN, state.phase)
        }

    @Test
    fun `sin QR para unirse pide hacerlo en Pedidos`() =
        runTest {
            val state = state(onCall = { _, _, _ -> Result.failure(notAtTable()) }, onJoin = null)
            state.tap()
            assertFalse(state.send(CallReason.ATENCION))
            assertEquals(MenuWaiterCallState.NOT_AT_TABLE_MESSAGE, state.error)
            assertEquals(MenuWaiterPhase.CHOOSING, state.phase)
        }

    @Test
    fun `los 60 s entre llamadas corren con lo que diga el servidor`() =
        runTest {
            val limited =
                OrderRepositoryException(
                    "RATE_LIMITED",
                    "Acabas de llamar al mesero; espera un momento para volver a llamar.",
                    ApiClientException("RATE_LIMITED", "Acabas de llamar…", 429, retryAfterSeconds = 37),
                )
            val state = state(onCall = { _, _, _ -> Result.failure(limited) })
            state.tap()
            assertFalse(state.send(CallReason.CUENTA))
            assertEquals(37, state.cooldown)
            assertEquals(MenuWaiterPhase.IDLE, state.phase)
            assertEquals("Acabas de llamar al mesero; espera un momento para volver a llamar.", state.error)
            // Mientras corre la espera, la píldora no vuelve a abrir los motivos.
            state.tap()
            assertEquals(MenuWaiterPhase.IDLE, state.phase)
            state.tick()
            assertEquals(36, state.cooldown)
        }

    @Test
    fun `cancelar cierra la llamada y arranca la espera`() =
        runTest {
            val cancelled = mutableListOf<String>()
            val state =
                state(
                    onCurrent = { Result.success(call(CallStatus.PENDIENTE)) },
                    onCancel = {
                        cancelled += it.id
                        Result.success(it.copy(status = CallStatus.CANCELADA))
                    },
                )
            state.refresh()
            assertEquals(MenuWaiterPhase.OPEN, state.phase)
            state.cancel()
            assertEquals(listOf("call-1"), cancelled)
            assertNull(state.call)
            assertEquals(MenuWaiterPhase.IDLE, state.phase)
            assertEquals(MenuWaiterCallState.CALL_COOLDOWN_SECONDS, state.cooldown)
        }

    @Test
    fun `la consulta sigue el estado en vivo y nota cuando la atienden`() =
        runTest {
            var current: TableCall? = call(CallStatus.PENDIENTE)
            val state = state(onCurrent = { Result.success(current) })
            state.refresh()
            assertEquals(CallStatus.PENDIENTE, state.call?.status)
            current = call(CallStatus.EN_CAMINO, taker = "Luis")
            state.refresh()
            assertEquals("Luis va en camino", menuWaiterPillLabel(state.call, state.cooldown).title)
            current = null
            state.refresh()
            assertEquals(MenuWaiterPhase.IDLE, state.phase)
            assertEquals(MenuWaiterCallState.CALL_COOLDOWN_SECONDS, state.cooldown)
        }

    @Test
    fun `sin llamadas en el negocio la pildora se va`() =
        runTest {
            val state = state(onCurrent = { Result.failure(CallsUnavailableException()) })
            state.refresh()
            assertEquals(MenuWaiterPhase.UNAVAILABLE, state.phase)
        }

    @Test
    fun `si el servidor falla al llamar la pildora se queda con el error`() =
        runTest {
            // C04: el servidor de pruebas responde 404; antes la píldora se iba sin decir nada.
            val state = state(onCall = { _, _, _ -> Result.failure(CallsUnavailableException()) })
            state.tap()
            assertFalse(state.send(CallReason.UTENSILIOS))
            assertEquals(MenuWaiterPhase.CHOOSING, state.phase)
            assertTrue(state.expanded)
            assertEquals("Llamar al mesero todavía no está disponible en esta cafetería.", state.error)
        }

    private fun notAtTable() =
        OrderRepositoryException(
            "NO_ACTIVE_ORDER_IN_SPACE",
            "Únete a la mesa o haz un pedido en ella para llamar al mesero.",
        )

    private fun state(
        onCurrent: suspend (Int) -> Result<TableCall?> = { Result.success(null) },
        onCall: suspend (
            Int,
            CallReason,
            String?,
        ) -> Result<TableCall> = { _, _, _ -> Result.success(call(CallStatus.PENDIENTE)) },
        onCancel: suspend (TableCall) -> Result<TableCall> = { Result.success(it) },
        onJoin: (suspend () -> Result<Unit>)? = { Result.success(Unit) },
    ) = MenuWaiterCallState(1, onCurrent, onCall, onCancel, onJoin)

    private fun call(
        status: CallStatus,
        taker: String? = null,
        client: String? = "Ana",
    ) = TableCall(
        id = "call-1",
        space = TableSpace(id = 1, name = "Mesa 1", type = "mesa"),
        orderId = null,
        reason = CallReason.UTENSILIOS,
        status = status,
        clientName = client,
        takenBy = taker?.let { TableCallTaker(userId = "u-2", name = it) },
        createdAt = "2026-10-22T16:54:08.777Z",
        takenAt = null,
        closedAt = null,
        version = 1,
    )
}
