package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.repository.FilterRepository
import javax.inject.Inject

/**
 * Use case для получения названия фильтра по ID.
 */
class GetFilterNameByIdUseCase
    @Inject
    constructor(
        private val filterRepository: FilterRepository,
    ) {
        /**
         * Получить название фильтра по ID.
         *
         * @param filterId ID фильтра
         * @return Название фильтра или null, если не найден
         */
        suspend fun invoke(filterId: Long): String? {
            return filterRepository.getFilterNameById(filterId)
        }
    }
