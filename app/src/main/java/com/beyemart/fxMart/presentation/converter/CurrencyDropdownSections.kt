package com.beyemart.fxMart.presentation.converter

import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.SortOrder

internal const val FAVORITES_SECTION_TITLE = "Favorites"
internal const val ALL_CURRENCIES_SECTION_TITLE = "All currencies"

data class CurrencyDropdownSection(
    val title: String,
    val currencies: List<Currency>,
)

fun buildCurrencyDropdownSections(
    currencies: List<Currency>,
    sortOrder: SortOrder,
): List<CurrencyDropdownSection> {
    if (currencies.isEmpty()) {
        return emptyList()
    }

    val comparator = when (sortOrder) {
        SortOrder.ALPHABETICAL -> compareBy<Currency> { it.code }
        SortOrder.LATEST_USED -> compareByDescending<Currency> { it.lastUsedTime }.thenBy { it.code }
    }

    val favoriteCurrencies = currencies
        .filter { it.isFavorite }
        .sortedWith(comparator)

    val otherCurrencies = currencies
        .filterNot { it.isFavorite }
        .sortedWith(comparator)

    return buildList {
        if (favoriteCurrencies.isNotEmpty()) {
            add(
                CurrencyDropdownSection(
                    title = FAVORITES_SECTION_TITLE,
                    currencies = favoriteCurrencies,
                )
            )
        }

        if (otherCurrencies.isNotEmpty()) {
            add(
                CurrencyDropdownSection(
                    title = ALL_CURRENCIES_SECTION_TITLE,
                    currencies = otherCurrencies,
                )
            )
        }
    }
}


