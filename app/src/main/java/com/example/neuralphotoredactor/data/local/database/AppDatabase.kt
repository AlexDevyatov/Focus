package com.example.neuralphotoredactor.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.withTransaction
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
    version = 2, // Увеличена версия из-за изменений схемы: удалено filterType из processing_history, удалено operationType и filterId стал NOT NULL в processing_operations
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
    
    /**
     * Сохранить историю обработки и операции в транзакции.
     * 
     * @param historyEntity Запись истории обработки
     * @param operations Список операций обработки (historyId должен быть 0, будет установлен автоматически)
     * @return ID созданной записи истории
     */
    suspend fun saveHistoryWithOperations(
        historyEntity: ProcessingHistoryEntity,
        operations: List<ProcessingOperationEntity>
    ): Long {
        val historyId = withTransaction {
            android.util.Log.d("AppDatabase", "Начало транзакции: операций для сохранения: ${operations.size}")
            val id = processingHistoryDao().insert(historyEntity)
            android.util.Log.d("AppDatabase", "История сохранена с ID: $id")
            
            if (operations.isNotEmpty()) {
                // Обновляем historyId для всех операций (сохраняем sequenceNumber из исходных операций)
                val operationsWithHistoryId = operations.map { it.copy(id = 0, historyId = id) }
                android.util.Log.d("AppDatabase", "Сохранение ${operationsWithHistoryId.size} операций с historyId=$id")
                operationsWithHistoryId.forEachIndexed { index, op ->
                    android.util.Log.d("AppDatabase", "Операция $index: filterId=${op.filterId}, sequenceNumber=${op.sequenceNumber}, historyId=${op.historyId}")
                }
                processingOperationDao().insertAll(operationsWithHistoryId)
                android.util.Log.d("AppDatabase", "insertAll вызван для ${operationsWithHistoryId.size} операций")
            } else {
                android.util.Log.w("AppDatabase", "Список операций пуст, операции не сохраняются")
            }
            
            id
        }
        
        // Проверяем после завершения транзакции
        if (operations.isNotEmpty()) {
            val savedOperations = processingOperationDao().getOperationsByHistoryIdSuspend(historyId)
            android.util.Log.d("AppDatabase", "Проверка после транзакции: сохранено операций в БД: ${savedOperations.size}")
            if (savedOperations.size != operations.size) {
                android.util.Log.e("AppDatabase", "ОШИБКА: Ожидалось ${operations.size} операций, но сохранено ${savedOperations.size}")
            } else {
                android.util.Log.d("AppDatabase", "Операции успешно сохранены: ${savedOperations.size}")
            }
        }
        
        return historyId
    }
}

