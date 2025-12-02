package com.example.neuralphotoredactor.domain.model

import android.net.Uri

/**
 * Domain модель сессии редактирования изображения.
 * 
 * Представляет рабочую сессию пользователя с одним изображением.
 * 
 * @param id Уникальный идентификатор сессии
 * @param originalImageUri URI исходного изображения
 * @param currentImageUri URI текущего состояния изображения после примененных операций
 * @param createdAt Временная метка создания сессии (миллисекунды)
 * @param updatedAt Временная метка последнего изменения сессии (миллисекунды)
 * @param metadata Метаданные сессии (разрешение, формат файла, EXIF-данные)
 */
data class EditingSession(
    val id: Long = 0,
    val originalImageUri: Uri,
    val currentImageUri: Uri,
    val createdAt: Long,
    val updatedAt: Long,
    val metadata: SessionMetadata
)

/**
 * Метаданные сессии редактирования.
 * 
 * @param width Ширина изображения в пикселях
 * @param height Высота изображения в пикселях
 * @param format Формат файла (JPEG, PNG и т.д.)
 * @param exifData EXIF-данные в формате Map
 */
data class SessionMetadata(
    val width: Int = 0,
    val height: Int = 0,
    val format: String = "JPEG",
    val exifData: Map<String, String> = emptyMap()
)

