package com.example.neuralphotoredactor.domain.model

/**
 * Модель опций для настройки процесса обработки изображения.
 * 
 * Содержит параметры, которые могут влиять на качество и способ обработки:
 * качество выходного изображения, использование GPU, размер выходного изображения и т.д.
 * 
 * @param quality Качество выходного изображения (1-100, где 100 - максимальное качество)
 * @param useGpu Флаг использования GPU для ускорения обработки (для on-device фильтров)
 * @param outputWidth Желаемая ширина выходного изображения (0 - сохранить оригинальный размер)
 * @param outputHeight Желаемая высота выходного изображения (0 - сохранить оригинальный размер)
 * @param preserveAspectRatio Сохранять ли соотношение сторон при изменении размера
 */
data class ProcessingOptions(
    val quality: Int = 90,
    val useGpu: Boolean = true,
    val outputWidth: Int = 0,
    val outputHeight: Int = 0,
    val preserveAspectRatio: Boolean = true
) {
    init {
        require(quality in 1..100) { "Quality must be between 1 and 100" }
        require(outputWidth >= 0) { "Output width must be non-negative" }
        require(outputHeight >= 0) { "Output height must be non-negative" }
    }
}

