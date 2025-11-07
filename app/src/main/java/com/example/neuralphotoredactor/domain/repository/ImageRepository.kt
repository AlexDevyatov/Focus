package com.example.neuralphotoredactor.domain.repository

import com.example.neuralphotoredactor.domain.model.ImageData
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с изображениями.
 * 
 * Определяет методы для получения изображений из различных источников:
 * галереи устройства, камеры, а также для получения списка всех доступных изображений.
 * Реализация находится в data слое.
 * 
 * @see com.example.neuralphotoredactor.data.repository.ImageRepositoryImpl
 */
interface ImageRepository {
    /**
     * Получает изображение из галереи устройства.
     * 
     * @return ImageData выбранного изображения или null, если выбор был отменен
     */
    suspend fun getImageFromGallery(): ImageData?
    
    /**
     * Захватывает изображение с камеры устройства.
     * 
     * @return ImageData захваченного изображения или null, если захват был отменен
     */
    suspend fun captureImageFromCamera(): ImageData?
    
    /**
     * Получает поток всех доступных изображений из галереи.
     * 
     * @return Flow со списком всех изображений, обновляющийся при изменении галереи
     */
    fun getAllImages(): Flow<List<ImageData>>
}

