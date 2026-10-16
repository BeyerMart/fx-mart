package com.beyemart.fxMart.data.remote.datasource

import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.ExchangeRate
import com.beyemart.fxMart.domain.model.HistoricalRate
import java.time.LocalDate

interface CurrencyDataSource {
    suspend fun getCurrencies(apiKey: String): List<Currency>
    suspend fun getLatestRates(base: String, apiKey: String): ExchangeRate
    suspend fun getHistoricalRates(
        base: String,
        target: String,
        startDate: LocalDate,
        endDate: LocalDate,
        apiKey: String
    ): List<HistoricalRate>
}
