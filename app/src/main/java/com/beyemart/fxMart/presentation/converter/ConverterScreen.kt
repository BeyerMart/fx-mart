package com.beyemart.fxMart.presentation.converter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
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
                menuSide = DropdownMenuSide.START,
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
                menuSide = DropdownMenuSide.END,
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

        if (chartState.historicalRates.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                CurrencyChart(
                    historicalRates = chartState.historicalRates,
                    base = chartState.base,
                    target = chartState.target,
                    amount = chartState.amount
                )

                if (chartState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        } else {
            if (chartState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
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
    menuSide: DropdownMenuSide,
    onCurrencySelected: (Currency) -> Unit,
    onToggleFavorite: (Currency) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val sections = remember(currencies, sortOrder) {
        buildCurrencyDropdownSections(currencies, sortOrder)
    }
    val popupPositionProvider = remember(menuSide) { DropdownPositionProvider(menuSide) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedCurrency?.code ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )

        if (expanded) {
            Popup(
                popupPositionProvider = popupPositionProvider,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier.width(320.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    if (sections.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No currencies available")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 360.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            sections.forEachIndexed { index, section ->
                                item(key = "${section.title}-header") {
                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }

                                items(section.currencies, key = { it.code }) { currency ->
                                    CurrencyDropdownMenuItem(
                                        currency = currency,
                                        onCurrencySelected = {
                                            onCurrencySelected(it)
                                            expanded = false
                                        },
                                        onToggleFavorite = onToggleFavorite,
                                    )
                                }

                                if (index < sections.lastIndex) {
                                    item(key = "${section.title}-divider") {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(top = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class DropdownMenuSide {
    START,
    END,
}

@Composable
private fun CurrencyDropdownMenuItem(
    currency: Currency,
    onCurrencySelected: (Currency) -> Unit,
    onToggleFavorite: (Currency) -> Unit,
) {
    Card(
        onClick = { onCurrencySelected(currency) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currency.code,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = currency.displayLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = { onToggleFavorite(currency) }) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = if (currency.isFavorite) {
                        "Unfavorite"
                    } else {
                        "Favorite"
                    },
                    tint = if (currency.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
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

private class DropdownPositionProvider(
    private val side: DropdownMenuSide,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val availableY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val x = when (side) {
            DropdownMenuSide.START -> {
                val leftX = anchorBounds.right - popupContentSize.width
                if (leftX >= 0) leftX else 0
            }
            DropdownMenuSide.END -> {
                val rightX = anchorBounds.left
                if (rightX + popupContentSize.width <= windowSize.width) {
                    rightX
                } else {
                    (windowSize.width - popupContentSize.width).coerceAtLeast(0)
                }
            }
        }
        val y = (anchorBounds.bottom + 8).coerceAtMost(availableY)
        return IntOffset(x, y)
    }
}

