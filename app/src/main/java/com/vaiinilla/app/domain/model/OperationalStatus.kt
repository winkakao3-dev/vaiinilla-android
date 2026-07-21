package com.vaiinilla.app.domain.model

data class OperationalStatus(
    val acceptingOrders: Boolean,
    val cashSessionOpen: Boolean,
    val cashierOnline: Boolean,
    val kitchenOnline: Boolean,
    val estimatedTimeMinutes: Int,
    val consultedAt: String,
)
