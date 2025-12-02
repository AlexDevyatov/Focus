package com.example.neuralphotoredactor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.usecase.GetAllImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана галереи.
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val getAllImagesUseCase: GetAllImagesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    
    init {
        loadImages()
    }
    
    private fun loadImages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                getAllImagesUseCase().collect { images ->
                    _uiState.value = _uiState.value.copy(
                        images = images,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки изображений"
                )
            }
        }
    }
}

/**
 * Состояние UI экрана галереи.
 */
data class GalleryUiState(
    val images: List<ImageData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

