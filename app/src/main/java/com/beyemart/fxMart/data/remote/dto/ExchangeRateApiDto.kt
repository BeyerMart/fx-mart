package com.beyemart.fxMart.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeRateApiResponse(
    @field:Json(name = "result") val result: String? = null,
    @field:Json(name = "base_code") val baseCode: String? = null,
    @field:Json(name = "conversion_rates") val conversionRates: Map<String, Double>? = null,
    @field:Json(name = "error-type") val errorType: String? = null
)
