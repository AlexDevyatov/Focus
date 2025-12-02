package com.example.neuralphotoredactor.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.core.content.ContextCompat
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.neuralphotoredactor.ui.components.ErrorMessage
import com.example.neuralphotoredactor.ui.components.LoadingIndicator

/**
 * Экран галереи для выбора изображений.
 * 
 * Автоматически запрашивает разрешение на доступ к галерее при открытии экрана.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    images: List<com.example.neuralphotoredactor.domain.model.ImageData>,
    isLoading: Boolean,
    error: String?,
    onImageClick: (com.example.neuralphotoredactor.domain.model.ImageData) -> Unit,
    onCameraClick: () -> Unit,
    onPermissionGranted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Определяем необходимое разрешение в зависимости от версии Android
    val permission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            // Android 12 и ниже
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    
    // Состояние разрешения
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Launcher для запроса разрешения
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        // Загружаем изображения сразу после предоставления разрешения
        if (isGranted) {
            onPermissionGranted()
        }
    }
    
    // Запрашиваем разрешение при открытии экрана, если оно не предоставлено
    LaunchedEffect(Unit) {
        if (hasPermission) {
            // Если разрешение уже предоставлено, загружаем изображения сразу
            onPermissionGranted()
        } else {
            // Если разрешения нет, запрашиваем его
            permissionLauncher.launch(permission)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Галерея") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCameraClick) {
                Text("📷")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        when {
            // Проверяем разрешения
            !hasPermission -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Необходимо разрешение на доступ к галерее",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Для отображения изображений из галереи требуется разрешение на доступ к медиафайлам",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { permissionLauncher.launch(permission) }
                        ) {
                            Text("Предоставить разрешение")
                        }
                    }
                }
            }
            isLoading -> {
                LoadingIndicator(Modifier.padding(paddingValues))
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorMessage(error)
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = paddingValues,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(images) { image ->
                        Card(
                            onClick = { onImageClick(image) },
                            modifier = Modifier
                                .aspectRatio(1f)
                        ) {
                            AsyncImage(
                                model = image.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

