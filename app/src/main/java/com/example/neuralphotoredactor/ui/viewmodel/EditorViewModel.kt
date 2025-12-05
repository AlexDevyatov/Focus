package com.example.neuralphotoredactor.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import com.example.neuralphotoredactor.domain.usecase.ProcessImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана редактора.
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val processImageUseCase: ProcessImageUseCase,
    private val processingRepository: ProcessingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    
    private var currentFilterJob: Job? = null // Для отмены предыдущих запросов
    private var currentPreviewJob: Job? = null // Для отмены предыдущих предпросмотров
    private var cachedOriginalBitmap: Bitmap? = null // Кэш исходного изображения
    
    val availableFilters = FilterType.entries
    
    fun setImage(imageData: ImageData) {
        _uiState.value = _uiState.value.copy(
            imageData = imageData,
            processedResult = null,
            previewBitmap = null,
            selectedFilter = null,
            filterIntensity = 0.5f
        )
        // Очищаем кэш при смене изображения
        cachedOriginalBitmap = null
        currentPreviewJob?.cancel()
        currentFilterJob?.cancel()
        
        // Предзагружаем Bitmap в фоне для быстрого доступа
        viewModelScope.launch {
            getOrLoadOriginalBitmap()
        }
    }
    
    fun selectFilter(filterType: FilterType) {
        _uiState.value = _uiState.value.copy(
            selectedFilter = filterType,
            filterIntensity = 0.5f // Сброс интенсивности при выборе нового фильтра
        )
        // Используем быстрый предпросмотр для начального отображения
        previewFilter(filterType, 0.5f)
    }
    
    fun updateFilterIntensity(intensity: Float) {
        val selectedFilter = _uiState.value.selectedFilter ?: return
        _uiState.value = _uiState.value.copy(filterIntensity = intensity)
        // Используем быстрый предпросмотр для реального времени
        previewFilter(selectedFilter, intensity)
    }
    
    /**
     * Быстрый предпросмотр фильтра без сохранения в файл.
     * Используется для отображения результата в реальном времени при перемещении слайдера.
     */
    private fun previewFilter(filterType: FilterType, intensity: Float) {
        // Отменяем предыдущий предпросмотр
        currentPreviewJob?.cancel()
        
        currentPreviewJob = viewModelScope.launch {
            // Небольшая задержка для debounce (чтобы не обрабатывать каждое микро-изменение)
            delay(100) // 100ms debounce для лучшей производительности
            
            val originalBitmap = getOrLoadOriginalBitmap()
            if (originalBitmap == null) {
                android.util.Log.e("EditorViewModel", "Не удалось получить исходный Bitmap для предпросмотра")
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        error = "Не удалось загрузить изображение"
                    )
                }
                return@launch
            }
            
            // Проверяем, что Bitmap не переработан
            if (originalBitmap.isRecycled) {
                android.util.Log.e("EditorViewModel", "Исходный Bitmap был переработан, перезагружаем...")
                cachedOriginalBitmap = null
                val reloadedBitmap = getOrLoadOriginalBitmap() ?: return@launch
                if (reloadedBitmap.isRecycled) {
                    android.util.Log.e("EditorViewModel", "Перезагруженный Bitmap также переработан")
                    return@launch
                }
            }
            
            val currentSelectedFilter = _uiState.value.selectedFilter ?: filterType
            
            try {
                android.util.Log.d("EditorViewModel", "Применяем фильтр $filterType с интенсивностью $intensity")
                val previewBitmap = processingRepository.previewFilter(
                    originalBitmap,
                    filterType,
                    intensity
                )
                
                if (isActive) {
                    if (previewBitmap != null) {
                        android.util.Log.d("EditorViewModel", "Предпросмотр создан: ${previewBitmap.width}x${previewBitmap.height}")
                        _uiState.value = _uiState.value.copy(
                            previewBitmap = previewBitmap,
                            selectedFilter = currentSelectedFilter
                        )
                    } else {
                        android.util.Log.e("EditorViewModel", "Предпросмотр вернул null")
                        _uiState.value = _uiState.value.copy(
                            error = "Не удалось применить фильтр"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Ошибка при применении фильтра: ${e.message}", e)
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Ошибка применения фильтра"
                    )
                }
            }
        }
    }
    
    /**
     * Применить фильтр с сохранением в файл (для финального результата).
     */
    fun applyFilter(filterType: FilterType, intensity: Float? = null) {
        val currentImage = _uiState.value.imageData ?: return
        val filterIntensity = intensity ?: _uiState.value.filterIntensity
        val currentSelectedFilter = _uiState.value.selectedFilter
        
        // Отменяем предыдущий запрос, если он еще выполняется
        currentFilterJob?.cancel()
        currentPreviewJob?.cancel()
        
        currentFilterJob = viewModelScope.launch {
            // Сохраняем выбранный фильтр перед началом обработки
            val savedSelectedFilter = currentSelectedFilter ?: filterType
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                selectedFilter = savedSelectedFilter // Явно сохраняем выбранный фильтр
            )
            
            try {
                val result = processImageUseCase.invoke(currentImage, filterType, filterIntensity)
                // Проверяем, что корутина не была отменена
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        processedResult = result,
                        previewBitmap = null, // Очищаем предпросмотр после сохранения
                        isLoading = false,
                        selectedFilter = savedSelectedFilter // Сохраняем выбранный фильтр
                    )
                }
            } catch (e: Exception) {
                // Проверяем, что корутина не была отменена
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message,
                        selectedFilter = savedSelectedFilter // Сохраняем выбранный фильтр даже при ошибке
                    )
                }
            }
        }
    }
    
    /**
     * Получить или загрузить исходный Bitmap.
     * Использует кэш для избежания повторной загрузки.
     */
    private suspend fun getOrLoadOriginalBitmap(): Bitmap? {
        // Проверяем кэш
        if (cachedOriginalBitmap != null && !cachedOriginalBitmap!!.isRecycled) {
            return cachedOriginalBitmap
        }
        
        val imageData = _uiState.value.imageData ?: return null
        
        // Используем репозиторий для загрузки Bitmap
        return try {
            val bitmap = processingRepository.loadBitmapFromUri(imageData.uri)
            if (bitmap != null) {
                // Кэшируем для последующих использований
                cachedOriginalBitmap = bitmap
                android.util.Log.d("EditorViewModel", "Bitmap закэширован: ${bitmap.width}x${bitmap.height}")
            } else {
                android.util.Log.e("EditorViewModel", "Не удалось загрузить Bitmap из URI: ${imageData.uri}")
            }
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("EditorViewModel", "Ошибка загрузки Bitmap: ${e.message}", e)
            null
        }
    }
    
    fun clearFilter() {
        currentPreviewJob?.cancel()
        currentFilterJob?.cancel()
        _uiState.value = _uiState.value.copy(
            processedResult = null,
            previewBitmap = null,
            selectedFilter = null,
            filterIntensity = 0.5f
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * Состояние UI экрана редактора.
 */
data class EditorUiState(
    val imageData: ImageData? = null,
    val processedResult: ProcessingResult? = null,
    val previewBitmap: Bitmap? = null, // Быстрый предпросмотр без сохранения
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFilter: FilterType? = null,
    val filterIntensity: Float = 0.5f // Значение по умолчанию
)