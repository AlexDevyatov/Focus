package com.example.neuralphotoredactor.presentation.state

import com.example.neuralphotoredactor.domain.model.ImageData

/**
 * Состояние UI для экрана галереи.
 * 
 * @param images Список всех изображений из галереи
 * @param isLoading Флаг загрузки изображений
 * @param error Сообщение об ошибке
 */
data class GalleryState(
    val images: List<ImageData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

