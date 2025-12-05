package com.example.neuralphotoredactor.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * Утилита для сохранения обработанных изображений.
 */
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Сохранить Bitmap в файл.
     * 
     * @param bitmap Изображение для сохранения
     * @param fileName Имя файла
     * @return URI сохраненного файла или null в случае ошибки
     */
    suspend fun saveBitmap(bitmap: Bitmap, fileName: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val imagesDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "processed"
            )
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            
            val imageFile = File(imagesDir, fileName)
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            Uri.fromFile(imageFile)
        } catch (e: IOException) {
            null
        }
    }
    
    /**
     * Удалить файл по URI.
     * 
     * @param uri URI файла для удаления
     */
    suspend fun deleteFile(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val file = File(uri.path ?: return@withContext)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            // Игнорируем ошибки удаления
        }
    }
    
    /**
     * Получить список всех обработанных изображений из папки processed.
     * 
     * @return Список URI обработанных изображений, отсортированных по дате создания (новые первыми)
     */
    suspend fun getProcessedImages(): List<Uri> = withContext(Dispatchers.IO) {
        try {
            val imagesDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "processed"
            )
            
            if (!imagesDir.exists() || !imagesDir.isDirectory) {
                return@withContext emptyList()
            }
            
            val imageFiles = imagesDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) || 
                               file.name.endsWith(".jpeg", ignoreCase = true) ||
                               file.name.endsWith(".png", ignoreCase = true))
            } ?: return@withContext emptyList()
            
            // Сортируем по дате изменения (новые первыми)
            val sortedFiles = imageFiles.sortedByDescending { it.lastModified() }
            
            sortedFiles.map { Uri.fromFile(it) }
        } catch (e: Exception) {
            android.util.Log.e("ImageStorage", "Ошибка получения обработанных изображений: ${e.message}", e)
            emptyList()
        }
    }
}

