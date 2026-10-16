package com.beyemart.fxMart.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeRateDto(
    @field:Json(name = "date") val date: String,
    @field:Json(name = "base") val base: String,
    @field:Json(name = "quote") val quote: String,
    @field:Json(name = "rate") val rate: Double
)
