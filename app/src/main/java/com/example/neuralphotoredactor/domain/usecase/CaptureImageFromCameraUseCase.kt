package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.repository.ImageRepository
import javax.inject.Inject

/**
 * Use case для захвата изображения с камеры устройства.
 * 
 * Инкапсулирует бизнес-логику захвата изображения с камеры.
 * Используется в ViewModel для открытия камеры и захвата фото.
 * 
 * @param imageRepository Репозиторий для работы с изображениями
 * 
 * @see com.example.neuralphotoredactor.domain.repository.ImageRepository
 */
class CaptureImageFromCameraUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    /**
     * Захватывает изображение с камеры устройства.
     * 
     * @return ImageData захваченного изображения или null, если захват был отменен
     */
    suspend operator fun invoke(): ImageData? {
        return imageRepository.captureImageFromCamera()
    }
}

