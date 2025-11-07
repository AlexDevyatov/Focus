package com.example.neuralphotoredactor.data.datasource

import com.example.neuralphotoredactor.domain.model.ImageData

/**
 * Интерфейс источника данных для работы с галереей устройства.
 * 
 * Определяет методы для выбора изображений из галереи и получения списка
 * всех доступных изображений. Реализация использует MediaStore API.
 * 
 * @see com.example.neuralphotoredactor.data.datasource.GalleryDataSourceImpl
 */
interface GalleryDataSource {
    /**
     * Открывает диалог выбора изображения из галереи.
     * 
     * @return ImageData выбранного изображения или null, если выбор был отменен
     */
    suspend fun pickImage(): ImageData?
    
    /**
     * Получает список всех изображений из галереи устройства.
     * 
     * @return Список всех доступных изображений из галереи
     */
    suspend fun getAllImages(): List<ImageData>
}

