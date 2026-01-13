package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.data.local.dao.FilterDao
import com.example.neuralphotoredactor.data.local.entity.FilterEntity
import com.example.neuralphotoredactor.domain.enums.EditType
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.repository.NeuralModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case для инициализации фильтров в базе данных.
 * 
 * Заполняет таблицу filters данными о фильтрах при первом запуске.
 * Связывает фильтры с нейросетевыми моделями, если фильтр использует модель.
 */
class InitializeFiltersUseCase @Inject constructor(
    private val filterDao: FilterDao,
    private val neuralModelRepository: NeuralModelRepository
) {
    
    /**
     * Инициализировать фильтры в базе данных.
     * 
     * Проверяет, есть ли уже фильтры в БД, и если нет - заполняет их данными.
     * 
     * @return true, если инициализация прошла успешно, false в случае ошибки
     */
    suspend fun invoke(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Проверяем, есть ли уже фильтры в БД
            val filtersList = filterDao.getAllFilters().first()
            
            if (filtersList.isNotEmpty()) {
                android.util.Log.d("InitializeFilters", "Фильтры уже инициализированы: ${filtersList.size} фильтров")
                return@withContext true
            }
            
            android.util.Log.d("InitializeFilters", "Начало инициализации фильтров")
            
            // Получаем модели для связи с фильтрами
            val models = neuralModelRepository.getAllModels().first()
            val modelMap = models.associateBy { it.name }
            
            // Список фильтров для инициализации (включая операции редактирования)
            val filtersToInitialize = buildList {
                // Фильтры обработки изображений
                add(FilterInfo(FilterType.GAUSSIAN_BLUR.name, null))
                add(FilterInfo(FilterType.NOISE_REDUCTION.name, null))
                add(FilterInfo(FilterType.SHARPEN.name, null))
                add(FilterInfo(FilterType.VIGNETTE.name, null))
                add(FilterInfo(FilterType.GRAYSCALE.name, null))
                add(FilterInfo(FilterType.SEPIA.name, null))
                add(FilterInfo(FilterType.STYLE_TRANSFER.name, "AnimeGAN2 Paprika"))
                add(FilterInfo(FilterType.DENOISE.name, "SplitterNet"))
                add(FilterInfo(FilterType.UPSCALE.name, "ESRGAN"))
                add(FilterInfo(FilterType.COLOR_CORRECTION.name, null))
                
                // Операции редактирования из вкладки Настройки
                add(FilterInfo(EditType.CROP.name, null))
                add(FilterInfo(EditType.ROTATE_90.name, null))
                add(FilterInfo(EditType.ROTATE_180.name, null))
                add(FilterInfo(EditType.ROTATE_270.name, null))
                add(FilterInfo(EditType.FLIP_HORIZONTAL.name, null))
                add(FilterInfo(EditType.FLIP_VERTICAL.name, null))
                add(FilterInfo(EditType.BRIGHTNESS.name, null))
                add(FilterInfo(EditType.CONTRAST.name, null))
                add(FilterInfo(EditType.COLOR_BALANCE_RED.name, null))
                add(FilterInfo(EditType.COLOR_BALANCE_GREEN.name, null))
                add(FilterInfo(EditType.COLOR_BALANCE_BLUE.name, null))
            }
            
            // Добавляем каждый фильтр
            var successCount = 0
            for (filterInfo in filtersToInitialize) {
                try {
                    val modelId = filterInfo.modelName?.let { modelName ->
                        modelMap[modelName]?.id
                    }
                    
                    filterDao.insert(
                        FilterEntity(
                            name = filterInfo.name,
                            modelId = modelId
                        )
                    )
                    successCount++
                    android.util.Log.d("InitializeFilters", "Фильтр добавлен: ${filterInfo.name} (modelId: $modelId)")
                } catch (e: Exception) {
                    android.util.Log.e("InitializeFilters", "Ошибка добавления фильтра ${filterInfo.name}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("InitializeFilters", "Инициализация завершена: $successCount из ${filtersToInitialize.size} фильтров")
            successCount == filtersToInitialize.size
            
        } catch (e: Exception) {
            android.util.Log.e("InitializeFilters", "Ошибка инициализации фильтров: ${e.message}", e)
            false
        }
    }
    
    /**
     * Информация о фильтре для инициализации.
     */
    private data class FilterInfo(
        val name: String,
        val modelName: String? // Название модели, если фильтр использует модель
    )
}

