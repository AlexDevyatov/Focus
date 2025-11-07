package com.example.neuralphotoredactor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) для работы с историей обработок в Room базе данных.
 * 
 * Определяет методы для выполнения CRUD операций над записями истории обработок.
 * Использует Flow для реактивного получения данных, что позволяет автоматически
 * обновлять UI при изменении данных в базе.
 * 
 * @see com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity
 */
@Dao
interface ProcessingHistoryDao {
    /**
     * Получает поток всех записей истории обработок, отсортированных по времени (новые первыми).
     * 
     * @return Flow со списком всех записей истории обработок
     */
    @Query("SELECT * FROM processing_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ProcessingHistoryEntity>>
    
    /**
     * Получает конкретную запись истории по идентификатору.
     * 
     * @param id Идентификатор записи
     * @return ProcessingHistoryEntity или null, если запись не найдена
     */
    @Query("SELECT * FROM processing_history WHERE id = :id")
    suspend fun getById(id: String): ProcessingHistoryEntity?
    
    /**
     * Вставляет новую запись в базу данных или обновляет существующую при конфликте.
     * 
     * @param entity Запись для вставки/обновления
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProcessingHistoryEntity)
    
    /**
     * Удаляет запись из базы данных.
     * 
     * @param entity Запись для удаления
     */
    @Delete
    suspend fun delete(entity: ProcessingHistoryEntity)
    
    /**
     * Удаляет запись из базы данных по идентификатору.
     * 
     * @param id Идентификатор записи для удаления
     */
    @Query("DELETE FROM processing_history WHERE id = :id")
    suspend fun deleteById(id: String)
}

