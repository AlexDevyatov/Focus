package com.example.neuralphotoredactor.domain.usecase

import android.net.Uri
import com.example.neuralphotoredactor.domain.model.EditingSession
import com.example.neuralphotoredactor.domain.model.SessionMetadata
import com.example.neuralphotoredactor.domain.repository.EditingSessionRepository
import javax.inject.Inject

/**
 * Use case для создания новой сессии редактирования.
 */
class CreateEditingSessionUseCase @Inject constructor(
    private val editingSessionRepository: EditingSessionRepository
) {
    /**
     * Создать новую сессию редактирования.
     * 
     * @param originalImageUri URI исходного изображения
     * @param metadata Метаданные изображения
     * @return ID созданной сессии
     */
    suspend fun invoke(
        originalImageUri: Uri,
        metadata: SessionMetadata
    ): Long {
        val currentTime = System.currentTimeMillis()
        val session = EditingSession(
            originalImageUri = originalImageUri,
            currentImageUri = originalImageUri, // Изначально текущее изображение = исходное
            createdAt = currentTime,
            updatedAt = currentTime,
            metadata = metadata
        )
        return editingSessionRepository.createSession(session)
    }
}

