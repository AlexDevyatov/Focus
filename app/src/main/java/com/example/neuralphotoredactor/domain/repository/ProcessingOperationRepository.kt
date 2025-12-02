package com.example.neuralphotoredactor.domain.repository

import com.example.neuralphotoredactor.domain.model.ProcessingOperation
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с операциями обработки.
 * 
 * Предоставляет методы для управления операциями обработки изображений.
 */
interface ProcessingOperationRepository {
    /**
     * Получить все операции для сессии.
     * 
     * @param sessionId ID сессии
     * @return Flow со списком операций
     */
    fun getOperationsBySessionId(sessionId: Long): Flow<List<ProcessingOperation>>
    
    /**
     * Получить операцию по ID.
     * 
     * @param id ID операции
     * @return Операция или null
     */
    suspend fun getOperationById(id: Long): ProcessingOperation?
    
    /**
     * Получить последнюю операцию для сессии.
     * 
     * @param sessionId ID сессии
     * @return Последняя операция или null
     */
    suspend fun getLastOperationBySessionId(sessionId: Long): ProcessingOperation?
    
    /**
     * Добавить операцию обработки.
     * 
     * @param operation Операция для добавления
     * @return ID созданной операции
     */
    suspend fun addOperation(operation: ProcessingOperation): Long
    
    /**
     * Удалить операцию.
     * 
     * @param operation Операция для удаления
     */
    suspend fun deleteOperation(operation: ProcessingOperation)
    
    /**
     * Удалить все операции для сессии.
     * 
     * @param sessionId ID сессии
     */
    suspend fun deleteOperationsBySessionId(sessionId: Long)
}

