package com.example.neuralphotoredactor.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.neuralphotoredactor.data.local.dao.NeuralModelDao
import com.example.neuralphotoredactor.data.local.dao.ProcessingHistoryDao
import com.example.neuralphotoredactor.data.local.dao.ProcessingOperationDao
import com.example.neuralphotoredactor.data.local.entity.NeuralModelEntity
import com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity
import com.example.neuralphotoredactor.data.local.entity.ProcessingOperationEntity

/**
 * База данных Room для приложения.
 * 
 * Версия 2 - добавлены новые сущности:
 * - ProcessingOperationEntity (операции обработки)
 * - NeuralModelEntity (нейросетевые модели)
 * 
 * Версия 1 - начальная версия схемы БД (ProcessingHistoryEntity).
 * 
 * При изменении схемы необходимо:
 * 1. Увеличить версию БД
 * 2. Добавить миграцию в DatabaseModule
 * 3. Задокументировать изменения
 */
@Database(
    entities = [
        ProcessingHistoryEntity::class, // Основная таблица для истории обработки
        ProcessingOperationEntity::class,
        NeuralModelEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Получить DAO для работы с историей обработок (основная таблица).
     */
    abstract fun processingHistoryDao(): ProcessingHistoryDao
    
    /**
     * Получить DAO для работы с операциями обработки.
     */
    abstract fun processingOperationDao(): ProcessingOperationDao
    
    /**
     * Получить DAO для работы с нейросетевыми моделями.
     */
    abstract fun neuralModelDao(): NeuralModelDao
}

