package com.beyemart.fxMart.data.remote.datasource

import com.beyemart.fxMart.data.remote.CurrencyApiService
import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.ExchangeRate
import com.beyemart.fxMart.domain.model.HistoricalRate
import com.beyemart.fxMart.domain.model.WorldCurrencies
import java.time.LocalDate
import javax.inject.Inject

class FrankfurterDataSource @Inject constructor(
    private val api: CurrencyApiService
) : CurrencyDataSource {

    override suspend fun getCurrencies(apiKey: String): List<Currency> {
        val remoteList = try {
            api.getCurrencies().map { Currency(code = it.isoCode, name = it.name) }
        } catch (e: Exception) {
            emptyList()
        }
        val existingCodes = remoteList.map { it.code }.toSet()
        val extra = WorldCurrencies.list.filter { !existingCodes.contains(it.code) }
        return (remoteList + extra).sortedBy { it.code }
    }

    override suspend fun getLatestRates(base: String, apiKey: String): ExchangeRate {
        val dtos = api.getLatestRates(base)
        val rateMap = dtos.associate { it.quote to it.rate }
        val dateStr = dtos.firstOrNull()?.date ?: LocalDate.now().toString()
        return ExchangeRate(
            base = base,
            date = LocalDate.parse(dateStr),
            rates = rateMap
        )
    }

    override suspend fun getHistoricalRates(
        base: String,
        target: String,
        startDate: LocalDate,
        endDate: LocalDate,
        apiKey: String
    ): List<HistoricalRate> {
        val dtos = api.getRatesForPeriod(
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            base = base,
            quotes = target
        )
        return dtos.filter { it.quote == target }.map {
            HistoricalRate(
                date = LocalDate.parse(it.date),
                rate = it.rate
            )
        }.sortedBy { it.date }
    }
}
