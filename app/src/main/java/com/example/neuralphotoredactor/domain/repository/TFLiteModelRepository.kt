package com.example.neuralphotoredactor.domain.repository

import com.example.neuralphotoredactor.domain.model.NeuralModel
import org.tensorflow.lite.Interpreter

/**
 * Интерфейс репозитория для работы с TensorFlow Lite моделями.
 *
 * Предоставляет методы для загрузки и управления TFLite Interpreter'ами.
 * Отвечает за жизненный цикл интерпретаторов и их кэширование.
 */
interface TFLiteModelRepository {
    /**
     * Получить Interpreter для указанной модели.
     *
     * @param model Модель для загрузки
     * @return Interpreter или null, если модель не удалось загрузить
     */
    suspend fun getInterpreterForModel(model: NeuralModel): Interpreter?

    /**
     * Получить Interpreter по ID модели.
     *
     * @param modelId ID модели
     * @return Interpreter или null, если модель не найдена или не удалось загрузить
     */
    suspend fun getInterpreterForModelId(modelId: Long): Interpreter?

    /**
     * Загрузить модель из файла.
     *
     * @param modelPath Путь к файлу модели
     * @return Interpreter или null, если модель не удалось загрузить
     */
    suspend fun loadModelFromPath(modelPath: String): Interpreter?

    /**
     * Загрузить модель из assets.
     *
     * @param assetPath Путь к модели в assets
     * @return Interpreter или null, если модель не удалось загрузить
     */
    suspend fun loadModelFromAssets(assetPath: String): Interpreter?

    /**
     * Освободить Interpreter и освободить ресурсы.
     *
     * @param interpreter Interpreter для освобождения
     */
    fun releaseInterpreter(interpreter: Interpreter)

    /**
     * Освободить все загруженные Interpreter'ы.
     */
    fun releaseAll()
}
