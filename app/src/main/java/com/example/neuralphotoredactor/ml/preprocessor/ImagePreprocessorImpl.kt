package com.example.neuralphotoredactor.ml.preprocessor

import android.graphics.Bitmap
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import javax.inject.Inject

/**
 * Реализация препроцессора изображений.
 * 
 * Выполняет ресайз и нормализацию изображений для TFLite моделей.
 */
class ImagePreprocessorImpl @Inject constructor() : ImagePreprocessor {
    
    override fun preprocess(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): TensorImage {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(targetHeight, targetWidth, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        
        val tensorImage = TensorImage.fromBitmap(bitmap)
        return imageProcessor.process(tensorImage)
    }
}

