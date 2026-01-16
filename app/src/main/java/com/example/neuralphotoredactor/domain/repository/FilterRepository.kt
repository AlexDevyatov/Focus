package com.example.neuralphotoredactor.domain.repository

/**
 * Интерфейс репозитория для работы с фильтрами.
 *
 * Предоставляет методы для получения информации о фильтрах.
 */
interface FilterRepository {
    /**
     * Получить название фильтра по ID.
     *
     * @param filterId ID фильтра
     * @return Название фильтра или null, если не найден
     */
    suspend fun getFilterNameById(filterId: Long): String?
}
