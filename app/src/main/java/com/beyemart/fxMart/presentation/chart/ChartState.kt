package com.beyemart.fxMart.presentation.chart

import com.beyemart.fxMart.domain.model.HistoricalRate
import java.time.LocalDate

data class ChartState(
    val base: String = "",
    val target: String = "",
    val timeframe: Timeframe = Timeframe.DAYS_30,
    val historicalRates: List<HistoricalRate> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val amount: Double = 1.0,
    val isUsingCachedData: Boolean = false,
)

enum class Timeframe(val label: String) {
    DAYS_30("30D"),
    MONTH_TO_DATE("MTD"),
    YEAR_TO_DATE("YTD"),
    YEAR_1("1Y"),
    YEAR_5("5Y"),
    MAX("MAX");

    fun getStartDate(today: LocalDate = LocalDate.now()): LocalDate {
        return when (this) {
            DAYS_30 -> today.minusDays(30)
            MONTH_TO_DATE -> today.withDayOfMonth(1)
            YEAR_TO_DATE -> today.withDayOfYear(1)
            YEAR_1 -> today.minusYears(1)
            YEAR_5 -> today.minusYears(5)
            MAX -> LocalDate.of(1999, 1, 4)
        }
    }
}
