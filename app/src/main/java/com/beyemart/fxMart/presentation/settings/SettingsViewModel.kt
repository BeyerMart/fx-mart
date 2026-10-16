package com.beyemart.fxMart.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beyemart.fxMart.domain.model.SortOrder
import com.beyemart.fxMart.domain.repository.CurrencyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: CurrencyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSortOrder().collect { sortOrder ->
                _state.update { it.copy(sortOrder = sortOrder) }
            }
        }
    }

    fun onSortOrderChange(sortOrder: SortOrder) {
        viewModelScope.launch {
            repository.setSortOrder(sortOrder)
        }
    }
}
