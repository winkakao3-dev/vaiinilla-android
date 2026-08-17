package com.vaiinilla.app.data.account

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.account.AccountDeletionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAccountDeletionRepository
    @Inject
    constructor(
        private val apiClient: VaiinillaApiClient,
    ) : AccountDeletionRepository {
        override suspend fun deleteAccount(
            firebaseIdToken: String,
            idempotencyKey: String,
        ): Result<Unit> =
            withContext(Dispatchers.IO) {
                apiClient
                    .deleteWithBearerExpecting200(
                        bearer = firebaseIdToken,
                        path = "identidad/cuenta",
                        body = BODY,
                        headers = mapOf("Idempotency-Key" to idempotencyKey),
                    ).map { }
            }

        private companion object {
            const val BODY = "{\"confirmacion\":\"ELIMINAR\"}"
        }
    }
