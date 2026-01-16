package com.example.neuralphotoredactor.data.repository

import com.example.neuralphotoredactor.data.datasource.GalleryDataSource
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.repository.ImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Реализация репозитория для работы с изображениями.
 */
class ImageRepositoryImpl
    @Inject
    constructor(
        private val galleryDataSource: GalleryDataSource,
    ) : ImageRepository {
        override fun getAllImages(): Flow<List<ImageData>> {
            return flow {
                val images = galleryDataSource.getAllImages()
                emit(images)
            }
        }

        override suspend fun invalidateCache() {
            galleryDataSource.invalidateCache()
        }
    }
