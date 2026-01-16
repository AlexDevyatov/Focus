package com.example.neuralphotoredactor.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.usecase.GetProcessedImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана обработанных изображений.
 */
@HiltViewModel
class ProcessedImagesViewModel
    @Inject
    constructor(
        private val getProcessedImagesUseCase: GetProcessedImagesUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProcessedImagesUiState())
        val uiState: StateFlow<ProcessedImagesUiState> = _uiState.asStateFlow()

        private var loadJob: kotlinx.coroutines.Job? = null

        init {
            loadProcessedImages()
        }

        /**
         * Загрузить обработанные изображения.
         */
        fun loadProcessedImages() {
            // Отменяем предыдущую загрузку, если она есть
            loadJob?.cancel()

            loadJob =
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    try {
                        val images = getProcessedImagesUseCase.invoke()
                        _uiState.value =
                            _uiState.value.copy(
                                images = images,
                                isLoading = false,
                            )
                    } catch (e: Exception) {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                error = e.message,
                            )
                    }
                }
        }

        /**
         * Обновить список обработанных изображений (pull to refresh).
         */
        fun refreshImages() {
            // Отменяем предыдущую загрузку, если она есть
            loadJob?.cancel()

            loadJob =
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
                    try {
                        val images = getProcessedImagesUseCase.invoke()
                        _uiState.value =
                            _uiState.value.copy(
                                images = images,
                                isRefreshing = false,
                            )
                    } catch (e: Exception) {
                        _uiState.value =
                            _uiState.value.copy(
                                isRefreshing = false,
                                error = e.message,
                            )
                    }
                }
        }
    }

/**
 * Состояние UI экрана обработанных изображений.
 */
data class ProcessedImagesUiState(
    val images: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)
