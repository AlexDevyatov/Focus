package com.example.neuralphotoredactor.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.neuralphotoredactor.data.local.dao.FilterDao
import com.example.neuralphotoredactor.data.local.dao.NeuralModelDao
import com.example.neuralphotoredactor.data.local.dao.ProcessingHistoryDao
import com.example.neuralphotoredactor.data.local.dao.ProcessingOperationDao
import com.example.neuralphotoredactor.data.local.entity.FilterEntity
import com.example.neuralphotoredactor.data.local.entity.NeuralModelEntity
import com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity
import com.example.neuralphotoredactor.data.local.entity.ProcessingOperationEntity

/**
 * База данных Room для приложения.
 * 
 * Первоначальная схема БД включает:
 * - ProcessingHistoryEntity (история обработки)
 * - ProcessingOperationEntity (детальные операции обработки, связанные с историей через historyId)
 * - FilterEntity (фильтры с ссылкой на модели)
 * - NeuralModelEntity (нейросетевые модели)
 * 
 * При изменении схемы необходимо:
 * 1. Увеличить версию БД
 * 2. Добавить миграцию в DatabaseModule
 * 3. Задокументировать изменения
 */
@Database(
    entities = [
        ProcessingHistoryEntity::class, // Основная таблица для истории обработки
        ProcessingOperationEntity::class, // Операции обработки, связанные с историей через historyId
        FilterEntity::class, // Фильтры с ссылкой на модели
        NeuralModelEntity::class // Нейросетевые модели
    ],
    version = 1,
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
     * Получить DAO для работы с фильтрами.
     */
    abstract fun filterDao(): FilterDao
    
    /**
     * Получить DAO для работы с нейросетевыми моделями.
     */
    abstract fun neuralModelDao(): NeuralModelDao
}

