package com.example.neuralphotoredactor.ml.filter.agsl

/**
 * AGSL (Android Graphics Shading Language) шейдеры для фильтров изображений.
 *
 * Все шейдеры оптимизированы для GPU-ускорения и работают на API 31+.
 */
object AGSLShaders {
    /**
     * Шейдер для преобразования в grayscale (чёрно-белое).
     */
    val GRAYSCALE_SHADER =
        """
        uniform shader input;
        
        half4 main(float2 coord) {
            half4 color = input.eval(coord);
            // Стандартная формула для grayscale: 0.299*R + 0.587*G + 0.114*B
            half gray = dot(color.rgb, half3(0.299, 0.587, 0.114));
            return half4(gray, gray, gray, color.a);
        }
        """.trimIndent()

    /**
     * Альтернативный способ - через Paint и Shader (для совместимости).
     */
    val GRAYSCALE_SHADER_ALT =
        """
        uniform shader input;
        
        half4 main(float2 coord) {
            half4 c = input.eval(coord);
            half gray = dot(c.rgb, half3(0.299, 0.587, 0.114));
            return half4(gray, gray, gray, c.a);
        }
        """.trimIndent()

    /**
     * Шейдер для сепия эффекта.
     */
    val SEPIA_SHADER =
        """
        uniform shader input;
        uniform float intensity;
        
        half4 main(float2 coord) {
            half4 color = input.eval(coord);
            // Сначала преобразуем в grayscale
            half gray = dot(color.rgb, half3(0.299, 0.587, 0.114));
            
            // Применяем сепия тонирование
            half r = gray * (0.393 + 0.607 * (1.0 - intensity)) + 
                     color.r * (0.607 * intensity);
            half g = gray * (0.769 - 0.769 * intensity) + 
                     color.g * (0.769 * intensity);
            half b = gray * (0.189 - 0.189 * intensity) + 
                     color.b * (0.189 * intensity);
            
            return half4(r, g, b, color.a);
        }
        """.trimIndent()

    /**
     * Шейдер для виньетки (радиальное затемнение от центра к краям).
     */
    val VIGNETTE_SHADER =
        """
        uniform shader input;
        uniform float2 size;
        uniform float intensity;
        
        half4 main(float2 coord) {
            half4 color = input.eval(coord);
            
            // Координаты нормализованы (0-1), но size в пикселях
            // Преобразуем coord в пиксели для вычисления расстояния
            float2 coordPx = coord * size;
            float2 center = size * 0.5;
            float dist = distance(coordPx, center);
            float maxDist = length(size) * 0.5;
            
            // Вычисляем фактор виньетки (квадратичное затухание)
            float startRadius = maxDist * 0.3;
            float vignetteFactor = 1.0;
            
            if (dist > startRadius) {
                float t = (dist - startRadius) / (maxDist - startRadius);
                vignetteFactor = 1.0 - intensity * t * t;
            }
            
            return color * vignetteFactor;
        }
        """.trimIndent()

    /**
     * Шейдер для резкости (unsharp mask).
     * Использует 3x3 ядро для повышения резкости.
     */
    val SHARPEN_SHADER =
        """
        uniform shader input;
        uniform float2 size;
        uniform float strength;
        
        half4 main(float2 coord) {
            // coord уже нормализован (0-1), pixelSize = 1/size
            float2 pixelSize = float2(1.0 / size.x, 1.0 / size.y);
            
            // Получаем центральный пиксель
            half4 center = input.eval(coord);
            
            // Получаем соседние пиксели для convolution
            half4 top = input.eval(coord + float2(0.0, -pixelSize.y));
            half4 bottom = input.eval(coord + float2(0.0, pixelSize.y));
            half4 left = input.eval(coord + float2(-pixelSize.x, 0.0));
            half4 right = input.eval(coord + float2(pixelSize.x, 0.0));
            
            // Ядро резкости: [0, -s, 0; -s, 1+4s, -s; 0, -s, 0]
            half4 result = center * (1.0 + 4.0 * strength) - 
                          (top + bottom + left + right) * strength;
            
            return clamp(result, 0.0, 1.0);
        }
        """.trimIndent()

    /**
     * Шейдер для размытия по Гауссу (упрощенная версия).
     * Для лучшего качества используется встроенный createBlurEffect.
     */
    val GAUSSIAN_BLUR_SHADER =
        """
        uniform shader input;
        uniform float2 size;
        uniform float radius;
        
        half4 main(float2 coord) {
            // coord нормализован (0-1), pixelSize = 1/size
            float2 pixelSize = float2(1.0 / size.x, 1.0 / size.y);
            half4 color = half4(0.0);
            float totalWeight = 0.0;
            
            // Упрощенное размытие по Гауссу (5x5 ядро)
            float sigma = radius / 3.0;
            
            for (int y = -2; y <= 2; y++) {
                for (int x = -2; x <= 2; x++) {
                    float2 offset = float2(float(x), float(y)) * pixelSize;
                    float dist = length(float2(float(x), float(y)));
                    float weight = exp(-(dist * dist) / (2.0 * sigma * sigma));
                    
                    color += input.eval(coord + offset) * weight;
                    totalWeight += weight;
                }
            }
            
            return totalWeight > 0.0 ? color / totalWeight : input.eval(coord);
        }
        """.trimIndent()

    /**
     * Шейдер для удаления шумов (bilateral filter - упрощенная версия).
     */
    val NOISE_REDUCTION_SHADER =
        """
        uniform shader input;
        uniform float2 size;
        uniform float intensity;
        
        half4 main(float2 coord) {
            // coord нормализован (0-1), pixelSize = 1/size
            float2 pixelSize = float2(1.0 / size.x, 1.0 / size.y);
            half4 center = input.eval(coord);
            half4 sum = half4(0.0);
            float totalWeight = 0.0;
            
            // Медианный фильтр через усреднение с весами
            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    float2 offset = float2(float(x), float(y)) * pixelSize;
                    half4 sample = input.eval(coord + offset);
                    
                    // Вес зависит от разницы цветов (bilateral filter)
                    float colorDiff = length(sample.rgb - center.rgb);
                    float weight = exp(-colorDiff * intensity * 10.0);
                    
                    sum += sample * weight;
                    totalWeight += weight;
                }
            }
            
            return totalWeight > 0.0 ? sum / totalWeight : center;
        }
        """.trimIndent()
}
