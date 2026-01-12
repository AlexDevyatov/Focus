package com.example.neuralphotoredactor.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import com.example.neuralphotoredactor.R
import com.example.neuralphotoredactor.ui.theme.BackgroundDark
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
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.screen_editor),
                        fontSize = 16.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                ),
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.back_arrow),
                                contentDescription = stringResource(R.string.navigate_back),
                                tint = Color.White
                            )
                        }
                    }
                },
                actions = {
                    if (showEditMode) {
                        IconButton(onClick = onSaveToGallery) {
                            Icon(
                                painter = painterResource(id = R.drawable.save_image),
                                contentDescription = stringResource(R.string.edit_save_to_gallery),
                                tint = Color.White
                            )
                        }
                    } else if (selectedFilters.isNotEmpty() || processedImageUri != null) {
                        IconButton(onClick = onSaveClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.save_image),
                                contentDescription = stringResource(R.string.editor_save_button),
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        // Состояние для текущего cropRect (доступно во всем Column)
        var currentCropRect by remember { mutableStateOf<Rect?>(null) }
        
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
                // Проверяем, есть ли выбранные нейрофильтры
                val neuralFilterTypes = listOf(
                    FilterType.STYLE_TRANSFER,
                    FilterType.DENOISE,
                    FilterType.UPSCALE,
                    FilterType.COLOR_CORRECTION
                )
                val hasSelectedNeuralFilters = selectedFilters.any { (filterType, _) ->
                    filterType in neuralFilterTypes
                }
                val isNeuralFilterProcessing = isLoading && hasSelectedNeuralFilters

                when {
                    // Для нейрофильтров показываем изображение с overlay, для остальных - LoadingIndicator
                    isLoading && !isNeuralFilterProcessing -> LoadingIndicator()
                    error != null -> ErrorMessage(
                        error,
                        defaultMessageId = R.string.error_process_image
                    )

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
                        // При обработке нейрофильтров показываем исходное изображение под overlay
                        AsyncImage(
                            model = imageUri,
                            contentDescription = stringResource(R.string.editor_original_image),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Overlay для кадрирования
                if (showCropOverlay && cropBitmap != null) {
                    CropOverlay(
                        bitmap = cropBitmap,
                        onCropApply = { rect ->
                            // Этот callback вызывается из внутренних кнопок CropOverlay (если showButtons = true)
                            onCropApply(rect)
                        },
                        onCropCancel = onCropCancel,
                        modifier = Modifier.fillMaxSize(),
                        showButtons = false,
                        onCropRectChange = { rect ->
                            // rect уже масштабирован в координаты bitmap
                            currentCropRect = rect
                        }
                    )
                }

                // Overlay с progress bar для нейрофильтров
                if (isNeuralFilterProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            
            // Слайдер Material Design 3 (скрывается при кадрировании)
            if (!showCropOverlay) {
                if (showEditMode) {
                    // Режим настроек
                    when (currentEditCategory) {
                        EditCategory.BRIGHTNESS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Slider(
                                    value = brightness,
                                    onValueChange = onBrightnessChange,
                                    valueRange = -1f..1f,
                                    steps = 199,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        EditCategory.CONTRAST -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Slider(
                                    value = contrast,
                                    onValueChange = onContrastChange,
                                    valueRange = -1f..1f,
                                    steps = 199,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        EditCategory.COLOR_BALANCE -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Красный канал
                                Slider(
                                    value = colorBalanceRed,
                                    onValueChange = { onColorBalanceChange(EditType.COLOR_BALANCE_RED, it) },
                                    valueRange = -1f..1f,
                                    steps = 199,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Red,
                                        activeTrackColor = Color.Red
                                    )
                                )
                                
                                // Зеленый канал
                                Slider(
                                    value = colorBalanceGreen,
                                    onValueChange = { onColorBalanceChange(EditType.COLOR_BALANCE_GREEN, it) },
                                    valueRange = -1f..1f,
                                    steps = 199,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Green,
                                        activeTrackColor = Color.Green
                                    )
                                )
                                
                                // Синий канал
                                Slider(
                                    value = colorBalanceBlue,
                                    onValueChange = { onColorBalanceChange(EditType.COLOR_BALANCE_BLUE, it) },
                                    valueRange = -1f..1f,
                                    steps = 199,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Blue,
                                        activeTrackColor = Color.Blue
                                    )
                                )
                            }
                        }
                        EditCategory.GEOMETRY -> {
                            // Для геометрических операций слайдер не показываем
                        }
                    }
                } else {
                    // Режим фильтров - показываем слайдер для последнего выбранного фильтра
                    val lastSelectedFilter = selectedFilters.lastOrNull()
                    if (lastSelectedFilter != null) {
                        val (filterType, savedIntensity) = lastSelectedFilter
                        // Показываем слайдер только для обычных фильтров (не нейросетевых)
                        val neuralFilters = listOf(
                            FilterType.STYLE_TRANSFER,
                            FilterType.DENOISE,
                            FilterType.UPSCALE,
                            FilterType.COLOR_CORRECTION
                        )
                        if (filterType !in neuralFilters) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Slider(
                                    value = savedIntensity ?: currentFilterIntensity,
                                    onValueChange = { intensity ->
                                        onIntensityChange(filterType, intensity)
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
            
            // 5 иконок в ряд: в зависимости от режима (скрываются при кадрировании)
            if (!showCropOverlay) {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (showEditMode) {
                    // Режим настроек: яркость, контрастность, баланс цветов, кадрирование, отражение
                    val lastAppliedEdit = appliedEdits.lastOrNull()?.first
                    val isFlipActive = lastAppliedEdit == EditType.FLIP_HORIZONTAL || lastAppliedEdit == EditType.FLIP_VERTICAL
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { onEditCategoryChange(EditCategory.BRIGHTNESS) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WbSunny,
                                contentDescription = stringResource(R.string.edit_brightness),
                                tint = Color.White
                            )
                        }
                        if (currentEditCategory == EditCategory.BRIGHTNESS && !showCropOverlay) {
                            Text(
                                text = stringResource(R.string.edit_brightness),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { onEditCategoryChange(EditCategory.CONTRAST) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tonality,
                                contentDescription = stringResource(R.string.edit_contrast),
                                tint = Color.White
                            )
                        }
                        if (currentEditCategory == EditCategory.CONTRAST && !showCropOverlay) {
                            Text(
                                text = stringResource(R.string.edit_contrast),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { onEditCategoryChange(EditCategory.COLOR_BALANCE) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = stringResource(R.string.edit_color_balance),
                                tint = Color.White
                            )
                        }
                        if (currentEditCategory == EditCategory.COLOR_BALANCE && !showCropOverlay) {
                            Text(
                                text = stringResource(R.string.edit_color_balance),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { onEditClick(EditType.CROP) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Crop,
                                contentDescription = stringResource(R.string.edit_crop),
                                tint = Color.White
                            )
                        }
                        if (showCropOverlay) {
                            Text(
                                text = stringResource(R.string.edit_crop),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { onEditClick(EditType.FLIP_HORIZONTAL) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = stringResource(R.string.edit_flip),
                                tint = Color.White
                            )
                        }
                        if (isFlipActive && !showCropOverlay) {
                            Text(
                                text = stringResource(R.string.edit_flip),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else {
                    // Режим фильтров: Размытие, Резкость, Виньетка, Черно-белый, Сепия
                    val filterTypes = listOf(
                        FilterType.GAUSSIAN_BLUR,
                        FilterType.SHARPEN,
                        FilterType.VIGNETTE,
                        FilterType.GRAYSCALE,
                        FilterType.SEPIA
                    )
                    val lastSelectedFilter = selectedFilters.lastOrNull()?.first
                    filterTypes.forEach { filterType ->
                        val isLastSelected = filterType == lastSelectedFilter
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { onFilterToggle(filterType) }
                            ) {
                                Icon(
                                    imageVector = when (filterType) {
                                        FilterType.GAUSSIAN_BLUR -> Icons.Filled.BlurOn
                                        FilterType.SHARPEN -> Icons.Filled.AutoFixHigh
                                        FilterType.VIGNETTE -> Icons.Filled.CenterFocusStrong
                                        FilterType.GRAYSCALE -> Icons.Filled.FilterBAndW
                                        FilterType.SEPIA -> Icons.Filled.PhotoFilter
                                        else -> Icons.Filled.BlurOn
                                    },
                                    contentDescription = getFilterName(filterType),
                                    tint = Color.White
                                )
                            }
                            if (isLastSelected) {
                                Text(
                                    text = getFilterName(filterType),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
                }
            }
            
            // Кнопки: Настройки/Фильтры или Отмена/Применить (при кадрировании)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showCropOverlay) {
                    // Кнопки для кадрирования: Отмена и Применить
                    FilledTonalButton(
                        onClick = onCropCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.edit_cancel),
                            color = Color.White
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            // currentCropRect уже содержит масштабированный Rect в координатах bitmap
                            if (currentCropRect != null) {
                                onCropApply(currentCropRect!!)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.edit_apply),
                            color = Color.White
                        )
                    }
                } else {
                    // Обычные кнопки: Настройки и Фильтры
                    FilledTonalButton(
                        onClick = {
                            if (!showEditMode) {
                                onToggleEditMode()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (showEditMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.background
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.editor_settings_button),
                            color = Color.White
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            if (showEditMode) {
                                onToggleEditMode()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (!showEditMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.background
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.editor_filters_button),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

