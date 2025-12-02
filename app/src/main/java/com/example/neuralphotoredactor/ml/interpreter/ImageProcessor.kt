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
 * Процессор изображений с использованием TensorFlow Lite.
 * 
 * Выполняет инференс TFLite моделей для обработки изображений.
 * Работает полностью оффлайн.
 */
class ImageProcessor @Inject constructor(
    private val interpreter: Interpreter?,
    private val preprocessor: ImagePreprocessor,
    private val postprocessor: ImagePostprocessor
) {
    
    /**
     * Обработать изображение с применением указанного фильтра.
     * 
     * @param bitmap Исходное изображение
     * @param filterType Тип фильтра
     * @return Обработанное изображение или null в случае ошибки
     */
    fun processImage(bitmap: Bitmap, filterType: FilterType): Bitmap? {
        if (interpreter == null) {
            android.util.Log.e("ImageProcessor", "TFLite Interpreter не инициализирован")
            return null
        }
        
        return try {
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height
            
            // Получаем размеры входного тензора модели
            val inputShape = interpreter.getInputTensor(0).shape()
            val targetWidth = inputShape[1]
            val targetHeight = inputShape[2]
            
            // Препроцессинг
            val inputImage = preprocessor.preprocess(bitmap, targetWidth, targetHeight)
            
            // Подготовка выходного тензора
            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            val outputDataType = outputTensor.dataType()
            
            // Создаем ByteBuffer для вывода
            val outputBuffer = ByteBuffer.allocateDirect(
                outputTensor.numBytes()
            )
            outputBuffer.order(java.nio.ByteOrder.nativeOrder())
            
            // Инференс
            interpreter.run(inputImage.buffer, outputBuffer)
            
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
            
            // Постпроцессинг
            postprocessor.postprocess(outputImage, originalWidth, originalHeight)
        } catch (e: Exception) {
            null
        }
    }
}

