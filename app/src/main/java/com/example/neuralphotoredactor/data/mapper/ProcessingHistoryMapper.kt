package com.example.neuralphotoredactor.data.mapper

import android.net.Uri
import com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.enums.ProcessingStatus
import com.example.neuralphotoredactor.domain.model.AIResult
import com.example.neuralphotoredactor.domain.model.ImageData

/**
 * Маппер для преобразования между domain моделями и Room entities.
 * 
 * Обеспечивает преобразование между AIResult (domain) и ProcessingHistoryEntity (data).
 */
object ProcessingHistoryMapper {
    /**
     * Преобразует ProcessingHistoryEntity в AIResult.
     * 
     * @param entity Entity из базы данных
     * @return AIResult для domain слоя
     */
    fun toDomain(entity: ProcessingHistoryEntity): AIResult {
        return AIResult(
            id = entity.id,
            originalImage = ImageData(
                uri = Uri.parse(entity.originalImageUri),
                path = null
            ),
            processedImage = entity.processedImageUri?.let {
                ImageData(
                    uri = Uri.parse(it),
                    path = null
                )
            },
            status = ProcessingStatus.valueOf(entity.status),
            error = entity.error,
            processingTime = entity.processingTime
        )
    }

    /**
     * Преобразует AIResult в ProcessingHistoryEntity.
     * 
     * @param result AIResult из domain слоя
     * @return ProcessingHistoryEntity для сохранения в базу данных
     */
    fun toEntity(result: AIResult): ProcessingHistoryEntity {
        return ProcessingHistoryEntity(
            id = result.id,
            originalImageUri = result.originalImage.uri.toString(),
            processedImageUri = result.processedImage?.uri?.toString(),
            filterType = "", // Будет заполнено при сохранении
            status = result.status.name,
            error = result.error,
            processingTime = result.processingTime,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Преобразует список entities в список domain моделей.
     * 
     * @param entities Список entities из базы данных
     * @return Список AIResult для domain слоя
     */
    fun toDomainList(entities: List<ProcessingHistoryEntity>): List<AIResult> {
        return entities.map { toDomain(it) }
    }
}

