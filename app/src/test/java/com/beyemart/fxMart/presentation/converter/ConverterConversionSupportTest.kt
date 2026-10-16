package com.beyemart.fxMart.presentation.converter

import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.ExchangeRate
import com.beyemart.fxMart.domain.repository.LatestRatesLoadResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ConverterConversionSupportTest {

    @Test
    fun chooseDefaultSecondTarget_keepsExistingDistinctCurrency() {
        val usd = Currency(code = "USD", name = "US Dollar")
        val eur = Currency(code = "EUR", name = "Euro")
        val gbp = Currency(code = "GBP", name = "British Pound")

        val result = chooseDefaultSecondTarget(
            currencies = listOf(usd, eur, gbp),
            fromCurrency = usd,
            primaryTargetCurrency = eur,
            currentSecondCurrency = gbp,
        )

        assertEquals("GBP", result?.code)
    }

    @Test
    fun chooseDefaultSecondTarget_fallsBackToFirstAvailableDistinctCurrency() {
        val usd = Currency(code = "USD", name = "US Dollar")
        val eur = Currency(code = "EUR", name = "Euro")
        val gbp = Currency(code = "GBP", name = "British Pound")

        val result = chooseDefaultSecondTarget(
            currencies = listOf(usd, eur, gbp),
            fromCurrency = usd,
            primaryTargetCurrency = eur,
            currentSecondCurrency = eur,
        )

        assertEquals("GBP", result?.code)
    }

    @Test
    fun chooseDefaultSecondTarget_returnsNullWhenNoDistinctCurrencyExists() {
        val usd = Currency(code = "USD", name = "US Dollar")
        val eur = Currency(code = "EUR", name = "Euro")

        val result = chooseDefaultSecondTarget(
            currencies = listOf(usd, eur),
            fromCurrency = usd,
            primaryTargetCurrency = eur,
            currentSecondCurrency = eur,
        )

        assertNull(result)
    }

    @Test
    fun resolveTargetRates_returnsRatesForPrimaryAndSecondaryTargets() {
        val latestRates = LatestRatesLoadResult(
            exchangeRate = ExchangeRate(
                base = "USD",
                date = LocalDate.of(2026, 10, 16),
                rates = mapOf(
                    "EUR" to 0.9,
                    "CHF" to 0.8,
                ),
            ),
            isStale = false,
        )

        val result = resolveTargetRates(
            latestRates = latestRates,
            fromCode = "USD",
            targets = resolveSelectedTargets(
                primaryCurrency = Currency(code = "EUR", name = "Euro"),
                secondaryCurrency = Currency(code = "CHF", name = "Swiss Franc"),
            ),
        )

        assertEquals(0.9, result?.get(ConversionTargetSlot.PRIMARY) ?: 0.0, 0.0001)
        assertEquals(0.8, result?.get(ConversionTargetSlot.SECONDARY) ?: 0.0, 0.0001)
    }

    @Test
    fun resolveTargetRates_returnsNullWhenAnyTargetRateIsMissing() {
        val latestRates = LatestRatesLoadResult(
            exchangeRate = ExchangeRate(
                base = "USD",
                date = LocalDate.of(2026, 10, 16),
                rates = mapOf("EUR" to 0.9),
            ),
            isStale = false,
        )

        val result = resolveTargetRates(
            latestRates = latestRates,
            fromCode = "USD",
            targets = resolveSelectedTargets(
                primaryCurrency = Currency(code = "EUR", name = "Euro"),
                secondaryCurrency = Currency(code = "CHF", name = "Swiss Franc"),
            ),
        )

        assertNull(result)
    }

    @Test
    fun formatConvertedAmounts_formatsBothTargetsIndependently() {
        val result = formatConvertedAmounts(
            amount = 2.0,
            targetRates = mapOf(
                ConversionTargetSlot.PRIMARY to 0.9,
                ConversionTargetSlot.SECONDARY to 0.8,
            ),
        )

        assertEquals("1.80", result.primaryAmount)
        assertEquals("1.60", result.secondaryAmount)
    }
}

