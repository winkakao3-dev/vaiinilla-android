package com.vaiinilla.app.data.sharedtable

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.guest.GuestCheckoutPolicy
import com.vaiinilla.app.data.guest.GuestReservationSession
import com.vaiinilla.app.data.guest.GuestSessionStore
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.repository.SharedTableRepository
import java.net.URLEncoder

/**
 * Mesa compartida. Con cuenta usa la sesión activa; sin cuenta (compra de invitado) usa la sesión del invitado
 * del negocio del QR, que el backend firma con rol cliente igual que la de una cuenta. Antes el invitado iba con
 * la sesión activa, que no tiene, y todo fallaba con "Falta el contexto operativo. Inicia sesión…".
 */
class RemoteSharedTableRepository(
    private val apiClient: VaiinillaApiClient,
    private val contractJson: SharedTableContractJson,
    private val guestStore: GuestSessionStore? = null,
    private val guestPolicy: GuestCheckoutPolicy? = null,
    private val guestSession: GuestReservationSession? = null,
) : SharedTableRepository {
    override fun current(): Result<SharedTable?> {
        val guest = guest() ?: return apiClient.get("mesas/actual").mapCatching(contractJson::parseTable)
        // Un invitado que aún no dio su nombre no está sentado en ninguna mesa.
        if (!guest.session.hasIdentity()) return Result.success(null)
        return guest.get("mesas/actual").mapCatching(contractJson::parseTable)
    }

    override fun join(
        qrToken: String,
        alias: String,
    ): Result<SharedTable> {
        val body = contractJson.encodeJoin(qrToken, alias)
        val guest = guest()
        val raw =
            if (guest == null) {
                apiClient.post("mesas/unirse", body)
            } else {
                // Sin cuenta, el nombre con que se une es el del invitado: si aún no lo dio, se da de alta con él.
                if (!guest.session.hasIdentity()) {
                    guest.session.enter(guest.slug, alias).onFailure { return Result.failure(it) }
                }
                guest.post("mesas/unirse", body)
            }
        return raw.mapCatching { requireNotNull(contractJson.parseTable(it)) { "La mesa no llegó." } }
    }

    override fun leave(): Result<Unit> {
        val guest = guest() ?: return apiClient.postWithoutBody("mesas/actual/salida").map { }
        return guest.post("mesas/actual/salida", null).map { }
    }

    override fun othersAt(qrToken: String): Result<List<String>> {
        val path = "mesas/espacio/${URLEncoder.encode(qrToken, Charsets.UTF_8.name())}"
        val guest = guest() ?: return apiClient.get(path).mapCatching(contractJson::parseOthersAtSpace)
        if (!guest.session.hasIdentity()) return Result.success(emptyList())
        return guest.get(path).mapCatching(contractJson::parseOthersAtSpace)
    }

    override fun claim(
        folio: Int,
        payIt: Boolean,
    ): Result<SharedTable> {
        val body = contractJson.encodeClaim(folio, payIt)
        val guest = guest()
        val raw = guest?.post("mesas/actual/reclamos", body) ?: apiClient.post("mesas/actual/reclamos", body)
        return raw.mapCatching { requireNotNull(contractJson.parseTable(it)) { "La mesa no llegó." } }
    }

    /** El invitado del negocio del QR cuando se compra sin cuenta; `null` con cuenta. */
    private fun guest(): Guest? {
        val store = guestStore ?: return null
        val policy = guestPolicy ?: return null
        val session = guestSession ?: return null
        val venue = store.readVenue() ?: return null
        if (!policy.allows(venue)) return null
        return Guest(venue.establishment.slug, session)
    }

    private inner class Guest(
        val slug: String,
        val session: GuestReservationSession,
    ) {
        fun get(path: String): Result<String> =
            session.withBearer(slug) { bearer -> apiClient.getWithBearer(bearer, path) }

        fun post(
            path: String,
            body: String?,
        ): Result<String> = session.withBearer(slug) { bearer -> apiClient.postWithBearer(bearer, path, body) }
    }
}
