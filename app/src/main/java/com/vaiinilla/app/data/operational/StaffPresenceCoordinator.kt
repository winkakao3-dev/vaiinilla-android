package com.vaiinilla.app.data.operational

import com.vaiinilla.app.BuildConfig
import com.vaiinilla.app.core.config.EffectiveDataSourceResolver
import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.data.auth.FirebaseSeedAuthRepository
import com.vaiinilla.app.domain.auth.SeedAccounts
import com.vaiinilla.app.domain.model.OperationalRole
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaffPresenceCoordinator
    @Inject
    constructor(
        private val dataSourceResolver: EffectiveDataSourceResolver,
        private val apiClient: HttpVaiinillaApiClient,
        private val seedAuthRepository: FirebaseSeedAuthRepository,
    ) {
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        fun primeStaffPresence(): Result<Unit> {
            // This is a debug-only single-device convenience. In normal builds the
            // backend remains the sole authority for staff availability; the student
            // flow must not depend on embedded or missing seed credentials.
            if (
                !dataSourceResolver.usesNetwork() ||
                !BuildConfig.SEED_AUTH_ENABLED ||
                !SeedAccounts.isConfigured()
            ) {
                return Result.success(Unit)
            }

            val staffRoles =
                listOf(
                    OperationalRole.CASHIER to "android-cajero",
                    OperationalRole.KITCHEN to "android-cocina",
                )

            var sent = 0
            var failed = 0
            var lastError: String? = null

            for ((role, deviceId) in staffRoles) {
                val tokenResult = runBlocking { seedAuthRepository.ensureRoleJwtForHeartbeat(role) }
                val token = tokenResult.getOrNull()?.trim().orEmpty()
                if (token.isEmpty()) {
                    failed++
                    lastError = tokenResult.exceptionOrNull()?.message ?: "missing_token_${role.name}"
                    continue
                }

                apiClient
                    .postWithAccessToken(
                        accessToken = token,
                        path = "latidos",
                        body =
                            json.encodeToString(
                                StaffHeartbeatRequestDto(
                                    device = deviceId,
                                    role = role.wireValue,
                                ),
                            ),
                    ).fold(
                        onSuccess = { sent++ },
                        onFailure = { error ->
                            failed++
                            lastError = error.message ?: error.javaClass.simpleName
                        },
                    )
            }

            return if (sent > 0) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(lastError ?: "No se pudo avisar a Caja o Cocina."))
            }
        }
    }

@Serializable
private data class StaffHeartbeatRequestDto(
    @SerialName("dispositivo") val device: String,
    @SerialName("rol") val role: String,
)
