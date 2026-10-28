package com.vaiinilla.app.data.admin

import com.vaiinilla.app.core.network.ApiClientException
import com.vaiinilla.app.core.network.VaiinillaApiClient
import com.vaiinilla.app.data.contract.ContractResponseParser
import com.vaiinilla.app.data.order.OrderContractJson
import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminOrdersPage
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.AdminSummary
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.Product
import com.vaiinilla.app.domain.repository.AdminRepository
import com.vaiinilla.app.domain.repository.CatalogRepositoryException

class RemoteAdminRepository(
    private val apiClient: VaiinillaApiClient,
    private val catalogParser: ContractResponseParser,
    private val orderJson: OrderContractJson,
    private val json: AdminContractJson = AdminContractJson(),
) : AdminRepository {
    override fun summary(period: AdminPeriod): Result<AdminSummary> =
        apiClient
            .get("reportes/resumen", mapOf("desde" to period.from.toString(), "hasta" to period.to.toString()))
            .mapCatching { json.parseSummary(it, period) }
            .mapApiErrors()

    override fun unpaidAccounts(): Result<AdminUnpaidAccounts> =
        apiClient
            .get("espacios/cuentas-sin-pagar")
            .mapCatching(json::parseUnpaidAccounts)
            .mapApiErrors()

    override fun orders(
        states: Set<OrderState>,
        cursor: String?,
    ): Result<AdminOrdersPage> {
        val query =
            buildMap {
                if (states.isNotEmpty()) put("estado", states.joinToString(",") { it.wireValue })
                cursor?.let { put("cursor", it) }
                put("limit", PAGE_SIZE.toString())
            }
        return apiClient
            .get("pedidos", query)
            .mapCatching { raw -> AdminOrdersPage(orderJson.parseOrderList(raw), json.parseCursor(raw)) }
            .mapApiErrors()
    }

    override fun createCategory(
        input: AdminCategoryInput,
        idempotencyKey: String,
    ): Result<Category> =
        apiClient
            .post("catalogo/categorias", json.encodeCategory(input), mapOf("Idempotency-Key" to idempotencyKey))
            .mapCatching(json::parseCategory)
            .mapApiErrors()

    override fun updateCategory(
        id: Int,
        input: AdminCategoryInput,
        idempotencyKey: String,
    ): Result<Category> =
        apiClient
            .patch("catalogo/categorias/$id", json.encodeCategory(input), mapOf("Idempotency-Key" to idempotencyKey))
            .mapCatching(json::parseCategory)
            .mapApiErrors()

    override fun createProduct(
        input: AdminProductInput,
        idempotencyKey: String,
    ): Result<Product> =
        apiClient
            .post("catalogo/productos", json.encodeProduct(input), mapOf("Idempotency-Key" to idempotencyKey))
            .mapCatching(catalogParser::parseProduct)
            .mapApiErrors()

    override fun updateProduct(
        id: Int,
        input: AdminProductInput,
        idempotencyKey: String,
    ): Result<Product> =
        apiClient
            .put("catalogo/productos/$id", json.encodeProduct(input), mapOf("Idempotency-Key" to idempotencyKey))
            .mapCatching(catalogParser::parseProduct)
            .mapApiErrors()

    override fun deleteProductImage(
        id: Int,
        idempotencyKey: String,
    ): Result<Product> =
        apiClient
            .delete("catalogo/productos/$id/imagen", headers = mapOf("Idempotency-Key" to idempotencyKey))
            .mapCatching(catalogParser::parseProduct)
            .mapApiErrors()

    override fun setProductArchived(
        id: Int,
        archived: Boolean,
        idempotencyKey: String,
    ): Result<Unit> =
        apiClient
            .post(
                "catalogo/productos/$id/${if (archived) "archivar" else "desarchivar"}",
                "{}",
                mapOf("Idempotency-Key" to idempotencyKey),
            ).map { }
            .mapApiErrors()

    private fun <T> Result<T>.mapApiErrors(): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Result.failure(
                    if (error is ApiClientException) {
                        CatalogRepositoryException(error.code, error.message ?: error.code, error)
                    } else {
                        error
                    },
                )
            },
        )

    private companion object {
        const val PAGE_SIZE = 30
    }
}
