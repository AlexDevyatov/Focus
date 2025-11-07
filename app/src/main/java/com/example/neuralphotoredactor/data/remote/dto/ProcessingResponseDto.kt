package com.example.neuralphotoredactor.data.remote.dto

/**
 * Data Transfer Object (DTO) для ответа от API обработки изображения.
 * 
 * Используется для десериализации ответа от облачного AI сервиса.
 * Обработанное изображение приходит в формате Base64.
 * 
 * @param id Уникальный идентификатор результата обработки
 * @param processedImageBase64 Обработанное изображение в Base64 (null, если обработка не завершена)
 * @param status Статус обработки (например, "completed", "processing", "failed")
 * @param error Сообщение об ошибке (null, если обработка успешна)
 * @param processingTime Время обработки в миллисекундах
 */
data class ProcessingResponseDto(
    val id: String,
    val processedImageBase64: String?,
    val status: String,
    val error: String?,
    val processingTime: Long
)

