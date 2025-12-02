package com.example.neuralphotoredactor.data.mapper

import android.net.Uri
import com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity
import com.example.neuralphotoredactor.domain.model.ProcessingResult

/**
 * Mapper для преобразования между Entity и Domain моделями.
 * 
 * Выполняет преобразование между слоями data и domain,
 * обеспечивая изоляцию слоев.
 */
object ProcessingHistoryMapper {
    
    /**
     * Преобразовать Entity в Domain модель.
     * 
     * @param entity Entity из базы данных
     * @return Domain модель ProcessingResult
     */
    fun toDomain(entity: ProcessingHistoryEntity): ProcessingResult {
        return ProcessingResult(
            originalUri = Uri.parse(entity.originalUri),
            processedUri = Uri.parse(entity.processedUri),
            filterType = entity.filterType,
            timestamp = entity.timestamp
        )
    }
    
    /**
     * Преобразовать Domain модель в Entity.
     * 
     * @param result Domain модель ProcessingResult
     * @return Entity для базы данных
     */
    fun toEntity(result: ProcessingResult): ProcessingHistoryEntity {
        return ProcessingHistoryEntity(
            originalUri = result.originalUri.toString(),
            processedUri = result.processedUri.toString(),
            filterType = result.filterType,
            timestamp = result.timestamp
        )
    }
    
    /**
     * Преобразовать список Entity в список Domain моделей.
     * 
     * @param entities Список Entity
     * @return Список Domain моделей
     */
    fun toDomainList(entities: List<ProcessingHistoryEntity>): List<ProcessingResult> {
        return entities.map { toDomain(it) }
    }
}

