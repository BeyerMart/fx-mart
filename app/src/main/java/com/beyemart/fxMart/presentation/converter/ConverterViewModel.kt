package com.beyemart.fxMart.presentation.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.repository.CurrencyRepository
import com.beyemart.fxMart.util.normalizeAmountInput
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
                            val currentSecond = currencies.find {
                                it.code == state.toSecondCurrency?.code
                            }

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
                                toSecondCurrency = chooseDefaultSecondTarget(
                                    currencies = currencies,
                                    fromCurrency = from,
                                    primaryTargetCurrency = to,
                                    currentSecondCurrency = currentSecond,
                                ),
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
        _state.update { currentState ->
            val updatedState = currentState.copy(fromCurrency = currency)
            updatedState.copy(
                toSecondCurrency = chooseDefaultSecondTarget(
                    currencies = updatedState.currencies,
                    fromCurrency = updatedState.fromCurrency,
                    primaryTargetCurrency = updatedState.toCurrency,
                    currentSecondCurrency = updatedState.toSecondCurrency,
                ),
            )
        }
        convert()
    }

    fun onToCurrencyChange(currency: Currency) {
        viewModelScope.launch {
            repository.updateLastUsed(currency.code)
        }
        _state.update { currentState ->
            val updatedState = currentState.copy(toCurrency = currency)
            updatedState.copy(
                toSecondCurrency = chooseDefaultSecondTarget(
                    currencies = updatedState.currencies,
                    fromCurrency = updatedState.fromCurrency,
                    primaryTargetCurrency = updatedState.toCurrency,
                    currentSecondCurrency = updatedState.toSecondCurrency,
                ),
            )
        }
        convert()
    }

    fun onToSecondCurrencyChange(currency: Currency) {
        viewModelScope.launch {
            repository.updateLastUsed(currency.code)
        }
        _state.update { currentState ->
            currentState.copy(
                toSecondCurrency = chooseDefaultSecondTarget(
                    currencies = currentState.currencies,
                    fromCurrency = currentState.fromCurrency,
                    primaryTargetCurrency = currentState.toCurrency,
                    currentSecondCurrency = currency,
                ),
            )
        }
        convert()
    }

    fun onToggleFavorite(currency: Currency) {
        viewModelScope.launch {
            repository.toggleFavorite(currency.code, !currency.isFavorite)
        }
    }

    fun onSwapCurrencies() {
        _state.update { currentState ->
            val swappedState = currentState.copy(
                fromCurrency = currentState.toCurrency,
                toCurrency = currentState.fromCurrency,
                error = null,
            )
            swappedState.copy(
                toSecondCurrency = chooseDefaultSecondTarget(
                    currencies = swappedState.currencies,
                    fromCurrency = swappedState.fromCurrency,
                    primaryTargetCurrency = swappedState.toCurrency,
                    currentSecondCurrency = swappedState.toSecondCurrency,
                ),
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
        currentState.toCurrency?.code ?: return
        val selectedTargets = resolveSelectedTargets(
            primaryCurrency = currentState.toCurrency,
            secondaryCurrency = currentState.toSecondCurrency,
        )
        val amount = parseAmount(currentState.amount)
        val amountValidationError = validateAmount(currentState.amount)

        if (amountValidationError != null) {
            _state.update {
                it.copy(
                    convertedAmount = "",
                    convertedSecondAmount = "",
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
                    convertedSecondAmount = "0.00",
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
                        val targetRates = resolveTargetRates(
                            latestRates = latestRates,
                            fromCode = from,
                            targets = selectedTargets,
                        )

                        if (targetRates == null) {
                            fetchEurCrossRates(from, selectedTargets, amountStr)
                            return@collect
                        }

                        updateConvertedAmounts(
                            amount = amountStr,
                            targetRates = targetRates,
                            referenceDate = latestRates.exchangeRate.date,
                            isStale = latestRates.isStale,
                        )
                    }
                    is Resource.Error -> {
                        val latestRates = result.data
                        if (latestRates != null) {
                            val targetRates = resolveTargetRates(
                                latestRates = latestRates,
                                fromCode = from,
                                targets = selectedTargets,
                            )
                            if (targetRates != null) {
                                updateConvertedAmounts(
                                    amount = amountStr,
                                    targetRates = targetRates,
                                    referenceDate = latestRates.exchangeRate.date,
                                    isStale = true,
                                    error = result.message,
                                )
                                return@collect
                            }
                        }

                        if (from != "EUR") {
                            fetchEurCrossRates(from, selectedTargets, amountStr)
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

    private suspend fun fetchEurCrossRates(
        from: String,
        targets: List<SelectedConversionTarget>,
        amountStr: Double,
    ) {
        repository.getLatestRates("EUR").collect { result ->
            when (result) {
                is Resource.Success -> {
                    val latestRates = result.data ?: return@collect
                    val targetRates = resolveTargetRates(
                        latestRates = latestRates,
                        fromCode = from,
                        targets = targets,
                    )

                    if (targetRates == null) {
                        _state.update {
                            it.copy(
                                error = "Unable to resolve exchange rate for the selected currencies",
                                isLoading = false,
                                isUsingCachedRate = latestRates.isStale,
                            )
                        }
                        return@collect
                    }

                    updateConvertedAmounts(
                        amount = amountStr,
                        targetRates = targetRates,
                        referenceDate = latestRates.exchangeRate.date,
                        isStale = latestRates.isStale,
                    )
                }
                is Resource.Error -> {
                    val latestRates = result.data
                    if (latestRates != null) {
                        val targetRates = resolveTargetRates(
                            latestRates = latestRates,
                            fromCode = from,
                            targets = targets,
                        )

                        if (targetRates != null) {
                            updateConvertedAmounts(
                                amount = amountStr,
                                targetRates = targetRates,
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

    private fun updateConvertedAmounts(
        amount: Double,
        targetRates: Map<ConversionTargetSlot, Double>,
        referenceDate: java.time.LocalDate,
        isStale: Boolean,
        error: String? = null,
    ) {
        val convertedAmounts = formatConvertedAmounts(
            amount = amount,
            targetRates = targetRates,
        )
        _state.update {
            it.copy(
                convertedAmount = convertedAmounts.primaryAmount,
                convertedSecondAmount = convertedAmounts.secondaryAmount,
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
