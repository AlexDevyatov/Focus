package com.example.neuralphotoredactor.domain.model

import android.net.Uri

/**
 * Результат обработки изображения.
 * 
 * Содержит исходное и обработанное изображение, а также информацию
 * о примененном фильтре или эффекте.
 * 
 * @param originalUri URI исходного изображения
 * @param processedUri URI обработанного изображения
 * @param filterType Тип примененного фильтра
 * @param timestamp Время обработки в миллисекундах
 */
data class ProcessingResult(
    val originalUri: Uri,
    val processedUri: Uri,
    val filterType: String,
    val timestamp: Long = System.currentTimeMillis()
)

