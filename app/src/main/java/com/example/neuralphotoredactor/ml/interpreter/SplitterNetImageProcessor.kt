package com.example.neuralphotoredactor.ml.interpreter

import android.graphics.Bitmap
import com.example.neuralphotoredactor.domain.enums.FilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import kotlin.math.PI
import kotlin.math.cos
import javax.inject.Inject

/**
 * Процессор изображений для модели SplitterNet (удаление шумов).
 * 
 * Выполняет инференс модели SplitterNet для удаления шумов с изображений.
 * Применяется для фильтра DENOISE.
 * Работает полностью оффлайн.
 * 
 * Обрабатывает изображение любого размера, разбивая его на патчи 256x256 с перекрытием,
 * обрабатывает каждый патч через модель splitternet_midd_model.tflite,
 * и собирает результат обратно в исходный размер с использованием взвешенного усреднения.
 * 
 * Размер и разрешение исходного изображения не изменяются.
 */
class SplitterNetImageProcessor @Inject constructor(
    private val interpreter: Interpreter?
) {
    
    companion object {
        private const val PATCH_SIZE = 256
        private const val STRIDE = 224 // Перекрытие 32 пикселя (256 - 224)
        private const val OVERLAP = 32 // 256 - 224
        // Количество параллельных патчей для обработки
        private const val MAX_PARALLEL_PATCHES = 8
    }
    
    // Мьютекс для синхронизации доступа к Interpreter (он не потокобезопасен)
    private val interpreterMutex = Mutex()
    
    /**
     * Обработать изображение через модель SplitterNet для удаления шумов по патчам 256x256.
     * 
     * Изображение разбивается на патчи размером 256x256 пикселей, каждый патч обрабатывается
     * параллельно через модель, затем результаты собираются в итоговое изображение того же размера.
     * 
     * Параллельная обработка ускоряет процесс за счет использования нескольких потоков.
     * Доступ к Interpreter синхронизируется через мьютекс, так как он не потокобезопасен.
     * 
     * @param bitmap Исходное изображение
     * @param filterType Тип фильтра (должен быть FilterType.DENOISE)
     * @return Обработанное изображение того же размера или null в случае ошибки
     */
    suspend fun processImage(bitmap: Bitmap, filterType: FilterType): Bitmap? = withContext(Dispatchers.Default) {
        if (interpreter == null) {
            android.util.Log.e("SplitterNetImageProcessor", "SplitterNet Interpreter не инициализирован")
            return@withContext null
        }
        
        // Проверка совместимости с архитектурой SplitterNet
        if (PATCH_SIZE % 16 != 0) {
            android.util.Log.e("SplitterNetImageProcessor", "Размер патча должен быть кратен 16 для SplitterNet. Текущий размер: $PATCH_SIZE")
            return@withContext null
        }
        
        return@withContext try {
            // Получаем размеры входного и выходного тензоров модели SplitterNet
            val inputTensor = interpreter.getInputTensor(0)
            val inputShape = inputTensor.shape()
            val inputDataType = inputTensor.dataType()
            val modelInputWidth = inputShape[1]
            val modelInputHeight = inputShape[2]
            
            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            val outputDataType = outputTensor.dataType()
            val modelOutputWidth = outputShape[1]
            val modelOutputHeight = outputShape[2]
            
            // Диагностика формата модели
            android.util.Log.d("MODEL_DIAGNOSTICS", 
                "SplitterNet model data types - Input: $inputDataType, Output: $outputDataType")
            android.util.Log.d("MODEL_DIAGNOSTICS", 
                "SplitterNet model shapes - Input: ${inputShape.contentToString()}, Output: ${outputShape.contentToString()}")
            
            // Для удаления шумов размер входного и выходного изображения должен быть одинаковым
            if (modelInputWidth != modelOutputWidth || modelInputHeight != modelOutputHeight) {
                android.util.Log.w("SplitterNetImageProcessor", 
                    "Модель изменяет размер: вход ${modelInputWidth}x${modelInputHeight}, выход ${modelOutputWidth}x${modelOutputHeight}")
            }
            
            android.util.Log.d("SplitterNetImageProcessor", "Input shape: ${inputShape.contentToString()}, Output shape: ${outputShape.contentToString()}")
            android.util.Log.d("SplitterNetImageProcessor", "Original image: ${bitmap.width}x${bitmap.height}")
            android.util.Log.d("SplitterNetImageProcessor", "Patch size: ${PATCH_SIZE}x${PATCH_SIZE}, Stride: $STRIDE, Overlap: $OVERLAP")
            
            // Создаем итоговое изображение того же размера, что и исходное
            val outputWidth = bitmap.width
            val outputHeight = bitmap.height
            
            // Вычисляем количество патчей
            val patchesX = ((outputWidth - OVERLAP + STRIDE - 1) / STRIDE).coerceAtLeast(1)
            val patchesY = ((outputHeight - OVERLAP + STRIDE - 1) / STRIDE).coerceAtLeast(1)
            
            val totalPatches = patchesX * patchesY
            android.util.Log.d("SplitterNetImageProcessor", "Processing ${patchesX}x${patchesY} = $totalPatches patches of ${PATCH_SIZE}x${PATCH_SIZE} (parallel: max $MAX_PARALLEL_PATCHES)")
            
            // Создаем список всех патчей для обработки
            data class PatchInfo(
                val patchX: Int,
                val patchY: Int,
                val srcX: Int,
                val srcY: Int,
                val actualPatchWidth: Int,
                val actualPatchHeight: Int
            )
            
            val patches = mutableListOf<PatchInfo>()
            for (patchY in 0 until patchesY) {
                for (patchX in 0 until patchesX) {
                    // Вычисляем координаты патча
                    val yStart = patchY * STRIDE
                    val yEnd = (yStart + PATCH_SIZE).coerceAtMost(outputHeight)
                    val xStart = patchX * STRIDE
                    val xEnd = (xStart + PATCH_SIZE).coerceAtMost(outputWidth)
                    
                    val actualPatchWidth = xEnd - xStart
                    val actualPatchHeight = yEnd - yStart
                    patches.add(PatchInfo(patchX, patchY, xStart, yStart, actualPatchWidth, actualPatchHeight))
                }
            }
            
            // Обрабатываем патчи параллельно батчами
            data class ProcessedPatchResult(
                val patchInfo: PatchInfo,
                val processedBitmap: Bitmap?
            )
            
            val results = mutableListOf<ProcessedPatchResult>()
            var processedCount = 0
            
            // Разбиваем патчи на батчи для параллельной обработки
            patches.chunked(MAX_PARALLEL_PATCHES).forEach { batch ->
                val batchResults = coroutineScope {
                    batch.map { patchInfo ->
                        async {
                            // Извлекаем патч из исходного изображения
                            val patchBitmap = Bitmap.createBitmap(
                                bitmap, 
                                patchInfo.srcX, 
                                patchInfo.srcY, 
                                patchInfo.actualPatchWidth, 
                                patchInfo.actualPatchHeight
                            )
                            
                            // Если патч меньше требуемого размера, добавляем паддинг
                            val resizedPatch = if (patchInfo.actualPatchWidth < PATCH_SIZE || patchInfo.actualPatchHeight < PATCH_SIZE) {
                                // Создаем патч нужного размера с нулевым паддингом
                                val paddedPatch = Bitmap.createBitmap(PATCH_SIZE, PATCH_SIZE, Bitmap.Config.ARGB_8888)
                                paddedPatch.eraseColor(android.graphics.Color.BLACK)
                                val canvas = android.graphics.Canvas(paddedPatch)
                                canvas.drawBitmap(patchBitmap, 0f, 0f, null)
                                patchBitmap.recycle()
                                paddedPatch
                            } else {
                                patchBitmap
                            }
                            
                            // Обрабатываем патч через модель (с синхронизацией через мьютекс)
                            val processedPatch = processPatch(resizedPatch, inputTensor, outputTensor, inputDataType, outputDataType)
                            
                            // Обрезаем до фактического размера патча
                            val finalPatch = if (processedPatch != null && 
                                (processedPatch.width != patchInfo.actualPatchWidth || processedPatch.height != patchInfo.actualPatchHeight)) {
                                Bitmap.createBitmap(processedPatch, 0, 0, patchInfo.actualPatchWidth, patchInfo.actualPatchHeight).also {
                                    processedPatch.recycle()
                                }
                            } else {
                                processedPatch
                            }
                            
                            // Освобождаем промежуточные bitmap
                            if (resizedPatch != patchBitmap && resizedPatch != processedPatch) {
                                resizedPatch.recycle()
                            }
                            
                            // Логируем успешную обработку патча
                            if (finalPatch != null) {
                                synchronized(results) {
                                    processedCount++
                                    // Логируем только каждый 10-й патч или каждый 25% прогресса
                                    val progress = (processedCount * 100) / totalPatches
                                    val shouldLog = processedCount % 10 == 0 || progress % 25 == 0 || processedCount == totalPatches
                                    
                                    if (shouldLog) {
                                        android.util.Log.d("SplitterNetImageProcessor", 
                                            "✓ Патч [${patchInfo.patchX}, ${patchInfo.patchY}] обработан " +
                                            "($processedCount/$totalPatches, $progress%)")
                                    }
                                }
                            } else {
                                android.util.Log.e("SplitterNetImageProcessor", "✗ Ошибка обработки патча [${patchInfo.patchX}, ${patchInfo.patchY}]")
                            }
                            
                            ProcessedPatchResult(patchInfo, finalPatch)
                        }
                    }.awaitAll()
                }
                results.addAll(batchResults)
            }
            
            // Создаем выходные массивы для аккумуляции результата
            // result - накопленный результат в формате [0, 1]
            // count - счетчик весов для усреднения
            val resultR = FloatArray(outputWidth * outputHeight) { 0f }
            val resultG = FloatArray(outputWidth * outputHeight) { 0f }
            val resultB = FloatArray(outputWidth * outputHeight) { 0f }
            val count = FloatArray(outputWidth * outputHeight) { 0f }
            
            // Обрабатываем каждый патч и аккумулируем результат
            for (result in results) {
                if (result.processedBitmap != null && !result.processedBitmap.isRecycled) {
                    val patchInfo = result.patchInfo
                    val xStart = patchInfo.srcX
                    val yStart = patchInfo.srcY
                    val patchWidth = patchInfo.actualPatchWidth
                    val patchHeight = patchInfo.actualPatchHeight
                    
                    // Получаем пиксели патча
                    val patchPixels = IntArray(patchWidth * patchHeight)
                    result.processedBitmap.getPixels(patchPixels, 0, patchWidth, 0, 0, patchWidth, patchHeight)
                    
                    // Вычисляем веса для гауссова взвешивания
                    // border = min(32, patch_h // 4, patch_w // 4)
                    val border = minOf(32, patchHeight / 4, patchWidth / 4)
                    val weights = Array(patchHeight) { y ->
                        FloatArray(patchWidth) { x ->
                            if (border > 0) {
                                // dist_to_border = min(y, x, patch_h - 1 - y, patch_w - 1 - x)
                                val distToBorder = minOf(y, x, patchHeight - 1 - y, patchWidth - 1 - x)
                                if (distToBorder < border) {
                                    distToBorder.toFloat() / border
                                } else {
                                    1.0f
                                }
                            } else {
                                1.0f
                            }
                        }
                    }
                    
                    // Добавляем результат в выходное изображение с весом
                    for (y in 0 until patchHeight) {
                        for (x in 0 until patchWidth) {
                            val outputX = xStart + x
                            val outputY = yStart + y
                            
                            if (outputX >= 0 && outputX < outputWidth && outputY >= 0 && outputY < outputHeight) {
                                val outputIndex = outputY * outputWidth + outputX
                                val patchIndex = y * patchWidth + x
                                val patchPixel = patchPixels[patchIndex]
                                
                                // Извлекаем компоненты пикселя и нормализуем в [0, 1]
                                val r = ((patchPixel shr 16) and 0xFF) / 255.0f
                                val g = ((patchPixel shr 8) and 0xFF) / 255.0f
                                val b = (patchPixel and 0xFF) / 255.0f
                                
                                val weight = weights[y][x]
                                
                                // Аккумулируем результат: result += denoised_patch * weight
                                resultR[outputIndex] += r * weight
                                resultG[outputIndex] += g * weight
                                resultB[outputIndex] += b * weight
                                count[outputIndex] += weight
                            }
                        }
                    }
                    
                    // Освобождаем обработанный патч
                    if (!result.processedBitmap.isRecycled) {
                        result.processedBitmap.recycle()
                    }
                }
            }
            
            // Усредняем результат
            // count = np.maximum(count, 1e-8) - избегаем деления на ноль
            val outputBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            val outputPixels = IntArray(outputWidth * outputHeight)
            
            for (y in 0 until outputHeight) {
                for (x in 0 until outputWidth) {
                    val index = y * outputWidth + x
                    val countValue = count[index].coerceAtLeast(1e-8f)
                    
                    // Если область не была обработана, используем исходное изображение
                    val r: Float
                    val g: Float
                    val b: Float
                    
                    if (countValue < 1e-7f) {
                        // Необработанная область - используем исходное изображение
                        val originalPixel = bitmap.getPixel(x, y)
                        r = ((originalPixel shr 16) and 0xFF) / 255.0f
                        g = ((originalPixel shr 8) and 0xFF) / 255.0f
                        b = (originalPixel and 0xFF) / 255.0f
                    } else {
                        // Усредняем результат
                        r = resultR[index] / countValue
                        g = resultG[index] / countValue
                        b = resultB[index] / countValue
                    }
                    
                    // Преобразуем [0, 1] → [0, 255] и создаем пиксель
                    val rInt = (r * 255.0f).coerceIn(0f, 255f).toInt()
                    val gInt = (g * 255.0f).coerceIn(0f, 255f).toInt()
                    val bInt = (b * 255.0f).coerceIn(0f, 255f).toInt()
                    
                    outputPixels[index] = (255 shl 24) or (rInt shl 16) or (gInt shl 8) or bInt
                }
            }
            
            // Применяем обработанные пиксели к итоговому изображению
            outputBitmap.setPixels(outputPixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
            
            android.util.Log.d("SplitterNetImageProcessor", "Обработка завершена: ${outputWidth}x${outputHeight}, обработано патчей: ${results.count { it.processedBitmap != null }}/$totalPatches")
            outputBitmap
        } catch (e: Exception) {
            android.util.Log.e("SplitterNetImageProcessor", "Ошибка обработки через SplitterNet: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Обработать один патч через модель SplitterNet.
     * 
     * Использует мьютекс для синхронизации доступа к Interpreter, так как он не потокобезопасен.
     * 
     * @param patchBitmap Патч изображения для обработки
     * @param inputTensor Входной тензор модели
     * @param outputTensor Выходной тензор модели
     * @param inputDataType Тип данных входного тензора
     * @param outputDataType Тип данных выходного тензора
     * @return Обработанный патч или null в случае ошибки
     */
    private suspend fun processPatch(
        patchBitmap: Bitmap,
        inputTensor: org.tensorflow.lite.Tensor,
        outputTensor: org.tensorflow.lite.Tensor,
        inputDataType: org.tensorflow.lite.DataType,
        outputDataType: org.tensorflow.lite.DataType
    ): Bitmap? = withContext(Dispatchers.Default) {
        return@withContext try {
            val inputShape = inputTensor.shape()
            val targetWidth = inputShape[1]
            val targetHeight = inputShape[2]
            
            // Создаем входной буфер
            val expectedBytes = inputTensor.numBytes()
            val inputBuffer = java.nio.ByteBuffer.allocateDirect(expectedBytes)
            inputBuffer.order(java.nio.ByteOrder.nativeOrder())
            
            // Получаем пиксели патча
            val pixels = IntArray(targetWidth * targetHeight)
            patchBitmap.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
            
            // Конвертируем пиксели в нужный формат данных
            // Модель SplitterNet ожидает RGB float32 в диапазоне [0, 1]
            when (inputDataType) {
                org.tensorflow.lite.DataType.FLOAT32 -> {
                    // Подаем RGB float32 в диапазоне [0, 1]
                    for (pixel in pixels) {
                        val r = ((pixel shr 16) and 0xFF) / 255.0f
                        val g = ((pixel shr 8) and 0xFF) / 255.0f
                        val b = (pixel and 0xFF) / 255.0f
                        
                        // Подаем RGB (модель ожидает именно RGB)
                        inputBuffer.putFloat(r)
                        inputBuffer.putFloat(g)
                        inputBuffer.putFloat(b)
                    }
                }
                org.tensorflow.lite.DataType.UINT8 -> {
                    // Используем значения напрямую [0, 255]
                    for (pixel in pixels) {
                        val r = ((pixel shr 16) and 0xFF).toByte()
                        val g = ((pixel shr 8) and 0xFF).toByte()
                        val b = (pixel and 0xFF).toByte()
                        
                        // Подаем RGB (модель ожидает именно RGB)
                        inputBuffer.put(r)
                        inputBuffer.put(g)
                        inputBuffer.put(b)
                    }
                }
                else -> {
                    android.util.Log.e("SplitterNetImageProcessor", "Неподдерживаемый тип данных: $inputDataType")
                    return@withContext null
                }
            }
            inputBuffer.rewind()
            
            // Создаем ByteBuffer для вывода
            val outputBuffer = java.nio.ByteBuffer.allocateDirect(
                outputTensor.numBytes()
            )
            outputBuffer.order(java.nio.ByteOrder.nativeOrder())
            
            // Инференс через модель SplitterNet с синхронизацией через мьютекс
            // Interpreter не потокобезопасен, поэтому используем мьютекс
            interpreterMutex.withLock {
                interpreter?.run(inputBuffer, outputBuffer)
            }
            
            // Получаем размеры выходного изображения
            val outputShape = outputTensor.shape()
            val outputWidth = outputShape[1]
            val outputHeight = outputShape[2]
            val outputChannels = outputShape[3]
            
            android.util.Log.d("SplitterNetImageProcessor", 
                "Выходной тензор: ${outputShape.contentToString()}, тип: $outputDataType, каналов: $outputChannels")
            
            // Обрабатываем выходные данные вручную
            outputBuffer.rewind()
            
            val resultBitmap = when (outputDataType) {
                org.tensorflow.lite.DataType.FLOAT32 -> {
                    // Модель возвращает RGB float32 в диапазоне [0, 1]
                    val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
                    val pixels = IntArray(outputWidth * outputHeight)
                    
                    // Оптимизация: используем прямой доступ к массиву пикселей
                    var pixelIndex = 0
                    for (y in 0 until outputHeight) {
                        for (x in 0 until outputWidth) {
                            // Читаем значения каналов RGB напрямую (в диапазоне [0, 1])
                            val rFloat = outputBuffer.getFloat().coerceIn(0f, 1f)
                            val gFloat = outputBuffer.getFloat().coerceIn(0f, 1f)
                            val bFloat = outputBuffer.getFloat().coerceIn(0f, 1f)
                            
                            // Преобразуем [0, 1] → [0, 255]
                            val r = (rFloat * 255f).toInt().coerceIn(0, 255)
                            val g = (gFloat * 255f).toInt().coerceIn(0, 255)
                            val b = (bFloat * 255f).toInt().coerceIn(0, 255)
                            
                            // Создаем ARGB пиксель напрямую
                            pixels[pixelIndex++] = (255 shl 24) or (r shl 16) or (g shl 8) or b
                        }
                    }
                    
                    bitmap.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
                    bitmap
                }
                org.tensorflow.lite.DataType.UINT8 -> {
                    // Данные уже в формате [0, 255] RGB
                    val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
                    val pixels = IntArray(outputWidth * outputHeight)
                    
                    // Оптимизация: используем прямой доступ к массиву пикселей
                    var pixelIndex = 0
                    for (y in 0 until outputHeight) {
                        for (x in 0 until outputWidth) {
                            val r = outputBuffer.get().toInt() and 0xFF
                            val g = outputBuffer.get().toInt() and 0xFF
                            val b = outputBuffer.get().toInt() and 0xFF
                            
                            // Создаем ARGB пиксель напрямую
                            pixels[pixelIndex++] = (255 shl 24) or (r shl 16) or (g shl 8) or b
                        }
                    }
                    
                    bitmap.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
                    bitmap
                }
                else -> {
                    android.util.Log.e("SplitterNetImageProcessor", "Неподдерживаемый тип выходных данных: $outputDataType")
                    null
                }
            }
            
            // Проверяем результат
            if (resultBitmap != null && !resultBitmap.isRecycled) {
                android.util.Log.d("SplitterNetImageProcessor", 
                    "Патч обработан: ${resultBitmap.width}x${resultBitmap.height}, " +
                    "конфиг: ${resultBitmap.config}")
            } else {
                android.util.Log.e("SplitterNetImageProcessor", "Обработанный патч null или переработан")
            }
            
            resultBitmap
        } catch (e: Exception) {
            android.util.Log.e("SplitterNetImageProcessor", "Ошибка обработки патча: ${e.message}", e)
            null
        }
    }
}

