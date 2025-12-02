package com.example.neuralphotoredactor.ml.util

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Утилита для загрузки TFLite моделей из assets.
 */
object ModelLoader {
    /**
     * Загрузить TFLite модель из assets.
     * 
     * @param context Контекст приложения
     * @param modelPath Путь к модели в assets (например, "model.tflite")
     * @return MappedByteBuffer с моделью
     */
    fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Создать TFLite Interpreter из модели.
     * 
     * @param modelBuffer Буфер с моделью
     * @return Interpreter для инференса
     */
    fun createInterpreter(modelBuffer: MappedByteBuffer): Interpreter {
        val options = Interpreter.Options()
        options.setNumThreads(4)
        return Interpreter(modelBuffer, options)
    }
}

