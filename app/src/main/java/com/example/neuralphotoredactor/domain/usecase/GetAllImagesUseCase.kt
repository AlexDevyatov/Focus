package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.repository.ImageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения всех изображений из галереи.
 */
class GetAllImagesUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    /**
     * Выполнить получение всех изображений.
     * 
     * @return Flow со списком изображений
     */
    operator fun invoke(): Flow<List<ImageData>> {
        return imageRepository.getAllImages()
    }
}

