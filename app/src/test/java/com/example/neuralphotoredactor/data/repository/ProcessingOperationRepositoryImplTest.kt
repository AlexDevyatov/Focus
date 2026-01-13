package com.example.neuralphotoredactor.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.neuralphotoredactor.data.local.dao.FilterDao
import com.example.neuralphotoredactor.data.local.dao.ProcessingHistoryDao
import com.example.neuralphotoredactor.data.local.dao.ProcessingOperationDao
import com.example.neuralphotoredactor.data.local.database.AppDatabase
import com.example.neuralphotoredactor.data.local.entity.FilterEntity
import com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity
import com.example.neuralphotoredactor.domain.model.OperationParameters
import com.example.neuralphotoredactor.domain.model.ProcessingOperation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Unit тесты для ProcessingOperationRepositoryImpl.
 * 
 * Использует in-memory Room базу данных для тестирования DAO.
 * Robolectric нужен только для ApplicationProvider.getApplicationContext(),
 * сам Room работает в памяти без эмулятора. Это быстрее и чище, чем мокировать DAO.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Используем SDK 34, так как Robolectric 4.11.1 поддерживает до SDK 34
class ProcessingOperationRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ProcessingOperationDao
    private lateinit var filterDao: FilterDao
    private lateinit var historyDao: ProcessingHistoryDao
    private lateinit var repository: ProcessingOperationRepositoryImpl

    @Before
    fun setup() {
        // Создаем in-memory базу данных для тестов
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries() // Для тестов разрешаем выполнение на главном потоке
            .build()
        
        dao = database.processingOperationDao()
        filterDao = database.filterDao()
        historyDao = database.processingHistoryDao()
        repository = ProcessingOperationRepositoryImpl(dao)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }
    
    /**
     * Создать фильтр в БД для тестов.
     */
    private suspend fun createTestFilter(name: String = "GAUSSIAN_BLUR"): Long {
        val filter = FilterEntity(name = name)
        return filterDao.insert(filter)
    }
    
    /**
     * Создать историю в БД для тестов.
     */
    private suspend fun createTestHistory(): Long {
        val history = ProcessingHistoryEntity(
            originalUri = "content://test/original",
            processedUri = "content://test/processed",
            timestamp = System.currentTimeMillis()
        )
        return historyDao.insert(history)
    }

    @Test
    fun `getOperationsBySessionId should return mapped operations`() = runTest {
        // Given
        val sessionId = 1L
        val historyId = createTestHistory()
        val filterId = createTestFilter()
        val operation = ProcessingOperation(
            id = 0,
            historyId = historyId,
            sessionId = sessionId,
            filterId = filterId,
            parameters = OperationParameters(filterType = "GAUSSIAN_BLUR", intensity = 0.5f),
            inputImageUri = android.net.Uri.parse("content://test/input"),
            outputImageUri = android.net.Uri.parse("content://test/output"),
            processingTimeMs = 100,
            sequenceNumber = 1
        )
        // Сначала добавляем операцию в БД
        repository.addOperation(operation)

        // When
        val result = repository.getOperationsBySessionId(sessionId).first()

        // Then
        assertEquals(1, result.size)
        assertEquals(sessionId, result[0].sessionId)
        assertEquals(filterId, result[0].filterId)
        assertEquals("GAUSSIAN_BLUR", result[0].parameters.filterType)
    }

    @Test
    fun `getOperationsBySessionId should return empty list when no operations`() = runTest {
        // Given
        val sessionId = 1L
        // База данных пуста, так как мы не добавили операции

        // When
        val result = repository.getOperationsBySessionId(sessionId).first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getOperationById should return mapped operation`() = runTest {
        // Given
        val historyId = createTestHistory()
        val filterId = createTestFilter()
        val operation = ProcessingOperation(
            id = 0,
            historyId = historyId,
            sessionId = 1L,
            filterId = filterId,
            parameters = OperationParameters(filterType = "GAUSSIAN_BLUR", intensity = 0.5f),
            inputImageUri = android.net.Uri.parse("content://test/input"),
            outputImageUri = android.net.Uri.parse("content://test/output"),
            processingTimeMs = 100,
            sequenceNumber = 1
        )
        val operationId = repository.addOperation(operation)

        // When
        val result = repository.getOperationById(operationId)

        // Then
        assertNotNull(result)
        assertEquals(operationId, result?.id)
        assertEquals(filterId, result?.filterId)
        assertEquals("GAUSSIAN_BLUR", result?.parameters?.filterType)
    }

    @Test
    fun `getOperationById should return null when not found`() = runTest {
        // Given
        val operationId = 999L
        // База данных пуста, операция с таким ID не существует

        // When
        val result = repository.getOperationById(operationId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getLastOperationBySessionId should return mapped operation`() = runTest {
        // Given
        val sessionId = 1L
        val historyId = createTestHistory()
        val filterId = createTestFilter()
        val operation = ProcessingOperation(
            id = 0,
            historyId = historyId,
            sessionId = sessionId,
            filterId = filterId,
            parameters = OperationParameters(filterType = "GAUSSIAN_BLUR", intensity = 0.5f),
            inputImageUri = android.net.Uri.parse("content://test/input"),
            outputImageUri = android.net.Uri.parse("content://test/output"),
            processingTimeMs = 100,
            sequenceNumber = 1
        )
        repository.addOperation(operation)

        // When
        val result = repository.getLastOperationBySessionId(sessionId)

        // Then
        assertNotNull(result)
        assertEquals(sessionId, result?.sessionId)
        assertEquals(filterId, result?.filterId)
        assertEquals("GAUSSIAN_BLUR", result?.parameters?.filterType)
    }

    @Test
    fun `getLastOperationBySessionId should return null when not found`() = runTest {
        // Given
        val sessionId = 999L
        // База данных пуста, операций для этой сессии нет

        // When
        val result = repository.getLastOperationBySessionId(sessionId)

        // Then
        assertNull(result)
    }

    @Test
    fun `addOperation should insert and return id`() = runTest {
        // Given
        val historyId = createTestHistory()
        val filterId = createTestFilter()
        val operation = ProcessingOperation(
            id = 0,
            historyId = historyId,
            sessionId = 1L,
            filterId = filterId,
            parameters = OperationParameters(filterType = "GAUSSIAN_BLUR", intensity = 0.5f),
            inputImageUri = android.net.Uri.parse("content://test/input"),
            outputImageUri = android.net.Uri.parse("content://test/output"),
            processingTimeMs = 100,
            sequenceNumber = 1
        )

        // When
        val result = repository.addOperation(operation)

        // Then
        assertTrue(result > 0)
        // Проверяем, что операция действительно сохранена
        val retrieved = repository.getOperationById(result)
        assertNotNull(retrieved)
        assertEquals(filterId, retrieved?.filterId)
        assertEquals("GAUSSIAN_BLUR", retrieved?.parameters?.filterType)
    }

    @Test
    fun `deleteOperation should delete from dao`() = runTest {
        // Given
        val historyId = createTestHistory()
        val filterId = createTestFilter()
        val operation = ProcessingOperation(
            id = 0,
            historyId = historyId,
            sessionId = 1L,
            filterId = filterId,
            parameters = OperationParameters(),
            inputImageUri = android.net.Uri.parse("content://test/input"),
            outputImageUri = android.net.Uri.parse("content://test/output"),
            processingTimeMs = 100,
            sequenceNumber = 1
        )
        val operationId = repository.addOperation(operation)
        val operationWithId = operation.copy(id = operationId)

        // When
        repository.deleteOperation(operationWithId)

        // Then
        // Проверяем, что операция действительно удалена
        val result = repository.getOperationById(operationId)
        assertNull(result)
    }

    @Test
    fun `deleteOperationsBySessionId should delete from dao`() = runTest {
        // Given
        val sessionId = 1L
        val historyId = createTestHistory()
        val filterId = createTestFilter()
        val operation = ProcessingOperation(
            id = 0,
            historyId = historyId,
            sessionId = sessionId,
            filterId = filterId,
            parameters = OperationParameters(),
            inputImageUri = android.net.Uri.parse("content://test/input"),
            outputImageUri = android.net.Uri.parse("content://test/output"),
            processingTimeMs = 100,
            sequenceNumber = 1
        )
        repository.addOperation(operation)
        
        // Убеждаемся, что операция есть
        assertTrue(repository.getOperationsBySessionId(sessionId).first().isNotEmpty())

        // When
        repository.deleteOperationsBySessionId(sessionId)

        // Then
        // Проверяем, что операции действительно удалены
        val result = repository.getOperationsBySessionId(sessionId).first()
        assertTrue(result.isEmpty())
    }
}

