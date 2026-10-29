package com.vaiinilla.app.ui.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.CallStatus
import com.vaiinilla.app.domain.repository.CallsUnavailableException
import com.vaiinilla.app.domain.repository.OrderRepositoryException
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.ui.sharedtable.SHARED_TABLE_ALIAS_MAX
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * La mesa desde la que se puede llamar al mesero en el menú: la del QR o el código con el que entró, o la mesa
 * compartida donde ya está sentado. Para llevar, en una escuela o sin cuenta no hay a quién llamar.
 */
fun menuWaiterSpaceId(
    guestSpaceId: Int?,
    sharedTableSpaceId: Int?,
    signedIn: Boolean,
): Int? = if (signedIn) guestSpaceId ?: sharedTableSpaceId else null

/** Con quién entra a la mesa quien llama sin haberse unido: su primer nombre, como en Pedidos. */
fun waiterCallAlias(displayName: String): String {
    val first =
        displayName
            .trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            .orEmpty()
    return first.ifEmpty { "Cliente" }.take(SHARED_TABLE_ALIAS_MAX)
}

enum class MenuWaiterIcon { BELL, RINGING, WALKING }

data class MenuWaiterPillLabel(
    val icon: MenuWaiterIcon,
    val title: String,
    /** Con "Pedir ya" a la vista la píldora se encoge para que quepan las dos. */
    val shortTitle: String,
)

fun menuWaiterPillLabel(
    call: TableCall?,
    cooldown: Int,
): MenuWaiterPillLabel {
    if (call != null) {
        if (call.status == CallStatus.EN_CAMINO) {
            val name = waiterFirstName(call)
            return MenuWaiterPillLabel(MenuWaiterIcon.WALKING, "$name va en camino", "$name va")
        }
        return MenuWaiterPillLabel(MenuWaiterIcon.RINGING, "Llamando al mesero…", "Llamando…")
    }
    if (cooldown > 0) return MenuWaiterPillLabel(MenuWaiterIcon.BELL, "Otra vez en $cooldown s", "$cooldown s")
    return MenuWaiterPillLabel(MenuWaiterIcon.BELL, "Llamar al mesero", "Mesero")
}

private fun waiterFirstName(call: TableCall): String =
    call.takenBy
        ?.name
        ?.trim()
        ?.split(Regex("\\s+"))
        ?.firstOrNull()
        ?.takeIf { it.isNotEmpty() } ?: "Tu mesero"

/**
 * Solo quien hizo la llamada la cancela; si la hizo otro de la mesa el servidor no manda su nombre. En camino
 * también se puede: el servidor la cancela mientras siga abierta.
 */
fun canCancelMenuCall(call: TableCall): Boolean =
    (call.status == CallStatus.PENDIENTE || call.status == CallStatus.EN_CAMINO) && call.clientName != null

enum class MenuWaiterPhase { IDLE, CHOOSING, OPEN, UNAVAILABLE }

/**
 * Estado de la píldora "Llamar al mesero" del menú. Llama sin pedido: el servidor deja hacerlo a quien está
 * sentado en la mesa compartida, así que si responde que no lo está, lo sienta con [onJoinTable] y reintenta.
 */
class MenuWaiterCallState(
    private val spaceId: Int,
    private val onCurrent: suspend (Int) -> Result<TableCall?>,
    private val onCall: suspend (Int, CallReason, String?) -> Result<TableCall>,
    private val onCancel: suspend (TableCall) -> Result<TableCall>,
    private val onJoinTable: (suspend () -> Result<Unit>)?,
) {
    var phase by mutableStateOf(MenuWaiterPhase.IDLE)
        private set
    var call by mutableStateOf<TableCall?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var cooldown by mutableIntStateOf(0)
        private set

    /** La ventana con el estado de la llamada ya enviada, abierta al tocar la píldora. */
    var showsCall by mutableStateOf(false)
        private set

    val expanded: Boolean get() = phase == MenuWaiterPhase.CHOOSING || showsCall

    suspend fun refresh() {
        onCurrent(spaceId).fold(
            onSuccess = { current ->
                // Se cerró sin que la cancelara aquí, casi siempre porque la atendieron: el servidor pide 15 s.
                if (current == null && call != null && cooldown == 0) cooldown = ATTENDED_COOLDOWN_SECONDS
                call = current
                phase =
                    when {
                        current != null -> MenuWaiterPhase.OPEN
                        phase == MenuWaiterPhase.OPEN -> MenuWaiterPhase.IDLE
                        else -> phase
                    }
                if (current == null) showsCall = false
            },
            onFailure = { cause ->
                if (cause is CallsUnavailableException) phase = MenuWaiterPhase.UNAVAILABLE
            },
        )
    }

    fun tap() {
        when (phase) {
            MenuWaiterPhase.OPEN -> showsCall = !showsCall
            MenuWaiterPhase.CHOOSING -> close()
            MenuWaiterPhase.IDLE ->
                if (cooldown == 0) {
                    error = null
                    phase = MenuWaiterPhase.CHOOSING
                }
            MenuWaiterPhase.UNAVAILABLE -> Unit
        }
    }

    fun close() {
        if (phase == MenuWaiterPhase.CHOOSING) phase = MenuWaiterPhase.IDLE
        showsCall = false
        error = null
    }

    fun tick() {
        if (cooldown > 0) cooldown -= 1
    }

    /** Devuelve true si la llamada quedó hecha. */
    suspend fun send(reason: CallReason): Boolean {
        if (busy) return false
        busy = true
        error = null
        val result = callJoiningIfNeeded(reason)
        busy = false
        return result.fold(
            onSuccess = { created ->
                call = created
                phase = MenuWaiterPhase.OPEN
                showsCall = false
                true
            },
            onFailure = { cause ->
                // Al fallar el envío la píldora se queda con el error a la vista: quitarla dejaba al cliente sin
                // saber qué pasó ni cómo reintentar.
                when {
                    cause.errorCode() == RATE_LIMITED -> {
                        cooldown = callRetrySeconds(cause) ?: ATTENDED_COOLDOWN_SECONDS
                        phase = MenuWaiterPhase.IDLE
                        error = cause.toUserFacingMessage()
                    }
                    else -> error = cause.toUserFacingMessage()
                }
                false
            },
        )
    }

    suspend fun cancel() {
        val current = call ?: return
        if (busy) return
        busy = true
        error = null
        onCancel(current).fold(
            onSuccess = {
                call = null
                phase = MenuWaiterPhase.IDLE
                showsCall = false
                // Tras cancelar se puede volver a llamar de inmediato.
                cooldown = 0
            },
            onFailure = { cause -> error = cause.toUserFacingMessage() },
        )
        busy = false
    }

    private suspend fun callJoiningIfNeeded(reason: CallReason): Result<TableCall> {
        val first = onCall(spaceId, reason, null)
        val cause = first.exceptionOrNull() ?: return first
        if (cause.errorCode() != NOT_AT_TABLE) return first
        val join = onJoinTable ?: return Result.failure(IllegalStateException(NOT_AT_TABLE_MESSAGE))
        join().onFailure { return Result.failure(it) }
        return onCall(spaceId, reason, null)
    }

    companion object {
        /** La espera después de una llamada atendida; tras cancelar no hay. */
        const val ATTENDED_COOLDOWN_SECONDS = 15
        const val MAX_COOLDOWN_SECONDS = 60
        const val NOT_AT_TABLE = "NO_ACTIVE_ORDER_IN_SPACE"
        const val RATE_LIMITED = "RATE_LIMITED"
        const val NOT_AT_TABLE_MESSAGE = "Para llamar al mesero, únete a la mesa en Pedidos."
    }
}

private fun Throwable.errorCode(): String? =
    generateSequence(this) { it.cause }.firstNotNullOfOrNull { cause ->
        when (cause) {
            is OrderRepositoryException -> cause.code
            is ApiClientException -> cause.code
            else -> null
        }
    }

/**
 * Lo que falta para volver a llamar según el `429` del servidor: `Retry-After` o `details[0].segundos_restantes`.
 * `null` si el error no trae segundos.
 */
internal fun callRetrySeconds(error: Throwable): Int? {
    val api = generateSequence(error) { it.cause }.filterIsInstance<ApiClientException>().firstOrNull() ?: return null
    val fromDetails =
        ((api.details as? JsonArray)?.firstOrNull() as? JsonObject)
            ?.get("segundos_restantes")
            ?.jsonPrimitive
            ?.intOrNull
    return (api.retryAfterSeconds?.toInt() ?: fromDetails)?.coerceIn(1, MenuWaiterCallState.MAX_COOLDOWN_SECONDS)
}
