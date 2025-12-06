package com.example.neuralphotoredactor.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.neuralphotoredactor.data.local.dao.ProcessingHistoryDao
import com.example.neuralphotoredactor.data.mapper.ProcessingHistoryMapper
import com.example.neuralphotoredactor.data.storage.ImageStorage
import com.example.neuralphotoredactor.domain.enums.EditType
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import com.example.neuralphotoredactor.ml.edit.ImageEditProcessor
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
    private val imageEditProcessor: ImageEditProcessor,
    private val imageStorage: ImageStorage,
    private val processingHistoryDao: ProcessingHistoryDao
) : ProcessingRepository {
    
    override suspend fun processImage(
        imageData: ImageData,
        filterType: FilterType,
        intensity: Float?
    ): ProcessingResult? = withContext(Dispatchers.IO) {
        try {
            // Загружаем Bitmap из URI
            val bitmap = loadBitmapFromUriSync(imageData.uri) ?: return@withContext null
            
            // Определяем, какой процессор использовать
            val processedBitmap = when (filterType) {
                FilterType.GAUSSIAN_BLUR,
                FilterType.NOISE_REDUCTION,
                FilterType.SHARPEN,
                FilterType.VIGNETTE,
                FilterType.GRAYSCALE,
                FilterType.SEPIA -> {
                    // Используем ImageFilterProcessor для новых фильтров
                    imageFilterProcessor.applyFilter(bitmap, filterType, intensity, isPreview = false)
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
    
    override suspend fun previewFilter(
        bitmap: Bitmap,
        filterType: FilterType,
        intensity: Float?
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            // Проверяем, что Bitmap не переработан
            if (bitmap.isRecycled) {
                android.util.Log.e("ProcessingRepository", "Bitmap переработан, невозможно применить фильтр")
                return@withContext null
            }
            
            android.util.Log.d("ProcessingRepository", "Применяем фильтр $filterType к Bitmap ${bitmap.width}x${bitmap.height}")
            
            // Определяем, какой процессор использовать
            val result = when (filterType) {
                FilterType.GAUSSIAN_BLUR,
                FilterType.NOISE_REDUCTION,
                FilterType.SHARPEN,
                FilterType.VIGNETTE,
                FilterType.GRAYSCALE,
                FilterType.SEPIA -> {
                    // Используем ImageFilterProcessor для новых фильтров
                    // isPreview = true для быстрого предпросмотра
                    imageFilterProcessor.applyFilter(bitmap, filterType, intensity, isPreview = true)
                }
                else -> {
                    // Используем ImageProcessor для ML-фильтров (требуют TFLite модели)
                    imageProcessor.processImage(bitmap, filterType)
                }
            }
            
            if (result != null) {
                android.util.Log.d("ProcessingRepository", "Фильтр применен успешно: ${result.width}x${result.height}")
            } else {
                android.util.Log.e("ProcessingRepository", "Фильтр вернул null")
            }
            
            result
        } catch (e: Exception) {
            android.util.Log.e("ProcessingRepository", "Ошибка применения фильтра: ${e.message}", e)
            null
        }
    }
    
    override suspend fun previewFilters(
        bitmap: Bitmap,
        filters: List<Pair<FilterType, Float?>>
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            if (bitmap.isRecycled) {
                android.util.Log.e("ProcessingRepository", "Bitmap переработан, невозможно применить фильтры")
                return@withContext null
            }
            
            if (filters.isEmpty()) {
                return@withContext bitmap
            }
            
            android.util.Log.d("ProcessingRepository", "Применяем ${filters.size} фильтров к Bitmap ${bitmap.width}x${bitmap.height}")
            
            // Используем оптимизированный метод для множественных фильтров
            val result = imageFilterProcessor.applyFilters(bitmap, filters, isPreview = true)
            
            if (result != null) {
                android.util.Log.d("ProcessingRepository", "Фильтры применены успешно: ${result.width}x${result.height}")
            } else {
                android.util.Log.e("ProcessingRepository", "Применение фильтров вернуло null")
            }
            
            result
        } catch (e: Exception) {
            android.util.Log.e("ProcessingRepository", "Ошибка применения фильтров: ${e.message}", e)
            null
        }
    }
    
    override suspend fun processImageWithFilters(
        imageData: ImageData,
        filters: List<Pair<FilterType, Float?>>
    ): ProcessingResult? = withContext(Dispatchers.IO) {
        var bitmap: android.graphics.Bitmap? = null
        var processedBitmap: android.graphics.Bitmap? = null
        
        try {
            if (filters.isEmpty()) {
                android.util.Log.w("ProcessingRepository", "Список фильтров пуст")
                return@withContext null
            }
            
            android.util.Log.d("ProcessingRepository", "Обработка изображения с ${filters.size} фильтрами")
            
            // Загружаем Bitmap из URI
            bitmap = loadBitmapFromUriSync(imageData.uri) ?: return@withContext null
            
            // Применяем фильтры (isPreview = false для финального результата)
            processedBitmap = imageFilterProcessor.applyFilters(bitmap, filters, isPreview = false)
                ?: return@withContext null
            
            // Освобождаем исходный bitmap после обработки
            if (bitmap != processedBitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            bitmap = null // Помечаем как освобожденный
            
            // Сохраняем обработанное изображение в отдельный файл
            val filterNames = filters.joinToString("_") { it.first.name }
            val timestamp = System.currentTimeMillis()
            val fileName = "processed_${timestamp}_${filterNames}.jpg"
            
            android.util.Log.d("ProcessingRepository", "Сохранение обработанного изображения: $fileName")
            val processedUri = imageStorage.saveBitmap(processedBitmap, fileName)
                ?: return@withContext null
            
            android.util.Log.d("ProcessingRepository", "Изображение сохранено: $processedUri")
            
            val result = ProcessingResult(
                originalUri = imageData.uri,
                processedUri = processedUri,
                filterType = filterNames // Сохраняем все примененные фильтры
            )
            
            // Сохраняем в базу данных
            val entity = ProcessingHistoryMapper.toEntity(result)
            processingHistoryDao.insert(entity)
            
            android.util.Log.d("ProcessingRepository", "Результат сохранен в базу данных")
            
            result
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("ProcessingRepository", "OutOfMemoryError при обработке: ${e.message}", e)
            // Освобождаем память при ошибке
            processedBitmap?.let { if (!it.isRecycled) it.recycle() }
            bitmap?.let { if (!it.isRecycled) it.recycle() }
            null
        } catch (e: Exception) {
            android.util.Log.e("ProcessingRepository", "Ошибка обработки с множественными фильтрами: ${e.message}", e)
            // Освобождаем память при ошибке
            processedBitmap?.let { if (!it.isRecycled) it.recycle() }
            bitmap?.let { if (!it.isRecycled) it.recycle() }
            null
        }
    }
    
    override suspend fun loadBitmapFromUri(uri: android.net.Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: run {
                    android.util.Log.e("ProcessingRepository", "Не удалось открыть InputStream для URI: $uri")
                    return@withContext null
                }
            
            inputStream.use {
                val bitmap = BitmapFactory.decodeStream(it)
                if (bitmap == null) {
                    android.util.Log.e("ProcessingRepository", "BitmapFactory.decodeStream вернул null для URI: $uri")
                } else {
                    android.util.Log.d("ProcessingRepository", "Bitmap загружен: ${bitmap.width}x${bitmap.height}")
                }
                bitmap
            }
        } catch (e: FileNotFoundException) {
            android.util.Log.e("ProcessingRepository", "Файл не найден: $uri", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("ProcessingRepository", "Ошибка загрузки Bitmap: ${e.message}", e)
            null
        }
    }
    
    private fun loadBitmapFromUriSync(uri: android.net.Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: FileNotFoundException) {
            null
        }
    }
    
    override suspend fun applyEdit(
        bitmap: android.graphics.Bitmap,
        editType: EditType,
        value: Float,
        cropRect: android.graphics.Rect?
    ): android.graphics.Bitmap? = withContext(Dispatchers.Default) {
        try {
            if (bitmap.isRecycled) {
                android.util.Log.e("ProcessingRepository", "Bitmap переработан, невозможно применить редактирование")
                return@withContext null
            }
            
            android.util.Log.d("ProcessingRepository", "Применяем редактирование $editType к Bitmap ${bitmap.width}x${bitmap.height}")
            
            val result = imageEditProcessor.applyEdit(bitmap, editType, value, cropRect)
            
            if (result != null) {
                android.util.Log.d("ProcessingRepository", "Редактирование применено успешно: ${result.width}x${result.height}")
            } else {
                android.util.Log.e("ProcessingRepository", "Редактирование вернуло null")
            }
            
            result
        } catch (e: Exception) {
            android.util.Log.e("ProcessingRepository", "Ошибка применения редактирования: ${e.message}", e)
            null
        }
    }
    
    override suspend fun saveEditedImageToGallery(
        bitmap: android.graphics.Bitmap,
        fileName: String
    ): android.net.Uri? = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val finalFileName = if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                fileName
            } else {
                "${fileName}_${timestamp}.jpg"
            }
            
            android.util.Log.d("ProcessingRepository", "Сохранение отредактированного изображения в галерею и processed: $finalFileName")
            
            // Сохраняем в галерею
            val galleryUri = imageStorage.saveBitmapToGallery(bitmap, finalFileName)
            
            // Сохраняем в папку processed
            val processedUri = imageStorage.saveBitmap(bitmap, finalFileName)
            
            if (galleryUri != null) {
                android.util.Log.d("ProcessingRepository", "Изображение сохранено в галерею: $galleryUri")
            } else {
                android.util.Log.e("ProcessingRepository", "Не удалось сохранить изображение в галерею")
            }
            
            if (processedUri != null) {
                android.util.Log.d("ProcessingRepository", "Изображение сохранено в processed: $processedUri")
                
                // Сохраняем в базу данных для истории
                val result = ProcessingResult(
                    originalUri = android.net.Uri.EMPTY, // Для отредактированных изображений оригинал может быть неизвестен
                    processedUri = processedUri,
                    filterType = "edited"
                )
                val entity = ProcessingHistoryMapper.toEntity(result)
                processingHistoryDao.insert(entity)
                android.util.Log.d("ProcessingRepository", "Результат сохранен в базу данных")
            } else {
                android.util.Log.e("ProcessingRepository", "Не удалось сохранить изображение в processed")
            }
            
            // Возвращаем URI из галереи, если он есть, иначе из processed
            galleryUri ?: processedUri
        } catch (e: Exception) {
            android.util.Log.e("ProcessingRepository", "Ошибка сохранения: ${e.message}", e)
            null
        }
    }
}

