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
    /** Lo vendido de ese producto; null si el servidor no lo manda. */
    val amount: String? = null,
    /** El estado como lo manda el servidor (`entregado`), para colorearlo. */
    val key: String? = null,
)

/** Cuentas de mesas y canchas que siguen sin pagar. */
data class AdminUnpaidAccounts(
    val accounts: List<AdminUnpaidAccount>,
    val total: String,
    val abandoned: Int,
    /** Lo que falta por cobrar de todas, ya sin lo abonado; un servidor anterior no lo manda. */
    val remaining: String = total,
)

data class AdminUnpaidAccount(
    val spaceId: Int,
    val spaceName: String,
    val orders: Int,
    /** R33: lo que falta cobrar (`restante`); lo abonado ya está en la caja. */
    val total: String,
    val hours: Int,
    /** El espacio ya se liberó y la cuenta sigue sin pagar: alguien se fue sin pagar. */
    val abandoned: Boolean,
    /** Lo ya abonado (dividir por monto) que sí entró a la caja (R3). */
    val paid: String = "0.00",
    /** Lo que falta por cobrar: total − abonado. Es el `total_esperado` del cobro (contrato F). */
    val remaining: String = total,
    /** Turno abierto al que pertenece; null en una cuenta de un turno ya cerrado (G09). */
    val sessionId: String? = null,
    /** Los pedidos de la cuenta; vacío con un servidor que solo manda cuántos son. */
    val orderLines: List<AdminUnpaidOrder> = emptyList(),
)

/** Un pedido de una cuenta sin pagar, con lo que falta de él. */
data class AdminUnpaidOrder(
    val id: String,
    val folio: String,
    val total: String,
    val remaining: String,
)

/** Largo del motivo de «Cerrar sin cobrar» que acepta el servidor (contrato D). */
const val FORCED_CLOSE_REASON_MIN = 3
const val FORCED_CLOSE_REASON_MAX = 240

/** Lo que dejó «Cerrar sin cobrar» (contrato D): pedidos cancelados, lo abonado que sí entró y lo que se perdió. */
data class ForcedCloseResult(
    val cancelled: Int,
    val paid: String,
    val lost: String,
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
    /** En una escuela: los alérgenos del catálogo; mandarlos marca el producto como revisado. `null` no los toca. */
    val allergenCodes: Set<String>? = null,
    /** Volverlo a "sin revisar": manda `alergenos_revisados: false` y no toca los códigos. */
    val markUnreviewed: Boolean = false,
    /** Repostería: días de preparación y tope por día; null no los toca (CASA-04). */
    val preparation: ProductPreparationInput? = null,
)

data class ProductPreparationInput(
    val days: Int,
    val maximumPerDay: Int?,
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
