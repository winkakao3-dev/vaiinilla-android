package com.vaiinilla.app.data.sharedtable

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.model.SharedTable
import com.vaiinilla.app.domain.repository.SharedTableRepository

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

    override fun leave(): Result<Unit> = apiClient.postWithoutBody("mesas/actual/salida").map { }

    override fun claim(
        folio: Int,
        payIt: Boolean,
    ): Result<SharedTable> =
        apiClient
            .post("mesas/actual/reclamos", contractJson.encodeClaim(folio, payIt))
            .mapCatching { requireNotNull(contractJson.parseTable(it)) { "La mesa no llegó." } }
}
