package com.example.neuralphotoredactor.domain.usecase

import android.graphics.Bitmap
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import javax.inject.Inject

/**
 * Use case для предпросмотра множественных фильтров.
 */
class PreviewFiltersUseCase
    @Inject
    constructor(
        private val processingRepository: ProcessingRepository,
    ) {
        /**
         * Предпросмотр фильтров без сохранения в файл.
         *
         * @param bitmap Исходное изображение
         * @param filters Список фильтров с их интенсивностями
         * @return Обработанное изображение или null
         */
        suspend fun invoke(
            bitmap: Bitmap,
            filters: List<Pair<FilterType, Float?>>,
        ): Bitmap? {
            return processingRepository.previewFilters(bitmap, filters)
        }
    }
