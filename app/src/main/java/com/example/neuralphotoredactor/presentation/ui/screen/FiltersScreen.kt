package com.example.neuralphotoredactor.presentation.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.neuralphotoredactor.presentation.ui.components.ErrorMessage
import com.example.neuralphotoredactor.presentation.ui.components.FilterItem
import com.example.neuralphotoredactor.presentation.ui.components.LoadingIndicator
import com.example.neuralphotoredactor.presentation.viewmodel.FiltersViewModel

/**
 * Экран со списком доступных AI фильтров и эффектов.
 * 
 * @param viewModel ViewModel для управления состоянием экрана
 * @param onFilterSelected Обработчик выбора фильтра (применение фильтра)
 * @param modifier Модификатор для настройки внешнего вида
 */
@Composable
fun FiltersScreen(
    viewModel: FiltersViewModel = hiltViewModel(),
    onFilterSelected: (com.example.neuralphotoredactor.domain.model.FilterPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.value

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    LoadingIndicator()
                }
                state.error != null -> {
                    ErrorMessage(
                        message = state.error,
                        onDismiss = { viewModel.clearError() }
                    )
                }
                state.filters.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No filters available")
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.filters) { filter ->
                                FilterItem(
                                    filterPreset = filter,
                                    isSelected = state.selectedFilter?.id == filter.id,
                                    onClick = {
                                        viewModel.selectFilter(filter)
                                        onFilterSelected(filter)
                                    }
                                )
                            }
                        }
                        
                        if (state.selectedFilter != null) {
                            Button(
                                onClick = { onFilterSelected(state.selectedFilter) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text("Apply ${state.selectedFilter.name}")
                            }
                        }
                    }
                }
            }
        }
    }
}

