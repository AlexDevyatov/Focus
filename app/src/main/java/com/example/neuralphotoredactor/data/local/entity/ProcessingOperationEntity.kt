package com.example.neuralphotoredactor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для хранения операций обработки изображений.
 * 
 * Фиксирует каждое отдельное действие, выполненное пользователем.
 * 
 * @param id Уникальный идентификатор операции
 * @param historyId Идентификатор записи в истории обработки (Foreign Key)
 * @param sessionId Идентификатор сессии (без Foreign Key)
 * @param filterId Ссылка на использованный фильтр или операцию редактирования (Foreign Key, NOT NULL)
 * @param parameters Параметры выполнения в формате JSON
 * @param inputImageUri URI входного изображения
 * @param outputImageUri URI выходного изображения
 * @param processingTimeMs Время обработки в миллисекундах
 * @param sequenceNumber Порядковый номер операции в истории изменений
 */
@Entity(
    tableName = "processing_operations",
    foreignKeys = [
        ForeignKey(
            entity = ProcessingHistoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["historyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FilterEntity::class,
            parentColumns = ["id"],
            childColumns = ["filterId"],
            onDelete = ForeignKey.RESTRICT // Нельзя удалить фильтр, если он используется в операциях
        )
    ],
    indices = [
        Index(value = ["historyId"]),
        Index(value = ["sessionId"]),
        Index(value = ["filterId"])
    ]
)
data class ProcessingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val historyId: Long, // Ссылка на запись в processing_history
    val sessionId: Long,
    val filterId: Long, // Ссылка на фильтр или операцию редактирования (NOT NULL)
    val parameters: String, // JSON строка с параметрами
    val inputImageUri: String,
    val outputImageUri: String,
    val processingTimeMs: Long,
    val sequenceNumber: Int
)

