package com.example.neuralphotoredactor.domain.usecase

import android.net.Uri
import com.example.neuralphotoredactor.data.storage.ImageStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case для получения всех обработанных изображений из папки processed.
 */
class GetProcessedImagesUseCase @Inject constructor(
    private val imageStorage: ImageStorage
) {
    /**
     * Получить все обработанные изображения.
     * 
     * @return Flow со списком URI обработанных изображений
     */
    suspend fun invoke(): List<Uri> {
        return imageStorage.getProcessedImages()
    }
}

