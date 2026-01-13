package com.example.neuralphotoredactor.domain.usecase

import android.content.Context
import com.example.neuralphotoredactor.domain.model.CompatibilityLevel
import com.example.neuralphotoredactor.domain.model.ModelType
import com.example.neuralphotoredactor.domain.repository.NeuralModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case для инициализации нейросетевых моделей в базе данных.
 * 
 * Заполняет таблицу neural_models данными о моделях из assets при первом запуске.
 */
class InitializeNeuralModelsUseCase @Inject constructor(
    private val neuralModelRepository: NeuralModelRepository,
    @ApplicationContext private val context: Context
) {
    
    /**
     * Инициализировать модели в базе данных.
     * 
     * Проверяет, есть ли уже модели в БД, и если нет - заполняет их данными из assets.
     * 
     * @return true, если инициализация прошла успешно, false в случае ошибки
     */
    suspend fun invoke(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Проверяем, есть ли уже модели в БД
            val modelsList = neuralModelRepository.getAllModels().first()
            
            if (modelsList.isNotEmpty()) {
                android.util.Log.d("InitializeNeuralModels", "Модели уже инициализированы: ${modelsList.size} моделей")
                return@withContext true
            }
            
            android.util.Log.d("InitializeNeuralModels", "Начало инициализации моделей")
            
            // Список моделей для инициализации
            val modelsToInitialize = listOf(
                ModelInfo(
                    name = "AnimeGAN2 Paprika",
                    type = ModelType.STYLE_TRANSFER,
                    version = "2.0",
                    assetPath = "animegan2_paprika.tflite",
                    compatibilityLevel = CompatibilityLevel.HIGH
                ),
                ModelInfo(
                    name = "ESRGAN",
                    type = ModelType.SUPER_RESOLUTION,
                    version = "1.0",
                    assetPath = "esrgan.tflite",
                    compatibilityLevel = CompatibilityLevel.MEDIUM
                ),
                ModelInfo(
                    name = "SplitterNet",
                    type = ModelType.ENHANCEMENT,
                    version = "1.0",
                    assetPath = "splitternet_midd_model.tflite",
                    compatibilityLevel = CompatibilityLevel.HIGH
                )
            )
            
            // Добавляем каждую модель
            var successCount = 0
            for (modelInfo in modelsToInitialize) {
                try {
                    val fileSize = getAssetFileSize(modelInfo.assetPath)
                    if (fileSize > 0) {
                        neuralModelRepository.addModel(
                            com.example.neuralphotoredactor.domain.model.NeuralModel(
                                name = modelInfo.name,
                                type = modelInfo.type,
                                version = modelInfo.version,
                                filePath = "assets://${modelInfo.assetPath}", // Путь в assets
                                fileSize = fileSize,
                                isActive = true,
                                compatibilityLevel = modelInfo.compatibilityLevel
                            )
                        )
                        successCount++
                        android.util.Log.d("InitializeNeuralModels", "Модель добавлена: ${modelInfo.name}")
                    } else {
                        android.util.Log.w("InitializeNeuralModels", "Не удалось получить размер файла: ${modelInfo.assetPath}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("InitializeNeuralModels", "Ошибка добавления модели ${modelInfo.name}: ${e.message}", e)
                }
            }
            
            android.util.Log.d("InitializeNeuralModels", "Инициализация завершена: $successCount из ${modelsToInitialize.size} моделей")
            successCount == modelsToInitialize.size
            
        } catch (e: Exception) {
            android.util.Log.e("InitializeNeuralModels", "Ошибка инициализации моделей: ${e.message}", e)
            false
        }
    }
    
    /**
     * Получить размер файла из assets.
     * 
     * @param assetPath Путь к файлу в assets
     * @return Размер файла в байтах или 0, если файл не найден
     */
    private fun getAssetFileSize(assetPath: String): Long {
        return try {
            val assetFileDescriptor = context.assets.openFd(assetPath)
            val size = assetFileDescriptor.length
            assetFileDescriptor.close()
            size
        } catch (e: Exception) {
            android.util.Log.e("InitializeNeuralModels", "Ошибка получения размера файла $assetPath: ${e.message}", e)
            0L
        }
    }
    
    /**
     * Информация о модели для инициализации.
     */
    private data class ModelInfo(
        val name: String,
        val type: ModelType,
        val version: String,
        val assetPath: String,
        val compatibilityLevel: CompatibilityLevel
    )
}

