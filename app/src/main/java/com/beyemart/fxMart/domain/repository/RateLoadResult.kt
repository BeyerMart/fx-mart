package com.beyemart.fxMart.domain.repository

import com.beyemart.fxMart.domain.model.ExchangeRate
import com.beyemart.fxMart.domain.model.HistoricalRate

data class LatestRatesLoadResult(
    val exchangeRate: ExchangeRate,
    val isStale: Boolean,
)

data class HistoricalRatesLoadResult(
    val historicalRates: List<HistoricalRate>,
    val isStale: Boolean,
)

