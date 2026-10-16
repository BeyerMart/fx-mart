package com.beyemart.fxMart.presentation.converter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.SortOrder
import com.beyemart.fxMart.domain.model.displayLabel
import com.beyemart.fxMart.presentation.chart.ChartViewModel
import com.beyemart.fxMart.presentation.chart.CurrencyChart
import com.beyemart.fxMart.presentation.chart.TimeframeSelector
import com.beyemart.fxMart.util.AmountInputVisualTransformation
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val chartViewModel: ChartViewModel = hiltViewModel()
    val chartState by chartViewModel.state.collectAsState()

    LaunchedEffect(state.fromCurrency?.code, state.toCurrency?.code, state.amount) {
        val from = state.fromCurrency?.code ?: return@LaunchedEffect
        val to = state.toCurrency?.code ?: return@LaunchedEffect
        val amount = state.amount.removeSuffix(".").toDoubleOrNull() ?: 1.0
        chartViewModel.init(from, to, amount)
        chartViewModel.onAmountChanged(amount)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Currency Converter",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = state.amount,
            onValueChange = { viewModel.onAmountChange(it) },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            visualTransformation = AmountInputVisualTransformation(),
            isError = state.amountInputError != null,
            modifier = Modifier.fillMaxWidth()
        )

        state.amountInputError?.let { amountError ->
            Text(
                text = amountError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencyDropdown(
                label = "From",
                selectedCurrency = state.fromCurrency,
                currencies = state.currencies,
                sortOrder = state.sortOrder,
                onCurrencySelected = { viewModel.onFromCurrencyChange(it) },
                onToggleFavorite = { viewModel.onToggleFavorite(it) },
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { viewModel.onSwapCurrencies() },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = "Swap")
            }

            CurrencyDropdown(
                label = "To",
                selectedCurrency = state.toCurrency,
                currencies = state.currencies,
                sortOrder = state.sortOrder,
                onCurrencySelected = { viewModel.onToCurrencyChange(it) },
                onToggleFavorite = { viewModel.onToggleFavorite(it) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Converted Amount",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "${state.convertedAmount} ${state.toCurrency?.code ?: ""}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                state.lastUpdatedDate?.let { referenceDate ->
                    Text(
                        text = "Reference date: ${referenceDate.format(DateTimeFormatter.ISO_DATE)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }

        if (state.isUsingCachedRate) {
            Spacer(modifier = Modifier.height(12.dp))
            StaleDataBanner(
                message = "Showing cached conversion data.",
                onRetry = viewModel::retry,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TimeframeSelector(
            selectedTimeframe = chartState.timeframe,
            onTimeframeSelected = chartViewModel::onTimeframeSelected
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (chartState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        } else if (chartState.historicalRates.isNotEmpty()) {
            CurrencyChart(
                historicalRates = chartState.historicalRates,
                base = chartState.base,
                target = chartState.target,
                amount = chartState.amount
            )
        } else {
            chartState.error?.let { chartError ->
                StaleDataBanner(
                    message = chartError,
                    isError = !chartState.isUsingCachedData,
                    onRetry = chartViewModel::retry,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }

        state.error?.let {
            StaleDataBanner(
                message = it,
                isError = !state.isUsingCachedRate,
                onRetry = viewModel::retry,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdown(
    label: String,
    selectedCurrency: Currency?,
    currencies: List<Currency>,
    sortOrder: SortOrder,
    onCurrencySelected: (Currency) -> Unit,
    onToggleFavorite: (Currency) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val comparator = remember(sortOrder) {
        when (sortOrder) {
            SortOrder.ALPHABETICAL -> compareBy<Currency> { it.code }
            SortOrder.LATEST_USED -> compareByDescending<Currency> { it.lastUsedTime }.thenBy { it.code }
        }
    }

    val favorites = remember(currencies, sortOrder) {
        currencies.filter { it.isFavorite }.sortedWith(comparator)
    }
    val allCurrencies = remember(currencies, sortOrder) {
        currencies.sortedWith(comparator)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCurrency?.code ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (favorites.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("Favorites", color = MaterialTheme.colorScheme.primary) },
                    onClick = {},
                    enabled = false,
                )
                favorites.forEach { currency ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(currency.displayLabel(), modifier = Modifier.weight(1f))
                                IconButton(onClick = { onToggleFavorite(currency) }) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Unfavorite",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onCurrencySelected(currency)
                            expanded = false
                        }
                    )
                }
                HorizontalDivider()
            }

            DropdownMenuItem(
                text = { Text("All currencies", color = MaterialTheme.colorScheme.primary) },
                onClick = {},
                enabled = false,
            )
            allCurrencies.forEach { currency ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(currency.displayLabel(), modifier = Modifier.weight(1f))
                            IconButton(onClick = { onToggleFavorite(currency) }) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = if (currency.isFavorite) "Unfavorite" else "Favorite",
                                    tint = if (currency.isFavorite) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                )
                            }
                        }
                    },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun StaleDataBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
