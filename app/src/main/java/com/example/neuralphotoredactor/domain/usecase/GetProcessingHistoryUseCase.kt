package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.AIResult
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения истории обработок изображений.
 * 
 * Инкапсулирует бизнес-логику получения списка всех выполненных обработок.
 * Используется в ViewModel для отображения истории в UI.
 * 
 * @param processingRepository Репозиторий для обработки изображений
 * 
 * @see com.example.neuralphotoredactor.domain.repository.ProcessingRepository
 */
class GetProcessingHistoryUseCase @Inject constructor(
    private val processingRepository: ProcessingRepository
) {
    /**
     * Получает поток истории всех обработок.
     * 
     * @return Flow со списком всех результатов обработки, обновляющийся при изменении истории
     */
    fun getProcessingHistory(): Flow<List<AIResult>> {
        return processingRepository.getProcessingHistory()
    }
}

