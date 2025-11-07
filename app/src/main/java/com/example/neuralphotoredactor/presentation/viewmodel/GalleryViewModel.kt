package com.example.neuralphotoredactor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.usecase.CaptureImageFromCameraUseCase
import com.example.neuralphotoredactor.domain.usecase.GetAllImagesUseCase
import com.example.neuralphotoredactor.domain.usecase.GetImageFromGalleryUseCase
import com.example.neuralphotoredactor.presentation.state.GalleryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
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

    /**
     * Загружает все изображения из галереи.
     */
    fun loadImages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Используем catch для обработки ошибок в Flow
                // Если произойдет исключение, catch перехватит его и эмитит пустой список
                // Затем take(1) возьмет первое значение (либо список изображений, либо пустой список)
                getAllImagesUseCase.getAllImages()
                    .catch { exception ->
                        // catch перехватывает исключения из upstream Flow
                        // Вместо того чтобы позволить исключению пройти дальше,
                        // мы эмитим пустой список как fallback значение
                        emit(emptyList())
                    }
                    .take(1) // Берем только первое значение и завершаем Flow
                    .collect { images ->
                        // Обновляем состояние с полученными изображениями
                        _state.update { 
                            it.copy(
                                images = images,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                // Обрабатываем исключения, которые могут возникнуть вне Flow
                // (например, при создании Flow или при работе с корутинами)
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load images"
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
            val image = getImageFromGalleryUseCase.getImageFromGallery()
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
            val image = captureImageFromCameraUseCase.captureImageFromCamera()
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
