package com.example.neuralphotoredactor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для хранения информации о фильтрах.
 * 
 * Содержит названия фильтров и ссылку на нейронную модель (если фильтр использует модель).
 * 
 * @param id Уникальный идентификатор фильтра
 * @param name Название фильтра (соответствует FilterType enum)
 * @param modelId Ссылка на нейросетевую модель (Foreign Key, может быть null для алгоритмических фильтров)
 */
@Entity(
    tableName = "filters",
    foreignKeys = [
        ForeignKey(
            entity = NeuralModelEntity::class,
            parentColumns = ["id"],
            childColumns = ["modelId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["modelId"]),
        Index(value = ["name"], unique = true)
    ]
)
data class FilterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // Название фильтра (например, "GAUSSIAN_BLUR", "STYLE_TRANSFER")
    val modelId: Long? = null // Ссылка на модель, если фильтр использует нейросеть
)

