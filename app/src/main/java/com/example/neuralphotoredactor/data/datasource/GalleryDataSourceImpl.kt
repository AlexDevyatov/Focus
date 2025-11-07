package com.example.neuralphotoredactor.data.datasource

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.presentation.util.PermissionHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Реализация источника данных для работы с галереей устройства.
 * 
 * Использует MediaStore API для доступа к изображениям в галерее устройства.
 * Внедряется через Hilt и используется в ImageRepository для получения изображений.
 * 
 * @param context Application контекст для доступа к ContentResolver
 * 
 * @see com.example.neuralphotoredactor.data.datasource.GalleryDataSource
 */
class GalleryDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GalleryDataSource {
    /**
     * Открывает диалог выбора изображения из галереи через системный Intent.
     * 
     * @return ImageData выбранного изображения или null, если выбор был отменен
     */
    override suspend fun pickImage(): ImageData? {
        // Выбор изображения обрабатывается через Activity Result API в UI слое
        return null
    }

    /**
     * Получает список всех изображений из галереи через MediaStore.
     * 
     * @return Список всех доступных изображений из галереи
     */
    override suspend fun getAllImages(): List<ImageData> = withContext(Dispatchers.IO) {
        val images = mutableListOf<ImageData>()
        
        // Проверяем разрешения перед запросом к MediaStore
        val permissions = PermissionHandler.getImagePermissions()
        val hasPermissions = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        if (!hasPermissions) {
            // Возвращаем пустой список, если разрешений нет
            return@withContext emptyList()
        }
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        try {
            val cursor = context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )
            
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn)
                    val size = it.getLong(sizeColumn)
                    val dateAdded = it.getLong(dateColumn) * 1000 // Конвертируем в миллисекунды
                    val width = it.getInt(widthColumn)
                    val height = it.getInt(heightColumn)
                    
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    
                    images.add(
                        ImageData(
                            uri = contentUri,
                            path = null,
                            width = width,
                            height = height,
                            size = size,
                            timestamp = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Логируем ошибку, но возвращаем пустой список
            e.printStackTrace()
        }
        
        images
    }
}

