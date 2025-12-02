package com.example.neuralphotoredactor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.usecase.ProcessImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val processImageUseCase: ProcessImageUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    
    val availableFilters = FilterType.entries
    
    fun setImage(imageData: ImageData) {
        _uiState.value = _uiState.value.copy(
            imageData = imageData,
            processedResult = null
        )
    }
    
    fun applyFilter(filterType: FilterType) {
        val currentImage = _uiState.value.imageData ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                val result = processImageUseCase.invoke(currentImage, filterType)
                _uiState.value = _uiState.value.copy(
                    processedResult = result,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка обработки изображения"
                )
            }
        }
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
    val isLoading: Boolean = false,
    val error: String? = null
)

