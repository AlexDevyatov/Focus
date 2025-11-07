package com.example.neuralphotoredactor.domain.enums

/**
 * Перечисление статусов обработки изображения AI алгоритмами.
 * 
 * Используется для отслеживания состояния процесса обработки изображения
 * от момента создания запроса до завершения (успешного или с ошибкой).
 * 
 * @property PENDING Запрос создан и ожидает начала обработки
 * @property PROCESSING Изображение находится в процессе обработки
 * @property COMPLETED Обработка успешно завершена
 * @property FAILED Обработка завершилась с ошибкой
 */
enum class ProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

