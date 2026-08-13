package com.vaiinilla.app.domain.model

data class WalletSnapshot(
    val id: String?,
    val userId: String?,
    val establishmentId: String?,
    val visibleBalance: String,
    val updatedAt: String?,
)

data class WalletMovement(
    val id: String,
    val type: String,
    val description: String,
    val amount: String,
    val balanceAfter: String,
    val orderId: String?,
    val createdAt: String,
)

data class WalletData(
    val wallet: WalletSnapshot,
    val movements: List<WalletMovement>,
)

data class WalletClient(
    val userId: String,
    val name: String,
    val contextualId: String?,
)

data class WalletReloadReceipt(
    val userId: String,
    val previousBalance: String,
    val amount: String,
    val newBalance: String,
    val movementId: String,
    val cashSessionId: String,
)
