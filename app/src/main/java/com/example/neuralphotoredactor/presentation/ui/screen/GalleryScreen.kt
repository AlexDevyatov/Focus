package com.example.neuralphotoredactor.presentation.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neuralphotoredactor.domain.model.ImageData
import com.example.neuralphotoredactor.presentation.ui.components.ErrorMessage
import com.example.neuralphotoredactor.presentation.ui.components.ImageItem
import com.example.neuralphotoredactor.presentation.ui.components.LoadingIndicator
import com.example.neuralphotoredactor.presentation.util.PermissionHandler
import com.example.neuralphotoredactor.presentation.viewmodel.GalleryViewModel
import java.io.File

/**
 * Экран галереи для выбора изображения из галереи или камеры.
 * 
 * @param viewModel ViewModel для управления состоянием экрана
 * @param onImageSelected Обработчик выбора изображения (переход на экран редактора)
 * @param modifier Модификатор для настройки внешнего вида
 */
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    onImageSelected: (ImageData) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Launcher для выбора изображения из галереи
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val imageData = ImageData(uri = it)
            onImageSelected(imageData)
        }
    }

    // URI для сохранения фото с камеры
    val cameraUri = remember {
        val file = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Используем FileProvider для Android 7.0+
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
    }

    // Launcher для камеры
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val imageData = ImageData(uri = cameraUri)
            onImageSelected(imageData)
        }
    }

    // Launcher для запроса разрешений на изображения
    val imagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.loadImages()
        } else {
            // Ошибка будет отображена через состояние ViewModel
        }
    }

    // Launcher для запроса разрешения на камеру
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(cameraUri)
        }
    }

    // Проверяем, есть ли уже разрешения
    val hasImagePermissions = remember {
        val permissions = PermissionHandler.getImagePermissions()
        permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    LaunchedEffect(Unit) {
        // Загружаем изображения, если разрешения уже есть
        if (hasImagePermissions) {
            viewModel.loadImages()
        } else {
            // Запрашиваем разрешения при первом запуске
            val permissions = PermissionHandler.getImagePermissions()
            imagePermissionLauncher.launch(permissions)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        // Запрашиваем разрешение на камеру и открываем камеру
                        cameraPermissionLauncher.launch(PermissionHandler.getCameraPermission())
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_camera),
                        contentDescription = "Camera"
                    )
                }
                FloatingActionButton(
                    onClick = {
                        // Открываем галерею для выбора изображения
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_gallery),
                        contentDescription = "Gallery"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.value.isLoading -> {
                    LoadingIndicator()
                }
                state.value.error != null -> {
                    ErrorMessage(
                        message = state.value.error ?: "",
                        onDismiss = { viewModel.clearError() }
                    )
                }
                state.value.images.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No images found")
                        Button(
                            onClick = {
                                // Проверяем разрешения перед загрузкой
                                val permissions = PermissionHandler.getImagePermissions()
                                val allGranted = permissions.all { permission ->
                                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                                }
                                
                                if (allGranted) {
                                    viewModel.loadImages()
                                } else {
                                    // Запрашиваем разрешения
                                    imagePermissionLauncher.launch(permissions)
                                }
                            }
                        ) {
                            Text("Refresh")
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        items(state.value.images) { image ->
                            ImageItem(
                                imageUri = image.uri,
                                onClick = { onImageSelected(image) }
                            )
                        }
                    }
                }
            }
        }
    }
}

