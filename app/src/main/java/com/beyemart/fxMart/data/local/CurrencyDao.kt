package com.beyemart.fxMart.data.local

import androidx.room.*
import com.beyemart.fxMart.data.local.entity.CurrencyEntity
import com.beyemart.fxMart.data.local.entity.ExchangeRateEntity
import com.beyemart.fxMart.data.local.entity.SettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {

    @Query("SELECT * FROM currencies")
    fun getCurrencies(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE isFavorite = 1")
    fun getFavoriteCurrencies(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE isFavorite = 1")
    suspend fun getFavoriteCurrenciesList(): List<CurrencyEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCurrencies(currencies: List<CurrencyEntity>)

    @Query("UPDATE currencies SET isFavorite = :isFavorite WHERE code = :code")
    suspend fun updateFavoriteStatus(code: String, isFavorite: Boolean)

    @Query("UPDATE currencies SET lastUsedTime = :lastUsedTime WHERE code = :code")
    suspend fun updateLastUsedTime(code: String, lastUsedTime: Long)

    @Query("SELECT value FROM settings WHERE `key` = :key")
    fun getSetting(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)

    @Query("SELECT * FROM exchange_rates WHERE base = :base AND isHistorical = 0")
    fun getLatestRates(base: String): Flow<List<ExchangeRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRates(rates: List<ExchangeRateEntity>)

    @Query("SELECT * FROM exchange_rates WHERE base = :base AND target = :target AND isHistorical = 1 AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getHistoricalRates(base: String, target: String, startDate: String, endDate: String): Flow<List<ExchangeRateEntity>>

    @Query("DELETE FROM exchange_rates WHERE base = :base AND isHistorical = 0")
    suspend fun deleteLatestRates(base: String)
}
