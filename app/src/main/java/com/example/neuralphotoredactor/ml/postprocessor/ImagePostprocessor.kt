package com.example.neuralphotoredactor.ml.postprocessor

import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage

/**
 * Постпроцессор изображений для TensorFlow Lite моделей.
 *
 * Выполняет преобразование результатов инференса обратно в Bitmap
 * и применяет необходимые преобразования (денормализация, ресайз).
 */
interface ImagePostprocessor {
    /**
     * Преобразовать TensorImage в Bitmap.
     *
     * @param tensorImage Результат инференса
     * @param originalWidth Исходная ширина изображения
     * @param originalHeight Исходная высота изображения
     * @return Обработанное изображение в виде Bitmap
     */
    fun postprocess(
        tensorImage: TensorImage,
        originalWidth: Int,
        originalHeight: Int,
    ): Bitmap
}
