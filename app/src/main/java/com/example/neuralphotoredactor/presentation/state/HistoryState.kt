package com.example.neuralphotoredactor.presentation.state

import com.example.neuralphotoredactor.domain.model.AIResult

/**
 * Состояние UI для экрана истории обработок.
 * 
 * @param results Список всех результатов обработки
 * @param selectedResult Выбранный результат для просмотра деталей
 * @param isLoading Флаг загрузки истории
 * @param error Сообщение об ошибке
 */
data class HistoryState(
    val results: List<AIResult> = emptyList(),
    val selectedResult: AIResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

