package com.beyemart.fxMart.domain.model

import java.time.LocalDate

data class HistoricalRate(
    val date: LocalDate,
    val rate: Double
)
