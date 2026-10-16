package com.beyemart.fxMart.presentation.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.repository.CurrencyRepository
import com.beyemart.fxMart.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrencyListViewModel @Inject constructor(
    private val repository: CurrencyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CurrencyListState())
    val state = _state.asStateFlow()

    init {
        getCurrencies()
    }

    private fun getCurrencies() {
        viewModelScope.launch {
            repository.getCurrencies().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _state.update { it.copy(currencies = result.data ?: emptyList(), isLoading = false) }
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(error = result.message, isLoading = false) }
                    }
                    is Resource.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun toggleFavorite(currency: Currency) {
        viewModelScope.launch {
            _state.update { currentState ->
                currentState.copy(
                    currencies = currentState.currencies.map { existingCurrency ->
                        if (existingCurrency.code == currency.code) {
                            existingCurrency.copy(isFavorite = !existingCurrency.isFavorite)
                        } else {
                            existingCurrency
                        }
                    }
                )
            }
            repository.toggleFavorite(currency.code, !currency.isFavorite)
        }
    }
}

data class CurrencyListState(
    val currencies: List<Currency> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
