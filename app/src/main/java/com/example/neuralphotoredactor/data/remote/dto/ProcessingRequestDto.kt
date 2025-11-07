package com.example.neuralphotoredactor.data.remote.dto

/**
 * Data Transfer Object (DTO) для запроса обработки изображения через API.
 * 
 * Используется для сериализации данных при отправке запроса на облачный AI сервис.
 * Изображение передается в формате Base64 для удобства передачи через HTTP.
 * 
 * @param imageBase64 Изображение, закодированное в Base64
 * @param filterType Тип фильтра для применения (строка)
 * @param parameters Дополнительные параметры обработки (например, интенсивность эффекта)
 */
data class ProcessingRequestDto(
    val imageBase64: String,
    val filterType: String,
    val parameters: Map<String, Any> = emptyMap()
)

