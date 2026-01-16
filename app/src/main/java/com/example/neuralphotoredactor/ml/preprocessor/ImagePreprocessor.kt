package com.example.neuralphotoredactor.ml.preprocessor

import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage

/**
 * Препроцессор изображений для TensorFlow Lite моделей.
 *
 * Выполняет нормализацию и преобразование изображений
 * в формат, требуемый для TFLite моделей.
 */
interface ImagePreprocessor {
    /**
     * Преобразовать Bitmap в TensorImage для TFLite.
     *
     * @param bitmap Исходное изображение
     * @param targetWidth Целевая ширина
     * @param targetHeight Целевая высота
     * @return TensorImage готовый для инференса
     */
    fun preprocess(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
    ): TensorImage
}
