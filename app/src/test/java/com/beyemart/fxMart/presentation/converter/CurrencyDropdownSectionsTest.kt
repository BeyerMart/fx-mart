package com.beyemart.fxMart.presentation.converter

import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyDropdownSectionsTest {

    @Test
    fun buildCurrencyDropdownSections_returnsEmptyListWhenNoCurrencies() {
        val sections = buildCurrencyDropdownSections(emptyList(), SortOrder.ALPHABETICAL)

        assertTrue(sections.isEmpty())
    }

    @Test
    fun buildCurrencyDropdownSections_partitionsFavoritesBeforeAllCurrencies() {
        val currencies = listOf(
            Currency(code = "USD", name = "US Dollar", isFavorite = true, lastUsedTime = 10),
            Currency(code = "AUD", name = "Australian Dollar", lastUsedTime = 5),
            Currency(code = "CHF", name = "Swiss Franc", isFavorite = true, lastUsedTime = 30),
            Currency(code = "EUR", name = "Euro", lastUsedTime = 20),
        )

        val sections = buildCurrencyDropdownSections(currencies, SortOrder.ALPHABETICAL)

        assertEquals(2, sections.size)
        assertEquals(FAVORITES_SECTION_TITLE, sections[0].title)
        assertEquals(listOf("CHF", "USD"), sections[0].currencies.map { it.code })
        assertEquals(ALL_CURRENCIES_SECTION_TITLE, sections[1].title)
        assertEquals(listOf("AUD", "EUR"), sections[1].currencies.map { it.code })
    }

    @Test
    fun buildCurrencyDropdownSections_sortsByLatestUsedWithinEachSection() {
        val currencies = listOf(
            Currency(code = "USD", name = "US Dollar", isFavorite = true, lastUsedTime = 10),
            Currency(code = "AUD", name = "Australian Dollar", lastUsedTime = 5),
            Currency(code = "CHF", name = "Swiss Franc", isFavorite = true, lastUsedTime = 30),
            Currency(code = "EUR", name = "Euro", lastUsedTime = 20),
        )

        val sections = buildCurrencyDropdownSections(currencies, SortOrder.LATEST_USED)

        assertEquals(2, sections.size)
        assertEquals(listOf("CHF", "USD"), sections[0].currencies.map { it.code })
        assertEquals(listOf("EUR", "AUD"), sections[1].currencies.map { it.code })
    }
}

