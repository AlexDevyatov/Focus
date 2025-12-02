package com.example.neuralphotoredactor.domain.repository

import com.example.neuralphotoredactor.domain.model.ModelType
import com.example.neuralphotoredactor.domain.model.NeuralModel
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с нейросетевыми моделями.
 * 
 * Предоставляет методы для управления нейросетевыми моделями.
 */
interface NeuralModelRepository {
    /**
     * Получить все активные модели.
     * 
     * @return Flow со списком активных моделей
     */
    fun getAllActiveModels(): Flow<List<NeuralModel>>
    
    /**
     * Получить все модели (включая неактивные).
     * 
     * @return Flow со списком всех моделей
     */
    fun getAllModels(): Flow<List<NeuralModel>>
    
    /**
     * Получить модели по типу.
     * 
     * @param type Тип модели
     * @return Flow со списком моделей указанного типа
     */
    fun getModelsByType(type: ModelType): Flow<List<NeuralModel>>
    
    /**
     * Получить модель по ID.
     * 
     * @param id ID модели
     * @return Модель или null
     */
    suspend fun getModelById(id: Long): NeuralModel?
    
    /**
     * Получить модель по имени.
     * 
     * @param name Название модели
     * @return Модель или null
     */
    suspend fun getModelByName(name: String): NeuralModel?
    
    /**
     * Добавить модель.
     * 
     * @param model Модель для добавления
     * @return ID созданной модели
     */
    suspend fun addModel(model: NeuralModel): Long
    
    /**
     * Обновить модель.
     * 
     * @param model Модель для обновления
     */
    suspend fun updateModel(model: NeuralModel)
    
    /**
     * Удалить модель.
     * 
     * @param model Модель для удаления
     */
    suspend fun deleteModel(model: NeuralModel)
    
    /**
     * Активировать/деактивировать модель.
     * 
     * @param id ID модели
     * @param isActive Новое состояние активности
     */
    suspend fun setModelActive(id: Long, isActive: Boolean)
}

