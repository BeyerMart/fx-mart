package com.beyemart.fxMart.presentation.chart

import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.HistoricalRate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ChartHelpersTest {

    @Test
    fun filterComparisonCurrencies_excludesBasePrimaryAndSelectedComparison() {
        val currencies = listOf(
            Currency(code = "USD", name = "US Dollar"),
            Currency(code = "EUR", name = "Euro"),
            Currency(code = "GBP", name = "British Pound"),
            Currency(code = "JPY", name = "Japanese Yen"),
        )

        val result = filterComparisonCurrencies(
            currencies = currencies,
            baseCurrencyCode = "USD",
            primaryTargetCode = "EUR",
            comparisonTargetCode = "GBP",
        )

        assertEquals(listOf("JPY"), result.map { it.code })
    }

    @Test
    fun calculateRateBounds_usesAllSeriesValuesWithPadding() {
        val series = listOf(
            listOf(
                HistoricalRate(LocalDate.of(2026, 10, 1), 1.0),
                HistoricalRate(LocalDate.of(2026, 10, 2), 1.5),
            ),
            listOf(
                HistoricalRate(LocalDate.of(2026, 10, 1), 2.0),
                HistoricalRate(LocalDate.of(2026, 10, 2), 3.0),
            ),
        )

        val (minRate, maxRate) = calculateRateBounds(series)

        assertEquals(0.7f, minRate, 0.0001f)
        assertEquals(3.3f, maxRate, 0.0001f)
    }
}

