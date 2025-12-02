package com.example.neuralphotoredactor.ui.coil

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * Модуль для настройки Coil ImageLoader с оптимизациями для галереи.
 * 
 * Оптимизации:
 * - Кэш на диске для превью (50 MB)
 * - Оптимизированный размер памяти (25% доступной памяти)
 * - Включенное кэширование для быстрой загрузки
 */
@Module
@InstallIn(SingletonComponent::class)
object CoilModule {
    
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context
    ): ImageLoader {
        val builder = ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // 25% доступной памяти
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50 MB
                    .build()
            }
            // Оптимизация кэширования
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
        
        // Логирование только в debug режиме
        try {
            val buildConfigClass = Class.forName("com.example.neuralphotoredactor.BuildConfig")
            val debugField = buildConfigClass.getField("DEBUG")
            val isDebug = debugField.getBoolean(null) as Boolean
            if (isDebug) {
                builder.logger(DebugLogger())
            }
        } catch (e: Exception) {
            // Если BuildConfig недоступен, просто не добавляем logger
        }
        
        return builder.build()
    }
}

