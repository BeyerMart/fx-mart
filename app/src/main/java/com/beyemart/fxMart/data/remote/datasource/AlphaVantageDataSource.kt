package com.beyemart.fxMart.data.remote.datasource

import com.beyemart.fxMart.data.remote.AlphaVantageApiService
import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.ExchangeRate
import com.beyemart.fxMart.domain.model.HistoricalRate
import com.beyemart.fxMart.domain.model.WorldCurrencies
import java.time.LocalDate
import javax.inject.Inject

class AlphaVantageDataSource @Inject constructor(
    private val api: AlphaVantageApiService
) : CurrencyDataSource {

    override suspend fun getCurrencies(apiKey: String): List<Currency> {
        return WorldCurrencies.list
    }

    override suspend fun getLatestRates(base: String, apiKey: String): ExchangeRate {
        if (apiKey.isBlank()) throw IllegalArgumentException("API Key is required for Alpha Vantage")

        val rates = mutableMapOf<String, Double>()
        WorldCurrencies.list.forEach { currency ->
            if (currency.code == base) {
                rates[base] = 1.0
            } else {
                try {
                    val response = api.getExchangeRate(fromCurrency = base, toCurrency = currency.code, apiKey = apiKey)
                    val rateStr = response.rateData?.exchangeRate
                    rateStr?.toDoubleOrNull()?.let { rate ->
                        rates[currency.code] = rate
                    }
                } catch (_: Exception) {}
            }
        }

        if (rates.isEmpty()) {
            throw Exception("Failed to fetch rates from Alpha Vantage")
        }

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
        if (apiKey.isBlank()) throw IllegalArgumentException("API Key is required for Alpha Vantage")

        val isIntraday = startDate.isAfter(LocalDate.now().minusDays(2))
        val result = mutableListOf<HistoricalRate>()

        if (isIntraday) {
            try {
                val response = api.getFxIntraday(fromSymbol = base, toSymbol = target, interval = "5min", apiKey = apiKey)
                response.timeSeries?.forEach { (timestamp, bar) ->
                    val closeRate = bar.close?.toDoubleOrNull() ?: return@forEach
                    val datePart = timestamp.split(" ")[0]
                    val parsedDate = try { LocalDate.parse(datePart) } catch (_: Exception) { LocalDate.now() }
                    result.add(HistoricalRate(date = parsedDate, rate = closeRate))
                }
            } catch (_: Exception) {}
        }

        if (result.isEmpty()) {
            try {
                val response = api.getFxDaily(fromSymbol = base, toSymbol = target, apiKey = apiKey)
                response.timeSeries?.forEach { (dateStr, bar) ->
                    val closeRate = bar.close?.toDoubleOrNull() ?: return@forEach
                    val parsedDate = try { LocalDate.parse(dateStr) } catch (_: Exception) { return@forEach }
                    if (!parsedDate.isBefore(startDate) && !parsedDate.isAfter(endDate)) {
                        result.add(HistoricalRate(date = parsedDate, rate = closeRate))
                    }
                }
            } catch (_: Exception) {}
        }

        return result.sortedBy { it.date }
    }
}
