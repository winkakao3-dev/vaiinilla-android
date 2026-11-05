package com.vaiinilla.app.data.assistant

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.guest.GuestClientAccess
import com.vaiinilla.app.domain.assistant.CustomerAssistantReply
import com.vaiinilla.app.domain.assistant.CustomerAssistantRepository
import com.vaiinilla.app.domain.assistant.CustomerAssistantTurn

/**
 * Con cuenta va la sesión activa; sin cuenta, la del invitado (igual que "Llamar al mesero"). El invitado que aún no
 * se da de alta primero da su nombre en la plática ([needsGuestName] / [enterAsGuest]).
 */
class RemoteCustomerAssistantRepository(
    private val apiClient: VaiinillaApiClient,
    private val guestAccess: GuestClientAccess? = null,
    private val json: CustomerAssistantJson = CustomerAssistantJson(),
) : CustomerAssistantRepository {
    override fun reply(
        turns: List<CustomerAssistantTurn>,
        establishmentId: String?,
    ): Result<CustomerAssistantReply> {
        val body = json.encodeTurns(turns, establishmentId)
        val guestSlug = guestAccess?.guestSlug()
        val raw =
            when {
                guestSlug == null -> apiClient.post(PATH, body)
                guestAccess?.hasIdentity() != true ->
                    Result.failure(ApiClientException(NEEDS_NAME, "Dinos tu nombre para platicar.", 401))
                else -> requireNotNull(guestAccess).withBearer(guestSlug) { apiClient.postWithBearer(it, PATH, body) }
            }
        return raw.mapCatching(json::parseReply)
    }

    override fun needsGuestName(): Boolean {
        val access = guestAccess ?: return false
        return access.guestSlug() != null && !access.hasIdentity()
    }

    override fun enterAsGuest(name: String): Result<Unit> {
        val access = guestAccess ?: return Result.failure(IllegalStateException("Sin compra de invitado."))
        val slug = access.guestSlug() ?: return Result.failure(IllegalStateException("Sin compra de invitado."))
        return access.session.enter(slug, name)
    }

    companion object {
        const val PATH = "asistente-cliente"
        const val NEEDS_NAME = "GUEST_NAME_REQUIRED"
    }
}
