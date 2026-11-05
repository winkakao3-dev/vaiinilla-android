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
 * La mesa desde la que se puede llamar al mesero en el menú: la del QR o el código con el que entró, la mesa
 * compartida donde ya está sentado o la cancha que rentó en la app y está jugando (el servidor acepta a quien
 * tiene la reserva en curso). Para llevar, en una escuela o sin cuenta no hay a quién llamar.
 */
fun menuWaiterSpaceId(
    guestSpaceId: Int?,
    sharedTableSpaceId: Int?,
    signedIn: Boolean,
    rentedCourtId: Int? = null,
): Int? = if (signedIn) guestSpaceId ?: sharedTableSpaceId ?: rentedCourtId else null

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
            // El cliente nunca ve el nombre del mesero: cuida al personal (decisión de Kao, 3 nov).
            return MenuWaiterPillLabel(MenuWaiterIcon.WALKING, "Tu mesero va en camino", "Mesero va")
        }
        return MenuWaiterPillLabel(MenuWaiterIcon.RINGING, "Llamando al mesero…", "Llamando…")
    }
    if (cooldown > 0) return MenuWaiterPillLabel(MenuWaiterIcon.BELL, "Otra vez en $cooldown s", "$cooldown s")
    return MenuWaiterPillLabel(MenuWaiterIcon.BELL, "Llamar al mesero", "Mesero")
}

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
    /**
     * Sin cuenta y sin nombre: en lugar de sentarlo como «Cliente», la hoja le pide su nombre (con el aviso de
     * términos) y se une a la mesa con él. Dos invitados ya no chocan con el mismo nombre.
     */
    onJoinAs: (suspend (String) -> Result<Unit>)? = null,
) {
    /**
     * Se actualiza desde la composición: al darse de alta el invitado ya tiene nombre y deja de hacer falta.
     * Cambiarlo aquí no recrea el estado, así que la llamada que va en curso no se pierde.
     */
    var onJoinAs: (suspend (String) -> Result<Unit>)? = onJoinAs

    /** La hoja pide el nombre antes de llamar; [pendingReason] es lo que eligió. */
    var askingName by mutableStateOf(false)
        private set
    private var pendingReason: CallReason? = null

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
        askingName = false
        pendingReason = null
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
        return finish(result)
    }

    /** Con el nombre escrito: se une a la mesa con él y hace la llamada que había elegido. */
    suspend fun sendWithName(name: String): Boolean {
        val reason = pendingReason ?: return false
        val join = onJoinAs ?: return false
        val clean = name.trim().replace(Regex("\\s+"), " ")
        if (busy) return false
        if (clean.isEmpty()) {
            error = NAME_REQUIRED_MESSAGE
            return false
        }
        busy = true
        error = null
        val joined = join(clean.take(SHARED_TABLE_ALIAS_MAX))
        if (joined.isFailure) {
            busy = false
            val cause = joined.exceptionOrNull()
            error = if (cause?.errorCode() == NAME_TAKEN) NAME_TAKEN_MESSAGE else cause?.toUserFacingMessage()
            return false
        }
        // Ya está sentado: un reintento vuelve a los motivos y llama directo, sin unirse de nuevo.
        askingName = false
        pendingReason = null
        val result = onCall(spaceId, reason, null)
        busy = false
        return finish(result)
    }

    private fun finish(result: Result<TableCall>): Boolean {
        if (result.exceptionOrNull() === NameNeeded) return false
        return result.fold(
            onSuccess = { created ->
                call = created
                phase = MenuWaiterPhase.OPEN
                // La hoja se queda abierta con «Ya le avisamos a tu mesero» (Doop L05).
                showsCall = true
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
        if (onJoinAs != null) {
            pendingReason = reason
            askingName = true
            return Result.failure(NameNeeded)
        }
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
        const val NAME_TAKEN = "PARTICIPANT_NAME_TAKEN"
        const val NAME_TAKEN_MESSAGE = "Alguien en la mesa ya usa ese nombre. Escribe otro."
        const val NAME_REQUIRED_MESSAGE = "Escribe tu nombre para avisarle al mesero."
    }

    /** No es un error: la hoja pide el nombre antes de llamar. */
    private object NameNeeded : IllegalStateException("Falta el nombre")
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
