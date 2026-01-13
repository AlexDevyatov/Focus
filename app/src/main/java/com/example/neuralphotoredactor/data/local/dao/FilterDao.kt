package com.example.neuralphotoredactor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.neuralphotoredactor.data.local.entity.FilterEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с фильтрами.
 * 
 * Предоставляет методы для CRUD операций с фильтрами.
 */
@Dao
interface FilterDao {
    
    /**
     * Получить все фильтры.
     * 
     * @return Flow со списком всех фильтров
     */
    @Query("SELECT * FROM filters ORDER BY name ASC")
    fun getAllFilters(): Flow<List<FilterEntity>>
    
    /**
     * Получить фильтр по ID.
     * 
     * @param id ID фильтра
     * @return Фильтр или null, если не найден
     */
    @Query("SELECT * FROM filters WHERE id = :id LIMIT 1")
    suspend fun getFilterById(id: Long): FilterEntity?
    
    /**
     * Получить фильтр по имени.
     * 
     * @param name Название фильтра
     * @return Фильтр или null, если не найден
     */
    @Query("SELECT * FROM filters WHERE name = :name LIMIT 1")
    suspend fun getFilterByName(name: String): FilterEntity?
    
    /**
     * Вставить новый фильтр.
     * 
     * @param filter Фильтр для вставки
     * @return ID вставленного фильтра
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(filter: FilterEntity): Long
    
    /**
     * Вставить несколько фильтров.
     * 
     * @param filters Список фильтров для вставки
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(filters: List<FilterEntity>)
}

