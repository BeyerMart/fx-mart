package com.beyemart.fxMart.domain.model

import java.util.Locale

fun Currency.matchesSearchQuery(query: String): Boolean {
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    if (normalizedQuery.isEmpty()) {
        return true
    }

    val countryName = WorldCurrencies.getCountryName(code).lowercase(Locale.ROOT)
    return code.lowercase(Locale.ROOT).contains(normalizedQuery) ||
        name.lowercase(Locale.ROOT).contains(normalizedQuery) ||
        countryName.contains(normalizedQuery)
}

fun Currency.displayLabel(): String {
    val countryName = WorldCurrencies.getCountryName(code)
    return if (countryName.isBlank()) {
        "$code - $name"
    } else {
        "$code - $name · $countryName"
    }
}

