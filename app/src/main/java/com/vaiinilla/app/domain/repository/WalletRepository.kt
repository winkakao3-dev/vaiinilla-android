package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.model.WalletData

interface WalletRepository {
    fun getMyWallet(): Result<WalletData>

    fun searchClients(query: String): Result<List<WalletClient>>

    fun reloadCash(
        userId: String,
        amount: String,
        idempotencyKey: String,
    ): Result<WalletData>
}

class WalletRepositoryException(
    val code: String,
    message: String,
) : IllegalStateException(message)
