package com.example.neuralphotoredactor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.neuralphotoredactor.data.local.entity.ProcessingOperationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с операциями обработки.
 *
 * Предоставляет методы для CRUD операций с операциями обработки.
 */
@Dao
interface ProcessingOperationDao {
    /**
     * Получить все операции для конкретной сессии, отсортированные по порядковому номеру.
     *
     * @param sessionId ID сессии
     * @return Flow со списком операций
     */
    @Query(
        "SELECT * FROM processing_operations WHERE sessionId = :sessionId ORDER BY sequenceNumber ASC",
    )
    fun getOperationsBySessionId(sessionId: Long): Flow<List<ProcessingOperationEntity>>

    /**
     * Получить все операции для конкретной сессии (suspend версия).
     *
     * @param sessionId ID сессии
     * @return Список операций
     */
    @Query(
        "SELECT * FROM processing_operations WHERE sessionId = :sessionId ORDER BY sequenceNumber ASC",
    )
    suspend fun getOperationsBySessionIdSuspend(sessionId: Long): List<ProcessingOperationEntity>

    /**
     * Получить все операции для конкретной записи истории, отсортированные по порядковому номеру.
     *
     * @param historyId ID записи в истории обработки
     * @return Flow со списком операций
     */
    @Query(
        "SELECT * FROM processing_operations WHERE historyId = :historyId ORDER BY sequenceNumber ASC",
    )
    fun getOperationsByHistoryId(historyId: Long): Flow<List<ProcessingOperationEntity>>

    /**
     * Получить все операции для конкретной записи истории (suspend версия).
     *
     * @param historyId ID записи в истории обработки
     * @return Список операций
     */
    @Query(
        "SELECT * FROM processing_operations WHERE historyId = :historyId ORDER BY sequenceNumber ASC",
    )
    suspend fun getOperationsByHistoryIdSuspend(historyId: Long): List<ProcessingOperationEntity>

    /**
     * Получить операцию по ID.
     *
     * @param id ID операции
     * @return Операция или null, если не найдена
     */
    @Query("SELECT * FROM processing_operations WHERE id = :id LIMIT 1")
    suspend fun getOperationById(id: Long): ProcessingOperationEntity?

    /**
     * Получить последнюю операцию для сессии.
     *
     * @param sessionId ID сессии
     * @return Последняя операция или null
     */
    @Query(
        "SELECT * FROM processing_operations WHERE sessionId = :sessionId ORDER BY sequenceNumber DESC LIMIT 1",
    )
    suspend fun getLastOperationBySessionId(sessionId: Long): ProcessingOperationEntity?

    /**
     * Получить максимальный порядковый номер для сессии.
     *
     * @param sessionId ID сессии
     * @return Максимальный порядковый номер или 0, если операций нет
     */
    @Query(
        "SELECT COALESCE(MAX(sequenceNumber), 0) FROM processing_operations WHERE sessionId = :sessionId",
    )
    suspend fun getMaxSequenceNumber(sessionId: Long): Int

    /**
     * Вставить новую операцию.
     *
     * @param operation Операция для вставки
     * @return ID вставленной операции
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: ProcessingOperationEntity): Long

    /**
     * Вставить несколько операций.
     *
     * @param operations Список операций для вставки
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(operations: List<ProcessingOperationEntity>)

    /**
     * Удалить операцию.
     *
     * @param operation Операция для удаления
     */
    @Delete
    suspend fun delete(operation: ProcessingOperationEntity)

    /**
     * Удалить операцию по ID.
     *
     * @param id ID операции для удаления
     */
    @Query("DELETE FROM processing_operations WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Удалить все операции для сессии.
     *
     * @param sessionId ID сессии
     */
    @Query("DELETE FROM processing_operations WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)
}
