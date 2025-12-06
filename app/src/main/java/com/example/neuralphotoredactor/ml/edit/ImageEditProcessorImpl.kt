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
     */
    private fun cropImage(bitmap: Bitmap, cropRect: Rect?): Bitmap? {
        if (cropRect == null) return null
        
        // Проверяем границы
        val left = cropRect.left.coerceIn(0, bitmap.width)
        val top = cropRect.top.coerceIn(0, bitmap.height)
        val right = cropRect.right.coerceIn(left, bitmap.width)
        val bottom = cropRect.bottom.coerceIn(top, bitmap.height)
        
        if (left >= right || top >= bottom) return null
        
        return try {
            Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        } catch (e: Exception) {
            android.util.Log.e("ImageEditProcessor", "Ошибка кадрирования: ${e.message}", e)
            null
        }
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
