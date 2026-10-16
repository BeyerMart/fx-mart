@file:Suppress("unused")

package com.beyemart.fxMart.presentation.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.repository.CurrencyRepository
import com.beyemart.fxMart.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val repository: CurrencyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChartState())
    val state = _state.asStateFlow()

    private var primaryLoadJob: Job? = null
    private var comparisonLoadJob: Job? = null
    private var currenciesLoadJob: Job? = null

    init {
        loadCurrencies()
    }

    fun init(base: String, target: String, amount: Double) {
        val currentState = _state.value
        if (currentState.base == base && currentState.target == target && currentState.amount == amount) {
            return
        }

        val needsReload = currentState.base != base || currentState.target != target

        _state.update {
            it.copy(
                base = base,
                target = target,
                amount = amount,
                comparisonTarget = if (needsReload) null else it.comparisonTarget,
                comparisonHistoricalRates = if (needsReload) emptyList() else it.comparisonHistoricalRates,
                comparisonError = if (needsReload) null else it.comparisonError,
                isComparisonLoading = if (needsReload) false else it.isComparisonLoading,
            )
        }

        if (needsReload || currentState.historicalRates.isEmpty()) {
            loadHistoricalDataForCurrentTargets()
        }
    }

    fun onTimeframeSelected(timeframe: Timeframe) {
        _state.update {
            it.copy(
                timeframe = timeframe,
                error = null,
                comparisonError = null,
            )
        }
        loadHistoricalDataForCurrentTargets()
    }

    fun onAmountChanged(amount: Double) {
        _state.update { it.copy(amount = amount) }
    }

    fun onComparisonTargetSelected(currency: Currency) {
        val currentState = _state.value
        if (currency.code == currentState.base || currency.code == currentState.target) {
            return
        }

        _state.update {
            it.copy(
                comparisonTarget = currency.code,
                comparisonHistoricalRates = emptyList(),
                comparisonError = null,
                isComparisonLoading = true,
            )
        }

        loadHistoricalDataForTarget(currency.code, isComparison = true)
    }

    fun clearComparisonTarget() {
        comparisonLoadJob?.cancel()
        _state.update {
            it.copy(
                comparisonTarget = null,
                comparisonHistoricalRates = emptyList(),
                comparisonError = null,
                isComparisonLoading = false,
            )
        }
    }

    fun retry() {
        loadHistoricalDataForCurrentTargets()
    }

    private fun loadCurrencies() {
        currenciesLoadJob?.cancel()
        currenciesLoadJob = viewModelScope.launch {
            repository.getCurrencies().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _state.update {
                            it.copy(availableCurrencies = result.data ?: emptyList())
                        }
                    }
                    is Resource.Error -> Unit
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    private fun loadHistoricalDataForCurrentTargets() {
        val currentState = _state.value
        if (currentState.base.isBlank() || currentState.target.isBlank()) {
            return
        }

        loadHistoricalDataForTarget(currentState.target, isComparison = false)

        currentState.comparisonTarget?.let { comparisonTarget ->
            if (comparisonTarget != currentState.target && comparisonTarget != currentState.base) {
                loadHistoricalDataForTarget(comparisonTarget, isComparison = true)
            }
        }
    }

    private fun loadHistoricalDataForTarget(
        target: String,
        isComparison: Boolean,
    ) {
        if (isComparison) {
            comparisonLoadJob?.cancel()
        } else {
            primaryLoadJob?.cancel()
        }

        val currentState = _state.value
        val loadJob = viewModelScope.launch {
            repository.getHistoricalRates(
                base = currentState.base,
                target = target,
                startDate = currentState.timeframe.getStartDate(),
                endDate = LocalDate.now(),
            ).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val historicalResult = result.data
                        _state.update { state ->
                            if (isComparison) {
                                state.copy(
                                    comparisonHistoricalRates = historicalResult?.historicalRates ?: emptyList(),
                                    isComparisonLoading = false,
                                    comparisonError = null,
                                )
                            } else {
                                state.copy(
                                    historicalRates = historicalResult?.historicalRates ?: emptyList(),
                                    isLoading = false,
                                    error = null,
                                    isUsingCachedData = historicalResult?.isStale == true,
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        val historicalResult = result.data
                        _state.update { state ->
                            if (isComparison) {
                                state.copy(
                                    comparisonHistoricalRates = historicalResult?.historicalRates ?: state.comparisonHistoricalRates,
                                    isComparisonLoading = false,
                                    comparisonError = result.message,
                                )
                            } else {
                                state.copy(
                                    historicalRates = historicalResult?.historicalRates ?: state.historicalRates,
                                    isLoading = false,
                                    error = result.message,
                                    isUsingCachedData = historicalResult?.isStale == true || state.isUsingCachedData,
                                )
                            }
                        }
                    }
                    is Resource.Loading -> {
                        _state.update { state ->
                            if (isComparison) {
                                state.copy(
                                    isComparisonLoading = true,
                                    comparisonError = null,
                                )
                            } else {
                                state.copy(
                                    isLoading = true,
                                    error = null,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isComparison) {
            comparisonLoadJob = loadJob
        } else {
            primaryLoadJob = loadJob
        }
    }
}
