package com.beyemart.fxMart.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

fun formatMoney(value: Double): String {
    val decimals = when {
        value == 0.0 -> 2
        abs(value) >= 1.0 -> 2
        abs(value) >= 0.01 -> 4
        else -> 8
    }

    return BigDecimal.valueOf(value)
        .setScale(decimals, RoundingMode.HALF_UP)
        .toPlainString()
}

fun formatMoneyWithGrouping(value: Double, locale: Locale = Locale.getDefault()): String {
    val plain = formatMoney(value)
    val symbols = DecimalFormatSymbols.getInstance(locale)
    val isNegative = plain.startsWith("-")
    val absolutePlain = if (isNegative) plain.drop(1) else plain
    val parts = absolutePlain.split('.')
    val integerPart = parts.first()
    val groupedIntegerPart = integerPart.reversed()
        .chunked(3)
        .joinToString(symbols.groupingSeparator.toString())
        .reversed()

    val base = buildString {
        if (isNegative) {
            append('-')
        }
        append(groupedIntegerPart)
        if (parts.size > 1) {
            append(symbols.decimalSeparator)
            append(parts[1])
        }
    }

    return base
}

