package com.example.neuralphotoredactor.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
 * - Версия 2: Добавлены таблицы processing_operations, neural_models
 * - Версия 1: Начальная схема с таблицей processing_history
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Миграция с версии 1 на версию 2.
     * Создает новые таблицы для операций обработки и моделей.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Создание таблицы neural_models
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS neural_models (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    version TEXT NOT NULL,
                    filePath TEXT NOT NULL,
                    fileSize INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    compatibilityLevel TEXT NOT NULL
                )
            """.trimIndent())
            
            // Создание таблицы processing_operations
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS processing_operations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sessionId INTEGER NOT NULL,
                    modelId INTEGER,
                    operationType TEXT NOT NULL,
                    parameters TEXT NOT NULL,
                    inputImageUri TEXT NOT NULL,
                    outputImageUri TEXT NOT NULL,
                    processingTimeMs INTEGER NOT NULL,
                    sequenceNumber INTEGER NOT NULL,
                    FOREIGN KEY(modelId) REFERENCES neural_models(id) ON DELETE SET NULL
                )
            """.trimIndent())
            
            // Создание индексов
            database.execSQL("CREATE INDEX IF NOT EXISTS index_processing_operations_sessionId ON processing_operations(sessionId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_processing_operations_modelId ON processing_operations(modelId)")
        }
    }
    
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
            .addMigrations(MIGRATION_1_2)
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

