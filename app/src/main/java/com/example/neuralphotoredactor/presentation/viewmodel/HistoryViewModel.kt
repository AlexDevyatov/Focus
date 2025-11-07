package com.example.neuralphotoredactor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.model.AIResult
import com.example.neuralphotoredactor.domain.usecase.DeleteProcessingResultUseCase
import com.example.neuralphotoredactor.domain.usecase.GetProcessingHistoryUseCase
import com.example.neuralphotoredactor.domain.usecase.GetProcessingResultUseCase
import com.example.neuralphotoredactor.presentation.state.HistoryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана истории обработок.
 * 
 * Управляет состоянием экрана истории: список результатов, просмотр деталей, удаление.
 * 
 * @param getProcessingHistoryUseCase Use case для получения истории обработок
 * @param getProcessingResultUseCase Use case для получения конкретного результата
 * @param deleteProcessingResultUseCase Use case для удаления результата
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getProcessingHistoryUseCase: GetProcessingHistoryUseCase,
    private val getProcessingResultUseCase: GetProcessingResultUseCase,
    private val deleteProcessingResultUseCase: DeleteProcessingResultUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    /**
     * Загружает историю обработок.
     */
    fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            getProcessingHistoryUseCase()
                .catch { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load history"
                        )
                    }
                }
                .collect { results ->
                    _state.update { 
                        it.copy(
                            results = results,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    /**
     * Выбирает результат для просмотра деталей.
     * 
     * @param result Результат обработки
     */
    fun selectResult(result: AIResult) {
        _state.update { it.copy(selectedResult = result) }
    }

    /**
     * Получает результат по идентификатору.
     * 
     * @param id Идентификатор результата
     */
    fun getResultById(id: String) {
        viewModelScope.launch {
            try {
                val result = getProcessingResultUseCase(id)
                if (result != null) {
                    _state.update { it.copy(selectedResult = result) }
                } else {
                    _state.update { it.copy(error = "Result not found") }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(error = e.message ?: "Failed to get result")
                }
            }
        }
    }

    /**
     * Удаляет результат обработки из истории.
     * 
     * @param id Идентификатор результата для удаления
     */
    fun deleteResult(id: String) {
        viewModelScope.launch {
            try {
                deleteProcessingResultUseCase(id)
                // История обновится автоматически через Flow
            } catch (e: Exception) {
                _state.update { 
                    it.copy(error = e.message ?: "Failed to delete result")
                }
            }
        }
    }

    /**
     * Очищает выбранный результат.
     */
    fun clearSelection() {
        _state.update { it.copy(selectedResult = null) }
    }

    /**
     * Очищает ошибку.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

