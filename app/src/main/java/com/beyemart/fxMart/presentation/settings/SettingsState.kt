package com.beyemart.fxMart.presentation.settings

import com.beyemart.fxMart.domain.model.SortOrder

data class SettingsState(
    val sortOrder: SortOrder = SortOrder.ALPHABETICAL
)
