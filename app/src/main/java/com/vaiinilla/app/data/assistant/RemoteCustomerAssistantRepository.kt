package com.vaiinilla.app.data.assistant

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.guest.GuestClientAccess
import com.vaiinilla.app.domain.assistant.CustomerAssistantReply
import com.vaiinilla.app.domain.assistant.CustomerAssistantRepository
import com.vaiinilla.app.domain.assistant.CustomerAssistantTurn

/**
 * Con cuenta va la sesión activa; sin cuenta, la del invitado (igual que "Llamar al mesero"). El invitado que aún
 * no se sienta no tiene sesión de cliente: se le pide unirse a la mesa primero.
 */
class RemoteCustomerAssistantRepository(
    private val apiClient: VaiinillaApiClient,
    private val guestAccess: GuestClientAccess? = null,
    private val json: CustomerAssistantJson = CustomerAssistantJson(),
) : CustomerAssistantRepository {
    override fun reply(turns: List<CustomerAssistantTurn>): Result<CustomerAssistantReply> {
        val body = json.encodeTurns(turns)
        val guestSlug = guestAccess?.guestSlug()
        val raw =
            when {
                guestSlug == null -> apiClient.post(PATH, body)
                guestAccess?.hasIdentity() != true ->
                    Result.failure(
                        ApiClientException(NOT_AT_TABLE, "Únete a la mesa para platicar con el asistente.", 403),
                    )
                else -> requireNotNull(guestAccess).withBearer(guestSlug) { apiClient.postWithBearer(it, PATH, body) }
            }
        return raw.mapCatching(json::parseReply)
    }

    private companion object {
        const val PATH = "asistente-cliente"
        const val NOT_AT_TABLE = "NO_ACTIVE_ORDER_IN_SPACE"
    }
}
