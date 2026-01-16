package com.example.neuralphotoredactor.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel для главного экрана приложения.
 */
@HiltViewModel
class MainViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(MainUiState())
        val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

        private var navigator: MainNavigator? = null

        /**
         * Установить навигатор для выполнения навигации.
         */
        fun setNavigator(navigator: MainNavigator) {
            this.navigator = navigator
        }

        /**
         * Очистить навигатор при уничтожении ViewModel.
         */
        override fun onCleared() {
            super.onCleared()
            navigator = null
        }

        /**
         * Навигация на экран галереи.
         */
        fun navigateToGallery() {
            navigator?.navigateToGallery()
        }

        /**
         * Навигация на экран обработанных изображений (ИИ).
         */
        fun navigateToProcessedImages() {
            navigator?.navigateToProcessedImages()
        }

        /**
         * Навигация на экран AI фильтров.
         */
        fun navigateToAiFilters() {
            navigator?.navigateToAiFilters()
        }

        /**
         * Навигация на экран истории.
         */
        fun navigateToHistory() {
            navigator?.navigateToHistory()
        }

        /**
         * Открытие камеры.
         */
        fun openCamera() {
            navigator?.openCamera()
        }
    }

/**
 * Состояние UI главного экрана.
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)
