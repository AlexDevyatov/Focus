package com.example.neuralphotoredactor.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.neuralphotoredactor.data.local.dao.ProcessingHistoryDao
import com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity

/**
 * Главная база данных приложения, использующая Room.
 * 
 * Определяет структуру базы данных, версию и список всех entities.
 * Предоставляет доступ к DAO интерфейсам для работы с данными.
 * Создается через Hilt в DatabaseModule.
 * 
 * @see com.example.neuralphotoredactor.di.DatabaseModule
 */
@Database(
    entities = [ProcessingHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Получает DAO для работы с историей обработок.
     * 
     * @return ProcessingHistoryDao для выполнения операций с историей
     */
    abstract fun processingHistoryDao(): ProcessingHistoryDao
}

