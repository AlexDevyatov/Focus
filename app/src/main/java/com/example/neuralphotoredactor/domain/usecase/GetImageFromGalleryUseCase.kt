package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.repository.ImageRepository
import javax.inject.Inject

/**
 * Use case для получения изображения из галереи устройства.
 * 
 * Инкапсулирует бизнес-логику выбора изображения из галереи.
 * Используется в ViewModel для открытия диалога выбора изображения.
 * 
 * @param imageRepository Репозиторий для работы с изображениями
 * 
 * @see com.example.neuralphotoredactor.domain.repository.ImageRepository
 */
class GetImageFromGalleryUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    /**
     * Получает изображение из галереи устройства.
     * 
     * @return ImageData выбранного изображения или null, если выбор был отменен
     */
    suspend fun getImageFromGallery(): ImageData? {
        return imageRepository.getImageFromGallery()
    }
}

