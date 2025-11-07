package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.repository.ImageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения всех изображений из галереи.
 * 
 * Инкапсулирует бизнес-логику получения списка всех доступных изображений.
 * Используется в ViewModel для отображения галереи изображений в UI.
 * 
 * @param imageRepository Репозиторий для работы с изображениями
 * 
 * @see com.example.neuralphotoredactor.domain.repository.ImageRepository
 */
class GetAllImagesUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    /**
     * Получает поток всех доступных изображений из галереи.
     * 
     * @return Flow со списком всех изображений, обновляющийся при изменении галереи
     */
    operator fun invoke(): Flow<List<ImageData>> {
        return imageRepository.getAllImages()
    }
}

