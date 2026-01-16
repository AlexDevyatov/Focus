package com.example.neuralphotoredactor.di

import android.content.Context
import com.example.neuralphotoredactor.ml.edit.ImageEditProcessor
import com.example.neuralphotoredactor.ml.edit.ImageEditProcessorImpl
import com.example.neuralphotoredactor.ml.filter.ImageFilterProcessor
import com.example.neuralphotoredactor.ml.filter.ImageFilterProcessorImpl
import com.example.neuralphotoredactor.ml.interpreter.AnimeGan2ImageProcessor
import com.example.neuralphotoredactor.ml.interpreter.AnimeGanFacePaintProcessor
import com.example.neuralphotoredactor.ml.interpreter.CelebADistillProcessor
import com.example.neuralphotoredactor.ml.interpreter.EsrganImageProcessor
import com.example.neuralphotoredactor.ml.interpreter.HayaoProcessor
import com.example.neuralphotoredactor.ml.interpreter.SplitterNetImageProcessor
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
        imagePreprocessorImpl: ImagePreprocessorImpl,
    ): ImagePreprocessor

    @Binds
    abstract fun bindImagePostprocessor(
        imagePostprocessorImpl: ImagePostprocessorImpl,
    ): ImagePostprocessor

    @Binds
    abstract fun bindImageFilterProcessor(
        imageFilterProcessorImpl: ImageFilterProcessorImpl,
    ): ImageFilterProcessor

    @Binds
    abstract fun bindImageEditProcessor(
        imageEditProcessorImpl: ImageEditProcessorImpl,
    ): ImageEditProcessor

    companion object {
        /**
         * Предоставляет Interpreter для модели ESRGAN (Enhanced Super-Resolution Generative Adversarial Network).
         *
         * Эта модель используется для super resolution (увеличения разрешения изображений).
         * Применяется для фильтра UPSCALE.
         * Модель загружается из assets при инициализации приложения.
         *
         * ВАЖНО: Создайте файл esrgan.tflite в app/src/main/assets/
         *
         * Для других моделей используйте TFLiteModelRepository, который поддерживает
         * динамическую загрузку множества моделей с кэшированием Interpreter'ов.
         *
         * @param context Контекст приложения для доступа к assets
         * @return Interpreter для модели ESRGAN или null, если модель не найдена
         */
        @Provides
        @Singleton
        @EsrganInterpreter
        fun provideERSGANInterpreter(
            @ApplicationContext context: Context,
        ): Interpreter? {
            return try {
                val modelBuffer = ModelLoader.loadModelFile(context, "esrgan.tflite")
                ModelLoader.createInterpreter(modelBuffer)
            } catch (e: Exception) {
                // Если модель не найдена, возвращаем null
                // Обработка ошибки будет в ErsganImageProcessor
                android.util.Log.e("MLModule", "ESRGAN модель не найдена: ${e.message}")
                null
            }
        }

        /**
         * Предоставляет ErsganImageProcessor с Interpreter для модели ESRGAN.
         *
         * Этот процессор используется для обработки изображений через модель ESRGAN
         * для super resolution (увеличения разрешения).
         * Применяется для фильтра UPSCALE.
         *
         * Для работы с другими моделями создавайте отдельные процессоры
         * (например, StyleTransferImageProcessor, DenoiseImageProcessor)
         * с соответствующими Interpreter'ами через TFLiteModelRepository.
         *
         * @param interpreter Interpreter для модели ESRGAN
         * @param preprocessor Препроцессор изображений
         * @param postprocessor Постпроцессор изображений
         * @return ErsganImageProcessor для обработки изображений через ESRGAN
         */
        @Provides
        @Singleton
        fun provideEsrganImageProcessor(
            @EsrganInterpreter interpreter: Interpreter?,
            preprocessor: ImagePreprocessor,
            postprocessor: ImagePostprocessor,
        ): EsrganImageProcessor {
            return EsrganImageProcessor(interpreter)
        }

        /**
         * Предоставляет Interpreter для модели SplitterNet (удаление шумов).
         *
         * Эта модель используется для удаления шумов с изображений.
         * Применяется для фильтра DENOISE.
         * Модель загружается из assets при инициализации приложения.
         *
         * ВАЖНО: Создайте файл splitternet_midd_model.tflite в app/src/main/assets/
         *
         * @param context Контекст приложения для доступа к assets
         * @return Interpreter для модели SplitterNet или null, если модель не найдена
         */
        @Provides
        @Singleton
        @SplitterNetInterpreter
        fun provideSplitterNetInterpreter(
            @ApplicationContext context: Context,
        ): Interpreter? {
            return try {
                val modelBuffer =
                    ModelLoader.loadModelFile(
                        context,
                        "splitternet_midd_model.tflite",
                    )
                ModelLoader.createInterpreter(modelBuffer)
            } catch (e: Exception) {
                // Если модель не найдена, возвращаем null
                // Обработка ошибки будет в SplitterNetImageProcessor
                android.util.Log.e("MLModule", "SplitterNet модель не найдена: ${e.message}")
                null
            }
        }

        /**
         * Предоставляет SplitterNetImageProcessor с Interpreter для модели SplitterNet.
         *
         * Этот процессор используется для обработки изображений через модель SplitterNet
         * для удаления шумов.
         * Применяется для фильтра DENOISE.
         *
         * Обрабатывает изображение любого размера, разбивая его на патчи 256x256 с перекрытием,
         * обрабатывает каждый патч через модель, и собирает результат обратно в исходный размер
         * с использованием взвешенного усреднения.
         *
         * @param interpreter Interpreter для модели SplitterNet
         * @return SplitterNetImageProcessor для обработки изображений через SplitterNet
         */
        @Provides
        @Singleton
        fun provideSplitterNetImageProcessor(
            @SplitterNetInterpreter interpreter: Interpreter?,
        ): SplitterNetImageProcessor {
            return SplitterNetImageProcessor(interpreter)
        }

        /**
         * Предоставляет Interpreter для модели AnimeGAN2 (стилизация в аниме стиль).
         *
         * Эта модель используется для стилизации изображений в аниме стиль.
         * Применяется для фильтра STYLE_TRANSFER.
         * Модель загружается из assets при инициализации приложения.
         *
         * ВАЖНО: Создайте файл animegan2_paprika.tflite в app/src/main/assets/
         *
         * @param context Контекст приложения для доступа к assets
         * @return Interpreter для модели AnimeGAN2 или null, если модель не найдена
         */
        @Provides
        @Singleton
        @AnimeGan2Interpreter
        fun provideAnimeGan2Interpreter(
            @ApplicationContext context: Context,
        ): Interpreter? {
            return try {
                val modelBuffer = ModelLoader.loadModelFile(context, "animegan2_paprika.tflite")
                ModelLoader.createInterpreter(modelBuffer)
            } catch (e: Exception) {
                // Если модель не найдена, возвращаем null
                // Обработка ошибки будет в AnimeGan2ImageProcessor
                android.util.Log.e("MLModule", "AnimeGAN2 модель не найдена: ${e.message}")
                null
            }
        }

        /**
         * Предоставляет AnimeGan2ImageProcessor с Interpreter для модели AnimeGAN2.
         *
         * Этот процессор используется для обработки изображений через модель AnimeGAN2
         * для стилизации в аниме стиль.
         * Применяется для фильтра STYLE_TRANSFER.
         *
         * Использует настройки из test_animegan2.py:
         * - Нормализация входных данных: (img_array / 127.5) - 1.0 (диапазон [-1, 1])
         * - Поддержка CHW и HWC форматов
         * - Денормализация выходных данных: ((output_img + 1.0) * 127.5) (диапазон [0, 255])
         *
         * @param interpreter Interpreter для модели AnimeGAN2
         * @return AnimeGan2ImageProcessor для обработки изображений через AnimeGAN2
         */
        @Provides
        @Singleton
        fun provideAnimeGan2ImageProcessor(
            @AnimeGan2Interpreter interpreter: Interpreter?,
        ): AnimeGan2ImageProcessor {
            return AnimeGan2ImageProcessor(interpreter)
        }

        /**
         * Предоставляет AnimeGanFacePaintProcessor для модели AnimeGAN Face Paint.
         *
         * Этот процессор используется для обработки изображений через модель animegan_face_paint_512_v2.tflite
         * для стилизации лиц в аниме стиль.
         * Применяется для фильтра STYLE_TRANSFER.
         *
         * Модель загружается динамически через TFLiteModelRepository из assets.
         *
         * @param tfliteModelRepository Репозиторий для загрузки TFLite моделей
         * @return AnimeGanFacePaintProcessor для обработки изображений
         */
        @Provides
        @Singleton
        fun provideAnimeGanFacePaintProcessor(
            tfliteModelRepository: com.example.neuralphotoredactor.domain.repository.TFLiteModelRepository,
        ): AnimeGanFacePaintProcessor {
            return AnimeGanFacePaintProcessor(tfliteModelRepository)
        }

        /**
         * Предоставляет CelebADistillProcessor для модели CelebA Distill.
         *
         * Этот процессор используется для обработки изображений через модель celeba_distill.tflite
         * для стилизации в аниме стиль.
         * Применяется для фильтра STYLE_TRANSFER.
         *
         * Модель загружается динамически через TFLiteModelRepository из assets.
         *
         * @param tfliteModelRepository Репозиторий для загрузки TFLite моделей
         * @return CelebADistillProcessor для обработки изображений
         */
        @Provides
        @Singleton
        fun provideCelebADistillProcessor(
            tfliteModelRepository: com.example.neuralphotoredactor.domain.repository.TFLiteModelRepository,
        ): CelebADistillProcessor {
            return CelebADistillProcessor(tfliteModelRepository)
        }

        /**
         * Предоставляет HayaoProcessor для модели Hayao.
         *
         * Этот процессор используется для обработки изображений через модель hayao.tflite
         * для стилизации в аниме стиль.
         * Применяется для фильтра STYLE_TRANSFER.
         *
         * Модель загружается динамически через TFLiteModelRepository из assets.
         *
         * @param tfliteModelRepository Репозиторий для загрузки TFLite моделей
         * @return HayaoProcessor для обработки изображений
         */
        @Provides
        @Singleton
        fun provideHayaoProcessor(
            tfliteModelRepository: com.example.neuralphotoredactor.domain.repository.TFLiteModelRepository,
        ): HayaoProcessor {
            return HayaoProcessor(tfliteModelRepository)
        }
    }
}
