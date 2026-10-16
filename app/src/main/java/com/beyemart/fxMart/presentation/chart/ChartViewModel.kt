package com.beyemart.fxMart.presentation.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyemart.fxMart.domain.repository.CurrencyRepository
import com.beyemart.fxMart.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val repository: CurrencyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChartState())
    val state = _state.asStateFlow()

    fun init(base: String, target: String, amount: Double) {
        val currentState = _state.value
        if (currentState.base == base && currentState.target == target && currentState.amount == amount) return
        
        val needsReload = currentState.base != base || currentState.target != target
        
        _state.update {
            it.copy(
                base = base,
                target = target,
                amount = amount,
            )
        }

        if (needsReload || currentState.historicalRates.isEmpty()) {
            loadHistoricalData()
        }
    }

    fun onTimeframeSelected(timeframe: Timeframe) {
        _state.update { it.copy(timeframe = timeframe) }
        loadHistoricalData()
    }

    fun onAmountChanged(amount: Double) {
        _state.update { it.copy(amount = amount) }
    }

    fun retry() {
        loadHistoricalData()
    }

    private fun loadHistoricalData() {
        val currentState = _state.value
        if (currentState.base.isBlank() || currentState.target.isBlank()) {
            return
        }

        viewModelScope.launch {
            repository.getHistoricalRates(
                base = currentState.base,
                target = currentState.target,
                startDate = currentState.timeframe.getStartDate(),
                endDate = LocalDate.now()
            ).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val historicalResult = result.data
                        _state.update {
                            it.copy(
                                historicalRates = historicalResult?.historicalRates ?: emptyList(),
                                isLoading = false,
                                error = null,
                                isUsingCachedData = historicalResult?.isStale == true,
                            )
                        }
                    }
                    is Resource.Error -> {
                        val historicalResult = result.data
                        _state.update {
                            it.copy(
                                historicalRates = historicalResult?.historicalRates ?: it.historicalRates,
                                isLoading = false,
                                error = result.message,
                                isUsingCachedData = historicalResult?.isStale == true || it.isUsingCachedData,
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }
}
