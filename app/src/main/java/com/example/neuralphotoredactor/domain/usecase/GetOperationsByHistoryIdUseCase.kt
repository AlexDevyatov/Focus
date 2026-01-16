package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.ProcessingOperation
import com.example.neuralphotoredactor.domain.repository.ProcessingOperationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения операций обработки по ID записи истории.
 */
class GetOperationsByHistoryIdUseCase
    @Inject
    constructor(
        private val processingOperationRepository: ProcessingOperationRepository,
    ) {
        /**
         * Получить все операции для записи истории обработки.
         *
         * @param historyId ID записи в истории обработки
         * @return Flow со списком операций обработки
         */
        fun invoke(historyId: Long): Flow<List<ProcessingOperation>> {
            return processingOperationRepository.getOperationsByHistoryId(historyId)
        }
    }
