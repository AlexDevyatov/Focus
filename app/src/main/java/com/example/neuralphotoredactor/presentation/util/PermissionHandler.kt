package com.example.neuralphotoredactor.presentation.util

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Утилита для работы с разрешениями в Compose.
 * 
 * Предоставляет функции для запроса разрешений на доступ к камере и галерее.
 */
object PermissionHandler {
    /**
     * Получает список необходимых разрешений для доступа к изображениям.
     * 
     * @return Список разрешений в зависимости от версии Android
     */
    fun getImagePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Получает разрешение для камеры.
     * 
     * @return Разрешение на доступ к камере
     */
    fun getCameraPermission(): String {
        return Manifest.permission.CAMERA
    }
}

