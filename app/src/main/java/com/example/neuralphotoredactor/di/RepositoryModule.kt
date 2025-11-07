package com.example.neuralphotoredactor.di

import com.example.neuralphotoredactor.data.datasource.CameraDataSource
import com.example.neuralphotoredactor.data.datasource.CameraDataSourceImpl
import com.example.neuralphotoredactor.data.datasource.GalleryDataSource
import com.example.neuralphotoredactor.data.datasource.GalleryDataSourceImpl
import com.example.neuralphotoredactor.data.repository.FilterRepositoryImpl
import com.example.neuralphotoredactor.data.repository.ImageRepositoryImpl
import com.example.neuralphotoredactor.data.repository.ProcessingRepositoryImpl
import com.example.neuralphotoredactor.domain.repository.FilterRepository
import com.example.neuralphotoredactor.domain.repository.ImageRepository
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt модуль для привязки интерфейсов репозиториев и datasources к их реализациям.
 * 
 * Использует @Binds для эффективной привязки абстракций (интерфейсов) к конкретным
 * реализациям. Все зависимости являются Singleton для обеспечения единственного
 * экземпляра в приложении.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    /**
     * Привязывает реализацию ImageRepository к интерфейсу.
     * 
     * @param imageRepositoryImpl Конкретная реализация репозитория
     * @return Интерфейс ImageRepository для внедрения зависимостей
     */
    @Binds
    @Singleton
    abstract fun bindImageRepository(
        imageRepositoryImpl: ImageRepositoryImpl
    ): ImageRepository

    /**
     * Привязывает реализацию ProcessingRepository к интерфейсу.
     * 
     * @param processingRepositoryImpl Конкретная реализация репозитория
     * @return Интерфейс ProcessingRepository для внедрения зависимостей
     */
    @Binds
    @Singleton
    abstract fun bindProcessingRepository(
        processingRepositoryImpl: ProcessingRepositoryImpl
    ): ProcessingRepository

    /**
     * Привязывает реализацию FilterRepository к интерфейсу.
     * 
     * @param filterRepositoryImpl Конкретная реализация репозитория
     * @return Интерфейс FilterRepository для внедрения зависимостей
     */
    @Binds
    @Singleton
    abstract fun bindFilterRepository(
        filterRepositoryImpl: FilterRepositoryImpl
    ): FilterRepository

    /**
     * Привязывает реализацию CameraDataSource к интерфейсу.
     * 
     * @param cameraDataSourceImpl Конкретная реализация источника данных
     * @return Интерфейс CameraDataSource для внедрения зависимостей
     */
    @Binds
    @Singleton
    abstract fun bindCameraDataSource(
        cameraDataSourceImpl: CameraDataSourceImpl
    ): CameraDataSource

    /**
     * Привязывает реализацию GalleryDataSource к интерфейсу.
     * 
     * @param galleryDataSourceImpl Конкретная реализация источника данных
     * @return Интерфейс GalleryDataSource для внедрения зависимостей
     */
    @Binds
    @Singleton
    abstract fun bindGalleryDataSource(
        galleryDataSourceImpl: GalleryDataSourceImpl
    ): GalleryDataSource
}

