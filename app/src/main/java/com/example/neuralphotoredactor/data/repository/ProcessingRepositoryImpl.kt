package com.example.neuralphotoredactor.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.neuralphotoredactor.data.local.dao.FilterDao
import com.example.neuralphotoredactor.data.local.dao.ProcessingHistoryDao
import com.example.neuralphotoredactor.data.local.database.AppDatabase
import com.example.neuralphotoredactor.data.mapper.ProcessingHistoryMapper
import com.example.neuralphotoredactor.data.mapper.ProcessingOperationMapper
import com.example.neuralphotoredactor.data.storage.ImageStorage
import com.example.neuralphotoredactor.domain.enums.EditType
import com.example.neuralphotoredactor.domain.enums.FilterType
import kotlin.collections.buildList
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.OperationParameters
import com.example.neuralphotoredactor.domain.model.ProcessingOperation
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.repository.NeuralModelRepository
import com.example.neuralphotoredactor.domain.repository.ProcessingOperationRepository
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import com.example.neuralphotoredactor.ml.edit.ImageEditProcessor
import com.example.neuralphotoredactor.ml.filter.ImageFilterProcessor
import com.example.neuralphotoredactor.ml.interpreter.AnimeGan2ImageProcessor
import com.example.neuralphotoredactor.ml.interpreter.AnimeGanFacePaintProcessor
import com.example.neuralphotoredactor.ml.interpreter.CelebADistillProcessor
import com.example.neuralphotoredactor.ml.interpreter.EsrganImageProcessor
import com.example.neuralphotoredactor.ml.interpreter.HayaoProcessor
import com.example.neuralphotoredactor.ml.interpreter.SplitterNetImageProcessor
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
    private val esrganImageProcessor: EsrganImageProcessor,
    private val splitterNetImageProcessor: SplitterNetImageProcessor,
    private val animeGan2ImageProcessor: AnimeGan2ImageProcessor,
    private val animeGanFacePaintProcessor: AnimeGanFacePaintProcessor,
    private val celebaDistillProcessor: CelebADistillProcessor,
    private val hayaoProcessor: HayaoProcessor,
    private val imageFilterProcessor: ImageFilterProcessor,
    private val imageEditProcessor: ImageEditProcessor,
    private val imageStorage: ImageStorage,
    private val processingHistoryDao: ProcessingHistoryDao,
    private val processingOperationRepository: ProcessingOperationRepository,
    private val neuralModelRepository: NeuralModelRepository,
    private val filterDao: FilterDao,
    private val appDatabase: AppDatabase
) : ProcessingRepository {
    
    override suspend fun processImage(
        imageData: ImageData,
        filterType: FilterType,
        intensity: Float?
    ): ProcessingResult? = withContext(Dispatchers.IO) {
        var processedBitmap: Bitmap? = null
        try {
            val startTime = System.currentTimeMillis()
            
            // Загружаем Bitmap из URI
            val bitmap = loadBitmapFromUriSync(imageData.uri) ?: return@withContext null
            
            // Определяем, какой процессор использовать
            processedBitmap = when (filterType) {
                FilterType.GAUSSIAN_BLUR,
                FilterType.NOISE_REDUCTION,
                FilterType.SHARPEN,
                FilterType.VIGNETTE,
                FilterType.GRAYSCALE,
                FilterType.SEPIA -> {
                    // Используем ImageFilterProcessor для новых фильтров
                    imageFilterProcessor.applyFilter(bitmap, filterType, intensity, isPreview = false)
                }
                FilterType.DENOISE -> {
                    // Используем SplitterNetImageProcessor для удаления шумов
                    splitterNetImageProcessor.processImage(bitmap, filterType)
                }
                FilterType.STYLE_TRANSFER -> {
                    // Используем модель стилизации
                    // Имя модели может быть передано через query параметр в URI (modelName=...)
                    val modelName = imageData.uri.getQueryParameter("modelName")
                    processStyleTransfer(bitmap, modelName)
                }
                else -> {
                    // Используем ErsganImageProcessor для других ML-фильтров (требуют TFLite модели)
                    esrganImageProcessor.processImage(bitmap, filterType)
                }
            } ?: return@withContext null
            
            val processingTime = System.currentTimeMillis() - startTime
            val timestamp = System.currentTimeMillis()
            
            // Сохраняем обработанное изображение
            val fileName = "processed_${timestamp}_${filterType.name}.jpg"
            val processedUri = imageStorage.saveBitmap(processedBitmap, fileName)
                ?: return@withContext null
            
            // Создаем запись в истории обработки и операцию в транзакции
            val result = ProcessingResult(
                originalUri = imageData.uri,
                processedUri = processedUri,
                filterType = filterType.name, // Для обратной совместимости, но не сохраняется в БД
                timestamp = timestamp
            )
            val historyEntity = ProcessingHistoryMapper.toEntity(result)
            val sessionId = timestamp // Используем timestamp как sessionId
            val filterId = findFilterIdByName(filterType.name) 
                ?: throw IllegalStateException("Фильтр ${filterType.name} не найден в базе данных")
            val operation = ProcessingOperation(
                historyId = 0, // Будет установлен в транзакции
                sessionId = sessionId,
                filterId = filterId,
                parameters = OperationParameters(
                    filterType = filterType.name,
                    intensity = intensity ?: 1.0f
                ),
                inputImageUri = imageData.uri,
                outputImageUri = processedUri,
                processingTimeMs = processingTime,
                sequenceNumber = 1
            )
            val operationEntity = ProcessingOperationMapper.toEntity(operation)
            android.util.Log.d("ProcessingRepository", "Создана операция: filterId=${operationEntity.filterId}, historyId=${operationEntity.historyId}")
            
            // Сохраняем в транзакции
            val savedHistoryId = appDatabase.saveHistoryWithOperations(historyEntity, listOf(operationEntity))
            android.util.Log.d("ProcessingRepository", "Транзакция завершена, historyId=$savedHistoryId")
            
            result
        } catch (e: Exception) {
            android.util.Log.e("ProcessingRepository", "Ошибка обработки изображения: ${e.message}", e)
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
                FilterType.DENOISE -> {
                    // Используем SplitterNetImageProcessor для удаления шумов
                    splitterNetImageProcessor.processImage(bitmap, filterType)
                }
                FilterType.STYLE_TRANSFER -> {
                    // Используем модель стилизации (по умолчанию AnimeGAN2)
                    // Масштабируем для предпросмотра для ускорения обработки
                    val maxPreviewDimension = 512 // Меньший размер для быстрого предпросмотра стилизации
                    val scaledBitmap = if (bitmap.width > maxPreviewDimension || bitmap.height > maxPreviewDimension) {
                        val scale = minOf(
                            maxPreviewDimension.toFloat() / bitmap.width,
                            maxPreviewDimension.toFloat() / bitmap.height
                        )
                        val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
                        val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
                        android.util.Log.d("ProcessingRepository", 
                            "Масштабируем для предпросмотра стилизации: ${bitmap.width}x${bitmap.height} -> ${scaledWidth}x${scaledHeight}")
                        Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                    } else {
                        null // Не масштабируем, используем оригинал
                    }
                    val bitmapToProcess = scaledBitmap ?: bitmap
                    processStyleTransfer(bitmapToProcess).also {
                        // Освобождаем масштабированный bitmap после использования
                        if (scaledBitmap != null && scaledBitmap != bitmap) {
                            scaledBitmap.recycle()
                        }
                    }
                }
                else -> {
                    // Используем ErsganImageProcessor для других ML-фильтров (требуют TFLite модели)
                    esrganImageProcessor.processImage(bitmap, filterType)
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
    ): Bitmap? {
        // Для обратной совместимости используем модель по умолчанию
        // Имя модели должно быть передано через другой механизм (например, через ImageData в processImage)
        return previewFilters(bitmap, filters, null)
    }
    
    /**
     * Внутренний метод для предпросмотра фильтров с поддержкой имени модели.
     * 
     * @param bitmap Исходное изображение
     * @param filters Список фильтров с их интенсивностями
     * @param modelName Имя модели для STYLE_TRANSFER (опционально)
     * @return Обработанное изображение или null в случае ошибки
     */
    private suspend fun previewFilters(
        bitmap: Bitmap,
        filters: List<Pair<FilterType, Float?>>,
        modelName: String?
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
            
            // Разделяем фильтры на обычные и нейросетевые
            val regularFilters = filters.filter { (filterType, _) ->
                filterType in listOf(
                    FilterType.GAUSSIAN_BLUR,
                    FilterType.NOISE_REDUCTION,
                    FilterType.SHARPEN,
                    FilterType.VIGNETTE,
                    FilterType.GRAYSCALE,
                    FilterType.SEPIA
                )
            }
            
            val neuralFilters = filters.filter { (filterType, _) ->
                filterType in listOf(
                    FilterType.STYLE_TRANSFER,
                    FilterType.DENOISE,
                    FilterType.UPSCALE,
                    FilterType.COLOR_CORRECTION
                )
            }
            
            var workingBitmap: Bitmap = bitmap
            
            // Сначала применяем нейросетевые фильтры (без intensity)
            for ((filterType, _) in neuralFilters) {
                val processed = when (filterType) {
                    FilterType.DENOISE -> {
                        // Используем SplitterNetImageProcessor для удаления шумов
                        splitterNetImageProcessor.processImage(workingBitmap, filterType)
                    }
                    FilterType.STYLE_TRANSFER -> {
                        // Используем модель стилизации
                        // Масштабируем для предпросмотра для ускорения обработки
                        val maxPreviewDimension = 512 // Меньший размер для быстрого предпросмотра стилизации
                        val scaledBitmap = if (workingBitmap.width > maxPreviewDimension || workingBitmap.height > maxPreviewDimension) {
                            val scale = minOf(
                                maxPreviewDimension.toFloat() / workingBitmap.width,
                                maxPreviewDimension.toFloat() / workingBitmap.height
                            )
                            val scaledWidth = (workingBitmap.width * scale).toInt().coerceAtLeast(1)
                            val scaledHeight = (workingBitmap.height * scale).toInt().coerceAtLeast(1)
                            android.util.Log.d("ProcessingRepository", 
                                "Масштабируем для предпросмотра стилизации: ${workingBitmap.width}x${workingBitmap.height} -> ${scaledWidth}x${scaledHeight}")
                            Bitmap.createScaledBitmap(workingBitmap, scaledWidth, scaledHeight, true)
                        } else {
                            null // Не масштабируем, используем оригинал
                        }
                        val bitmapToProcess = scaledBitmap ?: workingBitmap
                        // Используем переданное имя модели или модель по умолчанию
                        processStyleTransfer(bitmapToProcess, modelName).also {
                            // Освобождаем масштабированный bitmap после использования
                            if (scaledBitmap != null && scaledBitmap != workingBitmap) {
                                scaledBitmap.recycle()
                            }
                        }
                    }
                    else -> {
                        // Используем ErsganImageProcessor для других ML-фильтров
                        esrganImageProcessor.processImage(workingBitmap, filterType)
                    }
                }
                if (processed != null) {
                    // Освобождаем промежуточный bitmap, если он отличается от исходного
                    if (workingBitmap != bitmap && !workingBitmap.isRecycled) {
                        workingBitmap.recycle()
                    }
                    workingBitmap = processed
                } else {
                    android.util.Log.e("ProcessingRepository", "Нейросетевой фильтр $filterType вернул null")
                    // Освобождаем промежуточный bitmap при ошибке
                    if (workingBitmap != bitmap && !workingBitmap.isRecycled) {
                        workingBitmap.recycle()
                    }
                    return@withContext null
                }
            }
            
            // Затем применяем обычные фильтры (с intensity)
            val result = if (regularFilters.isNotEmpty()) {
                imageFilterProcessor.applyFilters(workingBitmap, regularFilters, isPreview = true)
            } else {
                workingBitmap
            }
            
            // Освобождаем промежуточный bitmap, если он отличается от исходного и результата
            if (result != null && workingBitmap != bitmap && workingBitmap != result && !workingBitmap.isRecycled) {
                workingBitmap.recycle()
            }
            
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
            
            // Разделяем фильтры на обычные и нейросетевые
            val regularFilters = filters.filter { (filterType, _) ->
                filterType in listOf(
                    FilterType.GAUSSIAN_BLUR,
                    FilterType.NOISE_REDUCTION,
                    FilterType.SHARPEN,
                    FilterType.VIGNETTE,
                    FilterType.GRAYSCALE,
                    FilterType.SEPIA
                )
            }
            
            val neuralFilters = filters.filter { (filterType, _) ->
                filterType in listOf(
                    FilterType.STYLE_TRANSFER,
                    FilterType.DENOISE,
                    FilterType.UPSCALE,
                    FilterType.COLOR_CORRECTION
                )
            }
            
            var workingBitmap: Bitmap = bitmap
            
            // Сначала применяем нейросетевые фильтры (без intensity)
            for ((filterType, _) in neuralFilters) {
                val processed = when (filterType) {
                    FilterType.DENOISE -> {
                        // Используем SplitterNetImageProcessor для удаления шумов
                        splitterNetImageProcessor.processImage(workingBitmap, filterType)
                    }
                    FilterType.STYLE_TRANSFER -> {
                        // Используем модель стилизации (по умолчанию AnimeGAN2)
                        // Для processImageWithFilters имя модели не передается, используем модель по умолчанию
                        processStyleTransfer(workingBitmap, null)
                    }
                    else -> {
                        // Используем ErsganImageProcessor для других ML-фильтров
                        esrganImageProcessor.processImage(workingBitmap, filterType)
                    }
                }
                if (processed != null) {
                    // Освобождаем промежуточный bitmap, если он отличается от исходного
                    if (workingBitmap != bitmap && !workingBitmap.isRecycled) {
                        workingBitmap.recycle()
                    }
                    workingBitmap = processed
                } else {
                    android.util.Log.e("ProcessingRepository", "Нейросетевой фильтр $filterType вернул null")
                    // Освобождаем промежуточный bitmap при ошибке
                    if (workingBitmap != bitmap && !workingBitmap.isRecycled) {
                        workingBitmap.recycle()
                    }
                    return@withContext null
                }
            }
            
            // Затем применяем обычные фильтры (с intensity, isPreview = false для финального результата)
            processedBitmap = if (regularFilters.isNotEmpty()) {
                imageFilterProcessor.applyFilters(workingBitmap, regularFilters, isPreview = false)
            } else {
                workingBitmap
            } ?: run {
                // Освобождаем промежуточный bitmap при ошибке
                if (workingBitmap != bitmap && !workingBitmap.isRecycled) {
                    workingBitmap.recycle()
                }
                return@withContext null
            }
            
            // Освобождаем промежуточный bitmap, если он отличается от исходного и результата
            if (workingBitmap != bitmap && workingBitmap != processedBitmap && !workingBitmap.isRecycled) {
                workingBitmap.recycle()
            }
            
            // Освобождаем исходный bitmap после обработки
            if (bitmap != processedBitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            bitmap = null // Помечаем как освобожденный
            
            // Сохраняем обработанное изображение в отдельный файл
            val filterNames = filters.joinToString("_") { it.first.name }
            val timestamp = System.currentTimeMillis()
            val processingStartTime = System.currentTimeMillis()
            val fileName = "processed_${timestamp}_${filterNames}.jpg"
            
            android.util.Log.d("ProcessingRepository", "Сохранение обработанного изображения: $fileName")
            val processedUri = imageStorage.saveBitmap(processedBitmap, fileName)
                ?: return@withContext null
            
            android.util.Log.d("ProcessingRepository", "Изображение сохранено: $processedUri")
            
            // Создаем запись в истории обработки и операции для каждого фильтра в транзакции
            val result = ProcessingResult(
                originalUri = imageData.uri,
                processedUri = processedUri,
                filterType = filterNames, // Для обратной совместимости, но не сохраняется в БД
                timestamp = timestamp
            )
            val historyEntity = ProcessingHistoryMapper.toEntity(result)
            val processingTime = System.currentTimeMillis() - processingStartTime
            val sessionId = timestamp
            
            // Создаем операции для каждого фильтра
            val operations = filters.mapIndexed { index, (filterType, intensity) ->
                val filterId = findFilterIdByName(filterType.name)
                    ?: throw IllegalStateException("Фильтр ${filterType.name} не найден в базе данных")
                val operation = ProcessingOperation(
                    historyId = 0, // Будет установлен в транзакции
                    sessionId = sessionId,
                    filterId = filterId,
                    parameters = OperationParameters(
                        filterType = filterType.name,
                        intensity = intensity ?: 1.0f,
                        additionalParams = emptyMap() // Каждая операция относится только к одному фильтру
                    ),
                    inputImageUri = if (index == 0) imageData.uri else processedUri,
                    outputImageUri = processedUri,
                    processingTimeMs = processingTime / filters.size, // Распределяем время между фильтрами
                    sequenceNumber = index + 1
                )
                ProcessingOperationMapper.toEntity(operation)
            }
            
            android.util.Log.d("ProcessingRepository", "Создано операций: ${operations.size}")
            if (operations.isEmpty()) {
                android.util.Log.e("ProcessingRepository", "ОШИБКА: Список операций пуст!")
            } else {
                operations.forEachIndexed { index, op ->
                    android.util.Log.d("ProcessingRepository", "Операция $index: filterId=${op.filterId}, sequenceNumber=${op.sequenceNumber}, historyId=${op.historyId}")
                }
            }
            
            // Сохраняем в транзакции
            val savedHistoryId = appDatabase.saveHistoryWithOperations(historyEntity, operations)
            android.util.Log.d("ProcessingRepository", "Транзакция завершена, historyId=$savedHistoryId, результат сохранен в базу данных")
            
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
    
    /**
     * Найти ID фильтра или операции редактирования по имени.
     * 
     * @param name Имя фильтра (FilterType.name) или операции редактирования (EditType.name)
     * @return ID фильтра или null, если не найден
     */
    private suspend fun findFilterIdByName(name: String): Long? {
        return try {
            filterDao.getFilterByName(name)?.id
        } catch (e: Exception) {
            android.util.Log.e("ProcessingRepository", "Ошибка поиска фильтра для $name: ${e.message}", e)
            null
        }
    }
    
    /**
     * Обработать изображение через модель стилизации.
     * 
     * Выбирает процессор на основе имени модели или использует AnimeGAN2 по умолчанию.
     * 
     * @param bitmap Исходное изображение
     * @param modelName Имя модели (опционально). Поддерживаемые модели:
     *                  - "AnimeGAN2 Paprika" -> AnimeGan2ImageProcessor
     *                  - "AnimeGAN Face Paint" -> AnimeGanFacePaintProcessor
     *                  - "CelebA Distill" -> CelebADistillProcessor
     *                  - "Hayao" -> HayaoProcessor
     *                  - null или другое -> AnimeGan2ImageProcessor (по умолчанию)
     * @return Обработанное изображение или null в случае ошибки
     */
    private suspend fun processStyleTransfer(
        bitmap: Bitmap,
        modelName: String? = null
    ): Bitmap? = withContext(Dispatchers.Default) {
        when (modelName) {
            "AnimeGAN Face Paint" -> animeGanFacePaintProcessor.processImage(bitmap, FilterType.STYLE_TRANSFER)
            "CelebA Distill" -> celebaDistillProcessor.processImage(bitmap, FilterType.STYLE_TRANSFER)
            "Hayao" -> hayaoProcessor.processImage(bitmap, FilterType.STYLE_TRANSFER)
            "AnimeGAN2 Paprika", null -> animeGan2ImageProcessor.processImage(bitmap, FilterType.STYLE_TRANSFER)
            else -> {
                android.util.Log.w("ProcessingRepository", "Неизвестная модель стилизации: $modelName, используем AnimeGAN2")
                animeGan2ImageProcessor.processImage(bitmap, FilterType.STYLE_TRANSFER)
            }
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
        fileName: String,
        originalUri: android.net.Uri?,
        filterType: String?,
        editSettings: Map<String, Any>?
    ): android.net.Uri? = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val finalFileName = if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                fileName
            } else {
                "${fileName}_${timestamp}.jpg"
            }
            
            android.util.Log.d("ProcessingRepository", "Сохранение обработанного изображения в галерею и processed: $finalFileName")
            
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
                
                // Создаем запись в истории обработки
                val result = ProcessingResult(
                    originalUri = originalUri ?: android.net.Uri.EMPTY,
                    processedUri = processedUri,
                    filterType = filterType ?: "edited", // Для обратной совместимости, но не сохраняется в БД
                    timestamp = timestamp
                )
                val historyEntity = ProcessingHistoryMapper.toEntity(result)
                val sessionId = timestamp
                
                // Создаем операции обработки
                val operations = buildList<ProcessingOperation> {
                    if (filterType == null || filterType == "edited") {
                        // Если это редактирование без фильтров, создаем операции для каждого примененного редактирования
                        val editSettingsMap = editSettings ?: emptyMap()
                        
                        // Извлекаем примененные редактирования из editSettings
                        // editSettings может содержать ключи типа "brightness", "contrast", "appliedEdits" и т.д.
                        @Suppress("UNCHECKED_CAST")
                        val appliedEdits = (editSettingsMap["appliedEdits"] as? List<*>) ?: emptyList<Any>()
                        
                        if (appliedEdits.isNotEmpty()) {
                            // Создаем операцию для каждого редактирования
                            appliedEdits.forEachIndexed { index, editItem ->
                                val editPair = editItem as? Pair<*, *>
                                if (editPair != null) {
                                    // editPair.first может быть String (имя EditType) или EditType
                                    val editTypeName = when (val first = editPair.first) {
                                        is EditType -> first.name
                                        is String -> first
                                        else -> null
                                    }
                                    if (editTypeName != null) {
                                        val filterId = findFilterIdByName(editTypeName)
                                        if (filterId != null) {
                                            add(
                                                ProcessingOperation(
                                                    historyId = 0, // Будет установлен в транзакции
                                                    sessionId = sessionId,
                                                    filterId = filterId,
                                                    parameters = OperationParameters(
                                                        filterType = null,
                                                        intensity = (editPair.second as? Float) ?: 0f,
                                                        additionalParams = editSettingsMap
                                                    ),
                                                    inputImageUri = if (index == 0) (originalUri ?: android.net.Uri.EMPTY) else processedUri,
                                                    outputImageUri = processedUri,
                                                    processingTimeMs = 0L,
                                                    sequenceNumber = index + 1
                                                )
                                            )
                                        } else {
                                            android.util.Log.w("ProcessingRepository", "Фильтр не найден для операции редактирования: $editTypeName")
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Всегда проверяем отдельные настройки (brightness, contrast и т.д.), даже если appliedEdits не пуст
                        // Это нужно для случаев, когда настройки передаются отдельно
                        var sequenceNum = if (appliedEdits.isNotEmpty()) appliedEdits.size + 1 else 1
                        listOf("brightness", "contrast", "colorBalanceRed", "colorBalanceGreen", "colorBalanceBlue").forEach { settingKey ->
                            if (editSettingsMap.containsKey(settingKey)) {
                                val intensity = (editSettingsMap[settingKey] as? Float) ?: 0f
                                
                                // Пропускаем операции с нулевой интенсивностью (не были изменены)
                                if (intensity == 0f) {
                                    return@forEach
                                }
                                
                                val editTypeName = when (settingKey) {
                                    "brightness" -> EditType.BRIGHTNESS.name
                                    "contrast" -> EditType.CONTRAST.name
                                    "colorBalanceRed" -> EditType.COLOR_BALANCE_RED.name
                                    "colorBalanceGreen" -> EditType.COLOR_BALANCE_GREEN.name
                                    "colorBalanceBlue" -> EditType.COLOR_BALANCE_BLUE.name
                                    else -> null
                                }
                                if (editTypeName != null) {
                                    val filterId = findFilterIdByName(editTypeName)
                                    if (filterId != null) {
                                        add(
                                            ProcessingOperation(
                                                historyId = 0,
                                                sessionId = sessionId,
                                                filterId = filterId,
                                                parameters = OperationParameters(
                                                    filterType = null,
                                                    intensity = intensity,
                                                    additionalParams = editSettingsMap
                                                ),
                                                inputImageUri = if (sequenceNum == 1) (originalUri ?: android.net.Uri.EMPTY) else processedUri,
                                                outputImageUri = processedUri,
                                                processingTimeMs = 0L,
                                                sequenceNumber = sequenceNum++
                                            )
                                        )
                                    } else {
                                        android.util.Log.w("ProcessingRepository", "Фильтр не найден для настройки: $editTypeName")
                                    }
                                }
                            }
                        }
                        
                        if (isEmpty()) {
                            // Если нет appliedEdits, но есть отдельные настройки (brightness, contrast и т.д.)
                            // Создаем операции для каждой настройки
                            var sequenceNum = 1
                            listOf("brightness", "contrast", "colorBalanceRed", "colorBalanceGreen", "colorBalanceBlue").forEach { settingKey ->
                                if (editSettingsMap.containsKey(settingKey)) {
                                    val editTypeName = when (settingKey) {
                                        "brightness" -> EditType.BRIGHTNESS.name
                                        "contrast" -> EditType.CONTRAST.name
                                        "colorBalanceRed" -> EditType.COLOR_BALANCE_RED.name
                                        "colorBalanceGreen" -> EditType.COLOR_BALANCE_GREEN.name
                                        "colorBalanceBlue" -> EditType.COLOR_BALANCE_BLUE.name
                                        else -> null
                                    }
                                    if (editTypeName != null) {
                                        val filterId = findFilterIdByName(editTypeName)
                                        if (filterId != null) {
                                            add(
                                                ProcessingOperation(
                                                    historyId = 0,
                                                    sessionId = sessionId,
                                                    filterId = filterId,
                                                    parameters = OperationParameters(
                                                        filterType = null,
                                                        intensity = (editSettingsMap[settingKey] as? Float) ?: 0f,
                                                        additionalParams = editSettingsMap
                                                    ),
                                                    inputImageUri = if (sequenceNum == 1) (originalUri ?: android.net.Uri.EMPTY) else processedUri,
                                                    outputImageUri = processedUri,
                                                    processingTimeMs = 0L,
                                                    sequenceNumber = sequenceNum++
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Если filterType содержит несколько фильтров (разделенных подчеркиванием),
                        // создаем отдельную операцию для каждого фильтра
                        val filterNames = filterType.split("_")
                        filterNames.forEachIndexed { index, filterName ->
                            // Пробуем найти как FilterType
                            val filterId = try {
                                val ft = FilterType.valueOf(filterName)
                                findFilterIdByName(ft.name)
                            } catch (e: IllegalArgumentException) {
                                // Если не FilterType, пробуем как EditType
                                try {
                                    val et = EditType.valueOf(filterName)
                                    findFilterIdByName(et.name)
                                } catch (e2: IllegalArgumentException) {
                                    android.util.Log.w("ProcessingRepository", "Неизвестный тип: $filterName")
                                    null
                                }
                            }
                            
                            if (filterId != null) {
                                add(
                                    ProcessingOperation(
                                        historyId = 0, // Будет установлен в транзакции
                                        sessionId = sessionId,
                                        filterId = filterId,
                                        parameters = OperationParameters(
                                            filterType = filterName,
                                            intensity = 1.0f,
                                            additionalParams = editSettings ?: emptyMap()
                                        ),
                                        inputImageUri = if (index == 0) (originalUri ?: android.net.Uri.EMPTY) else processedUri,
                                        outputImageUri = processedUri,
                                        processingTimeMs = 0L,
                                        sequenceNumber = index + 1
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Сохраняем в транзакции
                val operationEntities = operations.map { ProcessingOperationMapper.toEntity(it) }
                android.util.Log.d("ProcessingRepository", "Создано операций для сохранения: ${operationEntities.size}")
                if (operationEntities.isEmpty()) {
                    android.util.Log.e("ProcessingRepository", "ОШИБКА: Список операций пуст!")
                } else {
                    operationEntities.forEachIndexed { index, op ->
                        android.util.Log.d("ProcessingRepository", "Операция $index: filterId=${op.filterId}, sequenceNumber=${op.sequenceNumber}")
                    }
                }
                val savedHistoryId = appDatabase.saveHistoryWithOperations(historyEntity, operationEntities)
                android.util.Log.d("ProcessingRepository", "Транзакция завершена, historyId=$savedHistoryId, результат сохранен в базу данных")
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

