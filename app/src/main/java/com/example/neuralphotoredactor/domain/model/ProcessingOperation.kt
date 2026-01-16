package com.example.neuralphotoredactor.domain.model

import android.net.Uri

/**
 * Domain модель операции обработки изображения.
 *
 * Фиксирует каждое отдельное действие, выполненное пользователем
 * в рамках сессии редактирования.
 *
 * @param id Уникальный идентификатор операции
 * @param historyId ID записи в истории обработки
 * @param sessionId ID сессии редактирования
 * @param filterId ID использованного фильтра или операции редактирования (NOT NULL)
 * @param parameters Параметры выполнения операции
 * @param inputImageUri URI входного изображения
 * @param outputImageUri URI выходного изображения
 * @param processingTimeMs Время обработки в миллисекундах
 * @param sequenceNumber Порядковый номер операции в истории изменений
 */
data class ProcessingOperation(
    val id: Long = 0,
    val historyId: Long,
    val sessionId: Long,
    val filterId: Long, // NOT NULL - всегда должен быть фильтр или операция редактирования
    val parameters: OperationParameters,
    val inputImageUri: Uri,
    val outputImageUri: Uri,
    val processingTimeMs: Long,
    val sequenceNumber: Int,
)

/**
 * Параметры операции обработки.
 *
 * @param filterType Тип фильтра (если применимо)
 * @param intensity Интенсивность эффекта (0.0 - 1.0)
 * @param additionalParams Дополнительные параметры в формате Map
 */
data class OperationParameters(
    val filterType: String? = null,
    val intensity: Float = 1.0f,
    val additionalParams: Map<String, Any> = emptyMap(),
)
