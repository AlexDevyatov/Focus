package com.example.neuralphotoredactor.data.datasource

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.neuralphotoredactor.domain.model.ImageData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Реализация источника данных для работы с галереей.
 */
class GalleryDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GalleryDataSource {
    
    override suspend fun pickImage(): ImageData? {
        // Реализация выбора изображения через Intent будет в UI слое
        // Здесь возвращаем null, так как выбор происходит через Activity Result
        return null
    }
    
    override suspend fun getAllImages(): List<ImageData> = withContext(Dispatchers.IO) {
        try {
            val images = mutableListOf<ImageData>()
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.SIZE
            )
            
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )
            
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val widthColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val width = it.getInt(widthColumn)
                    val height = it.getInt(heightColumn)
                    val size = it.getLong(sizeColumn)
                    
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    
                    images.add(ImageData(uri, width, height, size))
                }
            }
            
            // Если cursor == null, это может означать отсутствие разрешения или отсутствие изображений
            // Возвращаем пустой список, так как SecurityException уже обработан выше
            images
        } catch (e: SecurityException) {
            // Разрешение не предоставлено
            emptyList()
        } catch (e: Exception) {
            // Другие ошибки
            throw e
        }
    }
}

