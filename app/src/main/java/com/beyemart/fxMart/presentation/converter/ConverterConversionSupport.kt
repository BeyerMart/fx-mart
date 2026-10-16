package com.beyemart.fxMart.presentation.converter

import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.repository.LatestRatesLoadResult
import com.beyemart.fxMart.util.formatMoneyWithGrouping

internal enum class ConversionTargetSlot {
    PRIMARY,
    SECONDARY,
}

internal data class SelectedConversionTarget(
    val slot: ConversionTargetSlot,
    val currencyCode: String,
)

internal data class FormattedConversionAmounts(
    val primaryAmount: String,
    val secondaryAmount: String,
)

internal fun resolveSelectedTargets(
    primaryCurrency: Currency?,
    secondaryCurrency: Currency?,
): List<SelectedConversionTarget> = buildList {
    primaryCurrency?.code?.let { currencyCode ->
        add(SelectedConversionTarget(ConversionTargetSlot.PRIMARY, currencyCode))
    }
    secondaryCurrency?.code?.let { currencyCode ->
        add(SelectedConversionTarget(ConversionTargetSlot.SECONDARY, currencyCode))
    }
}

internal fun chooseDefaultSecondTarget(
    currencies: List<Currency>,
    fromCurrency: Currency?,
    primaryTargetCurrency: Currency?,
    currentSecondCurrency: Currency?,
): Currency? {
    val blockedCodes = setOfNotNull(fromCurrency?.code, primaryTargetCurrency?.code)

    val persistedSecondTarget = currentSecondCurrency
        ?.takeIf { currency -> currency.code !in blockedCodes }
        ?.let { current -> currencies.find { currency -> currency.code == current.code } }

    if (persistedSecondTarget != null) {
        return persistedSecondTarget
    }

    return currencies.firstOrNull { currency -> currency.code !in blockedCodes }
}

internal fun resolveTargetRates(
    latestRates: LatestRatesLoadResult,
    fromCode: String,
    targets: List<SelectedConversionTarget>,
): Map<ConversionTargetSlot, Double>? {
    val resolvedRates = targets.associate { target ->
        target.slot to resolveTargetRate(
            latestRates = latestRates,
            fromCode = fromCode,
            targetCode = target.currencyCode,
        )
    }

    return resolvedRates.takeIf { rates -> rates.values.all { rate -> rate > 0.0 } }
}

internal fun formatConvertedAmounts(
    amount: Double,
    targetRates: Map<ConversionTargetSlot, Double>,
): FormattedConversionAmounts {
    val primaryAmount = targetRates[ConversionTargetSlot.PRIMARY]
        ?.let { rate -> formatMoneyWithGrouping(amount * rate) }
        .orEmpty()
    val secondaryAmount = targetRates[ConversionTargetSlot.SECONDARY]
        ?.let { rate -> formatMoneyWithGrouping(amount * rate) }
        .orEmpty()

    return FormattedConversionAmounts(
        primaryAmount = primaryAmount,
        secondaryAmount = secondaryAmount,
    )
}

private fun resolveTargetRate(
    latestRates: LatestRatesLoadResult,
    fromCode: String,
    targetCode: String,
): Double {
    val rates = latestRates.exchangeRate.rates
    var rate = if (fromCode == targetCode) 1.0 else rates[targetCode] ?: 0.0

    if (rate == 0.0) {
        val baseCode = latestRates.exchangeRate.base
        val rateFrom = if (fromCode == baseCode) 1.0 else rates[fromCode] ?: 0.0
        val rateTo = if (targetCode == baseCode) 1.0 else rates[targetCode] ?: 0.0
        if (rateFrom > 0.0 && rateTo > 0.0) {
            rate = rateTo / rateFrom
        }
    }

    return rate
}
