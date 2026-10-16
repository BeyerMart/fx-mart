package com.beyemart.fxMart.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.beyemart.fxMart.domain.model.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = "Currency Sorting",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onSortOrderChange(SortOrder.ALPHABETICAL) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.sortOrder == SortOrder.ALPHABETICAL,
                            onClick = { viewModel.onSortOrderChange(SortOrder.ALPHABETICAL) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Alphabetical (Default)", style = MaterialTheme.typography.bodyLarge)
                            Text("Sort currencies alphabetically by code", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onSortOrderChange(SortOrder.LATEST_USED) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.sortOrder == SortOrder.LATEST_USED,
                            onClick = { viewModel.onSortOrderChange(SortOrder.LATEST_USED) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Latest Used", style = MaterialTheme.typography.bodyLarge)
                            Text("Sort currencies by most recently used", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}
