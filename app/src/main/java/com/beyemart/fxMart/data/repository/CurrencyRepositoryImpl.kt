package com.beyemart.fxMart.data.repository

import com.beyemart.fxMart.data.local.CurrencyDao
import com.beyemart.fxMart.data.local.entity.CurrencyEntity
import com.beyemart.fxMart.data.local.entity.ExchangeRateEntity
import com.beyemart.fxMart.data.local.entity.SettingEntity
import com.beyemart.fxMart.data.mapper.*
import com.beyemart.fxMart.data.remote.datasource.FrankfurterDataSource
import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.SortOrder
import com.beyemart.fxMart.domain.model.WorldCurrencies
import com.beyemart.fxMart.domain.repository.CurrencyRepository
import com.beyemart.fxMart.domain.repository.HistoricalRatesLoadResult
import com.beyemart.fxMart.domain.repository.LatestRatesLoadResult
import com.beyemart.fxMart.util.Resource
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepositoryImpl @Inject constructor(
    private val dao: CurrencyDao,
    private val frankfurterDataSource: FrankfurterDataSource
) : CurrencyRepository {

    override fun getCurrencies(): Flow<Resource<List<Currency>>> = flow {
        val localCurrencies = dao.getCurrencies().first()
        if (localCurrencies.isEmpty()) {
            val initialEntities = WorldCurrencies.list.map { currency ->
                CurrencyEntity(
                    code = currency.code,
                    name = currency.name,
                    isFavorite = currency.code == "USD" || currency.code == "EUR"
                )
            }
            dao.insertCurrencies(initialEntities)
        }

        try {
            val remoteCurrencies = frankfurterDataSource.getCurrencies("")
            val remoteCodes = remoteCurrencies.map { it.code }.toSet()
            val extraCurrencies = WorldCurrencies.list.filter { !remoteCodes.contains(it.code) }
            val combinedCurrencies = (remoteCurrencies + extraCurrencies).sortedBy { it.code }

            val entities = combinedCurrencies.map { currency ->
                CurrencyEntity(
                    code = currency.code,
                    name = currency.name,
                    isFavorite = currency.code == "USD" || currency.code == "EUR"
                )
            }
            dao.insertCurrencies(entities)
        } catch (_: Exception) {}

        emitAll(
            dao.getCurrencies().map { entities ->
                Resource.Success(entities.map { it.toCurrency() })
            }
        )
    }

    override fun getLatestRates(base: String): Flow<Resource<LatestRatesLoadResult>> = flow {
        emit(Resource.Loading())

        val localRates = dao.getLatestRates(base).first()
        var localSnapshot: LatestRatesLoadResult? = null
        if (localRates.isNotEmpty()) {
            val latestDate = localRates.maxOf { it.date }
            localSnapshot = LatestRatesLoadResult(
                exchangeRate = localRates.toExchangeRate(base, latestDate),
                isStale = true,
            )
            emit(Resource.Success(localSnapshot))
        }

        try {
            val exchangeRate = frankfurterDataSource.getLatestRates(base, "")
            dao.deleteLatestRates(base)
            val entities = exchangeRate.rates.map { (quote, rate) ->
                ExchangeRateEntity(
                    base = base,
                    target = quote,
                    rate = rate,
                    date = exchangeRate.date.toString(),
                    isHistorical = false
                )
            }
            dao.insertExchangeRates(entities)
            emit(Resource.Success(LatestRatesLoadResult(exchangeRate = exchangeRate, isStale = false)))
        } catch (e: Exception) {
            if (localRates.isEmpty()) {
                emit(Resource.Error(e.message ?: "Unknown error"))
            } else {
                emit(Resource.Error(e.message ?: "Unknown error", localSnapshot))
            }
        }
    }

    override fun getHistoricalRates(
        base: String,
        target: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<Resource<HistoricalRatesLoadResult>> = flow {
        emit(Resource.Loading())

        val localHistorical = dao.getHistoricalRates(
            base, 
            target, 
            startDate.toString(), 
            endDate.toString()
        ).first()

        var localSnapshot: HistoricalRatesLoadResult? = null
        if (localHistorical.isNotEmpty()) {
            localSnapshot = HistoricalRatesLoadResult(
                historicalRates = localHistorical.toHistoricalRates(),
                isStale = true,
            )
            emit(Resource.Success(localSnapshot))
        }

        try {
            val rates = frankfurterDataSource.getHistoricalRates(base, target, startDate, endDate, "")
            val entities = rates.map {
                ExchangeRateEntity(
                    base = base,
                    target = target,
                    rate = it.rate,
                    date = it.date.toString(),
                    isHistorical = true
                )
            }
            dao.insertExchangeRates(entities)
            emit(Resource.Success(HistoricalRatesLoadResult(historicalRates = rates, isStale = false)))
        } catch (e: Exception) {
            if (localHistorical.isEmpty()) {
                emit(Resource.Error(e.message ?: "Unknown error"))
            } else {
                emit(Resource.Error(e.message ?: "Unknown error", localSnapshot))
            }
        }
    }

    override suspend fun toggleFavorite(currencyCode: String, isFavorite: Boolean) {
        dao.updateFavoriteStatus(currencyCode, isFavorite)
    }

    override fun getFavoriteCurrencies(): Flow<List<Currency>> {
        return dao.getFavoriteCurrencies().map { entities ->
            entities.map { it.toCurrency() }
        }
    }

    override suspend fun updateLastUsed(currencyCode: String) {
        dao.updateLastUsedTime(currencyCode, System.currentTimeMillis())
    }

    override fun getSortOrder(): Flow<SortOrder> {
        return dao.getSetting("sort_order").map { value ->
            try {
                value?.let { SortOrder.valueOf(it) } ?: SortOrder.ALPHABETICAL
            } catch (_: Exception) {
                SortOrder.ALPHABETICAL
            }
        }
    }

    override suspend fun setSortOrder(sortOrder: SortOrder) {
        dao.insertSetting(SettingEntity(key = "sort_order", value = sortOrder.name))
    }
}
