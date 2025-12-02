package com.example.neuralphotoredactor.ml.postprocessor

import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage
import javax.inject.Inject

/**
 * Реализация постпроцессора изображений.
 * 
 * Преобразует результаты TFLite инференса в Bitmap.
 */
class ImagePostprocessorImpl @Inject constructor() : ImagePostprocessor {
    
    override fun postprocess(
        tensorImage: TensorImage,
        originalWidth: Int,
        originalHeight: Int
    ): Bitmap {
        var bitmap = tensorImage.bitmap
        
        // Если размеры не совпадают, выполняем ресайз
        if (bitmap.width != originalWidth || bitmap.height != originalHeight) {
            bitmap = Bitmap.createScaledBitmap(
                bitmap,
                originalWidth,
                originalHeight,
                true
            )
        }
        
        return bitmap
    }
}

