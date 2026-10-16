package com.beyemart.fxMart.data.remote.datasource

import com.beyemart.fxMart.data.remote.ExchangeRateApiService
import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.ExchangeRate
import com.beyemart.fxMart.domain.model.HistoricalRate
import com.beyemart.fxMart.domain.model.WorldCurrencies
import java.time.LocalDate
import javax.inject.Inject

class ExchangeRateApiDataSource @Inject constructor(
    private val api: ExchangeRateApiService
) : CurrencyDataSource {

    override suspend fun getCurrencies(apiKey: String): List<Currency> {
        if (apiKey.isBlank()) throw IllegalArgumentException("API Key is required for ExchangeRate-API")
        return try {
            val response = api.getLatestRates(apiKey, "USD")
            val rates = response.conversionRates ?: return WorldCurrencies.list
            rates.keys.map { code ->
                Currency(code = code, name = WorldCurrencies.getName(code))
            }
        } catch (_: Exception) {
            WorldCurrencies.list
        }
    }

    override suspend fun getLatestRates(base: String, apiKey: String): ExchangeRate {
        if (apiKey.isBlank()) throw IllegalArgumentException("API Key is required for ExchangeRate-API")
        val response = api.getLatestRates(apiKey, base)
        val rates = response.conversionRates ?: throw Exception(response.errorType ?: "Failed to fetch rates from ExchangeRate-API")
        return ExchangeRate(
            base = base,
            date = LocalDate.now(),
            rates = rates
        )
    }

    override suspend fun getHistoricalRates(
        base: String,
        target: String,
        startDate: LocalDate,
        endDate: LocalDate,
        apiKey: String
    ): List<HistoricalRate> {
        if (apiKey.isBlank()) throw IllegalArgumentException("API Key is required for ExchangeRate-API")
        val latest = getLatestRates(base, apiKey)
        val rate = latest.rates[target] ?: 1.0
        val points = mutableListOf<HistoricalRate>()
        var curr = startDate
        while (!curr.isAfter(endDate)) {
            points.add(HistoricalRate(date = curr, rate = rate))
            curr = curr.plusDays(1)
        }
        return points
    }
}
