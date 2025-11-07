package com.example.neuralphotoredactor.presentation.state

/**
 * Состояние UI для экрана настроек.
 * 
 * @param processingQuality Качество обработки (1-100)
 * @param useGpu Использовать ли GPU для обработки
 * @param apiKey API ключ для облачных сервисов
 * @param isApiKeyVisible Видимость API ключа (для безопасности)
 */
data class SettingsState(
    val processingQuality: Int = 90,
    val useGpu: Boolean = true,
    val apiKey: String = "",
    val isApiKeyVisible: Boolean = false
)

