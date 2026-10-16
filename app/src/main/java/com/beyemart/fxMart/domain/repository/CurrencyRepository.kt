package com.beyemart.fxMart.domain.repository

import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.SortOrder
import com.beyemart.fxMart.util.Resource
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface CurrencyRepository {
    fun getCurrencies(): Flow<Resource<List<Currency>>>
    fun getLatestRates(base: String): Flow<Resource<LatestRatesLoadResult>>
    fun getHistoricalRates(
        base: String,
        target: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Resource<HistoricalRatesLoadResult>>

    suspend fun toggleFavorite(currencyCode: String, isFavorite: Boolean)
    fun getFavoriteCurrencies(): Flow<List<Currency>>

    suspend fun updateLastUsed(currencyCode: String)
    fun getSortOrder(): Flow<SortOrder>
    suspend fun setSortOrder(sortOrder: SortOrder)
}
