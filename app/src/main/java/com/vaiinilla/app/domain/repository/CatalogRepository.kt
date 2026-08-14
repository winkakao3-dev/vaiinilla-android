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
}

class CatalogRepositoryException(
    val code: String,
    message: String,
) : IllegalStateException(message)
