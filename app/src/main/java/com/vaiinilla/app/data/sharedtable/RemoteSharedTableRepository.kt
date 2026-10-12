package com.vaiinilla.app.data.sharedtable

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.model.TableSession
import com.vaiinilla.app.domain.repository.SharedTableRepository
import java.net.URLEncoder

class RemoteSharedTableRepository(
    private val apiClient: VaiinillaApiClient,
    private val contractJson: SharedTableContractJson,
) : SharedTableRepository {
    override fun current(): Result<SharedTable?> = apiClient.get("mesas/actual").mapCatching(contractJson::parseTable)

    override fun join(
        qrToken: String,
        alias: String,
    ): Result<SharedTable> =
        apiClient
            .post("mesas/unirse", contractJson.encodeJoin(qrToken, alias))
            .mapCatching { requireNotNull(contractJson.parseTable(it)) { "La mesa no llegó." } }

    override fun session(qrToken: String): Result<TableSession> =
        apiClient
            .get("mesas/espacio/${encodePathSegment(qrToken)}")
            .mapCatching(contractJson::parseSession)

    override fun continueAs(
        qrToken: String,
        participantId: String,
    ): Result<SharedTable> =
        apiClient
            .post("mesas/unirse", contractJson.encodeContinue(qrToken, participantId))
            .mapCatching { requireNotNull(contractJson.parseTable(it)) { "La mesa no llegó." } }

    override fun leave(): Result<Unit> = apiClient.postWithoutBody("mesas/actual/salida").map { }

    override fun claim(
        folio: Int,
        payIt: Boolean,
    ): Result<SharedTable> =
        apiClient
            .post("mesas/actual/reclamos", contractJson.encodeClaim(folio, payIt))
            .mapCatching { requireNotNull(contractJson.parseTable(it)) { "La mesa no llegó." } }
}

/** Como encodeURIComponent: el token del QR viaja en la ruta, y el espacio de "+" es "%20". */
private fun encodePathSegment(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
