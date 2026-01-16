package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import javax.inject.Inject

/**
 * Use case для обработки изображения с применением нескольких фильтров.
 */
class ProcessImageWithFiltersUseCase
    @Inject
    constructor(
        private val processingRepository: ProcessingRepository,
    ) {
        /**
         * Обработать изображение с применением нескольких фильтров.
         *
         * @param imageData Исходное изображение
         * @param filters Список фильтров с их интенсивностями
         * @return Результат обработки или null
         */
        suspend fun invoke(
            imageData: ImageData,
            filters: List<Pair<FilterType, Float?>>,
        ): ProcessingResult? {
            return processingRepository.processImageWithFilters(imageData, filters)
        }
    }
