package com.example.neuralphotoredactor.ui.viewmodel

/**
 * Интерфейс для навигации из ViewModel.
 * Реализуется в Activity/Fragment для выполнения навигации.
 */
interface MainNavigator {
    /**
     * Навигация на экран галереи.
     */
    fun navigateToGallery()

    /**
     * Навигация на экран обработанных изображений (ИИ).
     */
    fun navigateToProcessedImages()

    /**
     * Навигация на экран AI фильтров.
     */
    fun navigateToAiFilters()

    /**
     * Навигация на экран истории.
     */
    fun navigateToHistory()

    /**
     * Открытие камеры.
     */
    fun openCamera()
}
