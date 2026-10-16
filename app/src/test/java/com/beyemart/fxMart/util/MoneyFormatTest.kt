package com.beyemart.fxMart.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MoneyFormatTest {

    @Test
    fun formatMoney_keepsMoreDecimalsForTinyValues() {
        assertEquals("0.00012346", formatMoney(0.0001234567))
    }

    @Test
    fun formatMoney_keepsTwoDecimalsForWholeNumbers() {
        assertEquals("12.00", formatMoney(12.0))
    }

    @Test
    fun formatMoney_keepsTwoDecimalsForLargeValues() {
        assertEquals("1234.57", formatMoney(1234.5678))
    }

    @Test
    fun formatMoneyWithGrouping_usesLocaleSeparators() {
        assertEquals("1.234.567,89", formatMoneyWithGrouping(1234567.89, Locale.GERMANY))
    }
}
