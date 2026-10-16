package com.beyemart.fxMart.presentation.chart

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TimeframeTest {

    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun getStartDate_returnsThirtyDayRangeStart() {
        assertEquals(LocalDate.of(2026, 9, 14), Timeframe.DAYS_30.getStartDate(today))
    }

    @Test
    fun getStartDate_returnsMonthToDateStart() {
        assertEquals(LocalDate.of(2026, 10, 1), Timeframe.MONTH_TO_DATE.getStartDate(today))
    }

    @Test
    fun getStartDate_returnsYearToDateStart() {
        assertEquals(LocalDate.of(2026, 1, 1), Timeframe.YEAR_TO_DATE.getStartDate(today))
    }

    @Test
    fun getStartDate_returnsMaxProviderStart() {
        assertEquals(LocalDate.of(1999, 1, 4), Timeframe.MAX.getStartDate(today))
    }
}

