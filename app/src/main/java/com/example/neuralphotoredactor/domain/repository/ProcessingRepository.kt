package com.example.neuralphotoredactor.domain.repository

import com.example.neuralphotoredactor.domain.enums.EditType
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для обработки изображений.
 * 
 * Предоставляет методы для применения фильтров и эффектов к изображениям
 * с использованием TensorFlow Lite моделей.
 */
interface ProcessingRepository {
    /**
     * Обработать изображение с применением указанного фильтра.
     * 
     * @param imageData Исходное изображение
     * @param filterType Тип фильтра для применения
     * @param intensity Интенсивность фильтра (0.0 - 1.0), null для значения по умолчанию
     * @return Результат обработки или null в случае ошибки
     */
    suspend fun processImage(
        imageData: ImageData,
        filterType: FilterType,
        intensity: Float? = null
    ): ProcessingResult?
    
    /**
     * Получить историю обработок.
     * 
     * @return Flow со списком результатов обработки
     */
    fun getProcessingHistory(): Flow<List<ProcessingResult>>
    
    /**
     * Удалить результат обработки.
     * 
     * @param result Результат для удаления
     */
    suspend fun deleteProcessingResult(result: ProcessingResult)
    
    /**
     * Быстрый предпросмотр фильтра без сохранения в файл.
     * Используется для отображения результата в реальном времени.
     * 
     * @param bitmap Исходное изображение (Bitmap)
     * @param filterType Тип фильтра для применения
     * @param intensity Интенсивность фильтра (0.0 - 1.0), null для значения по умолчанию
     * @return Обработанное изображение (Bitmap) или null в случае ошибки
     */
    suspend fun previewFilter(
        bitmap: android.graphics.Bitmap,
        filterType: FilterType,
        intensity: Float? = null
    ): android.graphics.Bitmap?
    
    /**
     * Быстрый предпросмотр множественных фильтров без сохранения в файл.
     * 
     * @param bitmap Исходное изображение (Bitmap)
     * @param filters Список фильтров с их интенсивностями
     * @return Обработанное изображение (Bitmap) или null в случае ошибки
     */
    suspend fun previewFilters(
        bitmap: android.graphics.Bitmap,
        filters: List<Pair<FilterType, Float?>>
    ): android.graphics.Bitmap?
    
    /**
     * Обработать изображение с применением нескольких фильтров последовательно.
     * 
     * @param imageData Исходное изображение
     * @param filters Список фильтров с их интенсивностями
     * @return Результат обработки или null в случае ошибки
     */
    suspend fun processImageWithFilters(
        imageData: ImageData,
        filters: List<Pair<FilterType, Float?>>
    ): ProcessingResult?
    
    /**
     * Загрузить Bitmap из URI.
     * Используется для кэширования исходного изображения.
     * 
     * @param uri URI изображения
     * @return Bitmap или null в случае ошибки
     */
    suspend fun loadBitmapFromUri(uri: android.net.Uri): android.graphics.Bitmap?
    
    /**
     * Применить редактирование к изображению.
     * 
     * @param bitmap Исходное изображение
     * @param editType Тип редактирования
     * @param value Значение для редактирования (для яркости, контраста, цветового баланса)
     * @param cropRect Прямоугольник кадрирования (left, top, right, bottom) в пикселях
     * @return Обработанное изображение или null в случае ошибки
     */
    suspend fun applyEdit(
        bitmap: android.graphics.Bitmap,
        editType: EditType,
        value: Float = 0f,
        cropRect: android.graphics.Rect? = null
    ): android.graphics.Bitmap?
    
    /**
     * Сохранить отредактированное изображение в галерею.
     * 
     * @param bitmap Изображение для сохранения
     * @param fileName Имя файла
     * @param originalUri URI исходного изображения (опционально, для истории)
     * @param filterType Тип фильтра или редактирования (опционально, для истории)
     * @return URI сохраненного файла или null в случае ошибки
     */
    suspend fun saveEditedImageToGallery(
        bitmap: android.graphics.Bitmap,
        fileName: String,
        originalUri: android.net.Uri? = null,
        filterType: String? = null
    ): android.net.Uri?
}

