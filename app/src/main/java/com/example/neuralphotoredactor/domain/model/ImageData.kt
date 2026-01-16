package com.example.neuralphotoredactor.domain.model

import android.net.Uri

/**
 * Модель данных изображения для domain слоя.
 *
 * Представляет изображение с его метаданными и URI.
 * Используется для передачи данных между слоями приложения.
 *
 * @param uri URI изображения
 * @param width Ширина изображения в пикселях
 * @param height Высота изображения в пикселях
 * @param size Размер файла в байтах
 */
data class ImageData(
    val uri: Uri,
    val width: Int = 0,
    val height: Int = 0,
    val size: Long = 0L,
)
