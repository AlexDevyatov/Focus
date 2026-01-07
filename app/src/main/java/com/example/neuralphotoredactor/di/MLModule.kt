package com.example.neuralphotoredactor.di

import android.content.Context
import com.example.neuralphotoredactor.ml.edit.ImageEditProcessor
import com.example.neuralphotoredactor.ml.edit.ImageEditProcessorImpl
import com.example.neuralphotoredactor.ml.filter.ImageFilterProcessor
import com.example.neuralphotoredactor.ml.filter.ImageFilterProcessorImpl
import com.example.neuralphotoredactor.ml.interpreter.ErsganImageProcessor
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
 * 
 * Предоставляет Interpreter'ы для конкретных моделей и общие ML компоненты.
 * Каждая модель имеет свой собственный Interpreter, который загружается при инициализации.
 * 
 * Для работы с множеством моделей используйте TFLiteModelRepository,
 * который управляет загрузкой и кэшированием Interpreter'ов динамически.
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
    
    @Binds
    abstract fun bindImageEditProcessor(
        imageEditProcessorImpl: ImageEditProcessorImpl
    ): ImageEditProcessor
    
    companion object {
        /**
         * Предоставляет Interpreter для модели ERSGAN (Enhanced Super-Resolution Generative Adversarial Network).
         * 
         * Эта модель используется для super resolution (увеличения разрешения изображений).
         * Модель загружается из assets при инициализации приложения.
         * 
         * ВАЖНО: Создайте файл ersgan.tflite в app/src/main/assets/
         * 
         * Для других моделей используйте TFLiteModelRepository, который поддерживает
         * динамическую загрузку множества моделей с кэшированием Interpreter'ов.
         * 
         * @param context Контекст приложения для доступа к assets
         * @return Interpreter для модели ERSGAN или null, если модель не найдена
         */
        @Provides
        @Singleton
        fun provideERSGANInterpreter(
            @ApplicationContext context: Context
        ): Interpreter? {
            return try {
                val modelBuffer = ModelLoader.loadModelFile(context, "ersgan.tflite")
                ModelLoader.createInterpreter(modelBuffer)
            } catch (e: Exception) {
                // Если модель не найдена, возвращаем null
                // Обработка ошибки будет в ErsganImageProcessor
                android.util.Log.e("MLModule", "ERSGAN модель не найдена: ${e.message}")
                null
            }
        }
        
        /**
         * Предоставляет ErsganImageProcessor с Interpreter для модели ERSGAN.
         * 
         * Этот процессор используется для обработки изображений через модель ERSGAN
         * для super resolution (увеличения разрешения).
         * 
         * Для работы с другими моделями создавайте отдельные процессоры
         * (например, StyleTransferImageProcessor, DenoiseImageProcessor)
         * с соответствующими Interpreter'ами через TFLiteModelRepository.
         * 
         * @param interpreter Interpreter для модели ERSGAN
         * @param preprocessor Препроцессор изображений
         * @param postprocessor Постпроцессор изображений
         * @return ErsganImageProcessor для обработки изображений через ERSGAN
         */
        @Provides
        @Singleton
        fun provideErsganImageProcessor(
            interpreter: Interpreter?,
            preprocessor: ImagePreprocessor,
            postprocessor: ImagePostprocessor
        ): ErsganImageProcessor {
            return ErsganImageProcessor(interpreter, preprocessor, postprocessor)
        }
    }
}

