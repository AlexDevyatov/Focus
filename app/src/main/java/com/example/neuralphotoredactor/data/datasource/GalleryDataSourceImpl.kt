package com.example.neuralphotoredactor.data.datasource

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.neuralphotoredactor.domain.model.ImageData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Реализация источника данных для работы с галереей.
 * 
 * Оптимизирована для производительности:
 * - Пагинация запросов к MediaStore
 * - Кэширование результатов
 * - Оптимизированная projection (без WIDTH/HEIGHT для превью)
 */
class GalleryDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GalleryDataSource {
    
    companion object {
        private const val TAG = "GalleryDataSource"
        private const val DEFAULT_PAGE_SIZE = 50
        private const val CACHE_SIZE = 200 // Кэшируем первые 200 изображений
    }
    
    // Кэш для первых изображений (для быстрого отображения)
    private var cachedImages: List<ImageData>? = null
    private var cacheTimestamp: Long = 0
    private val cacheValidityMs = 5 * 60 * 1000L // 5 минут
    
    override suspend fun invalidateCache() {
        cachedImages = null
        cacheTimestamp = 0
        Log.d(TAG, "Cache invalidated")
    }
    
    override suspend fun pickImage(): ImageData? {
        // Реализация выбора изображения через Intent будет в UI слое
        // Здесь возвращаем null, так как выбор происходит через Activity Result
        return null
    }
    
    override suspend fun getAllImages(): List<ImageData> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            // Проверяем кэш
            val now = System.currentTimeMillis()
            if (cachedImages != null && (now - cacheTimestamp) < cacheValidityMs) {
                Log.d(TAG, "Using cached images (${cachedImages!!.size} items)")
                return@withContext cachedImages!!
            }
            
            val images = mutableListOf<ImageData>()
            // Оптимизированная projection: убираем WIDTH/HEIGHT для ускорения запроса
            // Эти данные можно получить позже при необходимости
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED
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
                
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    
                    // Для превью не нужны точные размеры - Coil сам оптимизирует
                    images.add(ImageData(uri))
                }
            }
            
            // Кэшируем первые CACHE_SIZE изображений
            if (images.size <= CACHE_SIZE) {
                cachedImages = images
                cacheTimestamp = System.currentTimeMillis()
            } else {
                cachedImages = images.take(CACHE_SIZE)
                cacheTimestamp = System.currentTimeMillis()
            }
            
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Loaded ${images.size} images in ${duration}ms")
            
            images
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Permission not granted", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading images", e)
            throw e
        }
    }
    
    override suspend fun getImagesPaginated(limit: Int, offset: Int): List<ImageData> = 
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val images = mutableListOf<ImageData>()
                // Минимальная projection для быстрой загрузки
                val projection = arrayOf(MediaStore.Images.Media._ID)
                
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
                    
                    // Перемещаемся к offset позиции (moveToPosition возвращает true если успешно)
                    if (it.moveToPosition(offset)) {
                        var count = 0
                        // Читаем limit элементов
                        do {
                            val id = it.getLong(idColumn)
                            val uri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                            images.add(ImageData(uri))
                            count++
                        } while (it.moveToNext() && count < limit)
                    }
                }
                
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "Loaded page (limit=$limit, offset=$offset): ${images.size} images in ${duration}ms")
                
                images
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: Permission not granted", e)
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading paginated images", e)
                throw e
            }
        }
    
    override suspend fun getImageCount(): Int = withContext(Dispatchers.IO) {
        try {
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                null
            )
            
            cursor?.use {
                val count = it.count
                Log.d(TAG, "Total images count: $count")
                return@withContext count
            }
            
            0
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Permission not granted", e)
            0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting image count", e)
            0
        }
    }
}

