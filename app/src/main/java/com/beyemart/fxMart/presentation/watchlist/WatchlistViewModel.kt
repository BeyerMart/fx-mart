package com.beyemart.fxMart.presentation.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyemart.fxMart.domain.model.WorldCurrencies
import com.beyemart.fxMart.domain.repository.CurrencyRepository
import com.beyemart.fxMart.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: CurrencyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WatchlistState())
    val state = _state.asStateFlow()

    init {
        getFavorites()
    }

    private fun getFavorites() {
        combine(
            repository.getFavoriteCurrencies(),
            _state.map { it.baseCurrencyCode }.distinctUntilChanged(),
        ) { favorites, baseCode ->
            Pair(favorites, baseCode)
        }.flatMapLatest { (favorites, baseCode) ->
            if (favorites.isEmpty()) {
                flowOf(Resource.Success(emptyList()))
            } else {
                repository.getLatestRates(baseCode).map { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val rates: Map<String, Double> =
                                resource.data?.exchangeRate?.rates ?: emptyMap()
                            Resource.Success(
                                favorites.map { currency ->
                                    CurrencyRate(
                                        code = currency.code,
                                        name = currency.name,
                                        country = WorldCurrencies.getCountryName(currency.code),
                                        rate = rates[currency.code] ?: 0.0,
                                        isFavorite = true
                                    )
                                }
                            )
                        }
                        is Resource.Error -> Resource.Error(resource.message ?: "Unknown error")
                        is Resource.Loading -> Resource.Loading()
                    }
                }
            }
        }.onEach { result ->
            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(favorites = result.data ?: emptyList(), isLoading = false) }
                }
                is Resource.Error -> {
                    _state.update { it.copy(error = result.message, isLoading = false) }
                }
                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun toggleFavorite(currencyCode: String) {
        viewModelScope.launch {
            repository.toggleFavorite(currencyCode, isFavorite = false)
        }
    }
}
