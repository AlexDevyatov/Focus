package com.example.neuralphotoredactor.di

import android.content.Context
import com.example.neuralphotoredactor.ml.filter.ImageFilterProcessor
import com.example.neuralphotoredactor.ml.filter.ImageFilterProcessorImpl
import com.example.neuralphotoredactor.ml.interpreter.ImageProcessor
import com.example.neuralphotoredactor.ml.postprocessor.ImagePostprocessor
import com.example.neuralphotoredactor.ml.postprocessor.ImagePostprocessorImpl
import com.example.neuralphotoredactor.ml.preprocessor.ImagePreprocessor
import com.example.neuralphotoredactor.ml.preprocessor.ImagePreprocessorImpl
import com.example.neuralphotoredactor.ml.util.ModelLoader
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tensorflow.lite.Interpreter
import javax.inject.Singleton

/**
 * Модуль для предоставления ML компонентов (TensorFlow Lite).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MLModule {
    
    @Binds
    abstract fun bindImagePreprocessor(
        imagePreprocessorImpl: ImagePreprocessorImpl
    ): ImagePreprocessor
    
    @Binds
    abstract fun bindImagePostprocessor(
        imagePostprocessorImpl: ImagePostprocessorImpl
    ): ImagePostprocessor
    
    @Binds
    abstract fun bindImageFilterProcessor(
        imageFilterProcessorImpl: ImageFilterProcessorImpl
    ): ImageFilterProcessor
    
    companion object {
        @Provides
        @Singleton
        fun provideTFLiteInterpreter(
            @ApplicationContext context: Context
        ): Interpreter? {
            // Загружаем модель из assets
            // ВАЖНО: Создайте файл model.tflite в app/src/main/assets/
            return try {
                val modelBuffer = ModelLoader.loadModelFile(context, "model.tflite")
                ModelLoader.createInterpreter(modelBuffer)
            } catch (e: Exception) {
                // Если модель не найдена, возвращаем null
                // Обработка ошибки будет в ImageProcessor
                android.util.Log.e("MLModule", "TFLite модель не найдена: ${e.message}")
                null
            }
        }
        
        @Provides
        @Singleton
        fun provideImageProcessor(
            interpreter: Interpreter?,
            preprocessor: ImagePreprocessor,
            postprocessor: ImagePostprocessor
        ): ImageProcessor {
            return ImageProcessor(interpreter, preprocessor, postprocessor)
        }
    }
}

