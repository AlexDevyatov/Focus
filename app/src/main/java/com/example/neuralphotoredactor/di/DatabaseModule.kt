package com.example.neuralphotoredactor.di

import android.content.Context
import androidx.room.Room
import com.example.neuralphotoredactor.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt модуль для предоставления зависимостей, связанных с базой данных Room.
 * 
 * Создает и предоставляет экземпляр AppDatabase и DAO интерфейсы для использования
 * в репозиториях. Все зависимости являются Singleton, так как база данных
 * должна быть единственной в приложении.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /**
     * Создает и предоставляет экземпляр Room базы данных.
     * 
     * @param context Application контекст для создания базы данных
     * @return Экземпляр AppDatabase
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ai_image_editor_db"
        ).fallbackToDestructiveMigration()
        .build()
    }
    
    /**
     * Предоставляет DAO для работы с историей обработок.
     * 
     * @param database Экземпляр базы данных
     * @return ProcessingHistoryDao для выполнения операций с историей
     */
    @Provides
    fun provideProcessingHistoryDao(database: AppDatabase) = database.processingHistoryDao()
}

