package com.vaiinilla.app.data.refunds

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.model.PendingRefund
import com.vaiinilla.app.domain.model.RefundMethod
import com.vaiinilla.app.domain.repository.RefundRepository

class RemoteRefundRepository(
    private val apiClient: VaiinillaApiClient,
    private val contractJson: RefundContractJson,
) : RefundRepository {
    override fun pending(): Result<List<PendingRefund>> =
        apiClient
            .get("devoluciones")
            .mapCatching(contractJson::parseList)

    override fun confirm(
        refundId: String,
        method: RefundMethod,
        note: String?,
        idempotencyKey: String,
    ): Result<PendingRefund> =
        apiClient
            .post(
                "devoluciones/$refundId/confirmaciones",
                contractJson.encodeConfirm(method, note),
                mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching(contractJson::parseRefund)
}
