package com.example.neuralphotoredactor.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.neuralphotoredactor.data.local.database.AppDatabase
import com.example.neuralphotoredactor.data.local.entity.FilterEntity
import com.example.neuralphotoredactor.data.local.entity.NeuralModelEntity
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
 * Unit тесты для FilterDao.
 * 
 * Использует in-memory базу данных Room для тестирования всех методов DAO.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FilterDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: FilterDao
    private lateinit var neuralModelDao: NeuralModelDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dao = database.filterDao()
        neuralModelDao = database.neuralModelDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createTestModel(): Long {
        val model = NeuralModelEntity(
            id = 0,
            name = "Test Model",
            type = "STYLE_TRANSFER",
            version = "1.0",
            filePath = "/path/to/model.tflite",
            fileSize = 1024L,
            isActive = true,
            compatibilityLevel = "HIGH"
        )
        return neuralModelDao.insert(model)
    }

    @Test
    fun `getAllFilters should return empty list when database is empty`() = runTest {
        // When
        val result = dao.getAllFilters().first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllFilters should return all filters sorted by name ascending`() = runTest {
        // Given
        val filter1 = FilterEntity(
            id = 0,
            name = "Z_FILTER",
            modelId = null
        )
        val filter2 = FilterEntity(
            id = 0,
            name = "A_FILTER",
            modelId = null
        )
        val filter3 = FilterEntity(
            id = 0,
            name = "M_FILTER",
            modelId = null
        )

        val id1 = dao.insert(filter1)
        val id2 = dao.insert(filter2)
        val id3 = dao.insert(filter3)

        // When
        val result = dao.getAllFilters().first()

        // Then
        assertEquals(3, result.size)
        // Проверяем сортировку по name ASC
        assertEquals(id2, result[0].id)
        assertEquals("A_FILTER", result[0].name)
        assertEquals(id3, result[1].id)
        assertEquals("M_FILTER", result[1].name)
        assertEquals(id1, result[2].id)
        assertEquals("Z_FILTER", result[2].name)
    }

    @Test
    fun `getAllFilters should return filters with and without modelId`() = runTest {
        // Given
        val modelId = createTestModel()

        val filter1 = FilterEntity(
            id = 0,
            name = "FILTER_WITH_MODEL",
            modelId = modelId
        )
        val filter2 = FilterEntity(
            id = 0,
            name = "FILTER_WITHOUT_MODEL",
            modelId = null
        )

        dao.insert(filter1)
        dao.insert(filter2)

        // When
        val result = dao.getAllFilters().first()

        // Then
        assertEquals(2, result.size)
        val withModel = result.find { it.name == "FILTER_WITH_MODEL" }
        val withoutModel = result.find { it.name == "FILTER_WITHOUT_MODEL" }
        assertNotNull(withModel)
        assertNotNull(withoutModel)
        assertEquals(modelId, withModel?.modelId)
        assertNull(withoutModel?.modelId)
    }

    @Test
    fun `getFilterById should return filter when found`() = runTest {
        // Given
        val filter = FilterEntity(
            id = 0,
            name = "TEST_FILTER",
            modelId = null
        )
        val id = dao.insert(filter)

        // When
        val result = dao.getFilterById(id)

        // Then
        assertNotNull(result)
        assertEquals(id, result?.id)
        assertEquals("TEST_FILTER", result?.name)
        assertNull(result?.modelId)
    }

    @Test
    fun `getFilterById should return filter with modelId when found`() = runTest {
        // Given
        val modelId = createTestModel()
        val filter = FilterEntity(
            id = 0,
            name = "TEST_FILTER",
            modelId = modelId
        )
        val id = dao.insert(filter)

        // When
        val result = dao.getFilterById(id)

        // Then
        assertNotNull(result)
        assertEquals(id, result?.id)
        assertEquals("TEST_FILTER", result?.name)
        assertEquals(modelId, result?.modelId)
    }

    @Test
    fun `getFilterById should return null when not found`() = runTest {
        // When
        val result = dao.getFilterById(999L)

        // Then
        assertNull(result)
    }

    @Test
    fun `getFilterByName should return filter when found`() = runTest {
        // Given
        val filter = FilterEntity(
            id = 0,
            name = "UNIQUE_FILTER",
            modelId = null
        )
        val id = dao.insert(filter)

        // When
        val result = dao.getFilterByName("UNIQUE_FILTER")

        // Then
        assertNotNull(result)
        assertEquals(id, result?.id)
        assertEquals("UNIQUE_FILTER", result?.name)
    }

    @Test
    fun `getFilterByName should return filter with modelId when found`() = runTest {
        // Given
        val modelId = createTestModel()
        val filter = FilterEntity(
            id = 0,
            name = "FILTER_WITH_MODEL",
            modelId = modelId
        )
        val id = dao.insert(filter)

        // When
        val result = dao.getFilterByName("FILTER_WITH_MODEL")

        // Then
        assertNotNull(result)
        assertEquals(id, result?.id)
        assertEquals("FILTER_WITH_MODEL", result?.name)
        assertEquals(modelId, result?.modelId)
    }

    @Test
    fun `getFilterByName should return null when not found`() = runTest {
        // When
        val result = dao.getFilterByName("NONEXISTENT_FILTER")

        // Then
        assertNull(result)
    }

    @Test
    fun `getFilterByName should be case sensitive`() = runTest {
        // Given
        val filter = FilterEntity(
            id = 0,
            name = "TestFilter",
            modelId = null
        )
        dao.insert(filter)

        // When
        val result = dao.getFilterByName("testfilter")

        // Then
        assertNull(result)
    }

    @Test
    fun `insert should insert filter and return generated id`() = runTest {
        // Given
        val filter = FilterEntity(
            id = 0,
            name = "NEW_FILTER",
            modelId = null
        )

        // When
        val id = dao.insert(filter)

        // Then
        assertTrue(id > 0)
        val inserted = dao.getFilterById(id)
        assertNotNull(inserted)
        assertEquals("NEW_FILTER", inserted?.name)
        assertNull(inserted?.modelId)
    }

    @Test
    fun `insert should insert filter with modelId`() = runTest {
        // Given
        val modelId = createTestModel()
        val filter = FilterEntity(
            id = 0,
            name = "FILTER_WITH_MODEL",
            modelId = modelId
        )

        // When
        val id = dao.insert(filter)

        // Then
        assertTrue(id > 0)
        val inserted = dao.getFilterById(id)
        assertNotNull(inserted)
        assertEquals("FILTER_WITH_MODEL", inserted?.name)
        assertEquals(modelId, inserted?.modelId)
    }

    @Test
    fun `insert should replace existing filter when id matches`() = runTest {
        // Given
        val filter = FilterEntity(
            id = 0,
            name = "ORIGINAL_FILTER",
            modelId = null
        )
        val id = dao.insert(filter)

        val updatedFilter = FilterEntity(
            id = id,
            name = "UPDATED_FILTER",
            modelId = null
        )

        // When
        dao.insert(updatedFilter)

        // Then
        val result = dao.getFilterById(id)
        assertNotNull(result)
        assertEquals("UPDATED_FILTER", result?.name)
    }

    @Test
    fun `insertAll should insert multiple filters`() = runTest {
        // Given
        val filters = listOf(
            FilterEntity(
                id = 0,
                name = "FILTER_1",
                modelId = null
            ),
            FilterEntity(
                id = 0,
                name = "FILTER_2",
                modelId = null
            ),
            FilterEntity(
                id = 0,
                name = "FILTER_3",
                modelId = null
            )
        )

        // When
        dao.insertAll(filters)

        // Then
        val result = dao.getAllFilters().first()
        assertEquals(3, result.size)
        assertTrue(result.any { it.name == "FILTER_1" })
        assertTrue(result.any { it.name == "FILTER_2" })
        assertTrue(result.any { it.name == "FILTER_3" })
    }

    @Test
    fun `insertAll should insert filters with mixed modelId`() = runTest {
        // Given
        val modelId = createTestModel()
        val filters = listOf(
            FilterEntity(
                id = 0,
                name = "FILTER_WITH_MODEL",
                modelId = modelId
            ),
            FilterEntity(
                id = 0,
                name = "FILTER_WITHOUT_MODEL",
                modelId = null
            )
        )

        // When
        dao.insertAll(filters)

        // Then
        val result = dao.getAllFilters().first()
        assertEquals(2, result.size)
        val withModel = result.find { it.name == "FILTER_WITH_MODEL" }
        val withoutModel = result.find { it.name == "FILTER_WITHOUT_MODEL" }
        assertNotNull(withModel)
        assertNotNull(withoutModel)
        assertEquals(modelId, withModel?.modelId)
        assertNull(withoutModel?.modelId)
    }

    @Test
    fun `insertAll should replace existing filters when ids match`() = runTest {
        // Given
        val filter1 = FilterEntity(
            id = 0,
            name = "ORIGINAL_1",
            modelId = null
        )
        val filter2 = FilterEntity(
            id = 0,
            name = "ORIGINAL_2",
            modelId = null
        )
        val id1 = dao.insert(filter1)
        val id2 = dao.insert(filter2)

        val updatedFilters = listOf(
            FilterEntity(
                id = id1,
                name = "UPDATED_1",
                modelId = null
            ),
            FilterEntity(
                id = id2,
                name = "UPDATED_2",
                modelId = null
            )
        )

        // When
        dao.insertAll(updatedFilters)

        // Then
        val result = dao.getAllFilters().first()
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "UPDATED_1" })
        assertTrue(result.any { it.name == "UPDATED_2" })
        assertTrue(result.none { it.name == "ORIGINAL_1" })
        assertTrue(result.none { it.name == "ORIGINAL_2" })
    }
}

