@file:Suppress("unused")

package com.beyemart.fxMart.presentation.chart

import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.HistoricalRate

internal fun filterComparisonCurrencies(
    currencies: List<Currency>,
    baseCurrencyCode: String,
    primaryTargetCode: String,
    comparisonTargetCode: String? = null,
): List<Currency> {
    val blockedCodes = buildSet {
        add(baseCurrencyCode)
        add(primaryTargetCode)
        comparisonTargetCode?.let { add(it) }
    }

    return currencies.filterNot { currency ->
        currency.code in blockedCodes
    }
}

internal fun calculateRateBounds(
    seriesRates: List<List<HistoricalRate>>,
): Pair<Float, Float> {
    val rates = seriesRates.flatten().map { it.rate.toFloat() }
    if (rates.isEmpty()) {
        return 0f to 1f
    }

    val minRate = rates.minOrNull() ?: 0f
    val maxRate = rates.maxOrNull() ?: 0f
    val range = maxRate - minRate
    val padding = if (range == 0f) 0.1f else range * 0.15f

    return (minRate - padding) to (maxRate + padding)
}


