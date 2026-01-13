package com.example.neuralphotoredactor.data.storage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Утилита для шаринга изображений в социальные сети.
 */
@Singleton
class ShareHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        // VK может иметь разные package names в зависимости от версии
        private val VK_PACKAGES = listOf("com.vk.katana", "com.vkontakte.android")
        private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
        private const val SHARE_DIR = "share"
    }
    
    /**
     * Проверить, установлено ли приложение ВКонтакте.
     */
    fun isVkInstalled(): Boolean {
        return VK_PACKAGES.any { isAppInstalled(it) }
    }
    
    /**
     * Проверить, установлено ли приложение Telegram.
     */
    fun isTelegramInstalled(): Boolean {
        return isAppInstalled(TELEGRAM_PACKAGE)
    }
    
    /**
     * Проверить, установлено ли приложение по пакету.
     * Использует несколько методов для надежной проверки.
     */
    private fun isAppInstalled(packageName: String): Boolean {
        // Метод 1: Проверка через getPackageInfo (работает на Android < 11 или с queries в манифесте)
        val installedViaPackageInfo = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            android.util.Log.d("ShareHelper", "Приложение $packageName найдено через getPackageInfo")
            true
        } catch (e: PackageManager.NameNotFoundException) {
            android.util.Log.d("ShareHelper", "Приложение $packageName не найдено через getPackageInfo")
            false
        } catch (e: Exception) {
            android.util.Log.e("ShareHelper", "Ошибка проверки установки приложения $packageName через getPackageInfo: ${e.message}", e)
            false
        }
        
        if (installedViaPackageInfo) {
            return true
        }
        
        // Метод 2: Проверка через queryIntentActivities без указания package (работает на Android 11+)
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
            }
            val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            val found = resolveInfos.any { it.activityInfo.packageName == packageName }
            android.util.Log.d("ShareHelper", "Приложение $packageName найдено через queryIntentActivities: $found")
            found
        } catch (e: Exception) {
            android.util.Log.e("ShareHelper", "Ошибка проверки установки приложения $packageName через queryIntentActivities: ${e.message}", e)
            false
        }
    }
    
    /**
     * Создать Intent для шаринга в ВКонтакте.
     * 
     * @param bitmap Изображение для шаринга
     * @return Intent для запуска или null, если приложение не установлено
     */
    suspend fun createVkShareIntent(bitmap: Bitmap): Intent? = withContext(Dispatchers.IO) {
        val vkPackage = VK_PACKAGES.firstOrNull { isAppInstalled(it) }
        if (vkPackage == null) {
            android.util.Log.w("ShareHelper", "ВК не установлено")
            return@withContext null
        }
        
        val uri = createShareableUri(bitmap) ?: return@withContext null
        
        Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "")
            `package` = vkPackage
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * Создать Intent для шаринга в Telegram.
     * 
     * @param bitmap Изображение для шаринга
     * @return Intent для запуска или null, если приложение не установлено
     */
    suspend fun createTelegramShareIntent(bitmap: Bitmap): Intent? = withContext(Dispatchers.IO) {
        if (!isTelegramInstalled()) {
            return@withContext null
        }
        
        val uri = createShareableUri(bitmap) ?: return@withContext null
        
        Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "")
            `package` = TELEGRAM_PACKAGE
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * Создать стандартный Intent для шаринга с системным диалогом выбора приложения.
     * 
     * @param bitmap Изображение для шаринга
     * @param chooserTitle Заголовок для диалога выбора (опционально)
     * @return Intent с chooser для запуска или null в случае ошибки
     */
    suspend fun createShareIntent(bitmap: Bitmap, chooserTitle: String? = null): Intent? = withContext(Dispatchers.IO) {
        val uri = createShareableUri(bitmap) ?: return@withContext null
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        Intent.createChooser(shareIntent, chooserTitle)
    }
    
    /**
     * Создать URI для шаринга изображения через FileProvider.
     * 
     * @param bitmap Изображение для шаринга
     * @return URI изображения или null в случае ошибки
     */
    private suspend fun createShareableUri(bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, SHARE_DIR)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val imageFile = File(cacheDir, "share_${System.currentTimeMillis()}.jpg")
            
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: IOException) {
            android.util.Log.e("ShareHelper", "Ошибка создания URI для шаринга: ${e.message}", e)
            null
        }
    }
    
    /**
     * Очистить временные файлы шаринга.
     */
    suspend fun clearShareCache() = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, SHARE_DIR)
            if (cacheDir.exists() && cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            // Игнорируем ошибки очистки
        }
    }
}

