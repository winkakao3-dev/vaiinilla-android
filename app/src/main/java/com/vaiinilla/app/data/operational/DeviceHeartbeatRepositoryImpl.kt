package com.vaiinilla.app.data.operational

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.domain.repository.DeviceHeartbeatRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RemoteDeviceHeartbeatRepository(
    private val apiClient: VaiinillaApiClient,
) : DeviceHeartbeatRepository {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override fun sendHeartbeat(
        deviceId: String,
        role: OperationalRole,
        idempotencyKey: String,
    ): Result<Unit> {
        if (role == OperationalRole.CLIENT) {
            return Result.success(Unit)
        }
        return apiClient
            .post(
                path = "latidos",
                body =
                    json.encodeToString(
                        HeartbeatRequestDto(
                            device = deviceId,
                            role = role.wireValue,
                        ),
                    ),
            ).fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { error ->
                    Result.failure(
                        if (error is ApiClientException) {
                            IllegalStateException("${error.code}: ${error.message}")
                        } else {
                            error
                        },
                    )
                },
            )
    }
}

class NoOpDeviceHeartbeatRepository : DeviceHeartbeatRepository {
    override fun sendHeartbeat(
        deviceId: String,
        role: OperationalRole,
        idempotencyKey: String,
    ): Result<Unit> = Result.success(Unit)
}

@Serializable
private data class HeartbeatRequestDto(
    @SerialName("dispositivo") val device: String,
    @SerialName("rol") val role: String,
)
