package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.repository.ImageRepository
import javax.inject.Inject

/**
 * Use case для получения изображения из галереи.
 */
class GetImageFromGalleryUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    /**
ре     * Получить изображение из галереи.
     * 
     * @return ImageData или null
     */
    suspend fun invoke(): ImageData? {
        return imageRepository.getImageFromGallery()
    }
}

