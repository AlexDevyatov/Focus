package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.EditingSession
import com.example.neuralphotoredactor.domain.repository.EditingSessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения сессии редактирования по ID.
 */
class GetEditingSessionByIdUseCase @Inject constructor(
    private val editingSessionRepository: EditingSessionRepository
) {
    /**
     * Получить сессию редактирования по ID.
     * 
     * @param id ID сессии
     * @return Flow с сессией или null
     */
    fun invoke(id: Long): Flow<EditingSession?> {
        return editingSessionRepository.getSessionById(id)
    }
}

