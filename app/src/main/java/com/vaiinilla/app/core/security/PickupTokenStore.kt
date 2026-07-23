package com.vaiinilla.app.core.security

import com.vaiinilla.app.domain.model.OrderDetail

interface PickupTokenStore {
    fun save(orderId: String, pickupToken: String)
    fun read(orderId: String): String?
    fun attach(order: OrderDetail): OrderDetail {
        if (!order.pickupToken.isNullOrBlank()) {
            save(order.summary.id, order.pickupToken)
            return order
        }
        val cached = read(order.summary.id) ?: return order
        return order.copy(pickupToken = cached)
    }
}

class InMemoryPickupTokenStore : PickupTokenStore {
    private val tokens = linkedMapOf<String, String>()

    override fun save(orderId: String, pickupToken: String) {
        if (orderId.isBlank() || pickupToken.isBlank()) return
        tokens[orderId] = pickupToken
    }

    override fun read(orderId: String): String? = tokens[orderId]
}
