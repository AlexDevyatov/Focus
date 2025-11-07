package com.example.neuralphotoredactor.data.repository

import com.example.neuralphotoredactor.data.datasource.CameraDataSource
import com.example.neuralphotoredactor.data.datasource.GalleryDataSource
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.repository.ImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Реализация репозитория для работы с изображениями.
 * 
 * Координирует работу с различными источниками данных (камера, галерея)
 * для получения изображений. Внедряется через Hilt и предоставляет
 * единый интерфейс для domain слоя.
 * 
 * @param galleryDataSource Источник данных для работы с галереей
 * @param cameraDataSource Источник данных для работы с камерой
 * 
 * @see com.example.neuralphotoredactor.domain.repository.ImageRepository
 */
class ImageRepositoryImpl @Inject constructor(
    private val galleryDataSource: GalleryDataSource,
    private val cameraDataSource: CameraDataSource
) : ImageRepository {
    override suspend fun getImageFromGallery(): ImageData? {
        return galleryDataSource.pickImage()
    }

    override suspend fun captureImageFromCamera(): ImageData? {
        return cameraDataSource.captureImage()
    }

    override fun getAllImages(): Flow<List<ImageData>> {
        return flow {
            try {
                val images = galleryDataSource.getAllImages()
                // Эмитим данные сразу после получения
                emit(images)
            } catch (e: Exception) {
                // Эмитим пустой список при ошибке
                emit(emptyList())
            }
        }
    }
}

