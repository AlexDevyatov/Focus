package com.example.neuralphotoredactor.domain.enums

/**
 * Типы фильтров для обработки изображений.
 * 
 * Все фильтры работают оффлайн через TensorFlow Lite модели.
 */
enum class FilterType {
    /** Улучшение качества изображения */
    ENHANCE,
    
    /** Стилизация изображения */
    STYLE_TRANSFER,
    
    /** Удаление шумов */
    DENOISE,
    
    /** Увеличение разрешения (super resolution) */
    UPSCALE,
    
    /** Цветовая коррекция */
    COLOR_CORRECTION,
    
    /** Чёрно-белое преобразование */
    GRAYSCALE
}

