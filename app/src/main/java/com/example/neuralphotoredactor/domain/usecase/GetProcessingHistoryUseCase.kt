package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения истории обработок.
 */
class GetProcessingHistoryUseCase @Inject constructor(
    private val processingRepository: ProcessingRepository
) {
    /**
     * Выполнить получение истории обработок.
     * 
     * @return Flow со списком результатов обработки
     */
    operator fun invoke(): Flow<List<ProcessingResult>> {
        return processingRepository.getProcessingHistory()
    }
}

