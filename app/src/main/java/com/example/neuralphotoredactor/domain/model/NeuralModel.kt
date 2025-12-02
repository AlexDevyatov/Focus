package com.example.neuralphotoredactor.domain.model

/**
 * Domain модель нейросетевой модели.
 * 
 * Содержит метаинформацию о доступных моделях искусственного интеллекта
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
data class NeuralModel(
    val id: Long = 0,
    val name: String,
    val type: ModelType,
    val version: String,
    val filePath: String,
    val fileSize: Long,
    val isActive: Boolean = true,
    val compatibilityLevel: CompatibilityLevel
)

/**
 * Тип нейросетевой модели.
 */
enum class ModelType {
    STYLE_TRANSFER,      // Стилизация
    SUPER_RESOLUTION,    // Супер-разрешение
    FILTER,             // Фильтр
    ENHANCEMENT,         // Улучшение качества
    OTHER                // Другое
}

/**
 * Уровень совместимости модели с устройствами.
 */
enum class CompatibilityLevel {
    LOW,      // Низкая совместимость
    MEDIUM,   // Средняя совместимость
    HIGH,     // Высокая совместимость
    UNIVERSAL // Универсальная совместимость
}

