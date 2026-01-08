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
import kotlin.math.PI
import kotlin.math.cos

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
class EsrganImageProcessor @Inject constructor(
    private val interpreter: Interpreter?,
    private val preprocessor: ImagePreprocessor,
    private val postprocessor: ImagePostprocessor
) {
    
    companion object {
        private const val PATCH_SIZE = 64
        // Увеличено перекрытие для лучшего устранения артефактов на границах
        // Для 4x масштаба рекомендуется перекрытие 30-40 пикселей для плавного blending
        private const val OVERLAP = 32 // Перекрытие патчей для избежания артефактов на границах
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
        
        // Проверка совместимости с архитектурой ESRGAN
        if (PATCH_SIZE % 16 != 0) {
            android.util.Log.e("ErsganImageProcessor", "Размер патча должен быть кратен 16 для ESRGAN. Текущий размер: $PATCH_SIZE")
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
            
            // Диагностика формата модели
            android.util.Log.d("MODEL_DIAGNOSTICS", 
                "Model data types - Input: $inputDataType, Output: $outputDataType")
            android.util.Log.d("MODEL_DIAGNOSTICS", 
                "Model shapes - Input: ${inputShape.contentToString()}, Output: ${outputShape.contentToString()}")
            
            // Вычисляем масштаб увеличения
            val scaleX = modelOutputWidth.toFloat() / modelInputWidth
            val scaleY = modelOutputHeight.toFloat() / modelInputHeight
            
            android.util.Log.d("ErsganImageProcessor", "Input shape: ${inputShape.contentToString()}, Output shape: ${outputShape.contentToString()}")
            android.util.Log.d("ErsganImageProcessor", "Original image: ${bitmap.width}x${bitmap.height}, Scale: ${scaleX}x${scaleY}")
            android.util.Log.d("ErsganImageProcessor", "Patch size: ${PATCH_SIZE}x${PATCH_SIZE}, Overlap: $OVERLAP (${(OVERLAP * 100 / PATCH_SIZE)}%)")
            
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
            
            // Копируем обработанные патчи в итоговое изображение с blending на границах
            // Используем прямой доступ к пикселям для более точного контроля
            val outputPixels = IntArray(outputWidth * outputHeight)
            outputBitmap.getPixels(outputPixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
            
            // Создаем массив для подсчета весов (для усреднения перекрывающихся областей)
            val weights = FloatArray(outputWidth * outputHeight)
            
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
                            // Получаем пиксели патча
                            val patchPixels = IntArray(dstWidth * dstHeight)
                            finalPatch.getPixels(patchPixels, 0, dstWidth, 0, 0, dstWidth, dstHeight)
                            
                            // Вычисляем размер перекрытия в выходных координатах
                            // Для 4x масштаба перекрытие в 32 пикселя становится ~128 пикселей на выходе
                            val overlapX = (OVERLAP * scaleX).toInt().coerceAtLeast(32)
                            val overlapY = (OVERLAP * scaleY).toInt().coerceAtLeast(32)
                            
                            // Копируем патч с blending на границах
                            for (y in 0 until dstHeight) {
                                for (x in 0 until dstWidth) {
                                    val outputX = dstX + x
                                    val outputY = dstY + y
                                    val outputIndex = outputY * outputWidth + outputX
                                    
                                    if (outputIndex >= 0 && outputIndex < outputPixels.size) {
                                        val patchIndex = y * dstWidth + x
                                        val patchPixel = patchPixels[patchIndex]
                                        
                                        // Вычисляем вес для blending на границах с использованием улучшенной функции
                                        var weight = 1.0f
                                        
                                        // Проверяем, находимся ли мы в области перекрытия
                                        val distFromLeft = x
                                        val distFromRight = dstWidth - x - 1
                                        val distFromTop = y
                                        val distFromBottom = dstHeight - y - 1
                                        
                                        // Используем улучшенную весовую функцию для более плавного перехода
                                        // Косинусная функция с дополнительным сглаживанием для устранения видимых швов
                                        fun smoothWeight(distance: Int, overlap: Int): Float {
                                            if (distance >= overlap) return 1.0f
                                            val normalized = distance.toFloat() / overlap
                                            // Используем косинусную интерполяцию для более естественного blending
                                            // Это обеспечивает плавный переход без видимых артефактов
                                            return 0.5f * (1.0f - cos(normalized * PI.toFloat()))
                                        }

                                        // Применяем веса на границах для плавного перехода
                                        // Учитываем все четыре границы патча
                                        if (distFromLeft < overlapX && dstX > 0) {
                                            weight *= smoothWeight(distFromLeft, overlapX)
                                        }
                                        if (distFromRight < overlapX && dstX + dstWidth < outputWidth) {
                                            weight *= smoothWeight(distFromRight, overlapX)
                                        }
                                        if (distFromTop < overlapY && dstY > 0) {
                                            weight *= smoothWeight(distFromTop, overlapY)
                                        }
                                        if (distFromBottom < overlapY && dstY + dstHeight < outputHeight) {
                                            weight *= smoothWeight(distFromBottom, overlapY)
                                        }
                                        
                                        // Минимальный вес для избежания полного затухания (может вызвать артефакты)
                                        weight = weight.coerceIn(0.1f, 1.0f)
                                        
                                        // Blending: усредняем с существующим пикселем
                                        val existingWeight = weights[outputIndex]
                                        
                                        if (existingWeight > 0f) {
                                            // Уже есть пиксель в этой позиции - смешиваем
                                            val existingPixel = outputPixels[outputIndex]
                                            
                                            // Извлекаем компоненты существующего пикселя
                                            val existingR = ((existingPixel shr 16) and 0xFF)
                                            val existingG = ((existingPixel shr 8) and 0xFF)
                                            val existingB = (existingPixel and 0xFF)
                                            
                                            // Извлекаем компоненты нового пикселя
                                            val newR = ((patchPixel shr 16) and 0xFF)
                                            val newG = ((patchPixel shr 8) and 0xFF)
                                            val newB = (patchPixel and 0xFF)
                                            
                                            // Взвешенное усреднение с нормализацией
                                            val totalWeight = existingWeight + weight
                                            val blendedR = ((existingR * existingWeight + newR * weight) / totalWeight).toInt().coerceIn(0, 255)
                                            val blendedG = ((existingG * existingWeight + newG * weight) / totalWeight).toInt().coerceIn(0, 255)
                                            val blendedB = ((existingB * existingWeight + newB * weight) / totalWeight).toInt().coerceIn(0, 255)
                                            
                                            // Сохраняем смешанный пиксель
                                            outputPixels[outputIndex] = (255 shl 24) or (blendedR shl 16) or (blendedG shl 8) or blendedB
                                            weights[outputIndex] = totalWeight
                                        } else {
                                            // Если это первый пиксель в этой позиции, просто копируем
                                            outputPixels[outputIndex] = patchPixel
                                            weights[outputIndex] = weight
                                        }
                                    }
                                }
                            }
                            
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
            
            // Применяем обработанные пиксели к итоговому изображению
            outputBitmap.setPixels(outputPixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
            
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
            // Модель ESRGAN (kaggle/esrgan-tf2) ожидает RGB float32 в диапазоне [0, 255]
            when (inputDataType) {
                org.tensorflow.lite.DataType.FLOAT32 -> {
                    // Подаем RGB float32 в диапазоне [0, 255] (без нормализации)
                    for (pixel in pixels) {
                        val r = ((pixel shr 16) and 0xFF).toFloat()
                        val g = ((pixel shr 8) and 0xFF).toFloat()
                        val b = (pixel and 0xFF).toFloat()
                        
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
            // Модель ESRGAN (kaggle/esrgan-tf2) возвращает RGB float32 в диапазоне [0, 255]
            outputBuffer.rewind()
            
            val resultBitmap = when (outputDataType) {
                org.tensorflow.lite.DataType.FLOAT32 -> {
                    // Модель возвращает RGB float32 в диапазоне [0, 255]
                    val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
                    val pixels = IntArray(outputWidth * outputHeight)
                    
                    // Оптимизация: используем прямой доступ к массиву пикселей
                    var pixelIndex = 0
                    for (y in 0 until outputHeight) {
                        for (x in 0 until outputWidth) {
                            // Читаем значения каналов RGB напрямую (без перестановки)
                            val r = outputBuffer.getFloat().toInt().coerceIn(0, 255)
                            val g = outputBuffer.getFloat().toInt().coerceIn(0, 255)
                            val b = outputBuffer.getFloat().toInt().coerceIn(0, 255)
                            
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
                            
                            // Создаем ARGB пиксель напрямую (быстрее чем Color.argb)
                            pixels[pixelIndex++] = (255 shl 24) or (r shl 16) or (g shl 8) or b
                        }
                    }
                    
                    bitmap.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
                    bitmap
                }
                else -> {
                    android.util.Log.e("ErsganImageProcessor", "Неподдерживаемый тип выходных данных: $outputDataType")
                    // Fallback: используем TensorImage с RGB цветовым пространством
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
            
            // Проверяем результат
            if (resultBitmap != null && !resultBitmap.isRecycled) {
                android.util.Log.d("ErsganImageProcessor", 
                    "Патч обработан: ${resultBitmap.width}x${resultBitmap.height}, " +
                    "конфиг: ${resultBitmap.config}")
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
