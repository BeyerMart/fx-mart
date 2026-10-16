package com.beyemart.fxMart.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object ConverterRoute : NavKey

@Serializable
object WatchlistRoute : NavKey

@Serializable
object CurrencyListRoute : NavKey

@Serializable
object SettingsRoute : NavKey

@Serializable
data class ChartRoute(val base: String, val target: String, val amount: Double = 1.0) : NavKey
