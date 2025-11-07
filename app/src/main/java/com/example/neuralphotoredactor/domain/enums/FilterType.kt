package com.example.neuralphotoredactor.domain.enums

/**
 * Перечисление типов AI фильтров и эффектов для обработки изображений.
 * 
 * Включает как on-device фильтры (обрабатываются локально на устройстве через TensorFlow Lite),
 * так и cloud-based фильтры (обрабатываются через внешние API сервисы).
 * 
 * @property STYLE_TRANSFER Перенос стиля с референсного изображения (on-device)
 * @property SUPER_RESOLUTION Увеличение разрешения изображения (on-device)
 * @property BACKGROUND_REMOVAL Удаление фона с изображения (on-device)
 * @property COLORIZATION Раскрашивание черно-белых фотографий (on-device)
 * @property FACE_ENHANCEMENT Улучшение качества лиц на фотографиях (on-device)
 * @property DEEPART_EFFECTS Художественные фильтры и эффекты (cloud-based)
 * @property BACKGROUND_REPLACEMENT Замена фона на изображении (cloud-based)
 * @property OBJECT_REMOVAL Удаление объектов с изображения (cloud-based)
 * @property AI_UPSCALING Профессиональное увеличение разрешения через AI (cloud-based)
 */
enum class FilterType {
    STYLE_TRANSFER,
    SUPER_RESOLUTION,
    BACKGROUND_REMOVAL,
    COLORIZATION,
    FACE_ENHANCEMENT,
    DEEPART_EFFECTS,
    BACKGROUND_REPLACEMENT,
    OBJECT_REMOVAL,
    AI_UPSCALING
}

