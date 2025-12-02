package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.ProcessingOperation
import com.example.neuralphotoredactor.domain.repository.ProcessingOperationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения операций обработки по ID сессии.
 */
class GetOperationsBySessionIdUseCase @Inject constructor(
    private val processingOperationRepository: ProcessingOperationRepository
) {
    /**
     * Получить все операции для сессии.
     * 
     * @param sessionId ID сессии
     * @return Flow со списком операций
     */
    fun invoke(sessionId: Long): Flow<List<ProcessingOperation>> {
        return processingOperationRepository.getOperationsBySessionId(sessionId)
    }
}

