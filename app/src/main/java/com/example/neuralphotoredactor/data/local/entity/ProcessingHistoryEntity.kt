package com.example.neuralphotoredactor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity для хранения истории обработок изображений в локальной базе данных.
 * 
 * Представляет запись об одной обработке изображения AI алгоритмами.
 * Сохраняет информацию об исходном и обработанном изображении, типе фильтра,
 * статусе обработки и времени выполнения.
 * 
 * @param id Уникальный идентификатор записи (первичный ключ)
 * @param originalImageUri URI исходного изображения в виде строки
 * @param processedImageUri URI обработанного изображения (null, если обработка не завершена)
 * @param filterType Тип примененного фильтра в виде строки
 * @param status Статус обработки в виде строки
 * @param error Сообщение об ошибке (null, если обработка успешна)
 * @param processingTime Время обработки в миллисекундах
 * @param timestamp Временная метка создания записи
 */
@Entity(tableName = "processing_history")
data class ProcessingHistoryEntity(
    @PrimaryKey
    val id: String,
    val originalImageUri: String,
    val processedImageUri: String?,
    val filterType: String,
    val status: String,
    val error: String?,
    val processingTime: Long,
    val timestamp: Long
)

