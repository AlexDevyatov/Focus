package com.example.neuralphotoredactor.presentation.navigation

/**
 * Sealed class для определения экранов приложения в навигации.
 * 
 * Используется с Jetpack Navigation Compose для определения маршрутов между экранами.
 * Каждый объект представляет отдельный экран приложения с уникальным route.
 * 
 * @property route Уникальный строковый идентификатор маршрута экрана
 */
sealed class Screen(val route: String) {
    /** Экран выбора изображения из галереи или камеры */
    object Gallery : Screen("gallery")
    
    /** Экран редактора изображений с панелью инструментов и предпросмотром */
    object Editor : Screen("editor")
    
    /** Экран со списком доступных AI фильтров и эффектов */
    object Filters : Screen("filters")
    
    /** Экран истории обработок с возможностью сравнения результатов */
    object History : Screen("history")
    
    /** Экран настроек приложения (качество обработки, API ключи и т.д.) */
    object Settings : Screen("settings")
}

