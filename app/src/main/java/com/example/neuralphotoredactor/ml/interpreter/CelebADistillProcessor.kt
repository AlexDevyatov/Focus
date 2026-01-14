package com.example.neuralphotoredactor.ml.interpreter

import android.graphics.Bitmap
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.repository.TFLiteModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Процессор изображений для модели CelebA Distill (стилизация в аниме стиль).
 * 
 * Выполняет инференс модели celeba_distill.tflite для стилизации изображений в аниме стиль.
 * Применяется для фильтра STYLE_TRANSFER.
 * Работает полностью оффлайн.
 * 
 * Модель принимает вход фиксированного размера 512x512, поэтому изображение масштабируется
 * до этого размера перед обработкой, а затем восстанавливается до исходных размеров.
 * 
 * Нормализация: входные данные нормализуются в диапазон [-1, 1] (как в AnimeGAN2).
 * Денормализация: выходные данные денормализуются из [-1, 1] в [0, 255].
 */
class CelebADistillProcessor @Inject constructor(
    private val tfliteModelRepository: TFLiteModelRepository
) {
    
    companion object {
        private const val MODEL_INPUT_SIZE = 512
        private const val MODEL_ASSET_PATH = "celeba_distill.tflite"
    }
    
    // Мьютекс для синхронизации доступа к Interpreter (он не потокобезопасен)
    private val interpreterMutex = Mutex()
    
    // Кэш Interpreter для модели
    private var cachedInterpreter: Interpreter? = null
    
    /**
     * Получить Interpreter для модели, загрузив его из assets при необходимости.
     */
    private suspend fun getInterpreter(): Interpreter? = withContext(Dispatchers.IO) {
        if (cachedInterpreter != null) {
            return@withContext cachedInterpreter
        }
        
        try {
            val interpreter = tfliteModelRepository.loadModelFromAssets(MODEL_ASSET_PATH)
            cachedInterpreter = interpreter
            interpreter
        } catch (e: Exception) {
            android.util.Log.e("CelebADistillProcessor", "Ошибка загрузки модели: ${e.message}", e)
            null
        }
    }
    
    /**
     * Обработать изображение через модель CelebA Distill для стилизации.
     * 
     * Изображение масштабируется до 512x512, обрабатывается через модель,
     * затем восстанавливается до исходных размеров.
     * 
     * @param bitmap Исходное изображение
     * @param filterType Тип фильтра (должен быть FilterType.STYLE_TRANSFER)
     * @return Обработанное изображение с исходными размерами или null в случае ошибки
     */
    suspend fun processImage(bitmap: Bitmap, filterType: FilterType): Bitmap? = withContext(Dispatchers.Default) {
        val interpreter = getInterpreter() ?: run {
            android.util.Log.e("CelebADistillProcessor", "Interpreter не инициализирован")
            return@withContext null
        }
        
        return@withContext try {
            // Сохраняем исходные размеры
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height
            
            android.util.Log.d("CelebADistillProcessor", "Обработка изображения: ${originalWidth}x${originalHeight}")
            
            // Получаем информацию о входном и выходном тензорах
            val inputDetails = interpreter.getInputTensor(0)
            val inputShape = inputDetails.shape()
            val inputDataType = inputDetails.dataType()
            
            val outputDetails = interpreter.getOutputTensor(0)
            val outputShape = outputDetails.shape()
            val outputDataType = outputDetails.dataType()
            
            android.util.Log.d("CelebADistillProcessor", "Input shape: ${inputShape.contentToString()}, Output shape: ${outputShape.contentToString()}")
            
            // Определяем формат входа (CHW или HWC)
            val isCHWFormat = inputShape.size >= 4 && inputShape[1] == 3
            
            // Масштабируем изображение до размера модели (512x512)
            val scaledBitmap = if (originalWidth != MODEL_INPUT_SIZE || originalHeight != MODEL_INPUT_SIZE) {
                Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)
            } else {
                bitmap
            }
            
            // Подготавливаем входные данные
            val inputWidth = MODEL_INPUT_SIZE
            val inputHeight = MODEL_INPUT_SIZE
            val inputChannels = 3 // RGB
            
            // Создаем массив для входных данных
            val inputArray = if (isCHWFormat) {
                Array(1) { Array(inputChannels) { Array(inputHeight) { FloatArray(inputWidth) } } }
            } else {
                Array(1) { Array(inputHeight) { Array(inputWidth) { FloatArray(inputChannels) } } }
            }
            
            // Извлекаем пиксели из bitmap
            val pixels = IntArray(inputWidth * inputHeight)
            scaledBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
            
            // Нормализуем пиксели в диапазон [-1, 1]
            for (y in 0 until inputHeight) {
                for (x in 0 until inputWidth) {
                    val pixelIndex = y * inputWidth + x
                    val pixel = pixels[pixelIndex]
                    
                    val r = ((pixel shr 16) and 0xFF).toFloat()
                    val g = ((pixel shr 8) and 0xFF).toFloat()
                    val b = (pixel and 0xFF).toFloat()
                    
                    // Нормализация в [-1, 1]
                    val rNorm = (r / 127.5f) - 1.0f
                    val gNorm = (g / 127.5f) - 1.0f
                    val bNorm = (b / 127.5f) - 1.0f
                    
                    if (isCHWFormat) {
                        inputArray[0][0][y][x] = rNorm
                        inputArray[0][1][y][x] = gNorm
                        inputArray[0][2][y][x] = bNorm
                    } else {
                        inputArray[0][y][x][0] = rNorm
                        inputArray[0][y][x][1] = gNorm
                        inputArray[0][y][x][2] = bNorm
                    }
                }
            }
            
            // Освобождаем масштабированный bitmap, если он был создан
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            
            // Создаем ByteBuffer для входных данных
            val inputBuffer = ByteBuffer.allocateDirect(inputDetails.numBytes())
            inputBuffer.order(ByteOrder.nativeOrder())
            
            // Заполняем входной буфер
            when (inputDataType) {
                org.tensorflow.lite.DataType.FLOAT32 -> {
                    if (isCHWFormat) {
                        for (c in 0 until inputChannels) {
                            for (y in 0 until inputHeight) {
                                for (x in 0 until inputWidth) {
                                    inputBuffer.putFloat(inputArray[0][c][y][x])
                                }
                            }
                        }
                    } else {
                        for (y in 0 until inputHeight) {
                            for (x in 0 until inputWidth) {
                                for (c in 0 until inputChannels) {
                                    inputBuffer.putFloat(inputArray[0][y][x][c])
                                }
                            }
                        }
                    }
                }
                else -> {
                    android.util.Log.e("CelebADistillProcessor", "Неподдерживаемый тип входных данных: $inputDataType")
                    return@withContext null
                }
            }
            inputBuffer.rewind()
            
            // Создаем выходной буфер
            val outputBuffer = ByteBuffer.allocateDirect(outputDetails.numBytes())
            outputBuffer.order(ByteOrder.nativeOrder())
            
            // Инференс через модель с синхронизацией через мьютекс
            interpreterMutex.withLock {
                interpreter.run(inputBuffer, outputBuffer)
            }
            
            // Обрабатываем выходные данные
            outputBuffer.rewind()
            
            val outputWidth = if (isCHWFormat) outputShape[3] else outputShape[2]
            val outputHeight = if (isCHWFormat) outputShape[2] else outputShape[1]
            
            val processedBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            val outputPixels = IntArray(outputWidth * outputHeight)
            
            when (outputDataType) {
                org.tensorflow.lite.DataType.FLOAT32 -> {
                    var pixelIndex = 0
                    
                    if (isCHWFormat) {
                        // CHW формат: [1, C, H, W]
                        val rValues = FloatArray(outputWidth * outputHeight)
                        val gValues = FloatArray(outputWidth * outputHeight)
                        val bValues = FloatArray(outputWidth * outputHeight)
                        
                        for (c in 0 until 3) {
                            for (y in 0 until outputHeight) {
                                for (x in 0 until outputWidth) {
                                    val index = y * outputWidth + x
                                    val value = outputBuffer.getFloat()
                                    when (c) {
                                        0 -> rValues[index] = value
                                        1 -> gValues[index] = value
                                        2 -> bValues[index] = value
                                    }
                                }
                            }
                        }
                        
                        for (y in 0 until outputHeight) {
                            for (x in 0 until outputWidth) {
                                val index = y * outputWidth + x
                                val rNorm = rValues[index]
                                val gNorm = gValues[index]
                                val bNorm = bValues[index]
                                
                                // Денормализация из [-1, 1] в [0, 255]
                                val r = ((rNorm + 1.0f) * 127.5f).coerceIn(0f, 255f).toInt()
                                val g = ((gNorm + 1.0f) * 127.5f).coerceIn(0f, 255f).toInt()
                                val b = ((bNorm + 1.0f) * 127.5f).coerceIn(0f, 255f).toInt()
                                
                                outputPixels[pixelIndex++] = (255 shl 24) or (r shl 16) or (g shl 8) or b
                            }
                        }
                    } else {
                        // HWC формат: [1, H, W, C]
                        for (y in 0 until outputHeight) {
                            for (x in 0 until outputWidth) {
                                val rNorm = outputBuffer.getFloat()
                                val gNorm = outputBuffer.getFloat()
                                val bNorm = outputBuffer.getFloat()
                                
                                // Денормализация из [-1, 1] в [0, 255]
                                val r = ((rNorm + 1.0f) * 127.5f).coerceIn(0f, 255f).toInt()
                                val g = ((gNorm + 1.0f) * 127.5f).coerceIn(0f, 255f).toInt()
                                val b = ((bNorm + 1.0f) * 127.5f).coerceIn(0f, 255f).toInt()
                                
                                outputPixels[pixelIndex++] = (255 shl 24) or (r shl 16) or (g shl 8) or b
                            }
                        }
                    }
                }
                else -> {
                    android.util.Log.e("CelebADistillProcessor", "Неподдерживаемый тип выходных данных: $outputDataType")
                    return@withContext null
                }
            }
            
            processedBitmap.setPixels(outputPixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
            
            // Восстанавливаем исходные размеры
            val finalBitmap = if (outputWidth != originalWidth || outputHeight != originalHeight) {
                Bitmap.createScaledBitmap(processedBitmap, originalWidth, originalHeight, true).also {
                    processedBitmap.recycle()
                }
            } else {
                processedBitmap
            }
            
            android.util.Log.d("CelebADistillProcessor", "Обработка завершена: ${originalWidth}x${originalHeight}")
            finalBitmap
        } catch (e: Exception) {
            android.util.Log.e("CelebADistillProcessor", "Ошибка обработки: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
}

