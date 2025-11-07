package com.example.neuralphotoredactor.presentation.state

import com.example.neuralphotoredactor.domain.model.FilterPreset

/**
 * Состояние UI для экрана фильтров.
 * 
 * @param filters Список всех доступных фильтров
 * @param selectedFilter Выбранный фильтр (для применения)
 * @param isLoading Флаг загрузки фильтров
 * @param error Сообщение об ошибке
 */
data class FiltersState(
    val filters: List<FilterPreset> = emptyList(),
    val selectedFilter: FilterPreset? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

