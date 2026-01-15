package com.example.neuralphotoredactor.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.FilterBAndW
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
import com.example.neuralphotoredactor.ui.components.ErrorMessage
import com.example.neuralphotoredactor.ui.components.LoadingIndicator
import com.example.neuralphotoredactor.ui.viewmodel.EditCategory
import android.graphics.Rect
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Vignette

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
    selectedFilters: List<Pair<FilterType, Float?>>,
    currentFilterIntensity: Float,
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
    onSaveClick: () -> Unit,
    onToggleEditMode: () -> Unit,
    onEditCategoryChange: (EditCategory) -> Unit,
    onEditClick: (EditType) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onColorBalanceChange: (EditType, Float) -> Unit,
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

        // Состояние для показа оверлея с кнопками Crop и Rotate
        var showCropRotateOverlay by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Изображение и иконки в одном контейнере
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Изображение
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.7f),
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
                        // Закрываем оверлей Crop/Rotate при показе cropOverlay
                        LaunchedEffect(showCropOverlay) {
                            if (showCropOverlay) {
                                showCropRotateOverlay = false
                            }
                        }
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

                // Иконки в ряд: в зависимости от режима (скрываются при кадрировании)
                // Размещаем сразу под изображением
                if (!showCropOverlay) {
                    val bottomPadding = if (showCropRotateOverlay) 132.dp else 8.dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = bottomPadding
                            ),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (showCropRotateOverlay) {
                            // Оверлей с иконками: Crop, Rotate 90, Rotate 180, Rotate 270
                            // Иконка Crop
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = {
                                        showCropRotateOverlay = false
                                        onEditClick(EditType.CROP)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Crop,
                                        contentDescription = stringResource(R.string.edit_crop),
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.edit_crop),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }

                            // Иконка Rotate 90
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = {
                                        showCropRotateOverlay = false
                                        onEditClick(EditType.ROTATE_90)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Rotate90DegreesCw,
                                        contentDescription = stringResource(R.string.edit_rotate_90),
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.edit_rotate_90),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }

                            // Иконка Rotate 180
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = {
                                        showCropRotateOverlay = false
                                        onEditClick(EditType.ROTATE_180)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Rotate90DegreesCw,
                                        contentDescription = stringResource(R.string.edit_rotate_180),
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.edit_rotate_180),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }

                            // Иконка Rotate 270
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = {
                                        showCropRotateOverlay = false
                                        onEditClick(EditType.ROTATE_270)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Rotate90DegreesCw,
                                        contentDescription = stringResource(R.string.edit_rotate_270),
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.edit_rotate_270),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                        } else if (showEditMode) {
                            // Режим настроек: яркость, контрастность, баланс цветов, кадрирование, отражение
                            val lastAppliedEdit = appliedEdits.lastOrNull()?.first
                            val isFlipActive =
                                lastAppliedEdit == EditType.FLIP_HORIZONTAL || lastAppliedEdit == EditType.FLIP_VERTICAL

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = { onEditCategoryChange(EditCategory.BRIGHTNESS) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Brightness1,
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
                                        imageVector = Icons.Filled.Contrast,
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
                                        painter = painterResource(R.drawable.triangle_circle),
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
                                    onClick = { showCropRotateOverlay = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CropRotate,
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
                                        imageVector = Icons.Filled.Flip,
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
                                        when (filterType) {
                                            FilterType.VIGNETTE -> {
                                                Icon(
                                                    painter = painterResource(R.drawable.vignette_2),
                                                    contentDescription = getFilterName(filterType),
                                                    tint = Color.White
                                                )
                                            }

                                            FilterType.SEPIA -> {
                                                Icon(
                                                    painter = painterResource(R.drawable.filter_vintage),
                                                    contentDescription = getFilterName(filterType),
                                                    tint = Color.White
                                                )
                                            }

                                            else -> {
                                                Icon(
                                                    imageVector = when (filterType) {
                                                        FilterType.GAUSSIAN_BLUR -> Icons.Filled.BlurOn
                                                        FilterType.SHARPEN -> Icons.Filled.Details
                                                        FilterType.GRAYSCALE -> Icons.Filled.FilterBAndW
                                                        else -> Icons.Filled.BlurOn
                                                    },
                                                    contentDescription = getFilterName(filterType),
                                                    tint = Color.White
                                                )
                                            }
                                        }
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
            }

            // Слайдер Material Design 3 (скрывается при кадрировании и оверлее Crop/Rotate)
            if (!showCropOverlay && !showCropRotateOverlay) {
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
                                    onValueChange = {
                                        onColorBalanceChange(
                                            EditType.COLOR_BALANCE_RED,
                                            it
                                        )
                                    },
                                    valueRange = -1f..1f,
                                    steps = 199,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFA79B),
                                        activeTrackColor = Color(0xFFFFA79B)
                                    )
                                )

                                // Зеленый канал
                                Slider(
                                    value = colorBalanceGreen,
                                    onValueChange = {
                                        onColorBalanceChange(
                                            EditType.COLOR_BALANCE_GREEN,
                                            it
                                        )
                                    },
                                    valueRange = -1f..1f,
                                    steps = 199,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF9CD49F),
                                        activeTrackColor = Color(0xFF9CD49F)
                                    )
                                )

                                // Синий канал
                                Slider(
                                    value = colorBalanceBlue,
                                    onValueChange = {
                                        onColorBalanceChange(
                                            EditType.COLOR_BALANCE_BLUE,
                                            it
                                        )
                                    },
                                    valueRange = -1f..1f,
                                    steps = 199,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF86D1EA),
                                        activeTrackColor = Color(0xFF86D1EA)
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
                        // Фильтры без слайдера
                        val filtersWithoutSlider = listOf(
                            FilterType.GRAYSCALE,
                            FilterType.SEPIA
                        )
                        if (filterType !in neuralFilters && filterType !in filtersWithoutSlider) {
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

            // Кнопки: Настройки/Фильтры или Отмена/Применить (при кадрировании и оверлее Crop/Rotate)
            if (!showCropRotateOverlay) {
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
}

