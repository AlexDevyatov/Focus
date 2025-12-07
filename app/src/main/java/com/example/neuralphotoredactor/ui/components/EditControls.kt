package com.example.neuralphotoredactor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.neuralphotoredactor.R
import com.example.neuralphotoredactor.domain.enums.EditType
import com.example.neuralphotoredactor.ui.viewmodel.EditCategory

/**
 * Компонент для управления редактированием изображений.
 * Показывает только выбранную категорию редактирования.
 */
@Composable
fun EditControls(
    currentCategory: EditCategory,
    onCategoryChange: (EditCategory) -> Unit,
    onEditClick: (EditType) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onColorBalanceChange: (EditType, Float) -> Unit,
    brightness: Float = 0f,
    contrast: Float = 0f,
    colorBalanceRed: Float = 0f,
    colorBalanceGreen: Float = 0f,
    colorBalanceBlue: Float = 0f,
    appliedEdits: List<Pair<EditType, Float>> = emptyList(),
    onClearGeometricEdits: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Меню выбора категории
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(EditCategory.entries) { category ->
                FilterChip(
                    selected = currentCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(getCategoryName(category)) }
                )
            }
        }
        
        // Отображаем только выбранную категорию
        when (currentCategory) {
            EditCategory.BRIGHTNESS -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${stringResource(R.string.edit_brightness)}: ${(brightness * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
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
                    Text(
                        text = "${stringResource(R.string.edit_contrast)}: ${(contrast * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Красный канал
                    Text(
                        text = "${stringResource(R.string.edit_color_balance_red)}: ${(colorBalanceRed * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = colorBalanceRed,
                        onValueChange = { onColorBalanceChange(EditType.COLOR_BALANCE_RED, it) },
                        valueRange = -1f..1f,
                        steps = 199,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Зеленый канал
                    Text(
                        text = "${stringResource(R.string.edit_color_balance_green)}: ${(colorBalanceGreen * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = colorBalanceGreen,
                        onValueChange = { onColorBalanceChange(EditType.COLOR_BALANCE_GREEN, it) },
                        valueRange = -1f..1f,
                        steps = 199,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Синий канал
                    Text(
                        text = "${stringResource(R.string.edit_color_balance_blue)}: ${(colorBalanceBlue * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = colorBalanceBlue,
                        onValueChange = { onColorBalanceChange(EditType.COLOR_BALANCE_BLUE, it) },
                        valueRange = -1f..1f,
                        steps = 199,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            EditCategory.GEOMETRY -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (appliedEdits.isNotEmpty()) {
                        Text(
                            text = "Применено: ${appliedEdits.size}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        TextButton(onClick = onClearGeometricEdits) {
                            Text("Сбросить")
                        }
                    }
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf(
                            EditType.CROP,
                            EditType.ROTATE_90,
                            EditType.ROTATE_180,
                            EditType.ROTATE_270,
                            EditType.FLIP_HORIZONTAL,
                            EditType.FLIP_VERTICAL
                        )) { editType ->
                            FilterChip(
                                selected = false,
                                onClick = { onEditClick(editType) },
                                label = { Text(getEditName(editType)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getEditIcon(editType),
                                        contentDescription = getEditName(editType),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getCategoryName(category: EditCategory): String {
    return when (category) {
        EditCategory.BRIGHTNESS -> stringResource(R.string.edit_brightness)
        EditCategory.CONTRAST -> stringResource(R.string.edit_contrast)
        EditCategory.COLOR_BALANCE -> stringResource(R.string.edit_color_balance)
        EditCategory.GEOMETRY -> stringResource(R.string.edit_category_geometry)
    }
}

@Composable
private fun getEditName(editType: EditType): String {
    return when (editType) {
        EditType.CROP -> stringResource(R.string.edit_crop)
        EditType.ROTATE_90 -> stringResource(R.string.edit_rotate_90)
        EditType.ROTATE_180 -> stringResource(R.string.edit_rotate_180)
        EditType.ROTATE_270 -> stringResource(R.string.edit_rotate_270)
        EditType.FLIP_HORIZONTAL -> stringResource(R.string.edit_flip_horizontal)
        EditType.FLIP_VERTICAL -> stringResource(R.string.edit_flip_vertical)
        else -> editType.name
    }
}

@Composable
private fun getEditIcon(editType: EditType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (editType) {
        EditType.CROP -> Icons.Filled.Crop
        EditType.ROTATE_90, EditType.ROTATE_180, EditType.ROTATE_270 -> Icons.Filled.RotateRight
        EditType.FLIP_HORIZONTAL -> Icons.Filled.SwapHoriz
        EditType.FLIP_VERTICAL -> Icons.Filled.SwapHoriz // Используем SwapHoriz как fallback
        else -> Icons.Filled.Edit
    }
}
