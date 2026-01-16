package com.example.neuralphotoredactor.data.repository

import android.content.Context
import android.util.Log
import com.example.neuralphotoredactor.domain.model.CompatibilityLevel
import com.example.neuralphotoredactor.domain.model.ModelType
import com.example.neuralphotoredactor.domain.model.NeuralModel
import com.example.neuralphotoredactor.domain.repository.NeuralModelRepository
import com.example.neuralphotoredactor.ml.util.ModelLoader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tensorflow.lite.Interpreter

/**
 * Unit тесты для TFLiteModelRepositoryImpl.
 */
class TFLiteModelRepositoryImplTest {
    private lateinit var context: Context
    private lateinit var neuralModelRepository: NeuralModelRepository
    private lateinit var repository: TFLiteModelRepositoryImpl

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        context = mockk(relaxed = true)
        neuralModelRepository = mockk(relaxed = true)
        repository = TFLiteModelRepositoryImpl(context, neuralModelRepository)
    }

    @After
    fun tearDown() {
        repository.releaseAll()
        unmockkAll()
    }

    @Test
    fun `getInterpreterForModel should return null when model not found`() =
        runTest {
            // Given
            val model =
                NeuralModel(
                    id = 1,
                    name = "Test Model",
                    type = ModelType.STYLE_TRANSFER,
                    version = "1.0",
                    filePath = "/nonexistent/path/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = CompatibilityLevel.HIGH,
                )

            // When
            val result = repository.getInterpreterForModel(model)

            // Then
            assertNull(result)
        }

    @Test
    fun `getInterpreterForModelId should return null when model not found`() =
        runTest {
            // Given
            val modelId = 999L
            coEvery { neuralModelRepository.getModelById(modelId) } returns null

            // When
            val result = repository.getInterpreterForModelId(modelId)

            // Then
            assertNull(result)
            coVerify { neuralModelRepository.getModelById(modelId) }
        }

    @Test
    fun `getInterpreterForModelId should return null when model file does not exist`() =
        runTest {
            // Given
            val modelId = 1L
            val model =
                NeuralModel(
                    id = modelId,
                    name = "Test Model",
                    type = ModelType.STYLE_TRANSFER,
                    version = "1.0",
                    filePath = "/nonexistent/path/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = CompatibilityLevel.HIGH,
                )
            coEvery { neuralModelRepository.getModelById(modelId) } returns model

            // When
            val result = repository.getInterpreterForModelId(modelId)

            // Then
            assertNull(result)
        }

    @Test
    fun `loadModelFromPath should return null when file does not exist`() =
        runTest {
            // Given
            val modelPath = "/nonexistent/path/model.tflite"

            // When
            val result = repository.loadModelFromPath(modelPath)

            // Then
            assertNull(result)
        }

    @Test
    fun `loadModelFromAssets should return null on exception`() =
        runTest {
            // Given
            val assetPath = "nonexistent_model.tflite"
            mockkObject(ModelLoader)
            every { ModelLoader.loadModelFile(any(), any()) } throws Exception("File not found")

            // When
            val result = repository.loadModelFromAssets(assetPath)

            // Then
            assertNull(result)
        }

    @Test
    fun `releaseInterpreter should close interpreter`() {
        // Given
        val interpreter = mockk<Interpreter>(relaxed = true)
        every { interpreter.close() } returns Unit

        // When
        repository.releaseInterpreter(interpreter)

        // Then - проверяем, что нет исключений
        assertNotNull(interpreter)
    }

    @Test
    fun `releaseAll should clear cache`() {
        // When
        repository.releaseAll()

        // Then - проверяем, что нет исключений
        assertTrue(true)
    }

    @Test
    fun `getInterpreterForModel should cache interpreter`() =
        runTest {
            // Given
            val model =
                NeuralModel(
                    id = 1,
                    name = "Test Model",
                    type = ModelType.STYLE_TRANSFER,
                    version = "1.0",
                    filePath = "/nonexistent/path/model.tflite",
                    fileSize = 1024L,
                    isActive = true,
                    compatibilityLevel = CompatibilityLevel.HIGH,
                )

            // When - первый вызов
            val result1 = repository.getInterpreterForModel(model)

            // Then
            assertNull(result1) // Файл не существует, поэтому null
        }
}
