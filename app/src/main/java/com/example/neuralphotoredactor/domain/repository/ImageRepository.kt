package com.example.neuralphotoredactor.domain.repository

import com.example.neuralphotoredactor.domain.model.ImageData
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с изображениями.
 * 
 * Предоставляет методы для получения изображений из галереи и камеры.
 */
interface ImageRepository {
    /**
     * Получить изображение из галереи.
     * 
     * @return ImageData или null, если изображение не выбрано
     */
    suspend fun getImageFromGallery(): ImageData?
    
    /**
     * Захватить изображение с камеры.
     * 
     * @return ImageData или null, если захват не удался
     */
    suspend fun captureImageFromCamera(): ImageData?
    
    /**
     * Получить все изображения из галереи.
     * 
     * @return Flow со списком всех изображений
     */
    fun getAllImages(): Flow<List<ImageData>>
    
    /**
     * Инвалидировать кэш изображений.
     * Используется для принудительного обновления списка изображений.
     */
    suspend fun invalidateCache()
}

