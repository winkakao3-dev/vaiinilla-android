package com.vaiinilla.app.data.operational

import com.vaiinilla.app.core.config.EffectiveDataSourceResolver
import com.vaiinilla.app.core.network.HttpVaiinillaApiClient
import com.vaiinilla.app.core.security.RoleAccessTokenStore
import com.vaiinilla.app.domain.model.OperationalRole
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class StaffPresenceCoordinator @Inject constructor(
    private val dataSourceResolver: EffectiveDataSourceResolver,
    private val roleAccessTokenStore: RoleAccessTokenStore,
    private val apiClient: HttpVaiinillaApiClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun primeStaffPresence(): Result<Unit> {
        if (!dataSourceResolver.usesNetwork()) {
            return Result.success(Unit)
        }

        val staffRoles = listOf(
            OperationalRole.CASHIER to "android-cajero",
            OperationalRole.KITCHEN to "android-cocina",
        )

        var sent = 0
        var failed = 0
        var lastError: String? = null

        for ((role, deviceId) in staffRoles) {
            val token = roleAccessTokenStore.tokenFor(role)?.trim().orEmpty()
            if (token.isEmpty()) {
                failed++
                lastError = "missing_token_${role.name}"
                continue
            }

            apiClient.postWithAccessToken(
                accessToken = token,
                path = "latidos",
                body = json.encodeToString(
                    StaffHeartbeatRequestDto(
                        device = deviceId,
                        role = role.wireValue,
                    ),
                ),
                headers = mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
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
