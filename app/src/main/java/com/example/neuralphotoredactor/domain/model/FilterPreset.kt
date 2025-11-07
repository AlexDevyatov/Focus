package com.example.neuralphotoredactor.domain.model

import com.example.neuralphotoredactor.domain.enums.FilterType

/**
 * Модель предустановки фильтра для отображения в UI.
 * 
 * Представляет информацию о доступном фильтре: его название, описание,
 * миниатюру и тип обработки (on-device или cloud-based).
 * 
 * @param id Уникальный идентификатор предустановки фильтра
 * @param name Название фильтра для отображения пользователю
 * @param type Тип фильтра из перечисления FilterType
 * @param thumbnailUri URI миниатюры фильтра для предпросмотра (опционально)
 * @param description Описание фильтра и его эффекта (опционально)
 * @param isOnDevice Флаг, указывающий, обрабатывается ли фильтр локально на устройстве
 *                   (true) или через облачный API (false)
 */
data class FilterPreset(
    val id: String,
    val name: String,
    val type: FilterType,
    val thumbnailUri: String? = null,
    val description: String? = null,
    val isOnDevice: Boolean = false
)

