package com.vaiinilla.app.data.wallet

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.domain.model.WalletClient
import com.vaiinilla.app.domain.model.WalletData
import com.vaiinilla.app.domain.model.WalletMovement
import com.vaiinilla.app.domain.model.WalletReloadReceipt
import com.vaiinilla.app.domain.model.WalletSnapshot
import com.vaiinilla.app.domain.repository.WalletRepository
import com.vaiinilla.app.domain.repository.WalletRepositoryException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

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
                val wallet = envelope.data.wallet.toDomain()
                WalletData(
                    wallet =
                        if (wallet.userId.isNullOrBlank()) {
                            wallet.copy(userId = envelope.data.cliente.userId)
                        } else {
                            wallet
                        },
                    movements = envelope.data.movimientos.map(WalletMovementDto::toDomain),
                )
            }.mapApiErrors()

    override fun searchClients(query: String): Result<List<WalletClient>> =
        apiClient
            .get("wallets/clientes", mapOf("q" to query.trim()))
            .mapCatching { raw ->
                val envelope = json.decodeFromString<ClientListEnvelopeDto>(raw)
                require(envelope.error == null) { "La búsqueda de clientes contiene error." }
                envelope.data.map { it.toDomain() }
            }.mapApiErrors()

    override fun reloadCash(
        userId: String,
        amount: String,
        idempotencyKey: String,
    ): Result<WalletReloadReceipt> =
        apiClient
            .post(
                "wallets/$userId/recargas-efectivo",
                json.encodeToString(ReloadRequestDto(amount)),
                mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching { raw ->
                val envelope = json.decodeFromString<ReloadEnvelopeDto>(raw)
                require(envelope.error == null) { "La recarga contiene error." }
                envelope.data.toDomain()
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
    val data: WalletDetailDto,
    val error: JsonElement? = null,
)

@Serializable
private data class WalletDetailDto(
    val cliente: ClientDto,
    val wallet: WalletSnapshotDto,
    val movimientos: List<WalletMovementDto> = emptyList(),
)

@Serializable
private data class WalletSnapshotDto(
    val id: String? = null,
    @SerialName("usuario_id") val userId: String? = null,
    @SerialName("establecimiento_id") val establishmentId: String? = null,
    @SerialName("saldo") val visibleBalance: String,
    @SerialName("actualizado_en") val updatedAt: String? = null,
) {
    fun toDomain() =
        WalletSnapshot(
            id = id,
            userId = userId,
            establishmentId = establishmentId,
            visibleBalance = visibleBalance,
            updatedAt = updatedAt,
        )
}

@Serializable
private data class WalletMovementDto(
    val id: String,
    val tipo: String,
    val descripcion: String,
    val monto: String,
    @SerialName("saldo_posterior") val balanceAfter: String,
    @SerialName("pedido_id") val orderId: String? = null,
    @SerialName("creado_en") val createdAt: String,
) {
    fun toDomain() =
        WalletMovement(
            id = id,
            type = tipo,
            description = descripcion,
            amount = monto,
            balanceAfter = balanceAfter,
            orderId = orderId,
            createdAt = createdAt,
        )
}

@Serializable
private data class ClientListEnvelopeDto(
    val data: List<ClientDto>,
    val error: JsonElement? = null,
)

@Serializable
private data class ClientDto(
    @SerialName("usuario_id") val userId: String,
    val nombre: String,
    @SerialName("identificador_cliente") val contextualId: String? = null,
) {
    fun toDomain() = WalletClient(userId = userId, name = nombre, contextualId = contextualId)
}

@Serializable
private data class ReloadEnvelopeDto(
    val data: WalletReloadReceiptDto,
    val error: JsonElement? = null,
)

@Serializable
private data class WalletReloadReceiptDto(
    @SerialName("usuario_id") val userId: String,
    @SerialName("saldo_anterior") val previousBalance: String,
    @SerialName("recarga") val amount: String,
    @SerialName("saldo_nuevo") val newBalance: String,
    @SerialName("movimiento_id") val movementId: String,
    @SerialName("sesion_caja_id") val cashSessionId: String,
) {
    fun toDomain() =
        WalletReloadReceipt(
            userId = userId,
            previousBalance = previousBalance,
            amount = amount,
            newBalance = newBalance,
            movementId = movementId,
            cashSessionId = cashSessionId,
        )
}

@Serializable
private data class ReloadRequestDto(
    val monto: String,
)
