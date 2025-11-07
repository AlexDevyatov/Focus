package com.example.neuralphotoredactor.presentation.state

import com.example.neuralphotoredactor.domain.model.ImageData

/**
 * Состояние UI для экрана редактора изображений.
 * 
 * Используется в ViewModel для управления состоянием экрана редактора.
 * Содержит текущее изображение, флаг загрузки и возможные ошибки.
 * 
 * @param currentImage Текущее изображение, открытое в редакторе (null, если изображение не выбрано)
 * @param isLoading Флаг, указывающий, выполняется ли какая-либо операция (загрузка, обработка)
 * @param error Сообщение об ошибке (null, если ошибок нет)
 */
data class ImageEditorState(
    val currentImage: ImageData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

