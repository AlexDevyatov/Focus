package com.example.neuralphotoredactor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с историей обработки изображений.
 * 
 * Предоставляет методы для CRUD операций с историей обработок.
 */
@Dao
interface ProcessingHistoryDao {
    
    /**
     * Получить всю историю обработок, отсортированную по времени (новые первыми).
     * 
     * История теперь связана с processing_operations через operationId.
     * 
     * @return Flow со списком всех записей истории
     */
    @Query("SELECT * FROM processing_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ProcessingHistoryEntity>>
    
    /**
     * Вставить новую запись в историю.
     * 
     * @param entity Запись для вставки
     * @return ID вставленной записи
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProcessingHistoryEntity): Long
    
    /**
     * Удалить запись из истории.
     * 
     * @param entity Запись для удаления
     */
    @Delete
    suspend fun delete(entity: ProcessingHistoryEntity)
    
    /**
     * Удалить запись по ID.
     * 
     * @param id ID записи для удаления
     */
    @Query("DELETE FROM processing_history WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * Найти запись по processedUri и timestamp.
     * 
     * @param processedUri URI обработанного изображения
     * @param timestamp Время обработки
     * @return Entity или null, если не найдено
     */
    @Query("SELECT * FROM processing_history WHERE processedUri = :processedUri AND timestamp = :timestamp LIMIT 1")
    suspend fun findByUriAndTimestamp(processedUri: String, timestamp: Long): ProcessingHistoryEntity?
}

