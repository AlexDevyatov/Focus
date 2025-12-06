package com.example.neuralphotoredactor.domain.usecase

import com.example.neuralphotoredactor.domain.repository.NeuralModelRepository
import com.example.neuralphotoredactor.domain.repository.TFLiteModelRepository
import org.tensorflow.lite.Interpreter
import javax.inject.Inject

/**
 * Use case для загрузки TensorFlow Lite модели.
 * 
 * Загружает модель по ID или имени и возвращает Interpreter для инференса.
 */
class LoadTFLiteModelUseCase @Inject constructor(
    private val tfliteModelRepository: TFLiteModelRepository,
    private val neuralModelRepository: NeuralModelRepository
) {
    /**
     * Загрузить модель по ID.
     * 
     * @param modelId ID модели
     * @return Interpreter или null, если модель не найдена или не удалось загрузить
     */
    suspend operator fun invoke(modelId: Long): Interpreter? {
        return tfliteModelRepository.getInterpreterForModelId(modelId)
    }
    
    /**
     * Загрузить модель по имени.
     * 
     * @param modelName Название модели
     * @return Interpreter или null, если модель не найдена или не удалось загрузить
     */
    suspend operator fun invoke(modelName: String): Interpreter? {
        val model = neuralModelRepository.getModelByName(modelName) ?: return null
        return tfliteModelRepository.getInterpreterForModel(model)
    }
    
    /**
     * Загрузить модель из assets.
     * 
     * @param assetPath Путь к модели в assets
     * @return Interpreter или null, если модель не удалось загрузить
     */
    suspend fun loadFromAssets(assetPath: String): Interpreter? {
        return tfliteModelRepository.loadModelFromAssets(assetPath)
    }
    
    /**
     * Загрузить модель из файла.
     * 
     * @param filePath Путь к файлу модели
     * @return Interpreter или null, если модель не удалось загрузить
     */
    suspend fun loadFromPath(filePath: String): Interpreter? {
        return tfliteModelRepository.loadModelFromPath(filePath)
    }
}
