package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import javax.inject.Inject

/**
 * Use case для обработки изображения с применением фильтра.
 */
class ProcessImageUseCase @Inject constructor(
    private val processingRepository: ProcessingRepository
) {
    /**
     * Обработать изображение.
     * 
     * @param imageData Исходное изображение
     * @param filterType Тип фильтра
     * @return Результат обработки или null
     */
    suspend fun invoke(
        imageData: ImageData,
        filterType: FilterType
    ): ProcessingResult? {
        return processingRepository.processImage(imageData, filterType)
    }
}

