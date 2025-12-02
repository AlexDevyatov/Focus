package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.NeuralModel
import com.example.neuralphotoredactor.domain.repository.NeuralModelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения всех активных нейросетевых моделей.
 */
class GetAllNeuralModelsUseCase @Inject constructor(
    private val neuralModelRepository: NeuralModelRepository
) {
    /**
     * Получить все активные модели.
     * 
     * @return Flow со списком активных моделей
     */
    val invoke: Flow<List<NeuralModel>>
        get() = neuralModelRepository.getAllActiveModels()
}

