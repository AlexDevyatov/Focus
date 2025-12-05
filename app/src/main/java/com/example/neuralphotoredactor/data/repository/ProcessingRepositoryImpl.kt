package com.example.neuralphotoredactor.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.neuralphotoredactor.data.local.dao.ProcessingHistoryDao
import com.example.neuralphotoredactor.data.mapper.ProcessingHistoryMapper
import com.example.neuralphotoredactor.data.storage.ImageStorage
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import com.example.neuralphotoredactor.ml.filter.ImageFilterProcessor
import com.example.neuralphotoredactor.ml.interpreter.ImageProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import javax.inject.Inject

/**
 * Реализация репозитория для обработки изображений.
 * 
 * Использует Room Database для хранения истории обработок.
 * Вся работа с БД происходит через DAO, обеспечивая изоляцию слоев.
 */
class ProcessingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageProcessor: ImageProcessor,
    private val imageFilterProcessor: ImageFilterProcessor,
    private val imageStorage: ImageStorage,
    private val processingHistoryDao: ProcessingHistoryDao
) : ProcessingRepository {
    
    override suspend fun processImage(
        imageData: ImageData,
        filterType: FilterType
    ): ProcessingResult? = withContext(Dispatchers.IO) {
        try {
            // Загружаем Bitmap из URI
            val bitmap = loadBitmapFromUri(imageData.uri) ?: return@withContext null
            
            // Определяем, какой процессор использовать
            val processedBitmap = when (filterType) {
                FilterType.GAUSSIAN_BLUR,
                FilterType.NOISE_REDUCTION,
                FilterType.SHARPEN,
                FilterType.VIGNETTE,
                FilterType.GRAYSCALE,
                FilterType.SEPIA -> {
                    // Используем ImageFilterProcessor для новых фильтров
                    imageFilterProcessor.applyFilter(bitmap, filterType)
                }
                else -> {
                    // Используем ImageProcessor для ML-фильтров (требуют TFLite модели)
                    imageProcessor.processImage(bitmap, filterType)
                }
            } ?: return@withContext null
            
            // Сохраняем обработанное изображение
            val fileName = "processed_${System.currentTimeMillis()}_${filterType.name}.jpg"
            val processedUri = imageStorage.saveBitmap(processedBitmap, fileName)
                ?: return@withContext null
            
            val result = ProcessingResult(
                originalUri = imageData.uri,
                processedUri = processedUri,
                filterType = filterType.name
            )
            
            // Сохраняем в базу данных
            val entity = ProcessingHistoryMapper.toEntity(result)
            processingHistoryDao.insert(entity)
            
            result
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getProcessingHistory(): Flow<List<ProcessingResult>> {
        return processingHistoryDao.getAllHistory()
            .map { entities ->
                ProcessingHistoryMapper.toDomainList(entities)
            }
    }
    
    override suspend fun deleteProcessingResult(result: ProcessingResult) = withContext(Dispatchers.IO) {
        // Удаляем файл изображения
        imageStorage.deleteFile(result.processedUri)
        
        // Находим и удаляем запись из БД по processedUri и timestamp
        val entityToDelete = processingHistoryDao.findByUriAndTimestamp(
            processedUri = result.processedUri.toString(),
            timestamp = result.timestamp
        )
        if (entityToDelete != null) {
            processingHistoryDao.delete(entityToDelete)
        }
    }
    
    private fun loadBitmapFromUri(uri: android.net.Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: FileNotFoundException) {
            null
        }
    }
}

