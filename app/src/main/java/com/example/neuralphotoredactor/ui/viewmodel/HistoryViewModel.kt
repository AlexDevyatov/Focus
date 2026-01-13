package com.example.neuralphotoredactor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.model.ProcessingOperation
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.usecase.GetFilterNameByIdUseCase
import com.example.neuralphotoredactor.domain.usecase.GetOperationByIdUseCase
import com.example.neuralphotoredactor.domain.usecase.GetOperationsByHistoryIdUseCase
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
    private val getProcessingHistoryUseCase: GetProcessingHistoryUseCase,
    private val getOperationByIdUseCase: GetOperationByIdUseCase,
    private val getOperationsByHistoryIdUseCase: GetOperationsByHistoryIdUseCase,
    private val getFilterNameByIdUseCase: GetFilterNameByIdUseCase
) : ViewModel() {
    
    /**
     * Получить детальную информацию об операции обработки (deprecated).
     * Используйте getOperationsByHistoryId для получения всех операций записи истории.
     */
    suspend fun getOperationDetails(operationId: Long?): ProcessingOperation? {
        return if (operationId != null) {
            getOperationByIdUseCase.invoke(operationId)
        } else {
            null
        }
    }
    
    /**
     * Получить все операции для записи истории обработки.
     * 
     * @param historyId ID записи в истории обработки
     * @return Список операций обработки
     */
    fun getOperationsByHistoryId(historyId: Long) = getOperationsByHistoryIdUseCase.invoke(historyId)
    
    /**
     * Получить название операции по filterId.
     * 
     * @param filterId ID фильтра или операции редактирования
     * @return Название операции или null, если не найдено
     */
    suspend fun getOperationName(filterId: Long): String? {
        return getFilterNameByIdUseCase.invoke(filterId)
    }
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
    }
    
    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                getProcessingHistoryUseCase.invoke.collect { history ->
                    _uiState.value = _uiState.value.copy(
                        history = history,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
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

