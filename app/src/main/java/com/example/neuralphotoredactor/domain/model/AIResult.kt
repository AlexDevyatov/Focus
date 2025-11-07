package com.example.neuralphotoredactor.domain.model

import com.example.neuralphotoredactor.domain.enums.ProcessingStatus

/**
 * Модель результата обработки изображения AI алгоритмами.
 * 
 * Содержит информацию о результате обработки: исходное и обработанное изображение,
 * статус обработки, возможные ошибки и время выполнения операции.
 * 
 * @param id Уникальный идентификатор результата обработки
 * @param originalImage Исходное изображение, которое было обработано
 * @param processedImage Обработанное изображение (null, если обработка не завершена или завершилась с ошибкой)
 * @param status Текущий статус обработки
 * @param error Сообщение об ошибке (если обработка завершилась с ошибкой)
 * @param processingTime Время обработки в миллисекундах
 */
data class AIResult(
    val id: String,
    val originalImage: ImageData,
    val processedImage: ImageData?,
    val status: ProcessingStatus,
    val error: String? = null,
    val processingTime: Long = 0L
)

