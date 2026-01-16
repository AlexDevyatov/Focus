package com.example.neuralphotoredactor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения метаинформации о нейросетевых моделях.
 *
 * Содержит информацию о доступных моделях искусственного интеллекта
 * для обработки изображений.
 *
 * @param id Уникальный идентификатор модели
 * @param name Название модели
 * @param type Тип модели (стилизация, супер-разрешение и т.д.)
 * @param version Версия модели
 * @param filePath Путь к файлу модели в хранилище устройства
 * @param fileSize Размер модели в байтах
 * @param isActive Флаг активности модели
 * @param compatibilityLevel Уровень совместимости с различными устройствами
 */
@Entity(tableName = "neural_models")
data class NeuralModelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // Тип модели: "style_transfer", "super_resolution", "filter" и т.д.
    val version: String,
    val filePath: String,
    val fileSize: Long,
    val isActive: Boolean = true,
    val compatibilityLevel: String, // JSON строка или enum значение
)
