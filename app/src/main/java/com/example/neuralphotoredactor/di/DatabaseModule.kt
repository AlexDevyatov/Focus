package com.example.neuralphotoredactor.di

import android.content.Context
import androidx.room.Room
import com.example.neuralphotoredactor.data.local.dao.ProcessingHistoryDao
import com.example.neuralphotoredactor.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Модуль для предоставления Room Database компонентов.
 * 
 * Создает и предоставляет:
 * - AppDatabase через @Provides
 * - ProcessingHistoryDao через @Provides
 * 
 * Миграции БД:
 * - Версия 1: Начальная схема с таблицей processing_history
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Предоставить экземпляр AppDatabase.
     * 
     * @param context Application Context
     * @return Экземпляр базы данных
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "neural_photo_redactor_db"
        )
            .fallbackToDestructiveMigration() // Для разработки
            .build()
    }
    
    /**
     * Предоставить DAO для работы с историей обработок.
     * 
     * @param database Экземпляр базы данных
     * @return DAO для истории обработок
     */
    @Provides
    @Singleton
    fun provideProcessingHistoryDao(
        database: AppDatabase
    ): ProcessingHistoryDao {
        return database.processingHistoryDao()
    }
}

