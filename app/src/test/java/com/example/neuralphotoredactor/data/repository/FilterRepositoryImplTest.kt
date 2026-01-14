package com.example.neuralphotoredactor.data.repository

import com.example.neuralphotoredactor.data.local.dao.FilterDao
import com.example.neuralphotoredactor.data.local.entity.FilterEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit тесты для FilterRepositoryImpl.
 */
class FilterRepositoryImplTest {

    private lateinit var dao: FilterDao
    private lateinit var repository: FilterRepositoryImpl

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repository = FilterRepositoryImpl(dao)
    }

    @Test
    fun `getFilterNameById should return filter name when found`() = runTest {
        // Given
        val filterId = 1L
        val filterName = "GAUSSIAN_BLUR"
        val filterEntity = FilterEntity(
            id = filterId,
            name = filterName,
            modelId = null
        )
        coEvery { dao.getFilterById(filterId) } returns filterEntity

        // When
        val result = repository.getFilterNameById(filterId)

        // Then
        assertEquals(filterName, result)
        coVerify { dao.getFilterById(filterId) }
    }

    @Test
    fun `getFilterNameById should return filter name when filter has modelId`() = runTest {
        // Given
        val filterId = 2L
        val filterName = "STYLE_TRANSFER"
        val modelId = 10L
        val filterEntity = FilterEntity(
            id = filterId,
            name = filterName,
            modelId = modelId
        )
        coEvery { dao.getFilterById(filterId) } returns filterEntity

        // When
        val result = repository.getFilterNameById(filterId)

        // Then
        assertEquals(filterName, result)
        coVerify { dao.getFilterById(filterId) }
    }

    @Test
    fun `getFilterNameById should return null when filter not found`() = runTest {
        // Given
        val filterId = 999L
        coEvery { dao.getFilterById(filterId) } returns null

        // When
        val result = repository.getFilterNameById(filterId)

        // Then
        assertNull(result)
        coVerify { dao.getFilterById(filterId) }
    }

    @Test
    fun `getFilterNameById should return correct name for different filter types`() = runTest {
        // Given
        val filterNames = listOf(
            "GAUSSIAN_BLUR",
            "VIGNETTE",
            "STYLE_TRANSFER",
            "SUPER_RESOLUTION",
            "DENOISING"
        )

        filterNames.forEachIndexed { index, filterName ->
            val filterId = index.toLong() + 1L
            val filterEntity = FilterEntity(
                id = filterId,
                name = filterName,
                modelId = null
            )
            coEvery { dao.getFilterById(filterId) } returns filterEntity

            // When
            val result = repository.getFilterNameById(filterId)

            // Then
            assertEquals(filterName, result)
            coVerify { dao.getFilterById(filterId) }
        }
    }
}

