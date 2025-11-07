package com.example.neuralphotoredactor.presentation.ui.screen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.neuralphotoredactor.presentation.ui.components.ErrorMessage
import com.example.neuralphotoredactor.presentation.ui.components.ImageItem
import com.example.neuralphotoredactor.presentation.ui.components.LoadingIndicator
import com.example.neuralphotoredactor.presentation.viewmodel.GalleryViewModel

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
    onImageSelected: (com.example.neuralphotoredactor.domain.model.ImageData) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.value

    LaunchedEffect(Unit) {
        viewModel.loadImages()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        // TODO: Открыть камеру
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
                        // TODO: Открыть галерею
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
                state.isLoading -> {
                    LoadingIndicator()
                }
                state.error != null -> {
                    ErrorMessage(
                        message = state.error,
                        onDismiss = { viewModel.clearError() }
                    )
                }
                state.images.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No images found")
                        Button(onClick = { viewModel.loadImages() }) {
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
                        items(state.images) { image ->
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

