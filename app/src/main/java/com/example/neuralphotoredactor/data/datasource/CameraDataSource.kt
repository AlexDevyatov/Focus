package com.example.neuralphotoredactor.data.datasource

import com.example.neuralphotoredactor.domain.model.ImageData

/**
 * Интерфейс источника данных для работы с камерой устройства.
 * 
 * Определяет методы для захвата изображений с камеры. Реализация использует
 * CameraX для работы с камерой Android устройства.
 * 
 * @see com.example.neuralphotoredactor.data.datasource.CameraDataSourceImpl
 */
interface CameraDataSource {
    /**
     * Захватывает изображение с камеры устройства.
     * 
     * @return ImageData захваченного изображения или null, если захват был отменен
     */
    suspend fun captureImage(): ImageData?
}

