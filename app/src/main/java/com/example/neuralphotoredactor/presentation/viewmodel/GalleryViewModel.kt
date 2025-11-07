package com.example.neuralphotoredactor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.usecase.CaptureImageFromCameraUseCase
import com.example.neuralphotoredactor.domain.usecase.GetAllImagesUseCase
import com.example.neuralphotoredactor.domain.usecase.GetImageFromGalleryUseCase
import com.example.neuralphotoredactor.presentation.state.GalleryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана галереи.
 * 
 * Управляет состоянием экрана выбора изображения из галереи или камеры.
 * 
 * @param getAllImagesUseCase Use case для получения всех изображений
 * @param getImageFromGalleryUseCase Use case для выбора изображения из галереи
 * @param captureImageFromCameraUseCase Use case для захвата изображения с камеры
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val getAllImagesUseCase: GetAllImagesUseCase,
    private val getImageFromGalleryUseCase: GetImageFromGalleryUseCase,
    private val captureImageFromCameraUseCase: CaptureImageFromCameraUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(GalleryState())
    val state: StateFlow<GalleryState> = _state.asStateFlow()

    init {
        loadImages()
    }

    /**
     * Загружает все изображения из галереи.
     */
    fun loadImages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            getAllImagesUseCase()
                .catch { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load images"
                        )
                    }
                }
                .collect { images ->
                    _state.update { 
                        it.copy(
                            images = images,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    /**
     * Открывает диалог выбора изображения из галереи.
     * 
     * @return ImageData выбранного изображения или null
     */
    suspend fun pickImageFromGallery(): com.example.neuralphotoredactor.domain.model.ImageData? {
        return try {
            _state.update { it.copy(isLoading = true, error = null) }
            val image = getImageFromGalleryUseCase()
            _state.update { it.copy(isLoading = false) }
            image
        } catch (e: Exception) {
            _state.update { 
                it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to pick image"
                )
            }
            null
        }
    }

    /**
     * Захватывает изображение с камеры.
     * 
     * @return ImageData захваченного изображения или null
     */
    suspend fun captureImage(): com.example.neuralphotoredactor.domain.model.ImageData? {
        return try {
            _state.update { it.copy(isLoading = true, error = null) }
            val image = captureImageFromCameraUseCase()
            _state.update { it.copy(isLoading = false) }
            image
        } catch (e: Exception) {
            _state.update { 
                it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to capture image"
                )
            }
            null
        }
    }

    /**
     * Очищает ошибку.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
