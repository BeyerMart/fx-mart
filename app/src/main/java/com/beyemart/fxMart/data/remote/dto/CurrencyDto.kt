package com.beyemart.fxMart.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CurrencyDto(
    @field:Json(name = "iso_code") val isoCode: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "symbol") val symbol: String? = null,
    @field:Json(name = "iso_numeric") val isoNumeric: String? = null,
    @field:Json(name = "start_date") val startDate: String? = null
)
