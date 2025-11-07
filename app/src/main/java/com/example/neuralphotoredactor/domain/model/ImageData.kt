package com.example.neuralphotoredactor.domain.model

import android.net.Uri

/**
 * Модель данных изображения в доменном слое.
 * 
 * Представляет информацию об изображении, включая его расположение,
 * размеры, размер файла и временную метку. Используется для передачи
 * данных об изображениях между слоями приложения.
 * 
 * @param uri URI изображения (может быть content:// или file://)
 * @param path Путь к файлу изображения (опционально, для совместимости)
 * @param width Ширина изображения в пикселях
 * @param height Высота изображения в пикселях
 * @param size Размер файла изображения в байтах
 * @param timestamp Временная метка создания или последнего изменения (по умолчанию текущее время)
 */
data class ImageData(
    val uri: Uri,
    val path: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val size: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
