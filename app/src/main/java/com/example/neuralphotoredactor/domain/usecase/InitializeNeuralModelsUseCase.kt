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
 * Сканирует папку assets на наличие .tflite файлов и добавляет их в БД,
 * если их там еще нет.
 */
class InitializeNeuralModelsUseCase @Inject constructor(
    private val neuralModelRepository: NeuralModelRepository,
    @ApplicationContext private val context: Context
) {
    
    /**
     * Инициализировать модели в базе данных.
     * 
     * Сканирует assets на наличие .tflite файлов и добавляет их в БД,
     * если их там еще нет (проверка по filePath).
     * 
     * @return true, если инициализация прошла успешно, false в случае ошибки
     */
    suspend fun invoke(): Boolean = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("InitializeNeuralModels", "Начало инициализации моделей")
            
            // Получаем список всех моделей из БД
            val existingModels = neuralModelRepository.getAllModels().first()
            val existingFilePaths = existingModels.map { it.filePath }.toSet()
            
            android.util.Log.d("InitializeNeuralModels", "В БД уже есть ${existingModels.size} моделей")
            
            // Сканируем assets на наличие .tflite файлов
            val assetFiles = try {
                context.assets.list("")?.filter { it.endsWith(".tflite", ignoreCase = true) } ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("InitializeNeuralModels", "Ошибка сканирования assets: ${e.message}", e)
                emptyList()
            }
            
            android.util.Log.d("InitializeNeuralModels", "Найдено ${assetFiles.size} .tflite файлов в assets")
            
            // Добавляем только те модели, которых еще нет в БД
            var successCount = 0
            var skippedCount = 0
            
            for (assetFile in assetFiles) {
                val filePath = "assets://$assetFile"
                
                // Проверяем, есть ли уже эта модель в БД
                if (existingFilePaths.contains(filePath)) {
                    android.util.Log.d("InitializeNeuralModels", "Модель уже есть в БД: $assetFile")
                    skippedCount++
                    continue
                }
                
                // Получаем метаданные для модели
                val modelInfo = getModelInfo(assetFile)
                if (modelInfo == null) {
                    android.util.Log.w("InitializeNeuralModels", "Не удалось определить метаданные для: $assetFile")
                    continue
                }
                
                try {
                    val fileSize = getAssetFileSize(assetFile)
                    if (fileSize > 0) {
                        neuralModelRepository.addModel(
                            com.example.neuralphotoredactor.domain.model.NeuralModel(
                                name = modelInfo.name,
                                type = modelInfo.type,
                                version = modelInfo.version,
                                filePath = filePath,
                                fileSize = fileSize,
                                isActive = true,
                                compatibilityLevel = modelInfo.compatibilityLevel
                            )
                        )
                        successCount++
                        android.util.Log.d("InitializeNeuralModels", "Модель добавлена: ${modelInfo.name} ($assetFile)")
                    } else {
                        android.util.Log.w("InitializeNeuralModels", "Не удалось получить размер файла: $assetFile")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("InitializeNeuralModels", "Ошибка добавления модели $assetFile: ${e.message}", e)
                }
            }
            
            android.util.Log.d("InitializeNeuralModels", "Инициализация завершена: добавлено $successCount, пропущено $skippedCount из ${assetFiles.size} моделей")
            true
            
        } catch (e: Exception) {
            android.util.Log.e("InitializeNeuralModels", "Ошибка инициализации моделей: ${e.message}", e)
            false
        }
    }
    
    /**
     * Получить метаданные модели по имени файла.
     * 
     * @param assetFileName Имя файла модели в assets
     * @return Метаданные модели или null, если не удалось определить
     */
    private fun getModelInfo(assetFileName: String): ModelInfo? {
        return when (assetFileName.lowercase()) {
            "animegan2_paprika.tflite" -> ModelInfo(
                name = "AnimeGAN2 Paprika",
                type = ModelType.STYLE_TRANSFER,
                version = "2.0",
                assetPath = assetFileName,
                compatibilityLevel = CompatibilityLevel.HIGH
            )
            "animegan_face_paint_512_v2.tflite" -> ModelInfo(
                name = "AnimeGAN Face Paint",
                type = ModelType.STYLE_TRANSFER,
                version = "2.0",
                assetPath = assetFileName,
                compatibilityLevel = CompatibilityLevel.HIGH
            )
            "celeba_distill.tflite" -> ModelInfo(
                name = "CelebA Distill",
                type = ModelType.STYLE_TRANSFER,
                version = "1.0",
                assetPath = assetFileName,
                compatibilityLevel = CompatibilityLevel.MEDIUM
            )
            "esrgan.tflite" -> ModelInfo(
                name = "ESRGAN",
                type = ModelType.SUPER_RESOLUTION,
                version = "1.0",
                assetPath = assetFileName,
                compatibilityLevel = CompatibilityLevel.MEDIUM
            )
            "hayao.tflite" -> ModelInfo(
                name = "Hayao",
                type = ModelType.STYLE_TRANSFER,
                version = "1.0",
                assetPath = assetFileName,
                compatibilityLevel = CompatibilityLevel.HIGH
            )
            "splitternet_midd_model.tflite" -> ModelInfo(
                name = "SplitterNet",
                type = ModelType.ENHANCEMENT,
                version = "1.0",
                assetPath = assetFileName,
                compatibilityLevel = CompatibilityLevel.HIGH
            )
            else -> {
                // Для неизвестных моделей используем имя файла как название
                android.util.Log.w("InitializeNeuralModels", "Неизвестная модель, используем дефолтные метаданные: $assetFileName")
                ModelInfo(
                    name = assetFileName.removeSuffix(".tflite").replace("_", " ").split(" ").joinToString(" ") { 
                        it.replaceFirstChar { char -> char.uppercaseChar() }
                    },
                    type = ModelType.OTHER,
                    version = "1.0",
                    assetPath = assetFileName,
                    compatibilityLevel = CompatibilityLevel.MEDIUM
                )
            }
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

