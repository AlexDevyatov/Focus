package com.example.neuralphotoredactor.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neuralphotoredactor.domain.model.FilterPreset
import com.example.neuralphotoredactor.ui.theme.Primary
import com.example.neuralphotoredactor.ui.theme.Secondary

/**
 * Компонент для отображения элемента фильтра в списке.
 * 
 * @param filterPreset Предустановка фильтра
 * @param isSelected Флаг выбранного фильтра
 * @param onClick Обработчик клика на фильтр
 * @param modifier Модификатор для настройки внешнего вида
 */
@Composable
fun FilterItem(
    filterPreset: FilterPreset,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Secondary else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = filterPreset.name,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            filterPreset.description?.let { description ->
                Text(
                    text = description,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (filterPreset.isOnDevice) Primary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (filterPreset.isOnDevice) "On-Device" else "Cloud-Based",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

