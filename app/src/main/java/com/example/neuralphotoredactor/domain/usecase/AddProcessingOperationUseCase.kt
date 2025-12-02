package com.example.neuralphotoredactor.domain.usecase

import android.net.Uri
import com.example.neuralphotoredactor.domain.model.OperationParameters
import com.example.neuralphotoredactor.domain.model.ProcessingOperation
import com.example.neuralphotoredactor.domain.repository.ProcessingOperationRepository
import javax.inject.Inject

/**
 * Use case для добавления операции обработки.
 */
class AddProcessingOperationUseCase @Inject constructor(
    private val processingOperationRepository: ProcessingOperationRepository
) {
    /**
     * Добавить операцию обработки.
     * 
     * @param sessionId ID сессии редактирования
     * @param modelId ID использованной модели (может быть null)
     * @param operationType Тип операции
     * @param parameters Параметры операции
     * @param inputImageUri URI входного изображения
     * @param outputImageUri URI выходного изображения
     * @param processingTimeMs Время обработки в миллисекундах
     * @return ID созданной операции
     */
    suspend fun invoke(
        sessionId: Long,
        modelId: Long?,
        operationType: String,
        parameters: OperationParameters,
        inputImageUri: Uri,
        outputImageUri: Uri,
        processingTimeMs: Long
    ): Long {
        // Получаем максимальный порядковый номер для сессии
        val lastOperation = processingOperationRepository.getLastOperationBySessionId(sessionId)
        val sequenceNumber = (lastOperation?.sequenceNumber ?: 0) + 1
        
        val operation = ProcessingOperation(
            sessionId = sessionId,
            modelId = modelId,
            operationType = operationType,
            parameters = parameters,
            inputImageUri = inputImageUri,
            outputImageUri = outputImageUri,
            processingTimeMs = processingTimeMs,
            sequenceNumber = sequenceNumber
        )
        
        return processingOperationRepository.addOperation(operation)
    }
}

