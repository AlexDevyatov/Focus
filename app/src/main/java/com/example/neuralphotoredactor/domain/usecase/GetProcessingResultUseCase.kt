package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.AIResult
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import javax.inject.Inject

/**
 * Use case для получения конкретного результата обработки по идентификатору.
 * 
 * Инкапсулирует бизнес-логику получения результата обработки из истории.
 * Используется в ViewModel для отображения деталей конкретной обработки.
 * 
 * @param processingRepository Репозиторий для обработки изображений
 * 
 * @see com.example.neuralphotoredactor.domain.repository.ProcessingRepository
 */
class GetProcessingResultUseCase @Inject constructor(
    private val processingRepository: ProcessingRepository
) {
    /**
     * Получает результат обработки по идентификатору.
     * 
     * @param id Идентификатор результата обработки
     * @return AIResult или null, если результат не найден
     */
    suspend fun getProcessingResult(id: String): AIResult? {
        return processingRepository.getProcessingResult(id)
    }
}

