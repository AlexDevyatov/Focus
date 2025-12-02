package com.example.neuralphotoredactor.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.ui.components.ErrorMessage
import com.example.neuralphotoredactor.ui.components.LoadingIndicator

/**
 * Экран редактора изображений.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    imageUri: android.net.Uri?,
    processedImageUri: android.net.Uri?,
    isLoading: Boolean,
    error: String?,
    filters: List<FilterType>,
    onFilterClick: (FilterType) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактор") },
                actions = {
                    if (processedImageUri != null) {
                        TextButton(onClick = onSaveClick) {
                            Text("Сохранить")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Изображение
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> LoadingIndicator()
                    error != null -> ErrorMessage(error)
                    processedImageUri != null -> {
                        AsyncImage(
                            model = processedImageUri,
                            contentDescription = "Обработанное изображение",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    imageUri != null -> {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Исходное изображение",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
            // Список фильтров
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = false,
                        onClick = { onFilterClick(filter) },
                        label = { Text(filter.name) }
                    )
                }
            }
        }
    }
}

