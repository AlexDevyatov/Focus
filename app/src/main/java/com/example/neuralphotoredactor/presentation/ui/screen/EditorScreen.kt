package com.example.neuralphotoredactor.presentation.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.neuralphotoredactor.presentation.ui.components.ErrorMessage
import com.example.neuralphotoredactor.presentation.ui.components.LoadingIndicator
import com.example.neuralphotoredactor.presentation.viewmodel.EditorViewModel

/**
 * Экран редактора изображений с панелью инструментов и предпросмотром.
 * 
 * @param imageData Исходное изображение для редактирования
 * @param viewModel ViewModel для управления состоянием экрана
 * @param onNavigateToFilters Обработчик перехода на экран фильтров
 * @param modifier Модификатор для настройки внешнего вида
 */
@Composable
fun EditorScreen(
    imageData: com.example.neuralphotoredactor.domain.model.ImageData?,
    viewModel: EditorViewModel = hiltViewModel(),
    onNavigateToFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.value

    LaunchedEffect(imageData) {
        imageData?.let { viewModel.setCurrentImage(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onNavigateToFilters,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Filter")
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    ErrorMessage(
                        message = state.error,
                        onDismiss = { viewModel.clearError() }
                    )
                }
                state.currentImage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AsyncImage(
                            model = state.currentImage.uri,
                            contentDescription = "Edited Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No image selected")
                    }
                }
            }
        }
    }
}

