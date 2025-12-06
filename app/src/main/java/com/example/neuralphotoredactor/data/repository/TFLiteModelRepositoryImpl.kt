package com.example.neuralphotoredactor.data.repository

import android.content.Context
import com.example.neuralphotoredactor.domain.model.NeuralModel
import com.example.neuralphotoredactor.domain.repository.NeuralModelRepository
import com.example.neuralphotoredactor.domain.repository.TFLiteModelRepository
import com.example.neuralphotoredactor.ml.util.ModelLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория для работы с TensorFlow Lite моделями.
 * 
 * Управляет загрузкой и кэшированием TFLite Interpreter'ов.
 * Обеспечивает потокобезопасность и управление ресурсами.
 */
@Singleton
class TFLiteModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val neuralModelRepository: NeuralModelRepository
) : TFLiteModelRepository {
    
    // Кэш загруженных Interpreter'ов
    private val interpreterCache = ConcurrentHashMap<String, Interpreter>()
    
    // Мьютекс для потокобезопасности
    private val mutex = Mutex()
    
    override suspend fun getInterpreterForModel(model: NeuralModel): Interpreter? = withContext(Dispatchers.IO) {
        mutex.withLock {
            // Проверяем кэш
            val cacheKey = model.id.toString()
            interpreterCache[cacheKey]?.let { return@withContext it }
            
            // Загружаем модель из файла
            val modelFile = File(model.filePath)
            if (modelFile.exists()) {
                loadModelFromPath(model.filePath)?.also { interpreter ->
                    interpreterCache[cacheKey] = interpreter
                    return@withContext interpreter
                }
            }
            
            null
        }
    }
    
    override suspend fun getInterpreterForModelId(modelId: Long): Interpreter? = withContext(Dispatchers.IO) {
        val model = neuralModelRepository.getModelById(modelId) ?: return@withContext null
        getInterpreterForModel(model)
    }
    
    override suspend fun loadModelFromPath(modelPath: String): Interpreter? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val cacheKey = "path:$modelPath"
                interpreterCache[cacheKey]?.let { return@withContext it }
                
                val modelFile = File(modelPath)
                if (!modelFile.exists()) {
                    android.util.Log.e("TFLiteModelRepository", "Модель не найдена: $modelPath")
                    return@withContext null
                }
                
                val modelBuffer = ModelLoader.loadModelFileFromPath(modelPath)
                val interpreter = ModelLoader.createInterpreter(modelBuffer)
                
                interpreterCache[cacheKey] = interpreter
                android.util.Log.d("TFLiteModelRepository", "Модель загружена: $modelPath")
                interpreter
            } catch (e: Exception) {
                android.util.Log.e("TFLiteModelRepository", "Ошибка загрузки модели из $modelPath: ${e.message}", e)
                null
            }
        }
    }
    
    override suspend fun loadModelFromAssets(assetPath: String): Interpreter? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val cacheKey = "assets:$assetPath"
                interpreterCache[cacheKey]?.let { return@withContext it }
                
                val modelBuffer = ModelLoader.loadModelFile(context, assetPath)
                val interpreter = ModelLoader.createInterpreter(modelBuffer)
                
                interpreterCache[cacheKey] = interpreter
                android.util.Log.d("TFLiteModelRepository", "Модель загружена из assets: $assetPath")
                interpreter
            } catch (e: Exception) {
                android.util.Log.e("TFLiteModelRepository", "Ошибка загрузки модели из assets/$assetPath: ${e.message}", e)
                null
            }
        }
    }
    
    override fun releaseInterpreter(interpreter: Interpreter) {
        interpreterCache.entries.removeIf { (_, cachedInterpreter) ->
            if (cachedInterpreter === interpreter) {
                try {
                    cachedInterpreter.close()
                    android.util.Log.d("TFLiteModelRepository", "Interpreter освобожден")
                    true
                } catch (e: Exception) {
                    android.util.Log.e("TFLiteModelRepository", "Ошибка освобождения Interpreter: ${e.message}", e)
                    false
                }
            } else {
                false
            }
        }
    }
    
    override fun releaseAll() {
        interpreterCache.values.forEach { interpreter ->
            try {
                interpreter.close()
            } catch (e: Exception) {
                android.util.Log.e("TFLiteModelRepository", "Ошибка освобождения Interpreter: ${e.message}", e)
            }
        }
        interpreterCache.clear()
        android.util.Log.d("TFLiteModelRepository", "Все Interpreter'ы освобождены")
    }
}
