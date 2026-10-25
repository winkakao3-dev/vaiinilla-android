package com.vaiinilla.app.domain.usecase

import com.vaiinilla.app.domain.model.CartLine
import com.vaiinilla.app.domain.model.CreateOrderItem
import com.vaiinilla.app.domain.model.CreateOrderRequest
import com.vaiinilla.app.domain.model.OrderDestination
import com.vaiinilla.app.domain.model.OrderOrigin
import com.vaiinilla.app.domain.model.PaymentMethod
import javax.inject.Inject

class BuildCreateOrderRequestUseCase
    @Inject
    constructor() {
        operator fun invoke(
            lines: List<CartLine>,
            kitchenNotes: String,
            paymentMethod: PaymentMethod = PaymentMethod.CASH,
            destination: OrderDestination = OrderDestination.TAKE_AWAY,
            spaceId: Int? = null,
            payAtEnd: Boolean = false,
            origin: OrderOrigin = OrderOrigin.APP,
            customerName: String? = null,
        ): CreateOrderRequest {
            val lineSummary = kitchenLineNotesSummary(lines.map { it.product.name to it.notes })
            return CreateOrderRequest(
                paymentMethod = paymentMethod,
                destination = destination,
                spaceId = spaceId,
                kitchenNotes =
                    if (lineSummary.isEmpty()) {
                        kitchenNotes
                    } else {
                        combineKitchenNotes(kitchenNotes, lineSummary)
                    },
                payAtEnd = payAtEnd,
                origin = origin,
                customerName = customerName?.trim()?.takeIf { it.isNotEmpty() },
                items =
                    lines.map { line ->
                        CreateOrderItem(
                            productId = line.product.id,
                            quantity = line.quantity,
                            optionIds = line.selectedOptionIds.sorted(),
                            notes = line.notes,
                        )
                    },
            )
        }
    }

/** Largo máximo de `notas_cocina` que acepta el servidor. */
const val KITCHEN_NOTES_MAX = 500

/** Resumen por línea como lo lee cocina: "Burrito: sin cebolla · Agua: sin hielo". Omite líneas sin nota. */
internal fun kitchenLineNotesSummary(namesAndNotes: List<Pair<String, String>>): String =
    namesAndNotes
        .filter { (_, notes) -> notes.isNotEmpty() }
        .joinToString(" · ") { (name, notes) -> "$name: $notes" }

/** Nota general y resumen por línea en un solo texto para `notas_cocina`. */
internal fun combineKitchenNotes(
    general: String,
    lineSummary: String,
): String =
    listOf(general.trim(), lineSummary)
        .filter { it.isNotEmpty() }
        .joinToString(" | ")

/** Inversa de [combineKitchenNotes]: quita el resumen por línea para recuperar solo la nota general. */
internal fun generalKitchenNotes(
    combined: String,
    lineSummary: String,
): String =
    when {
        lineSummary.isEmpty() -> combined
        combined == lineSummary -> ""
        combined.endsWith(" | $lineSummary") -> combined.removeSuffix(" | $lineSummary")
        else -> combined
    }
