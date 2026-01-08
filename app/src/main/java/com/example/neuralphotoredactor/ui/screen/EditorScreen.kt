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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.neuralphotoredactor.R
import com.example.neuralphotoredactor.domain.enums.EditType
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.ui.components.CropOverlay
import com.example.neuralphotoredactor.ui.components.EditControls
import com.example.neuralphotoredactor.ui.components.ErrorMessage
import com.example.neuralphotoredactor.ui.components.LoadingIndicator
import com.example.neuralphotoredactor.ui.viewmodel.EditCategory
import android.graphics.Rect

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
        FilterType.STYLE_TRANSFER -> stringResource(R.string.filter_style_transfer)
        FilterType.DENOISE -> stringResource(R.string.filter_denoise)
        FilterType.UPSCALE -> stringResource(R.string.filter_upscale)
        FilterType.COLOR_CORRECTION -> stringResource(R.string.filter_color_correction)
        else -> filterType.name
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
    selectedFilters: List<Pair<FilterType, Float?>>,
    currentFilterIntensity: Float,
    showNeuralFilters: Boolean,
    showEditMode: Boolean,
    brightness: Float,
    contrast: Float,
    colorBalanceRed: Float,
    colorBalanceGreen: Float,
    colorBalanceBlue: Float,
    currentEditCategory: EditCategory,
    appliedEdits: List<Pair<EditType, Float>>,
    showCropOverlay: Boolean,
    cropBitmap: android.graphics.Bitmap?,
    onFilterToggle: (FilterType) -> Unit,
    onIntensityChange: (FilterType, Float) -> Unit,
    onClearFilters: () -> Unit,
    onSaveClick: () -> Unit,
    onToggleFilterCategory: () -> Unit,
    onToggleEditMode: () -> Unit,
    onEditCategoryChange: (EditCategory) -> Unit,
    onEditClick: (EditType) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onColorBalanceChange: (EditType, Float) -> Unit,
    onClearGeometricEdits: () -> Unit,
    onSaveToGallery: () -> Unit,
    onCropApply: (Rect) -> Unit,
    onCropCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_editor)) },
                actions = {
                    if (selectedFilters.isNotEmpty()) {
                        IconButton(onClick = onClearFilters) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.editor_clear_filter)
                            )
                        }
                    }
                    if (showEditMode) {
                        IconButton(onClick = onSaveToGallery) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = stringResource(R.string.edit_save_to_gallery)
                            )
                        }
                    } else if (selectedFilters.isNotEmpty() || processedImageUri != null) {
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
                
                // Overlay для кадрирования
                if (showCropOverlay) {
                    CropOverlay(
                        bitmap = cropBitmap,
                        onCropApply = onCropApply,
                        onCropCancel = onCropCancel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Переключатель режимов (фильтры/редактирование)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !showEditMode,
                    onClick = onToggleEditMode,
                    label = { Text(stringResource(R.string.edit_mode_filters)) }
                )
                FilterChip(
                    selected = showEditMode,
                    onClick = onToggleEditMode,
                    label = { Text(stringResource(R.string.edit_mode_editing)) }
                )
            }
            
            if (showEditMode) {
                // Режим редактирования - компактное меню
                EditControls(
                    currentCategory = currentEditCategory,
                    onCategoryChange = onEditCategoryChange,
                    onEditClick = onEditClick,
                    onBrightnessChange = onBrightnessChange,
                    onContrastChange = onContrastChange,
                    onColorBalanceChange = onColorBalanceChange,
                    brightness = brightness,
                    contrast = contrast,
                    colorBalanceRed = colorBalanceRed,
                    colorBalanceGreen = colorBalanceGreen,
                    colorBalanceBlue = colorBalanceBlue,
                    appliedEdits = appliedEdits,
                    onClearGeometricEdits = onClearGeometricEdits,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            } else {
                // Режим фильтров
                // Переключатель категорий фильтров
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showNeuralFilters,
                        onClick = onToggleFilterCategory,
                        label = { Text(stringResource(R.string.filter_category_regular)) }
                    )
                    FilterChip(
                        selected = showNeuralFilters,
                        onClick = onToggleFilterCategory,
                        label = { Text(stringResource(R.string.filter_category_neural)) }
                    )
                }
                
                // Список фильтров текущей категории
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filters) { filter ->
                        val isSelected = selectedFilters.any { it.first == filter }
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterToggle(filter) },
                            label = { Text(getFilterName(filter)) }
                        )
                    }
                }
                
                // Список выбранных фильтров с их слайдерами
                if (selectedFilters.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.editor_selected_filters, selectedFilters.size),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Слайдеры только для обычных фильтров (нейросетевые применяются сразу без настроек)
                        selectedFilters.forEach { (filterType, intensity) ->
                            val isNeuralFilter = filterType in listOf(
                                FilterType.STYLE_TRANSFER,
                                FilterType.DENOISE,
                                FilterType.UPSCALE,
                                FilterType.COLOR_CORRECTION
                            )
                            
                            if (isNeuralFilter) {
                                // Для нейросетевых фильтров показываем только название без слайдера
                                Text(
                                    text = getFilterName(filterType),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                // Для обычных фильтров показываем слайдер
                                val currentIntensity = intensity ?: currentFilterIntensity
                                Column(
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${getFilterName(filterType)}: ${(currentIntensity * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Slider(
                                        value = currentIntensity,
                                        onValueChange = { newIntensity ->
                                            onIntensityChange(filterType, newIntensity)
                                        },
                                        valueRange = 0f..1f,
                                        steps = 99,
                                        modifier = Modifier.fillMaxWidth()
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

