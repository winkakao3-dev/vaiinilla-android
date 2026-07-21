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

    fun productUnitPreview(product: Product, selectedOptionIds: Set<Int>): String {
        val optionPrices = product.optionGroups
            .flatMap(OptionGroup::options)
            .filter { it.id in selectedOptionIds }
            .fold(BigDecimal.ZERO) { total, option -> total + parse(option.extraPrice) }
        return format(parse(product.digitalPrice) + optionPrices)
    }

    fun cartLinePreview(line: CartLine): String = format(
        parse(productUnitPreview(line.product, line.selectedOptionIds)) * line.quantity.toBigDecimal(),
    )

    fun cartPreview(lines: List<CartLine>): String = format(
        lines.fold(BigDecimal.ZERO) { total, line -> total + parse(cartLinePreview(line)) },
    )
}
