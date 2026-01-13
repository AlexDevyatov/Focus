package com.example.neuralphotoredactor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения истории обработки изображений в Room Database.
 * 
 * Хранит только URI изображений, метаданные и информацию о фильтре.
 * Bitmap не хранится в БД согласно правилам архитектуры.
 * 
 * **Связи с другими сущностями:**
 * - Не имеет Foreign Keys и не связана напрямую с другими таблицами
 * - Независимая таблица для быстрого доступа к истории обработок
 * - Используется для отображения истории в UI (HistoryScreen)
 * 
 * @param id Уникальный идентификатор записи
 * @param originalUri URI исходного изображения (строка)
 * @param processedUri URI обработанного изображения (строка)
 * @param filterType Тип примененного фильтра
 * @param timestamp Время обработки в миллисекундах
 */
@Entity(tableName = "processing_history")
data class ProcessingHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalUri: String,
    val processedUri: String,
    val filterType: String,
    val timestamp: Long
)

