package com.example.neuralphotoredactor.domain.model

/**
 * Модель расширенных метаданных изображения.
 * 
 * Содержит дополнительную информацию об изображении: EXIF данные,
 * ориентацию, формат файла и другую техническую информацию.
 * 
 * @param format Формат изображения (JPEG, PNG, WEBP и т.д.)
 * @param orientation Ориентация изображения (0-8, согласно EXIF)
 * @param hasAlpha Наличие альфа-канала (прозрачности)
 * @param colorSpace Цветовое пространство изображения
 * @param exifData Дополнительные EXIF данные в виде ключ-значение
 */
data class ImageMetadata(
    val format: String = "JPEG",
    val orientation: Int = 0,
    val hasAlpha: Boolean = false,
    val colorSpace: String? = null,
    val exifData: Map<String, String> = emptyMap()
)

