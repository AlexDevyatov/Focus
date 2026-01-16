package com.example.neuralphotoredactor.data.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
class ImageStorage
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        /**
         * Сохранить Bitmap в файл (старый метод для обратной совместимости).
         *
         * @param bitmap Изображение для сохранения
         * @param fileName Имя файла
         * @return URI сохраненного файла или null в случае ошибки
         */
        suspend fun saveBitmap(
            bitmap: Bitmap,
            fileName: String,
        ): Uri? =
            withContext(Dispatchers.IO) {
                try {
                    val imagesDir =
                        File(
                            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            "processed",
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
         * Сохранить Bitmap в отдельный альбом галереи.
         *
         * @param bitmap Изображение для сохранения
         * @param fileName Имя файла
         * @param albumName Название альбома (по умолчанию "Neural Photo Redactor")
         * @return URI сохраненного файла или null в случае ошибки
         */
        suspend fun saveBitmapToGallery(
            bitmap: Bitmap,
            fileName: String,
            albumName: String = "Neural Photo Redactor",
        ): Uri? =
            withContext(Dispatchers.IO) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+ (API 29+): используем MediaStore
                        val contentValues =
                            ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                put(
                                    MediaStore.MediaColumns.RELATIVE_PATH,
                                    Environment.DIRECTORY_PICTURES + "/" + albumName,
                                )
                            }

                        val uri =
                            context.contentResolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                contentValues,
                            ) ?: return@withContext null

                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        }

                        uri
                    } else {
                        // Android 9 и ниже: используем старый способ
                        val imagesDir =
                            File(
                                Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES,
                                ),
                                albumName,
                            )
                        if (!imagesDir.exists()) {
                            imagesDir.mkdirs()
                        }

                        val imageFile = File(imagesDir, fileName)
                        FileOutputStream(imageFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }

                        // Добавляем в MediaStore для отображения в галерее
                        val contentValues =
                            ContentValues().apply {
                                put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                put(MediaStore.Images.Media.TITLE, fileName)
                                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                            }

                        context.contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues,
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "ImageStorage",
                        "Ошибка сохранения в галерею: ${e.message}",
                        e,
                    )
                    null
                }
            }

        /**
         * Удалить файл по URI.
         *
         * @param uri URI файла для удаления
         */
        suspend fun deleteFile(uri: Uri) =
            withContext(Dispatchers.IO) {
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
        suspend fun getProcessedImages(): List<Uri> =
            withContext(Dispatchers.IO) {
                try {
                    val imagesDir =
                        File(
                            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            "processed",
                        )

                    if (!imagesDir.exists() || !imagesDir.isDirectory) {
                        return@withContext emptyList()
                    }

                    val imageFiles =
                        imagesDir.listFiles { file ->
                            file.isFile && (
                                file.name.endsWith(".jpg", ignoreCase = true) ||
                                    file.name.endsWith(".jpeg", ignoreCase = true) ||
                                    file.name.endsWith(".png", ignoreCase = true)
                            )
                        } ?: return@withContext emptyList()

                    // Сортируем по дате изменения (новые первыми)
                    val sortedFiles = imageFiles.sortedByDescending { it.lastModified() }

                    sortedFiles.map { Uri.fromFile(it) }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "ImageStorage",
                        "Ошибка получения обработанных изображений: ${e.message}",
                        e,
                    )
                    emptyList()
                }
            }
    }
