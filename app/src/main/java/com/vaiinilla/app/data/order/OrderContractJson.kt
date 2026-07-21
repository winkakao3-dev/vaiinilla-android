package com.vaiinilla.app.data.order

import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDetail
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class OrderContractJson @Inject constructor() {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = true
        isLenient = false
        encodeDefaults = true
    }

    fun encodeCreateRequest(request: CreateOrderRequest): String = json.encodeToString(request.toDto())

    fun parseOrderDetail(raw: String): OrderDetail {
        val envelope = json.decodeFromString<OrderDetailEnvelopeDto>(raw)
        require(envelope.error == null) { "El fixture de pedido contiene error." }
        return envelope.data.toDomain()
    }
}
