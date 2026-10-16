package com.beyemart.fxMart.data.remote

import com.beyemart.fxMart.data.remote.dto.CurrencyDto
import com.beyemart.fxMart.data.remote.dto.ExchangeRateDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApiService {

    @GET("currencies")
    suspend fun getCurrencies(): List<CurrencyDto>

    @GET("rates")
    suspend fun getLatestRates(
        @Query("base") base: String,
        @Query("quotes") quotes: String? = null
    ): List<ExchangeRateDto>

    @GET("rates")
    suspend fun getHistoricalRates(
        @Query("date") date: String,
        @Query("base") base: String,
        @Query("quotes") quotes: String? = null
    ): List<ExchangeRateDto>

    @GET("rates")
    suspend fun getRatesForPeriod(
        @Query("from") startDate: String,
        @Query("to") endDate: String,
        @Query("base") base: String,
        @Query("quotes") quotes: String? = null
    ): List<ExchangeRateDto>
}
