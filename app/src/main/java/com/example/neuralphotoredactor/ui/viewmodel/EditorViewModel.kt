package com.example.neuralphotoredactor.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Rect
import com.example.neuralphotoredactor.domain.enums.EditType
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.domain.model.ProcessingResult
import com.example.neuralphotoredactor.domain.repository.ProcessingRepository
import com.example.neuralphotoredactor.domain.usecase.ProcessImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана редактора.
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val processImageUseCase: ProcessImageUseCase,
    private val processingRepository: ProcessingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    
    private var currentFilterJob: Job? = null // Для отмены предыдущих запросов
    private var currentPreviewJob: Job? = null // Для отмены предыдущих предпросмотров
    private var cachedOriginalBitmap: Bitmap? = null // Кэш исходного изображения
    
    // Callback для обновления галереи после сохранения
    var onImageSaved: (() -> Unit)? = null
    
    // Callback для навигации после успешного применения фильтров
    var onNavigateToProcessed: (() -> Unit)? = null
    
    // Обычные фильтры (алгоритмические)
    val regularFilters = listOf(
        FilterType.GAUSSIAN_BLUR,
        FilterType.NOISE_REDUCTION,
        FilterType.SHARPEN,
        FilterType.VIGNETTE,
        FilterType.GRAYSCALE,
        FilterType.SEPIA
    )
    
    // Нейросетевые фильтры (требуют TFLite модели)
    val neuralFilters = listOf(
        FilterType.ENHANCE,
        FilterType.STYLE_TRANSFER,
        FilterType.DENOISE,
        FilterType.UPSCALE,
        FilterType.COLOR_CORRECTION
    )
    
    val availableFilters = FilterType.entries
    
    fun setImage(imageData: ImageData) {
        _uiState.value = _uiState.value.copy(
            imageData = imageData,
            processedResult = null,
            previewBitmap = null,
            selectedFilters = emptyList(),
            currentFilterIntensity = 0.5f
        )
        // Очищаем кэш при смене изображения
        cachedOriginalBitmap = null
        currentPreviewJob?.cancel()
        currentFilterJob?.cancel()
        
        // Предзагружаем Bitmap в фоне для быстрого доступа
        viewModelScope.launch {
            getOrLoadOriginalBitmap()
        }
    }
    
    fun toggleFilter(filterType: FilterType) {
        val currentFilters = _uiState.value.selectedFilters.toMutableList()
        val existingIndex = currentFilters.indexOfFirst { it.first == filterType }
        
        if (existingIndex >= 0) {
            // Удаляем фильтр, если он уже выбран
            currentFilters.removeAt(existingIndex)
        } else {
            // Для нейросетевых фильтров не добавляем intensity (null)
            // Для обычных фильтров добавляем с интенсивностью по умолчанию
            val intensity = if (filterType in neuralFilters) {
                null // Нейросетевые фильтры применяются без настроек
            } else {
                0.5f // Обычные фильтры с интенсивностью по умолчанию
            }
            currentFilters.add(Pair(filterType, intensity))
        }
        
        _uiState.value = _uiState.value.copy(selectedFilters = currentFilters)
        // Обновляем предпросмотр
        previewFilters(currentFilters)
    }
    
    fun updateFilterIntensity(filterType: FilterType, intensity: Float) {
        // Нейросетевые фильтры не поддерживают изменение intensity
        if (filterType in neuralFilters) {
            return
        }
        
        val currentFilters = _uiState.value.selectedFilters.toMutableList()
        val existingIndex = currentFilters.indexOfFirst { it.first == filterType }
        
        if (existingIndex >= 0) {
            // Обновляем интенсивность существующего фильтра
            currentFilters[existingIndex] = Pair(filterType, intensity)
        } else {
            // Добавляем новый фильтр с указанной интенсивностью
            currentFilters.add(Pair(filterType, intensity))
        }
        
        _uiState.value = _uiState.value.copy(
            selectedFilters = currentFilters,
            currentFilterIntensity = intensity
        )
        // Обновляем предпросмотр
        previewFilters(currentFilters)
    }
    
    /**
     * Быстрый предпросмотр множественных фильтров без сохранения в файл.
     * Используется для отображения результата в реальном времени.
     */
    private fun previewFilters(filters: List<Pair<FilterType, Float?>>) {
        // Отменяем предыдущий предпросмотр
        currentPreviewJob?.cancel()
        
        if (filters.isEmpty()) {
            _uiState.value = _uiState.value.copy(previewBitmap = null)
            return
        }
        
        currentPreviewJob = viewModelScope.launch {
            // Небольшая задержка для debounce
            delay(100)
            
            val originalBitmap = getOrLoadOriginalBitmap()
            if (originalBitmap == null) {
                android.util.Log.e("EditorViewModel", "Не удалось получить исходный Bitmap для предпросмотра")
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        error = "Не удалось загрузить изображение"
                    )
                }
                return@launch
            }
            
            // Проверяем, что Bitmap не переработан
            if (originalBitmap.isRecycled) {
                android.util.Log.e("EditorViewModel", "Исходный Bitmap был переработан, перезагружаем...")
                cachedOriginalBitmap = null
                val reloadedBitmap = getOrLoadOriginalBitmap() ?: return@launch
                if (reloadedBitmap.isRecycled) {
                    android.util.Log.e("EditorViewModel", "Перезагруженный Bitmap также переработан")
                    return@launch
                }
            }
            
            try {
                android.util.Log.d("EditorViewModel", "Применяем ${filters.size} фильтров для предпросмотра")
                val previewBitmap = processingRepository.previewFilters(
                    originalBitmap,
                    filters.map { it.first to it.second }
                )
                
                if (isActive) {
                    if (previewBitmap != null) {
                        android.util.Log.d("EditorViewModel", "Предпросмотр создан: ${previewBitmap.width}x${previewBitmap.height}")
                        _uiState.value = _uiState.value.copy(previewBitmap = previewBitmap)
                    } else {
                        android.util.Log.e("EditorViewModel", "Предпросмотр вернул null")
                        _uiState.value = _uiState.value.copy(
                            error = "Не удалось применить фильтры"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Ошибка при применении фильтров: ${e.message}", e)
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Ошибка применения фильтров"
                    )
                }
            }
        }
    }
    
    /**
     * Применить выбранные фильтры с сохранением в файл (для финального результата).
     */
    fun applyFilters() {
        val currentImage = _uiState.value.imageData ?: return
        val selectedFilters = _uiState.value.selectedFilters
        
        if (selectedFilters.isEmpty()) {
            android.util.Log.w("EditorViewModel", "Нет выбранных фильтров для применения")
            return
        }
        
        // Отменяем предыдущий запрос, если он еще выполняется
        currentFilterJob?.cancel()
        currentPreviewJob?.cancel()
        
        currentFilterJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                val result = processingRepository.processImageWithFilters(
                    currentImage,
                    selectedFilters.map { it.first to it.second }
                )
                
                // Проверяем, что корутина не была отменена и результат успешен
                if (isActive && result != null) {
                    _uiState.value = _uiState.value.copy(
                        processedResult = result,
                        previewBitmap = null, // Очищаем предпросмотр после сохранения
                        isLoading = false
                    )
                    
                    // Обновляем галерею после успешного сохранения
                    onImageSaved?.invoke()
                    
                    // Переходим на экран обработанных изображений
                    onNavigateToProcessed?.invoke()
                } else if (isActive && result == null) {
                    // Если результат null, показываем ошибку
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не удалось применить фильтры"
                    )
                }
            } catch (e: Exception) {
                // Проверяем, что корутина не была отменена
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
    
    /**
     * Получить или загрузить исходный Bitmap.
     * Использует кэш для избежания повторной загрузки.
     */
    private suspend fun getOrLoadOriginalBitmap(): Bitmap? {
        // Проверяем кэш
        if (cachedOriginalBitmap != null && !cachedOriginalBitmap!!.isRecycled) {
            return cachedOriginalBitmap
        }
        
        val imageData = _uiState.value.imageData ?: return null
        
        // Используем репозиторий для загрузки Bitmap
        return try {
            val bitmap = processingRepository.loadBitmapFromUri(imageData.uri)
            if (bitmap != null) {
                // Кэшируем для последующих использований
                cachedOriginalBitmap = bitmap
                android.util.Log.d("EditorViewModel", "Bitmap закэширован: ${bitmap.width}x${bitmap.height}")
            } else {
                android.util.Log.e("EditorViewModel", "Не удалось загрузить Bitmap из URI: ${imageData.uri}")
            }
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("EditorViewModel", "Ошибка загрузки Bitmap: ${e.message}", e)
            null
        }
    }
    
    fun clearFilters() {
        currentPreviewJob?.cancel()
        currentFilterJob?.cancel()
        _uiState.value = _uiState.value.copy(
            processedResult = null,
            previewBitmap = null,
            selectedFilters = emptyList(),
            currentFilterIntensity = 0.5f
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Переключить категорию фильтров (обычные/нейросетевые).
     */
    fun toggleFilterCategory() {
        _uiState.value = _uiState.value.copy(
            showNeuralFilters = !_uiState.value.showNeuralFilters
        )
    }
    
    /**
     * Переключить режим (фильтры/редактирование).
     */
    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(
            showEditMode = !_uiState.value.showEditMode
        )
    }
    
    /**
     * Обновить яркость.
     */
    fun updateBrightness(value: Float) {
        _uiState.value = _uiState.value.copy(brightness = value)
        recalculatePreview()
    }
    
    /**
     * Обновить контраст.
     */
    fun updateContrast(value: Float) {
        _uiState.value = _uiState.value.copy(contrast = value)
        recalculatePreview()
    }
    
    /**
     * Обновить цветовой баланс.
     */
    fun updateColorBalance(editType: EditType, value: Float) {
        val newState = when (editType) {
            EditType.COLOR_BALANCE_RED -> _uiState.value.copy(colorBalanceRed = value)
            EditType.COLOR_BALANCE_GREEN -> _uiState.value.copy(colorBalanceGreen = value)
            EditType.COLOR_BALANCE_BLUE -> _uiState.value.copy(colorBalanceBlue = value)
            else -> _uiState.value
        }
        _uiState.value = newState
        recalculatePreview()
    }
    
    /**
     * Получить список фильтров для текущей категории.
     */
    fun getCurrentCategoryFilters(): List<FilterType> {
        return if (_uiState.value.showNeuralFilters) {
            neuralFilters
        } else {
            regularFilters
        }
    }
    
    /**
     * Применить редактирование к изображению.
     * Для геометрических операций (повороты, отражения) накапливает изменения.
     * Для цветовых корректировок применяет сразу.
     */
    fun applyEdit(editType: EditType, value: Float = 0f, cropRect: Rect? = null) {
        // Для кадрирования показываем overlay и загружаем bitmap
        if (editType == EditType.CROP) {
            viewModelScope.launch {
                val bitmap = getBitmapForCrop()
                _uiState.value = _uiState.value.copy(
                    showCropOverlay = true,
                    cropBitmap = bitmap
                )
            }
            return
        }
        
        currentPreviewJob?.cancel()
        
        // Для геометрических операций накапливаем изменения
        val isGeometric = editType in listOf(
            EditType.ROTATE_90, EditType.ROTATE_180, EditType.ROTATE_270,
            EditType.FLIP_HORIZONTAL, EditType.FLIP_VERTICAL
        )
        
        val newAppliedEdits = if (isGeometric) {
            _uiState.value.appliedEdits + (editType to 0f)
        } else {
            _uiState.value.appliedEdits
        }
        
        _uiState.value = _uiState.value.copy(appliedEdits = newAppliedEdits)
        
        // Пересчитываем предпросмотр со всеми накопленными изменениями
        recalculatePreview()
    }
    
    /**
     * Получить bitmap для кадрирования (previewBitmap или загрузить из imageUri).
     */
    suspend fun getBitmapForCrop(): Bitmap? {
        return _uiState.value.previewBitmap ?: getOrLoadOriginalBitmap()
    }
    
    /**
     * Применить кадрирование с указанным прямоугольником.
     */
    fun applyCrop(cropRect: Rect) {
        currentPreviewJob?.cancel()
        
        _uiState.value = _uiState.value.copy(
            showCropOverlay = false,
            appliedEdits = _uiState.value.appliedEdits + (EditType.CROP to 0f)
        )
        
        // Применяем кадрирование и пересчитываем предпросмотр
        currentPreviewJob = viewModelScope.launch {
            delay(100)
            
            val originalBitmap = getOrLoadOriginalBitmap()
            if (originalBitmap == null || originalBitmap.isRecycled) {
                _uiState.value = _uiState.value.copy(
                    error = "Не удалось загрузить изображение"
                )
                return@launch
            }
            
            try {
                val croppedBitmap = processingRepository.applyEdit(
                    originalBitmap,
                    EditType.CROP,
                    0f,
                    cropRect
                )
                
                if (isActive && croppedBitmap != null) {
                    _uiState.value = _uiState.value.copy(
                        previewBitmap = croppedBitmap,
                        cropBitmap = null,
                        error = null
                    )
                    // Обновляем кэш
                    cachedOriginalBitmap = croppedBitmap
                } else if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        error = "Не удалось применить кадрирование"
                    )
                }
            } catch (e: Exception) {
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Ошибка применения кадрирования"
                    )
                }
            }
        }
    }
    
    /**
     * Отменить кадрирование.
     */
    fun cancelCrop() {
        _uiState.value = _uiState.value.copy(
            showCropOverlay = false,
            cropBitmap = null
        )
    }
    
    /**
     * Пересчитать предпросмотр с учетом всех накопленных изменений.
     */
    private fun recalculatePreview() {
        currentPreviewJob?.cancel()
        
        currentPreviewJob = viewModelScope.launch {
            delay(100)
            
            val originalBitmap = getOrLoadOriginalBitmap()
            if (originalBitmap == null || originalBitmap.isRecycled) {
                _uiState.value = _uiState.value.copy(
                    error = "Не удалось загрузить изображение"
                )
                return@launch
            }
            
            try {
                // Применяем все накопленные изменения последовательно
                var workingBitmap: Bitmap? = originalBitmap
                val bitmapsToRecycle = mutableListOf<Bitmap>()
                
                // Сначала применяем все геометрические изменения
                for ((geometricEditType, _) in _uiState.value.appliedEdits) {
                    if (workingBitmap == null || workingBitmap.isRecycled) break
                    val result = processingRepository.applyEdit(workingBitmap, geometricEditType, 0f, null)
                    if (result != null && result != workingBitmap) {
                        if (workingBitmap != originalBitmap) {
                            bitmapsToRecycle.add(workingBitmap)
                        }
                        workingBitmap = result
                    }
                }
                
                if (workingBitmap == null || workingBitmap.isRecycled) {
                    _uiState.value = _uiState.value.copy(
                        error = "Ошибка применения геометрических изменений"
                    )
                    return@launch
                }
                
                // Затем применяем цветовые корректировки
                if (_uiState.value.brightness != 0f) {
                    val result = processingRepository.applyEdit(workingBitmap, EditType.BRIGHTNESS, _uiState.value.brightness, null)
                    if (result != null && result != workingBitmap) {
                        if (workingBitmap != originalBitmap) {
                            bitmapsToRecycle.add(workingBitmap)
                        }
                        workingBitmap = result
                    }
                }
                
                if (_uiState.value.contrast != 0f) {
                    if (workingBitmap == null || workingBitmap.isRecycled) {
                        _uiState.value = _uiState.value.copy(
                            error = "Ошибка применения контраста"
                        )
                        return@launch
                    }
                    val result = processingRepository.applyEdit(workingBitmap, EditType.CONTRAST, _uiState.value.contrast, null)
                    if (result != null && result != workingBitmap) {
                        if (workingBitmap != originalBitmap) {
                            bitmapsToRecycle.add(workingBitmap)
                        }
                        workingBitmap = result
                    }
                }
                
                // Применяем цветовой баланс
                if (_uiState.value.colorBalanceRed != 0f) {
                    if (workingBitmap == null || workingBitmap.isRecycled) {
                        _uiState.value = _uiState.value.copy(
                            error = "Ошибка применения цветового баланса"
                        )
                        return@launch
                    }
                    val result = processingRepository.applyEdit(workingBitmap, EditType.COLOR_BALANCE_RED, _uiState.value.colorBalanceRed, null)
                    if (result != null && result != workingBitmap) {
                        if (workingBitmap != originalBitmap) {
                            bitmapsToRecycle.add(workingBitmap)
                        }
                        workingBitmap = result
                    }
                }
                
                if (_uiState.value.colorBalanceGreen != 0f) {
                    if (workingBitmap == null || workingBitmap.isRecycled) {
                        _uiState.value = _uiState.value.copy(
                            error = "Ошибка применения цветового баланса"
                        )
                        return@launch
                    }
                    val result = processingRepository.applyEdit(workingBitmap, EditType.COLOR_BALANCE_GREEN, _uiState.value.colorBalanceGreen, null)
                    if (result != null && result != workingBitmap) {
                        if (workingBitmap != originalBitmap) {
                            bitmapsToRecycle.add(workingBitmap)
                        }
                        workingBitmap = result
                    }
                }
                
                if (_uiState.value.colorBalanceBlue != 0f) {
                    if (workingBitmap == null || workingBitmap.isRecycled) {
                        _uiState.value = _uiState.value.copy(
                            error = "Ошибка применения цветового баланса"
                        )
                        return@launch
                    }
                    val result = processingRepository.applyEdit(workingBitmap, EditType.COLOR_BALANCE_BLUE, _uiState.value.colorBalanceBlue, null)
                    if (result != null && result != workingBitmap) {
                        if (workingBitmap != originalBitmap) {
                            bitmapsToRecycle.add(workingBitmap)
                        }
                        workingBitmap = result
                    }
                }
                
                if (isActive) {
                    if (workingBitmap != null && !workingBitmap.isRecycled) {
                        _uiState.value = _uiState.value.copy(
                            previewBitmap = workingBitmap
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Не удалось применить редактирование"
                        )
                    }
                }
                
                // Освобождаем промежуточные bitmaps
                bitmapsToRecycle.forEach { bitmap ->
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Ошибка применения редактирования"
                    )
                }
            }
        }
    }
    
    /**
     * Установить текущую категорию редактирования.
     */
    fun setEditCategory(category: EditCategory) {
        _uiState.value = _uiState.value.copy(currentEditCategory = category)
    }
    
    /**
     * Очистить все примененные геометрические изменения.
     */
    fun clearGeometricEdits() {
        _uiState.value = _uiState.value.copy(appliedEdits = emptyList())
        // Пересчитываем предпросмотр
        recalculatePreview()
    }
    
    /**
     * Сохранить отредактированное изображение в галерею.
     */
    fun saveEditedImageToGallery() {
        val previewBitmap = _uiState.value.previewBitmap ?: return
        
        currentFilterJob?.cancel()
        currentPreviewJob?.cancel()
        
        currentFilterJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                val timestamp = System.currentTimeMillis()
                val fileName = "edited_${timestamp}.jpg"
                // Сохраняем и в галерею, и в папку processed
                val uri = processingRepository.saveEditedImageToGallery(previewBitmap, fileName)
                
                if (isActive && uri != null) {
                    _uiState.value = _uiState.value.copy(
                        processedResult = ProcessingResult(
                            originalUri = _uiState.value.imageData?.uri ?: uri,
                            processedUri = uri,
                            filterType = "edited"
                        ),
                        isLoading = false
                    )
                    
                    // Обновляем галерею и обработанные изображения
                    onImageSaved?.invoke()
                    onNavigateToProcessed?.invoke()
                } else if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не удалось сохранить изображение"
                    )
                }
            } catch (e: Exception) {
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
}

/**
 * Состояние UI экрана редактора.
 */
data class EditorUiState(
    val imageData: ImageData? = null,
    val processedResult: ProcessingResult? = null,
    val previewBitmap: Bitmap? = null, // Быстрый предпросмотр без сохранения
    val cropBitmap: Bitmap? = null, // Bitmap для кадрирования
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFilters: List<Pair<FilterType, Float?>> = emptyList(), // Список выбранных фильтров с интенсивностями (null для нейросетевых)
    val currentFilterIntensity: Float = 0.5f, // Интенсивность для текущего редактируемого фильтра
    val showNeuralFilters: Boolean = false, // Показывать нейросетевые фильтры (false = обычные)
    val showEditMode: Boolean = false, // Показывать режим редактирования (false = фильтры)
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val colorBalanceRed: Float = 0f,
    val colorBalanceGreen: Float = 0f,
    val colorBalanceBlue: Float = 0f,
    val appliedEdits: List<Pair<EditType, Float>> = emptyList(), // Накопленные редактирования (повороты, отражения)
    val currentEditCategory: EditCategory = EditCategory.BRIGHTNESS, // Текущая категория редактирования
    val showCropOverlay: Boolean = false // Показывать overlay для кадрирования
)

/**
 * Категории редактирования изображений.
 */
enum class EditCategory {
    BRIGHTNESS,
    CONTRAST,
    COLOR_BALANCE,
    GEOMETRY
}