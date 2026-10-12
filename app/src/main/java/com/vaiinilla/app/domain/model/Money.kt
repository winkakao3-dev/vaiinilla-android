package com.vaiinilla.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

object Money {
    private const val SCALE = 2

    fun parse(value: String): BigDecimal {
        require(ContractRules.isValidMoney(value)) { "Importe contractual inválido: $value" }
        return value.toBigDecimal().setScale(SCALE, RoundingMode.UNNECESSARY)
    }

    fun format(value: BigDecimal): String = value.setScale(SCALE, RoundingMode.HALF_UP).toPlainString()

    /** Precio del menú y de la ficha: el digital, que ya incluye la comisión si el dueño la pasa al cliente. */
    fun productUnitPreview(
        product: Product,
        selectedOptionIds: Set<Int>,
    ): String = format(parse(product.digitalPrice) + optionExtras(product, selectedOptionIds))

    /**
     * Lo que se cobra por una unidad según el método. Solo la tarjeta lleva la comisión (precio
     * digital); efectivo, saldo y pagar al final usan el de mostrador. Sin método elegido, el de mostrador.
     */
    fun chargeUnitPreview(
        product: Product,
        selectedOptionIds: Set<Int>,
        method: PaymentMethod?,
    ): String {
        val base = if (method == PaymentMethod.STRIPE) product.digitalPrice else product.counterPrice
        return format(parse(base) + optionExtras(product, selectedOptionIds))
    }

    fun cartLinePreview(
        line: CartLine,
        method: PaymentMethod? = null,
    ): String =
        format(
            parse(chargeUnitPreview(line.product, line.selectedOptionIds, method)) * line.quantity.toBigDecimal(),
        )

    fun cartPreview(
        lines: List<CartLine>,
        method: PaymentMethod? = null,
    ): String =
        format(
            lines.fold(BigDecimal.ZERO) { total, line -> total + parse(cartLinePreview(line, method)) },
        )

    private fun optionExtras(
        product: Product,
        selectedOptionIds: Set<Int>,
    ): BigDecimal =
        product.optionGroups
            .flatMap(OptionGroup::options)
            .filter { it.id in selectedOptionIds }
            .fold(BigDecimal.ZERO) { total, option -> total + parse(option.extraPrice) }
}
