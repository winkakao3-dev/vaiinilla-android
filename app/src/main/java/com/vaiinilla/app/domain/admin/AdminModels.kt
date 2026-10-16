package com.vaiinilla.app.domain.admin

import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.PreparationStation
import java.time.LocalDate

/** Rango de fechas del resumen, en la fecha local del negocio, como lo pide `/reportes/resumen`. */
data class AdminPeriod(
    val from: LocalDate,
    val to: LocalDate,
) {
    companion object {
        /** Los últimos [days] días contando hoy, como los atajos de la web. */
        fun lastDays(
            days: Int,
            today: LocalDate = LocalDate.now(),
        ): AdminPeriod = AdminPeriod(from = today.minusDays((days - 1).toLong()), to = today)
    }
}

/** Lo que el dueño ve primero: el resumen del periodo, como en el panel de la web. */
data class AdminSummary(
    val period: AdminPeriod,
    val sales: String,
    val orders: Int,
    val averageTicket: String,
    val productsSold: Int,
    val tips: String?,
    val walletReloads: String,
    val canceledOrders: Int?,
    /** Comisiones del periodo; null si el backend no las manda. */
    val commissions: String? = null,
    val dailySales: List<AdminDailySales>,
    val paymentMethods: List<AdminMetric>,
    val ordersByStatus: List<AdminCount>,
    val topProducts: List<AdminCount>,
)

data class AdminDailySales(
    val date: String,
    val sales: String,
    val orders: Int,
)

/** Una fila con dinero: por método de pago. */
data class AdminMetric(
    val label: String,
    val amount: String,
    val orders: Int,
)

/** Una fila con cantidad: pedidos por estado o unidades por producto. */
data class AdminCount(
    val label: String,
    val count: Int,
)

/** Cuentas de mesas y canchas que siguen sin pagar. */
data class AdminUnpaidAccounts(
    val accounts: List<AdminUnpaidAccount>,
    val total: String,
    val abandoned: Int,
)

data class AdminUnpaidAccount(
    val spaceId: Int,
    val spaceName: String,
    val orders: Int,
    val total: String,
    val hours: Int,
    /** El espacio ya se liberó y la cuenta sigue sin pagar: alguien se fue sin pagar. */
    val abandoned: Boolean,
)

/** Una página del historial de pedidos; [cursor] trae la siguiente. */
data class AdminOrdersPage(
    val orders: List<OrderDetail>,
    val cursor: String?,
)

/** Categoría nueva o editada. */
data class AdminCategoryInput(
    val name: String,
    val order: Int,
)

/** Producto completo, nuevo o editado, como lo guarda la administración (`PUT /catalogo/productos/:id`). */
data class AdminProductInput(
    val categoryId: Int,
    val preparationStation: PreparationStation,
    val name: String,
    val description: String,
    val ingredients: String,
    val allergens: String,
    val estimatedTimeMinutes: Int,
    val counterPrice: String,
    val available: Boolean,
    val optionGroups: List<AdminOptionGroupInput>,
)

/** Un grupo de variantes o extras; [id] nulo si es nuevo. */
data class AdminOptionGroupInput(
    val id: Int?,
    val name: String,
    val minimumSelections: Int,
    val maximumSelections: Int,
    val options: List<AdminOptionInput>,
)

data class AdminOptionInput(
    val id: Int?,
    val name: String,
    val extraPrice: String,
)
