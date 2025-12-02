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
import androidx.navigation.compose.rememberNavController
import com.example.neuralphotoredactor.ui.navigation.AppNavigation
import com.example.neuralphotoredactor.ui.screen.EditorScreen
import com.example.neuralphotoredactor.ui.screen.GalleryScreen
import com.example.neuralphotoredactor.ui.screen.HistoryScreen
import com.example.neuralphotoredactor.ui.theme.NeuralPhotoRedactorTheme
import com.example.neuralphotoredactor.ui.viewmodel.EditorViewModel
import com.example.neuralphotoredactor.ui.viewmodel.GalleryViewModel
import com.example.neuralphotoredactor.ui.viewmodel.HistoryViewModel
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
            NeuralPhotoRedactorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val galleryViewModel: GalleryViewModel = viewModel()
                    val editorViewModel: EditorViewModel = viewModel()
                    val historyViewModel: HistoryViewModel = viewModel()
                    
                    // Используем collectAsState() для реактивного обновления UI
                    val galleryUiState by galleryViewModel.uiState.collectAsState()
                    val editorUiState by editorViewModel.uiState.collectAsState()
                    val historyUiState by historyViewModel.uiState.collectAsState()
                    
                    AppNavigation(
                        navController = navController,
                        galleryScreen = {
                            GalleryScreen(
                                images = galleryUiState.images,
                                isLoading = galleryUiState.isLoading,
                                error = galleryUiState.error,
                                onImageClick = { image ->
                                    editorViewModel.setImage(image)
                                    navController.navigate("editor")
                                },
                                onCameraClick = {
                                    // TODO: Реализовать открытие камеры
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
                                isLoading = editorUiState.isLoading,
                                error = editorUiState.error,
                                filters = editorViewModel.availableFilters,
                                onFilterClick = { filter ->
                                    editorViewModel.applyFilter(filter)
                                },
                                onSaveClick = {
                                    // TODO: Реализовать сохранение
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
                        }
                    )
                }
            }
        }
    }
}

