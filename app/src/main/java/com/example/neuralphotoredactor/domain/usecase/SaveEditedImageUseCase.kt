package com.example.neuralphotoredactor.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import javax.inject.Inject

/**
 * Use case для сохранения отредактированного изображения в галерею.
 */
class SaveEditedImageUseCase @Inject constructor(
    private val processingRepository: ProcessingRepository
) {
    /**
     * Сохранить отредактированное изображение в галерею.
     * 
     * @param bitmap Изображение для сохранения
     * @param fileName Имя файла
     * @param originalUri URI исходного изображения
     * @param filterType Тип фильтра или редактирования
     * @param editSettings Настройки редактирования
     * @return URI сохраненного файла или null
     */
    suspend fun invoke(
        bitmap: Bitmap,
        fileName: String,
        originalUri: Uri? = null,
        filterType: String? = null,
        editSettings: Map<String, Any>? = null
    ): Uri? {
        return processingRepository.saveEditedImageToGallery(
            bitmap,
            fileName,
            originalUri,
            filterType,
            editSettings
        )
    }
}

