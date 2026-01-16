package com.example.neuralphotoredactor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.neuralphotoredactor.data.local.entity.NeuralModelEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с нейросетевыми моделями.
 *
 * Предоставляет методы для CRUD операций с моделями.
 */
@Dao
interface NeuralModelDao {
    /**
     * Получить все активные модели.
     *
     * @return Flow со списком активных моделей
     */
    @Query("SELECT * FROM neural_models WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveModels(): Flow<List<NeuralModelEntity>>

    /**
     * Получить все модели (включая неактивные).
     *
     * @return Flow со списком всех моделей
     */
    @Query("SELECT * FROM neural_models ORDER BY name ASC")
    fun getAllModels(): Flow<List<NeuralModelEntity>>

    /**
     * Получить модели по типу.
     *
     * @param type Тип модели
     * @return Flow со списком моделей указанного типа
     */
    @Query("SELECT * FROM neural_models WHERE type = :type AND isActive = 1 ORDER BY name ASC")
    fun getModelsByType(type: String): Flow<List<NeuralModelEntity>>

    /**
     * Получить модель по ID.
     *
     * @param id ID модели
     * @return Модель или null, если не найдена
     */
    @Query("SELECT * FROM neural_models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: Long): NeuralModelEntity?

    /**
     * Получить модель по имени.
     *
     * @param name Название модели
     * @return Модель или null, если не найдена
     */
    @Query("SELECT * FROM neural_models WHERE name = :name LIMIT 1")
    suspend fun getModelByName(name: String): NeuralModelEntity?

    /**
     * Вставить новую модель.
     *
     * @param model Модель для вставки
     * @return ID вставленной модели
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(model: NeuralModelEntity): Long

    /**
     * Вставить несколько моделей.
     *
     * @param models Список моделей для вставки
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(models: List<NeuralModelEntity>)

    /**
     * Обновить существующую модель.
     *
     * @param model Модель для обновления
     */
    @Update
    suspend fun update(model: NeuralModelEntity)

    /**
     * Удалить модель.
     *
     * @param model Модель для удаления
     */
    @Delete
    suspend fun delete(model: NeuralModelEntity)

    /**
     * Удалить модель по ID.
     *
     * @param id ID модели для удаления
     */
    @Query("DELETE FROM neural_models WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Активировать/деактивировать модель.
     *
     * @param id ID модели
     * @param isActive Новое состояние активности
     */
    @Query("UPDATE neural_models SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(
        id: Long,
        isActive: Boolean,
    )
}
