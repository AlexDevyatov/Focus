package com.example.neuralphotoredactor.data.mapper

import com.example.neuralphotoredactor.domain.enums.FilterType

/**
 * Маппер для преобразования FilterType enum в строку и обратно.
 * 
 * Используется для сериализации/десериализации типов фильтров
 * при работе с API и базой данных.
 */
object FilterTypeMapper {
    /**
     * Преобразует FilterType в строку для API/базы данных.
     * 
     * @param filterType Тип фильтра
     * @return Строковое представление типа фильтра
     */
    fun toString(filterType: FilterType): String {
        return filterType.name
    }

    /**
     * Преобразует строку в FilterType.
     * 
     * @param value Строковое представление типа фильтра
     * @return FilterType или null, если строка не соответствует ни одному типу
     */
    fun fromString(value: String): FilterType? {
        return try {
            FilterType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

