package com.beyemart.fxMart.presentation.converter

import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.SortOrder
import java.time.LocalDate

data class ConverterState(
    val currencies: List<Currency> = emptyList(),
    val fromCurrency: Currency? = null,
    val toCurrency: Currency? = null,
    val toSecondCurrency: Currency? = null,
    val amount: String = "1",
    val convertedAmount: String = "",
    val convertedSecondAmount: String = "",
    val sortOrder: SortOrder = SortOrder.ALPHABETICAL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val amountInputError: String? = null,
    val isUsingCachedRate: Boolean = false,
    val lastUpdatedDate: LocalDate? = null,
)
