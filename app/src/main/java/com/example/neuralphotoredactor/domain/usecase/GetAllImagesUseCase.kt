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
     * Получить все изображения.
     * 
     * @return Flow со списком изображений
     */
    val invoke: Flow<List<ImageData>>
        get() = imageRepository.getAllImages()
}

