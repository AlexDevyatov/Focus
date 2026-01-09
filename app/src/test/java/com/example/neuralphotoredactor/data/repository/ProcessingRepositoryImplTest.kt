package com.example.neuralphotoredactor.data.repository

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.neuralphotoredactor.data.local.dao.ProcessingHistoryDao
import com.example.neuralphotoredactor.data.local.entity.ProcessingHistoryEntity
import com.example.neuralphotoredactor.data.mapper.ProcessingHistoryMapper
import com.example.neuralphotoredactor.data.storage.ImageStorage
import com.example.neuralphotoredactor.domain.enums.EditType
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.ml.edit.ImageEditProcessor
import com.example.neuralphotoredactor.ml.filter.ImageFilterProcessor
import com.example.neuralphotoredactor.ml.interpreter.EsrganImageProcessor
import com.example.neuralphotoredactor.ml.interpreter.SplitterNetImageProcessor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import android.util.Log
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Unit тесты для ProcessingRepositoryImpl.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Используем SDK 34, так как Robolectric 4.11.1 поддерживает до SDK 34
class ProcessingRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var esrganImageProcessor: EsrganImageProcessor
    private lateinit var splitterNetImageProcessor: SplitterNetImageProcessor
    private lateinit var imageFilterProcessor: ImageFilterProcessor
    private lateinit var imageEditProcessor: ImageEditProcessor
    private lateinit var imageStorage: ImageStorage
    private lateinit var processingHistoryDao: ProcessingHistoryDao
    private lateinit var repository: ProcessingRepositoryImpl

    private val testBitmap: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    private val testUri = Uri.parse("content://test/image")

    @Before
    fun setup() {
        // Используем Robolectric для получения реального Android Context
        context = ApplicationProvider.getApplicationContext()
        contentResolver = mockk(relaxed = true)
        esrganImageProcessor = mockk(relaxed = true)
        splitterNetImageProcessor = mockk(relaxed = true)
        imageFilterProcessor = mockk(relaxed = true)
        imageEditProcessor = mockk(relaxed = true)
        imageStorage = mockk(relaxed = true)
        processingHistoryDao = mockk(relaxed = true)

        // Мокируем contentResolver для тестов
        val contextSpy = mockk<Context>(relaxed = true)
        every { contextSpy.contentResolver } returns contentResolver

        repository = ProcessingRepositoryImpl(
            context = contextSpy,
            esrganImageProcessor = esrganImageProcessor,
            splitterNetImageProcessor = splitterNetImageProcessor,
            imageFilterProcessor = imageFilterProcessor,
            imageEditProcessor = imageEditProcessor,
            imageStorage = imageStorage,
            processingHistoryDao = processingHistoryDao
        )
    }

    @Test
    fun `processImage should return null when bitmap cannot be loaded`() = runTest {
        // Given
        val imageData = ImageData(testUri)
        every { contentResolver.openInputStream(testUri) } returns null

        // When
        val result = repository.processImage(imageData, FilterType.GAUSSIAN_BLUR)

        // Then
        assertNull(result)
    }

    @Test
    fun `processImage should process image with ImageFilterProcessor for regular filters`() = runTest {
        // Given
        val imageData = ImageData(testUri)
        val processedBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val processedUri = Uri.parse("content://test/processed")
        
        // Создаем реальный InputStream с данными для Bitmap
        // Robolectric позволяет использовать реальные Android классы
        val bitmapBytes = ByteArray(100 * 100 * 4) // ARGB_8888 = 4 bytes per pixel
        val inputStream = ByteArrayInputStream(bitmapBytes)
        every { contentResolver.openInputStream(testUri) } returns inputStream
        
        // BitmapFactory.decodeStream будет работать с реальным InputStream через Robolectric
        // Но для тестов мы мокируем результат, так как decodeStream может вернуть null для пустых данных
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any<InputStream>()) } returns testBitmap
        
        every { imageFilterProcessor.applyFilter(testBitmap, FilterType.GAUSSIAN_BLUR, null, false) } returns processedBitmap
        coEvery { imageStorage.saveBitmap(any(), any()) } returns processedUri
        coEvery { processingHistoryDao.insert(any()) } returns 1L

        // When
        val result = repository.processImage(imageData, FilterType.GAUSSIAN_BLUR)

        // Then
        assertNotNull(result)
        assertEquals(processedUri, result?.processedUri)
        coVerify { imageStorage.saveBitmap(any(), any()) }
        coVerify { processingHistoryDao.insert(any()) }
    }

    @Test
    fun `processImage should return null when filter processor returns null`() = runTest {
        // Given
        val imageData = ImageData(testUri)
        val inputStream = ByteArrayInputStream(ByteArray(0))
        every { contentResolver.openInputStream(testUri) } returns inputStream
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any<InputStream>()) } returns testBitmap
        every { imageFilterProcessor.applyFilter(any(), FilterType.GAUSSIAN_BLUR, null, false) } returns null

        // When
        val result = repository.processImage(imageData, FilterType.GAUSSIAN_BLUR)

        // Then
        assertNull(result)
    }

    @Test
    fun `getProcessingHistory should return mapped results`() = runTest {
        // Given
        val entity = ProcessingHistoryEntity(
            id = 1,
            originalUri = "content://test/original",
            processedUri = "content://test/processed",
            filterType = "GAUSSIAN_BLUR",
            timestamp = System.currentTimeMillis()
        )
        every { processingHistoryDao.getAllHistory() } returns flowOf(listOf(entity))

        // When
        val result = repository.getProcessingHistory().first()

        // Then
        assertEquals(1, result.size)
        assertEquals("GAUSSIAN_BLUR", result[0].filterType)
    }

    @Test
    fun `getProcessingHistory should return empty list when no history`() = runTest {
        // Given
        every { processingHistoryDao.getAllHistory() } returns flowOf(emptyList())

        // When
        val result = repository.getProcessingHistory().first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `deleteProcessingResult should delete file and database entry`() = runTest {
        // Given
        val result = ProcessingResult(
            originalUri = Uri.parse("content://test/original"),
            processedUri = Uri.parse("content://test/processed"),
            filterType = "GAUSSIAN_BLUR",
            timestamp = System.currentTimeMillis()
        )
        val entity = ProcessingHistoryEntity(
            id = 1,
            originalUri = result.originalUri.toString(),
            processedUri = result.processedUri.toString(),
            filterType = result.filterType,
            timestamp = result.timestamp
        )
        coEvery { processingHistoryDao.findByUriAndTimestamp(any(), any()) } returns entity
        coEvery { imageStorage.deleteFile(any()) } returns Unit
        coEvery { processingHistoryDao.delete(any()) } returns Unit

        // When
        repository.deleteProcessingResult(result)

        // Then
        coVerify { imageStorage.deleteFile(result.processedUri) }
        coVerify { processingHistoryDao.findByUriAndTimestamp(any(), any()) }
        coVerify { processingHistoryDao.delete(any()) }
    }

    @Test
    fun `deleteProcessingResult should not delete database entry when not found`() = runTest {
        // Given
        val result = ProcessingResult(
            originalUri = Uri.parse("content://test/original"),
            processedUri = Uri.parse("content://test/processed"),
            filterType = "GAUSSIAN_BLUR",
            timestamp = System.currentTimeMillis()
        )
        coEvery { processingHistoryDao.findByUriAndTimestamp(any(), any()) } returns null
        coEvery { imageStorage.deleteFile(any()) } returns Unit

        // When
        repository.deleteProcessingResult(result)

        // Then
        coVerify { imageStorage.deleteFile(result.processedUri) }
        coVerify(exactly = 0) { processingHistoryDao.delete(any()) }
    }

    @Test
    fun `previewFilter should return null when bitmap is recycled`() = runTest {
        // Given
        val recycledBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        recycledBitmap.recycle()

        // When
        val result = repository.previewFilter(recycledBitmap, FilterType.GAUSSIAN_BLUR)

        // Then
        assertNull(result)
    }

    @Test
    fun `previewFilter should apply filter with ImageFilterProcessor`() = runTest {
        // Given
        val processedBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        every { imageFilterProcessor.applyFilter(testBitmap, FilterType.GAUSSIAN_BLUR, null, true) } returns processedBitmap

        // When
        val result = repository.previewFilter(testBitmap, FilterType.GAUSSIAN_BLUR)

        // Then
        assertNotNull(result)
        assertEquals(processedBitmap, result)
    }

    @Test
    fun `previewFilter should return null when processor returns null`() = runTest {
        // Given
        every { imageFilterProcessor.applyFilter(any(), FilterType.GAUSSIAN_BLUR, null, true) } returns null

        // When
        val result = repository.previewFilter(testBitmap, FilterType.GAUSSIAN_BLUR)

        // Then
        assertNull(result)
    }

    @Test
    fun `previewFilters should return original bitmap when filters list is empty`() = runTest {
        // When
        val result = repository.previewFilters(testBitmap, emptyList())

        // Then
        assertEquals(testBitmap, result)
    }

    @Test
    fun `previewFilters should return null when bitmap is recycled`() = runTest {
        // Given
        val recycledBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        recycledBitmap.recycle()

        // When
        val result = repository.previewFilters(recycledBitmap, listOf(Pair(FilterType.GAUSSIAN_BLUR, 0.5f)))

        // Then
        assertNull(result)
    }

    @Test
    fun `loadBitmapFromUri should return null when inputStream is null`() = runTest {
        // Given
        every { contentResolver.openInputStream(testUri) } returns null

        // When
        val result = repository.loadBitmapFromUri(testUri)

        // Then
        assertNull(result)
    }

    @Test
    fun `loadBitmapFromUri should return bitmap when successful`() = runTest {
        // Given
        val inputStream = ByteArrayInputStream(ByteArray(0))
        every { contentResolver.openInputStream(testUri) } returns inputStream
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any<InputStream>()) } returns testBitmap

        // When
        val result = repository.loadBitmapFromUri(testUri)

        // Then
        assertNotNull(result)
        assertEquals(testBitmap, result)
    }

    @Test
    fun `applyEdit should return null when bitmap is recycled`() = runTest {
        // Given
        val recycledBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        recycledBitmap.recycle()

        // When
        val result = repository.applyEdit(recycledBitmap, EditType.BRIGHTNESS, 0.5f)

        // Then
        assertNull(result)
    }

    @Test
    fun `applyEdit should apply edit with ImageEditProcessor`() = runTest {
        // Given
        val processedBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        every { imageEditProcessor.applyEdit(testBitmap, EditType.BRIGHTNESS, 0.5f, null) } returns processedBitmap

        // When
        val result = repository.applyEdit(testBitmap, EditType.BRIGHTNESS, 0.5f)

        // Then
        assertNotNull(result)
        assertEquals(processedBitmap, result)
    }

    @Test
    fun `saveEditedImageToGallery should save to both gallery and processed folder`() = runTest {
        // Given
        val galleryUri = Uri.parse("content://test/gallery")
        val processedUri = Uri.parse("content://test/processed")
        coEvery { imageStorage.saveBitmapToGallery(any(), any()) } returns galleryUri
        coEvery { imageStorage.saveBitmap(any(), any()) } returns processedUri
        coEvery { processingHistoryDao.insert(any()) } returns 1L

        // When
        val result = repository.saveEditedImageToGallery(testBitmap, "test.jpg")

        // Then
        assertNotNull(result)
        assertEquals(galleryUri, result)
        coVerify { imageStorage.saveBitmapToGallery(any(), any()) }
        coVerify { imageStorage.saveBitmap(any(), any()) }
        coVerify { processingHistoryDao.insert(any()) }
    }

    @Test
    fun `saveEditedImageToGallery should return processedUri when galleryUri is null`() = runTest {
        // Given
        val processedUri = Uri.parse("content://test/processed")
        coEvery { imageStorage.saveBitmapToGallery(any(), any()) } returns null
        coEvery { imageStorage.saveBitmap(any(), any()) } returns processedUri
        coEvery { processingHistoryDao.insert(any()) } returns 1L

        // When
        val result = repository.saveEditedImageToGallery(testBitmap, "test.jpg")

        // Then
        assertNotNull(result)
        assertEquals(processedUri, result)
    }

    @Test
    fun `saveEditedImageToGallery should return null when both saves fail`() = runTest {
        // Given
        coEvery { imageStorage.saveBitmapToGallery(any(), any()) } returns null
        coEvery { imageStorage.saveBitmap(any(), any()) } returns null

        // When
        val result = repository.saveEditedImageToGallery(testBitmap, "test.jpg")

        // Then
        assertNull(result)
    }

    @Test
    fun `processImageWithFilters should return null when filters list is empty`() = runTest {
        // Given
        val imageData = ImageData(testUri)

        // When
        val result = repository.processImageWithFilters(imageData, emptyList())

        // Then
        assertNull(result)
    }

    @Test
    fun `processImageWithFilters should return null when bitmap cannot be loaded`() = runTest {
        // Given
        val imageData = ImageData(testUri)
        every { contentResolver.openInputStream(testUri) } returns null

        // When
        val result = repository.processImageWithFilters(imageData, listOf(Pair(FilterType.GAUSSIAN_BLUR, 0.5f)))

        // Then
        assertNull(result)
    }
}

