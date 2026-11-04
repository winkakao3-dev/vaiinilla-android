package com.vaiinilla.app.data.localhub

import com.vaiinilla.app.domain.localhub.LocalHubOperations
import com.vaiinilla.app.domain.localhub.LocalHubSigning
import com.vaiinilla.app.domain.localhub.LocalTicketBoardSnapshot
import com.vaiinilla.app.domain.localhub.LocalTicketState
import java.time.Instant

/**
 * Lo que hace el concentrador con cada petición, sin sockets de por medio (así se prueba): revisa la firma con las
 * llaves del negocio y aplica la operación sobre el tablero de comandas, que se guarda en disco en cada cambio.
 */
class LocalHubHandler(
    private val keys: () -> List<String>,
    private val loadBoard: () -> LocalTicketBoardSnapshot,
    private val saveBoard: (LocalTicketBoardSnapshot) -> Unit,
    private val now: () -> Instant = Instant::now,
    private val onChange: (LocalTicketBoardSnapshot) -> Unit = {},
) {
    private val lock = Any()

    fun handle(raw: String): String {
        val response =
            runCatching { LocalHubJson.decodeRequest(raw) }.fold(
                onSuccess = { request -> handle(request) },
                onFailure = { LocalHubJson.Response(ok = false, error = ERROR_BAD_REQUEST) },
            )
        return LocalHubJson.encodeResponse(response)
    }

    private fun handle(request: LocalHubJson.Request): LocalHubJson.Response {
        val signed =
            LocalHubSigning.verify(
                keys = keys(),
                timestampMs = request.ts,
                operation = request.op,
                payload = request.payload,
                signature = request.firma,
                now = now(),
            )
        if (!signed) return LocalHubJson.Response(ok = false, error = ERROR_SIGNATURE)
        return runCatching {
            when (request.op) {
                LocalHubOperations.ADD_TICKET -> {
                    val ticket = LocalHubJson.decodeTicket(request.payload)
                    update { it.add(ticket) }
                    LocalHubJson.Response(ok = true)
                }
                LocalHubOperations.LIST_TICKETS -> {
                    val board = update { it }
                    LocalHubJson.Response(ok = true, payload = LocalHubJson.encodeBoard(board))
                }
                LocalHubOperations.MARK_TICKET -> {
                    val mark = LocalHubJson.decodeMark(request.payload)
                    update { it.mark(mark.id, LocalTicketState.fromWire(mark.estado)) }
                    LocalHubJson.Response(ok = true)
                }
                else -> LocalHubJson.Response(ok = false, error = ERROR_UNKNOWN_OPERATION)
            }
        }.getOrElse { LocalHubJson.Response(ok = false, error = ERROR_BAD_REQUEST) }
    }

    /** Aplica un cambio local (la Caja también toma pedidos) con la misma regla que las peticiones de la red. */
    fun update(change: (LocalTicketBoardSnapshot) -> LocalTicketBoardSnapshot): LocalTicketBoardSnapshot =
        synchronized(lock) {
            val before = loadBoard().withoutOlderThan(now())
            val after = change(before)
            if (after != before) {
                saveBoard(after)
                onChange(after)
            }
            after
        }

    companion object {
        const val ERROR_SIGNATURE = "firma_invalida"
        const val ERROR_BAD_REQUEST = "peticion_invalida"
        const val ERROR_UNKNOWN_OPERATION = "operacion_desconocida"
    }
}
