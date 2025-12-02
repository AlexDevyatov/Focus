package com.example.neuralphotoredactor.domain.repository

import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для обработки изображений.
 * 
 * Предоставляет методы для применения фильтров и эффектов к изображениям
 * с использованием TensorFlow Lite моделей.
 */
interface ProcessingRepository {
    /**
     * Обработать изображение с применением указанного фильтра.
     * 
     * @param imageData Исходное изображение
     * @param filterType Тип фильтра для применения
     * @return Результат обработки или null в случае ошибки
     */
    suspend fun processImage(
        imageData: ImageData,
        filterType: FilterType
    ): ProcessingResult?
    
    /**
     * Получить историю обработок.
     * 
     * @return Flow со списком результатов обработки
     */
    fun getProcessingHistory(): Flow<List<ProcessingResult>>
    
    /**
     * Удалить результат обработки.
     * 
     * @param result Результат для удаления
     */
    suspend fun deleteProcessingResult(result: ProcessingResult)
}

