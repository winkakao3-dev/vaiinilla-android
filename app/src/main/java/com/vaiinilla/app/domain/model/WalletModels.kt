package com.vaiinilla.app.domain.model

data class WalletSnapshot(
    val id: String?,
    val userId: String?,
    val establishmentId: String?,
    val paidCommissionBalance: String,
    val pendingCommissionBalance: String,
    val visibleBalance: String,
    val updatedAt: String?,
)

data class WalletMovement(
    val id: String,
    val type: String,
    val amount: String,
    val bucket: String,
    val orderId: String?,
    val registeredBy: String?,
    val idempotencyKey: String,
    val createdAt: String,
)

data class WalletData(
    val wallet: WalletSnapshot,
    val movements: List<WalletMovement>,
)

data class WalletClient(
    val userId: String,
    val name: String,
    val enrollment: String?,
    val contextualId: String?,
)
