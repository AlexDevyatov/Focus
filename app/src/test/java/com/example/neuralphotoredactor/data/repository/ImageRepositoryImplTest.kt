package com.example.neuralphotoredactor.data.repository

import com.example.neuralphotoredactor.data.datasource.GalleryDataSource
import com.example.neuralphotoredactor.domain.model.ImageData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import android.util.Log
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit тесты для ImageRepositoryImpl.
 * 
 * Мокирует DataSource на уровне выше, чтобы избежать проблем с Cursor и MediaStore.
 * Robolectric нужен для работы с android.net.Uri и другими Android классами.
 * Это чище и быстрее, чем мокировать Android-специфичные классы.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Используем SDK 34, так как Robolectric 4.11.1 поддерживает до SDK 34
class ImageRepositoryImplTest {

    private lateinit var galleryDataSource: GalleryDataSource
    private lateinit var repository: ImageRepositoryImpl

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        galleryDataSource = mockk(relaxed = true)
        repository = ImageRepositoryImpl(galleryDataSource)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllImages should emit images from galleryDataSource`() = runTest(StandardTestDispatcher()) {
        // Given
        val expectedImages = listOf(
            ImageData(android.net.Uri.parse("content://test/image1")),
            ImageData(android.net.Uri.parse("content://test/image2"))
        )
        coEvery { galleryDataSource.getAllImages() } returns expectedImages

        // When
        val flow = repository.getAllImages()
        advanceUntilIdle() // Даем Flow время на выполнение
        val result = flow.first()

        // Then
        assertEquals(expectedImages, result)
        coVerify { galleryDataSource.getAllImages() }
    }

    @Test
    fun `getAllImages should emit empty list when galleryDataSource returns empty list`() = runTest {
        // Given
        coEvery { galleryDataSource.getAllImages() } returns emptyList()

        // When
        val result = repository.getAllImages().first()

        // Then
        assertTrue(result.isEmpty())
        coVerify { galleryDataSource.getAllImages() }
    }

    @Test
    fun `invalidateCache should delegate to galleryDataSource`() = runTest {
        // Given
        coEvery { galleryDataSource.invalidateCache() } returns Unit

        // When
        repository.invalidateCache()

        // Then
        coVerify { galleryDataSource.invalidateCache() }
    }
}

