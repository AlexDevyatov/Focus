package com.example.neuralphotoredactor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingRequest
import com.example.neuralphotoredactor.domain.usecase.ProcessImageUseCase
import com.example.neuralphotoredactor.presentation.state.ImageEditorState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана редактора изображений.
 * 
 * Управляет состоянием экрана редактора: текущее изображение, обработка, результаты.
 * 
 * @param processImageUseCase Use case для обработки изображения
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val processImageUseCase: ProcessImageUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ImageEditorState())
    val state: StateFlow<ImageEditorState> = _state.asStateFlow()

    /**
     * Устанавливает текущее изображение для редактирования.
     * 
     * @param imageData Изображение для редактирования
     */
    fun setCurrentImage(imageData: ImageData) {
        _state.update { it.copy(currentImage = imageData, error = null) }
    }

    /**
     * Обрабатывает текущее изображение с указанным фильтром.
     * 
     * @param filterType Тип фильтра для применения
     * @param parameters Дополнительные параметры обработки
     */
    fun processImage(
        filterType: com.example.neuralphotoredactor.domain.enums.FilterType,
        parameters: Map<String, Any> = emptyMap()
    ) {
        val currentImage = _state.value.currentImage
        if (currentImage == null) {
            _state.update { it.copy(error = "No image selected") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val request = ProcessingRequest(
                    imageData = currentImage,
                    filterType = filterType,
                    parameters = parameters
                )
                
                val result = processImageUseCase.processImage(request)
                
                if (result.processedImage != null) {
                    _state.update { 
                        it.copy(
                            currentImage = result.processedImage,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = result.error ?: "Processing failed"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to process image"
                    )
                }
            }
        }
    }

    /**
     * Очищает текущее изображение.
     */
    fun clearImage() {
        _state.update { ImageEditorState() }
    }

    /**
     * Очищает ошибку.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

