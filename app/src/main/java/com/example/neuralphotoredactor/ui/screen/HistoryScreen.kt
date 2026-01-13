package com.example.neuralphotoredactor.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Palette
import coil.compose.AsyncImage
import com.example.neuralphotoredactor.R
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.enums.EditType
import com.example.neuralphotoredactor.ui.components.ErrorMessage
import com.example.neuralphotoredactor.ui.components.LoadingIndicator
import com.example.neuralphotoredactor.ui.theme.BackgroundDark
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Flip

/**
 * Экран истории обработок.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<com.example.neuralphotoredactor.domain.model.ProcessingResult>,
    isLoading: Boolean,
    error: String?,
    onItemClick: (com.example.neuralphotoredactor.domain.model.ProcessingResult) -> Unit,
    onEditClick: (com.example.neuralphotoredactor.domain.model.ProcessingResult) -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedResult by remember { mutableStateOf<com.example.neuralphotoredactor.domain.model.ProcessingResult?>(null) }
    
    // BottomSheet для отображения деталей обработки
    selectedResult?.let { result ->
        HistoryDetailsBottomSheet(
            result = result,
            onDismiss = { selectedResult = null },
            onEditClick = {
                selectedResult = null
                onEditClick(result)
            }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.screen_history),
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
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when {
            isLoading -> {
                LoadingIndicator(Modifier.padding(paddingValues))
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorMessage(error, defaultMessageId = R.string.error_load_history)
                }
            }
            history.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.history_empty))
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = 38.dp,
                        vertical = paddingValues.calculateTopPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(history) { result ->
                        HistoryItemCard(
                            result = result,
                            onClick = { selectedResult = result }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Карточка элемента истории обработки.
 * 
 * @param result Результат обработки изображения
 * @param onClick Обработчик клика по карточке
 */
@Composable
private fun HistoryItemCard(
    result: com.example.neuralphotoredactor.domain.model.ProcessingResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = Color(0xFF313630)
    val cardShape = RoundedCornerShape(12.dp)
    
    // Форматирование даты и времени
    val dateFormat = java.text.SimpleDateFormat(
        "dd.MM.yyyy",
        java.util.Locale.getDefault()
    )
    val timeFormat = java.text.SimpleDateFormat(
        "HH:mm",
        java.util.Locale.getDefault()
    )
    val date = dateFormat.format(java.util.Date(result.timestamp))
    val time = timeFormat.format(java.util.Date(result.timestamp))
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Круглая миниатюра слева
            AsyncImage(
                model = result.processedUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Дата и время справа (прижаты к правому краю)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * BottomSheet для отображения деталей обработки изображения.
 * 
 * @param result Результат обработки изображения
 * @param onDismiss Обработчик закрытия BottomSheet
 * @param onEditClick Обработчик нажатия на кнопку "Редактировать"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDetailsBottomSheet(
    result: com.example.neuralphotoredactor.domain.model.ProcessingResult,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Карточка с изображением сверху
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF313630)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Изображение слева (прямоугольное с сохранением пропорций, прижато к левому краю)
                    AsyncImage(
                        model = result.processedUri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(4f / 3f) // Сохраняем пропорции
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Дата и время справа
                    val dateFormat = java.text.SimpleDateFormat(
                        "dd.MM.yyyy",
                        java.util.Locale.getDefault()
                    )
                    val timeFormat = java.text.SimpleDateFormat(
                        "HH:mm",
                        java.util.Locale.getDefault()
                    )
                    val date = dateFormat.format(java.util.Date(result.timestamp))
                    val time = timeFormat.format(java.util.Date(result.timestamp))
                    
                    Column(
                        modifier = Modifier.padding(end = 16.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = time,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Примененные фильтры, настройки и геометрия
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.history_applied_filters),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                
                // Парсим filterType - может содержать несколько фильтров через "_"
                val filtersList = remember(result.filterType) {
                    result.filterType.split("_").mapNotNull { filterName ->
                        try {
                            FilterType.valueOf(filterName)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    }
                }
                
                // Проверяем, применены ли настройки редактирования
                val hasEdits = result.filterType == "edited" || filtersList.isEmpty()
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Фильтры
                    items(filtersList) { filterType ->
                        FilterItemCard(filterType = filterType)
                    }
                    
                    // Настройки (brightness, contrast, color balance)
                    if (hasEdits) {
                        item {
                            FilterItemCard(
                                icon = Icons.Filled.Brightness1,
                                name = stringResource(R.string.edit_brightness)
                            )
                        }
                        item {
                            FilterItemCard(
                                icon = Icons.Filled.Contrast,
                                name = stringResource(R.string.edit_contrast)
                            )
                        }
                        item {
                            FilterItemCard(
                                icon = Icons.Filled.Palette,
                                name = stringResource(R.string.edit_color_balance)
                            )
                        }
                        // Геометрия (crop, flip, rotate)
                        item {
                            FilterItemCard(
                                icon = Icons.Filled.CropRotate,
                                name = stringResource(R.string.edit_crop)
                            )
                        }
                        item {
                            FilterItemCard(
                                icon = Icons.Filled.Flip,
                                name = stringResource(R.string.edit_flip)
                            )
                        }
                    }
                    
                    // Если не удалось распарсить и это не "edited", показываем просто текст
                    if (filtersList.isEmpty() && !hasEdits) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF313630)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = result.filterType,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Кнопка "Редактировать"
            Button(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.history_edit_button),
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Отступ снизу для безопасной зоны
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Карточка элемента фильтра/настройки/геометрии.
 */
@Composable
private fun FilterItemCard(
    filterType: FilterType? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    name: String? = null
) {
    val displayName = name ?: (filterType?.let { getFilterName(it) } ?: "")
    val displayIcon = icon ?: (filterType?.let { getFilterIcon(it) } ?: Icons.Filled.BlurOn)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF313630)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Иконка
            when {
                filterType == FilterType.VIGNETTE -> {
                    Icon(
                        painter = painterResource(R.drawable.vignette_2),
                        contentDescription = displayName,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                filterType == FilterType.SEPIA -> {
                    Icon(
                        painter = painterResource(R.drawable.filter_vintage),
                        contentDescription = displayName,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = displayIcon,
                        contentDescription = displayName,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Название
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

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
    }
}

/**
 * Получить иконку для фильтра.
 */
@Composable
private fun getFilterIcon(filterType: FilterType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (filterType) {
        FilterType.GAUSSIAN_BLUR -> Icons.Filled.BlurOn
        FilterType.SHARPEN -> Icons.Filled.Details
        FilterType.GRAYSCALE -> Icons.Filled.FilterBAndW
        FilterType.STYLE_TRANSFER -> Icons.Filled.PhotoFilter
        FilterType.DENOISE,
        FilterType.NOISE_REDUCTION -> Icons.Filled.AutoFixHigh
        FilterType.UPSCALE -> Icons.Filled.ZoomIn
        FilterType.COLOR_CORRECTION -> Icons.Filled.Palette
        else -> Icons.Filled.BlurOn
    }
}

