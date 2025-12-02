package com.example.neuralphotoredactor.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.neuralphotoredactor.data.storage.ImageStorage
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import com.example.neuralphotoredactor.ml.interpreter.ImageProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import javax.inject.Inject

/**
 * Реализация репозитория для обработки изображений.
 */
class ProcessingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageProcessor: ImageProcessor,
    private val imageStorage: ImageStorage
) : ProcessingRepository {
    
    private val processingHistory = MutableStateFlow<List<ProcessingResult>>(emptyList())
    
    override suspend fun processImage(
        imageData: ImageData,
        filterType: FilterType
    ): ProcessingResult? = withContext(Dispatchers.IO) {
        try {
            // Загружаем Bitmap из URI
            val bitmap = loadBitmapFromUri(imageData.uri) ?: return@withContext null
            
            // Обрабатываем изображение через ML модель
            val processedBitmap = imageProcessor.processImage(bitmap, filterType)
                ?: return@withContext null
            
            // Сохраняем обработанное изображение
            val fileName = "processed_${System.currentTimeMillis()}_${filterType.name}.jpg"
            val processedUri = imageStorage.saveBitmap(processedBitmap, fileName)
                ?: return@withContext null
            
            val result = ProcessingResult(
                originalUri = imageData.uri,
                processedUri = processedUri,
                filterType = filterType.name
            )
            
            // Добавляем в историю
            val currentHistory = processingHistory.value.toMutableList()
            currentHistory.add(0, result)
            processingHistory.value = currentHistory
            
            result
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getProcessingHistory(): Flow<List<ProcessingResult>> {
        return processingHistory.asStateFlow()
    }
    
    override suspend fun deleteProcessingResult(result: ProcessingResult) = withContext(Dispatchers.IO) {
        imageStorage.deleteFile(result.processedUri)
        val currentHistory = processingHistory.value.toMutableList()
        currentHistory.remove(result)
        processingHistory.value = currentHistory
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

