package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.CatalogProductDraft
import com.vaiinilla.app.domain.model.OperationalStatus
import com.vaiinilla.app.domain.model.Product

interface CatalogRepository {
    fun getCatalog(): Result<Catalog>

    fun getOperationalStatus(): Result<OperationalStatus>

    fun createProduct(
        draft: CatalogProductDraft,
        idempotencyKey: String,
    ): Result<Product>

    fun setProductAvailability(
        productId: Int,
        available: Boolean,
        idempotencyKey: String,
    ): Result<Product>

    fun uploadProductImage(
        productId: Int,
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        idempotencyKey: String,
    ): Result<Product>

    /** El logo y la foto del negocio que ven los clientes; Caja los cambia desde la app. */
    fun getStoreImages(): Result<StoreImages> =
        Result.failure(UnsupportedOperationException("Sin imágenes del negocio."))

    fun uploadStoreImage(
        kind: StoreImageKind,
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        idempotencyKey: String,
    ): Result<StoreImages> = Result.failure(UnsupportedOperationException("Sin imágenes del negocio."))

    fun removeStoreImage(
        kind: StoreImageKind,
        idempotencyKey: String,
    ): Result<StoreImages> = Result.failure(UnsupportedOperationException("Sin imágenes del negocio."))
}

/** Lo que ya está puesto: null si el negocio no lo ha subido. */
data class StoreImages(
    val photoUrl: String?,
    val logoUrl: String?,
)

enum class StoreImageKind(
    val wireValue: String,
) {
    LOGO("logo"),
    PHOTO("foto"),
}

class CatalogRepositoryException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
