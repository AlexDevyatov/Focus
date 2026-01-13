package com.example.neuralphotoredactor.domain.usecase

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.neuralphotoredactor.domain.enums.EditType
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import javax.inject.Inject

/**
 * Use case для применения редактирования к изображению.
 */
class ApplyEditUseCase @Inject constructor(
    private val processingRepository: ProcessingRepository
) {
    /**
     * Применить редактирование к изображению.
     * 
     * @param bitmap Исходное изображение
     * @param editType Тип редактирования
     * @param value Значение для редактирования
     * @param cropRect Прямоугольник кадрирования
     * @return Обработанное изображение или null
     */
    suspend fun invoke(
        bitmap: Bitmap,
        editType: EditType,
        value: Float = 0f,
        cropRect: Rect? = null
    ): Bitmap? {
        return processingRepository.applyEdit(bitmap, editType, value, cropRect)
    }
}

