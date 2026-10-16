package com.beyemart.fxMart.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AlphaVantageRateData(
    @field:Json(name = "1. From_Currency Code") val fromCode: String? = null,
    @field:Json(name = "3. To_Currency Code") val toCode: String? = null,
    @field:Json(name = "5. Exchange Rate") val exchangeRate: String? = null,
    @field:Json(name = "6. Last Refreshed") val lastRefreshed: String? = null
)

@JsonClass(generateAdapter = true)
data class AlphaVantageRateResponse(
    @field:Json(name = "Realtime Currency Exchange Rate") val rateData: AlphaVantageRateData? = null,
    @field:Json(name = "Note") val note: String? = null,
    @field:Json(name = "Error Message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class AlphaVantageTimeSeriesBar(
    @field:Json(name = "1. open") val open: String? = null,
    @field:Json(name = "2. high") val high: String? = null,
    @field:Json(name = "3. low") val low: String? = null,
    @field:Json(name = "4. close") val close: String? = null
)

@JsonClass(generateAdapter = true)
data class AlphaVantageIntradayResponse(
    @field:Json(name = "Time Series FX (5min)") val timeSeries: Map<String, AlphaVantageTimeSeriesBar>? = null,
    @field:Json(name = "Note") val note: String? = null,
    @field:Json(name = "Error Message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class AlphaVantageDailyResponse(
    @field:Json(name = "Time Series FX (Daily)") val timeSeries: Map<String, AlphaVantageTimeSeriesBar>? = null,
    @field:Json(name = "Note") val note: String? = null,
    @field:Json(name = "Error Message") val errorMessage: String? = null
)
