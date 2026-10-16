package com.beyemart.fxMart.presentation.watchlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.beyemart.fxMart.util.formatMoneyWithGrouping

@Composable
fun WatchlistScreen(
    onAddFavorite: () -> Unit,
    onViewChart: (String, String) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val filteredFavorites = remember(state.favorites, state.searchQuery) {
        if (state.searchQuery.isBlank()) {
            state.favorites
        } else {
            state.favorites.filter {
                it.code.contains(state.searchQuery, ignoreCase = true) ||
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                it.country.contains(state.searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFavorite) {
                Icon(Icons.Default.Add, contentDescription = "Add Currency")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Watchlist",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Base Currency: ${state.baseCurrencyCode}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = { Text("Search watchlist favorites") },
                placeholder = { Text("Search by USD, EUR...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            if (state.isLoading && state.favorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.favorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No favorites yet. Tap + to search & add new currencies!")
                }
            } else if (state.searchQuery.isNotBlank() && filteredFavorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No favorites match '${state.searchQuery}'")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFavorites) { favorite ->
                        WatchlistItem(
                            favorite = favorite,
                            onToggleFavorite = { viewModel.toggleFavorite(favorite.code) }
                        ) { onViewChart(state.baseCurrencyCode, favorite.code) }
                    }
                }
            }

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
fun WatchlistItem(
    favorite: CurrencyRate,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = favorite.code,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${favorite.name} · ${favorite.country}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (favorite.rate > 0.0) {
                    Text(
                        text = formatMoneyWithGrouping(favorite.rate),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (favorite.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (favorite.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
