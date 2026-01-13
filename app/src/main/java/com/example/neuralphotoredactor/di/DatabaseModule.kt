package com.example.neuralphotoredactor.di

import android.content.Context
import androidx.room.Room
import com.example.neuralphotoredactor.data.local.dao.FilterDao
import com.example.neuralphotoredactor.data.local.dao.NeuralModelDao
import com.example.neuralphotoredactor.data.local.dao.ProcessingHistoryDao
import com.example.neuralphotoredactor.data.local.dao.ProcessingOperationDao
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
 * - ProcessingOperationDao через @Provides
 * - NeuralModelDao через @Provides
 * 
 * Миграции БД:
 * - Миграции не используются, текущая схема является первоначальной
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
        // Удаляем старую базу данных ai_image_editor_db, если она существует
        val oldDbFile = context.getDatabasePath("ai_image_editor_db")
        if (oldDbFile.exists()) {
            try {
                oldDbFile.delete()
                android.util.Log.d("DatabaseModule", "Удалена старая база данных: ai_image_editor_db")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseModule", "Ошибка удаления старой БД: ${e.message}", e)
            }
        }
        
        // Удаляем также связанные файлы старой БД (WAL, SHM)
        val oldDbWal = context.getDatabasePath("ai_image_editor_db-wal")
        val oldDbShm = context.getDatabasePath("ai_image_editor_db-shm")
        oldDbWal.takeIf { it.exists() }?.delete()
        oldDbShm.takeIf { it.exists() }?.delete()
        
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
    
    /**
     * Предоставить DAO для работы с операциями обработки.
     * 
     * @param database Экземпляр базы данных
     * @return DAO для операций обработки
     */
    @Provides
    @Singleton
    fun provideProcessingOperationDao(
        database: AppDatabase
    ): ProcessingOperationDao {
        return database.processingOperationDao()
    }
    
    /**
     * Предоставить DAO для работы с фильтрами.
     * 
     * @param database Экземпляр базы данных
     * @return DAO для фильтров
     */
    @Provides
    @Singleton
    fun provideFilterDao(
        database: AppDatabase
    ): FilterDao {
        return database.filterDao()
    }
    
    /**
     * Предоставить DAO для работы с нейросетевыми моделями.
     * 
     * @param database Экземпляр базы данных
     * @return DAO для нейросетевых моделей
     */
    @Provides
    @Singleton
    fun provideNeuralModelDao(
        database: AppDatabase
    ): NeuralModelDao {
        return database.neuralModelDao()
    }
}

