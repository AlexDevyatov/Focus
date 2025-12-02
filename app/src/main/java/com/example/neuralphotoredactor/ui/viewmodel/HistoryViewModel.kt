package com.example.neuralphotoredactor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.usecase.GetProcessingHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана истории.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getProcessingHistoryUseCase: GetProcessingHistoryUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
    }
    
    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                getProcessingHistoryUseCase().collect { history ->
                    _uiState.value = _uiState.value.copy(
                        history = history,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки истории"
                )
            }
        }
    }
}

/**
 * Состояние UI экрана истории.
 */
data class HistoryUiState(
    val history: List<ProcessingResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

