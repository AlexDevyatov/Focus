package com.example.neuralphotoredactor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neuralphotoredactor.domain.model.FilterPreset
import com.example.neuralphotoredactor.domain.usecase.GetAllFiltersUseCase
import com.example.neuralphotoredactor.domain.usecase.GetFilterByIdUseCase
import com.example.neuralphotoredactor.presentation.state.FiltersState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана фильтров.
 * 
 * Управляет состоянием экрана со списком доступных AI фильтров.
 * 
 * @param getAllFiltersUseCase Use case для получения всех фильтров
 * @param getFilterByIdUseCase Use case для получения фильтра по ID
 */
@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val getAllFiltersUseCase: GetAllFiltersUseCase,
    private val getFilterByIdUseCase: GetFilterByIdUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FiltersState())
    val state: StateFlow<FiltersState> = _state.asStateFlow()

    init {
        loadFilters()
    }

    /**
     * Загружает все доступные фильтры.
     */
    fun loadFilters() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            getAllFiltersUseCase()
                .catch { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load filters"
                        )
                    }
                }
                .collect { filters ->
                    _state.update { 
                        it.copy(
                            filters = filters,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    /**
     * Выбирает фильтр для применения.
     * 
     * @param filterPreset Выбранный фильтр
     */
    fun selectFilter(filterPreset: FilterPreset) {
        _state.update { it.copy(selectedFilter = filterPreset) }
    }

    /**
     * Получает фильтр по идентификатору.
     * 
     * @param id Идентификатор фильтра
     */
    fun getFilterById(id: String) {
        viewModelScope.launch {
            try {
                val filter = getFilterByIdUseCase(id)
                if (filter != null) {
                    _state.update { it.copy(selectedFilter = filter) }
                } else {
                    _state.update { it.copy(error = "Filter not found") }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(error = e.message ?: "Failed to get filter")
                }
            }
        }
    }

    /**
     * Очищает выбранный фильтр.
     */
    fun clearSelection() {
        _state.update { it.copy(selectedFilter = null) }
    }

    /**
     * Очищает ошибку.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

