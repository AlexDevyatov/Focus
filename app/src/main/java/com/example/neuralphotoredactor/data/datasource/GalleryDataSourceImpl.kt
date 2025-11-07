package com.example.neuralphotoredactor.data.datasource

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.neuralphotoredactor.domain.model.ImageData
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
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn) * 1000 // Конвертируем в миллисекунды
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    
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

