package com.example.neuralphotoredactor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
                    
                    AppNavigation(
                        navController = navController,
                        galleryScreen = {
                            GalleryScreen(
                                images = galleryViewModel.uiState.value.images,
                                isLoading = galleryViewModel.uiState.value.isLoading,
                                error = galleryViewModel.uiState.value.error,
                                onImageClick = { image ->
                                    editorViewModel.setImage(image)
                                    navController.navigate("editor")
                                },
                                onCameraClick = {
                                    // TODO: Реализовать открытие камеры
                                }
                            )
                        },
                        editorScreen = {
                            val state = editorViewModel.uiState.value
                            EditorScreen(
                                imageUri = state.imageData?.uri,
                                processedImageUri = state.processedResult?.processedUri,
                                isLoading = state.isLoading,
                                error = state.error,
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
                                history = historyViewModel.uiState.value.history,
                                isLoading = historyViewModel.uiState.value.isLoading,
                                error = historyViewModel.uiState.value.error,
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

