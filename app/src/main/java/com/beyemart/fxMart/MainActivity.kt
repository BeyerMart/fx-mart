package com.beyemart.fxMart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.beyemart.fxMart.presentation.chart.ChartScreen
import com.beyemart.fxMart.presentation.converter.ConverterScreen
import com.beyemart.fxMart.presentation.navigation.*
import com.beyemart.fxMart.presentation.settings.SettingsScreen
import com.beyemart.fxMart.presentation.watchlist.CurrencyListScreen
import com.beyemart.fxMart.presentation.watchlist.WatchlistScreen
import com.beyemart.fxMart.ui.theme.FxMartTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FxMartTheme(darkTheme = true) {
                BeyeApp()
            }
        }
    }
}

@Composable
fun BeyeApp() {
    val topLevelRoutes = remember { setOf(ConverterRoute, WatchlistRoute, SettingsRoute) }
    val navigationState = rememberNavigationState(
        startRoute = ConverterRoute,
        topLevelRoutes = topLevelRoutes,
    )
    val navigator = remember { Navigator(navigationState) }
    
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val isExpanded = adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    val entryProvider = remember {
        entryProvider {
            entry<ConverterRoute> {
                ConverterScreen()
            }
            entry<WatchlistRoute> {
                WatchlistScreen(
                    onAddFavorite = { navigator.navigate(CurrencyListRoute) },
                    onViewChart = { base, target ->
                        navigator.navigate(ChartRoute(base, target))
                    }
                )
            }
            entry<CurrencyListRoute> {
                CurrencyListScreen(onBack = { navigator.goBack() })
            }
            entry<SettingsRoute> {
                SettingsScreen()
            }
            entry<ChartRoute> { route ->
                ChartScreen(
                    base = route.base,
                    target = route.target,
                    amount = route.amount,
                    onBack = { navigator.goBack() },
                )
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!isExpanded) {
                BeyeBottomBar(
                    currentRoute = navigationState.topLevelRoute
                ) { navigator.navigate(it) }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isExpanded) {
                BeyeNavRail(
                    currentRoute = navigationState.topLevelRoute
                ) { navigator.navigate(it) }
            }
            
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                NavDisplay(
                    entries = navigationState.toEntries(entryProvider)
                ) { navigator.goBack() }
            }
        }
    }
}

@Composable
fun BeyeBottomBar(
    currentRoute: NavKey,
    onNavigate: (NavKey) -> Unit
) {
    NavigationBar {
        val items = listOf(
            NavigationItem("Converter", ConverterRoute, Icons.Default.SwapHoriz),
            NavigationItem("Watchlist", WatchlistRoute, Icons.AutoMirrored.Filled.List),
            NavigationItem("Settings", SettingsRoute, Icons.Default.Settings)
        )
        
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

@Composable
fun BeyeNavRail(
    currentRoute: NavKey,
    onNavigate: (NavKey) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        val items = listOf(
            NavigationItem("Converter", ConverterRoute, Icons.Default.SwapHoriz),
            NavigationItem("Watchlist", WatchlistRoute, Icons.AutoMirrored.Filled.List),
            NavigationItem("Settings", SettingsRoute, Icons.Default.Settings)
        )
        
        items.forEach { item ->
            NavigationRailItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

data class NavigationItem(
    val label: String,
    val route: NavKey,
    val icon: ImageVector
)
