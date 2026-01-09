package com.example.neuralphotoredactor.ml.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import com.example.neuralphotoredactor.domain.enums.EditType
import javax.inject.Inject

/**
 * Реализация процессора для редактирования изображений.
 */
class ImageEditProcessorImpl @Inject constructor() : ImageEditProcessor {
    
    override fun applyEdit(
        bitmap: Bitmap,
        editType: EditType,
        value: Float,
        cropRect: Rect?
    ): Bitmap? {
        return try {
            when (editType) {
                EditType.CROP -> cropImage(bitmap, cropRect)
                EditType.ROTATE_90 -> rotateImage(bitmap, 90f)
                EditType.ROTATE_180 -> rotateImage(bitmap, 180f)
                EditType.ROTATE_270 -> rotateImage(bitmap, 270f)
                EditType.FLIP_HORIZONTAL -> flipImage(bitmap, horizontal = true)
                EditType.FLIP_VERTICAL -> flipImage(bitmap, horizontal = false)
                EditType.BRIGHTNESS -> adjustBrightness(bitmap, value)
                EditType.CONTRAST -> adjustContrast(bitmap, value)
                EditType.COLOR_BALANCE_RED -> adjustColorBalance(bitmap, value, 0f, 0f)
                EditType.COLOR_BALANCE_GREEN -> adjustColorBalance(bitmap, 0f, value, 0f)
                EditType.COLOR_BALANCE_BLUE -> adjustColorBalance(bitmap, 0f, 0f, value)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageEditProcessor", "Ошибка применения редактирования: ${e.message}", e)
            null
        }
    }
    
    /**
     * Кадрирование изображения.
     * 
     * @param bitmap Исходное изображение
     * @param cropRect Прямоугольник кадрирования в пикселях или null
     * @param displayWidth Ширина отображения изображения в UI (для масштабирования координат)
     * @param displayHeight Высота отображения изображения в UI (для масштабирования координат)
     * @return Обрезанное изображение или null в случае ошибки
     */
    private fun cropImage(
        bitmap: Bitmap,
        cropRect: Rect?,
        displayWidth: Int? = null,
        displayHeight: Int? = null
    ): Bitmap? {
        if (cropRect == null) {
            android.util.Log.w("ImageEditProcessor", "cropRect равен null, обрезка невозможна")
            return null
        }
        
        // Проверяем, что bitmap валиден
        if (bitmap.isRecycled) {
            android.util.Log.e("ImageEditProcessor", "Bitmap переработан, обрезка невозможна")
            return null
        }
        
        // Масштабируем координаты, если они приходят из UI с другим размером
        val scaledRect = if (displayWidth != null && displayHeight != null && 
                            displayWidth > 0 && displayHeight > 0) {
            scaleCropRect(cropRect, bitmap.width, bitmap.height, displayWidth, displayHeight)
        } else {
            cropRect
        }
        
        // Валидация и нормализация координат
        val left = scaledRect.left.coerceIn(0, bitmap.width)
        val top = scaledRect.top.coerceIn(0, bitmap.height)
        val right = scaledRect.right.coerceIn(left + 1, bitmap.width)
        val bottom = scaledRect.bottom.coerceIn(top + 1, bitmap.height)
        
        // Проверяем, что прямоугольник валиден
        val width = right - left
        val height = bottom - top
        
        if (width <= 0 || height <= 0) {
            android.util.Log.e("ImageEditProcessor", 
                "Некорректные размеры области обрезки: width=$width, height=$height")
            return null
        }
        
        // Проверяем минимальный размер (хотя бы 1x1 пиксель)
        if (width < 1 || height < 1) {
            android.util.Log.e("ImageEditProcessor", 
                "Область обрезки слишком мала: ${width}x${height}")
            return null
        }
        
        return try {
            val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
            
            android.util.Log.d("ImageEditProcessor", 
                "Обрезка выполнена успешно: исходное ${bitmap.width}x${bitmap.height} -> результат ${croppedBitmap.width}x${croppedBitmap.height}")
            
            croppedBitmap
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("ImageEditProcessor", 
                "Некорректные параметры обрезки: left=$left, top=$top, width=$width, height=$height, " +
                "bitmap size=${bitmap.width}x${bitmap.height}", e)
            null
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("ImageEditProcessor", 
                "Недостаточно памяти для обрезки изображения", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("ImageEditProcessor", 
                "Ошибка кадрирования: ${e.message}", e)
            null
        }
    }
    
    /**
     * Масштабирует координаты обрезки из координат UI в координаты bitmap.
     * 
     * @param uiRect Прямоугольник в координатах UI
     * @param bitmapWidth Ширина bitmap в пикселях
     * @param bitmapHeight Высота bitmap в пикселях
     * @param displayWidth Ширина отображения в UI
     * @param displayHeight Высота отображения в UI
     * @return Прямоугольник в координатах bitmap
     */
    private fun scaleCropRect(
        uiRect: Rect,
        bitmapWidth: Int,
        bitmapHeight: Int,
        displayWidth: Int,
        displayHeight: Int
    ): Rect {
        // Вычисляем масштаб с учетом соотношения сторон
        val bitmapAspect = bitmapWidth.toFloat() / bitmapHeight.toFloat()
        val displayAspect = displayWidth.toFloat() / displayHeight.toFloat()
        
        val scaleX: Float
        val scaleY: Float
        val offsetX: Float
        val offsetY: Float
        
        // Определяем, как изображение масштабируется в UI (ContentScale.Fit)
        if (bitmapAspect > displayAspect) {
            // Изображение шире - масштабируется по ширине
            scaleX = bitmapWidth.toFloat() / displayWidth.toFloat()
            scaleY = scaleX
            offsetX = 0f
            offsetY = (displayHeight - bitmapHeight / scaleY) / 2f
        } else {
            // Изображение выше - масштабируется по высоте
            scaleY = bitmapHeight.toFloat() / displayHeight.toFloat()
            scaleX = scaleY
            offsetX = (displayWidth - bitmapWidth / scaleX) / 2f
            offsetY = 0f
        }
        
        // Масштабируем координаты
        val left = ((uiRect.left - offsetX) * scaleX).toInt().coerceIn(0, bitmapWidth)
        val top = ((uiRect.top - offsetY) * scaleY).toInt().coerceIn(0, bitmapHeight)
        val right = ((uiRect.right - offsetX) * scaleX).toInt().coerceIn(left, bitmapWidth)
        val bottom = ((uiRect.bottom - offsetY) * scaleY).toInt().coerceIn(top, bitmapHeight)
        
        return Rect(left, top, right, bottom)
    }
    
    /**
     * Поворот изображения.
     */
    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap? {
        return try {
            val matrix = Matrix().apply {
                postRotate(degrees)
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            android.util.Log.e("ImageEditProcessor", "Ошибка поворота: ${e.message}", e)
            null
        }
    }
    
    /**
     * Отражение изображения.
     */
    private fun flipImage(bitmap: Bitmap, horizontal: Boolean): Bitmap? {
        return try {
            val matrix = Matrix().apply {
                if (horizontal) {
                    postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                } else {
                    postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
                }
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            android.util.Log.e("ImageEditProcessor", "Ошибка отражения: ${e.message}", e)
            null
        }
    }
    
    /**
     * Коррекция яркости.
     * value: -1.0 до 1.0 (отрицательные значения затемняют, положительные осветляют)
     */
    private fun adjustBrightness(bitmap: Bitmap, value: Float): Bitmap? {
        return try {
            val adjustedValue = value.coerceIn(-1f, 1f) * 255f
            val colorMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, adjustedValue,
                0f, 1f, 0f, 0f, adjustedValue,
                0f, 0f, 1f, 0f, adjustedValue,
                0f, 0f, 0f, 1f, 0f
            ))
            applyColorMatrix(bitmap, colorMatrix)
        } catch (e: Exception) {
            android.util.Log.e("ImageEditProcessor", "Ошибка коррекции яркости: ${e.message}", e)
            null
        }
    }
    
    /**
     * Коррекция контраста.
     * value: -1.0 до 1.0 (отрицательные значения уменьшают контраст, положительные увеличивают)
     */
    private fun adjustContrast(bitmap: Bitmap, value: Float): Bitmap? {
        return try {
            val adjustedValue = value.coerceIn(-1f, 1f)
            val scale = 1f + adjustedValue
            val translate = (-.5f * scale + .5f) * 255f
            
            val colorMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            applyColorMatrix(bitmap, colorMatrix)
        } catch (e: Exception) {
            android.util.Log.e("ImageEditProcessor", "Ошибка коррекции контраста: ${e.message}", e)
            null
        }
    }
    
    /**
     * Настройка цветового баланса.
     * value: -1.0 до 1.0 для каждого канала (R, G, B)
     */
    private fun adjustColorBalance(bitmap: Bitmap, red: Float, green: Float, blue: Float): Bitmap? {
        return try {
            val r = red.coerceIn(-1f, 1f)
            val g = green.coerceIn(-1f, 1f)
            val b = blue.coerceIn(-1f, 1f)
            
            val colorMatrix = ColorMatrix(floatArrayOf(
                1f + r, 0f, 0f, 0f, 0f,
                0f, 1f + g, 0f, 0f, 0f,
                0f, 0f, 1f + b, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            applyColorMatrix(bitmap, colorMatrix)
        } catch (e: Exception) {
            android.util.Log.e("ImageEditProcessor", "Ошибка настройки цветового баланса: ${e.message}", e)
            null
        }
    }
    
    /**
     * Применить ColorMatrix к изображению.
     */
    private fun applyColorMatrix(bitmap: Bitmap, colorMatrix: ColorMatrix): Bitmap? {
        return try {
            val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint().apply {
                colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            result
        } catch (e: Exception) {
            android.util.Log.e("ImageEditProcessor", "Ошибка применения ColorMatrix: ${e.message}", e)
            null
        }
    }
}
