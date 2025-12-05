package com.example.neuralphotoredactor.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.neuralphotoredactor.R
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.ui.components.ErrorMessage
import com.example.neuralphotoredactor.ui.components.LoadingIndicator

/**
 * Получить локализованное название фильтра.
 */
@Composable
private fun getFilterName(filterType: FilterType): String {
    return when (filterType) {
        FilterType.GAUSSIAN_BLUR -> stringResource(R.string.filter_gaussian_blur)
        FilterType.NOISE_REDUCTION -> stringResource(R.string.filter_noise_reduction)
        FilterType.SHARPEN -> stringResource(R.string.filter_sharpen)
        FilterType.VIGNETTE -> stringResource(R.string.filter_vignette)
        FilterType.GRAYSCALE -> stringResource(R.string.filter_grayscale)
        FilterType.SEPIA -> stringResource(R.string.filter_sepia)
        else -> filterType.name // Для старых фильтров используем имя enum
    }
}

/**
 * Экран редактора изображений.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    imageUri: android.net.Uri?,
    processedImageUri: android.net.Uri?,
    previewBitmap: android.graphics.Bitmap?,
    isLoading: Boolean,
    error: String?,
    filters: List<FilterType>,
    selectedFilter: FilterType?,
    filterIntensity: Float,
    onFilterClick: (FilterType) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onClearFilter: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_editor)) },
                actions = {
                    if (selectedFilter != null) {
                        IconButton(onClick = onClearFilter) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.editor_clear_filter)
                            )
                        }
                    }
                    if (processedImageUri != null) {
                        IconButton(onClick = onSaveClick) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = stringResource(R.string.editor_save_button)
                            )
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
                    error != null -> ErrorMessage(error, defaultMessageId = R.string.error_process_image)
                    previewBitmap != null -> {
                        // Отображаем быстрый предпросмотр
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.editor_processed_image),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    processedImageUri != null -> {
                        AsyncImage(
                            model = processedImageUri,
                            contentDescription = stringResource(R.string.editor_processed_image),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    imageUri != null -> {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = stringResource(R.string.editor_original_image),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
            // Список фильтров
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = filter == selectedFilter,
                        onClick = { onFilterClick(filter) },
                        label = { Text(getFilterName(filter)) }
                    )
                }
            }
            
            // Слайдер интенсивности (показывается только при выбранном фильтре)
            if (selectedFilter != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.editor_filter_intensity),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Slider(
                        value = filterIntensity,
                        onValueChange = onIntensityChange,
                        valueRange = 0f..1f,
                        steps = 99, // 100 шагов (0.0 до 1.0 с шагом 0.01)
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(filterIntensity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

