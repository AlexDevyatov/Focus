package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.CompatibilityLevel
import com.example.neuralphotoredactor.domain.model.ModelType
import com.example.neuralphotoredactor.domain.model.NeuralModel
import com.example.neuralphotoredactor.domain.repository.NeuralModelRepository
import javax.inject.Inject

/**
 * Use case для добавления нейросетевой модели.
 */
class AddNeuralModelUseCase @Inject constructor(
    private val neuralModelRepository: NeuralModelRepository
) {
    /**
     * Добавить нейросетевую модель.
     * 
     * @param name Название модели
     * @param type Тип модели
     * @param version Версия модели
     * @param filePath Путь к файлу модели
     * @param fileSize Размер модели в байтах
     * @param compatibilityLevel Уровень совместимости
     * @return ID созданной модели
     */
    suspend fun invoke(
        name: String,
        type: ModelType,
        version: String,
        filePath: String,
        fileSize: Long,
        compatibilityLevel: CompatibilityLevel = CompatibilityLevel.MEDIUM
    ): Long {
        val model = NeuralModel(
            name = name,
            type = type,
            version = version,
            filePath = filePath,
            fileSize = fileSize,
            isActive = true,
            compatibilityLevel = compatibilityLevel
        )
        return neuralModelRepository.addModel(model)
    }
}

