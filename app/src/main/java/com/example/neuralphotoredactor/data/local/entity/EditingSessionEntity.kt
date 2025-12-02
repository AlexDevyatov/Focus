package com.example.neuralphotoredactor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения сессии редактирования изображения.
 * 
 * Ключевая сущность для работы с изображениями. Представляет рабочую сессию
 * пользователя с одним изображением.
 * 
 * @param id Уникальный идентификатор сессии
 * @param originalImageUri URI исходного изображения
 * @param currentImageUri URI текущего состояния изображения после примененных операций
 * @param createdAt Временная метка создания сессии (миллисекунды)
 * @param updatedAt Временная метка последнего изменения сессии (миллисекунды)
 * @param metadata Метаданные сессии в формате JSON (разрешение, формат файла, EXIF-данные)
 */
@Entity(tableName = "editing_sessions")
data class EditingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalImageUri: String,
    val currentImageUri: String,
    val createdAt: Long,
    val updatedAt: Long,
    val metadata: String // JSON строка с метаданными
)

