package com.example.neuralphotoredactor.ml.interpreter

import android.graphics.Bitmap
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.ml.preprocessor.ImagePreprocessor
import com.example.neuralphotoredactor.ml.postprocessor.ImagePostprocessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * Процессор изображений для модели ESRGAN (Enhanced Super-Resolution Generative Adversarial Network).
 * 
 * Выполняет инференс модели ESRGAN для super resolution (увеличения разрешения изображений).
 * Применяется для фильтра UPSCALE.
 * Работает полностью оффлайн.
 * 
 * Каждый нейрофильтр имеет свой собственный процессор с соответствующим Interpreter'ом.
 * Этот процессор специфичен для модели ESRGAN и использует Interpreter, загруженный из esrgan.tflite.
 * 
 * Для других моделей создавайте отдельные процессоры (например, StyleTransferImageProcessor,
 * DenoiseImageProcessor и т.д.) с соответствующими Interpreter'ами.
 */
class ErsganImageProcessor @Inject constructor(
    private val interpreter: Interpreter?,
    private val preprocessor: ImagePreprocessor,
    private val postprocessor: ImagePostprocessor
) {
    
    companion object {
        // Увеличенный размер патча для уменьшения количества патчей и накладных расходов
        private const val PATCH_SIZE = 100
        private const val OVERLAP = 10 // Перекрытие патчей для избежания артефактов на границах
        // Увеличено количество параллельных патчей для лучшего использования многоядерности
        // Оптимальное значение зависит от количества ядер CPU (обычно 4-8)
        private const val MAX_PARALLEL_PATCHES = 8
    }
    
    // Мьютекс для синхронизации доступа к Interpreter (он не потокобезопасен)
    private val interpreterMutex = Mutex()
    
    /**
     * Обработать изображение через модель ESRGAN для увеличения разрешения по патчам 50x50.
     * 
     * Изображение разбивается на патчи размером 50x50 пикселей, каждый патч обрабатывается
     * параллельно через модель, затем результаты собираются в итоговое изображение.
     * 
     * Параллельная обработка ускоряет процесс за счет использования нескольких потоков.
     * Доступ к Interpreter синхронизируется через мьютекс, так как он не потокобезопасен.
     * 
     * @param bitmap Исходное изображение
     * @param filterType Тип фильтра (должен соответствовать ESRGAN модели)
     * @return Обработанное изображение с увеличенным разрешением или null в случае ошибки
     */
    suspend fun processImage(bitmap: Bitmap, filterType: FilterType): Bitmap? = withContext(Dispatchers.Default) {
        if (interpreter == null) {
            android.util.Log.e("ErsganImageProcessor", "ESRGAN Interpreter не инициализирован")
            return@withContext null
        }
        
        return@withContext try {
            // Получаем размеры входного и выходного тензоров модели ESRGAN
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
            
            // Вычисляем масштаб увеличения
            val scaleX = modelOutputWidth.toFloat() / modelInputWidth
            val scaleY = modelOutputHeight.toFloat() / modelInputHeight
            
            android.util.Log.d("ErsganImageProcessor", "Input shape: ${inputShape.contentToString()}, Output shape: ${outputShape.contentToString()}")
            android.util.Log.d("ErsganImageProcessor", "Original image: ${bitmap.width}x${bitmap.height}, Scale: ${scaleX}x${scaleY}")
            
            // Вычисляем размеры итогового изображения
            val outputWidth = (bitmap.width * scaleX).toInt()
            val outputHeight = (bitmap.height * scaleY).toInt()
            val outputBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            
            // Заполняем белым цветом (чтобы не было черного фона)
            outputBitmap.eraseColor(android.graphics.Color.WHITE)
            
            // Вычисляем размеры патча с учетом масштаба
            val patchInputSize = PATCH_SIZE
            val patchOutputSize = (patchInputSize * scaleX).toInt()
            val stepSize = patchInputSize - OVERLAP // Шаг между патчами
            
            // Вычисляем количество патчей
            val patchesX = ((bitmap.width - OVERLAP + stepSize - 1) / stepSize).coerceAtLeast(1)
            val patchesY = ((bitmap.height - OVERLAP + stepSize - 1) / stepSize).coerceAtLeast(1)
            
            val totalPatches = patchesX * patchesY
            android.util.Log.d("ErsganImageProcessor", "Processing ${patchesX}x${patchesY} = $totalPatches patches of ${patchInputSize}x${patchInputSize} (parallel: max $MAX_PARALLEL_PATCHES)")
            
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
                    val srcX = (patchX * stepSize).coerceAtMost(bitmap.width - patchInputSize)
                    val srcY = (patchY * stepSize).coerceAtMost(bitmap.height - patchInputSize)
                    val actualPatchWidth = (srcX + patchInputSize).coerceAtMost(bitmap.width) - srcX
                    val actualPatchHeight = (srcY + patchInputSize).coerceAtMost(bitmap.height) - srcY
                    patches.add(PatchInfo(patchX, patchY, srcX, srcY, actualPatchWidth, actualPatchHeight))
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
                            
                            // Ресайзим патч до размера модели, если нужно
                            val resizedPatch = if (patchInfo.actualPatchWidth != modelInputWidth || patchInfo.actualPatchHeight != modelInputHeight) {
                                Bitmap.createScaledBitmap(patchBitmap, modelInputWidth, modelInputHeight, true)
                            } else {
                                patchBitmap
                            }
                            
                            // Обрабатываем патч через модель (с синхронизацией через мьютекс)
                            val processedPatch = processPatch(resizedPatch, inputTensor, outputTensor, inputDataType, outputDataType)
                            
                            // Освобождаем промежуточные bitmap
                            if (resizedPatch != patchBitmap) {
                                resizedPatch.recycle()
                            }
                            if (patchBitmap != bitmap) {
                                patchBitmap.recycle()
                            }
                            
                            // Логируем успешную обработку патча (только для каждого 10-го патча или при ошибках)
                            if (processedPatch != null) {
                                synchronized(results) {
                                    processedCount++
                                    // Логируем только каждый 10-й патч или каждый 25% прогресса для уменьшения накладных расходов
                                    val progress = (processedCount * 100) / totalPatches
                                    val shouldLog = processedCount % 10 == 0 || progress % 25 == 0 || processedCount == totalPatches
                                    
                                    if (shouldLog) {
                                        android.util.Log.d("ErsganImageProcessor", 
                                            "✓ Патч [${patchInfo.patchX}, ${patchInfo.patchY}] обработан " +
                                            "($processedCount/$totalPatches, $progress%)")
                                    }
                                }
                            } else {
                                android.util.Log.e("ErsganImageProcessor", "✗ Ошибка обработки патча [${patchInfo.patchX}, ${patchInfo.patchY}]")
                            }
                            
                            ProcessedPatchResult(patchInfo, processedPatch)
                        }
                    }.awaitAll()
                }
                results.addAll(batchResults)
            }
            
            // Копируем обработанные патчи в итоговое изображение
            // Используем Canvas для эффективного копирования
            val canvas = android.graphics.Canvas(outputBitmap)
            val paint = android.graphics.Paint().apply {
                isFilterBitmap = true // Включаем фильтрацию для лучшего качества при масштабировании
                isAntiAlias = false // Отключаем антиалиасинг для скорости
            }
            
            var copiedPatches = 0
            for (result in results) {
                if (result.processedBitmap != null && !result.processedBitmap.isRecycled) {
                    val patchInfo = result.patchInfo
                    val dstX = (patchInfo.srcX * scaleX).toInt()
                    val dstY = (patchInfo.srcY * scaleY).toInt()
                    val dstWidth = (patchInfo.actualPatchWidth * scaleX).toInt().coerceAtMost(outputWidth - dstX)
                    val dstHeight = (patchInfo.actualPatchHeight * scaleY).toInt().coerceAtMost(outputHeight - dstY)
                    
                    // Проверяем валидность координат
                    if (dstX >= 0 && dstY >= 0 && dstX + dstWidth <= outputWidth && dstY + dstHeight <= outputHeight) {
                        // Ресайзим обработанный патч до нужного размера, если нужно
                        val finalPatch = if (result.processedBitmap.width != dstWidth || result.processedBitmap.height != dstHeight) {
                            Bitmap.createScaledBitmap(result.processedBitmap, dstWidth, dstHeight, true)
                        } else {
                            result.processedBitmap
                        }
                        
                        // Проверяем, что патч не пустой перед копированием
                        if (finalPatch != null && !finalPatch.isRecycled) {
                            // Копируем патч в итоговое изображение
                            canvas.drawBitmap(finalPatch, dstX.toFloat(), dstY.toFloat(), paint)
                            copiedPatches++
                            
                            // Освобождаем обработанный патч
                            if (finalPatch != result.processedBitmap) {
                                finalPatch.recycle()
                            }
                        }
                    }
                    
                    // Освобождаем исходный патч
                    if (!result.processedBitmap.isRecycled) {
                        result.processedBitmap.recycle()
                    }
                }
            }
            
            android.util.Log.d("ErsganImageProcessor", "Обработка завершена: ${outputWidth}x${outputHeight}, обработано патчей: ${results.count { it.processedBitmap != null }}/$totalPatches")
            outputBitmap
        } catch (e: Exception) {
            android.util.Log.e("ErsganImageProcessor", "Ошибка обработки через ESRGAN: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Обработать один патч через модель ESRGAN.
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
            val inputBuffer = ByteBuffer.allocateDirect(expectedBytes)
            inputBuffer.order(java.nio.ByteOrder.nativeOrder())
            
            // Получаем пиксели патча
            val pixels = IntArray(targetWidth * targetHeight)
            patchBitmap.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
            
            // Конвертируем пиксели в нужный формат данных
            when (inputDataType) {
                org.tensorflow.lite.DataType.FLOAT32 -> {
                    // Нормализуем значения в диапазон [0, 1]
                    for (pixel in pixels) {
                        val r = ((pixel shr 16) and 0xFF) / 255.0f
                        val g = ((pixel shr 8) and 0xFF) / 255.0f
                        val b = (pixel and 0xFF) / 255.0f
                        inputBuffer.putFloat(r)
                        inputBuffer.putFloat(g)
                        inputBuffer.putFloat(b)
                    }
                }
                org.tensorflow.lite.DataType.UINT8 -> {
                    // Используем значения напрямую [0, 255]
                    for (pixel in pixels) {
                        inputBuffer.put(((pixel shr 16) and 0xFF).toByte())
                        inputBuffer.put(((pixel shr 8) and 0xFF).toByte())
                        inputBuffer.put((pixel and 0xFF).toByte())
                    }
                }
                else -> {
                    android.util.Log.e("ErsganImageProcessor", "Неподдерживаемый тип данных: $inputDataType")
                    return@withContext null
                }
            }
            inputBuffer.rewind()
            
            // Создаем ByteBuffer для вывода
            val outputBuffer = ByteBuffer.allocateDirect(
                outputTensor.numBytes()
            )
            outputBuffer.order(java.nio.ByteOrder.nativeOrder())
            
            // Инференс через модель ESRGAN с синхронизацией через мьютекс
            // Interpreter не потокобезопасен, поэтому используем мьютекс
            interpreterMutex.withLock {
                interpreter?.run(inputBuffer, outputBuffer)
            }
            
            // Получаем размеры выходного изображения
            val outputShape = outputTensor.shape()
            val outputWidth = outputShape[1]
            val outputHeight = outputShape[2]
            val outputChannels = outputShape[3]
            
            android.util.Log.d("ErsganImageProcessor", 
                "Выходной тензор: ${outputShape.contentToString()}, тип: $outputDataType, каналов: $outputChannels")
            
            // Обрабатываем выходные данные вручную
            outputBuffer.rewind()
            
            // Логируем первые значения только для первого патча (для диагностики)
            // Это уменьшает накладные расходы на логирование
            val shouldLogFirstValues = false // Отключаем детальное логирование для производительности
            
            val resultBitmap = when (outputDataType) {
                org.tensorflow.lite.DataType.FLOAT32 -> {
                    // Данные в формате FLOAT32 - могут быть [0, 1] или [0, 255]
                    val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
                    val pixels = IntArray(outputWidth * outputHeight)
                    
                    // Проверяем диапазон первых значений для определения формата
                    // Читаем первые 3 значения (RGB первого пикселя) для определения формата
                    val rSample = outputBuffer.getFloat()
                    val gSample = outputBuffer.getFloat()
                    val bSample = outputBuffer.getFloat()
                    outputBuffer.rewind() // Возвращаемся в начало
                    
                    // Определяем формат по первому пикселю
                    val isNormalized = rSample >= 0f && rSample <= 1f && 
                                      gSample >= 0f && gSample <= 1f && 
                                      bSample >= 0f && bSample <= 1f
                    
                    // Оптимизация: используем прямой доступ к массиву пикселей
                    var pixelIndex = 0
                    for (y in 0 until outputHeight) {
                        for (x in 0 until outputWidth) {
                            // Читаем RGB значения
                            val r = outputBuffer.getFloat()
                            val g = outputBuffer.getFloat()
                            val b = outputBuffer.getFloat()
                            
                            // Денормализуем, если нужно (оптимизированная версия)
                            val rInt = if (isNormalized) {
                                (r * 255f + 0.5f).toInt().coerceIn(0, 255) // +0.5 для правильного округления
                            } else {
                                r.toInt().coerceIn(0, 255)
                            }
                            val gInt = if (isNormalized) {
                                (g * 255f + 0.5f).toInt().coerceIn(0, 255)
                            } else {
                                g.toInt().coerceIn(0, 255)
                            }
                            val bInt = if (isNormalized) {
                                (b * 255f + 0.5f).toInt().coerceIn(0, 255)
                            } else {
                                b.toInt().coerceIn(0, 255)
                            }
                            
                            // Создаем ARGB пиксель напрямую (быстрее чем Color.argb)
                            pixels[pixelIndex++] = (255 shl 24) or (rInt shl 16) or (gInt shl 8) or bInt
                        }
                    }
                    
                    bitmap.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
                    bitmap
                }
                org.tensorflow.lite.DataType.UINT8 -> {
                    // Данные уже в формате [0, 255]
                    val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
                    val pixels = IntArray(outputWidth * outputHeight)
                    
                    // Оптимизация: используем прямой доступ к массиву пикселей
                    var pixelIndex = 0
                    for (y in 0 until outputHeight) {
                        for (x in 0 until outputWidth) {
                            val r = outputBuffer.get().toInt() and 0xFF
                            val g = outputBuffer.get().toInt() and 0xFF
                            val b = outputBuffer.get().toInt() and 0xFF
                            
                            // Создаем ARGB пиксель напрямую (быстрее чем Color.argb)
                            pixels[pixelIndex++] = (255 shl 24) or (r shl 16) or (g shl 8) or b
                        }
                    }
                    
                    bitmap.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
                    bitmap
                }
                else -> {
                    android.util.Log.e("ErsganImageProcessor", "Неподдерживаемый тип выходных данных: $outputDataType")
                    // Fallback: используем TensorImage
                    val tensorBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType)
                    outputBuffer.rewind()
                    tensorBuffer.loadBuffer(outputBuffer)
                    
                    val outputImage = TensorImage(outputDataType)
                    outputImage.load(
                        tensorBuffer,
                        org.tensorflow.lite.support.image.ColorSpaceType.RGB
                    )
                    outputImage.bitmap
                }
            }
            
            // Проверяем результат и логируем информацию
            if (resultBitmap != null && !resultBitmap.isRecycled) {
                android.util.Log.d("ErsganImageProcessor", 
                    "Патч обработан: ${resultBitmap.width}x${resultBitmap.height}, " +
                    "конфиг: ${resultBitmap.config}, " +
                    "непрозрачный: ${resultBitmap.hasAlpha()}")
                
                // Проверяем первый пиксель для диагностики
                if (resultBitmap.width > 0 && resultBitmap.height > 0) {
                    val testPixel = resultBitmap.getPixel(0, 0)
                    val alpha = android.graphics.Color.alpha(testPixel)
                    val red = android.graphics.Color.red(testPixel)
                    val green = android.graphics.Color.green(testPixel)
                    val blue = android.graphics.Color.blue(testPixel)
                    android.util.Log.d("ErsganImageProcessor", 
                        "Первый пиксель обработанного патча: ARGB($alpha, $red, $green, $blue)")
                }
            } else {
                android.util.Log.e("ErsganImageProcessor", "Обработанный патч null или переработан")
            }
            
            resultBitmap
        } catch (e: Exception) {
            android.util.Log.e("ErsganImageProcessor", "Ошибка обработки патча: ${e.message}", e)
            null
        }
    }
}
