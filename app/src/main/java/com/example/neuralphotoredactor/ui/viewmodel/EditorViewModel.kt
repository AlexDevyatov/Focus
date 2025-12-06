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
    
    // Callback для обновления галереи после сохранения
    var onImageSaved: (() -> Unit)? = null
    
    // Callback для навигации после успешного применения фильтров
    var onNavigateToProcessed: (() -> Unit)? = null
    
    val availableFilters = FilterType.entries
    
    fun setImage(imageData: ImageData) {
        _uiState.value = _uiState.value.copy(
            imageData = imageData,
            processedResult = null,
            previewBitmap = null,
            selectedFilters = emptyList(),
            currentFilterIntensity = 0.5f
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
    
    fun toggleFilter(filterType: FilterType) {
        val currentFilters = _uiState.value.selectedFilters.toMutableList()
        val existingIndex = currentFilters.indexOfFirst { it.first == filterType }
        
        if (existingIndex >= 0) {
            // Удаляем фильтр, если он уже выбран
            currentFilters.removeAt(existingIndex)
        } else {
            // Добавляем фильтр с интенсивностью по умолчанию
            currentFilters.add(Pair(filterType, 0.5f))
        }
        
        _uiState.value = _uiState.value.copy(selectedFilters = currentFilters)
        // Обновляем предпросмотр
        previewFilters(currentFilters)
    }
    
    fun updateFilterIntensity(filterType: FilterType, intensity: Float) {
        val currentFilters = _uiState.value.selectedFilters.toMutableList()
        val existingIndex = currentFilters.indexOfFirst { it.first == filterType }
        
        if (existingIndex >= 0) {
            // Обновляем интенсивность существующего фильтра
            currentFilters[existingIndex] = Pair(filterType, intensity)
        } else {
            // Добавляем новый фильтр с указанной интенсивностью
            currentFilters.add(Pair(filterType, intensity))
        }
        
        _uiState.value = _uiState.value.copy(
            selectedFilters = currentFilters,
            currentFilterIntensity = intensity
        )
        // Обновляем предпросмотр
        previewFilters(currentFilters)
    }
    
    /**
     * Быстрый предпросмотр множественных фильтров без сохранения в файл.
     * Используется для отображения результата в реальном времени.
     */
    private fun previewFilters(filters: List<Pair<FilterType, Float>>) {
        // Отменяем предыдущий предпросмотр
        currentPreviewJob?.cancel()
        
        if (filters.isEmpty()) {
            _uiState.value = _uiState.value.copy(previewBitmap = null)
            return
        }
        
        currentPreviewJob = viewModelScope.launch {
            // Небольшая задержка для debounce
            delay(100)
            
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
            
            try {
                android.util.Log.d("EditorViewModel", "Применяем ${filters.size} фильтров для предпросмотра")
                val previewBitmap = processingRepository.previewFilters(
                    originalBitmap,
                    filters.map { it.first to it.second }
                )
                
                if (isActive) {
                    if (previewBitmap != null) {
                        android.util.Log.d("EditorViewModel", "Предпросмотр создан: ${previewBitmap.width}x${previewBitmap.height}")
                        _uiState.value = _uiState.value.copy(previewBitmap = previewBitmap)
                    } else {
                        android.util.Log.e("EditorViewModel", "Предпросмотр вернул null")
                        _uiState.value = _uiState.value.copy(
                            error = "Не удалось применить фильтры"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Ошибка при применении фильтров: ${e.message}", e)
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Ошибка применения фильтров"
                    )
                }
            }
        }
    }
    
    /**
     * Применить выбранные фильтры с сохранением в файл (для финального результата).
     */
    fun applyFilters() {
        val currentImage = _uiState.value.imageData ?: return
        val selectedFilters = _uiState.value.selectedFilters
        
        if (selectedFilters.isEmpty()) {
            android.util.Log.w("EditorViewModel", "Нет выбранных фильтров для применения")
            return
        }
        
        // Отменяем предыдущий запрос, если он еще выполняется
        currentFilterJob?.cancel()
        currentPreviewJob?.cancel()
        
        currentFilterJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                val result = processingRepository.processImageWithFilters(
                    currentImage,
                    selectedFilters.map { it.first to it.second }
                )
                
                // Проверяем, что корутина не была отменена и результат успешен
                if (isActive && result != null) {
                    _uiState.value = _uiState.value.copy(
                        processedResult = result,
                        previewBitmap = null, // Очищаем предпросмотр после сохранения
                        isLoading = false
                    )
                    
                    // Обновляем галерею после успешного сохранения
                    onImageSaved?.invoke()
                    
                    // Переходим на экран обработанных изображений
                    onNavigateToProcessed?.invoke()
                } else if (isActive && result == null) {
                    // Если результат null, показываем ошибку
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не удалось применить фильтры"
                    )
                }
            } catch (e: Exception) {
                // Проверяем, что корутина не была отменена
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
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
    
    fun clearFilters() {
        currentPreviewJob?.cancel()
        currentFilterJob?.cancel()
        _uiState.value = _uiState.value.copy(
            processedResult = null,
            previewBitmap = null,
            selectedFilters = emptyList(),
            currentFilterIntensity = 0.5f
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
    val selectedFilters: List<Pair<FilterType, Float>> = emptyList(), // Список выбранных фильтров с интенсивностями
    val currentFilterIntensity: Float = 0.5f // Интенсивность для текущего редактируемого фильтра
)