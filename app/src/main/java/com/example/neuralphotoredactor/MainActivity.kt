package com.example.neuralphotoredactor

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.neuralphotoredactor.ui.navigation.AppNavigation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.example.neuralphotoredactor.ui.navigation.BottomNavigationBar
import com.example.neuralphotoredactor.ui.screen.EditorScreen
import com.example.neuralphotoredactor.ui.screen.GalleryScreen
import com.example.neuralphotoredactor.ui.screen.HistoryScreen
import com.example.neuralphotoredactor.ui.screen.ProcessedImagesScreen
import com.example.neuralphotoredactor.ui.theme.AppTheme
import com.example.neuralphotoredactor.R
import com.example.neuralphotoredactor.ui.viewmodel.EditorViewModel
import com.example.neuralphotoredactor.ui.viewmodel.GalleryViewModel
import com.example.neuralphotoredactor.ui.viewmodel.HistoryViewModel
import com.example.neuralphotoredactor.ui.viewmodel.ProcessedImagesViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Главная Activity приложения.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val coroutineScope = rememberCoroutineScope()
                    
                    val galleryViewModel: GalleryViewModel = viewModel()
                    val editorViewModel: EditorViewModel = viewModel()
                    val historyViewModel: HistoryViewModel = viewModel()
                    val processedImagesViewModel: ProcessedImagesViewModel = viewModel()
                    
                    // Настраиваем callback для обновления галереи и обработанных изображений после сохранения
                    editorViewModel.onImageSaved = {
                        galleryViewModel.refreshImages()
                        processedImagesViewModel.refreshImages()
                    }
                    
                    // Настраиваем callback для навигации на экран обработанных изображений
                    editorViewModel.onNavigateToProcessed = {
                        processedImagesViewModel.refreshImages()
                        // Показываем Toast сообщение и выполняем навигацию после задержки
                        coroutineScope.launch(Dispatchers.Main) {
                            // Показываем Toast сообщение
                            Toast.makeText(
                                context,
                                context.getString(R.string.image_saved_toast),
                                Toast.LENGTH_SHORT
                            ).show()
                            // Выполняем навигацию после небольшой задержки, чтобы Toast был виден
                            delay(500) // Задержка 500мс для отображения Toast
                            navController.navigate("processed")
                        }
                    }
                    
                    // Используем collectAsState() для реактивного обновления UI
                    val galleryUiState by galleryViewModel.uiState.collectAsState()
                    val editorUiState by editorViewModel.uiState.collectAsState()
                    val historyUiState by historyViewModel.uiState.collectAsState()
                    val processedImagesUiState by processedImagesViewModel.uiState.collectAsState()
                    
                    // Логика для работы с камерой
                    var imageUri: Uri? by remember { mutableStateOf(null) }
                    
                    // Функция для создания временного файла
                    fun createImageFile(ctx: Context): File {
                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val imageFileName = "JPEG_${timeStamp}_"
                        val storageDir = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                        return File.createTempFile(
                            imageFileName,
                            ".jpg",
                            storageDir
                        )
                    }
                    
                    // Launcher для съемки фото
                    val takePictureLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.TakePicture()
                    ) { success ->
                        if (success && imageUri != null) {
                            // Передаем снятое изображение в редактор
                            editorViewModel.setImage(
                                com.example.neuralphotoredactor.domain.model.ImageData(imageUri!!)
                            )
                            navController.navigate("editor")
                            // Обновляем галерею, чтобы новое фото появилось
                            galleryViewModel.refreshImages()
                        }
                        // Очищаем временный URI
                        imageUri = null
                    }
                    
                    // Launcher для запроса разрешения камеры
                    val cameraPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            // Если разрешение предоставлено, создаем файл и открываем камеру
                            val photoFile = createImageFile(context)
                            val photoUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            imageUri = photoUri
                            takePictureLauncher.launch(photoUri)
                        }
                    }
                    
                    // Функция для проверки разрешения и открытия камеры
                    fun handleCameraClick() {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        
                        if (hasPermission) {
                            // Создаем файл и открываем камеру
                            val photoFile = createImageFile(context)
                            val photoUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            imageUri = photoUri
                            takePictureLauncher.launch(photoUri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    
                    Scaffold(
                        bottomBar = {
                            BottomNavigationBar(navController = navController)
                        }
                    ) { paddingValues ->
                        AppNavigation(
                            navController = navController,
                            galleryScreen = {
                            GalleryScreen(
                                images = galleryUiState.images,
                                isLoading = galleryUiState.isLoading,
                                isRefreshing = galleryUiState.isRefreshing,
                                error = galleryUiState.error,
                                onImageClick = { image ->
                                    editorViewModel.setImage(image)
                                    navController.navigate("editor")
                                },
                                onCameraClick = {
                                    handleCameraClick()
                                },
                                onRefresh = {
                                    galleryViewModel.refreshImages()
                                },
                                onPermissionGranted = {
                                    galleryViewModel.loadImages()
                                }
                            )
                        },
                        editorScreen = {
                            EditorScreen(
                                imageUri = editorUiState.imageData?.uri,
                                processedImageUri = editorUiState.processedResult?.processedUri,
                                previewBitmap = editorUiState.previewBitmap,
                                isLoading = editorUiState.isLoading,
                                error = editorUiState.error,
                                filters = editorViewModel.getCurrentCategoryFilters(),
                                selectedFilters = editorUiState.selectedFilters,
                                currentFilterIntensity = editorUiState.currentFilterIntensity,
                                showNeuralFilters = editorUiState.showNeuralFilters,
                                showEditMode = editorUiState.showEditMode,
                                brightness = editorUiState.brightness,
                                contrast = editorUiState.contrast,
                                colorBalanceRed = editorUiState.colorBalanceRed,
                                colorBalanceGreen = editorUiState.colorBalanceGreen,
                                colorBalanceBlue = editorUiState.colorBalanceBlue,
                                currentEditCategory = editorUiState.currentEditCategory,
                                appliedEdits = editorUiState.appliedEdits,
                                showCropOverlay = editorUiState.showCropOverlay,
                                cropBitmap = editorUiState.cropBitmap,
                                onFilterToggle = { filter ->
                                    editorViewModel.toggleFilter(filter)
                                },
                                onIntensityChange = { filter, intensity ->
                                    editorViewModel.updateFilterIntensity(filter, intensity)
                                },
                                onClearFilters = {
                                    editorViewModel.clearFilters()
                                },
                                onSaveClick = {
                                    // Применяем все выбранные фильтры с сохранением в файл
                                    editorViewModel.applyFilters()
                                },
                                onToggleFilterCategory = {
                                    editorViewModel.toggleFilterCategory()
                                },
                                onToggleEditMode = {
                                    editorViewModel.toggleEditMode()
                                },
                                onEditCategoryChange = { category ->
                                    editorViewModel.setEditCategory(category)
                                },
                                onEditClick = { editType ->
                                    editorViewModel.applyEdit(editType)
                                },
                                onBrightnessChange = { value ->
                                    editorViewModel.updateBrightness(value)
                                },
                                onContrastChange = { value ->
                                    editorViewModel.updateContrast(value)
                                },
                                onColorBalanceChange = { editType, value ->
                                    editorViewModel.updateColorBalance(editType, value)
                                },
                                onClearGeometricEdits = {
                                    editorViewModel.clearGeometricEdits()
                                },
                                onSaveToGallery = {
                                    editorViewModel.saveEditedImageToGallery()
                                },
                                onCropApply = { cropRect ->
                                    editorViewModel.applyCrop(cropRect)
                                },
                                onCropCancel = {
                                    editorViewModel.cancelCrop()
                                }
                            )
                        },
                        historyScreen = {
                            HistoryScreen(
                                history = historyUiState.history,
                                isLoading = historyUiState.isLoading,
                                error = historyUiState.error,
                                onItemClick = { result ->
                                    editorViewModel.setImage(
                                        com.example.neuralphotoredactor.domain.model.ImageData(
                                            result.processedUri
                                        )
                                    )
                                    navController.navigate("editor")
                                }
                            )
                        },
                        processedImagesScreen = {
                            ProcessedImagesScreen(
                                images = processedImagesUiState.images,
                                isLoading = processedImagesUiState.isLoading,
                                isRefreshing = processedImagesUiState.isRefreshing,
                                error = processedImagesUiState.error,
                                onImageClick = { imageData ->
                                    editorViewModel.setImage(imageData)
                                    navController.navigate("editor")
                                },
                                onRefresh = {
                                    processedImagesViewModel.refreshImages()
                                }
                            )
                        }
                    )
                    }
                }
            }
        }
    }
}

