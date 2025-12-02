package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.EditingSession
import com.example.neuralphotoredactor.domain.repository.EditingSessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения всех сессий редактирования.
 */
class GetAllEditingSessionsUseCase @Inject constructor(
    private val editingSessionRepository: EditingSessionRepository
) {
    /**
     * Получить все сессии редактирования.
     * 
     * @return Flow со списком всех сессий
     */
    val invoke: Flow<List<EditingSession>>
        get() = editingSessionRepository.getAllSessions()
}

