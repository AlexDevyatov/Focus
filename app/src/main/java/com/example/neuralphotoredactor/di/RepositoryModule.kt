package com.example.neuralphotoredactor.di

import com.example.neuralphotoredactor.data.datasource.GalleryDataSource
import com.example.neuralphotoredactor.data.datasource.GalleryDataSourceImpl
import com.example.neuralphotoredactor.data.repository.FilterRepositoryImpl
import com.example.neuralphotoredactor.data.repository.ImageRepositoryImpl
import com.example.neuralphotoredactor.data.repository.NeuralModelRepositoryImpl
import com.example.neuralphotoredactor.data.repository.ProcessingOperationRepositoryImpl
import com.example.neuralphotoredactor.data.repository.ProcessingRepositoryImpl
import com.example.neuralphotoredactor.data.repository.TFLiteModelRepositoryImpl
import com.example.neuralphotoredactor.domain.repository.FilterRepository
import com.example.neuralphotoredactor.domain.repository.ImageRepository
import com.example.neuralphotoredactor.domain.repository.NeuralModelRepository
import com.example.neuralphotoredactor.domain.repository.ProcessingOperationRepository
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import com.example.neuralphotoredactor.domain.repository.TFLiteModelRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Модуль для предоставления репозиториев и источников данных.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindImageRepository(imageRepositoryImpl: ImageRepositoryImpl): ImageRepository

    @Binds
    @Singleton
    abstract fun bindProcessingRepository(
        processingRepositoryImpl: ProcessingRepositoryImpl,
    ): ProcessingRepository

    @Binds
    @Singleton
    abstract fun bindProcessingOperationRepository(
        processingOperationRepositoryImpl: ProcessingOperationRepositoryImpl,
    ): ProcessingOperationRepository

    @Binds
    @Singleton
    abstract fun bindNeuralModelRepository(
        neuralModelRepositoryImpl: NeuralModelRepositoryImpl,
    ): NeuralModelRepository

    @Binds
    @Singleton
    abstract fun bindTFLiteModelRepository(
        tfliteModelRepositoryImpl: TFLiteModelRepositoryImpl,
    ): TFLiteModelRepository

    @Binds
    @Singleton
    abstract fun bindFilterRepository(filterRepositoryImpl: FilterRepositoryImpl): FilterRepository

    @Binds
    abstract fun bindGalleryDataSource(
        galleryDataSourceImpl: GalleryDataSourceImpl,
    ): GalleryDataSource
}
