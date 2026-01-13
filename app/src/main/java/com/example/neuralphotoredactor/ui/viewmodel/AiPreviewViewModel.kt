package com.example.neuralphotoredactor.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана предпросмотра AI фильтра.
 */
@HiltViewModel
class AiPreviewViewModel @Inject constructor(
    private val processingRepository: ProcessingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AiPreviewUiState())
    val uiState: StateFlow<AiPreviewUiState> = _uiState.asStateFlow()
    
    // Callback для обновления галереи после сохранения
    var onImageSaved: (() -> Unit)? = null
    
    /**
     * Установить изображение и фильтр для обработки.
     */
    fun setImageAndFilter(imageData: ImageData, filterType: FilterType) {
        // Сбрасываем состояние перед новой обработкой
        _uiState.value = AiPreviewUiState(
            imageData = imageData,
            filterType = filterType,
            isLoading = true,
            error = null,
            originalBitmap = null,
            processedBitmap = null,
            progress = 0f,
            showOriginal = false
        )
        
        // Запускаем обработку
        processImage()
    }
    
    /**
     * Обработать изображение с применением фильтра.
     */
    private fun processImage() {
        val currentState = _uiState.value
        val imageData = currentState.imageData ?: return
        val filterType = currentState.filterType ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null, progress = 0f)
                
                // Симулируем прогресс загрузки (0-20%)
                var progress = 0f
                while (progress < 0.2f) {
                    kotlinx.coroutines.delay(30)
                    progress += 0.02f
                    _uiState.value = _uiState.value.copy(progress = progress.coerceAtMost(0.2f))
                }
                
                // Загружаем Bitmap из URI
                val originalBitmap = processingRepository.loadBitmapFromUri(imageData.uri)
                if (originalBitmap == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не удалось загрузить изображение",
                        progress = 0f
                    )
                    return@launch
                }
                
                // Сохраняем исходное изображение для сравнения
                _uiState.value = _uiState.value.copy(originalBitmap = originalBitmap)
                
                // Симулируем прогресс обработки (20-95%) параллельно с реальной обработкой
                val progressJob = launch {
                    var currentProgress = 0.2f
                    while (currentProgress < 0.95f) {
                        kotlinx.coroutines.delay(100)
                        currentProgress += 0.05f
                        _uiState.value = _uiState.value.copy(progress = currentProgress.coerceAtMost(0.95f))
                    }
                }
                
                // Применяем фильтр
                val processedBitmap = processingRepository.previewFilters(
                    originalBitmap,
                    listOf(filterType to null) // Нейросетевые фильтры без intensity
                )
                
                // Отменяем симуляцию прогресса
                progressJob.cancel()
                
                // Завершаем прогресс (95-100%)
                _uiState.value = _uiState.value.copy(progress = 1f)
                kotlinx.coroutines.delay(100)
                
                if (processedBitmap != null) {
                    _uiState.value = _uiState.value.copy(
                        processedBitmap = processedBitmap,
                        isLoading = false,
                        error = null,
                        progress = 1f
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не удалось применить фильтр",
                        progress = 0f
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка обработки изображения",
                    progress = 0f
                )
            }
        }
    }
    
    /**
     * Сохранить обработанное изображение.
     */
    fun saveProcessedImage() {
        val currentState = _uiState.value
        val processedBitmap = currentState.processedBitmap
        val imageData = currentState.imageData
        val filterType = currentState.filterType
        
        if (processedBitmap == null || imageData == null || filterType == null) {
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                
                val filterName = filterType.name
                val timestamp = System.currentTimeMillis()
                val fileName = "ai_processed_${timestamp}_${filterName}.jpg"
                
                val editSettings = mapOf(
                    "filters" to listOf(filterName),
                    "intensities" to emptyList<Float>()
                )
                
                val uri = processingRepository.saveEditedImageToGallery(
                    processedBitmap,
                    fileName,
                    originalUri = imageData.uri,
                    filterType = filterName,
                    editSettings = editSettings
                )
                
                if (uri != null) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        savedResult = ProcessingResult(
                            originalUri = imageData.uri,
                            processedUri = uri,
                            filterType = filterName
                        )
                    )
                    
                    // Обновляем галерею
                    onImageSaved?.invoke()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Не удалось сохранить изображение"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Ошибка сохранения изображения"
                )
            }
        }
    }
    
    /**
     * Переключить отображение между исходным и обработанным изображением.
     */
    fun toggleImageComparison() {
        _uiState.value = _uiState.value.copy(
            showOriginal = !_uiState.value.showOriginal
        )
    }
    
    /**
     * Очистить состояние.
     */
    fun clearState() {
        _uiState.value = AiPreviewUiState(
            imageData = null,
            filterType = null,
            isLoading = false,
            isSaving = false,
            error = null,
            originalBitmap = null,
            processedBitmap = null,
            savedResult = null,
            progress = 0f,
            showOriginal = false
        )
    }
}

/**
 * Состояние UI экрана предпросмотра AI фильтра.
 */
data class AiPreviewUiState(
    val imageData: ImageData? = null,
    val filterType: FilterType? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val originalBitmap: Bitmap? = null, // Исходное изображение для сравнения
    val processedBitmap: Bitmap? = null,
    val savedResult: ProcessingResult? = null,
    val progress: Float = 0f, // Прогресс обработки от 0.0 до 1.0
    val showOriginal: Boolean = false // Показывать исходное изображение (false = обработанное)
)

