package com.example.neuralphotoredactor.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.neuralphotoredactor.data.local.dao.ProcessingHistoryDao
import com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity

/**
 * База данных Room для приложения.
 * 
 * Версия 1 - начальная версия схемы БД.
 * 
 * При изменении схемы необходимо:
 * 1. Увеличить версию БД
 * 2. Добавить миграцию в DatabaseModule
 * 3. Задокументировать изменения
 */
@Database(
    entities = [ProcessingHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Получить DAO для работы с историей обработок.
     */
    abstract fun processingHistoryDao(): ProcessingHistoryDao
}

