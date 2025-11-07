package com.example.neuralphotoredactor.data.repository

import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.FilterPreset
import com.example.neuralphotoredactor.domain.repository.FilterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Реализация репозитория для работы с предустановками фильтров.
 * 
 * Предоставляет список доступных AI фильтров для отображения в UI.
 * Может получать данные из локального источника (hardcoded список) или
 * из удаленного API. Внедряется через Hilt.
 * 
 * @see com.example.neuralphotoredactor.domain.repository.FilterRepository
 */
class FilterRepositoryImpl @Inject constructor() : FilterRepository {
    
    /**
     * Список всех доступных фильтров.
     * В будущем может быть загружен из API или базы данных.
     */
    private val availableFilters = listOf(
        // On-device фильтры
        FilterPreset(
            id = "style_transfer",
            name = "Style Transfer",
            type = FilterType.STYLE_TRANSFER,
            description = "Перенос стиля с референсного изображения",
            isOnDevice = true
        ),
        FilterPreset(
            id = "super_resolution",
            name = "Super Resolution",
            type = FilterType.SUPER_RESOLUTION,
            description = "Увеличение разрешения изображения",
            isOnDevice = true
        ),
        FilterPreset(
            id = "background_removal",
            name = "Background Removal",
            type = FilterType.BACKGROUND_REMOVAL,
            description = "Удаление фона с изображения",
            isOnDevice = true
        ),
        FilterPreset(
            id = "colorization",
            name = "Colorization",
            type = FilterType.COLORIZATION,
            description = "Раскрашивание черно-белых фотографий",
            isOnDevice = true
        ),
        FilterPreset(
            id = "face_enhancement",
            name = "Face Enhancement",
            type = FilterType.FACE_ENHANCEMENT,
            description = "Улучшение качества лиц на фотографиях",
            isOnDevice = true
        ),
        // Cloud-based фильтры
        FilterPreset(
            id = "deepart_effects",
            name = "DeepArt Effects",
            type = FilterType.DEEPART_EFFECTS,
            description = "Художественные фильтры и эффекты",
            isOnDevice = false
        ),
        FilterPreset(
            id = "background_replacement",
            name = "Background Replacement",
            type = FilterType.BACKGROUND_REPLACEMENT,
            description = "Замена фона на изображении",
            isOnDevice = false
        ),
        FilterPreset(
            id = "object_removal",
            name = "Object Removal",
            type = FilterType.OBJECT_REMOVAL,
            description = "Удаление объектов с изображения",
            isOnDevice = false
        ),
        FilterPreset(
            id = "ai_upscaling",
            name = "AI Upscaling",
            type = FilterType.AI_UPSCALING,
            description = "Профессиональное увеличение разрешения через AI",
            isOnDevice = false
        )
    )

    override fun getAllFilters(): Flow<List<FilterPreset>> {
        return flowOf(availableFilters)
    }

    override suspend fun getFilterById(id: String): FilterPreset? {
        return availableFilters.find { it.id == id }
    }
}

