package com.example.neuralphotoredactor.ml.edit

import android.graphics.Bitmap
import com.example.neuralphotoredactor.domain.enums.EditType
import javax.inject.Inject

/**
 * Процессор для редактирования изображений.
 * 
 * Поддерживает:
 * - Кадрирование
 * - Поворот и отражение
 * - Коррекция яркости и контраста
 * - Настройка цветового баланса
 */
interface ImageEditProcessor {
    /**
     * Применить редактирование к изображению.
     * 
     * @param bitmap Исходное изображение
     * @param editType Тип редактирования
     * @param value Значение для редактирования (для яркости, контраста, цветового баланса)
     * @param cropRect Прямоугольник кадрирования (left, top, right, bottom) в пикселях
     * @return Обработанное изображение или null в случае ошибки
     */
    fun applyEdit(
        bitmap: Bitmap,
        editType: EditType,
        value: Float = 0f,
        cropRect: android.graphics.Rect? = null
    ): Bitmap?
}
