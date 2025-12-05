package com.example.neuralphotoredactor.data.datasource

import com.example.neuralphotoredactor.domain.model.ImageData
import kotlinx.coroutines.flow.Flow

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
    
    /**
     * Получить изображения с пагинацией.
     * 
     * @param limit Максимальное количество изображений за один запрос
     * @param offset Смещение для пагинации
     * @return Список изображений
     */
    suspend fun getImagesPaginated(limit: Int = 50, offset: Int = 0): List<ImageData>
    
    /**
     * Получить общее количество изображений в галерее.
     * 
     * @return Количество изображений
     */
    suspend fun getImageCount(): Int
    
    /**
     * Инвалидировать кэш изображений.
     * Используется для принудительного обновления списка изображений.
     */
    suspend fun invalidateCache()
}

