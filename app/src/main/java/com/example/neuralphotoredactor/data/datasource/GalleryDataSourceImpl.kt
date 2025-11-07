package com.example.neuralphotoredactor.data.datasource

import com.example.neuralphotoredactor.domain.model.ImageData
import javax.inject.Inject

/**
 * Реализация источника данных для работы с галереей устройства.
 * 
 * Использует MediaStore API для доступа к изображениям в галерее устройства.
 * Внедряется через Hilt и используется в ImageRepository для получения изображений.
 * 
 * @see com.example.neuralphotoredactor.data.datasource.GalleryDataSource
 */
class GalleryDataSourceImpl @Inject constructor() : GalleryDataSource {
    /**
     * Открывает диалог выбора изображения из галереи через системный Intent.
     * 
     * @return ImageData выбранного изображения или null, если выбор был отменен
     */
    override suspend fun pickImage(): ImageData? {
        // TODO: Implement image picking from gallery
        return null
    }

    /**
     * Получает список всех изображений из галереи через MediaStore.
     * 
     * @return Список всех доступных изображений из галереи
     */
    override suspend fun getAllImages(): List<ImageData> {
        // TODO: Implement getting all images from gallery
        return emptyList()
    }
}

