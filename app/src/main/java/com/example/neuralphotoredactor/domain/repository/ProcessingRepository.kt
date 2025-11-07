package com.example.neuralphotoredactor.domain.repository

import com.example.neuralphotoredactor.domain.model.AIResult
import com.example.neuralphotoredactor.domain.model.ProcessingRequest
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для обработки изображений AI алгоритмами.
 * 
 * Определяет методы для запуска обработки изображений, получения истории обработок
 * и управления результатами. Реализация находится в data слое и может использовать
 * как локальные TensorFlow Lite модели, так и облачные API.
 * 
 * @see com.example.neuralphotoredactor.data.repository.ProcessingRepositoryImpl
 */
interface ProcessingRepository {
    /**
     * Обрабатывает изображение с использованием указанного AI фильтра.
     * 
     * @param request Запрос на обработку, содержащий изображение и параметры фильтра
     * @return AIResult с результатом обработки (может быть в статусе PROCESSING, COMPLETED или FAILED)
     */
    suspend fun processImage(request: ProcessingRequest): AIResult
    
    /**
     * Получает поток истории всех обработок изображений.
     * 
     * @return Flow со списком всех результатов обработки, отсортированных по времени
     */
    fun getProcessingHistory(): Flow<List<AIResult>>
    
    /**
     * Получает конкретный результат обработки по идентификатору.
     * 
     * @param id Идентификатор результата обработки
     * @return AIResult или null, если результат не найден
     */
    suspend fun getProcessingResult(id: String): AIResult?
    
    /**
     * Удаляет результат обработки из истории.
     * 
     * @param id Идентификатор результата обработки для удаления
     */
    suspend fun deleteProcessingResult(id: String)
}

