package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.FilterPreset
import com.example.neuralphotoredactor.domain.repository.FilterRepository
import javax.inject.Inject

/**
 * Use case для получения информации о конкретном фильтре по идентификатору.
 * 
 * Инкапсулирует бизнес-логику получения детальной информации о фильтре.
 * Используется в ViewModel для отображения деталей фильтра или применения фильтра.
 * 
 * @param filterRepository Репозиторий для работы с фильтрами
 * 
 * @see com.example.neuralphotoredactor.domain.repository.FilterRepository
 */
class GetFilterByIdUseCase @Inject constructor(
    private val filterRepository: FilterRepository
) {
    /**
     * Получает информацию о фильтре по его идентификатору.
     * 
     * @param id Идентификатор фильтра
     * @return FilterPreset или null, если фильтр не найден
     */
    suspend operator fun invoke(id: String): FilterPreset? {
        return filterRepository.getFilterById(id)
    }
}

