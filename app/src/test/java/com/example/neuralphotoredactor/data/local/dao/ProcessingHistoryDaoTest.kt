package com.example.neuralphotoredactor.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.neuralphotoredactor.data.local.database.AppDatabase
import com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity
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
 * Unit тесты для ProcessingHistoryDao.
 *
 * Использует in-memory базу данных Room для тестирования всех методов DAO.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProcessingHistoryDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ProcessingHistoryDao

    @Before
    fun setup() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            )
                .allowMainThreadQueries()
                .build()
        dao = database.processingHistoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getAllHistory should return empty list when database is empty`() =
        runTest {
            // When
            val result = dao.getAllHistory().first()

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `getAllHistory should return all history entries sorted by timestamp descending`() =
        runTest {
            // Given
            val entity1 =
                ProcessingHistoryEntity(
                    id = 0,
                    originalUri = "content://test/original1",
                    processedUri = "content://test/processed1",
                    timestamp = 1000L,
                )
            val entity2 =
                ProcessingHistoryEntity(
                    id = 0,
                    originalUri = "content://test/original2",
                    processedUri = "content://test/processed2",
                    timestamp = 2000L,
                )
            val entity3 =
                ProcessingHistoryEntity(
                    id = 0,
                    originalUri = "content://test/original3",
                    processedUri = "content://test/processed3",
                    timestamp = 1500L,
                )

            val id1 = dao.insert(entity1)
            val id2 = dao.insert(entity2)
            val id3 = dao.insert(entity3)

            // When
            val result = dao.getAllHistory().first()

            // Then
            assertEquals(3, result.size)
            // Проверяем сортировку по timestamp DESC (новые первыми)
            assertEquals(id2, result[0].id)
            assertEquals(2000L, result[0].timestamp)
            assertEquals(id3, result[1].id)
            assertEquals(1500L, result[1].timestamp)
            assertEquals(id1, result[2].id)
            assertEquals(1000L, result[2].timestamp)
        }

    @Test
    fun `insert should insert entity and return generated id`() =
        runTest {
            // Given
            val entity =
                ProcessingHistoryEntity(
                    id = 0,
                    originalUri = "content://test/original",
                    processedUri = "content://test/processed",
                    timestamp = System.currentTimeMillis(),
                )

            // When
            val id = dao.insert(entity)

            // Then
            assertTrue(id > 0)
            val inserted = dao.getHistoryById(id)
            assertNotNull(inserted)
            assertEquals(entity.originalUri, inserted?.originalUri)
            assertEquals(entity.processedUri, inserted?.processedUri)
            assertEquals(entity.timestamp, inserted?.timestamp)
        }

    @Test
    fun `insert should replace existing entity when id matches`() =
        runTest {
            // Given
            val entity =
                ProcessingHistoryEntity(
                    id = 0,
                    originalUri = "content://test/original",
                    processedUri = "content://test/processed",
                    timestamp = 1000L,
                )
            val id = dao.insert(entity)

            val updatedEntity =
                ProcessingHistoryEntity(
                    id = id,
                    originalUri = "content://test/original_updated",
                    processedUri = "content://test/processed_updated",
                    timestamp = 2000L,
                )

            // When
            dao.insert(updatedEntity)

            // Then
            val result = dao.getHistoryById(id)
            assertNotNull(result)
            assertEquals("content://test/original_updated", result?.originalUri)
            assertEquals("content://test/processed_updated", result?.processedUri)
            assertEquals(2000L, result?.timestamp)
        }

    @Test
    fun `delete should remove entity from database`() =
        runTest {
            // Given
            val entity =
                ProcessingHistoryEntity(
                    id = 0,
                    originalUri = "content://test/original",
                    processedUri = "content://test/processed",
                    timestamp = System.currentTimeMillis(),
                )
            val id = dao.insert(entity)
            val insertedEntity = dao.getHistoryById(id)!!

            // When
            dao.delete(insertedEntity)

            // Then
            val result = dao.getHistoryById(id)
            assertNull(result)
            val allHistory = dao.getAllHistory().first()
            assertTrue(allHistory.isEmpty())
        }

    @Test
    fun `deleteById should remove entity by id`() =
        runTest {
            // Given
            val entity1 =
                ProcessingHistoryEntity(
                    id = 0,
                    originalUri = "content://test/original1",
                    processedUri = "content://test/processed1",
                    timestamp = 1000L,
                )
            val entity2 =
                ProcessingHistoryEntity(
                    id = 0,
                    originalUri = "content://test/original2",
                    processedUri = "content://test/processed2",
                    timestamp = 2000L,
                )
            val id1 = dao.insert(entity1)
            val id2 = dao.insert(entity2)

            // When
            dao.deleteById(id1)

            // Then
            assertNull(dao.getHistoryById(id1))
            assertNotNull(dao.getHistoryById(id2))
            val allHistory = dao.getAllHistory().first()
            assertEquals(1, allHistory.size)
            assertEquals(id2, allHistory[0].id)
        }

    @Test
    fun `deleteById should not throw when deleting non-existent id`() =
        runTest {
            // When/Then - не должно быть исключения
            dao.deleteById(999L)
        }

    @Test
    fun `findByUriAndTimestamp should return entity when found`() =
        runTest {
            // Given
            val timestamp = System.currentTimeMillis()
            val entity =
                ProcessingHistoryEntity(
                    id = 0,
                    originalUri = "content://test/original",
                    processedUri = "content://test/processed",
                    timestamp = timestamp,
                )
            val id = dao.insert(entity)

            // When
            val result = dao.findByUriAndTimestamp("content://test/processed", timestamp)

            // Then
            assertNotNull(result)
            assertEquals(id, result?.id)
            assertEquals("content://test/processed", result?.processedUri)
            assertEquals(timestamp, result?.timestamp)
        }

    @Test
    fun `findByUriAndTimestamp should return null when not found`() =
        runTest {
            // When
            val result = dao.findByUriAndTimestamp("content://test/nonexistent", 9999L)

            // Then
            assertNull(result)
        }

    @Test
    fun `findByUriAndTimestamp should return null when uri matches but timestamp does not`() =
        runTest {
            // Given
            val entity =
                ProcessingHistoryEntity(
                    id = 0,
                    originalUri = "content://test/original",
                    processedUri = "content://test/processed",
                    timestamp = 1000L,
                )
            dao.insert(entity)

            // When
            val result = dao.findByUriAndTimestamp("content://test/processed", 2000L)

            // Then
            assertNull(result)
        }

    @Test
    fun `findByUriAndTimestamp should return null when timestamp matches but uri does not`() =
        runTest {
            // Given
            val timestamp = 1000L
            val entity =
                ProcessingHistoryEntity(
                    id = 0,
                    originalUri = "content://test/original",
                    processedUri = "content://test/processed",
                    timestamp = timestamp,
                )
            dao.insert(entity)

            // When
            val result = dao.findByUriAndTimestamp("content://test/different", timestamp)

            // Then
            assertNull(result)
        }

    @Test
    fun `getHistoryById should return entity when found`() =
        runTest {
            // Given
            val entity =
                ProcessingHistoryEntity(
                    id = 0,
                    originalUri = "content://test/original",
                    processedUri = "content://test/processed",
                    timestamp = System.currentTimeMillis(),
                )
            val id = dao.insert(entity)

            // When
            val result = dao.getHistoryById(id)

            // Then
            assertNotNull(result)
            assertEquals(id, result?.id)
            assertEquals(entity.originalUri, result?.originalUri)
            assertEquals(entity.processedUri, result?.processedUri)
            assertEquals(entity.timestamp, result?.timestamp)
        }

    @Test
    fun `getHistoryById should return null when not found`() =
        runTest {
            // When
            val result = dao.getHistoryById(999L)

            // Then
            assertNull(result)
        }
}
