package com.example.neuralphotoredactor.domain.repository

import com.example.neuralphotoredactor.domain.model.FilterPreset
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с предустановками фильтров.
 * 
 * Определяет методы для получения списка доступных AI фильтров и их детальной информации.
 * Используется для отображения фильтров в UI и получения информации о конкретном фильтре.
 * 
 * @see com.example.neuralphotoredactor.data.repository.FilterRepositoryImpl
 */
interface FilterRepository {
    /**
     * Получает поток всех доступных фильтров.
     * 
     * @return Flow со списком всех предустановок фильтров
     */
    fun getAllFilters(): Flow<List<FilterPreset>>
    
    /**
     * Получает информацию о конкретном фильтре по его идентификатору.
     * 
     * @param id Идентификатор фильтра
     * @return FilterPreset или null, если фильтр не найден
     */
    suspend fun getFilterById(id: String): FilterPreset?
}

