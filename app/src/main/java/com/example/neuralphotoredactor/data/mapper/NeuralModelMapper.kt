package com.example.neuralphotoredactor.data.mapper

import com.example.neuralphotoredactor.data.local.entity.NeuralModelEntity
import com.example.neuralphotoredactor.domain.model.CompatibilityLevel
import com.example.neuralphotoredactor.domain.model.ModelType
import com.example.neuralphotoredactor.domain.model.NeuralModel

/**
 * Mapper для преобразования между NeuralModelEntity и NeuralModel.
 * 
 * Выполняет преобразование между слоями data и domain,
 * включая преобразование строковых типов в enum.
 */
object NeuralModelMapper {
    
    /**
     * Преобразовать Entity в Domain модель.
     * 
     * @param entity Entity из базы данных
     * @return Domain модель NeuralModel
     */
    fun toDomain(entity: NeuralModelEntity): NeuralModel {
        return NeuralModel(
            id = entity.id,
            name = entity.name,
            type = parseModelType(entity.type),
            version = entity.version,
            filePath = entity.filePath,
            fileSize = entity.fileSize,
            isActive = entity.isActive,
            compatibilityLevel = parseCompatibilityLevel(entity.compatibilityLevel)
        )
    }
    
    /**
     * Преобразовать Domain модель в Entity.
     * 
     * @param model Domain модель NeuralModel
     * @return Entity для базы данных
     */
    fun toEntity(model: NeuralModel): NeuralModelEntity {
        return NeuralModelEntity(
            id = model.id,
            name = model.name,
            type = model.type.name,
            version = model.version,
            filePath = model.filePath,
            fileSize = model.fileSize,
            isActive = model.isActive,
            compatibilityLevel = model.compatibilityLevel.name
        )
    }
    
    /**
     * Преобразовать список Entity в список Domain моделей.
     * 
     * @param entities Список Entity
     * @return Список Domain моделей
     */
    fun toDomainList(entities: List<NeuralModelEntity>): List<NeuralModel> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Парсить строковый тип модели в ModelType enum.
     */
    private fun parseModelType(typeString: String): ModelType {
        return try {
            ModelType.valueOf(typeString)
        } catch (e: Exception) {
            // Пытаемся найти по частичному совпадению
            when (typeString.uppercase()) {
                "STYLE_TRANSFER", "STYLIZATION", "STYLE" -> ModelType.STYLE_TRANSFER
                "SUPER_RESOLUTION", "SUPERRESOLUTION", "UPSCALE" -> ModelType.SUPER_RESOLUTION
                "FILTER" -> ModelType.FILTER
                "ENHANCEMENT", "ENHANCE" -> ModelType.ENHANCEMENT
                else -> ModelType.OTHER
            }
        }
    }
    
    /**
     * Парсить строковый уровень совместимости в CompatibilityLevel enum.
     */
    private fun parseCompatibilityLevel(levelString: String): CompatibilityLevel {
        return try {
            CompatibilityLevel.valueOf(levelString)
        } catch (e: Exception) {
            CompatibilityLevel.MEDIUM // Значение по умолчанию
        }
    }
}

