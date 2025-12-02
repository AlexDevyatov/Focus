package com.example.neuralphotoredactor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.neuralphotoredactor.data.local.entity.EditingSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с сессиями редактирования.
 * 
 * Предоставляет методы для CRUD операций с сессиями редактирования.
 */
@Dao
interface EditingSessionDao {
    
    /**
     * Получить все сессии, отсортированные по времени последнего изменения (новые первыми).
     * 
     * @return Flow со списком всех сессий
     */
    @Query("SELECT * FROM editing_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<EditingSessionEntity>>
    
    /**
     * Получить сессию по ID.
     * 
     * @param id ID сессии
     * @return Сессия или null, если не найдена
     */
    @Query("SELECT * FROM editing_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): EditingSessionEntity?
    
    /**
     * Получить сессию по ID как Flow.
     * 
     * @param id ID сессии
     * @return Flow с сессией или null
     */
    @Query("SELECT * FROM editing_sessions WHERE id = :id LIMIT 1")
    fun getSessionByIdFlow(id: Long): Flow<EditingSessionEntity?>
    
    /**
     * Вставить новую сессию.
     * 
     * @param session Сессия для вставки
     * @return ID вставленной сессии
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: EditingSessionEntity): Long
    
    /**
     * Обновить существующую сессию.
     * 
     * @param session Сессия для обновления
     */
    @Update
    suspend fun update(session: EditingSessionEntity)
    
    /**
     * Удалить сессию.
     * 
     * @param session Сессия для удаления
     */
    @Delete
    suspend fun delete(session: EditingSessionEntity)
    
    /**
     * Удалить сессию по ID.
     * 
     * @param id ID сессии для удаления
     */
    @Query("DELETE FROM editing_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

