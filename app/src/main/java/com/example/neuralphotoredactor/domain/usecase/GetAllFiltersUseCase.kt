package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.FilterPreset
import com.example.neuralphotoredactor.domain.repository.FilterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения всех доступных фильтров.
 * 
 * Инкапсулирует бизнес-логику получения списка всех предустановок фильтров.
 * Используется в ViewModel для отображения фильтров в UI.
 * 
 * @param filterRepository Репозиторий для работы с фильтрами
 * 
 * @see com.example.neuralphotoredactor.domain.repository.FilterRepository
 */
class GetAllFiltersUseCase @Inject constructor(
    private val filterRepository: FilterRepository
) {
    /**
     * Получает поток всех доступных фильтров.
     * 
     * @return Flow со списком всех предустановок фильтров
     */
    fun getAllFilters(): Flow<List<FilterPreset>> {
        return filterRepository.getAllFilters()
    }
}

