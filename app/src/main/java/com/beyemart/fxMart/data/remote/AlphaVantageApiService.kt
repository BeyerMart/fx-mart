package com.beyemart.fxMart.data.remote

import com.beyemart.fxMart.data.remote.dto.AlphaVantageDailyResponse
import com.beyemart.fxMart.data.remote.dto.AlphaVantageIntradayResponse
import com.beyemart.fxMart.data.remote.dto.AlphaVantageRateResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AlphaVantageApiService {

    @GET("query")
    suspend fun getExchangeRate(
        @Query("function") function: String = "CURRENCY_EXCHANGE_RATE",
        @Query("from_currency") fromCurrency: String,
        @Query("to_currency") toCurrency: String,
        @Query("apikey") apiKey: String
    ): AlphaVantageRateResponse

    @GET("query")
    suspend fun getFxIntraday(
        @Query("function") function: String = "FX_INTRADAY",
        @Query("from_symbol") fromSymbol: String,
        @Query("to_symbol") toSymbol: String,
        @Query("interval") interval: String = "5min",
        @Query("apikey") apiKey: String
    ): AlphaVantageIntradayResponse

    @GET("query")
    suspend fun getFxDaily(
        @Query("function") function: String = "FX_DAILY",
        @Query("from_symbol") fromSymbol: String,
        @Query("to_symbol") toSymbol: String,
        @Query("outputsize") outputSize: String = "compact",
        @Query("apikey") apiKey: String
    ): AlphaVantageDailyResponse
}
