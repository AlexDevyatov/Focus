package com.example.neuralphotoredactor.data.datasource

import com.example.neuralphotoredactor.domain.model.ImageData

/**
 * Интерфейс источника данных для работы с камерой.
 */
interface CameraDataSource {
    /**
     * Захватить изображение с камеры.
     * 
     * @return ImageData или null
     */
    suspend fun captureImage(): ImageData?
}

