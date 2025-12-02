package com.example.neuralphotoredactor.domain.repository

import com.example.neuralphotoredactor.domain.model.EditingSession
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с сессиями редактирования.
 * 
 * Предоставляет методы для управления сессиями редактирования изображений.
 */
interface EditingSessionRepository {
    /**
     * Получить все сессии редактирования.
     * 
     * @return Flow со списком всех сессий
     */
    fun getAllSessions(): Flow<List<EditingSession>>
    
    /**
     * Получить сессию по ID.
     * 
     * @param id ID сессии
     * @return Flow с сессией или null
     */
    fun getSessionById(id: Long): Flow<EditingSession?>
    
    /**
     * Создать новую сессию редактирования.
     * 
     * @param session Сессия для создания
     * @return ID созданной сессии
     */
    suspend fun createSession(session: EditingSession): Long
    
    /**
     * Обновить сессию редактирования.
     * 
     * @param session Сессия для обновления
     */
    suspend fun updateSession(session: EditingSession)
    
    /**
     * Удалить сессию редактирования.
     * 
     * @param session Сессия для удаления
     */
    suspend fun deleteSession(session: EditingSession)
    
    /**
     * Удалить сессию по ID.
     * 
     * @param id ID сессии для удаления
     */
    suspend fun deleteSessionById(id: Long)
}

