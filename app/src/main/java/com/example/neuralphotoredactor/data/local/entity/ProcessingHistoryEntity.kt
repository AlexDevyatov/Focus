package com.example.neuralphotoredactor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для хранения истории обработки изображений в Room Database.
 * 
 * Хранит только URI изображений, метаданные и информацию о фильтре.
 * Bitmap не хранится в БД согласно правилам архитектуры.
 * 
 * **Связи с другими сущностями:**
 * - Имеет Foreign Key на ProcessingOperationEntity через operationId
 * - Ссылается на последнюю операцию обработки для получения детальной информации
 * - Используется для отображения истории в UI (HistoryScreen)
 * 
 * @param id Уникальный идентификатор записи
 * @param originalUri URI исходного изображения (строка)
 * @param processedUri URI обработанного изображения (строка)
 * @param filterType Тип примененного фильтра
 * @param timestamp Время обработки в миллисекундах
 * @param operationId Ссылка на операцию обработки (Foreign Key, может быть null для обратной совместимости)
 */
@Entity(
    tableName = "processing_history",
    foreignKeys = [
        ForeignKey(
            entity = ProcessingOperationEntity::class,
            parentColumns = ["id"],
            childColumns = ["operationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["operationId"])
    ]
)
data class ProcessingHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalUri: String,
    val processedUri: String,
    val filterType: String,
    val timestamp: Long,
    val operationId: Long? = null // Ссылка на операцию обработки
)

