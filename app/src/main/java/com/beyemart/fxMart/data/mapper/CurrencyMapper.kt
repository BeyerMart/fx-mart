package com.beyemart.fxMart.data.mapper

import com.beyemart.fxMart.data.local.entity.CurrencyEntity
import com.beyemart.fxMart.data.local.entity.ExchangeRateEntity
import com.beyemart.fxMart.data.remote.dto.CurrencyDto
import com.beyemart.fxMart.data.remote.dto.ExchangeRateDto
import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.ExchangeRate
import com.beyemart.fxMart.domain.model.HistoricalRate
import java.time.LocalDate

fun CurrencyEntity.toCurrency(): Currency {
    return Currency(
        code = code,
        name = name,
        isFavorite = isFavorite,
        lastUsedTime = lastUsedTime
    )
}

fun CurrencyDto.toCurrencyEntity(): CurrencyEntity {
    return CurrencyEntity(
        code = isoCode,
        name = name,
        lastUsedTime = 0L
    )
}

fun List<ExchangeRateDto>.toExchangeRate(): ExchangeRate? {
    if (isEmpty()) return null
    val first = first()
    return ExchangeRate(
        base = first.base,
        date = LocalDate.parse(first.date),
        rates = associate { it.quote to it.rate }
    )
}

fun List<ExchangeRateDto>.toExchangeRateEntities(isHistorical: Boolean): List<ExchangeRateEntity> {
    return map {
        ExchangeRateEntity(
            base = it.base,
            target = it.quote,
            rate = it.rate,
            date = it.date,
            isHistorical = isHistorical
        )
    }
}

fun List<ExchangeRateEntity>.toExchangeRate(base: String, date: String): ExchangeRate {
    return ExchangeRate(
        base = base,
        date = LocalDate.parse(date),
        rates = associate { it.target to it.rate }
    )
}

fun List<ExchangeRateEntity>.toHistoricalRates(): List<HistoricalRate> {
    return map {
        HistoricalRate(
            date = LocalDate.parse(it.date),
            rate = it.rate
        )
    }
}
