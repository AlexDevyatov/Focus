package com.example.neuralphotoredactor.data.datasource

import com.example.neuralphotoredactor.domain.model.ImageData

/**
 * Интерфейс источника данных для работы с галереей.
 */
interface GalleryDataSource {
    /**
     * Выбрать изображение из галереи.
     * 
     * @return ImageData или null
     */
    suspend fun pickImage(): ImageData?
    
    /**
     * Получить все изображения из галереи.
     * 
     * @return Список всех изображений
     */
    suspend fun getAllImages(): List<ImageData>
}

