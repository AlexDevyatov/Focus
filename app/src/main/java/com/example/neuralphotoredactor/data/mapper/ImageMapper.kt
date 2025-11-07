package com.example.neuralphotoredactor.data.mapper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.neuralphotoredactor.domain.model.ImageData
import java.io.ByteArrayOutputStream

/**
 * Маппер для работы с изображениями: преобразование в Base64 и обратно.
 * 
 * Используется для передачи изображений через API в формате Base64.
 */
object ImageMapper {
    /**
     * Преобразует изображение в Base64 строку.
     * 
     * @param imageData Данные изображения
     * @param quality Качество сжатия (0-100)
     * @return Base64 строка с изображением
     */
    suspend fun toBase64(imageData: ImageData, quality: Int = 90): String {
        // TODO: Реализовать чтение изображения по URI и конвертацию в Base64
        // Это требует контекста приложения для работы с ContentResolver
        return ""
    }

    /**
     * Преобразует Base64 строку в Bitmap.
     * 
     * @param base64 Base64 строка с изображением
     * @return Bitmap или null, если декодирование не удалось
     */
    fun fromBase64(base64: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Преобразует Bitmap в Base64 строку.
     * 
     * @param bitmap Bitmap изображение
     * @param quality Качество сжатия (0-100)
     * @return Base64 строка с изображением
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 90): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }
}

