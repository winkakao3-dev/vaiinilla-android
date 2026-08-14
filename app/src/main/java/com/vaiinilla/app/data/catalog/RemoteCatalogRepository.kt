package com.vaiinilla.app.data.catalog

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.CatalogRepositoryException

class RemoteCatalogRepository(
    private val apiClient: VaiinillaApiClient,
    private val parser: ContractResponseParser,
) : CatalogRepository {
    override fun getCatalog(): Result<Catalog> =
        apiClient
            .get("catalogo")
            .mapCatching { parser.parseCatalog(it) }
            .mapApiErrors()

    override fun getOperationalStatus(): Result<OperationalStatus> =
        apiClient
            .get("estado-operativo")
            .mapCatching { parser.parseOperationalStatus(it) }
            .mapApiErrors()

    override fun createProduct(
        draft: CatalogProductDraft,
        idempotencyKey: String,
    ): Result<Product> =
        apiClient
            .post(
                "catalogo/productos",
                parser.encodeProductDraft(draft),
                mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching { parser.parseProduct(it) }
            .mapApiErrors()

    override fun setProductAvailability(
        productId: Int,
        available: Boolean,
        idempotencyKey: String,
    ): Result<Product> =
        apiClient
            .post(
                "catalogo/productos/$productId/disponibilidad",
                parser.encodeAvailability(available),
                mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching { parser.parseProduct(it) }
            .mapApiErrors()

    override fun uploadProductImage(
        productId: Int,
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        idempotencyKey: String,
    ): Result<Product> =
        apiClient
            .putMultipart(
                path = "catalogo/productos/$productId/imagen",
                fieldName = "imagen",
                filename = filename,
                mimeType = mimeType,
                bytes = bytes,
                headers = mapOf("Idempotency-Key" to idempotencyKey),
            ).mapCatching { parser.parseProduct(it) }
            .mapApiErrors()

    private fun <T> Result<T>.mapApiErrors(): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Result.failure(
                    when (error) {
                        is ApiClientException -> CatalogRepositoryException(error.code, error.message ?: error.code)
                        is CatalogRepositoryException -> error
                        else -> error
                    },
                )
            },
        )
}
