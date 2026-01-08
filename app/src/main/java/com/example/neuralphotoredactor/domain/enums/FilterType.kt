package com.example.neuralphotoredactor.domain.enums

/**
 * Типы фильтров для обработки изображений.
 * 
 * Все фильтры работают оффлайн с использованием различных технологий:
 * - AGSL (Android Graphics Shading Language) для GPU-ускоренных эффектов
 * - RenderEffect для некоторых эффектов (API 31+)
 * - TensorFlow Lite для ML-эффектов
 * - Нативные Android API для простых эффектов
 */
enum class FilterType {
    /** Размытие по Гауссу */
    GAUSSIAN_BLUR,
    
    /** Удаление шумов */
    NOISE_REDUCTION,
    
    /** Резкость / Unsharp Mask */
    SHARPEN,
    
    /** Виньетка */
    VIGNETTE,
    
    /** Чёрно-белое преобразование */
    GRAYSCALE,
    
    /** Сепия */
    SEPIA,
    
    /** Стилизация изображения */
    STYLE_TRANSFER,
    
    /** Удаление шумов (старое название) */
    DENOISE,
    
    /** Увеличение разрешения (super resolution) */
    UPSCALE,
    
    /** Цветовая коррекция */
    COLOR_CORRECTION
}

