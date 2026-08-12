package com.vaiinilla.app.data.wallet

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.model.WalletData
import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.model.WalletMovement
import com.vaiinilla.app.domain.model.WalletSnapshot
import com.vaiinilla.app.domain.repository.WalletRepository
import com.vaiinilla.app.domain.repository.WalletRepositoryException
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class RemoteWalletRepository(
    private val apiClient: VaiinillaApiClient,
) : WalletRepository {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = true
            isLenient = false
        }

    override fun getMyWallet(): Result<WalletData> =
        apiClient
            .get("wallets/me")
            .mapCatching { raw ->
                val envelope = json.decodeFromString<WalletEnvelopeDto>(raw)
                require(envelope.error == null) { "La respuesta de wallet contiene error." }
                WalletData(
                    wallet = envelope.data.wallet.toDomain(),
                    movements = envelope.data.movements.map(WalletMovementDto::toDomain),
                )
            }.mapApiErrors()

    override fun searchClients(query: String): Result<List<WalletClient>> =
        apiClient
            .get("wallets/clientes", mapOf("query" to query))
            .mapCatching { raw ->
                val envelope = json.decodeFromString<ClientListEnvelopeDto>(raw)
                require(envelope.error == null) { "La búsqueda de clientes contiene error." }
                envelope.data.map { WalletClient(it.userId, it.nombre, it.matricula, it.contextualId) }
            }.mapApiErrors()

    override fun reloadCash(userId: String, amount: String, idempotencyKey: String): Result<WalletData> =
        apiClient
            .post(
                "wallets/$userId/recargas-efectivo",
                json.encodeToString(ReloadRequestDto(amount)),
                mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching { raw ->
                val envelope = json.decodeFromString<ReloadEnvelopeDto>(raw)
                require(envelope.error == null) { "La recarga contiene error." }
                WalletData(envelope.data.wallet.toDomain(), emptyList())
            }.mapApiErrors()

    private fun <T> Result<T>.mapApiErrors(): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Result.failure(
                    when (error) {
                        is ApiClientException -> WalletRepositoryException(error.code, error.message ?: error.code)
                        is WalletRepositoryException -> error
                        else -> error
                    },
                )
            },
        )
}

@Serializable
private data class WalletEnvelopeDto(
    val data: WalletDataDto,
    val error: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
private data class WalletDataDto(
    val wallet: WalletSnapshotDto,
    @SerialName("movimientos") val movements: List<WalletMovementDto> = emptyList(),
)

@Serializable
private data class WalletSnapshotDto(
    val id: String? = null,
    @SerialName("usuario_id") val userId: String? = null,
    @SerialName("establecimiento_id") val establishmentId: String? = null,
    @SerialName("saldo_comision_pagada") val paidCommissionBalance: String,
    @SerialName("saldo_comision_pendiente") val pendingCommissionBalance: String,
    @SerialName("saldo_visible") val visibleBalance: String,
    @SerialName("actualizado_en") val updatedAt: String? = null,
) {
    fun toDomain() =
        WalletSnapshot(
            id,
            userId,
            establishmentId,
            paidCommissionBalance,
            pendingCommissionBalance,
            visibleBalance,
            updatedAt,
        )
}

@Serializable
private data class WalletMovementDto(
    val id: String,
    val tipo: String,
    val monto: String,
    val bucket: String,
    @SerialName("pedido_id") val orderId: String? = null,
    @SerialName("registrado_por") val registeredBy: String? = null,
    @SerialName("idempotency_key") val idempotencyKey: String,
    @SerialName("creado_en") val createdAt: String,
) {
    fun toDomain() =
        WalletMovement(id, tipo, monto, bucket, orderId, registeredBy, idempotencyKey, createdAt)
}

@Serializable
private data class ClientListEnvelopeDto(
    val data: List<ClientDto>,
    val error: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
private data class ClientDto(
    @SerialName("usuario_id") val userId: String,
    val nombre: String,
    val matricula: String? = null,
    @SerialName("identificador_cliente") val contextualId: String? = null,
)

@Serializable
private data class ReloadEnvelopeDto(
    val data: ReloadDataDto,
    val error: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
private data class ReloadDataDto(
    val wallet: WalletSnapshotDto,
)

@Serializable
private data class ReloadRequestDto(
    val monto: String,
)
