package com.example.neuralphotoredactor.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import javax.inject.Inject

/**
 * Use case для загрузки Bitmap из URI.
 */
class LoadBitmapUseCase @Inject constructor(
    private val processingRepository: ProcessingRepository
) {
    /**
     * Загрузить Bitmap из URI.
     * 
     * @param uri URI изображения
     * @return Bitmap или null
     */
    suspend fun invoke(uri: Uri): Bitmap? {
        return processingRepository.loadBitmapFromUri(uri)
    }
}

