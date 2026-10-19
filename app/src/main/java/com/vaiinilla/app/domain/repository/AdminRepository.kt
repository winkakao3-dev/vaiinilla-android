package com.vaiinilla.app.domain.repository

import com.vaiinilla.app.domain.admin.AdminCategoryInput
import com.vaiinilla.app.domain.admin.AdminOrdersPage
import com.vaiinilla.app.domain.admin.AdminPeriod
import com.vaiinilla.app.domain.admin.AdminProductInput
import com.vaiinilla.app.domain.admin.AdminSummary
import com.vaiinilla.app.domain.admin.AdminUnpaidAccounts
import com.vaiinilla.app.domain.model.Category
import com.vaiinilla.app.domain.model.OrderState
import com.vaiinilla.app.domain.model.Product

/** Lo que la administración del negocio hace desde la app, con su propia membresía de admin. */
interface AdminRepository {
    fun summary(period: AdminPeriod): Result<AdminSummary>

    fun unpaidAccounts(): Result<AdminUnpaidAccounts>

    fun orders(
        states: Set<OrderState>,
        cursor: String?,
    ): Result<AdminOrdersPage>

    fun createCategory(
        input: AdminCategoryInput,
        idempotencyKey: String,
    ): Result<Category>

    fun updateCategory(
        id: Int,
        input: AdminCategoryInput,
        idempotencyKey: String,
    ): Result<Category>

    fun createProduct(
        input: AdminProductInput,
        idempotencyKey: String,
    ): Result<Product>

    fun updateProduct(
        id: Int,
        input: AdminProductInput,
        idempotencyKey: String,
    ): Result<Product>

    fun deleteProductImage(
        id: Int,
        idempotencyKey: String,
    ): Result<Product>
}
