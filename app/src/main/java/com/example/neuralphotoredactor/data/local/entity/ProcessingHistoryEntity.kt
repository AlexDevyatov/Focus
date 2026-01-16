package com.example.neuralphotoredactor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения истории обработки изображений в Room Database.
 *
 * Хранит только URI изображений и метаданные.
 * Bitmap не хранится в БД согласно правилам архитектуры.
 * Информация о примененных фильтрах хранится в таблице processing_operations.
 *
 * **Связи с другими сущностями:**
 * - Имеет связь один-ко-многим с ProcessingOperationEntity через historyId
 * - Все операции обработки для этой записи истории можно получить через historyId
 * - Используется для отображения истории в UI (HistoryScreen)
 *
 * @param id Уникальный идентификатор записи
 * @param originalUri URI исходного изображения (строка)
 * @param processedUri URI обработанного изображения (строка)
 * @param timestamp Время обработки в миллисекундах
 */
@Entity(
    tableName = "processing_history",
)
data class ProcessingHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalUri: String,
    val processedUri: String,
    val timestamp: Long,
)
