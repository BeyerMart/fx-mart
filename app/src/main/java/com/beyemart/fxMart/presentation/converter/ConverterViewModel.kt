package com.beyemart.fxMart.presentation.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.repository.LatestRatesLoadResult
import com.beyemart.fxMart.domain.repository.CurrencyRepository
import com.beyemart.fxMart.util.normalizeAmountInput
import com.beyemart.fxMart.util.formatMoneyWithGrouping
import com.beyemart.fxMart.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val repository: CurrencyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConverterState())
    val state = _state.asStateFlow()

    private var conversionJob: Job? = null

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.getCurrencies(),
                repository.getSortOrder()
            ) { currencyResource, sortOrder ->
                Pair(currencyResource, sortOrder)
            }.collect { (resource, sortOrder) ->
                when (resource) {
                    is Resource.Success -> {
                        val currencies = resource.data ?: emptyList()

                        _state.update { state ->
                            val currentFrom = currencies.find { it.code == state.fromCurrency?.code }
                            val currentTo = currencies.find { it.code == state.toCurrency?.code }

                            val from = currentFrom
                                ?: currencies.find { c -> c.code == "USD" }
                                ?: currencies.firstOrNull()

                            val to = currentTo
                                ?: currencies.find { c -> c.code == "EUR" }
                                ?: currencies.getOrNull(1)
                                ?: currencies.firstOrNull()

                            state.copy(
                                currencies = currencies,
                                sortOrder = sortOrder,
                                fromCurrency = from,
                                toCurrency = to,
                                isLoading = false
                            )
                        }
                        convert()
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(error = resource.message, isLoading = false) }
                    }
                    is Resource.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun onAmountChange(amount: String) {
        val normalizedAmount = normalizeAmountInput(amount)
        _state.update {
            it.copy(
                amount = normalizedAmount,
                amountInputError = validateAmount(normalizedAmount),
                error = null,
            )
        }
        convert()
    }

    fun onFromCurrencyChange(currency: Currency) {
        viewModelScope.launch {
            repository.updateLastUsed(currency.code)
        }
        _state.update { it.copy(fromCurrency = currency) }
        convert()
    }

    fun onToCurrencyChange(currency: Currency) {
        viewModelScope.launch {
            repository.updateLastUsed(currency.code)
        }
        _state.update { it.copy(toCurrency = currency) }
        convert()
    }

    fun onToggleFavorite(currency: Currency) {
        viewModelScope.launch {
            repository.toggleFavorite(currency.code, !currency.isFavorite)
        }
    }

    fun onSwapCurrencies() {
        _state.update {
            it.copy(
                fromCurrency = it.toCurrency,
                toCurrency = it.fromCurrency,
                error = null,
            )
        }
        convert()
    }

    fun retry() {
        convert()
    }

    private fun convert() {
        val currentState = _state.value
        val from = currentState.fromCurrency?.code ?: return
        val to = currentState.toCurrency?.code ?: return
        val amount = parseAmount(currentState.amount)
        val amountValidationError = validateAmount(currentState.amount)

        if (amountValidationError != null) {
            _state.update {
                it.copy(
                    convertedAmount = "",
                    amountInputError = amountValidationError,
                    isLoading = false,
                    isUsingCachedRate = false,
                )
            }
            return
        }

        val amountStr = amount ?: 0.0

        if (amountStr == 0.0) {
            _state.update {
                it.copy(
                    convertedAmount = "0.00",
                    amountInputError = null,
                    error = null,
                    isLoading = false,
                )
            }
            return
        }

        conversionJob?.cancel()
        conversionJob = viewModelScope.launch {
            repository.getLatestRates(from).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val latestRates = result.data ?: return@collect
                        val rate = resolveRate(latestRates, from, to)

                        if (rate == 0.0) {
                            fetchEurCrossRate(from, to, amountStr)
                            return@collect
                        }

                        updateConvertedAmount(
                            amount = amountStr,
                            rate = rate,
                            referenceDate = latestRates.exchangeRate.date,
                            isStale = latestRates.isStale,
                        )
                    }
                    is Resource.Error -> {
                        val latestRates = result.data
                        if (latestRates != null) {
                            val rate = resolveRate(latestRates, from, to)
                            if (rate > 0.0) {
                                updateConvertedAmount(
                                    amount = amountStr,
                                    rate = rate,
                                    referenceDate = latestRates.exchangeRate.date,
                                    isStale = true,
                                    error = result.message,
                                )
                                return@collect
                            }
                        }

                        if (from != "EUR") {
                            fetchEurCrossRate(from, to, amountStr)
                        } else {
                            _state.update {
                                it.copy(
                                    error = result.message,
                                    isLoading = false,
                                    isUsingCachedRate = latestRates?.isStale == true,
                                )
                            }
                        }
                    }
                    is Resource.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private suspend fun fetchEurCrossRate(from: String, to: String, amountStr: Double) {
        repository.getLatestRates("EUR").collect { result ->
            when (result) {
                is Resource.Success -> {
                    val latestRates = result.data ?: return@collect
                    val rates = latestRates.exchangeRate.rates
                    val rateFrom = if (from == "EUR") 1.0 else rates[from] ?: 0.0
                    val rateTo = if (to == "EUR") 1.0 else rates[to] ?: 0.0
                    val rate = if (rateFrom > 0.0 && rateTo > 0.0) rateTo / rateFrom else 0.0
                    updateConvertedAmount(
                        amount = amountStr,
                        rate = rate,
                        referenceDate = latestRates.exchangeRate.date,
                        isStale = latestRates.isStale,
                    )
                }
                is Resource.Error -> {
                    val latestRates = result.data
                    if (latestRates != null) {
                        val rates = latestRates.exchangeRate.rates
                        val rateFrom = if (from == "EUR") 1.0 else rates[from] ?: 0.0
                        val rateTo = if (to == "EUR") 1.0 else rates[to] ?: 0.0
                        val rate = if (rateFrom > 0.0 && rateTo > 0.0) rateTo / rateFrom else 0.0
                        if (rate > 0.0) {
                            updateConvertedAmount(
                                amount = amountStr,
                                rate = rate,
                                referenceDate = latestRates.exchangeRate.date,
                                isStale = true,
                                error = result.message,
                            )
                            return@collect
                        }
                    }
                    _state.update { it.copy(error = result.message, isLoading = false) }
                }
                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun resolveRate(latestRates: LatestRatesLoadResult, from: String, to: String): Double {
        val rates = latestRates.exchangeRate.rates
        var rate = if (from == to) 1.0 else rates[to] ?: 0.0

        if (rate == 0.0) {
            val baseCode = latestRates.exchangeRate.base
            val rateFrom = if (from == baseCode) 1.0 else rates[from] ?: 0.0
            val rateTo = if (to == baseCode) 1.0 else rates[to] ?: 0.0
            if (rateFrom > 0.0 && rateTo > 0.0) {
                rate = rateTo / rateFrom
            }
        }

        return rate
    }

    private fun updateConvertedAmount(
        amount: Double,
        rate: Double,
        referenceDate: java.time.LocalDate,
        isStale: Boolean,
        error: String? = null,
    ) {
        val converted = amount * rate
        _state.update {
            it.copy(
                convertedAmount = formatMoneyWithGrouping(converted),
                isLoading = false,
                error = error,
                amountInputError = null,
                isUsingCachedRate = isStale,
                lastUpdatedDate = referenceDate,
            )
        }
    }

    private fun parseAmount(amount: String): Double? {
        val normalizedAmount = amount.trim().replace(',', '.')
        val candidate = if (normalizedAmount.endsWith('.')) {
            normalizedAmount.dropLast(1)
        } else {
            normalizedAmount
        }

        return candidate.toDoubleOrNull()
    }

    private fun validateAmount(amount: String): String? {
        val trimmedAmount = amount.trim()
        if (trimmedAmount.isEmpty()) {
            return "Enter an amount"
        }

        val parsedAmount = parseAmount(trimmedAmount)
            ?: return "Enter a valid number"

        if (parsedAmount < 0.0) {
            return "Enter a non-negative amount"
        }

        return null
    }
}
