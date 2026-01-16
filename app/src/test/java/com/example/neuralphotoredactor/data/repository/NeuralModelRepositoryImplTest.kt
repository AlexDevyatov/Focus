package com.example.neuralphotoredactor.data.repository

import android.util.Log
import com.example.neuralphotoredactor.data.local.dao.NeuralModelDao
import com.example.neuralphotoredactor.data.local.entity.NeuralModelEntity
import com.example.neuralphotoredactor.domain.model.CompatibilityLevel
import com.example.neuralphotoredactor.domain.model.ModelType
import com.example.neuralphotoredactor.domain.model.NeuralModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit тесты для NeuralModelRepositoryImpl.
 */
class NeuralModelRepositoryImplTest {
    private lateinit var dao: NeuralModelDao
    private lateinit var repository: NeuralModelRepositoryImpl

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        dao = mockk(relaxed = true)
        repository = NeuralModelRepositoryImpl(dao)
    }

    @Test
    fun `getAllActiveModels should return mapped models`() =
        runTest {
            // Given
            val entity =
                NeuralModelEntity(
                    id = 1,
                    name = "Test Model",
                    type = "STYLE_TRANSFER",
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "HIGH",
                )
            every { dao.getAllActiveModels() } returns flowOf(listOf(entity))

            // When
            val result = repository.getAllActiveModels().first()

            // Then
            assertEquals(1, result.size)
            assertEquals("Test Model", result[0].name)
            assertEquals(ModelType.STYLE_TRANSFER, result[0].type)
        }

    @Test
    fun `getAllActiveModels should return empty list when no models`() =
        runTest {
            // Given
            every { dao.getAllActiveModels() } returns flowOf(emptyList())

            // When
            val result = repository.getAllActiveModels().first()

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `getAllModels should return mapped models`() =
        runTest {
            // Given
            val entity =
                NeuralModelEntity(
                    id = 1,
                    name = "Test Model",
                    type = "SUPER_RESOLUTION",
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = false,
                    compatibilityLevel = "MEDIUM",
                )
            every { dao.getAllModels() } returns flowOf(listOf(entity))

            // When
            val result = repository.getAllModels().first()

            // Then
            assertEquals(1, result.size)
            assertEquals("Test Model", result[0].name)
            assertTrue(!result[0].isActive)
        }

    @Test
    fun `getModelsByType should return mapped models`() =
        runTest {
            // Given
            val entity =
                NeuralModelEntity(
                    id = 1,
                    name = "Test Model",
                    type = "FILTER",
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "LOW",
                )
            every { dao.getModelsByType("FILTER") } returns flowOf(listOf(entity))

            // When
            val result = repository.getModelsByType(ModelType.FILTER).first()

            // Then
            assertEquals(1, result.size)
            assertEquals(ModelType.FILTER, result[0].type)
        }

    @Test
    fun `getModelById should return mapped model`() =
        runTest {
            // Given
            val modelId = 1L
            val entity =
                NeuralModelEntity(
                    id = modelId,
                    name = "Test Model",
                    type = "ENHANCEMENT",
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "UNIVERSAL",
                )
            coEvery { dao.getModelById(modelId) } returns entity

            // When
            val result = repository.getModelById(modelId)

            // Then
            assertNotNull(result)
            assertEquals(modelId, result?.id)
            assertEquals("Test Model", result?.name)
            coVerify { dao.getModelById(modelId) }
        }

    @Test
    fun `getModelById should return null when not found`() =
        runTest {
            // Given
            val modelId = 999L
            coEvery { dao.getModelById(modelId) } returns null

            // When
            val result = repository.getModelById(modelId)

            // Then
            assertNull(result)
            coVerify { dao.getModelById(modelId) }
        }

    @Test
    fun `getModelByName should return mapped model`() =
        runTest {
            // Given
            val modelName = "Test Model"
            val entity =
                NeuralModelEntity(
                    id = 1,
                    name = modelName,
                    type = "OTHER",
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = "MEDIUM",
                )
            coEvery { dao.getModelByName(modelName) } returns entity

            // When
            val result = repository.getModelByName(modelName)

            // Then
            assertNotNull(result)
            assertEquals(modelName, result?.name)
            coVerify { dao.getModelByName(modelName) }
        }

    @Test
    fun `getModelByName should return null when not found`() =
        runTest {
            // Given
            val modelName = "NonExistent"
            coEvery { dao.getModelByName(modelName) } returns null

            // When
            val result = repository.getModelByName(modelName)

            // Then
            assertNull(result)
            coVerify { dao.getModelByName(modelName) }
        }

    @Test
    fun `addModel should insert and return id`() =
        runTest {
            // Given
            val model =
                NeuralModel(
                    id = 0,
                    name = "New Model",
                    type = ModelType.STYLE_TRANSFER,
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 2048L,
                    isActive = true,
                    compatibilityLevel = CompatibilityLevel.HIGH,
                )
            val expectedId = 1L
            coEvery { dao.insert(any()) } returns expectedId

            // When
            val result = repository.addModel(model)

            // Then
            assertEquals(expectedId, result)
            coVerify { dao.insert(any()) }
        }

    @Test
    fun `updateModel should update in dao`() =
        runTest {
            // Given
            val model =
                NeuralModel(
                    id = 1,
                    name = "Updated Model",
                    type = ModelType.SUPER_RESOLUTION,
                    version = "2.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 2048L,
                    isActive = true,
                    compatibilityLevel = CompatibilityLevel.MEDIUM,
                )
            coEvery { dao.update(any()) } returns Unit

            // When
            repository.updateModel(model)

            // Then
            coVerify { dao.update(any()) }
        }

    @Test
    fun `deleteModel should delete from dao`() =
        runTest {
            // Given
            val model =
                NeuralModel(
                    id = 1,
                    name = "Model to Delete",
                    type = ModelType.FILTER,
                    version = "1.0",
                    filePath = "/path/to/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = CompatibilityLevel.LOW,
                )
            coEvery { dao.delete(any()) } returns Unit

            // When
            repository.deleteModel(model)

            // Then
            coVerify { dao.delete(any()) }
        }

    @Test
    fun `setModelActive should update active status`() =
        runTest {
            // Given
            val modelId = 1L
            val isActive = true
            coEvery { dao.setActive(modelId, isActive) } returns Unit

            // When
            repository.setModelActive(modelId, isActive)

            // Then
            coVerify { dao.setActive(modelId, isActive) }
        }
}
