package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.ProcessingRequest
import com.example.neuralphotoredactor.domain.model.AIResult
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import javax.inject.Inject

/**
 * Use case для обработки изображения AI алгоритмами.
 * 
 * Инкапсулирует бизнес-логику запуска процесса обработки изображения.
 * Используется в ViewModel для выполнения обработки по запросу пользователя.
 * 
 * @param processingRepository Репозиторий для обработки изображений
 * 
 * @see com.example.neuralphotoredactor.domain.repository.ProcessingRepository
 */
class ProcessImageUseCase @Inject constructor(
    private val processingRepository: ProcessingRepository
) {
    /**
     * Запускает обработку изображения с указанным фильтром.
     * 
     * @param request Запрос на обработку, содержащий изображение и параметры фильтра
     * @return AIResult с результатом обработки
     */
    suspend fun processImage(request: ProcessingRequest): AIResult {
        return processingRepository.processImage(request)
    }
}

