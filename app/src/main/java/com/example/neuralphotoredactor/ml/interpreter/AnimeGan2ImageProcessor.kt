package com.example.neuralphotoredactor.ml.interpreter

import android.graphics.Bitmap
import com.example.neuralphotoredactor.domain.enums.FilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Процессор изображений для модели AnimeGAN2 (стилизация в аниме стиль).
 *
 * Выполняет инференс модели AnimeGAN2 для стилизации изображений в аниме стиль.
 * Применяется для фильтра STYLE_TRANSFER.
 * Работает полностью оффлайн.
 *
 * Использует настройки из test_animegan2.py:
 * - Нормализация входных данных: (img_array / 127.5) - 1.0 (диапазон [-1, 1])
 * - Поддержка CHW и HWC форматов
 * - Денормализация выходных данных: ((output_img + 1.0) * 127.5) (диапазон [0, 255])
 */
class AnimeGan2ImageProcessor
    @Inject
    constructor(
        private val interpreter: Interpreter?,
    ) {
        // Мьютекс для синхронизации доступа к Interpreter (он не потокобезопасен)
        private val interpreterMutex = Mutex()

        /**
         * Обработать изображение через модель AnimeGAN2 для стилизации.
         *
         * Изображение обрабатывается целиком через модель с динамическим изменением размера тензора.
         * Модель AnimeGAN2 поддерживает изображения произвольного размера.
         *
         * @param bitmap Исходное изображение
         * @param filterType Тип фильтра (должен быть FilterType.STYLE_TRANSFER)
         * @return Обработанное изображение или null в случае ошибки
         */
        suspend fun processImage(
            bitmap: Bitmap,
            filterType: FilterType,
        ): Bitmap? =
            withContext(Dispatchers.Default) {
                if (interpreter == null) {
                    android.util.Log.e(
                        "AnimeGan2ImageProcessor",
                        "AnimeGAN2 Interpreter не инициализирован",
                    )
                    return@withContext null
                }

                return@withContext try {
                    // Получаем информацию о входном и выходном тензорах
                    val inputDetails = interpreter.getInputTensor(0)
                    val inputShape = inputDetails.shape()
                    val inputDataType = inputDetails.dataType()

                    val outputDetails = interpreter.getOutputTensor(0)
                    val outputShape = outputDetails.shape()
                    val outputDataType = outputDetails.dataType()

                    android.util.Log.d(
                        "AnimeGan2ImageProcessor",
                        "Input shape: ${inputShape.contentToString()}, Output shape: ${outputShape.contentToString()}",
                    )
                    android.util.Log.d(
                        "AnimeGan2ImageProcessor",
                        "Input dtype: $inputDataType, Output dtype: $outputDataType",
                    )
                    android.util.Log.d(
                        "AnimeGan2ImageProcessor",
                        "Original image: ${bitmap.width}x${bitmap.height}",
                    )

                    // Определяем формат входа (CHW или HWC)
                    // Если shape[1] == 3, то формат [batch, channels, height, width] (CHW)
                    // Иначе формат [batch, height, width, channels] (HWC)
                    val isCHWFormat = inputShape.size >= 4 && inputShape[1] == 3

                    // Подготавливаем входные данные
                    val inputWidth = bitmap.width
                    val inputHeight = bitmap.height
                    val inputChannels = 3 // RGB

                    // Создаем массив для входных данных
                    val inputArray =
                        if (isCHWFormat) {
                            // Формат CHW: [1, C, H, W]
                            Array(
                                1,
                            ) {
                                Array(
                                    inputChannels,
                                ) { Array(inputHeight) { FloatArray(inputWidth) } }
                            }
                        } else {
                            // Формат HWC: [1, H, W, C]
                            Array(
                                1,
                            ) {
                                Array(
                                    inputHeight,
                                ) { Array(inputWidth) { FloatArray(inputChannels) } }
                            }
                        }

                    // Извлекаем пиксели из bitmap
                    val pixels = IntArray(inputWidth * inputHeight)
                    bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

                    // Нормализуем пиксели в диапазон [-1, 1] как в test_animegan2.py
                    // Нормализация: (img_array / 127.5) - 1.0
                    for (y in 0 until inputHeight) {
                        for (x in 0 until inputWidth) {
                            val pixelIndex = y * inputWidth + x
                            val pixel = pixels[pixelIndex]

                            val r = ((pixel shr 16) and 0xFF).toFloat()
                            val g = ((pixel shr 8) and 0xFF).toFloat()
                            val b = (pixel and 0xFF).toFloat()

                            // Нормализация в [-1, 1]
                            val rNorm = (r / 127.5f) - 1.0f
                            val gNorm = (g / 127.5f) - 1.0f
                            val bNorm = (b / 127.5f) - 1.0f

                            if (isCHWFormat) {
                                // CHW формат: [1, C, H, W]
                                inputArray[0][0][y][x] = rNorm
                                inputArray[0][1][y][x] = gNorm
                                inputArray[0][2][y][x] = bNorm
                            } else {
                                // HWC формат: [1, H, W, C]
                                inputArray[0][y][x][0] = rNorm
                                inputArray[0][y][x][1] = gNorm
                                inputArray[0][y][x][2] = bNorm
                            }
                        }
                    }

                    // Изменяем размер входного тензора под размер изображения
                    val inputShapeDynamic =
                        if (isCHWFormat) {
                            intArrayOf(1, inputChannels, inputHeight, inputWidth)
                        } else {
                            intArrayOf(1, inputHeight, inputWidth, inputChannels)
                        }

                    android.util.Log.d(
                        "AnimeGan2ImageProcessor",
                        "Resizing input tensor to: ${inputShapeDynamic.contentToString()}",
                    )

                    // Используем синхронизацию для resizeInput и allocateTensors
                    val updatedInputDetails: org.tensorflow.lite.Tensor
                    val updatedOutputDetails: org.tensorflow.lite.Tensor
                    val updatedInputShape: IntArray
                    val updatedOutputShape: IntArray

                    interpreterMutex.withLock {
                        interpreter.resizeInput(0, inputShapeDynamic)
                        interpreter.allocateTensors()

                        // Обновляем детали тензоров после изменения размера
                        updatedInputDetails = interpreter.getInputTensor(0)
                        updatedOutputDetails = interpreter.getOutputTensor(0)
                        updatedInputShape = updatedInputDetails.shape()
                        updatedOutputShape = updatedOutputDetails.shape()
                    }

                    android.util.Log.d(
                        "AnimeGan2ImageProcessor",
                        "Updated input shape: ${updatedInputShape.contentToString()}",
                    )
                    android.util.Log.d(
                        "AnimeGan2ImageProcessor",
                        "Updated output shape: ${updatedOutputShape.contentToString()}",
                    )

                    // Проверяем, правильно ли обновился выходной тензор
                    // Если выходной тензор не обновился (все еще [1, 3, 1, 1]), вычисляем размер вручную
                    val outputHeight: Int
                    val outputWidth: Int

                    if (updatedOutputShape.size >= 4 && updatedOutputShape[2] == 1 && updatedOutputShape[3] == 1) {
                        // Выходной тензор не обновился - используем размеры входного тензора
                        outputHeight = if (isCHWFormat) updatedInputShape[2] else updatedInputShape[1]
                        outputWidth = if (isCHWFormat) updatedInputShape[3] else updatedInputShape[2]
                        android.util.Log.d(
                            "AnimeGan2ImageProcessor",
                            "Output tensor not updated, using input dimensions: ${outputWidth}x$outputHeight",
                        )
                    } else {
                        // Выходной тензор обновился правильно
                        outputHeight = if (isCHWFormat) updatedOutputShape[2] else updatedOutputShape[1]
                        outputWidth = if (isCHWFormat) updatedOutputShape[3] else updatedOutputShape[2]
                        android.util.Log.d(
                            "AnimeGan2ImageProcessor",
                            "Output tensor updated, using output dimensions: ${outputWidth}x$outputHeight",
                        )
                    }

                    // Создаем ByteBuffer для входных данных
                    val inputBuffer = ByteBuffer.allocateDirect(updatedInputDetails.numBytes())
                    inputBuffer.order(ByteOrder.nativeOrder())

                    // Заполняем входной буфер
                    when (inputDataType) {
                        org.tensorflow.lite.DataType.FLOAT32 -> {
                            if (isCHWFormat) {
                                // CHW: [1, C, H, W]
                                for (c in 0 until inputChannels) {
                                    for (y in 0 until inputHeight) {
                                        for (x in 0 until inputWidth) {
                                            inputBuffer.putFloat(inputArray[0][c][y][x])
                                        }
                                    }
                                }
                            } else {
                                // HWC: [1, H, W, C]
                                for (y in 0 until inputHeight) {
                                    for (x in 0 until inputWidth) {
                                        for (c in 0 until inputChannels) {
                                            inputBuffer.putFloat(inputArray[0][y][x][c])
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            android.util.Log.e(
                                "AnimeGan2ImageProcessor",
                                "Неподдерживаемый тип входных данных: $inputDataType",
                            )
                            return@withContext null
                        }
                    }
                    inputBuffer.rewind()

                    // Вычисляем размер выходного буфера на основе размеров выхода
                    // Размер = batch * channels * height * width * sizeof(float32)
                    val expectedOutputSizeBytes =
                        if (isCHWFormat) {
                            1 * 3 * outputHeight * outputWidth * 4 // float32 = 4 байта
                        } else {
                            1 * outputHeight * outputWidth * 3 * 4 // float32 = 4 байта
                        }

                    android.util.Log.d(
                        "AnimeGan2ImageProcessor",
                        "Creating output buffer: $expectedOutputSizeBytes bytes for ${outputWidth}x$outputHeight (outputHeight=$outputHeight, outputWidth=$outputWidth)",
                    )

                    // Проверяем фактический размер выходного тензора
                    val actualOutputSizeBytes = updatedOutputDetails.numBytes()
                    android.util.Log.d(
                        "AnimeGan2ImageProcessor",
                        "Output tensor numBytes: $actualOutputSizeBytes, expected: $expectedOutputSizeBytes",
                    )

                    // Проблема: после resizeInput выходной тензор не обновляется правильно (остается [1, 3, 1, 1]),
                    // но модель выдает данные правильного размера (1769472 байта для 288x512).
                    // interpreter.run() проверяет размер выходного тензора перед копированием и выдает ошибку.
                    // Решение: используем прямой доступ к выходному тензору через getOutputTensor()
                    // после инференса и читаем данные с правильным размером.

                    // Создаем выходной буфер с вычисленным размером на основе входных размеров
                    val outputBuffer = ByteBuffer.allocateDirect(expectedOutputSizeBytes)
                    outputBuffer.order(ByteOrder.nativeOrder())

                    android.util.Log.d(
                        "AnimeGan2ImageProcessor",
                        "Created output buffer: $expectedOutputSizeBytes bytes for ${outputWidth}x$outputHeight",
                    )

                    // Инференс через модель AnimeGAN2 с синхронизацией через мьютекс
                    // Используем подход: выполняем инференс, затем получаем выходной тензор и читаем данные
                    interpreterMutex.withLock {
                        // Выполняем инференс с выходным буфером правильного размера
                        // TFLite может не проверить размер тензора, если использовать правильный размер буфера
                        try {
                            interpreter.run(inputBuffer, outputBuffer)
                            android.util.Log.d(
                                "AnimeGan2ImageProcessor",
                                "Inference completed using run()",
                            )
                        } catch (e: IllegalArgumentException) {
                            // Если run() выдает ошибку из-за размера тензора, используем альтернативный подход
                            android.util.Log.w(
                                "AnimeGan2ImageProcessor",
                                "run() failed due to tensor size mismatch: ${e.message}",
                            )
                            android.util.Log.w(
                                "AnimeGan2ImageProcessor",
                                "Trying alternative approach: reading from tensor after inference",
                            )

                            // Альтернативный подход: используем TensorBuffer для работы с выходными данными
                            // TensorBuffer может автоматически обработать размер и обойти проверку тензора
                            val outputShapeForTensorBuffer =
                                if (isCHWFormat) {
                                    intArrayOf(1, 3, outputHeight, outputWidth)
                                } else {
                                    intArrayOf(1, outputHeight, outputWidth, 3)
                                }

                            val tensorBuffer =
                                TensorBuffer.createFixedSize(
                                    outputShapeForTensorBuffer,
                                    outputDataType,
                                )

                            // Используем run() с буфером из TensorBuffer
                            // TensorBuffer автоматически обработает размер
                            try {
                                interpreter.run(inputBuffer, tensorBuffer.buffer)
                                android.util.Log.d(
                                    "AnimeGan2ImageProcessor",
                                    "Inference completed using TensorBuffer",
                                )

                                // Копируем данные из TensorBuffer в выходной буфер
                                tensorBuffer.buffer.rewind()
                                outputBuffer.position(0)
                                outputBuffer.put(tensorBuffer.buffer)
                                outputBuffer.rewind()
                            } catch (e2: IllegalArgumentException) {
                                // Если и это не работает, возвращаем null
                                android.util.Log.e(
                                    "AnimeGan2ImageProcessor",
                                    "Cannot perform inference: ${e2.message}",
                                )
                                return@withContext null
                            }
                        }
                    }

                    // Определяем финальные размеры выхода (такие же, как входные)
                    val finalOutputHeight = outputHeight
                    val finalOutputWidth = outputWidth

                    android.util.Log.d(
                        "AnimeGan2ImageProcessor",
                        "Final output dimensions: ${finalOutputWidth}x$finalOutputHeight",
                    )

                    // Обрабатываем выходные данные
                    outputBuffer.rewind()

                    val outputBitmap =
                        Bitmap.createBitmap(
                            finalOutputWidth,
                            finalOutputHeight,
                            Bitmap.Config.ARGB_8888,
                        )
                    val outputPixels = IntArray(finalOutputWidth * finalOutputHeight)

                    when (outputDataType) {
                        org.tensorflow.lite.DataType.FLOAT32 -> {
                            // Модель возвращает данные в диапазоне [-1, 1]
                            // Денормализация: ((output_img + 1.0) * 127.5)
                            var pixelIndex = 0

                            // Определяем формат выхода (такой же, как входной)
                            val isOutputCHW = isCHWFormat

                            if (isOutputCHW) {
                                // CHW формат: [1, C, H, W]
                                val rValues = FloatArray(finalOutputWidth * finalOutputHeight)
                                val gValues = FloatArray(finalOutputWidth * finalOutputHeight)
                                val bValues = FloatArray(finalOutputWidth * finalOutputHeight)

                                // Читаем каналы последовательно
                                for (c in 0 until 3) {
                                    for (y in 0 until finalOutputHeight) {
                                        for (x in 0 until finalOutputWidth) {
                                            val index = y * finalOutputWidth + x
                                            val value = outputBuffer.getFloat()
                                            when (c) {
                                                0 -> rValues[index] = value
                                                1 -> gValues[index] = value
                                                2 -> bValues[index] = value
                                            }
                                        }
                                    }
                                }

                                // Денормализуем и создаем пиксели
                                for (y in 0 until finalOutputHeight) {
                                    for (x in 0 until finalOutputWidth) {
                                        val index = y * finalOutputWidth + x
                                        val rNorm = rValues[index]
                                        val gNorm = gValues[index]
                                        val bNorm = bValues[index]

                                        // Денормализация из [-1, 1] в [0, 255]
                                        val r = ((rNorm + 1.0f) * 127.5f).coerceIn(0f, 255f).toInt()
                                        val g = ((gNorm + 1.0f) * 127.5f).coerceIn(0f, 255f).toInt()
                                        val b = ((bNorm + 1.0f) * 127.5f).coerceIn(0f, 255f).toInt()

                                        outputPixels[pixelIndex++] = (255 shl 24) or (r shl 16) or (g shl 8) or b
                                    }
                                }
                            } else {
                                // HWC формат: [1, H, W, C]
                                for (y in 0 until finalOutputHeight) {
                                    for (x in 0 until finalOutputWidth) {
                                        val rNorm = outputBuffer.getFloat()
                                        val gNorm = outputBuffer.getFloat()
                                        val bNorm = outputBuffer.getFloat()

                                        // Денормализация из [-1, 1] в [0, 255]
                                        val r = ((rNorm + 1.0f) * 127.5f).coerceIn(0f, 255f).toInt()
                                        val g = ((gNorm + 1.0f) * 127.5f).coerceIn(0f, 255f).toInt()
                                        val b = ((bNorm + 1.0f) * 127.5f).coerceIn(0f, 255f).toInt()

                                        outputPixels[pixelIndex++] = (255 shl 24) or (r shl 16) or (g shl 8) or b
                                    }
                                }
                            }
                        }
                        else -> {
                            android.util.Log.e(
                                "AnimeGan2ImageProcessor",
                                "Неподдерживаемый тип выходных данных: $outputDataType",
                            )
                            return@withContext null
                        }
                    }

                    outputBitmap.setPixels(
                        outputPixels,
                        0,
                        finalOutputWidth,
                        0,
                        0,
                        finalOutputWidth,
                        finalOutputHeight,
                    )

                    android.util.Log.d(
                        "AnimeGan2ImageProcessor",
                        "Обработка завершена: ${finalOutputWidth}x$finalOutputHeight",
                    )
                    outputBitmap
                } catch (e: Exception) {
                    android.util.Log.e(
                        "AnimeGan2ImageProcessor",
                        "Ошибка обработки через AnimeGAN2: ${e.message}",
                        e,
                    )
                    e.printStackTrace()
                    null
                }
            }
    }
