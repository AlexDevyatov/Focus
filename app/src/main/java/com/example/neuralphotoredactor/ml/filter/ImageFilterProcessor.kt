package com.example.neuralphotoredactor.ml.filter

import android.graphics.Bitmap
import com.example.neuralphotoredactor.domain.enums.FilterType
import javax.inject.Inject

/**
 * Процессор для применения фильтров к изображениям.
 * 
 * Поддерживает различные типы фильтров:
 * - Gaussian Blur (RenderEffect)
 * - Noise Reduction (алгоритмический или ML)
 * - Sharpen/Unsharp Mask (Convolution)
 * - Vignette (AGSL или RenderEffect)
 * - Grayscale (ColorMatrix)
 * - Sepia (ColorMatrix)
 * 
 * Все фильтры работают полностью оффлайн.
 * Вся работа с Bitmap происходит в ml/ слое.
 */
interface ImageFilterProcessor {
    /**
     * Применить фильтр к изображению.
     * 
     * @param bitmap Исходное изображение
     * @param filterType Тип фильтра
     * @param intensity Интенсивность фильтра (0.0 - 1.0), null для значения по умолчанию
     * @param isPreview Если true, используется меньший размер для быстрого предпросмотра
     * @return Обработанное изображение или null в случае ошибки
     */
    fun applyFilter(bitmap: Bitmap, filterType: FilterType, intensity: Float? = null, isPreview: Boolean = false): Bitmap?
}

