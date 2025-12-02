package com.example.neuralphotoredactor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.usecase.GetAllImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    
    private var loadJob: kotlinx.coroutines.Job? = null
    
    /**
     * Загрузить изображения из галереи.
     * Должен вызываться только после предоставления разрешения на доступ к галерее.
     */
    fun loadImages() {
        // Отменяем предыдущую загрузку, если она есть
        loadJob?.cancel()
        
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Используем first() для одноразового Flow
                val imageList = getAllImagesUseCase.invoke.first()
                _uiState.value = _uiState.value.copy(
                    images = imageList,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки изображений"
                )
            }
        }
    }
    
    /**
     * Остановить загрузку изображений.
     */
    fun stopLoading() {
        loadJob?.cancel()
        loadJob = null
        _uiState.value = _uiState.value.copy(isLoading = false)
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

