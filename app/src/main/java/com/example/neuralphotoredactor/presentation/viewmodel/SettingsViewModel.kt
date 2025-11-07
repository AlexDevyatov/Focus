package com.example.neuralphotoredactor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.presentation.state.SettingsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel для экрана настроек.
 * 
 * Управляет настройками приложения: качество обработки, использование GPU, API ключи.
 * 
 * TODO: Добавить сохранение настроек в SharedPreferences или DataStore
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    /**
     * Обновляет качество обработки.
     * 
     * @param quality Качество обработки (1-100)
     */
    fun updateProcessingQuality(quality: Int) {
        if (quality in 1..100) {
            _state.update { it.copy(processingQuality = quality) }
        }
    }

    /**
     * Переключает использование GPU.
     * 
     * @param useGpu Использовать ли GPU
     */
    fun toggleGpuUsage(useGpu: Boolean) {
        _state.update { it.copy(useGpu = useGpu) }
    }

    /**
     * Обновляет API ключ.
     * 
     * @param apiKey API ключ для облачных сервисов
     */
    fun updateApiKey(apiKey: String) {
        _state.update { it.copy(apiKey = apiKey) }
    }

    /**
     * Переключает видимость API ключа.
     * 
     * @param isVisible Видимость API ключа
     */
    fun toggleApiKeyVisibility(isVisible: Boolean) {
        _state.update { it.copy(isApiKeyVisible = isVisible) }
    }

    /**
     * Сохраняет настройки.
     * TODO: Реализовать сохранение в SharedPreferences или DataStore
     */
    fun saveSettings() {
        // TODO: Сохранить настройки в постоянное хранилище
    }
}

