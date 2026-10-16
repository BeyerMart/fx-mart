package com.beyemart.fxMart.presentation.watchlist

data class WatchlistState(
    val favorites: List<CurrencyRate> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val baseCurrencyCode: String = "USD"
)

data class CurrencyRate(
    val code: String,
    val name: String,
    val country: String,
    val rate: Double,
    val isFavorite: Boolean = true
)
