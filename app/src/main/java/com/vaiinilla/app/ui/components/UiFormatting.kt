package com.vaiinilla.app.ui.components

import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.domain.model.NameInitials
import java.math.BigDecimal

fun moneyLabel(value: Number): String = compactMoney(BigDecimal(value.toString()))

fun moneyLabel(value: String): String =
    value.toBigDecimalOrNull()?.let(::compactMoney) ?: ("\$" + value.removeSuffix(".00"))

/** La primera letra del nombre para un monograma; un emoji al inicio no cuenta. */
fun initialOf(name: String): String = NameInitials.of(name, max = 1)
