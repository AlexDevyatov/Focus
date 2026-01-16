package com.example.neuralphotoredactor.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.neuralphotoredactor.data.local.database.AppDatabase
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
 * Unit тесты для NeuralModelDao.
 *
 * Использует in-memory базу данных Room для тестирования всех методов DAO.
 * Тестирует фильтрацию по типу модели и флагу isActive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NeuralModelDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: NeuralModelDao

    @Before
    fun setup() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            )
                .allowMainThreadQueries()
                .build()
        dao = database.neuralModelDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getAllActiveModels should return empty list when database is empty`() =
        runTest {
            // When
            val result = dao.getAllActiveModels().first()

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `getAllActiveModels should return only active models sorted by name`() =
        runTest {
            // Given
            val activeModel1 =
                NeuralModelEntity(
                    id = 0,
                    name = "Z_Model",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/model1.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )
            val activeModel2 =
                NeuralModelEntity(
                    id = 0,
                    name = "A_Model",
                    type = "SUPER_RESOLUTION",
                    version = "1.0",
                    filePath = "/path/to/model2.tflite",
                    fileSize = 2048L,
                    isActive = true,
                    compatibilityLevel = "MEDIUM",
                )
            val inactiveModel =
                NeuralModelEntity(
                    id = 0,
                    name = "Inactive_Model",
                    type = "FILTER",
                    version = "1.0",
                    filePath = "/path/to/model3.tflite",
                    fileSize = 3072L,
                    isActive = false,
                    compatibilityLevel = "LOW",
                )

            val id1 = dao.insert(activeModel1)
            val id2 = dao.insert(activeModel2)
            dao.insert(inactiveModel)

            // When
            val result = dao.getAllActiveModels().first()

            // Then
            assertEquals(2, result.size)
            // Проверяем сортировку по name ASC
            assertEquals(id2, result[0].id)
            assertEquals("A_Model", result[0].name)
            assertEquals(true, result[0].isActive)
            assertEquals(id1, result[1].id)
            assertEquals("Z_Model", result[1].name)
            assertEquals(true, result[1].isActive)
        }

    @Test
    fun `getAllActiveModels should return empty list when no active models`() =
        runTest {
            // Given
            val inactiveModel =
                NeuralModelEntity(
                    id = 0,
                    name = "Inactive_Model",
                    type = "FILTER",
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = false,
                    compatibilityLevel = "LOW",
                )
            dao.insert(inactiveModel)

            // When
            val result = dao.getAllActiveModels().first()

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `getAllModels should return all models including inactive sorted by name`() =
        runTest {
            // Given
            val activeModel =
                NeuralModelEntity(
                    id = 0,
                    name = "Z_Active",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/model1.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )
            val inactiveModel =
                NeuralModelEntity(
                    id = 0,
                    name = "A_Inactive",
                    type = "FILTER",
                    version = "1.0",
                    filePath = "/path/to/model2.tflite",
                    fileSize = 2048L,
                    isActive = false,
                    compatibilityLevel = "LOW",
                )

            val id1 = dao.insert(activeModel)
            val id2 = dao.insert(inactiveModel)

            // When
            val result = dao.getAllModels().first()

            // Then
            assertEquals(2, result.size)
            // Проверяем сортировку по name ASC
            assertEquals(id2, result[0].id)
            assertEquals("A_Inactive", result[0].name)
            assertEquals(false, result[0].isActive)
            assertEquals(id1, result[1].id)
            assertEquals("Z_Active", result[1].name)
            assertEquals(true, result[1].isActive)
        }

    @Test
    fun `getModelsByType should return empty list when no models of type`() =
        runTest {
            // When
            val result = dao.getModelsByType("STYLE_TRANSFER").first()

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `getModelsByType should return only active models of specified type`() =
        runTest {
            // Given
            val activeModel1 =
                NeuralModelEntity(
                    id = 0,
                    name = "Style_Model_1",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/model1.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )
            val activeModel2 =
                NeuralModelEntity(
                    id = 0,
                    name = "Style_Model_2",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/model2.tflite",
                    fileSize = 2048L,
                    isActive = true,
                    compatibilityLevel = "MEDIUM",
                )
            val inactiveModel =
                NeuralModelEntity(
                    id = 0,
                    name = "Inactive_Style",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/model3.tflite",
                    fileSize = 3072L,
                    isActive = false,
                    compatibilityLevel = "LOW",
                )
            val otherTypeModel =
                NeuralModelEntity(
                    id = 0,
                    name = "Super_Res",
                    type = "SUPER_RESOLUTION",
                    version = "1.0",
                    filePath = "/path/to/model4.tflite",
                    fileSize = 4096L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )

            dao.insert(activeModel1)
            dao.insert(activeModel2)
            dao.insert(inactiveModel)
            dao.insert(otherTypeModel)

            // When
            val result = dao.getModelsByType("STYLE_TRANSFER").first()

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.type == "STYLE_TRANSFER" })
            assertTrue(result.all { it.isActive })
            assertTrue(result.any { it.name == "Style_Model_1" })
            assertTrue(result.any { it.name == "Style_Model_2" })
            assertTrue(result.none { it.name == "Inactive_Style" })
            assertTrue(result.none { it.name == "Super_Res" })
        }

    @Test
    fun `getModelById should return model when found`() =
        runTest {
            // Given
            val model =
                NeuralModelEntity(
                    id = 0,
                    name = "Test_Model",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )
            val id = dao.insert(model)

            // When
            val result = dao.getModelById(id)

            // Then
            assertNotNull(result)
            assertEquals(id, result?.id)
            assertEquals("Test_Model", result?.name)
            assertEquals("STYLE_TRANSFER", result?.type)
            assertEquals("1.0", result?.version)
            assertEquals("/path/to/model.tflite", result?.filePath)
            assertEquals(1024L, result?.fileSize)
            assertEquals(true, result?.isActive)
            assertEquals("HIGH", result?.compatibilityLevel)
        }

    @Test
    fun `getModelById should return null when not found`() =
        runTest {
            // When
            val result = dao.getModelById(999L)

            // Then
            assertNull(result)
        }

    @Test
    fun `getModelByName should return model when found`() =
        runTest {
            // Given
            val model =
                NeuralModelEntity(
                    id = 0,
                    name = "Unique_Model",
                    type = "SUPER_RESOLUTION",
                    version = "2.0",
                    filePath = "/path/to/unique.tflite",
                    fileSize = 2048L,
                    isActive = true,
                    compatibilityLevel = "MEDIUM",
                )
            val id = dao.insert(model)

            // When
            val result = dao.getModelByName("Unique_Model")

            // Then
            assertNotNull(result)
            assertEquals(id, result?.id)
            assertEquals("Unique_Model", result?.name)
        }

    @Test
    fun `getModelByName should return null when not found`() =
        runTest {
            // When
            val result = dao.getModelByName("NonExistent_Model")

            // Then
            assertNull(result)
        }

    @Test
    fun `getModelByName should be case sensitive`() =
        runTest {
            // Given
            val model =
                NeuralModelEntity(
                    id = 0,
                    name = "TestModel",
                    type = "FILTER",
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "LOW",
                )
            dao.insert(model)

            // When
            val result = dao.getModelByName("testmodel")

            // Then
            assertNull(result)
        }

    @Test
    fun `insert should insert model and return generated id`() =
        runTest {
            // Given
            val model =
                NeuralModelEntity(
                    id = 0,
                    name = "New_Model",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/new.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )

            // When
            val id = dao.insert(model)

            // Then
            assertTrue(id > 0)
            val inserted = dao.getModelById(id)
            assertNotNull(inserted)
            assertEquals("New_Model", inserted?.name)
            assertEquals("STYLE_TRANSFER", inserted?.type)
            assertEquals(true, inserted?.isActive)
        }

    @Test
    fun `insert should replace existing model when id matches`() =
        runTest {
            // Given
            val model =
                NeuralModelEntity(
                    id = 0,
                    name = "Original_Model",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/original.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )
            val id = dao.insert(model)

            val updatedModel =
                NeuralModelEntity(
                    id = id,
                    name = "Updated_Model",
                    type = "SUPER_RESOLUTION",
                    version = "2.0",
                    filePath = "/path/to/updated.tflite",
                    fileSize = 2048L,
                    isActive = false,
                    compatibilityLevel = "MEDIUM",
                )

            // When
            dao.insert(updatedModel)

            // Then
            val result = dao.getModelById(id)
            assertNotNull(result)
            assertEquals("Updated_Model", result?.name)
            assertEquals("SUPER_RESOLUTION", result?.type)
            assertEquals("2.0", result?.version)
            assertEquals(false, result?.isActive)
        }

    @Test
    fun `insertAll should insert multiple models`() =
        runTest {
            // Given
            val models =
                listOf(
                    NeuralModelEntity(
                        id = 0,
                        name = "Model_1",
                        type = "STYLE_TRANSFER",
                        version = "1.0",
                        filePath = "/path/to/model1.tflite",
                        fileSize = 1024L,
                        isActive = true,
                        compatibilityLevel = "HIGH",
                    ),
                    NeuralModelEntity(
                        id = 0,
                        name = "Model_2",
                        type = "SUPER_RESOLUTION",
                        version = "1.0",
                        filePath = "/path/to/model2.tflite",
                        fileSize = 2048L,
                        isActive = true,
                        compatibilityLevel = "MEDIUM",
                    ),
                    NeuralModelEntity(
                        id = 0,
                        name = "Model_3",
                        type = "FILTER",
                        version = "1.0",
                        filePath = "/path/to/model3.tflite",
                        fileSize = 3072L,
                        isActive = false,
                        compatibilityLevel = "LOW",
                    ),
                )

            // When
            dao.insertAll(models)

            // Then
            val result = dao.getAllModels().first()
            assertEquals(3, result.size)
            assertTrue(result.any { it.name == "Model_1" })
            assertTrue(result.any { it.name == "Model_2" })
            assertTrue(result.any { it.name == "Model_3" })
        }

    @Test
    fun `update should update existing model`() =
        runTest {
            // Given
            val model =
                NeuralModelEntity(
                    id = 0,
                    name = "Original_Model",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/original.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )
            val id = dao.insert(model)
            val insertedModel = dao.getModelById(id)!!

            val updatedModel =
                insertedModel.copy(
                    name = "Updated_Model",
                    type = "SUPER_RESOLUTION",
                    version = "2.0",
                    filePath = "/path/to/updated.tflite",
                    fileSize = 2048L,
                    isActive = false,
                    compatibilityLevel = "MEDIUM",
                )

            // When
            dao.update(updatedModel)

            // Then
            val result = dao.getModelById(id)
            assertNotNull(result)
            assertEquals("Updated_Model", result?.name)
            assertEquals("SUPER_RESOLUTION", result?.type)
            assertEquals("2.0", result?.version)
            assertEquals("/path/to/updated.tflite", result?.filePath)
            assertEquals(2048L, result?.fileSize)
            assertEquals(false, result?.isActive)
            assertEquals("MEDIUM", result?.compatibilityLevel)
        }

    @Test
    fun `delete should remove model from database`() =
        runTest {
            // Given
            val model =
                NeuralModelEntity(
                    id = 0,
                    name = "Model_To_Delete",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )
            val id = dao.insert(model)
            val insertedModel = dao.getModelById(id)!!

            // When
            dao.delete(insertedModel)

            // Then
            assertNull(dao.getModelById(id))
            val allModels = dao.getAllModels().first()
            assertTrue(allModels.isEmpty())
        }

    @Test
    fun `deleteById should remove model by id`() =
        runTest {
            // Given
            val model1 =
                NeuralModelEntity(
                    id = 0,
                    name = "Model_1",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/model1.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )
            val model2 =
                NeuralModelEntity(
                    id = 0,
                    name = "Model_2",
                    type = "SUPER_RESOLUTION",
                    version = "1.0",
                    filePath = "/path/to/model2.tflite",
                    fileSize = 2048L,
                    isActive = true,
                    compatibilityLevel = "MEDIUM",
                )
            val id1 = dao.insert(model1)
            val id2 = dao.insert(model2)

            // When
            dao.deleteById(id1)

            // Then
            assertNull(dao.getModelById(id1))
            assertNotNull(dao.getModelById(id2))
            val allModels = dao.getAllModels().first()
            assertEquals(1, allModels.size)
            assertEquals(id2, allModels[0].id)
        }

    @Test
    fun `deleteById should not throw when deleting non-existent id`() =
        runTest {
            // When/Then - не должно быть исключения
            dao.deleteById(999L)
        }

    @Test
    fun `setActive should activate model`() =
        runTest {
            // Given
            val model =
                NeuralModelEntity(
                    id = 0,
                    name = "Inactive_Model",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = false,
                    compatibilityLevel = "HIGH",
                )
            val id = dao.insert(model)

            // When
            dao.setActive(id, true)

            // Then
            val result = dao.getModelById(id)
            assertNotNull(result)
            assertEquals(true, result?.isActive)
            val activeModels = dao.getAllActiveModels().first()
            assertTrue(activeModels.any { it.id == id })
        }

    @Test
    fun `setActive should deactivate model`() =
        runTest {
            // Given
            val model =
                NeuralModelEntity(
                    id = 0,
                    name = "Active_Model",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )
            val id = dao.insert(model)

            // When
            dao.setActive(id, false)

            // Then
            val result = dao.getModelById(id)
            assertNotNull(result)
            assertEquals(false, result?.isActive)
            val activeModels = dao.getAllActiveModels().first()
            assertTrue(activeModels.none { it.id == id })
        }

    @Test
    fun `setActive should not affect other fields`() =
        runTest {
            // Given
            val model =
                NeuralModelEntity(
                    id = 0,
                    name = "Test_Model",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )
            val id = dao.insert(model)

            // When
            dao.setActive(id, false)

            // Then
            val result = dao.getModelById(id)
            assertNotNull(result)
            assertEquals("Test_Model", result?.name)
            assertEquals("STYLE_TRANSFER", result?.type)
            assertEquals("1.0", result?.version)
            assertEquals("/path/to/model.tflite", result?.filePath)
            assertEquals(1024L, result?.fileSize)
            assertEquals("HIGH", result?.compatibilityLevel)
            assertEquals(false, result?.isActive)
        }
}
