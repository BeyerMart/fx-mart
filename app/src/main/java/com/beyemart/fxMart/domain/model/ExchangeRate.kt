package com.beyemart.fxMart.domain.model

import java.time.LocalDate

data class ExchangeRate(
    val base: String,
    val date: LocalDate,
    val rates: Map<String, Double>
)
