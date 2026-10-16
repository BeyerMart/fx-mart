package com.beyemart.fxMart.presentation.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beyemart.fxMart.domain.model.Currency
import com.beyemart.fxMart.domain.model.HistoricalRate
import com.beyemart.fxMart.domain.model.displayLabel
import com.beyemart.fxMart.domain.model.matchesSearchQuery
import com.beyemart.fxMart.util.formatMoneyWithGrouping
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    base: String,
    target: String,
    amount: Double,
    onBack: () -> Unit,
    viewModel: ChartViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isWideLayout = LocalConfiguration.current.screenWidthDp >= 700
    var isComparisonPickerVisible by rememberSaveable { mutableStateOf(false) }
    val selectableCurrencies = remember(
        state.availableCurrencies,
        state.base,
        state.target,
        state.comparisonTarget,
    ) {
        filterComparisonCurrencies(
            currencies = state.availableCurrencies,
            baseCurrencyCode = state.base,
            primaryTargetCode = state.target,
            comparisonTargetCode = state.comparisonTarget,
        )
    }

    LaunchedEffect(base, target, amount) {
        viewModel.init(base, target, amount)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${state.base} / ${state.target} Chart") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${state.base} to ${state.target}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TimeframeSelector(
                selectedTimeframe = state.timeframe,
                onTimeframeSelected = viewModel::onTimeframeSelected
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { isComparisonPickerVisible = true },
                        enabled = selectableCurrencies.isNotEmpty(),
                    ) {
                        Text(
                            text = if (state.comparisonTarget == null) {
                                "Add third currency"
                            } else {
                                "Change third currency"
                            },
                        )
                    }

                    state.comparisonTarget?.let {
                        TextButton(onClick = viewModel::clearComparisonTarget) {
                            Text("Remove")
                        }
                    }
                }

                state.comparisonTarget?.let { comparisonTarget ->
                    val comparisonCurrency = state.availableCurrencies.firstOrNull { it.code == comparisonTarget }
                    if (comparisonCurrency != null) {
                        Text(
                            text = "Third currency: ${comparisonCurrency.displayLabel()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.isUsingCachedData) {
                InlineStatusBanner(
                    message = "Showing cached historical data.",
                    onRetry = viewModel::retry,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            state.error?.let { message ->
                InlineStatusBanner(
                    message = message,
                    onRetry = viewModel::retry,
                    modifier = Modifier.padding(bottom = 16.dp),
                    isError = !state.isUsingCachedData,
                )
            }

                state.comparisonError?.let { message ->
                    InlineStatusBanner(
                        message = message,
                        onRetry = viewModel::retry,
                        modifier = Modifier.padding(bottom = 16.dp),
                        isError = true,
                    )
                }

                val isInitialLoading = state.isLoading && state.historicalRates.isEmpty()
                val comparisonTarget = state.comparisonTarget
                if (isInitialLoading) {
                Box(modifier = Modifier.height(300.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.historicalRates.isNotEmpty()) {
                    if (comparisonTarget != null && state.comparisonHistoricalRates.isNotEmpty()) {
                        ComparisonCurrencyChart(
                            primaryHistoricalRates = state.historicalRates,
                            comparisonHistoricalRates = state.comparisonHistoricalRates,
                            base = state.base,
                            primaryTarget = state.target,
                            comparisonTarget = comparisonTarget,
                            amount = state.amount,
                        )
                    } else {
                        CurrencyChart(
                            historicalRates = state.historicalRates,
                            base = state.base,
                            target = state.target,
                            amount = state.amount
                        )
                    }
            } else {
                Text(text = "No data available for this period.")
            }

                if (state.isComparisonLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
        }
    }

        if (isComparisonPickerVisible) {
            ComparisonCurrencyPickerDialog(
                currencies = selectableCurrencies,
                baseCurrencyCode = state.base,
                primaryTargetCode = state.target,
                comparisonTargetCode = state.comparisonTarget,
                onDismiss = { isComparisonPickerVisible = false },
                onCurrencySelected = {
                    viewModel.onComparisonTargetSelected(it)
                    isComparisonPickerVisible = false
                },
            )
        }
}

@Composable
fun TimeframeSelector(
    selectedTimeframe: Timeframe,
    onTimeframeSelected: (Timeframe) -> Unit
) {
    SecondaryScrollableTabRow(
        selectedTabIndex = Timeframe.entries.indexOf(selectedTimeframe),
        edgePadding = 16.dp,
        containerColor = Color.Transparent,
        divider = {},
        indicator = {}
    ) {
        Timeframe.entries.forEach { timeframe ->
            val selected = selectedTimeframe == timeframe
            Tab(
                selected = selected,
                onClick = { onTimeframeSelected(timeframe) },
                text = {
                    Text(
                        text = timeframe.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@Composable
fun CurrencyChart(
    historicalRates: List<HistoricalRate>,
    base: String,
    target: String,
    amount: Double
) {
    var selectedIndex by remember(historicalRates) { mutableStateOf<Int?>(null) }
    val selectedRate = selectedIndex?.let { historicalRates.getOrNull(it) }

    Column {
        ChartHeader(
            selectedRate = selectedRate,
            latestRate = historicalRates.lastOrNull(),
            firstRate = historicalRates.firstOrNull(),
            base = base,
            target = target,
            amount = amount
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        CustomCanvasChart(
            historicalRates = historicalRates,
            selectedIndex = selectedIndex,
            onIndexSelected = { selectedIndex = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Simple X-axis labels (Start and End date)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = historicalRates.firstOrNull()?.date?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = historicalRates.lastOrNull()?.date?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChartHeader(
    selectedRate: HistoricalRate?,
    latestRate: HistoricalRate?,
    firstRate: HistoricalRate?,
    base: String,
    target: String,
    amount: Double
) {
    val rateToShow = selectedRate ?: latestRate
    val isScrubbing = selectedRate != null
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isScrubbing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isScrubbing) "Selected Rate" else "Latest Rate",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isScrubbing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = rateToShow?.date?.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")) ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isScrubbing) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!isScrubbing && latestRate != null && firstRate != null) {
                    val change = ((latestRate.rate - firstRate.rate) / firstRate.rate) * 100
                    val changeColor = if (change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Text(
                        text = String.format(Locale.ROOT, "%+.2f%%", change),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = changeColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "1 $base = ${formatMoneyWithGrouping(rateToShow?.rate ?: 0.0)} $target",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isScrubbing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "${formatMoneyWithGrouping(amount)} $base = ${formatMoneyWithGrouping(amount * (rateToShow?.rate ?: 0.0))} $target",
                style = MaterialTheme.typography.titleMedium,
                color = if (isScrubbing) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CustomCanvasChart(
    historicalRates: List<HistoricalRate>,
    selectedIndex: Int?,
    onIndexSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    
    val rates = historicalRates.map { it.rate.toFloat() }
    val minRate = rates.minOrNull() ?: 0f
    val maxRate = rates.maxOrNull() ?: 0f
    val range = maxRate - minRate
    
    // Add vertical padding so the line doesn't hit the edges
    val padding = if (range == 0f) 0.1f else range * 0.15f
    val yMin = minRate - padding
    val yMax = maxRate + padding
    val yRange = yMax - yMin
    
    Canvas(
        modifier = modifier
            .pointerInput(historicalRates) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val initialIndex = (down.position.x / size.width * (historicalRates.size - 1)).toInt().coerceIn(0, historicalRates.size - 1)
                        onIndexSelected(initialIndex)
                        
                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val anyPressed = event.changes.any { it.pressed && it.id == pointerId }
                            if (!anyPressed) break
                            
                            val change = event.changes.first { it.id == pointerId }
                            val newIndex = (change.position.x / size.width * (historicalRates.size - 1)).toInt().coerceIn(0, historicalRates.size - 1)
                            onIndexSelected(newIndex)
                            change.consume()
                        }
                        onIndexSelected(null)
                    }
                }
            }
    ) {
        if (historicalRates.isEmpty()) return@Canvas
        
        val width = size.width
        val height = size.height
        val xStep = width / (historicalRates.size - 1).coerceAtLeast(1)
        
        val points = historicalRates.mapIndexed { index, rate ->
            val x = index * xStep
            val y = height - ((rate.rate.toFloat() - yMin) / yRange * height)
            Offset(x, y)
        }
        
        // Draw Gradient Fill
        val fillPath = Path().apply {
            moveTo(0f, height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(width, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent),
                startY = points.minOf { it.y },
                endY = height
            )
        )
        
        // Draw Line
        val linePath = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        drawPath(
            path = linePath,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx())
        )
        
        // Draw Scrubbing Indicator
        if (selectedIndex != null) {
            val selectedPoint = points.getOrNull(selectedIndex) ?: return@Canvas
            
            // Vertical Line
            drawLine(
                color = onSurfaceVariant.copy(alpha = 0.3f),
                start = Offset(selectedPoint.x, 0f),
                end = Offset(selectedPoint.x, height),
                strokeWidth = 2.dp.toPx()
            )
            
            // Point on Line
            drawCircle(
                color = primaryColor,
                radius = 8.dp.toPx(),
                center = selectedPoint
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = selectedPoint
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCurrencyChart() {
    val rates = listOf(
        HistoricalRate(LocalDate.now().minusDays(3), 0.91),
        HistoricalRate(LocalDate.now().minusDays(2), 0.92),
        HistoricalRate(LocalDate.now().minusDays(1), 0.915),
        HistoricalRate(LocalDate.now(), 0.93)
    )
    MaterialTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            CurrencyChart(
                historicalRates = rates,
                base = "USD",
                target = "EUR",
                amount = 100.0
            )
        }
    }
}

@Composable
private fun InlineStatusBanner(
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

private data class ComparisonChartSeries(
    val label: String,
    val rates: List<HistoricalRate>,
    val color: Color,
)

@Composable
fun ComparisonCurrencyChart(
    primaryHistoricalRates: List<HistoricalRate>,
    comparisonHistoricalRates: List<HistoricalRate>,
    base: String,
    primaryTarget: String,
    comparisonTarget: String,
    amount: Double,
) {
    var selectedIndex by remember(primaryHistoricalRates, comparisonHistoricalRates) {
        mutableStateOf<Int?>(null)
    }
    val selectedRate = selectedIndex?.let { primaryHistoricalRates.getOrNull(it) }
    val primaryLatest = primaryHistoricalRates.lastOrNull()
    val comparisonLatest = comparisonHistoricalRates.lastOrNull()
    val primaryColor = MaterialTheme.colorScheme.primary
    val comparisonColor = MaterialTheme.colorScheme.tertiary

    Column {
        ChartHeader(
            selectedRate = selectedRate,
            latestRate = primaryLatest,
            firstRate = primaryHistoricalRates.firstOrNull(),
            base = base,
            target = primaryTarget,
            amount = amount,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ComparisonSeriesLegend(
                label = primaryTarget,
                latestRate = primaryLatest,
                color = primaryColor,
                modifier = Modifier.weight(1f),
            )
            ComparisonSeriesLegend(
                label = comparisonTarget,
                latestRate = comparisonLatest,
                color = comparisonColor,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        ComparisonCanvasChart(
            series = listOf(
                ComparisonChartSeries(
                    label = primaryTarget,
                    rates = primaryHistoricalRates,
                    color = primaryColor,
                ),
                ComparisonChartSeries(
                    label = comparisonTarget,
                    rates = comparisonHistoricalRates,
                    color = comparisonColor,
                ),
            ),
            selectedIndex = selectedIndex,
            onIndexSelected = { selectedIndex = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = primaryHistoricalRates.firstOrNull()?.date?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = primaryHistoricalRates.lastOrNull()?.date?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ComparisonSeriesLegend(
    label: String,
    latestRate: HistoricalRate?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "● $label",
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
            Text(
                text = latestRate?.let {
                    "${formatMoneyWithGrouping(it.rate)} · ${it.date.format(DateTimeFormatter.ofPattern("MMM dd"))}"
                } ?: "Loading rate...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ComparisonCanvasChart(
    series: List<ComparisonChartSeries>,
    selectedIndex: Int?,
    onIndexSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val (yMin, yMax) = calculateRateBounds(series.map { it.rates })
    val yRange = yMax - yMin

    Canvas(
        modifier = modifier.pointerInput(series) {
            awaitPointerEventScope {
                while (true) {
                    val primarySeries = series.firstOrNull { it.rates.isNotEmpty() } ?: break
                    val down = awaitFirstDown()
                    val initialIndex = (
                        down.position.x / size.width * (primarySeries.rates.size - 1)
                        ).toInt().coerceIn(0, primarySeries.rates.size - 1)
                    onIndexSelected(initialIndex)

                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.pressed && it.id == pointerId }
                        if (!anyPressed) break

                        val change = event.changes.first { it.id == pointerId }
                        val newIndex = (
                            change.position.x / size.width * (primarySeries.rates.size - 1)
                            ).toInt().coerceIn(0, primarySeries.rates.size - 1)
                        onIndexSelected(newIndex)
                        change.consume()
                    }
                    onIndexSelected(null)
                }
            }
        }
    ) {
        val drawableSeries = series.filter { it.rates.isNotEmpty() }
        if (drawableSeries.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height

        val chartData = drawableSeries.map { chartSeries ->
            val xStep = width / (chartSeries.rates.size - 1).coerceAtLeast(1)
            val points = chartSeries.rates.mapIndexed { index, rate ->
                val x = index * xStep
                val y = height - ((rate.rate.toFloat() - yMin) / yRange * height)
                Offset(x, y)
            }
            chartSeries to points
        }

        chartData.forEachIndexed { index, (chartSeries, points) ->
            if (points.isEmpty()) return@forEachIndexed

            // Secondary/tertiary series are drawn lighter so the primary series stays dominant.
            val isPrimarySeries = index == 0
            val fillAlpha = if (isPrimarySeries) 0.18f else 0.08f
            val lineAlpha = if (isPrimarySeries) 1f else 0.55f

            val fillPath = Path().apply {
                moveTo(0f, height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(width, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(chartSeries.color.copy(alpha = fillAlpha), Color.Transparent),
                    startY = points.minOf { it.y },
                    endY = height,
                ),
            )

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = chartSeries.color.copy(alpha = lineAlpha),
                style = Stroke(width = 3.dp.toPx()),
            )
        }

        if (selectedIndex != null) {
            val selectedSeries = chartData.firstOrNull()?.second ?: return@Canvas
            val selectedPoint = selectedSeries.getOrNull(selectedIndex) ?: return@Canvas

            drawLine(
                color = onSurfaceVariant.copy(alpha = 0.3f),
                start = Offset(selectedPoint.x, 0f),
                end = Offset(selectedPoint.x, height),
                strokeWidth = 2.dp.toPx(),
            )

            chartData.forEachIndexed { index, (chartSeries, points) ->
                val point = points.getOrNull(selectedIndex) ?: return@forEachIndexed
                val pointAlpha = if (index == 0) 1f else 0.7f
                drawCircle(
                    color = chartSeries.color.copy(alpha = pointAlpha),
                    radius = 8.dp.toPx(),
                    center = point,
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = point,
                )
            }
        }
    }
}

@Composable
private fun ComparisonCurrencyPickerDialog(
    currencies: List<Currency>,
    baseCurrencyCode: String,
    primaryTargetCode: String,
    comparisonTargetCode: String?,
    onDismiss: () -> Unit,
    onCurrencySelected: (Currency) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredCurrencies = remember(
        currencies,
        searchQuery,
        baseCurrencyCode,
        primaryTargetCode,
        comparisonTargetCode,
    ) {
        filterComparisonCurrencies(
            currencies = currencies,
            baseCurrencyCode = baseCurrencyCode,
            primaryTargetCode = primaryTargetCode,
            comparisonTargetCode = comparisonTargetCode,
        ).filter { it.matchesSearchQuery(searchQuery) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .heightIn(max = 560.dp),
            ) {
                Text(
                    text = "Select third currency",
                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search currencies") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredCurrencies.isEmpty()) {
                    Text(
                        text = "No currencies available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        items(filteredCurrencies, key = { it.code }) { currency ->
                            Card(
                                onClick = { onCurrencySelected(currency) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
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
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

