package com.beyemart.fxMart.presentation.watchlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.beyemart.fxMart.domain.model.matchesSearchQuery
import com.beyemart.fxMart.domain.model.WorldCurrencies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyListScreen(
    onBack: () -> Unit,
    viewModel: CurrencyListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredCurrencies = remember(state.currencies, searchQuery) {
        state.currencies.filter { currency ->
            currency.matchesSearchQuery(searchQuery)
        }
    }
    val favoriteCurrencies = remember(filteredCurrencies) {
        filteredCurrencies.filter { it.isFavorite }
    }
    val otherCurrencies = remember(filteredCurrencies) {
        filteredCurrencies.filterNot { it.isFavorite }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Currencies") },
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
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search currencies") },
                    placeholder = { Text("Code, name, country") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )

                if (filteredCurrencies.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No currencies match '$searchQuery'")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (favoriteCurrencies.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Favorites",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            items(favoriteCurrencies, key = { it.code }) { currency ->
                                CurrencyItem(
                                    currency = currency
                                ) { viewModel.toggleFavorite(currency) }
                            }
                        }

                        if (otherCurrencies.isNotEmpty()) {
                            item {
                                Text(
                                    text = "All currencies",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            items(otherCurrencies, key = { it.code }) { currency ->
                                CurrencyItem(
                                    currency = currency
                                ) { viewModel.toggleFavorite(currency) }
                            }
                        }
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
fun CurrencyItem(
    currency: com.beyemart.fxMart.domain.model.Currency,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = currency.code,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${currency.name} · ${WorldCurrencies.getCountryName(currency.code)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (currency.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (currency.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
