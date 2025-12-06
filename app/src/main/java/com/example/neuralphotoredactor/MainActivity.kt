package com.example.neuralphotoredactor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.neuralphotoredactor.ui.navigation.AppNavigation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.example.neuralphotoredactor.ui.navigation.BottomNavigationBar
import com.example.neuralphotoredactor.ui.screen.EditorScreen
import com.example.neuralphotoredactor.ui.screen.GalleryScreen
import com.example.neuralphotoredactor.ui.screen.HistoryScreen
import com.example.neuralphotoredactor.ui.screen.ProcessedImagesScreen
import com.example.neuralphotoredactor.ui.theme.AppTheme
import com.example.neuralphotoredactor.ui.viewmodel.EditorViewModel
import com.example.neuralphotoredactor.ui.viewmodel.GalleryViewModel
import com.example.neuralphotoredactor.ui.viewmodel.HistoryViewModel
import com.example.neuralphotoredactor.ui.viewmodel.ProcessedImagesViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.viewmodel.compose.viewModel

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
                        navController.navigate("processed")
                    }
                    
                    // Используем collectAsState() для реактивного обновления UI
                    val galleryUiState by galleryViewModel.uiState.collectAsState()
                    val editorUiState by editorViewModel.uiState.collectAsState()
                    val historyUiState by historyViewModel.uiState.collectAsState()
                    val processedImagesUiState by processedImagesViewModel.uiState.collectAsState()
                    
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
                                    // TODO: Реализовать открытие камеры
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
                                filters = editorViewModel.availableFilters,
                                selectedFilters = editorUiState.selectedFilters,
                                currentFilterIntensity = editorUiState.currentFilterIntensity,
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

