package com.example.neuralphotoredactor.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.neuralphotoredactor.data.local.database.AppDatabase
import com.example.neuralphotoredactor.data.local.entity.FilterEntity
import com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity
import com.example.neuralphotoredactor.data.local.entity.ProcessingOperationEntity
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

/**
 * Unit тесты для ProcessingOperationDao.
 *
 * Использует in-memory базу данных Room для тестирования всех методов DAO.
 * Требует наличия связанных сущностей (ProcessingHistoryEntity и FilterEntity) из-за foreign keys.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProcessingOperationDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ProcessingOperationDao
    private lateinit var historyDao: ProcessingHistoryDao
    private lateinit var filterDao: FilterDao

    @Before
    fun setup() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            )
                .allowMainThreadQueries()
                .build()
        dao = database.processingOperationDao()
        historyDao = database.processingHistoryDao()
        filterDao = database.filterDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createTestHistory(): Long {
        val history =
            ProcessingHistoryEntity(
                id = 0,
                originalUri = "content://test/original",
                processedUri = "content://test/processed",
                timestamp = System.currentTimeMillis(),
            )
        return historyDao.insert(history)
    }

    private suspend fun createTestFilter(): Long {
        val filter =
            FilterEntity(
                id = 0,
                name = "TEST_FILTER",
                modelId = null,
            )
        return filterDao.insert(filter)
    }

    @Test
    fun `getOperationsBySessionId should return empty list when no operations`() =
        runTest {
            // When
            val result = dao.getOperationsBySessionId(1L).first()

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `getOperationsBySessionId should return operations sorted by sequenceNumber`() =
        runTest {
            // Given
            val historyId = createTestHistory()
            val filterId = createTestFilter()
            val sessionId = 1L

            val op1 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = sessionId,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input1",
                    outputImageUri = "content://test/output1",
                    processingTimeMs = 100L,
                    sequenceNumber = 2,
                )
            val op2 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = sessionId,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input2",
                    outputImageUri = "content://test/output2",
                    processingTimeMs = 200L,
                    sequenceNumber = 1,
                )
            val op3 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = sessionId,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input3",
                    outputImageUri = "content://test/output3",
                    processingTimeMs = 300L,
                    sequenceNumber = 3,
                )

            dao.insert(op1)
            dao.insert(op2)
            dao.insert(op3)

            // When
            val result = dao.getOperationsBySessionId(sessionId).first()

            // Then
            assertEquals(3, result.size)
            // Проверяем сортировку по sequenceNumber ASC
            assertEquals(1, result[0].sequenceNumber)
            assertEquals(2, result[1].sequenceNumber)
            assertEquals(3, result[2].sequenceNumber)
        }

    @Test
    fun `getOperationsBySessionId should return only operations for specified sessionId`() =
        runTest {
            // Given
            val historyId = createTestHistory()
            val filterId = createTestFilter()

            val op1 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 1L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input1",
                    outputImageUri = "content://test/output1",
                    processingTimeMs = 100L,
                    sequenceNumber = 1,
                )
            val op2 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 2L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input2",
                    outputImageUri = "content://test/output2",
                    processingTimeMs = 200L,
                    sequenceNumber = 1,
                )

            dao.insert(op1)
            dao.insert(op2)

            // When
            val result = dao.getOperationsBySessionId(1L).first()

            // Then
            assertEquals(1, result.size)
            assertEquals(1L, result[0].sessionId)
        }

    @Test
    fun `getOperationsBySessionIdSuspend should return operations sorted by sequenceNumber`() =
        runTest {
            // Given
            val historyId = createTestHistory()
            val filterId = createTestFilter()
            val sessionId = 1L

            val op1 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = sessionId,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input1",
                    outputImageUri = "content://test/output1",
                    processingTimeMs = 100L,
                    sequenceNumber = 3,
                )
            val op2 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = sessionId,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input2",
                    outputImageUri = "content://test/output2",
                    processingTimeMs = 200L,
                    sequenceNumber = 1,
                )

            dao.insert(op1)
            dao.insert(op2)

            // When
            val result = dao.getOperationsBySessionIdSuspend(sessionId)

            // Then
            assertEquals(2, result.size)
            assertEquals(1, result[0].sequenceNumber)
            assertEquals(3, result[1].sequenceNumber)
        }

    @Test
    fun `getOperationsByHistoryId should return empty list when no operations`() =
        runTest {
            // Given
            val historyId = createTestHistory()

            // When
            val result = dao.getOperationsByHistoryId(historyId).first()

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `getOperationsByHistoryId should return operations sorted by sequenceNumber`() =
        runTest {
            // Given
            val historyId1 = createTestHistory()
            val historyId2 = createTestHistory()
            val filterId = createTestFilter()

            val op1 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId1,
                    sessionId = 1L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input1",
                    outputImageUri = "content://test/output1",
                    processingTimeMs = 100L,
                    sequenceNumber = 2,
                )
            val op2 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId1,
                    sessionId = 1L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input2",
                    outputImageUri = "content://test/output2",
                    processingTimeMs = 200L,
                    sequenceNumber = 1,
                )
            val op3 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId2,
                    sessionId = 2L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input3",
                    outputImageUri = "content://test/output3",
                    processingTimeMs = 300L,
                    sequenceNumber = 1,
                )

            dao.insert(op1)
            dao.insert(op2)
            dao.insert(op3)

            // When
            val result = dao.getOperationsByHistoryId(historyId1).first()

            // Then
            assertEquals(2, result.size)
            assertEquals(1, result[0].sequenceNumber)
            assertEquals(2, result[1].sequenceNumber)
            assertTrue(result.all { it.historyId == historyId1 })
        }

    @Test
    fun `getOperationsByHistoryIdSuspend should return operations sorted by sequenceNumber`() =
        runTest {
            // Given
            val historyId = createTestHistory()
            val filterId = createTestFilter()

            val op1 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 1L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input1",
                    outputImageUri = "content://test/output1",
                    processingTimeMs = 100L,
                    sequenceNumber = 2,
                )
            val op2 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 1L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input2",
                    outputImageUri = "content://test/output2",
                    processingTimeMs = 200L,
                    sequenceNumber = 1,
                )

            dao.insert(op1)
            dao.insert(op2)

            // When
            val result = dao.getOperationsByHistoryIdSuspend(historyId)

            // Then
            assertEquals(2, result.size)
            assertEquals(1, result[0].sequenceNumber)
            assertEquals(2, result[1].sequenceNumber)
        }

    @Test
    fun `getOperationById should return operation when found`() =
        runTest {
            // Given
            val historyId = createTestHistory()
            val filterId = createTestFilter()

            val operation =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 1L,
                    filterId = filterId,
                    parameters = """{"param": "value"}""",
                    inputImageUri = "content://test/input",
                    outputImageUri = "content://test/output",
                    processingTimeMs = 150L,
                    sequenceNumber = 1,
                )
            val id = dao.insert(operation)

            // When
            val result = dao.getOperationById(id)

            // Then
            assertNotNull(result)
            assertEquals(id, result?.id)
            assertEquals(historyId, result?.historyId)
            assertEquals(1L, result?.sessionId)
            assertEquals(filterId, result?.filterId)
            assertEquals("""{"param": "value"}""", result?.parameters)
            assertEquals("content://test/input", result?.inputImageUri)
            assertEquals("content://test/output", result?.outputImageUri)
            assertEquals(150L, result?.processingTimeMs)
            assertEquals(1, result?.sequenceNumber)
        }

    @Test
    fun `getOperationById should return null when not found`() =
        runTest {
            // When
            val result = dao.getOperationById(999L)

            // Then
            assertNull(result)
        }

    @Test
    fun `getLastOperationBySessionId should return last operation by sequenceNumber`() =
        runTest {
            // Given
            val historyId = createTestHistory()
            val filterId = createTestFilter()
            val sessionId = 1L

            val op1 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = sessionId,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input1",
                    outputImageUri = "content://test/output1",
                    processingTimeMs = 100L,
                    sequenceNumber = 1,
                )
            val op2 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = sessionId,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input2",
                    outputImageUri = "content://test/output2",
                    processingTimeMs = 200L,
                    sequenceNumber = 3,
                )
            val op3 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = sessionId,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input3",
                    outputImageUri = "content://test/output3",
                    processingTimeMs = 300L,
                    sequenceNumber = 2,
                )

            dao.insert(op1)
            val id2 = dao.insert(op2)
            dao.insert(op3)

            // When
            val result = dao.getLastOperationBySessionId(sessionId)

            // Then
            assertNotNull(result)
            assertEquals(id2, result?.id)
            assertEquals(3, result?.sequenceNumber)
        }

    @Test
    fun `getLastOperationBySessionId should return null when no operations`() =
        runTest {
            // When
            val result = dao.getLastOperationBySessionId(1L)

            // Then
            assertNull(result)
        }

    @Test
    fun `getMaxSequenceNumber should return 0 when no operations`() =
        runTest {
            // When
            val result = dao.getMaxSequenceNumber(1L)

            // Then
            assertEquals(0, result)
        }

    @Test
    fun `getMaxSequenceNumber should return maximum sequenceNumber for sessionId`() =
        runTest {
            // Given
            val historyId = createTestHistory()
            val filterId = createTestFilter()
            val sessionId = 1L

            val op1 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = sessionId,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input1",
                    outputImageUri = "content://test/output1",
                    processingTimeMs = 100L,
                    sequenceNumber = 1,
                )
            val op2 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = sessionId,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input2",
                    outputImageUri = "content://test/output2",
                    processingTimeMs = 200L,
                    sequenceNumber = 5,
                )
            val op3 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 2L, // Другая сессия
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input3",
                    outputImageUri = "content://test/output3",
                    processingTimeMs = 300L,
                    sequenceNumber = 10,
                )

            dao.insert(op1)
            dao.insert(op2)
            dao.insert(op3)

            // When
            val result = dao.getMaxSequenceNumber(sessionId)

            // Then
            assertEquals(5, result)
        }

    @Test
    fun `insert should insert operation and return generated id`() =
        runTest {
            // Given
            val historyId = createTestHistory()
            val filterId = createTestFilter()

            val operation =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 1L,
                    filterId = filterId,
                    parameters = """{"test": "value"}""",
                    inputImageUri = "content://test/input",
                    outputImageUri = "content://test/output",
                    processingTimeMs = 100L,
                    sequenceNumber = 1,
                )

            // When
            val id = dao.insert(operation)

            // Then
            assertTrue(id > 0)
            val inserted = dao.getOperationById(id)
            assertNotNull(inserted)
            assertEquals(historyId, inserted?.historyId)
            assertEquals(1L, inserted?.sessionId)
            assertEquals(filterId, inserted?.filterId)
            assertEquals(1, inserted?.sequenceNumber)
        }

    @Test
    fun `insertAll should insert multiple operations`() =
        runTest {
            // Given
            val historyId = createTestHistory()
            val filterId = createTestFilter()

            val operations =
                listOf(
                    ProcessingOperationEntity(
                        id = 0,
                        historyId = historyId,
                        sessionId = 1L,
                        filterId = filterId,
                        parameters = "{}",
                        inputImageUri = "content://test/input1",
                        outputImageUri = "content://test/output1",
                        processingTimeMs = 100L,
                        sequenceNumber = 1,
                    ),
                    ProcessingOperationEntity(
                        id = 0,
                        historyId = historyId,
                        sessionId = 1L,
                        filterId = filterId,
                        parameters = "{}",
                        inputImageUri = "content://test/input2",
                        outputImageUri = "content://test/output2",
                        processingTimeMs = 200L,
                        sequenceNumber = 2,
                    ),
                    ProcessingOperationEntity(
                        id = 0,
                        historyId = historyId,
                        sessionId = 1L,
                        filterId = filterId,
                        parameters = "{}",
                        inputImageUri = "content://test/input3",
                        outputImageUri = "content://test/output3",
                        processingTimeMs = 300L,
                        sequenceNumber = 3,
                    ),
                )

            // When
            dao.insertAll(operations)

            // Then
            val result = dao.getOperationsByHistoryId(historyId).first()
            assertEquals(3, result.size)
        }

    @Test
    fun `delete should remove operation from database`() =
        runTest {
            // Given
            val historyId = createTestHistory()
            val filterId = createTestFilter()

            val operation =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 1L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input",
                    outputImageUri = "content://test/output",
                    processingTimeMs = 100L,
                    sequenceNumber = 1,
                )
            val id = dao.insert(operation)
            val insertedOperation = dao.getOperationById(id)!!

            // When
            dao.delete(insertedOperation)

            // Then
            assertNull(dao.getOperationById(id))
            val result = dao.getOperationsByHistoryId(historyId).first()
            assertTrue(result.isEmpty())
        }

    @Test
    fun `deleteById should remove operation by id`() =
        runTest {
            // Given
            val historyId = createTestHistory()
            val filterId = createTestFilter()

            val op1 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 1L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input1",
                    outputImageUri = "content://test/output1",
                    processingTimeMs = 100L,
                    sequenceNumber = 1,
                )
            val op2 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 1L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input2",
                    outputImageUri = "content://test/output2",
                    processingTimeMs = 200L,
                    sequenceNumber = 2,
                )
            val id1 = dao.insert(op1)
            val id2 = dao.insert(op2)

            // When
            dao.deleteById(id1)

            // Then
            assertNull(dao.getOperationById(id1))
            assertNotNull(dao.getOperationById(id2))
            val result = dao.getOperationsByHistoryId(historyId).first()
            assertEquals(1, result.size)
            assertEquals(id2, result[0].id)
        }

    @Test
    fun `deleteBySessionId should remove all operations for sessionId`() =
        runTest {
            // Given
            val historyId = createTestHistory()
            val filterId = createTestFilter()

            val op1 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 1L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input1",
                    outputImageUri = "content://test/output1",
                    processingTimeMs = 100L,
                    sequenceNumber = 1,
                )
            val op2 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 1L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input2",
                    outputImageUri = "content://test/output2",
                    processingTimeMs = 200L,
                    sequenceNumber = 2,
                )
            val op3 =
                ProcessingOperationEntity(
                    id = 0,
                    historyId = historyId,
                    sessionId = 2L,
                    filterId = filterId,
                    parameters = "{}",
                    inputImageUri = "content://test/input3",
                    outputImageUri = "content://test/output3",
                    processingTimeMs = 300L,
                    sequenceNumber = 1,
                )

            dao.insert(op1)
            dao.insert(op2)
            val id3 = dao.insert(op3)

            // When
            dao.deleteBySessionId(1L)

            // Then
            val result1 = dao.getOperationsBySessionId(1L).first()
            assertTrue(result1.isEmpty())

            val result2 = dao.getOperationsBySessionId(2L).first()
            assertEquals(1, result2.size)
            assertEquals(id3, result2[0].id)
        }
}
