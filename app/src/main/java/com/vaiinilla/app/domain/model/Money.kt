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

    fun productUnitPreview(
        product: Product,
        selectedOptionIds: Set<Int>,
    ): String = format(parse(product.digitalPrice) + optionExtras(product, selectedOptionIds))

    /** Precio de una unidad en mostrador (sin el cargo digital) con los extras elegidos. */
    fun counterUnitPreview(
        product: Product,
        selectedOptionIds: Set<Int>,
    ): String = format(parse(product.counterPrice) + optionExtras(product, selectedOptionIds))

    fun cartLinePreview(line: CartLine): String =
        format(
            parse(productUnitPreview(line.product, line.selectedOptionIds)) * line.quantity.toBigDecimal(),
        )

    fun cartPreview(lines: List<CartLine>): String =
        format(
            lines.fold(BigDecimal.ZERO) { total, line -> total + parse(cartLinePreview(line)) },
        )

    /** Lo que se cobra en mostrador: precio de mostrador más extras. El servidor confirma el total. */
    fun counterPreview(lines: List<CartLine>): String =
        format(
            lines.fold(BigDecimal.ZERO) { total, line ->
                total +
                    (parse(line.product.counterPrice) + optionExtras(line.product, line.selectedOptionIds)) *
                    line.quantity.toBigDecimal()
            },
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
