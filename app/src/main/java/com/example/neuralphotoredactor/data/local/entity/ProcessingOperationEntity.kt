package com.example.neuralphotoredactor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для хранения операций обработки изображений.
 * 
 * Фиксирует каждое отдельное действие, выполненное пользователем
 * в рамках сессии редактирования.
 * 
 * @param id Уникальный идентификатор операции
 * @param sessionId Связь с сессией редактирования (Foreign Key)
 * @param modelId Ссылка на использованную нейросетевую модель (Foreign Key)
 * @param operationType Тип операции
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
            entity = EditingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NeuralModelEntity::class,
            parentColumns = ["id"],
            childColumns = ["modelId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["modelId"])
    ]
)
data class ProcessingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val modelId: Long?,
    val operationType: String,
    val parameters: String, // JSON строка с параметрами
    val inputImageUri: String,
    val outputImageUri: String,
    val processingTimeMs: Long,
    val sequenceNumber: Int
)

