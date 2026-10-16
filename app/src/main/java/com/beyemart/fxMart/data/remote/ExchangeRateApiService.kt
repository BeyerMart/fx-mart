package com.beyemart.fxMart.data.remote

import com.beyemart.fxMart.data.remote.dto.ExchangeRateApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApiService {

    @GET("v6/{apiKey}/latest/{base}")
    suspend fun getLatestRates(
        @Path("apiKey") apiKey: String,
        @Path("base") base: String
    ): ExchangeRateApiResponse
}
