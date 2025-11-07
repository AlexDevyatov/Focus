package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import javax.inject.Inject

/**
 * Use case для удаления результата обработки из истории.
 * 
 * Инкапсулирует бизнес-логику удаления записи из истории обработок.
 * Используется в ViewModel для удаления результатов обработки по запросу пользователя.
 * 
 * @param processingRepository Репозиторий для обработки изображений
 * 
 * @see com.example.neuralphotoredactor.domain.repository.ProcessingRepository
 */
class DeleteProcessingResultUseCase @Inject constructor(
    private val processingRepository: ProcessingRepository
) {
    /**
     * Удаляет результат обработки из истории.
     * 
     * @param id Идентификатор результата обработки для удаления
     */
    suspend operator fun invoke(id: String) {
        processingRepository.deleteProcessingResult(id)
    }
}

