package com.vaiinilla.app.data.sharedtable

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.guest.GuestClientAccess
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.repository.SharedTableRepository
import java.net.URLEncoder

/**
 * Mesa compartida. Con cuenta usa la sesión activa; sin cuenta (compra de invitado) usa la sesión del invitado
 * del negocio del QR. Antes el invitado iba con la sesión activa, que no tiene, y todo fallaba con
 * "Falta el contexto operativo. Inicia sesión…".
 */
class RemoteSharedTableRepository(
    private val apiClient: VaiinillaApiClient,
    private val contractJson: SharedTableContractJson,
    private val guestAccess: GuestClientAccess? = null,
) : SharedTableRepository {
    override fun current(): Result<SharedTable?> {
        val slug = guestSlug() ?: return apiClient.get("mesas/actual").mapCatching(contractJson::parseTable)
        // Un invitado que aún no dio su nombre no está sentado en ninguna mesa.
        if (guestAccess?.hasIdentity() != true) return Result.success(null)
        return guestGet(slug, "mesas/actual").mapCatching(contractJson::parseTable)
    }

    override fun join(
        qrToken: String,
        alias: String,
    ): Result<SharedTable> {
        val body = contractJson.encodeJoin(qrToken, alias)
        val slug = guestSlug()
        val raw =
            if (slug == null) {
                apiClient.post("mesas/unirse", body)
            } else {
                // Sin cuenta, el nombre con que se une es el del invitado: si aún no lo dio, se da de alta con él.
                val session = requireNotNull(guestAccess).session
                if (!session.hasIdentity()) session.enter(slug, alias).onFailure { return Result.failure(it) }
                guestPost(slug, "mesas/unirse", body)
            }
        return raw.mapCatching { requireNotNull(contractJson.parseTable(it)) { "La mesa no llegó." } }
    }

    override fun leave(): Result<Unit> {
        val slug = guestSlug() ?: return apiClient.postWithoutBody("mesas/actual/salida").map { }
        return guestPost(slug, "mesas/actual/salida", null).map { }
    }

    override fun othersAt(qrToken: String): Result<List<String>> {
        val path = "mesas/espacio/${URLEncoder.encode(qrToken, Charsets.UTF_8.name())}"
        val slug = guestSlug() ?: return apiClient.get(path).mapCatching(contractJson::parseOthersAtSpace)
        if (guestAccess?.hasIdentity() != true) return Result.success(emptyList())
        return guestGet(slug, path).mapCatching(contractJson::parseOthersAtSpace)
    }

    override fun claim(
        folio: Int,
        payIt: Boolean,
    ): Result<SharedTable> {
        val body = contractJson.encodeClaim(folio, payIt)
        val slug = guestSlug()
        val raw =
            if (slug == null) {
                apiClient.post("mesas/actual/reclamos", body)
            } else {
                guestPost(slug, "mesas/actual/reclamos", body)
            }
        return raw.mapCatching { requireNotNull(contractJson.parseTable(it)) { "La mesa no llegó." } }
    }

    private fun guestSlug(): String? = guestAccess?.guestSlug()

    private fun guestGet(
        slug: String,
        path: String,
    ): Result<String> = requireNotNull(guestAccess).withBearer(slug) { apiClient.getWithBearer(it, path) }

    private fun guestPost(
        slug: String,
        path: String,
        body: String?,
    ): Result<String> = requireNotNull(guestAccess).withBearer(slug) { apiClient.postWithBearer(it, path, body) }
}
