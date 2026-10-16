package com.beyemart.fxMart.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencySearchTest {

    @Test
    fun matchesSearchQuery_matchesByCurrencyCode() {
        val currency = Currency(code = "CHF", name = "Swiss Franc")

        assertTrue(currency.matchesSearchQuery("chf"))
    }

    @Test
    fun matchesSearchQuery_matchesByCurrencyName() {
        val currency = Currency(code = "CHF", name = "Swiss Franc")

        assertTrue(currency.matchesSearchQuery("franc"))
    }

    @Test
    fun matchesSearchQuery_matchesByCountryName() {
        val currency = Currency(code = "CHF", name = "Swiss Franc")

        assertTrue(currency.matchesSearchQuery("switzerland"))
    }

    @Test
    fun matchesSearchQuery_returnsFalseForUnknownQuery() {
        val currency = Currency(code = "CHF", name = "Swiss Franc")

        assertFalse(currency.matchesSearchQuery("japan"))
    }
}

