package com.vaiinilla.app.data.catalog

import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.core.network.idempotencyHeaders
import com.vaiinilla.app.core.network.mapApiFailure
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.domain.offline.OfflineStaffOrderRules
import com.vaiinilla.app.domain.repository.CatalogRepository
import com.vaiinilla.app.domain.repository.CatalogRepositoryException
import com.vaiinilla.app.domain.repository.StoreImageKind
import com.vaiinilla.app.domain.repository.StoreImages

class RemoteCatalogRepository(
    private val apiClient: VaiinillaApiClient,
    private val parser: ContractResponseParser,
) : CatalogRepository {
    /**
     * Sin internet, Mesero y Caja siguen tomando pedidos con el último menú que llegó a este teléfono (plan «Vaiinilla
     * sin internet», fase 1). El servidor recalcula precio y disponibilidad cuando el pedido llega.
     */
    override fun getCatalog(): Result<Catalog> =
        apiClient
            .get("catalogo")
            .recoverCatching { error ->
                if (!OfflineStaffOrderRules.isConnectionProblem(error)) throw error
                apiClient.readCachedGet("catalogo") ?: throw error
            }.mapCatching { parser.parseCatalog(it) }
            .mapApiErrors()

    override fun getOperationalStatus(): Result<OperationalStatus> =
        apiClient
            .get("estado-operativo")
            .mapCatching { parser.parseOperationalStatus(it) }
            .mapApiErrors()
            .onSuccess { Venue.update(it.businessType) }

    override fun createProduct(
        draft: CatalogProductDraft,
        idempotencyKey: String,
    ): Result<Product> =
        apiClient
            .post(
                "catalogo/productos",
                parser.encodeProductDraft(draft),
                idempotencyHeaders(idempotencyKey),
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
                idempotencyHeaders(idempotencyKey),
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
                headers = idempotencyHeaders(idempotencyKey),
            ).mapCatching { parser.parseProduct(it) }
            .mapApiErrors()

    override fun getStoreImages(): Result<StoreImages> =
        apiClient
            .get("establecimiento/imagenes")
            .mapCatching { parser.parseStoreImages(it) }
            .mapApiErrors()

    override fun uploadStoreImage(
        kind: StoreImageKind,
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        idempotencyKey: String,
    ): Result<StoreImages> =
        apiClient
            .putMultipart(
                path = "establecimiento/${kind.wireValue}",
                fieldName = "imagen",
                filename = filename,
                mimeType = mimeType,
                bytes = bytes,
                headers = idempotencyHeaders(idempotencyKey),
            ).mapCatching { parser.parseStoreImages(it) }
            .mapApiErrors()

    override fun removeStoreImage(
        kind: StoreImageKind,
        idempotencyKey: String,
    ): Result<StoreImages> =
        apiClient
            .delete(
                path = "establecimiento/${kind.wireValue}",
                headers = idempotencyHeaders(idempotencyKey),
            ).mapCatching { parser.parseStoreImages(it) }
            .mapApiErrors()

    private fun <T> Result<T>.mapApiErrors(): Result<T> =
        mapApiFailure {
            CatalogRepositoryException(
                it.code,
                it.message ?: it.code,
                it,
            )
        }
}
