package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.ProcessingOperation
import com.example.neuralphotoredactor.domain.repository.ProcessingOperationRepository
import javax.inject.Inject

/**
 * Use case для получения операции обработки по ID.
 */
class GetOperationByIdUseCase @Inject constructor(
    private val processingOperationRepository: ProcessingOperationRepository
) {
    /**
     * Получить операцию обработки по ID.
     * 
     * @param operationId ID операции
     * @return Операция или null, если не найдена
     */
    suspend fun invoke(operationId: Long): ProcessingOperation? {
        return processingOperationRepository.getOperationById(operationId)
    }
}

