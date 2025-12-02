package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.repository.ImageRepository
import javax.inject.Inject

/**
 * Use case для захвата изображения с камеры.
 */
class CaptureImageFromCameraUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    /**
     * Выполнить захват изображения с камеры.
     * 
     * @return ImageData или null
     */
    suspend operator fun invoke(): ImageData? {
        return imageRepository.captureImageFromCamera()
    }
}

