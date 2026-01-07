package com.example.neuralphotoredactor.ml.interpreter

import android.graphics.Bitmap
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.ml.preprocessor.ImagePreprocessor
import com.example.neuralphotoredactor.ml.postprocessor.ImagePostprocessor
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
    
    /**
     * Обработать изображение через модель ERSGAN для увеличения разрешения.
     * 
     * @param bitmap Исходное изображение
     * @param filterType Тип фильтра (должен соответствовать ERSGAN модели)
     * @return Обработанное изображение с увеличенным разрешением или null в случае ошибки
     */
    fun processImage(bitmap: Bitmap, filterType: FilterType): Bitmap? {
        if (interpreter == null) {
            android.util.Log.e("ErsganImageProcessor", "ESRGAN Interpreter не инициализирован")
            return null
        }
        
        return try {
            // Получаем размеры входного тензора модели ESRGAN
            val inputTensor = interpreter.getInputTensor(0)
            val inputShape = inputTensor.shape()
            val inputDataType = inputTensor.dataType()
            val targetWidth = inputShape[1]
            val targetHeight = inputShape[2]
            
            android.util.Log.d("ErsganImageProcessor", "Input shape: ${inputShape.contentToString()}, dtype: $inputDataType")
            android.util.Log.d("ErsganImageProcessor", "Original image: ${bitmap.width}x${bitmap.height}, target: ${targetWidth}x${targetHeight}")
            
            // Ресайзим изображение до нужного размера
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            
            // Создаем входной буфер вручную с правильным форматом
            val expectedBytes = inputTensor.numBytes()
            val inputBuffer = ByteBuffer.allocateDirect(expectedBytes)
            inputBuffer.order(java.nio.ByteOrder.nativeOrder())
            
            // Получаем пиксели изображения
            val pixels = IntArray(targetWidth * targetHeight)
            resizedBitmap.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
            
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
                    return null
                }
            }
            inputBuffer.rewind()
            
            // Освобождаем промежуточный bitmap
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            
            // Подготовка выходного тензора
            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            val outputDataType = outputTensor.dataType()
            
            android.util.Log.d("ErsganImageProcessor", "Output shape: ${outputShape.contentToString()}, dtype: $outputDataType")
            
            // Создаем ByteBuffer для вывода
            val outputBuffer = ByteBuffer.allocateDirect(
                outputTensor.numBytes()
            )
            outputBuffer.order(java.nio.ByteOrder.nativeOrder())
            
            // Инференс через модель ESRGAN
            interpreter.run(inputBuffer, outputBuffer)
            
            // Создаем TensorBuffer из результата
            val tensorBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType)
            outputBuffer.rewind()
            tensorBuffer.loadBuffer(outputBuffer)
            
            // Создаем TensorImage из TensorBuffer
            val outputImage = TensorImage(outputDataType)
            outputImage.load(
                tensorBuffer,
                org.tensorflow.lite.support.image.ColorSpaceType.RGB
            )
            
            // Для super resolution модель уже увеличила разрешение
            // Возвращаем результат напрямую без постпроцессинга
            outputImage.bitmap
        } catch (e: Exception) {
            android.util.Log.e("ErsganImageProcessor", "Ошибка обработки через ESRGAN: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
}
