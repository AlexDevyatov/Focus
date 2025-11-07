package com.example.neuralphotoredactor.domain.model

import com.example.neuralphotoredactor.domain.enums.FilterType

/**
 * Модель запроса на обработку изображения AI алгоритмами.
 * 
 * Содержит всю необходимую информацию для запуска процесса обработки:
 * исходное изображение, тип применяемого фильтра и дополнительные параметры.
 * 
 * @param imageData Исходное изображение для обработки
 * @param filterType Тип AI фильтра или эффекта, который нужно применить
 * @param parameters Дополнительные параметры обработки (например, интенсивность эффекта,
 *                   референсное изображение для style transfer и т.д.)
 */
data class ProcessingRequest(
    val imageData: ImageData,
    val filterType: FilterType,
    val parameters: Map<String, Any> = emptyMap()
)

