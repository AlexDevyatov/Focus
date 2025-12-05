package com.example.neuralphotoredactor.ml.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.neuralphotoredactor.domain.enums.FilterType
import com.example.neuralphotoredactor.ml.filter.agsl.AGSLShaders
import javax.inject.Inject

/**
 * Реализация процессора фильтров изображений.
 * 
 * Использует различные технологии в зависимости от типа фильтра:
 * - RenderEffect для GPU-ускоренных эффектов (API 31+)
 * - ColorMatrix для цветовых преобразований
 * - Convolution для резкости
 * - AGSL для виньетки (через RenderEffect)
 * 
 * Все операции выполняются полностью оффлайн.
 */
class ImageFilterProcessorImpl @Inject constructor() : ImageFilterProcessor {
    
    override fun applyFilter(bitmap: Bitmap, filterType: FilterType, intensity: Float?): Bitmap? {
        return try {
            // AGSL/RenderEffect требует API 33+ для setRenderEffect на Paint
            // Для API 31-32 используем только алгоритмические методы
            val useAGSL = false // Отключено, так как требует API 33+ и hardware acceleration
            
            if (useAGSL) {
                val result = when (filterType) {
                    FilterType.GAUSSIAN_BLUR -> applyGaussianBlurAGSL(bitmap, intensity ?: 0.5f)
                    FilterType.NOISE_REDUCTION -> applyNoiseReductionAGSL(bitmap, intensity ?: 0.5f)
                    FilterType.SHARPEN -> applySharpenAGSL(bitmap, intensity ?: 0.5f)
                    FilterType.VIGNETTE -> applyVignetteAGSL(bitmap, intensity ?: 0.5f)
                    FilterType.GRAYSCALE -> applyGrayscaleAGSL(bitmap)
                    FilterType.SEPIA -> applySepiaAGSL(bitmap, intensity ?: 1.0f)
                    // Старые фильтры - возвращаем null (требуют ML модели)
                    FilterType.ENHANCE,
                    FilterType.STYLE_TRANSFER,
                    FilterType.DENOISE,
                    FilterType.UPSCALE,
                    FilterType.COLOR_CORRECTION -> null
                }
                
                // Если AGSL вернул null, используем fallback
                result ?: run {
                    android.util.Log.w("ImageFilterProcessor", "AGSL вернул null для $filterType, используем fallback")
                    applyFilterFallback(bitmap, filterType, intensity)
                }
            } else {
                // Используем алгоритмические методы (fallback)
                applyFilterFallback(bitmap, filterType, intensity)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageFilterProcessor", "Ошибка применения фильтра ${filterType.name}: ${e.message}", e)
            e.printStackTrace()
            // Fallback на алгоритмические методы при ошибке
            applyFilterFallback(bitmap, filterType, intensity)
        }
    }
    
    /**
     * Fallback метод для применения фильтров (алгоритмические методы).
     */
    private fun applyFilterFallback(bitmap: Bitmap, filterType: FilterType, intensity: Float?): Bitmap? {
        return try {
            when (filterType) {
                FilterType.GAUSSIAN_BLUR -> applyGaussianBlurAlgorithmic(bitmap, intensity ?: 0.5f)
                FilterType.NOISE_REDUCTION -> applyNoiseReduction(bitmap, intensity ?: 0.5f)
                FilterType.SHARPEN -> applySharpen(bitmap, intensity ?: 0.5f)
                FilterType.VIGNETTE -> applyVignetteAlgorithmic(bitmap, intensity ?: 0.5f)
                FilterType.GRAYSCALE -> applyGrayscale(bitmap)
                FilterType.SEPIA -> applySepia(bitmap, intensity ?: 1.0f)
                else -> null
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageFilterProcessor", "Ошибка fallback: ${e.message}", e)
            null
        }
    }
    
    // ==================== AGSL методы (API 31+) ====================
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyGrayscaleAGSL(bitmap: Bitmap): Bitmap? {
        return try {
            val result = applyAGSLShader(bitmap, AGSLShaders.GRAYSCALE_SHADER)
            result ?: run {
                android.util.Log.w("ImageFilterProcessor", "Grayscale AGSL вернул null, используем fallback")
                applyGrayscale(bitmap)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageFilterProcessor", "Ошибка применения Grayscale AGSL: ${e.message}", e)
            applyGrayscale(bitmap) // Fallback
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applySepiaAGSL(bitmap: Bitmap, intensity: Float): Bitmap? {
        return try {
            val shaderCode = AGSLShaders.SEPIA_SHADER.replace(
                "uniform float intensity;",
                "uniform float intensity = $intensity;"
            )
            val result = applyAGSLShader(bitmap, shaderCode)
            result ?: run {
                android.util.Log.w("ImageFilterProcessor", "Sepia AGSL вернул null, используем fallback")
                applySepia(bitmap, intensity)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageFilterProcessor", "Ошибка применения Sepia AGSL: ${e.message}", e)
            applySepia(bitmap, intensity) // Fallback
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyVignetteAGSL(bitmap: Bitmap, intensity: Float): Bitmap? {
        return try {
            val shaderCode = AGSLShaders.VIGNETTE_SHADER
                .replace("uniform float intensity;", "uniform float intensity = $intensity;")
                .replace("uniform float2 size;", "uniform float2 size = float2(${bitmap.width.toFloat()}, ${bitmap.height.toFloat()});")
            val result = applyAGSLShader(bitmap, shaderCode)
            result ?: run {
                android.util.Log.w("ImageFilterProcessor", "Vignette AGSL вернул null, используем fallback")
                applyVignetteAlgorithmic(bitmap, intensity)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageFilterProcessor", "Ошибка применения Vignette AGSL: ${e.message}", e)
            applyVignetteAlgorithmic(bitmap, intensity) // Fallback
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applySharpenAGSL(bitmap: Bitmap, intensity: Float): Bitmap? {
        val strength = intensity * 2f // Максимальная сила 2.0
        return try {
            val shaderCode = AGSLShaders.SHARPEN_SHADER
                .replace("uniform float strength;", "uniform float strength = $strength;")
                .replace("uniform float2 size;", "uniform float2 size = float2(${bitmap.width.toFloat()}, ${bitmap.height.toFloat()});")
            val result = applyAGSLShader(bitmap, shaderCode)
            result ?: run {
                android.util.Log.w("ImageFilterProcessor", "Sharpen AGSL вернул null, используем fallback")
                applySharpen(bitmap, intensity)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageFilterProcessor", "Ошибка применения Sharpen AGSL: ${e.message}", e)
            applySharpen(bitmap, intensity) // Fallback
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyGaussianBlurAGSL(bitmap: Bitmap, intensity: Float): Bitmap? {
        return try {
            // Для размытия используем встроенный createBlurEffect (быстрее и качественнее)
            val radius = intensity * 25f // Максимальный радиус 25px
            val blurEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            val result = applyRenderEffectViaPaint(bitmap, blurEffect)
            result ?: run {
                android.util.Log.w("ImageFilterProcessor", "Gaussian Blur RenderEffect вернул null, используем fallback")
                applyGaussianBlurAlgorithmic(bitmap, intensity)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageFilterProcessor", "Ошибка применения Gaussian Blur: ${e.message}", e)
            applyGaussianBlurAlgorithmic(bitmap, intensity) // Fallback
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyNoiseReductionAGSL(bitmap: Bitmap, intensity: Float): Bitmap? {
        return try {
            val shaderCode = AGSLShaders.NOISE_REDUCTION_SHADER
                .replace("uniform float intensity;", "uniform float intensity = $intensity;")
                .replace("uniform float2 size;", "uniform float2 size = float2(${bitmap.width.toFloat()}, ${bitmap.height.toFloat()});")
            val result = applyAGSLShader(bitmap, shaderCode)
            result ?: run {
                android.util.Log.w("ImageFilterProcessor", "Noise Reduction AGSL вернул null, используем fallback")
                applyNoiseReduction(bitmap, intensity)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageFilterProcessor", "Ошибка применения Noise Reduction AGSL: ${e.message}", e)
            applyNoiseReduction(bitmap, intensity) // Fallback
        }
    }
    
    /**
     * Применить AGSL шейдер к изображению.
     * 
     * @param bitmap Исходное изображение
     * @param shaderCode Код AGSL шейдера
     * @param configureUniforms Функция для настройки uniform переменных
     * @return Обработанное изображение
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyAGSLShader(
        bitmap: Bitmap,
        shaderCode: String
    ): Bitmap? {
        return try {
            android.util.Log.d("ImageFilterProcessor", "Применяем AGSL шейдер, размер Bitmap: ${bitmap.width}x${bitmap.height}")
            
            // Создаем RuntimeShader из кода AGSL
            val runtimeShader = RuntimeShader(shaderCode)
            
            // Создаем BitmapShader из исходного изображения
            val bitmapShader = android.graphics.BitmapShader(
                bitmap,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )
            
            // Устанавливаем входной shader uniform (имя должно совпадать с "uniform shader input;" в коде)
            runtimeShader.setInputShader("input", bitmapShader)
            
            android.util.Log.d("ImageFilterProcessor", "Input shader установлен")
            
            // Создаем RenderEffect из RuntimeShader
            // Второй параметр - имя входного shader uniform в AGSL коде
            val renderEffect = RenderEffect.createRuntimeShaderEffect(
                runtimeShader,
                "input"
            )
            
            android.util.Log.d("ImageFilterProcessor", "RenderEffect создан успешно")
            
            // Применяем через Paint (работает без hardware acceleration)
            val result = applyRenderEffectViaPaint(bitmap, renderEffect)
            
            if (result == null) {
                android.util.Log.e("ImageFilterProcessor", "applyRenderEffectViaPaint вернул null")
            } else {
                android.util.Log.d("ImageFilterProcessor", "Результат создан: ${result.width}x${result.height}")
            }
            
            result
        } catch (e: IllegalStateException) {
            android.util.Log.e("ImageFilterProcessor", "IllegalStateException: ${e.message}", e)
            null
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("ImageFilterProcessor", "IllegalArgumentException: ${e.message}", e)
            android.util.Log.e("ImageFilterProcessor", "Возможно, неправильный синтаксис AGSL шейдера")
            null
        } catch (e: Exception) {
            android.util.Log.e("ImageFilterProcessor", "Ошибка применения AGSL: ${e.javaClass.simpleName}: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Применить RenderEffect к изображению через Paint.
     * Альтернативный подход, который работает без hardware acceleration.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyRenderEffect(
        bitmap: Bitmap,
        createEffect: (RenderEffect?) -> RenderEffect
    ): Bitmap? {
        return try {
            val effect = createEffect(null)
            applyRenderEffectViaPaint(bitmap, effect)
        } catch (e: Exception) {
            android.util.Log.e("ImageFilterProcessor", "Ошибка применения RenderEffect: ${e.message}", e)
            null
        }
    }
    
    /**
     * Применить RenderEffect через Paint (работает без hardware acceleration).
     * 
     * Примечание: setRenderEffect на Paint доступен только с API 33.
     * Для API 31-32 этот метод всегда возвращает null и используется fallback.
     * 
     * ВАЖНО: Этот метод отключен, так как RenderEffect не работает в software rendering.
     * Используются только алгоритмические методы (fallback).
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyRenderEffectViaPaint(bitmap: Bitmap, effect: RenderEffect): Bitmap? {
        // Отключено: RenderEffect требует hardware acceleration и API 33+ для setRenderEffect
        android.util.Log.w("ImageFilterProcessor", "applyRenderEffectViaPaint отключен, используем fallback")
        return null
    }
    
    // ==================== Старые методы (fallback) ====================
    
    /**
     * Применить размытие по Гауссу (алгоритмический подход для fallback).
     */
    private fun applyGaussianBlurAlgorithmic(bitmap: Bitmap, intensity: Float): Bitmap? {
        // Упрощенная реализация размытия для старых версий Android
        val radius = (intensity * 10f).toInt().coerceIn(1, 10)
        return applyConvolutionFilter(bitmap, createGaussianKernel(radius))
    }
    
    /**
     * Применить удаление шумов.
     * 
     * Использует медианный фильтр для удаления шумов.
     * 
     * @param bitmap Исходное изображение
     * @param intensity Интенсивность (0.0 - 1.0)
     * @return Обработанное изображение
     */
    private fun applyNoiseReduction(bitmap: Bitmap, intensity: Float): Bitmap? {
        val kernelSize = (intensity * 5f).toInt().coerceIn(1, 5)
        return applyMedianFilter(bitmap, kernelSize)
    }
    
    /**
     * Применить резкость / Unsharp Mask.
     * 
     * @param bitmap Исходное изображение
     * @param intensity Интенсивность резкости (0.0 - 1.0)
     * @return Обработанное изображение
     */
    private fun applySharpen(bitmap: Bitmap, intensity: Float): Bitmap? {
        return try {
            val strength = intensity * 2f // Максимальная сила 2.0
            val kernel = createSharpenKernel(strength)
            android.util.Log.d("ImageFilterProcessor", "Применяем Sharpen: сила=$strength, размер=${bitmap.width}x${bitmap.height}")
            applyConvolutionFilter(bitmap, kernel)
        } catch (e: Exception) {
            android.util.Log.e("ImageFilterProcessor", "Ошибка Sharpen: ${e.message}", e)
            null
        }
    }
    
    /**
     * Применить виньетку.
     * 
     * Использует AGSL через RenderEffect (API 31+) или алгоритмический подход.
     * 
     * @param bitmap Исходное изображение
     * @param intensity Интенсивность виньетки (0.0 - 1.0)
     * @return Обработанное изображение
     */
    private fun applyVignette(bitmap: Bitmap, intensity: Float): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyVignetteRenderEffect(bitmap, intensity)
        } else {
            applyVignetteAlgorithmic(bitmap, intensity)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyVignetteRenderEffect(bitmap: Bitmap, intensity: Float): Bitmap? {
        // Используем более эффективный подход с радиальным градиентом
        return applyVignetteAlgorithmic(bitmap, intensity)
    }
    
    private fun applyVignetteAlgorithmic(bitmap: Bitmap, intensity: Float): Bitmap? {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val centerX: Float = bitmap.width / 2f
        val centerY: Float = bitmap.height / 2f
        val maxRadius: Float = (kotlin.math.sqrt((bitmap.width * bitmap.width + bitmap.height * bitmap.height).toDouble()) / 2.0).toFloat()
        val startRadius: Float = maxRadius * 0.3f // Начало виньетки на 30% от центра
        
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val idx = y * bitmap.width + x
                val pixel = pixels[idx]
                
                val dx = x - centerX
                val dy = y - centerY
                val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                
                // Вычисляем фактор виньетки
                val vignetteFactor: Float = when {
                    dist < startRadius -> 1.0f
                    dist > maxRadius -> (1.0f - intensity)
                    else -> {
                        val numerator: Float = dist - startRadius
                        val denominator: Float = maxRadius - startRadius
                        val t: Float = numerator / denominator
                        (1.0f - intensity * t * t) // Квадратичное затухание
                    }
                }
                
                val redValue = android.graphics.Color.red(pixel).toFloat()
                val greenValue = android.graphics.Color.green(pixel).toFloat()
                val blueValue = android.graphics.Color.blue(pixel).toFloat()
                
                val r = (redValue * vignetteFactor).toInt().coerceIn(0, 255)
                val g = (greenValue * vignetteFactor).toInt().coerceIn(0, 255)
                val b = (blueValue * vignetteFactor).toInt().coerceIn(0, 255)
                val a = android.graphics.Color.alpha(pixel)
                
                pixels[idx] = android.graphics.Color.argb(a, r, g, b)
            }
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
    
    /**
     * Применить чёрно-белое преобразование.
     * 
     * Использует ColorMatrix для преобразования в grayscale.
     * 
     * @param bitmap Исходное изображение
     * @return Чёрно-белое изображение
     */
    private fun applyGrayscale(bitmap: Bitmap): Bitmap? {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f) // Убираем насыщенность = grayscale
        }
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Применить сепию эффект.
     * 
     * Использует ColorMatrix для создания сепия тона.
     * 
     * @param bitmap Исходное изображение
     * @param intensity Интенсивность эффекта (0.0 - 1.0)
     * @return Изображение с сепия эффектом
     */
    private fun applySepia(bitmap: Bitmap, intensity: Float): Bitmap? {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        
        // ColorMatrix для сепия эффекта
        val colorMatrix = ColorMatrix().apply {
            // Сначала делаем grayscale
            setSaturation(0f)
            
            // Затем применяем сепия тонирование
            val sepiaMatrix = ColorMatrix(floatArrayOf(
                0.393f + 0.607f * (1 - intensity), 0.769f - 0.769f * (1 - intensity), 0.189f - 0.189f * (1 - intensity), 0f, 0f,
                0.349f - 0.349f * (1 - intensity), 0.686f + 0.314f * (1 - intensity), 0.168f - 0.168f * (1 - intensity), 0f, 0f,
                0.272f - 0.272f * (1 - intensity), 0.534f - 0.534f * (1 - intensity), 0.131f + 0.869f * (1 - intensity), 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 0f, 1f
            ))
            
            postConcat(sepiaMatrix)
        }
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Применить медианный фильтр для удаления шумов.
     */
    private fun applyMedianFilter(bitmap: Bitmap, kernelSize: Int): Bitmap? {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val halfKernel = kernelSize / 2
        val resultPixels = pixels.copyOf()
        
        for (y in halfKernel until bitmap.height - halfKernel) {
            for (x in halfKernel until bitmap.width - halfKernel) {
                val neighbors = mutableListOf<Int>()
                
                for (ky in -halfKernel..halfKernel) {
                    for (kx in -halfKernel..halfKernel) {
                        val idx = (y + ky) * bitmap.width + (x + kx)
                        neighbors.add(pixels[idx])
                    }
                }
                
                // Медиана по каждому каналу
                neighbors.sortBy { android.graphics.Color.red(it) }
                val r = android.graphics.Color.red(neighbors[neighbors.size / 2])
                
                neighbors.sortBy { android.graphics.Color.green(it) }
                val g = android.graphics.Color.green(neighbors[neighbors.size / 2])
                
                neighbors.sortBy { android.graphics.Color.blue(it) }
                val b = android.graphics.Color.blue(neighbors[neighbors.size / 2])
                
                val a = android.graphics.Color.alpha(pixels[y * bitmap.width + x])
                resultPixels[y * bitmap.width + x] = android.graphics.Color.argb(a, r, g, b)
            }
        }
        
        result.setPixels(resultPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
    
    /**
     * Применить свёрточный фильтр.
     * Оптимизированная версия с обработкой границ и улучшенной производительностью.
     */
    private fun applyConvolutionFilter(bitmap: Bitmap, kernel: Array<FloatArray>): Bitmap? {
        return try {
            val startTime = System.currentTimeMillis()
            android.util.Log.d("ImageFilterProcessor", "Начало applyConvolutionFilter: ${bitmap.width}x${bitmap.height}, ядро: ${kernel.size}x${kernel.size}")
            
            // Для больших изображений уменьшаем размер для производительности
            val maxDimension = 1500 // Максимальный размер для обработки
            val scaleFactor = if (maxOf(bitmap.width, bitmap.height) > maxDimension) {
                maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            } else {
                1.0f
            }
            
            val workingBitmap = if (scaleFactor < 1.0f) {
                android.util.Log.d("ImageFilterProcessor", "Уменьшаем изображение для обработки: фактор=$scaleFactor")
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scaleFactor).toInt(), (bitmap.height * scaleFactor).toInt(), true)
            } else {
                bitmap
            }
            
            val result = Bitmap.createBitmap(workingBitmap.width, workingBitmap.height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(workingBitmap.width * workingBitmap.height)
            workingBitmap.getPixels(pixels, 0, workingBitmap.width, 0, 0, workingBitmap.width, workingBitmap.height)
            
            val kernelSize = kernel.size
            val halfKernel = kernelSize / 2
            val resultPixels = IntArray(pixels.size)
            
            // Копируем исходные пиксели для обработки границ
            pixels.copyInto(resultPixels)
            
            android.util.Log.d("ImageFilterProcessor", "Обработка центральной области: y от $halfKernel до ${workingBitmap.height - halfKernel}")
            
            // Обрабатываем центральную область (где можно применить полное ядро)
            var processedPixels = 0
            val totalPixels = (workingBitmap.height - 2 * halfKernel) * (workingBitmap.width - 2 * halfKernel)
            
            for (y in halfKernel until workingBitmap.height - halfKernel) {
                for (x in halfKernel until workingBitmap.width - halfKernel) {
                    var r = 0f
                    var g = 0f
                    var b = 0f
                    
                    for (ky in 0 until kernelSize) {
                        for (kx in 0 until kernelSize) {
                            val px = x + kx - halfKernel
                            val py = y + ky - halfKernel
                            val idx = py * workingBitmap.width + px
                            
                            if (idx >= 0 && idx < pixels.size) {
                                val pixel = pixels[idx]
                                val weight = kernel[ky][kx]
                                
                                r += android.graphics.Color.red(pixel) * weight
                                g += android.graphics.Color.green(pixel) * weight
                                b += android.graphics.Color.blue(pixel) * weight
                            }
                        }
                    }
                    
                    val idx = y * workingBitmap.width + x
                    if (idx >= 0 && idx < pixels.size) {
                        val a = android.graphics.Color.alpha(pixels[idx])
                        resultPixels[idx] = android.graphics.Color.argb(
                            a,
                            r.coerceIn(0f, 255f).toInt(),
                            g.coerceIn(0f, 255f).toInt(),
                            b.coerceIn(0f, 255f).toInt()
                        )
                        processedPixels++
                    }
                }
            }
            
            android.util.Log.d("ImageFilterProcessor", "Обработано пикселей: $processedPixels из $totalPixels")
            
            // Применяем результат
            result.setPixels(resultPixels, 0, workingBitmap.width, 0, 0, workingBitmap.width, workingBitmap.height)
            
            // Если изображение было уменьшено, увеличиваем обратно
            val finalResult = if (scaleFactor < 1.0f) {
                android.util.Log.d("ImageFilterProcessor", "Увеличиваем результат обратно до исходного размера")
                Bitmap.createScaledBitmap(result, bitmap.width, bitmap.height, true)
            } else {
                result
            }
            
            // Освобождаем временный bitmap, если был создан
            if (workingBitmap != bitmap) {
                workingBitmap.recycle()
            }
            if (finalResult != result) {
                result.recycle()
            }
            
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("ImageFilterProcessor", "applyConvolutionFilter завершен за ${duration}ms")
            finalResult
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("ImageFilterProcessor", "OutOfMemoryError в applyConvolutionFilter: ${e.message}", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("ImageFilterProcessor", "Ошибка в applyConvolutionFilter: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Создать ядро Гаусса для размытия.
     */
    private fun createGaussianKernel(radius: Int): Array<FloatArray> {
        val size = radius * 2 + 1
        val kernel = Array(size) { FloatArray(size) }
        val sigma = radius / 3f
        var sum = 0f
        
        for (y in -radius..radius) {
            for (x in -radius..radius) {
                val value = kotlin.math.exp(-(x * x + y * y) / (2 * sigma * sigma))
                kernel[y + radius][x + radius] = value
                sum += value
            }
        }
        
        // Нормализация
        for (y in 0 until size) {
            for (x in 0 until size) {
                kernel[y][x] /= sum
            }
        }
        
        return kernel
    }
    
    /**
     * Создать ядро для резкости.
     */
    private fun createSharpenKernel(strength: Float): Array<FloatArray> {
        return arrayOf(
            floatArrayOf(0f, -strength, 0f),
            floatArrayOf(-strength, 1 + 4 * strength, -strength),
            floatArrayOf(0f, -strength, 0f)
        )
    }
}

